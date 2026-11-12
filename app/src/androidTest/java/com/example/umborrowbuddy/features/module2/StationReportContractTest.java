package com.example.umborrowbuddy.features.module2;

import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(AndroidJUnit4.class)
public class StationReportContractTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void passesTheStableIdOnlyToThisApplication() {
        Intent intent = StationReportContract.forStation(context, "fsktm-002");
        assertEquals("com.example.umborrowbuddy.action.REPORT_ISSUE", intent.getAction());
        assertEquals("com.example.umborrowbuddy", intent.getPackage());
        assertEquals("fsktm-002", intent.getStringExtra("com.example.umborrowbuddy.extra.STATION_ID"));
    }

    @Test
    public void rejectsMissingStationIds() {
        for (String id : new String[]{null, "", " "}) {
            assertThrows(IllegalArgumentException.class,
                    () -> StationReportContract.forStation(context, id));
        }
    }
}
