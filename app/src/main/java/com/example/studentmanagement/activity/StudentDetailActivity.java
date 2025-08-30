package com.example.studentmanagement.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.studentmanagement.R;
import com.example.studentmanagement.activity.CertificateActivity;
import com.example.studentmanagement.dialog.StudentDialog;
import com.example.studentmanagement.models.Student;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class StudentDetailActivity extends AppCompatActivity {

    private TextView tvName, tvStudentId, tvEmail, tvPhone, tvClass, tvDateOfBirth, tvAddress;
    private Button btnViewCertificates;
    private FloatingActionButton fabEditStudent;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private String studentId;
    private Student currentStudent;
    private String userRole = "employee";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_detail);
        String roleFromIntent = getIntent().getStringExtra("USER_ROLE");
        if (roleFromIntent != null) {
            userRole = roleFromIntent;
        }

        // Show/hide edit button based on permission
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Student Details");
        }

        // Get student ID from intent
        studentId = getIntent().getStringExtra("STUDENT_ID");
        if (studentId == null) {
            Toast.makeText(this, "Error: No student ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();
        updateUIBasedOnPermissions();

        // Load student data
        loadStudentData();

        // Setup view certificates button click listener
        btnViewCertificates.setOnClickListener(v -> openCertificateActivity());

        // Setup edit student FAB click listener
        fabEditStudent.setOnClickListener(v -> showEditStudentDialog());

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
    }

    private void initViews() {
        tvName = findViewById(R.id.tv_student_name);
        tvStudentId = findViewById(R.id.tv_student_id);
        tvEmail = findViewById(R.id.tv_student_email);
        tvPhone = findViewById(R.id.tv_student_phone);
        tvClass = findViewById(R.id.tv_student_class);
        tvDateOfBirth = findViewById(R.id.tv_student_dob);
        tvAddress = findViewById(R.id.tv_student_address);

        btnViewCertificates = findViewById(R.id.btn_view_certificates);
        fabEditStudent = findViewById(R.id.fab_edit_student);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
    }

    private void loadStudentData() {
        swipeRefreshLayout.setRefreshing(true);

        db.collection("students").document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentStudent = documentSnapshot.toObject(Student.class);
                        if (currentStudent != null) {
                            currentStudent.setId(documentSnapshot.getId());
                            displayStudentInfo(currentStudent);
                        }
                    } else {
                        Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading student data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                    finish();
                });
    }

    private void displayStudentInfo(Student student) {
        tvName.setText(student.getName());
        tvStudentId.setText(student.getStudentId());
        tvEmail.setText(student.getEmail() != null ? student.getEmail() : "N/A");
        tvPhone.setText(student.getPhoneNumber() != null ? student.getPhoneNumber() : "N/A");
        tvClass.setText(student.getClassName() != null ? student.getClassName() : "N/A");
        tvDateOfBirth.setText(student.getDateOfBirth() != null ? student.getDateOfBirth() : "N/A");
        tvAddress.setText(student.getAddress() != null ? student.getAddress() : "N/A");

        // Update toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(student.getName());
        }
    }

    private void refreshData() {
        loadStudentData();
    }
    private void updateUIBasedOnPermissions() {
        // Control visibility of the Edit FAB based on role
        if ("admin".equals(userRole) || "manager".equals(userRole)) {
            fabEditStudent.setVisibility(View.VISIBLE);
        } else {
            fabEditStudent.setVisibility(View.GONE);
        }

        // Control visibility of the View Certificates button
        if ("admin".equals(userRole) || "manager".equals(userRole) || "employee".equals(userRole)) {
            btnViewCertificates.setVisibility(View.VISIBLE);
        } else {
            btnViewCertificates.setVisibility(View.GONE);
        }
    }

    private void openCertificateActivity() {
        // Create intent for CertificateActivity with student ID
        Intent intent = new Intent(this, CertificateActivity.class);
        intent.putExtra("STUDENT_ID", studentId);
        intent.putExtra("USER_ROLE", userRole);
        // Start the activity with transition animation
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void showEditStudentDialog() {
        StudentDialog dialog = new StudentDialog(this, currentStudent, new StudentDialog.StudentDialogListener() {
            @Override
            public void onStudentAdded(Student student) {
                // Not applicable for Edit operation
            }

            @Override
            public void onStudentUpdated(Student updatedStudent) {
                // Student is already updated in Firestore by the dialog
                currentStudent = updatedStudent;
                displayStudentInfo(currentStudent);
            }

            @Override
            public void onStudentDeleted(Student student) {
                // Handle deletion - maybe finish activity
                Toast.makeText(StudentDetailActivity.this, "Student deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}