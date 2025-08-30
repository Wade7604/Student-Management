package com.example.studentmanagement.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.studentmanagement.R;
import com.example.studentmanagement.activity.StudentDetailActivity;
import com.example.studentmanagement.adapters.StudentAdapter;
import com.example.studentmanagement.dialog.ImportExportDialog;
import com.example.studentmanagement.dialog.StudentDialog;
import com.example.studentmanagement.models.Student;
import com.example.studentmanagement.utils.CSVUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.content.Intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudentFragment extends Fragment implements StudentAdapter.OnStudentClickListener {

    private RecyclerView recyclerViewStudents;
    private FloatingActionButton fabAddStudent;
    private SwipeRefreshLayout swipeRefreshLayout;
    private StudentAdapter studentAdapter;
    private List<Student> studentList;
    private List<Student> filteredStudentList;
    private EditText searchEditText;
    private Spinner sortSpinner;

    private FirebaseFirestore db;
    private String userRole = "employee"; // Default role

    // Register ActivityResultLaunchers at the fragment level
    private ActivityResultLauncher<Intent> importFileLauncher;
    private ActivityResultLauncher<Intent> exportFileLauncher;

    public StudentFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable options menu
        setHasOptionsMenu(true);

        // Get user role from arguments
        if (getArguments() != null) {
            userRole = getArguments().getString("USER_ROLE", "employee");
        }

        // Initialize launchers here, early in the fragment lifecycle
        registerActivityResultLaunchers();
    }

    private void registerActivityResultLaunchers() {
        // Initialize the import file launcher
        importFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedFile = result.getData().getData();
                        if (selectedFile != null) {
                            handleImportCSV(selectedFile);
                        }
                    }
                });

        // Initialize the export file launcher
        exportFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            handleExportCSV(fileUri);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_student, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        initViews(view);

        // Set up RecyclerView
        studentList = new ArrayList<>();
        filteredStudentList = new ArrayList<>();
        studentAdapter = new StudentAdapter(filteredStudentList, this);
        recyclerViewStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewStudents.setAdapter(studentAdapter);

        // Load students
        loadStudents();

        // Setup FAB click listener (with permission check)
        fabAddStudent.setOnClickListener(v -> {
            if (canAddStudent()) {
                showAddStudentDialog();
            } else {
                Toast.makeText(getContext(), "You don't have permission to add students", Toast.LENGTH_SHORT).show();
            }
        });

        // Show/hide FAB based on permissions
        updateFabVisibility();

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshStudentList);

        // Setup search functionality
        setupSearchFunctionality();

        // Setup sort functionality
        setupSortFunctionality();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_student_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_import_export) {
            if (canImportExport()) {
                showImportExportDialog();
            } else {
                Toast.makeText(getContext(), "You don't have permission for import/export operations", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews(View view) {
        recyclerViewStudents = view.findViewById(R.id.recycler_view_students);
        fabAddStudent = view.findViewById(R.id.fab_add_student);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        searchEditText = view.findViewById(R.id.search_edit_text);
        sortSpinner = view.findViewById(R.id.sort_spinner);
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    private void setupSortFunctionality() {
        // Create spinner adapter with sort options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.sort_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        // Handle sort selection
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortStudents(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Not needed
            }
        });
    }

    private void loadStudents() {
        swipeRefreshLayout.setRefreshing(true);
        studentList.clear();
        filteredStudentList.clear();

        db.collection("students")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Student student = document.toObject(Student.class);
                        student.setId(document.getId());
                        studentList.add(student);
                    }
                    // Apply current sort and filter
                    applyCurrentSortAndFilter();
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void filterStudents(String query) {
        filteredStudentList.clear();

        if (query.isEmpty()) {
            filteredStudentList.addAll(studentList);
        } else {
            String lowerCaseQuery = query.toLowerCase();

            for (Student student : studentList) {
                if (student.getName().toLowerCase().contains(lowerCaseQuery) ||
                        student.getStudentId().toLowerCase().contains(lowerCaseQuery) ||
                        (student.getEmail() != null && student.getEmail().toLowerCase().contains(lowerCaseQuery)) ||
                        (student.getClassName() != null && student.getClassName().toLowerCase().contains(lowerCaseQuery))) {
                    filteredStudentList.add(student);
                }
            }
        }

        // Apply current sort to filtered results
        int sortPosition = sortSpinner.getSelectedItemPosition();
        if (sortPosition > 0) { // 0 is usually "Default" or "None"
            sortStudentList(sortPosition);
        }

        studentAdapter.notifyDataSetChanged();
    }

    private void sortStudents(int sortOption) {
        // Apply sorting to the already filtered list
        sortStudentList(sortOption);
        studentAdapter.notifyDataSetChanged();
    }

    private void sortStudentList(int sortOption) {
        switch (sortOption) {
            case 0: // Default - No sort
                // Do nothing, keep original order
                break;
            case 1: // Name (A-Z)
                Collections.sort(filteredStudentList, (s1, s2) ->
                        s1.getName().compareToIgnoreCase(s2.getName()));
                break;
            case 2: // Name (Z-A)
                Collections.sort(filteredStudentList, (s1, s2) ->
                        s2.getName().compareToIgnoreCase(s1.getName()));
                break;
            case 3: // Student ID (ascending)
                Collections.sort(filteredStudentList, (s1, s2) ->
                        s1.getStudentId().compareToIgnoreCase(s2.getStudentId()));
                break;
            case 4: // Student ID (descending)
                Collections.sort(filteredStudentList, (s1, s2) ->
                        s2.getStudentId().compareToIgnoreCase(s1.getStudentId()));
                break;
            case 5: // Class name
                Collections.sort(filteredStudentList, (s1, s2) -> {
                    if (s1.getClassName() == null) return -1;
                    if (s2.getClassName() == null) return 1;
                    return s1.getClassName().compareToIgnoreCase(s2.getClassName());
                });
                break;
        }
    }

    private void applyCurrentSortAndFilter() {
        // First apply filter
        String currentQuery = searchEditText.getText().toString();
        filteredStudentList.clear();

        if (currentQuery.isEmpty()) {
            filteredStudentList.addAll(studentList);
        } else {
            filterStudents(currentQuery);
            return; // filterStudents already handles sorting
        }

        // Then apply sort
        int sortPosition = sortSpinner.getSelectedItemPosition();
        if (sortPosition > 0) {
            sortStudentList(sortPosition);
        }

        studentAdapter.notifyDataSetChanged();
    }

    private void refreshStudentList() {
        loadStudents();
    }

    private void showAddStudentDialog() {
        StudentDialog dialog = new StudentDialog(getContext(), null, new StudentDialog.StudentDialogListener() {
            @Override
            public void onStudentAdded(Student student) {
                // Student is already added to Firestore by the dialog
                // Update our local list
                studentList.add(student);
                applyCurrentSortAndFilter();
            }

            @Override
            public void onStudentUpdated(Student student) {
                // Not applicable for Add operation
            }

            @Override
            public void onStudentDeleted(Student student) {
                // Not applicable for Add operation
            }
        });
        dialog.show();
    }

    private void showEditStudentDialog(Student student, int position) {
        StudentDialog dialog = new StudentDialog(getContext(), student, new StudentDialog.StudentDialogListener() {
            @Override
            public void onStudentAdded(Student student) {
                // Not applicable for Edit operation
            }

            @Override
            public void onStudentUpdated(Student updatedStudent) {
                // Find and update in main list
                for (int i = 0; i < studentList.size(); i++) {
                    if (studentList.get(i).getId().equals(updatedStudent.getId())) {
                        studentList.set(i, updatedStudent);
                        break;
                    }
                }

                // Update filtered list and adapter
                filteredStudentList.set(position, updatedStudent);
                studentAdapter.notifyItemChanged(position);
            }

            @Override
            public void onStudentDeleted(Student student) {
                // Not applicable for Edit operation
            }
        });
        dialog.show();
    }

    private void showImportExportDialog() {
        ImportExportDialog dialog = new ImportExportDialog(
                requireContext(),
                studentList,
                new ImportExportDialog.ImportExportListener() {
                    @Override
                    public void onImportCompleted() {
                        // Refresh the student list after import
                        loadStudents();
                    }

                    @Override
                    public void onExportCompleted() {
                        // Nothing special needed after export
                        Toast.makeText(getContext(), "Export completed successfully", Toast.LENGTH_SHORT).show();
                    }
                },
                importFileLauncher,
                exportFileLauncher);
        dialog.show();
    }

    // These handlers should be in the fragment since they're used with the fragment's launchers
    private void handleImportCSV(Uri fileUri) {
        CSVUtils.importStudentsFromCSV(requireContext(), fileUri, new CSVUtils.OnCSVOperationListener() {
            @Override
            public void onProgressUpdate(int current, int total) {
                // Progress updates will be handled by the dialog
            }

            @Override
            public void onOperationComplete(boolean success, String message) {
                if (success) {
                    loadStudents();
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleExportCSV(Uri fileUri) {
        CSVUtils.exportStudentsToCSV(requireContext(), studentList, fileUri, new CSVUtils.OnCSVOperationListener() {
            @Override
            public void onProgressUpdate(int current, int total) {
                // Export doesn't provide progress updates in the current implementation
            }

            @Override
            public void onOperationComplete(boolean success, String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteStudent(Student student, int position) {
        // First, delete all certificates associated with this student
        db.collection("certificates")
                .whereEqualTo("studentId", student.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Create a batch to delete all certificates
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Create a batch operation for efficient deletion
                        com.google.firebase.firestore.WriteBatch batch = db.batch();

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            batch.delete(document.getReference());
                        }

                        // Commit the batch
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("StudentFragment", "All certificates deleted for student: " + student.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("StudentFragment", "Error deleting certificates: " + e.getMessage());
                                });
                    }

                    // Now delete the student
                    deleteStudentDocument(student, position);
                })
                .addOnFailureListener(e -> {
                    Log.e("StudentFragment", "Error querying certificates: " + e.getMessage());
                    // Still try to delete the student even if certificate query fails
                    deleteStudentDocument(student, position);
                });
    }

    // Extracted method to delete the student document
    private void deleteStudentDocument(Student student, int position) {
        db.collection("students").document(student.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove from main list
                    for (int i = 0; i < studentList.size(); i++) {
                        if (studentList.get(i).getId().equals(student.getId())) {
                            studentList.remove(i);
                            break;
                        }
                    }

                    // Remove from filtered list and update adapter
                    filteredStudentList.remove(position);
                    studentAdapter.notifyItemRemoved(position);
                    Toast.makeText(getContext(), "Student deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error deleting student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onStudentClick(Student student, int position) {
        // Navigate to student details activity
        Intent intent = new Intent(getContext(), StudentDetailActivity.class);
        intent.putExtra("STUDENT_ID", student.getId());
        intent.putExtra("USER_ROLE", userRole); // Pass user role to the detail activity
        startActivity(intent);
    }

    @Override
    public void onEditClick(Student student, int position) {
        if (canEditStudent()) {
            showEditStudentDialog(student, position);
        } else {
            Toast.makeText(getContext(), "You don't have permission to edit students", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(Student student, int position) {
        if (canDeleteStudent()) {
            deleteStudent(student, position);
        } else {
            Toast.makeText(getContext(), "You don't have permission to delete students", Toast.LENGTH_SHORT).show();
        }
    }

    // Permission methods
    private boolean canAddStudent() {
        return "admin".equals(userRole) || "manager".equals(userRole);
    }

    private boolean canEditStudent() {
        return "admin".equals(userRole) || "manager".equals(userRole);
    }

    private boolean canDeleteStudent() {
        return "admin".equals(userRole) || "manager".equals(userRole);
    }

    private boolean canImportExport() {
        return "admin".equals(userRole) || "manager".equals(userRole);
    }

    private void updateFabVisibility() {
        if (canAddStudent()) {
            fabAddStudent.show();
        } else {
            fabAddStudent.hide();
        }
    }
}