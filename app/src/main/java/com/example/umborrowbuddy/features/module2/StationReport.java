package com.example.umborrowbuddy.features.module2;

import java.util.Date;
import java.util.Map;

final class StationReport {
    enum Status { OPEN, IN_PROGRESS, RESOLVED, CLOSED }
    final String id;
    final String description;
    final Status status;
    final Date createdAt;

    private StationReport(String id, String description, Status status, Date createdAt) {
        this.id = id;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    boolean isOpen() { return status == Status.OPEN || status == Status.IN_PROGRESS; }

    static StationReport read(String id, String stationId, Map<String, Object> data) {
        Object description = data.get("description");
        Object status = data.get("status");
        if (!stationId.equals(data.get("stationId")) || !(description instanceof String)
                || ((String) description).trim().isEmpty() || !(status instanceof String)) {
            throw new IllegalArgumentException("Invalid report summary.");
        }
        return new StationReport(id, ((String) description).trim(), Status.valueOf((String) status),
                StationRepository.optionalDate(data, "createdAt"));
    }
}
