package com.example.umborrowbuddy.features.module2;

import android.content.Context;
import android.content.Intent;

public final class StationReportContract {
    public static final String ACTION_REPORT_ISSUE = "com.example.umborrowbuddy.action.REPORT_ISSUE";
    public static final String EXTRA_STATION_ID = "com.example.umborrowbuddy.extra.STATION_ID";

    private StationReportContract() { }

    public static Intent forStation(Context context, String stationId) {
        if (stationId == null || stationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Station ID is required.");
        }
        return new Intent(ACTION_REPORT_ISSUE).setPackage(context.getPackageName())
                .putExtra(EXTRA_STATION_ID, stationId);
    }
}
