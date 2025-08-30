package com.example.studentmanagement.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.studentmanagement.R;
import com.example.studentmanagement.adapters.UserAdapter;
import com.example.studentmanagement.dialog.UserDialog;
import com.example.studentmanagement.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserFragment extends Fragment implements UserAdapter.OnUserClickListener {

    private RecyclerView recyclerViewUsers;
    private FloatingActionButton fabAddUser;
    private SwipeRefreshLayout swipeRefreshLayout;
    private UserAdapter userAdapter;
    private List<User> userList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    public UserFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Initialize UI components
        initViews(view);

        // Set up RecyclerView
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList, this);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewUsers.setAdapter(userAdapter);

        // Load users
        loadUsers();

        // Setup FAB click listener
        fabAddUser.setOnClickListener(v -> showAddUserDialog());

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshUserList);
    }

    private void initViews(View view) {
        recyclerViewUsers = view.findViewById(R.id.recycler_view_users);
        fabAddUser = view.findViewById(R.id.fab_add_user);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
    }

    private void loadUsers() {
        swipeRefreshLayout.setRefreshing(true);
        userList.clear();

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setId(document.getId());
                        if (!"admin".equals(user.getRole())) {
                            userList.add(user);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void refreshUserList() {
        loadUsers();
    }

    private void showAddUserDialog() {
        UserDialog dialog = new UserDialog(getContext(), null, new UserDialog.UserDialogListener() {
            @Override
            public void onUserAdded(User user) {
                // User is already added to Firestore by the dialog
                // Just update our local list
                userList.add(user);
                userAdapter.notifyItemInserted(userList.size() - 1);
            }

            @Override
            public void onUserUpdated(User user) {
                // Not applicable for Add operation
            }

            @Override
            public void onUserDeleted(User user) {
                // Not applicable for Add operation
            }
        });
        dialog.show();
    }

    private void showEditUserDialog(User user, int position) {
        UserDialog dialog = new UserDialog(getContext(), user, new UserDialog.UserDialogListener() {
            @Override
            public void onUserAdded(User user) {
                // Not applicable for Edit operation
            }

            @Override
            public void onUserUpdated(User updatedUser) {
                // User is already updated in Firestore by the dialog
                // Just update our local list
                userList.set(position, updatedUser);
                userAdapter.notifyItemChanged(position);
            }

            @Override
            public void onUserDeleted(User user) {
                // Not applicable for Edit operation
            }
        });
        dialog.show();
    }

    private void deleteUser(User user, int position) {
        // Check if trying to delete yourself
        if (user.getId().equals(currentUserId)) {
            Toast.makeText(getContext(), "You cannot delete your own account", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getFullName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // First delete from Firebase Authentication
                    FirebaseUser userToDelete = FirebaseAuth.getInstance().getCurrentUser();

                    // Only try to delete from Authentication if we're signed in as that user
                    if (userToDelete != null && userToDelete.getEmail().equals(user.getEmail())) {
                        userToDelete.delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Now delete from Firestore
                                    deleteUserFromFirestore(user, position);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w("UserFragment", "Error deleting auth user", e);
                                    // Continue with Firestore deletion anyway
                                    deleteUserFromFirestore(user, position);
                                });
                    } else {
                        // If we can't delete from Authentication (maybe we're not signed in as that user),
                        // just delete from Firestore
                        deleteUserFromFirestore(user, position);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserFromFirestore(User user, int position) {
        // Delete user from Firestore
        db.collection("users").document(user.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // If the user is a student, also delete from students collection
                    if ("student".equals(user.getRole())) {
                        removeStudentFromFirebase(user.getEmail());
                    }

                    // Find the user in the list instead of relying on the position parameter
                    int currentPosition = userList.indexOf(user);
                    if (currentPosition != -1 && currentPosition < userList.size()) {
                        userList.remove(currentPosition);
                        userAdapter.notifyItemRemoved(currentPosition);
                    } else {
                        // If we can't find the user or the position is invalid, just refresh the whole list
                        refreshUserList();
                    }
                    Toast.makeText(getContext(), "User deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error deleting user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Method to remove student from Firebase students collection
    private void removeStudentFromFirebase(String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("students").document(email)  // Use email as document ID
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserFragment", "Student record successfully removed from students collection");
                })
                .addOnFailureListener(e -> {
                    Log.e("UserFragment", "Error removing student from students collection", e);
                });
    }

    @Override
    public void onUserClick(User user, int position) {
        // Show user details or edit dialog
        showEditUserDialog(user, position);
    }

    @Override
    public void onDeleteClick(User user, int position) {
        deleteUser(user, position);
    }
}