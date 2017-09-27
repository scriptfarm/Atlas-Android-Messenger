package com.layer.messenger.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.layer.sdk.LayerClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CustomEndpoint provides a mechanism for using endpoints besides the default Layer endpoints.
 * This is only useful for enterprise customers with custom endpoints.  Contact support@layer.com
 * for information.
 *
 * @see LayerClient.Options.Builder#customEndpoint(String, String, String)
 */
public class CustomEndpoint {
    private static Endpoint sEndpoint;
    private static Map<String, Endpoint> sEndpoints;

    public static String getLayerAppId(Context context) {
        Endpoint endpoint = getEndpoint(context);
        return endpoint == null ? null : endpoint.getAppId();
    }

    public static void setLayerClientOptions(Context context, LayerClient.Options.Builder optionsBuilder) {
        Endpoint endpoint = getEndpoint(context);
        if (endpoint != null) endpoint.setLayerClientOptions(optionsBuilder);
    }

    public static boolean hasEndpoints(Context context) {
        Map<String, Endpoint> endpoints = getEndpoints(context);
        return endpoints != null && !endpoints.isEmpty();
    }

    public static Spinner createSpinner(final Context context) {
        Set<String> endpointNames = getNames(context);
        if (endpointNames == null || endpointNames.isEmpty()) return null;

        List<String> namesList = new ArrayList<String>(endpointNames);
        Collections.sort(namesList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, namesList);
        Spinner spinner = new Spinner(context);
        spinner.setAdapter(adapter);

        Endpoint endpoint = getEndpoint(context);
        if (endpoint != null) {
            int position = namesList.indexOf(endpoint.getName());
            if (position != -1) spinner.setSelection(position);
        }
        setEndpointName(context, (String) spinner.getSelectedItem());

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setEndpointName(context, (String) parent.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setEndpointName(context, null);
            }
        });

        return spinner;
    }

    private static Set<String> getNames(Context context) {
        Map<String, Endpoint> endpoints = getEndpoints(context);
        return endpoints == null ? null : endpoints.keySet();
    }

    private static void setEndpointName(@NonNull Context context, String name) {
        context.getSharedPreferences("layer_custom_endpoint", Context.MODE_PRIVATE).edit().putString("name", name).apply();
        Map<String, Endpoint> endpoints = getEndpoints(context);
        sEndpoint = (endpoints == null) ? null : endpoints.get(name);
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Setting custom endpoint to: " + sEndpoint);
    }

    public static Endpoint getEndpoint(Context context) {
        if (sEndpoint != null) return sEndpoint;
        String savedEndpointName = context.getSharedPreferences("layer_custom_endpoint", Context.MODE_PRIVATE).getString("name", null);
        if (savedEndpointName == null) return null;
        Map<String, Endpoint> endpoints = getEndpoints(context);
        sEndpoint = (endpoints == null) ? null : endpoints.get(savedEndpointName);
        return sEndpoint;
    }

    @Nullable
    private static Map<String, Endpoint> getEndpoints(Context context) {
        if (sEndpoints != null) return sEndpoints;
        sEndpoints = new HashMap<>();

        try {
            // Read endpoints from assets
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            InputStream is = context.getAssets().open("LayerConfiguration.json");
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } catch (IOException e) {
                if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
                    }
                }
            }
            String content = writer.toString().trim();
            if (content.isEmpty()) return null;

            // Parse endpoints from JSON
            try {
                JSONArray array = new JSONArray(content);
                for (int i = 0; i < array.length(); i++) {
                    Endpoint endpoint = new Endpoint(array.getJSONObject(i));
                    sEndpoints.put(endpoint.getName(), endpoint);
                }
                return sEndpoints;
            } catch (JSONException e) {
                String errorMessage = "Unable to parse the LayerConfiguration.json file. Please ensure the formatting is correct.";
                if (Log.isLoggable(Log.ERROR)) Log.e(errorMessage, e);
                throw new IllegalStateException(errorMessage, e);
            }

        } catch (IOException e) {
            if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
        }
        return null;
    }

    public static class Endpoint {
        final String mName;
        final String mAppId;
        final String mProviderUrl;

        final String mPlatformUrl;
        final String mPlatformToken;

        final String mEndpointConf;
        final String mEndpointCert;
        final String mEndpointSync;
        final String mEndpointTelemetry;

        public Endpoint(JSONObject o) throws JSONException {
            mName = o.getString("name");
            mAppId = o.getString("app_id");
            mProviderUrl = o.getString("identity_provider_url");

            JSONObject platform = o.optJSONObject("platform");
            if (platform != null) {
                mPlatformUrl = platform.optString("url");
                mPlatformToken = platform.optString("token");
            } else {
                mPlatformUrl = null;
                mPlatformToken = null;
            }

            JSONObject endpoint = o.optJSONObject("endpoint");
            if (endpoint != null) {
                mEndpointConf = endpoint.getString("conf");
                mEndpointCert = endpoint.getString("cert");
                mEndpointSync = endpoint.getString("sync");
                mEndpointTelemetry = endpoint.has("telemetry") ? endpoint.getString("telemetry") : null;
            } else {
                mEndpointConf = null;
                mEndpointCert = null;
                mEndpointSync = null;
                mEndpointTelemetry = null;
            }
        }

        public void setLayerClientOptions(LayerClient.Options.Builder optionsBuilder) {
            if (mEndpointSync != null) {
                optionsBuilder.customEndpoint(mEndpointConf, mEndpointCert, mEndpointSync);
            }
        }

        public String getName() {
            return mName;
        }

        public String getAppId() {
            return mAppId;
        }

        public String getProviderUrl() {
            return mProviderUrl;
        }

        @Override
        public String toString() {
            return "Endpoint{" +
                    "mName='" + mName + '\'' +
                    ", mAppId='" + mAppId + '\'' +
                    ", mProviderUrl='" + mProviderUrl + '\'' +
                    ", mPlatformUrl='" + mPlatformUrl + '\'' +
                    ", mPlatformToken='" + mPlatformToken + '\'' +
                    ", mEndpointConf='" + mEndpointConf + '\'' +
                    ", mEndpointCert='" + mEndpointCert + '\'' +
                    ", mEndpointSync='" + mEndpointSync + '\'' +
                    ", mEndpointTelemetry= '" + mEndpointTelemetry + '\'' +
                    '}';
        }
    }
}
