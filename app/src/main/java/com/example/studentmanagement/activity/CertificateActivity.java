package com.example.studentmanagement.activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.studentmanagement.R;
import com.example.studentmanagement.adapters.CertificateAdapter;
import com.example.studentmanagement.dialog.CertificateDialog;
import com.example.studentmanagement.dialog.CertificateImportExportDialog;
import com.example.studentmanagement.models.Certificate;
import com.example.studentmanagement.models.Student;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CertificateActivity extends AppCompatActivity implements CertificateAdapter.OnCertificateClickListener {

    private RecyclerView recyclerViewCertificates;
    private FloatingActionButton fabAddCertificate;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvStudentName;
    private CertificateAdapter certificateAdapter;
    private List<Certificate> certificateList;

    private FirebaseFirestore db;
    private String studentId;
    private Student currentStudent;

    // Activity result launchers for file operations
    private ActivityResultLauncher<Intent> importFileLauncher;
    private ActivityResultLauncher<Intent> exportFileLauncher;

    // Reference to the import/export dialog
    private CertificateImportExportDialog importExportDialog;
    private String userRole = "employee";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate);

        // Get student ID from intent - FIXED: using consistent key "STUDENT_ID"
        studentId = getIntent().getStringExtra("STUDENT_ID");
        String roleFromIntent = getIntent().getStringExtra("USER_ROLE");
        userRole = roleFromIntent != null ? roleFromIntent : "employee"; // Update userRole with fallback
        // Setup toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Certificates");
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        initViews();

        // Register activity result launchers for file operations
        registerActivityResultLaunchers();
        updateUIBasedOnPermissions();

        // Set up RecyclerView
        certificateList = new ArrayList<>();
        certificateAdapter = new CertificateAdapter(certificateList, this);
        certificateAdapter.setUserRole(userRole);

        recyclerViewCertificates.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCertificates.setAdapter(certificateAdapter);

        // Load student info
        if (studentId != null) {
            loadStudentInfo();
            // Load certificates for this student only
            loadCertificatesForStudent();
        } else {
            Toast.makeText(this, "No student ID provided", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no student ID
        }

        // Setup FAB click listener
        fabAddCertificate.setOnClickListener(v -> showAddCertificateDialog());

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshCertificateList);
    }

    private void initViews() {
        recyclerViewCertificates = findViewById(R.id.recycler_view_certificates);
        fabAddCertificate = findViewById(R.id.fab_add_certificate);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        tvStudentName = findViewById(R.id.tv_student_name);
    }
    private void updateUIBasedOnPermissions() {
        // Control visibility of the Add Certificate FAB based on role
        if ("admin".equals(userRole) || "manager".equals(userRole)) {
            fabAddCertificate.setVisibility(View.VISIBLE);
        } else {
            fabAddCertificate.setVisibility(View.GONE);
        }
    }
    private void registerActivityResultLaunchers() {
        // Register launcher for importing files
        importFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null && importExportDialog != null) {
                            importExportDialog.handleImportFile(uri);
                        }
                    }
                }
        );

        // Register launcher for exporting files
        exportFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null && importExportDialog != null) {
                            importExportDialog.handleExportFile(uri);
                        }
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_certificate, menu);
        MenuItem importExportItem = menu.findItem(R.id.action_import_export);
        if (importExportItem != null) {
            importExportItem.setVisible("admin".equals(userRole) || "manager".equals(userRole));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_import_export) {
            showImportExportDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showImportExportDialog() {
        importExportDialog = new CertificateImportExportDialog(
                this,
                certificateList,
                studentId,
                new CertificateImportExportDialog.ImportExportListener() {
                    @Override
                    public void onImportCompleted() {
                        // Refresh the list after import
                        refreshCertificateList();
                    }

                    @Override
                    public void onExportCompleted() {
                        // No need to refresh after export
                        Toast.makeText(CertificateActivity.this, "Export completed", Toast.LENGTH_SHORT).show();
                    }
                },
                importFileLauncher,
                exportFileLauncher
        );
        importExportDialog.show();
    }

    private void loadStudentInfo() {
        db.collection("students").document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentStudent = documentSnapshot.toObject(Student.class);
                        if (currentStudent != null) {
                            currentStudent.setId(documentSnapshot.getId());
                            tvStudentName.setText(currentStudent.getName());
                        }
                    } else {
                        Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading student data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadCertificatesForStudent() {
        swipeRefreshLayout.setRefreshing(true);
        certificateList.clear();

        Log.d("CertificateActivity", "Loading certificates for student: " + studentId);

        db.collection("certificates")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("CertificateActivity", "Found " + queryDocumentSnapshots.size() + " certificates");
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Certificate certificate = document.toObject(Certificate.class);
                        certificate.setId(document.getId());
                        certificateList.add(certificate);
                    }
                    certificateAdapter.notifyDataSetChanged();
                    Log.d("CertificateActivity", "Final list size: " + certificateList.size());
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("CertificateActivity", "Error loading certificates: " + e.getMessage());
                    Toast.makeText(this, "Error loading certificates: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void refreshCertificateList() {
        loadCertificatesForStudent();
    }

    private void showAddCertificateDialog() {
        CertificateDialog dialog = new CertificateDialog(this, null, new CertificateDialog.CertificateDialogListener() {
            @Override
            public void onCertificateAdded(Certificate certificate) {
                // Certificate is already added to Firestore by the dialog
                // Just update our local list
                if ("employee".equals(userRole)) {
                    return;
                }
                certificateList.add(certificate);
                certificateAdapter.notifyItemInserted(certificateList.size() - 1);
            }

            @Override
            public void onCertificateUpdated(Certificate certificate) {
                // Not applicable for Add operation
            }

            @Override
            public void onCertificateDeleted(Certificate certificate) {
                // Not applicable for Add operation
            }
        });
        dialog.show();
    }

    private void showEditCertificateDialog(Certificate certificate, int position) {
        CertificateDialog dialog = new CertificateDialog(this, certificate, new CertificateDialog.CertificateDialogListener() {
            @Override
            public void onCertificateAdded(Certificate certificate) {
                // Not applicable for Edit operation
            }

            @Override
            public void onCertificateUpdated(Certificate updatedCertificate) {
                // Certificate is already updated in Firestore by the dialog
                // Just update our local list
                certificateList.set(position, updatedCertificate);
                certificateAdapter.notifyItemChanged(position);
            }

            @Override
            public void onCertificateDeleted(Certificate certificate) {
                // Not applicable for Edit operation
            }
        });
        dialog.show();
    }

    private void deleteCertificate(Certificate certificate, int position) {
        db.collection("certificates").document(certificate.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    certificateList.remove(position);
                    certificateAdapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Certificate deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting certificate: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onCertificateClick(Certificate certificate, int position) {
        Log.d("CertificateActivity", "Certificate clicked: " + (certificate != null ? certificate.getCertificateName() : "null") + ", position: " + position + ", userRole: " + userRole);
        if ("employee".equals(userRole)) {
            // For employees, just show details in a read-only mode
            showCertificateDetailsDialog(certificate);
        } else {
            // For admin and manager, show the edit dialog
            showEditCertificateDialog(certificate, position);
        }
    }
    private void showCertificateDetailsDialog(Certificate certificate) {
        // Create a new method to show certificate details in read-only mode
        CertificateDialog dialog = new CertificateDialog(this, certificate, null);
        dialog.setReadOnlyMode(true); // You'll need to add this method to CertificateDialog
        dialog.show();
    }
    @Override
    public void onDeleteClick(Certificate certificate, int position) {
        deleteCertificate(certificate, position);
    }
}