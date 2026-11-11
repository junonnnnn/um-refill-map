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

    private static Station station(String id, String name, String floor, double longitude) {
        return new Station(id, name, "Block B", floor, "Landmark", 3.128393, longitude,
                Station.Status.UNKNOWN);
    }
}
