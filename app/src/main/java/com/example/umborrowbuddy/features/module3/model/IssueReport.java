package com.example.umborrowbuddy.features.module3.model; // 请根据你实际的包名修改

import com.google.firebase.Timestamp;

public class IssueReport {
    private String issueId;
    private String dispenserId;
    private String userId;
    private String userEmail;
    private String description;
    private Timestamp timestamp;
    private String status; // 工单状态，例如：PENDING

    public IssueReport() {} // Firestore 需要无参构造函数

    public IssueReport(String issueId, String dispenserId, String userId, String userEmail, String description, Timestamp timestamp, String status) {
        this.issueId = issueId;
        this.dispenserId = dispenserId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.description = description;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getIssueId() { return issueId; }
    public String getDispenserId() { return dispenserId; }
    public String getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
    public String getDescription() { return description; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
}