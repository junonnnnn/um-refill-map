package com.example.umborrowbuddy.features.module2;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class StationTest {
    @Test
    public void pinsGroupOnlyStationsWithTheSameCoordinates() {
        Station entrance = station("1", "Entrance", "Ground floor", 101.650283);
        Station upstairs = station("2", "Lounge", "First floor", 101.650283);
        Station other = station("3", "Other", "Ground floor", 101.650284);

        assertEquals(Arrays.asList(Arrays.asList(entrance, upstairs), Arrays.asList(other)),
                Station.groupPins(Arrays.asList(entrance, other, upstairs)));
    }

    @Test
    public void directorySortsByDistanceOrName() {
        Station near = station("1", "Lounge", "First floor", 101.650050);
        Station far = station("2", "Entrance", "Ground floor", 101.650283);
        List<Station> ordered = new ArrayList<>(Arrays.asList(far, near));

        ordered.sort(Station.byDistance(3.128393, 101.650050));
        assertEquals(Arrays.asList(near, far), ordered);
        assertEquals(26, Station.distanceMeters(3.128393, 101.650050, 3.128393, 101.650283), 1);

        ordered.sort(Station.byName());
        assertEquals(Arrays.asList(far, near), ordered);
    }

    @Test
    public void pinSelectionUsesStableIdsAfterGroupsReorderOrDisappear() {
        Station entrance = station("fsktm-001", "Entrance", "Ground floor", 101.650283);
        Station upstairs = station("fsktm-002", "Lounge", "First floor", 101.650283);
        Station other = station("fsktm-003", "Other", "Ground floor", 101.650050);
        List<List<Station>> reordered = Station.groupPins(Arrays.asList(other, entrance, upstairs));
        assertEquals(Arrays.asList(entrance, upstairs), Station.findPinGroup(reordered, "fsktm-001"));
        assertEquals(Arrays.asList(entrance, upstairs), Station.findPinGroup(reordered, "fsktm-002"));
        assertEquals(Arrays.asList(), Station.findPinGroup(
                Station.groupPins(Arrays.asList(other)), "fsktm-001"));
    }

    @Test
    public void filtersCombineAndNearestUsesOnlyWorkingMatches() {
        Station cold = campusStation("cold", "Science", "", Station.Status.AVAILABLE,
                Arrays.asList(Station.WaterType.COLD, Station.WaterType.FILTERED), 101.65);
        Station hot = campusStation("hot", "Science", "College 1", Station.Status.AVAILABLE,
                Arrays.asList(Station.WaterType.HOT), 101.66);
        Station reported = campusStation("reported", "Science", "College 1", Station.Status.REPORTED,
                Arrays.asList(Station.WaterType.COLD), 101.65001);
        List<Station> all = Arrays.asList(cold, hot, reported);
        assertEquals(Arrays.asList(cold, reported), Station.matching(all, "Science", null, Station.WaterType.COLD));
        assertEquals(Arrays.asList(hot), Station.matching(all, "Science", "College 1", Station.WaterType.HOT));
        assertEquals(Arrays.asList(), Station.matching(all, "Other", null, null));
        assertEquals(cold, Station.nearestWorking(all, 3.128393, 101.65001));
        assertEquals(hot, Station.nearestWorking(Station.matching(all, null, "College 1", null), 3.128393, 101.65));
        assertEquals(null, Station.nearestWorking(Arrays.asList(reported), 3.128393, 101.65));
        assertEquals(null, Station.nearestWorking(Arrays.asList(), 3.128393, 101.65));
        for (Station.Status status : Station.Status.values()) {
            if (status != Station.Status.AVAILABLE) assertEquals(null, Station.nearestWorking(
                    Arrays.asList(campusStation("x", "Science", "", status, Arrays.asList(), 101.65)), 3, 101));
        }
    }

    @Test
    public void sharedPinsUseWorstStatusIndependentOfOrder() {
        List<Station> group = new ArrayList<>();
        for (Station.Status status : new Station.Status[]{Station.Status.AVAILABLE,
                Station.Status.UNKNOWN, Station.Status.REPORTED, Station.Status.OUT_OF_SERVICE}) {
            group.add(campusStation(status.name(), "Science", "", status, Arrays.asList(), 101.65));
            assertEquals(status, Station.pinStatus(group));
        }
        java.util.Collections.reverse(group);
        assertEquals(Station.Status.OUT_OF_SERVICE, Station.pinStatus(group));
    }

    private static Station campusStation(String id, String faculty, String college, Station.Status status,
            List<Station.WaterType> types, double longitude) {
        return new Station(id, id, "Building", "Ground", "Entrance", 3.128393, longitude,
                status, faculty, college, types, null, null);
    }

    private static Station station(String id, String name, String floor, double longitude) {
        return new Station(id, name, "Block B", floor, "Landmark", 3.128393, longitude,
                Station.Status.UNKNOWN);
    }
}
