package com.example.studentmanagement.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentmanagement.R;
import com.example.studentmanagement.adapters.LoginHistoryAdapter;
import com.example.studentmanagement.models.LoginHistory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LoginHistoryFragment extends Fragment {

    private static final String TAG = "LoginHistoryFragment";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private LoginHistoryAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<LoginHistory> loginHistoryList;
    private boolean isAdmin = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firestore and Auth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_login_history);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loginHistoryList = new ArrayList<>();
        adapter = new LoginHistoryAdapter(loginHistoryList, isAdmin, db);
        recyclerView.setAdapter(adapter);

        // Check if current user is admin
        checkUserRole();
    }

    private void checkUserRole() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && "admin".equals(documentSnapshot.getString("role"))) {
                            isAdmin = true;
                            // Update adapter with admin status
                            adapter.setAdmin(isAdmin);
                        }
                        // Load login history based on role
                        loadLoginHistory();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking user role", e);
                        Toast.makeText(getContext(), "Error checking user role", Toast.LENGTH_SHORT).show();
                        // Load login history for current user only in case of error
                        loadLoginHistory();
                    });
        } else {
            // User not logged in, shouldn't reach here but handle gracefully
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            showEmptyView("Please login to view history");
        }
    }

    private void loadLoginHistory() {
        showLoading(true);

        Query query;
        if (isAdmin) {
            // Admins can see all login history
            query = db.collection("loginHistory")
                    .orderBy("loginDate", Query.Direction.DESCENDING);
        } else {
            // Regular users only see their own login history
            query = db.collection("loginHistory")
                    .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                    .orderBy("loginDate", Query.Direction.DESCENDING);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    loginHistoryList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyView("No login history found");
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        LoginHistory loginHistory = document.toObject(LoginHistory.class);
                        loginHistory.setId(document.getId());
                        loginHistoryList.add(loginHistory);
                    }

                    adapter.notifyDataSetChanged();
                    showRecyclerView();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading login history", e);
                    Toast.makeText(getContext(), "Error loading login history: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showEmptyView("Error loading login history");
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showEmptyView(String message) {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        emptyView.setText(message);
    }

    private void showRecyclerView() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }
}