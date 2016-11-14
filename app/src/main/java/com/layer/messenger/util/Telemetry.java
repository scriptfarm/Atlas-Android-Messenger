package com.layer.messenger.util;


import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Telemetry {
    INSTANCE;

    public enum Scenario {
        LOGIN_FIRST_SYNC_ITERATION("Login to first synced data displayed");

        private final String mDescription;

        Scenario(String description) {
            mDescription = description;
        }

        @Override
        public String toString() {
            return mDescription;
        }
    }

    private static final String TAG = "AtlasMessengerTelemetry";
    private static boolean mEnabled = false;
    private Map<Scenario, List<Marker>> mScenarioTelemetry = new HashMap<>();


    public static void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public static boolean isEnabled() {
        return mEnabled;
    }

    public boolean isScenarioActive(Scenario scenario) {
        return mScenarioTelemetry.containsKey(scenario);
    }

    public void beginScenario(Scenario scenario) {
        long currentMillis = System.currentTimeMillis();
        Marker beginMarker = new Marker(currentMillis, "Beginning scenario - " + scenario);

        ArrayList<Marker> markers = new ArrayList<>();
        markers.add(beginMarker);
        mScenarioTelemetry.put(scenario, markers);
    }

    public void addMarker(Scenario scenario, String markerDescription) {
        long currentMillis = System.currentTimeMillis();
        Marker marker = new Marker(currentMillis, markerDescription);

        List<Marker> markers = mScenarioTelemetry.get(scenario);
        if (markers == null) {
            // No scenario started.
            android.util.Log.w(TAG, "Attempting to add marker to scenario before beginning. Scenario: " + scenario);
            return;
        }
        markers.add(marker);
    }

    public void endScenario(Scenario scenario) {
        long currentMillis = System.currentTimeMillis();
        Marker endMarker = new Marker(currentMillis, "Ending scenario - " + scenario);

        List<Marker> markers = mScenarioTelemetry.get(scenario);
        if (markers == null) {
            // No scenario started.
            android.util.Log.w(TAG, "Attempting to end scenario before beginning. Scenario: " + scenario);
            return;
        }
        markers.add(endMarker);

        logMarkers(scenario, markers);
        mScenarioTelemetry.remove(scenario);
    }

    private void logMarkers(Scenario scenario, @NonNull List<Marker> markers) {
        if (markers.size() < 2) {
            android.util.Log.w(TAG, "Not enough markers to analyze performance for scenario: " + scenario);
            return;
        }

        StringBuilder sb = new StringBuilder();

        // Append beginning marker
        Marker firstMarker = markers.get(0);
        long startTime = firstMarker.mTime;
        long previousTime = startTime;
        sb.append(firstMarker.mDescription)
                .append(" at ")
                .append(startTime)
                .append("ms\n");

        // Append markers
        for (int i = 1; i < markers.size() - 1; i++) {
            Marker marker = markers.get(i);
            sb.append(marker.mDescription)
                    .append(". Time since previous marker: ")
                    .append(marker.mTime - previousTime)
                    .append("ms\n");
            previousTime = marker.mTime;
        }

        // Append last marker
        Marker lastMarker = markers.get(markers.size() - 1);
        sb.append(lastMarker.mDescription)
                .append(". Time since previous marker: ")
                .append(lastMarker.mTime - previousTime)
                .append("ms. Time for scenario: ")
                .append(lastMarker.mTime - startTime)
                .append("ms.");

        android.util.Log.i("LayerTelemetry", sb.toString());
    }

    private static class Marker {
        long mTime;
        String mDescription;

        Marker(long time, String description) {
            mTime = time;
            mDescription = description;
        }
    }

}