package com.layer.messenger;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import com.layer.atlas.messagetypes.text.TextCellFactory;
import com.layer.atlas.messagetypes.threepartimage.ThreePartImageUtils;
import com.layer.atlas.util.Util;
import com.layer.atlas.util.picasso.requesthandlers.MessagePartRequestHandler;
import com.layer.messenger.util.LayerAuthenticationProvider;
import com.layer.messenger.util.CustomEndpoint;
import com.layer.messenger.util.AuthenticationProvider;
import com.layer.messenger.util.Log;
import com.layer.sdk.LayerClient;
import com.squareup.picasso.Picasso;

import java.util.Arrays;

/**
 * App provides static access to a LayerClient and other Atlas and Messenger context, including
 * AuthenticationProvider, ParticipantProvider, Participant, and Picasso.
 * @see LayerClient
 * @see Picasso
 * @see AuthenticationProvider
 */
public class App extends Application {

    // Set your Layer App ID from your Layer Developer Dashboard.
    public final static String LAYER_APP_ID = null;

    private static Application sInstance;
    private static LayerClient sLayerClient;
    private static AuthenticationProvider sAuthProvider;
    private static Picasso sPicasso;

    //==============================================================================================
    // Application Overrides
    //==============================================================================================

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable verbose logging in debug builds
        if (BuildConfig.DEBUG) {
            com.layer.atlas.util.Log.setLoggingEnabled(true);
            com.layer.messenger.util.Log.setAlwaysLoggable(true);
            LayerClient.setLoggingEnabled(this, true);
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

        // Allow the LayerClient to track app state
        LayerClient.applicationCreated(this);

        sInstance = this;
    }

    public static Application getInstance() {
        return sInstance;
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
    public static boolean routeLogin(Activity from) {
        return getAuthenticationProvider().routeLogin(getLayerClient(), getLayerAppId(), from);
    }

    /**
     * Authenticates with the AuthenticationProvider and Layer, returning asynchronous results to
     * the provided callback.
     *
     * @param credentials Credentials associated with the current AuthenticationProvider.
     * @param callback    Callback to receive authentication results.
     */
    @SuppressWarnings("unchecked")
    public static void authenticate(Object credentials, AuthenticationProvider.Callback callback) {
        LayerClient client = getLayerClient();
        if (client == null) return;
        String layerAppId = getLayerAppId();
        if (layerAppId == null) return;
        getAuthenticationProvider()
                .setCredentials(credentials)
                .setCallback(callback);
        client.authenticate();
    }

    /**
     * Deauthenticates with Layer and clears cached AuthenticationProvider credentials.
     *
     * @param callback Callback to receive deauthentication success and failure.
     */
    public static void deauthenticate(final Util.DeauthenticationCallback callback) {
        Util.deauthenticate(getLayerClient(), new Util.DeauthenticationCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onDeauthenticationSuccess(LayerClient client) {
                getAuthenticationProvider().setCredentials(null);
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
     * unable to create a LayerClient (due to no App ID, etc.). Set App Id {@link App.LAYER_APP_ID}
     * @return New or existing LayerClient, or `null` if a LayerClient could not be constructed.
     */
    public static LayerClient getLayerClient() {
        if (sLayerClient == null) {
            // Custom options for constructing a LayerClient
            LayerClient.Options options = new LayerClient.Options()

                    /* Fetch the minimum amount per conversation when first authenticated */
                    .historicSyncPolicy(LayerClient.Options.HistoricSyncPolicy.FROM_LAST_MESSAGE)
                    
                    /* Automatically download text and ThreePartImage info/preview */
                    .autoDownloadMimeTypes(Arrays.asList(
                            TextCellFactory.MIME_TYPE,
                            ThreePartImageUtils.MIME_TYPE_INFO,
                            ThreePartImageUtils.MIME_TYPE_PREVIEW));

            sLayerClient = generateLayerClient(sInstance, options);

            // Unable to generate Layer Client (no App ID, etc.)
            if (sLayerClient == null) return null;

            /* Register AuthenticationProvider for handling authentication challenges */
            sLayerClient.registerAuthenticationListener(getAuthenticationProvider());
        }
        return sLayerClient;
    }

    public static AuthenticationProvider getAuthenticationProvider() {
        if (sAuthProvider == null) {
            sAuthProvider = generateAuthenticationProvider(sInstance);

            // If we have cached credentials, try authenticating with Layer
            LayerClient layerClient = getLayerClient();
            if (layerClient != null && sAuthProvider.hasCredentials()) layerClient.authenticate();
        }
        return sAuthProvider;
    }

    public static Picasso getPicasso() {
        if (sPicasso == null) {
            // Picasso with custom RequestHandler for loading from Layer MessageParts.
            sPicasso = new Picasso.Builder(sInstance)
                    .addRequestHandler(new MessagePartRequestHandler(getLayerClient()))
                    .build();
        }
        return sPicasso;
    }

    public static String getLayerAppId() {
        return (LAYER_APP_ID != null) ? LAYER_APP_ID : CustomEndpoint.getLayerAppId();
    }

    private static LayerClient generateLayerClient(Context context, LayerClient.Options options) {
        String layerAppId = getLayerAppId();
        if (layerAppId == null) {
            if (Log.isLoggable(Log.ERROR)) Log.e(context.getString(R.string.app_id_required));
            return null;
        }
        options.useFirebaseCloudMessaging(true);
        CustomEndpoint.setLayerClientOptions(options);
        return LayerClient.newInstance(context, layerAppId, options);
    }

    private static AuthenticationProvider generateAuthenticationProvider(Context context) {
        return new LayerAuthenticationProvider(context);
    }

}
