package com.example.studentmanagement.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.studentmanagement.R;
import com.example.studentmanagement.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserDialog extends Dialog {

    private User user;
    private UserDialogListener listener;

    private TextView tvTitle;
    private TextInputLayout tilFullName, tilEmail, tilPassword, tilPhone, tilAge;
    private TextInputEditText etFullName, etEmail, etPassword, etPhone, etAge;
    private Spinner spinnerRole;
    private CheckBox checkBoxLocked;
    private Button btnSave, btnCancel;

    private FirebaseAuth mAuth;

    public interface UserDialogListener {
        void onUserAdded(User user);
        void onUserUpdated(User user);
        void onUserDeleted(User user);
    }

    public UserDialog(@NonNull Context context, User user, UserDialogListener listener) {
        super(context);
        this.user = user;
        this.listener = listener;
        this.mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_user);
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Initialize views
        initViews();

        // Set up spinner for roles
        setupRoleSpinner();

        // Set up dialog based on mode (add or edit)
        if (user == null) {
            // Add mode
            tvTitle.setText("Add New User");
            btnSave.setText("Add");
            tilPassword.setVisibility(View.VISIBLE); // Show password field in add mode

            // Default values
            checkBoxLocked.setChecked(false);
        } else {
            // Edit mode
            tvTitle.setText("Edit User");
            btnSave.setText("Update");
            tilPassword.setVisibility(View.GONE); // Hide password field in edit mode

            // Fill in existing data
            etFullName.setText(user.getFullName());
            etEmail.setText(user.getEmail());
            etPhone.setText(user.getPhoneNumber());
            etAge.setText(String.valueOf(user.getAge()));
            checkBoxLocked.setChecked(user.isLocked());

            // Select role in spinner
            selectRoleInSpinner(user.getRole());
        }

        // Set click listeners
        btnSave.setOnClickListener(v -> saveUser());
        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_dialog_title);
        tilFullName = findViewById(R.id.til_full_name);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password); // New password field
        tilPhone = findViewById(R.id.til_phone);
        tilAge = findViewById(R.id.til_age);

        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password); // New password field
        etPhone = findViewById(R.id.et_phone);
        etAge = findViewById(R.id.et_age);

        spinnerRole = findViewById(R.id.spinner_role);
        checkBoxLocked = findViewById(R.id.checkbox_locked);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
    }

    private void setupRoleSpinner() {
        String[] roles = {"manager", "employee"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }

    private void selectRoleInSpinner(String role) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerRole.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(role)) {
                spinnerRole.setSelection(i);
                break;
            }
        }
    }

    private void saveUser() {
        // Validate inputs
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword != null ? etPassword.getText().toString().trim() : "";
        String phone = etPhone.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();

        if (fullName.isEmpty()) {
            tilFullName.setError("Full name is required");
            return;
        }

        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            return;
        }

        // Validate password only in add mode
        if (user == null && password.isEmpty()) {
            tilPassword.setError("Password is required");
            return;
        }

        if (user == null && password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return;
        }

        int age = 0;
        if (!ageStr.isEmpty()) {
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                tilAge.setError("Please enter a valid age");
                return;
            }
        }

        String role = spinnerRole.getSelectedItem().toString();
        boolean isLocked = checkBoxLocked.isChecked();

        if (user == null) {
            // Create new user
            user = new User(fullName, email, age, phone, isLocked, role);

            // Create user authentication first
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        // After authentication is created, save to Firestore
                        String uid = authResult.getUser().getUid();
                        user.setId(uid);

                        // Add to Firestore users collection
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(uid)
                                .set(user.toMap())
                                .addOnSuccessListener(aVoid -> {
                                    if (listener != null) {
                                        listener.onUserAdded(user);
                                    }
                                    Toast.makeText(getContext(), "User added successfully", Toast.LENGTH_SHORT).show();
                                    dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Error adding user to database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("UserDialog", "Error adding user to database", e);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error creating authentication: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("UserDialog", "Error creating authentication", e);
                    });
        } else {
            // Update existing user
            user.setFullName(fullName);
            user.setEmail(email);
            user.setAge(age);
            user.setPhoneNumber(phone);
            user.setLocked(isLocked);
            user.setRole(role);

            // Update user in Firestore
            updateUserInFirestore(user);

            if (listener != null) {
                listener.onUserUpdated(user);
            }

            dismiss();
        }
    }

    private void updateUserInFirestore(User user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Check if user ID is null to prevent the crash
        if (user.getId() == null || user.getId().isEmpty()) {
            Toast.makeText(getContext(), "Error: User ID is missing", Toast.LENGTH_SHORT).show();
            Log.e("UserDialog", "Cannot update user with null ID");
            return;
        }

        // Update user information in Firestore (users collection)
        db.collection("users").document(user.getId())
                .set(user.toMap())
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "User updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error updating user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("UserDialog", "Error updating user", e);
                });
    }
}