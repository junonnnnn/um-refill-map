package com.example.umborrowbuddy.features.module2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class Station {
    enum Status {
        AVAILABLE, OUT_OF_SERVICE, UNKNOWN
    }

    final String id;
    final String name;
    final String building;
    final String floor;
    final String landmark;
    final double latitude;
    final double longitude;
    final Status status;

    Station(String id, String name, String building, String floor, String landmark,
            double latitude, double longitude, Status status) {
        this.id = id;
        this.name = name;
        this.building = building;
        this.floor = floor;
        this.landmark = landmark;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
    }

    static List<List<Station>> groupPins(List<Station> stations) {
        List<List<Station>> groups = new ArrayList<>();
        for (Station station : stations) {
            List<Station> group = groups.stream()
                    .filter(items -> items.get(0).latitude == station.latitude
                            && items.get(0).longitude == station.longitude)
                    .findFirst().orElse(null);
            if (group == null) {
                group = new ArrayList<>();
                groups.add(group);
            }
            group.add(station);
        }
        return groups;
    }

    static Comparator<Station> byName() {
        return Comparator.comparing((Station station) -> station.name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(station -> station.id);
    }

    static Comparator<Station> byDistance(double latitude, double longitude) {
        return Comparator.comparingDouble((Station station) ->
                distanceMeters(latitude, longitude, station.latitude, station.longitude))
                .thenComparing(byName());
    }

    static double distanceMeters(double fromLat, double fromLon, double toLat, double toLon) {
        double halfLat = Math.sin(Math.toRadians(toLat - fromLat) / 2);
        double halfLon = Math.sin(Math.toRadians(toLon - fromLon) / 2);
        double a = halfLat * halfLat + Math.cos(Math.toRadians(fromLat))
                * Math.cos(Math.toRadians(toLat)) * halfLon * halfLon;
        return 12_742_000 * Math.asin(Math.sqrt(Math.min(1, a)));
    }
}
