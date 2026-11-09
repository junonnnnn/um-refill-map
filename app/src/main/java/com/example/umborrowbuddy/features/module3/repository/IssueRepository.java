package com.example.umborrowbuddy.features.module3.repository; // 请根据你实际的包名修改

import com.example.umborrowbuddy.features.module3.model.IssueReport;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

public class IssueRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<Void> submitIssue(IssueReport report) {
        // 将工单存入 Firestore 的 "issues" 集合中
        return db.collection("issues")
                .document(report.getIssueId())
                .set(report);
    }
}