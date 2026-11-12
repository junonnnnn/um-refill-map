package com.example.umborrowbuddy.features.module2;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class StationReportTest {
    @Test public void showsOnlyOpenWorkflowStatesAndPreservesDescriptionAndDate() {
        Map<String, Object> record = record();
        record.put("createdAt", new com.google.firebase.Timestamp(1234, 0));
        for (StationReport.Status status : StationReport.Status.values()) {
            record.put("status", status.name());
            StationReport report = StationReport.read("report-1", "station-1", record);
            assertEquals("report-1", report.id);
            assertEquals("Low water pressure", report.description);
            assertEquals(1234000, report.createdAt.getTime());
            assertEquals(status == StationReport.Status.OPEN || status == StationReport.Status.IN_PROGRESS,
                    report.isOpen());
        }
    }

    @Test public void rejectsWrongStationMalformedStatusAndEmptyDescription() {
        Map<String, Object> record = record();
        assertThrows(IllegalArgumentException.class, () -> StationReport.read("r", "wrong", record));
        record.put("status", "UNKNOWN");
        assertThrows(IllegalArgumentException.class, () -> StationReport.read("r", "station-1", record));
        record.put("status", "OPEN");
        record.put("description", "  ");
        assertThrows(IllegalArgumentException.class, () -> StationReport.read("r", "station-1", record));
    }

    private static Map<String, Object> record() {
        Map<String, Object> record = new HashMap<>();
        record.put("stationId", "station-1");
        record.put("description", "Low water pressure");
        record.put("status", "OPEN");
        return record;
    }
}
