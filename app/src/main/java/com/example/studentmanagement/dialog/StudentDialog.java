package com.example.studentmanagement.dialog;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.studentmanagement.R;
import com.example.studentmanagement.models.Student;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class StudentDialog {
    private AlertDialog dialog;
    private EditText etName, etStudentId, etEmail, etPhone, etClass, etAddress;
    private TextView tvDateOfBirth;
    private Button btnSave, btnCancel, btnDelete;

    private FirebaseFirestore db;
    private Student student;
    private StudentDialogListener listener;
    private Context context;

    public interface StudentDialogListener {
        void onStudentAdded(Student student);
        void onStudentUpdated(Student student);
        void onStudentDeleted(Student student);
    }

    public StudentDialog(Context context, Student student, StudentDialogListener listener) {
        this.context = context;
        this.student = student;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();

        init();
    }

    private void init() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_student, null);

        // Initialize views
        etName = view.findViewById(R.id.et_name);
        etStudentId = view.findViewById(R.id.et_student_id);
        etEmail = view.findViewById(R.id.et_email);
        etPhone = view.findViewById(R.id.et_phone);
        etClass = view.findViewById(R.id.et_class);
        tvDateOfBirth = view.findViewById(R.id.tv_date_of_birth);
        etAddress = view.findViewById(R.id.et_address);

        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnDelete = view.findViewById(R.id.btn_delete);

        // Set up date picker
        tvDateOfBirth.setOnClickListener(v -> showDatePickerDialog());

        // Fill data if student exists (Edit mode)
        if (student != null && student.getId() != null) {
            etName.setText(student.getName());
            etStudentId.setText(student.getStudentId());
            etEmail.setText(student.getEmail());
            etPhone.setText(student.getPhoneNumber());
            etClass.setText(student.getClassName());
            tvDateOfBirth.setText(student.getDateOfBirth());
            etAddress.setText(student.getAddress());

            // Show delete button in edit mode
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            // Create a new student object for add mode
            student = new Student();

            // Hide delete button in add mode
            btnDelete.setVisibility(View.GONE);
        }

        // Set button click listeners
        btnSave.setOnClickListener(v -> saveStudent());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> deleteStudent());

        // Build dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        builder.setCancelable(false);

        dialog = builder.create();
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Parse existing date if available
        if (tvDateOfBirth.getText() != null && !tvDateOfBirth.getText().toString().isEmpty()) {
            try {
                String[] dateParts = tvDateOfBirth.getText().toString().split("/");
                if (dateParts.length == 3) {
                    day = Integer.parseInt(dateParts[0]);
                    month = Integer.parseInt(dateParts[1]) - 1; // Month is 0-based in Calendar
                    year = Integer.parseInt(dateParts[2]);
                }
            } catch (Exception e) {
                // Use current date if parsing fails
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Format date as DD/MM/YYYY
                    String dateString = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    tvDateOfBirth.setText(dateString);
                },
                year, month, day);

        datePickerDialog.show();
    }

    private void saveStudent() {
        // Validate input
        if (!validateInput()) {
            return;
        }

        // Update student object with form data
        student.setName(etName.getText().toString().trim());
        student.setStudentId(etStudentId.getText().toString().trim());
        student.setEmail(etEmail.getText().toString().trim());
        student.setPhoneNumber(etPhone.getText().toString().trim());
        student.setClassName(etClass.getText().toString().trim());
        student.setDateOfBirth(tvDateOfBirth.getText().toString().trim());
        student.setAddress(etAddress.getText().toString().trim());

        // Prepare data for Firestore
        Map<String, Object> studentData = new HashMap<>();
        studentData.put("name", student.getName());
        studentData.put("studentId", student.getStudentId());
        studentData.put("email", student.getEmail());
        studentData.put("phoneNumber", student.getPhoneNumber());
        studentData.put("className", student.getClassName());
        studentData.put("dateOfBirth", student.getDateOfBirth());
        studentData.put("address", student.getAddress());

        if (student.getId() != null) {
            // Update existing student
            db.collection("students").document(student.getId())
                    .update(studentData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Student updated successfully", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onStudentUpdated(student);
                        }
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error updating student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Add new student
            db.collection("students")
                    .add(studentData)
                    .addOnSuccessListener(documentReference -> {
                        student.setId(documentReference.getId());
                        Toast.makeText(context, "Student added successfully", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onStudentAdded(student);
                        }
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error adding student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private boolean validateInput() {
        // Check name
        if (etName.getText().toString().trim().isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return false;
        }

        // Check student ID
        if (etStudentId.getText().toString().trim().isEmpty()) {
            etStudentId.setError("Student ID is required");
            etStudentId.requestFocus();
            return false;
        }

        // Other validations can be added as needed

        return true;
    }

    private void deleteStudent() {
        // Show confirmation dialog
        new AlertDialog.Builder(context)
                .setTitle("Delete Student")
                .setMessage("Are you sure you want to delete this student? This will also delete all their certificates.")
                .setPositiveButton("Delete", (dialogInterface, i) -> {
                    // Delete student from Firestore
                    if (student.getId() != null) {
                        db.collection("students").document(student.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Also delete related certificates
                                    deleteRelatedCertificates();

                                    Toast.makeText(context, "Student deleted successfully", Toast.LENGTH_SHORT).show();
                                    if (listener != null) {
                                        listener.onStudentDeleted(student);
                                    }
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Error deleting student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRelatedCertificates() {
        // Delete all certificates associated with this student
        db.collection("certificates")
                .whereEqualTo("studentId", student.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        db.collection("certificates").document(doc.getId()).delete();
                    }
                });
    }

    public void show() {
        dialog.show();
    }
}