package com.example.studentmanagement.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.studentmanagement.models.Certificate;
import com.example.studentmanagement.models.Student;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for handling batch operations such as bulk importing students
 */
public class BatchOperationsUtil {
    private static final String TAG = "BatchOperationsUtil";
    private static final int BATCH_SIZE = 500; // Firestore batch limit is 500 operations

    /**
     * Import a list of students in batches to avoid Firestore limitations
     * @param students List of students to import
     * @param listener Callback for import progress and completion
     */
    public static void batchImportStudents(List<Student> students, BatchOperationListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Process students in batches
        List<List<Student>> batches = splitIntoBatches(students, BATCH_SIZE);
        final int totalBatches = batches.size();
        final int[] completedBatches = {0};
        final int[] successCount = {0};

        // If empty list, call completion right away
        if (batches.isEmpty()) {
            if (listener != null) {
                listener.onBatchOperationProgress(0, 0);
                listener.onBatchOperationComplete(0);
            }
            return;
        }

        // Process each batch
        for (int i = 0; i < batches.size(); i++) {
            List<Student> batch = batches.get(i);
            WriteBatch writeBatch = db.batch();

            for (Student student : batch) {
                Map<String, Object> studentData = new HashMap<>();
                studentData.put("name", student.getName());
                studentData.put("studentId", student.getStudentId());
                studentData.put("email", student.getEmail());
                studentData.put("phoneNumber", student.getPhoneNumber());
                studentData.put("className", student.getClassName());
                studentData.put("dateOfBirth", student.getDateOfBirth());
                studentData.put("address", student.getAddress());

                // Create a new document with auto-generated ID
                writeBatch.set(db.collection("students").document(), studentData);
            }

            // Commit the batch
            final int batchIndex = i;
            writeBatch.commit()
                    .addOnSuccessListener(aVoid -> {
                        successCount[0] += batches.get(batchIndex).size();
                        completedBatches[0]++;

                        if (listener != null) {
                            listener.onBatchOperationProgress(completedBatches[0], totalBatches);
                        }

                        if (completedBatches[0] == totalBatches && listener != null) {
                            listener.onBatchOperationComplete(successCount[0]);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error importing batch " + batchIndex, e);
                        completedBatches[0]++;

                        if (listener != null) {
                            listener.onBatchOperationProgress(completedBatches[0], totalBatches);
                        }

                        if (completedBatches[0] == totalBatches && listener != null) {
                            listener.onBatchOperationComplete(successCount[0]);
                        }
                    });
        }
    }

    /**
     * Split a list into smaller batches
     * @param list The original list
     * @param batchSize Size of each batch
     * @return List of batches
     */
    private static <T> List<List<T>> splitIntoBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();

        for (int i = 0; i < list.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, list.size());
            batches.add(new ArrayList<>(list.subList(i, endIndex)));
        }

        return batches;
    }

    public static void batchImportCertificates(List<Certificate> certificates,
                                               BatchOperationListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        final int BATCH_SIZE = 20; // Firestore batch limit is 500, but smaller is safer

        executor.execute(() -> {
            try {
                int totalBatches = (int) Math.ceil((double) certificates.size() / BATCH_SIZE);
                int processedCount = 0;
                int successCount = 0;

                for (int batchNum = 0; batchNum < totalBatches; batchNum++) {
                    // Create a new batch
                    WriteBatch batch = db.batch();

                    // Get subset for this batch
                    int startIndex = batchNum * BATCH_SIZE;
                    int endIndex = Math.min((batchNum + 1) * BATCH_SIZE, certificates.size());
                    List<Certificate> batchItems = certificates.subList(startIndex, endIndex);

                    // Add each certificate to the batch
                    for (Certificate certificate : batchItems) {
                        // Generate a new document ID if needed
                        String docId = certificate.getId();
                        if (docId == null || docId.isEmpty()) {
                            docId = db.collection("certificates").document().getId();
                            certificate.setId(docId);
                        }

                        // Create document reference
                        DocumentReference docRef = db.collection("certificates").document(docId);

                        // Add to batch
                        batch.set(docRef, certificate);
                        processedCount++;
                    }

                    // Commit the batch
                    Tasks.await(batch.commit());
                    successCount += batchItems.size();

                    // Report progress
                    final int currentBatch = batchNum + 1;
                    final int currentSuccess = successCount;

                    handler.post(() -> {
                        if (listener != null) {
                            listener.onBatchOperationProgress(currentBatch, totalBatches);
                        }
                    });
                }

                // Report completion
                final int finalSuccessCount = successCount;
                handler.post(() -> {
                    if (listener != null) {
                        listener.onBatchOperationComplete(finalSuccessCount);
                    }
                });

            } catch (Exception e) {
                Log.e("BatchOperations", "Error during batch import: " + e.getMessage());
                handler.post(() -> {
                    if (listener != null) {
                        listener.onBatchOperationComplete(0);
                    }
                });
            }
        });
    }

    /**
     * Interface for batch operation callbacks
     */
    public interface BatchOperationListener {
        void onBatchOperationProgress(int completedBatches, int totalBatches);
        void onBatchOperationComplete(int totalSuccessCount);
    }
}