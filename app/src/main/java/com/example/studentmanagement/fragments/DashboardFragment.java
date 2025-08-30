package com.example.studentmanagement.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.studentmanagement.R;
import com.example.studentmanagement.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private TextView tvStudentCount, tvUserCount, tvCurrentDate;
    private TextView tvWelcomeMessage, tvUserRole;
    private CardView cardStudents, cardUsers, cardProfile;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserRole = "";

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        initViews(view);

        // Set current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        tvCurrentDate.setText(currentDate);

        // Get user role from arguments
        if (getArguments() != null) {
            currentUserRole = getArguments().getString("USER_ROLE", "employee");
        }

        // Get current user info
        loadUserInfo();

        // Load dashboard statistics
        loadDashboardData();

        // Setup card click listeners
        setupCardClickListeners();

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshDashboard);
    }

    private void initViews(View view) {
        tvStudentCount = view.findViewById(R.id.tv_student_count);
        tvUserCount = view.findViewById(R.id.tv_user_count);
        tvCurrentDate = view.findViewById(R.id.tv_current_date);
        tvWelcomeMessage = view.findViewById(R.id.tv_welcome_message);
        tvUserRole = view.findViewById(R.id.tv_user_role);

        cardStudents = view.findViewById(R.id.card_students);
        cardUsers = view.findViewById(R.id.card_users);
        cardProfile = view.findViewById(R.id.card_profile);

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
    }

    private void loadUserInfo() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                String welcomeText = "Welcome, " + user.getFullName() + "!";
                                tvWelcomeMessage.setText(welcomeText);

                                // Get user role (use the one passed from arguments if available)
                                if (currentUserRole.isEmpty()) {
                                    currentUserRole = user.getRole();
                                }

                                // Display role with first letter capitalized
                                tvUserRole.setText(currentUserRole.substring(0, 1).toUpperCase() +
                                        currentUserRole.substring(1).toLowerCase());

                                // Configure dashboard based on role
                                configureUIBasedOnRole();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error loading user info", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void configureUIBasedOnRole() {
        // Configure dashboard UI elements based on user role
        switch (currentUserRole.toLowerCase()) {
            case "admin":
                // Admin can see all cards
                cardStudents.setVisibility(View.VISIBLE);
                cardUsers.setVisibility(View.VISIBLE);
                break;

            case "manager":
                // Manager can see students but not users
                cardStudents.setVisibility(View.VISIBLE);
                cardUsers.setVisibility(View.GONE);
                break;

            case "employee":
            default:
                // Employee can see students but not users
                cardStudents.setVisibility(View.VISIBLE);
                cardUsers.setVisibility(View.GONE);
                break;
        }
    }

    private void loadDashboardData() {
        // Load Students Count for all roles
        db.collection("students")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvStudentCount.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading student data", Toast.LENGTH_SHORT).show();
                });

        // Load Users Count (for admin only)
        if ("admin".equalsIgnoreCase(currentUserRole)) {
            db.collection("users")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int count = queryDocumentSnapshots.size();
                        tvUserCount.setText(String.valueOf(count));
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                    });
        } else {
            tvUserCount.setText("N/A");
        }
    }

    private void setupCardClickListeners() {
        cardStudents.setOnClickListener(v -> {
            // Navigate to Students Fragment with user role
            Bundle args = new Bundle();
            args.putString("USER_ROLE", currentUserRole);

            StudentFragment fragment = new StudentFragment();
            fragment.setArguments(args);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        cardUsers.setOnClickListener(v -> {
            // Navigate to User Management Fragment (admin only)
            if ("admin".equalsIgnoreCase(currentUserRole)) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new UserFragment())
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(getContext(), "You don't have permission to access this feature",
                        Toast.LENGTH_SHORT).show();
            }
        });

        cardProfile.setOnClickListener(v -> {
            // Navigate to Profile Fragment
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new UserDetailFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void refreshDashboard() {
        // Reload user info and dashboard statistics
        loadUserInfo();
        loadDashboardData();

        // Stop the refreshing animation
        swipeRefreshLayout.setRefreshing(false);
    }
}