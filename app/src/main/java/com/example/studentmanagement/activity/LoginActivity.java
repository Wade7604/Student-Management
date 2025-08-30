package com.example.studentmanagement.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentmanagement.MainActivity;
import com.example.studentmanagement.R;
import com.example.studentmanagement.models.LoginHistory;
import com.example.studentmanagement.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);

        // Set click listener for login button
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private void attemptLogin() {
        // Reset errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        // Get input values
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate inputs
        boolean cancel = false;
        View focusView = null;

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_field_required));
            focusView = etPassword;
            cancel = true;
        }

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_field_required));
            focusView = etEmail;
            cancel = true;
        } else if (!isEmailValid(email)) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            focusView = etEmail;
            cancel = true;
        }

        if (cancel) {
            // There was an error; focus the first form field with an error
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            // Show progress spinner and attempt login
            showProgress(true);
            loginWithFirebase(email, password);
        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
    }

    private void loginWithFirebase(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String userId = mAuth.getCurrentUser().getUid();
                        String userEmail = mAuth.getCurrentUser().getEmail();

                        // Check if user exists in database and verify access
                        db.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        User user = documentSnapshot.toObject(User.class);
                                        if (user != null && user.isLocked()) {
                                            // Account is locked
                                            showProgress(false);
                                            mAuth.signOut();
                                            // Record failed login attempt due to locked account
                                            recordLoginHistory(false);
                                            Toast.makeText(LoginActivity.this,
                                                    "Your account has been locked. Please contact an administrator.",
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            // Login successful
                                            recordLoginHistory(true);
                                            showProgress(false);
                                            Toast.makeText(LoginActivity.this,
                                                    "Login successful", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                            finish();
                                        }
                                    } else {
                                        // User doesn't exist in Firestore, create a new user document
                                        User newUser = new User();
                                        newUser.setEmail(userEmail);

                                        // Special handling for admin@gmail.com - set as admin
                                        if ("admin@gmail.com".equals(userEmail)) {
                                            newUser.setFullName("Administrator");
                                            newUser.setRole("admin");
                                        } else {
                                            newUser.setFullName("User"); // Default name
                                            newUser.setRole("student"); // Default role
                                        }

                                        // Set default values
                                        newUser.setAge(0);
                                        newUser.setPhoneNumber("");
                                        newUser.setPhotoUrl("");
                                        newUser.setLocked(false);
                                        newUser.setId(userId);

                                        db.collection("users").document(userId)
                                                .set(newUser)
                                                .addOnSuccessListener(aVoid -> {
                                                    // User created successfully
                                                    recordLoginHistory(true);
                                                    showProgress(false);
                                                    Toast.makeText(LoginActivity.this,
                                                            "Account created and login successful", Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    // Failed to create user
                                                    showProgress(false);
                                                    recordLoginHistory(false);
                                                    mAuth.signOut();
                                                    Log.e(TAG, "Error creating user document", e);
                                                    Toast.makeText(LoginActivity.this,
                                                            "Error creating user profile. Please try again.",
                                                            Toast.LENGTH_LONG).show();
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    showProgress(false);
                                    Log.e(TAG, "Error checking user status", e);

                                    // Create user document on error
                                    User newUser = new User();
                                    newUser.setEmail(userEmail);

                                    // Special handling for admin
                                    if ("admin@gmail.com".equals(userEmail)) {
                                        newUser.setFullName("Administrator");
                                        newUser.setRole("admin");
                                    } else {
                                        newUser.setFullName("User");
                                        newUser.setRole("student");
                                    }

                                    newUser.setAge(0);
                                    newUser.setPhoneNumber("");
                                    newUser.setPhotoUrl("");
                                    newUser.setLocked(false);
                                    newUser.setId(userId);

                                    db.collection("users").document(userId)
                                            .set(newUser)
                                            .addOnSuccessListener(aVoid -> {
                                                // Make sure to record login history before proceeding
                                                recordLoginHistory(true);
                                                showProgress(false);
                                                Toast.makeText(LoginActivity.this,
                                                        "Login successful", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                finish();
                                            })
                                            .addOnFailureListener(ex -> {
                                                showProgress(false);
                                                recordLoginHistory(false);
                                                mAuth.signOut();
                                                Toast.makeText(LoginActivity.this,
                                                        "Error creating user profile: " + ex.getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                            });
                                });
                    } else {
                        // If email is admin@gmail.com and login failed, try to create the account
                        if (email.equals("admin@gmail.com") && password.equals("admin123")) {
                            // Try to create admin account
                            mAuth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(createTask -> {
                                        if (createTask.isSuccessful() && mAuth.getCurrentUser() != null) {
                                            String userId = mAuth.getCurrentUser().getUid();

                                            // Create admin user
                                            User adminUser = new User();
                                            adminUser.setEmail(email);
                                            adminUser.setFullName("Administrator");
                                            adminUser.setRole("admin");
                                            adminUser.setAge(0);
                                            adminUser.setPhoneNumber("");
                                            adminUser.setPhotoUrl("");
                                            adminUser.setLocked(false);
                                            adminUser.setId(userId);

                                            db.collection("users").document(userId)
                                                    .set(adminUser)
                                                    .addOnSuccessListener(aVoid -> {
                                                        recordLoginHistory(true);
                                                        showProgress(false);
                                                        Toast.makeText(LoginActivity.this,
                                                                "Admin account created and login successful",
                                                                Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        showProgress(false);
                                                        recordLoginHistory(false);
                                                        mAuth.signOut();
                                                        Toast.makeText(LoginActivity.this,
                                                                "Error creating admin profile: " + e.getMessage(),
                                                                Toast.LENGTH_LONG).show();
                                                    });
                                        } else {
                                            // Failed to create admin account
                                            showProgress(false);
                                            recordLoginHistory(false);
                                            String errorMessage = createTask.getException() != null ?
                                                    createTask.getException().getMessage() :
                                                    "Failed to create admin account";
                                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                        }
                                    });
                        } else {
                            // Normal login failure
                            showProgress(false);
                            recordLoginHistory(false);
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() :
                                    getString(R.string.authentication_failed);
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void recordLoginHistory(boolean isSuccessful) {
        String userId = isSuccessful && mAuth.getCurrentUser() != null ?
                mAuth.getCurrentUser().getUid() : "unknown";

        // Create login history record
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String loginDate = sdf.format(new Date());

        // Get device info
        String deviceInfo = Build.MANUFACTURER + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE;

        // Create login history object
        LoginHistory loginHistory = new LoginHistory(
                userId,
                loginDate,
                "Device Login", // Placeholder for IP address
                deviceInfo,
                isSuccessful
        );

        // Save login history to Firestore
        db.collection("loginHistory")
                .add(loginHistory.toMap())
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Login history recorded with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding login history", e);
                });
    }
}