package com.example.umborrowbuddy.features.module2;

import com.google.firebase.firestore.GeoPoint;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class StationRepositoryTest {
    @Test
    public void readsApprovedFieldsAndKeepsTheDocumentId() {
        Station station = StationRepository.readStation("fsktm-001", record());
        assertEquals("fsktm-001", station.id);
        assertEquals("Block B Entrance", station.name);
        assertEquals("Block B", station.building);
        assertEquals("Ground floor", station.floor);
        assertEquals("Behind the door", station.landmark);
        assertEquals(3.128393, station.latitude, 0);
        assertEquals(101.650283, station.longitude, 0);
    }

    @Test
    public void readsEachConfirmedStatusWithoutDefaulting() {
        for (Station.Status status : Station.Status.values()) {
            Map<String, Object> data = record();
            data.put("status", status.name());
            assertEquals(status, StationRepository.readStation("fsktm-001", data).status);
        }
    }

    @Test
    public void rejectsMissingOrInvalidStatuses() {
        for (Object status : new Object[]{null, "", "broken", 1}) {
            Map<String, Object> data = record();
            data.put("status", status);
            assertThrows(IllegalArgumentException.class,
                    () -> StationRepository.readStation("fsktm-001", data));
        }
    }

    @Test
    public void requiresARealGeoPointAndCompleteStationDetails() {
        for (String field : new String[]{"location", "name", "building", "floor", "landmark"}) {
            Map<String, Object> data = record();
            data.put(field, " ");
            assertThrows(IllegalArgumentException.class,
                    () -> StationRepository.readStation("fsktm-001", data));
        }
    }

    @Test
    public void readsStationsAcrossCampus() {
        Map<String, Object> data = record();
        data.put("faculty", "Other faculty");
        assertEquals("Other faculty", StationRepository.readStation("campus-001", data).faculty);
        data.remove("faculty");
        data.put("college", "College 1");
        data.put("waterTypes", java.util.Arrays.asList("COLD", "FILTERED"));
        data.put("lastCleanedAt", new com.google.firebase.Timestamp(1234, 0));
        Station station = StationRepository.readStation("college-001", data);
        assertEquals("College 1", station.college);
        assertEquals(2, station.waterTypes.size());
        assertEquals(1234000, station.lastCleanedAt.getTime());
        data.put("waterTypes", java.util.Arrays.asList("WARM"));
        assertThrows(IllegalArgumentException.class, () -> StationRepository.readStation("bad", data));
        data.remove("waterTypes");
        data.put("lastCleanedAt", "yesterday");
        assertThrows(IllegalArgumentException.class, () -> StationRepository.readStation("bad", data));
    }

    @Test
    public void metadataOnlySnapshotsKeepDetailsButContentChangesRefreshThem() {
        Map<String, Object> data = record();
        Station before = StationRepository.readStation("s", data);
        org.junit.Assert.assertTrue(before.hasSameDetails(StationRepository.readStation("s", data)));
        data.put("status", "REPORTED");
        org.junit.Assert.assertFalse(before.hasSameDetails(StationRepository.readStation("s", data)));
        data.put("status", "UNKNOWN");
        data.put("waterTypes", java.util.Arrays.asList("COLD"));
        org.junit.Assert.assertFalse(before.hasSameDetails(StationRepository.readStation("s", data)));
        data.remove("waterTypes");
        data.put("lastFilterChangedAt", new com.google.firebase.Timestamp(1234, 0));
        org.junit.Assert.assertFalse(before.hasSameDetails(StationRepository.readStation("s", data)));
    }

    private static Map<String, Object> record() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Block B Entrance");
        data.put("building", "Block B");
        data.put("floor", "Ground floor");
        data.put("landmark", "Behind the door");
        data.put("faculty", "FSKTM");
        data.put("location", new GeoPoint(3.128393, 101.650283));
        data.put("status", "UNKNOWN");
        return data;
    }
}
