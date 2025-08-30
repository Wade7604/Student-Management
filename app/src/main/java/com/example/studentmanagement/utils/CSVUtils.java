package com.example.studentmanagement.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.studentmanagement.models.Student;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CSVUtils {
    private static final String TAG = "CSVUtils";
    private static final String CSV_HEADER = "ID,Name,Email,Phone,Class,Date of Birth,Address";
    private static final String CSV_DELIMITER = ",";

    // Method to export students to CSV
    public static void exportStudentsToCSV(Context context, List<Student> students, Uri fileUri,
                                           OnCSVOperationListener listener) {
        // Use a background thread for file operations
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean success = false;
            try {
                OutputStream outputStream = context.getContentResolver().openOutputStream(fileUri);
                if (outputStream == null) {
                    handler.post(() -> {
                        if (listener != null) {
                            listener.onOperationComplete(false, "Could not open file for writing");
                        }
                    });
                    return;
                }

                // Write CSV header
                outputStream.write((CSV_HEADER + "\n").getBytes(StandardCharsets.UTF_8));

                // Write student data
                for (Student student : students) {
                    StringBuilder line = new StringBuilder();
                    // Escape fields that might contain commas
                    line.append(escapeField(student.getStudentId())).append(CSV_DELIMITER);
                    line.append(escapeField(student.getName())).append(CSV_DELIMITER);
                    line.append(escapeField(student.getEmail())).append(CSV_DELIMITER);
                    line.append(escapeField(student.getPhoneNumber())).append(CSV_DELIMITER);
                    line.append(escapeField(student.getClassName())).append(CSV_DELIMITER);
                    line.append(escapeField(student.getDateOfBirth())).append(CSV_DELIMITER);
                    line.append(escapeField(student.getAddress()));
                    line.append("\n");

                    outputStream.write(line.toString().getBytes(StandardCharsets.UTF_8));
                }

                outputStream.close();
                success = true;
            } catch (IOException e) {
                Log.e(TAG, "Error exporting students to CSV", e);
                success = false;
            }

            // Return result on main thread
            final boolean result = success;
            handler.post(() -> {
                if (listener != null) {
                    listener.onOperationComplete(result, result ?
                            students.size() + " students exported successfully" :
                            "Failed to export students");
                }
            });
        });
    }

    // Method to import students from CSV
    public static void importStudentsFromCSV(Context context, Uri fileUri, OnCSVOperationListener listener) {
        // Use a background thread for file operations
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<Student> importedStudents = new ArrayList<>();
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                if (inputStream == null) {
                    handler.post(() -> {
                        if (listener != null) {
                            listener.onOperationComplete(false, "Could not open file for reading");
                        }
                    });
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    // Skip header line
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    // Parse CSV line to student object
                    Student student = parseCSVLineToStudent(line);
                    if (student != null) {
                        importedStudents.add(student);
                    }
                }

                reader.close();
                inputStream.close();

                // Now save the imported students using batch operations
                if (!importedStudents.isEmpty()) {
                    handler.post(() -> {
                        // Use BatchOperationsUtil to save students in batches
                        BatchOperationsUtil.batchImportStudents(importedStudents,
                                new BatchOperationsUtil.BatchOperationListener() {
                                    @Override
                                    public void onBatchOperationProgress(int completedBatches, int totalBatches) {
                                        if (listener != null) {
                                            listener.onProgressUpdate(completedBatches, totalBatches);
                                        }
                                    }

                                    @Override
                                    public void onBatchOperationComplete(int totalSuccessCount) {
                                        if (listener != null) {
                                            listener.onOperationComplete(true,
                                                    totalSuccessCount + " students imported successfully");
                                        }
                                    }
                                });
                    });
                } else {
                    handler.post(() -> {
                        if (listener != null) {
                            listener.onOperationComplete(false,
                                    "No valid student records found in the CSV file");
                        }
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Error importing students from CSV", e);
                handler.post(() -> {
                    if (listener != null) {
                        listener.onOperationComplete(false, "Error reading the CSV file: " + e.getMessage());
                    }
                });
            }
        });
    }

    // Helper method to parse a CSV line to a Student object
    private static Student parseCSVLineToStudent(String line) {
        try {
            // Handle quoted fields that may contain commas
            List<String> fields = parseCSVLine(line);
            if (fields.size() < 7) {
                return null;
            }

            Student student = new Student();
            student.setStudentId(fields.get(0));
            student.setName(fields.get(1));
            student.setEmail(fields.get(2));
            student.setPhoneNumber(fields.get(3));
            student.setClassName(fields.get(4));
            student.setDateOfBirth(fields.get(5));
            student.setAddress(fields.get(6));

            return student;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing CSV line: " + line, e);
            return null;
        }
    }

    // Helper method to escape fields for CSV
    private static String escapeField(String field) {
        if (field == null) {
            return "";
        }

        // If the field contains quotes, commas, or newlines, wrap it in quotes and escape quotes
        if (field.contains("\"") || field.contains(",") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    // Helper to parse CSV line handling quoted fields
    private static List<String> parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        Log.d(TAG, "Reading line: " + line);

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\"') {
                if (i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    // Escaped quote
                    field.append('\"');
                    i++; // Skip the next quote
                } else {
                    // Toggle in-quotes state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // End of field
                fields.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }

        // Add the last field
        fields.add(field.toString());
        return fields;
    }

    // Interface for CSV operation callbacks
    public interface OnCSVOperationListener {
        void onProgressUpdate(int current, int total);
        void onOperationComplete(boolean success, String message);
    }
}