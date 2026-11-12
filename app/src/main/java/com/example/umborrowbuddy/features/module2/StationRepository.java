package com.example.umborrowbuddy.features.module2;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class StationRepository {
    interface Callback {
        void onStations(List<Station> stations, boolean fromCache);
        void onFailure();
    }

    private final FirebaseFirestore firestore;

    StationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    StationRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    ListenerRegistration listen(Callback callback) {
        return firestore.collection("refill_stations")
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        callback.onFailure();
                        return;
                    }
                    List<Station> stations = new ArrayList<>();
                    try {
                        for (QueryDocumentSnapshot document : snapshot) {
                            stations.add(readStation(document.getId(), document.getData()));
                        }
                    } catch (IllegalArgumentException invalidRecord) {
                        callback.onFailure();
                        return;
                    }
                    callback.onStations(stations, snapshot.getMetadata().isFromCache());
                });
    }

    interface ReportCallback {
        void onReports(List<StationReport> reports, boolean fromCache);
        void onFailure();
    }

    ListenerRegistration listenReports(String stationId, ReportCallback callback) {
        return firestore.collection("maintenance_reports").whereEqualTo("stationId", stationId)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        callback.onFailure();
                        return;
                    }
                    List<StationReport> reports = new ArrayList<>();
                    try {
                        for (QueryDocumentSnapshot document : snapshot) {
                            StationReport report = StationReport.read(document.getId(), stationId, document.getData());
                            if (report.isOpen()) reports.add(report);
                        }
                        reports.sort(java.util.Comparator.comparing((StationReport r) -> r.createdAt,
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                                .thenComparing(r -> r.id));
                    } catch (IllegalArgumentException invalidRecord) {
                        callback.onFailure();
                        return;
                    }
                    callback.onReports(reports, snapshot.getMetadata().isFromCache());
                });
    }

    static Station readStation(String id, Map<String, Object> data) {
        String faculty = optionalText(data, "faculty");
        String college = optionalText(data, "college");
        if ((faculty.isEmpty() && college.isEmpty()) || !(data.get("location") instanceof GeoPoint)) {
            throw new IllegalArgumentException("Invalid campus station.");
        }
        List<Station.WaterType> waterTypes = new ArrayList<>();
        Object types = data.get("waterTypes");
        if (types != null) {
            if (!(types instanceof List)) throw new IllegalArgumentException("Invalid water types.");
            for (Object type : (List<?>) types) {
                if (!(type instanceof String)) throw new IllegalArgumentException("Invalid water type.");
                Station.WaterType value = Station.WaterType.valueOf((String) type);
                if (!waterTypes.contains(value)) waterTypes.add(value);
            }
        }
        GeoPoint location = (GeoPoint) data.get("location");
        return new Station(id, requiredText(data, "name"), requiredText(data, "building"),
                requiredText(data, "floor"), requiredText(data, "landmark"),
                location.getLatitude(), location.getLongitude(),
                Station.Status.valueOf(requiredText(data, "status")), faculty, college, waterTypes,
                optionalDate(data, "lastCleanedAt"), optionalDate(data, "lastFilterChangedAt"));
    }

    static java.util.Date optionalDate(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (!(value instanceof com.google.firebase.Timestamp)) throw new IllegalArgumentException("Invalid date: " + key);
        return ((com.google.firebase.Timestamp) value).toDate();
    }

    private static String optionalText(Map<String, Object> data, String key) {
        if (!data.containsKey(key) || data.get(key) == null) return "";
        return requiredText(data, key);
    }

    private static String requiredText(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            throw new IllegalArgumentException("Missing station field: " + key);
        }
        return ((String) value).trim();
    }
}
