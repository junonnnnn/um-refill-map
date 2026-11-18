package com.example.umborrowbuddy.features.module2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class Station {
    enum Status {
        AVAILABLE, REPORTED, OUT_OF_SERVICE, UNKNOWN
    }

    final String id;
    final String name;
    final String building;
    final String floor;
    final String landmark;
    final double latitude;
    final double longitude;
    enum WaterType { COLD, HOT, FILTERED }

    final Status status;
    final String faculty;
    final String college;
    final List<WaterType> waterTypes;
    final java.util.Date lastCleanedAt;
    final java.util.Date lastFilterChangedAt;

    Station(String id, String name, String building, String floor, String landmark,
            double latitude, double longitude, Status status) {
        this(id, name, building, floor, landmark, latitude, longitude, status,
                "FSKTM", "", Collections.emptyList(), null, null);
    }

    Station(String id, String name, String building, String floor, String landmark,
            double latitude, double longitude, Status status, String faculty, String college,
            List<WaterType> waterTypes, java.util.Date lastCleanedAt, java.util.Date lastFilterChangedAt) {
        this.faculty = faculty;
        this.college = college;
        this.waterTypes = Collections.unmodifiableList(new ArrayList<>(waterTypes));
        this.lastCleanedAt = lastCleanedAt;
        this.lastFilterChangedAt = lastFilterChangedAt;
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

    boolean hasSameDetails(Station other) {
        return id.equals(other.id) && name.equals(other.name) && building.equals(other.building)
                && floor.equals(other.floor) && landmark.equals(other.landmark)
                && latitude == other.latitude && longitude == other.longitude && status == other.status
                && faculty.equals(other.faculty) && college.equals(other.college) && waterTypes.equals(other.waterTypes)
                && java.util.Objects.equals(lastCleanedAt, other.lastCleanedAt)
                && java.util.Objects.equals(lastFilterChangedAt, other.lastFilterChangedAt);
    }

    static List<Station> matching(List<Station> stations, String faculty, String college, WaterType waterType) {
        List<Station> result = new ArrayList<>();
        for (Station station : stations) {
            if ((faculty == null || faculty.equals(station.faculty))
                    && (college == null || college.equals(station.college))
                    && (waterType == null || station.waterTypes.contains(waterType))) result.add(station);
        }
        return result;
    }

    static Station nearestWorking(List<Station> stations, double latitude, double longitude) {
        return stations.stream().filter(station -> station.status == Status.AVAILABLE)
                .min(byDistance(latitude, longitude)).orElse(null);
    }

    static Status pinStatus(List<Station> group) {
        if (group.stream().anyMatch(s -> s.status == Status.OUT_OF_SERVICE)) return Status.OUT_OF_SERVICE;
        if (group.stream().anyMatch(s -> s.status == Status.REPORTED)) return Status.REPORTED;
        if (group.stream().anyMatch(s -> s.status == Status.UNKNOWN)) return Status.UNKNOWN;
        return Status.AVAILABLE;
    }

    static Comparator<Station> byName() {
        return Comparator.comparing((Station station) -> station.name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(station -> station.id);
    }

    static List<Station> findPinGroup(List<List<Station>> groups, String stationId) {
        for (List<Station> group : groups) {
            for (Station station : group) {
                if (station.id.equals(stationId)) return group;
            }
        }
        return Collections.emptyList();
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
