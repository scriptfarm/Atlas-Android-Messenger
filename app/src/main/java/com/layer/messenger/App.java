package com.layer.messenger;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;

import com.layer.messenger.util.AuthenticationProvider;
import com.layer.messenger.util.CustomEndpoint;
import com.layer.messenger.util.LayerAuthenticationProvider;
import com.layer.messenger.util.Log;
import com.layer.sdk.LayerClient;
import com.layer.ui.message.messagetypes.text.TextCellFactory;
import com.layer.ui.message.messagetypes.threepartimage.ThreePartImageUtils;
import com.layer.ui.util.Util;
import com.layer.ui.util.imagecache.requesthandlers.MessagePartRequestHandler;
import com.squareup.picasso.Picasso;

import java.util.HashSet;
import java.util.Set;

/**
 * App provides static access to a LayerClient and other Atlas and Messenger context, including
 * AuthenticationProvider, ParticipantProvider, Participant, and Picasso.
 *
 * @see LayerClient
 * @see Picasso
 * @see AuthenticationProvider
 */
public class App extends Application {

    public static final String SHARED_PREFS = "MESSENGER_SHARED_PREFS";
    public static final String SHARED_PREFS_KEY_TELEMETRY_ENABLED = "TELEMETRY_ENABLED";

    private LayerClient mLayerClient;
    private LayerAuthenticationProvider mAuthProvider;
    private Picasso mPicasso;

    //==============================================================================================
    // Application Overrides
    //==============================================================================================

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable verbose logging in debug builds
        if (BuildConfig.DEBUG) {
            com.layer.ui.util.Log.setLoggingEnabled(true);
            com.layer.messenger.util.Log.setAlwaysLoggable(true);
            LayerClient.setLoggingEnabled(true);
            LayerClient.setPrivateLoggingEnabled(true);

            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        if (Log.isPerfLoggable()) {
            Log.perf("Application onCreate()");
        }

        mAuthProvider = new LayerAuthenticationProvider(this);
        LayerClient.registerAuthenticationChallengeListener(mAuthProvider);
    }


    //==============================================================================================
    // Identity Provider Methods
    //==============================================================================================

    /**
     * Routes the user to the proper Activity depending on their authenticated state.  Returns
     * `true` if the user has been routed to another Activity, or `false` otherwise.
     *
     * @param from Activity to route from.
     * @return `true` if the user has been routed to another Activity, or `false` otherwise.
     */
    public boolean routeLogin(Activity from) {
        return mAuthProvider.routeLogin(getLayerClient(), getLayerAppId(), from);
    }

    /**
     * Authenticates with the AuthenticationProvider and Layer, returning asynchronous results to
     * the provided callback.
     *
     * @param credentials Credentials associated with the current AuthenticationProvider.
     * @param callback    Callback to receive authentication results.
     */
    @SuppressWarnings("unchecked")
    public void authenticate(LayerAuthenticationProvider.Credentials credentials, AuthenticationProvider.Callback callback) {
        LayerClient client = getLayerClient();
        if (client == null) return;
        String layerAppId = getLayerAppId();
        if (layerAppId == null) return;
        mAuthProvider
                .setCredentials(credentials)
                .setCallback(callback);
        // TODO we shouldn't always request an authentication nonce
        client.requestAuthenticationNonce();
    }

    /**
     * Deauthenticates with Layer and clears cached AuthenticationProvider credentials.
     *
     * @param callback Callback to receive deauthentication success and failure.
     */
    public void deauthenticate(final Util.DeauthenticationCallback callback) {
        Util.deauthenticate(getLayerClient(), new Util.DeauthenticationCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onDeauthenticationSuccess(LayerClient client) {
                mAuthProvider.setCredentials(null);
                callback.onDeauthenticationSuccess(client);
            }

            @Override
            public void onDeauthenticationFailed(LayerClient client, String reason) {
                callback.onDeauthenticationFailed(client, reason);
            }
        });
    }


    //==============================================================================================
    // Getters / Setters
    //==============================================================================================

    /**
     * Gets or creates a LayerClient, using a default set of LayerClient.Options
     * App ID and Options from the `generateLayerClient` method.  Returns `null` if the App was
     * unable to create a LayerClient (due to no App ID, etc.). Set the information in assets/LayerConfiguration.json
     * @return New or existing LayerClient, or `null` if a LayerClient could not be constructed.
     */
    public LayerClient getLayerClient() {
        if (mLayerClient == null) {
            String layerAppId = getLayerAppId();
            if (layerAppId == null) {
                if (Log.isLoggable(Log.ERROR)) Log.e(getString(R.string.app_id_required));
                return null;
            }

            boolean telemetryEnabled;
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
            if (sharedPreferences.contains(SHARED_PREFS_KEY_TELEMETRY_ENABLED)) {
                telemetryEnabled = sharedPreferences.getBoolean(SHARED_PREFS_KEY_TELEMETRY_ENABLED, true);
            } else {
                sharedPreferences.edit().putBoolean(SHARED_PREFS_KEY_TELEMETRY_ENABLED, true).apply();
                telemetryEnabled = true;
            }

            Set<String> autoDownloadMimeTypes = new HashSet<>(3);
            autoDownloadMimeTypes.add(TextCellFactory.MIME_TYPE);
            autoDownloadMimeTypes.add(ThreePartImageUtils.MIME_TYPE_INFO);
            autoDownloadMimeTypes.add(ThreePartImageUtils.MIME_TYPE_PREVIEW);

            // Custom options for constructing a LayerClient
            // TODO Telemetry support
            LayerClient.Options.Builder optionsBuilder = new LayerClient.Options.Builder()

                    /* Fetch the minimum amount per conversation when first authenticated */
                    .historicSyncPolicy(LayerClient.Options.HistoricSyncPolicy.FROM_LAST_MESSAGE)

                    /* Automatically download text and ThreePartImage info/preview */
                    .autoDownloadMimeTypes(autoDownloadMimeTypes)

                    .runAgainstStandalone(true)

                    .useFirebaseCloudMessaging(true, "565052870572");


            CustomEndpoint.setLayerClientOptions(this, optionsBuilder);

            mLayerClient = LayerClient.newInstance(layerAppId, optionsBuilder.build());

            com.layer.messenger.util.Util.init(this, mLayerClient, getPicasso());

            /* Register AuthenticationProvider for handling authentication challenges */
            mLayerClient.registerAuthenticationListener(mAuthProvider);

            /* Connect the LayerClient to Layer servers */
            mLayerClient.connect();
        }
        return mLayerClient;
    }

    public Picasso getPicasso() {
        if (mPicasso == null) {
            // Picasso with custom RequestHandler for loading from Layer MessageParts.
            mPicasso = new Picasso.Builder(this)
                    .addRequestHandler(new MessagePartRequestHandler(getLayerClient()))
                    .build();
        }
        return mPicasso;
    }

    public String getLayerAppId() {
        return CustomEndpoint.getLayerAppId(this);
    }

    private static AuthenticationProvider generateAuthenticationProvider(Context context) {
        return new LayerAuthenticationProvider(context);
    }

}