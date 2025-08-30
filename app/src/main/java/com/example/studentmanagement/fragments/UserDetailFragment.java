package com.example.studentmanagement.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.studentmanagement.R;
import com.example.studentmanagement.dialog.UserDialog;
import com.example.studentmanagement.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserDetailFragment extends Fragment {

    private static final String TAG = "UserDetailFragment";

    private ImageView ivUserPhoto;
    private TextView tvFullName, tvEmail, tvPhone, tvAge, tvRole;
    private Button btnChangePhoto, btnEditProfile;

    private User currentUser;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri selectedImageUri;
    private String currentUserId;
    private ProgressBar progressBar;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public UserDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_detail, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Set up image picker launcher
        setupImagePicker();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        initViews(view);

        // Load current user data
        loadCurrentUserData();

        // Set click listeners
        btnChangePhoto.setOnClickListener(v -> openImagePicker());
        btnEditProfile.setOnClickListener(v -> openEditProfileDialog());
;
    }

    private void initViews(View view) {
        ivUserPhoto = view.findViewById(R.id.iv_user_photo);
        tvFullName = view.findViewById(R.id.tv_full_name);
        tvEmail = view.findViewById(R.id.tv_email);
        tvPhone = view.findViewById(R.id.tv_phone);
        tvAge = view.findViewById(R.id.tv_age);
        tvRole = view.findViewById(R.id.tv_role);
        btnChangePhoto = view.findViewById(R.id.btn_change_photo);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        progressBar = view.findViewById(R.id.progress_bar);

    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            // Show the selected image
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .circleCrop()
                                    .into(ivUserPhoto);

                            // Upload the image to Firebase Storage
                            uploadImageToFirebase(selectedImageUri);
                        }
                    }
                }
        );
    }

    private void loadCurrentUserData() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        currentUser.setId(documentSnapshot.getId());

                        // Display user data
                        displayUserData();

                        // Set up based on user role
                        setupUserPermissions();
                    } else {
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading user data", e);
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                });
    }

    private void displayUserData() {
        if (currentUser != null && isAdded()) {
            // Set text fields
            tvFullName.setText(currentUser.getFullName());
            tvEmail.setText(currentUser.getEmail());
            tvPhone.setText(currentUser.getPhoneNumber());
            tvAge.setText(String.valueOf(currentUser.getAge()));
            tvRole.setText(currentUser.getRole());

            // Load profile photo if available
            if (currentUser.getPhotoUrl() != null && !currentUser.getPhotoUrl().isEmpty() && isAdded()) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(ivUserPhoto);
            } else {
                ivUserPhoto.setImageResource(R.drawable.default_profile);
            }
        }
    }

    private void setupUserPermissions() {
        if (currentUser != null) {
            String role = currentUser.getRole();

            if ("admin".equals(role)) {
                // Admin can edit all their information
                btnEditProfile.setVisibility(View.VISIBLE);
            } else {
                // Employees and managers can only change their photo
                btnEditProfile.setVisibility(View.GONE);
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (currentUser == null || !isAdded()) return;

        // Show progress bar immediately before starting upload
        progressBar.setVisibility(View.VISIBLE);

        // Record start time (for minimum display time)
        long startTime = System.currentTimeMillis();

        // Create a storage reference
        StorageReference storageRef = storage.getReference()
                .child("profile_images")
                .child(UUID.randomUUID().toString());

        // Upload file with progress tracking
        storageRef.putFile(imageUri)
                .addOnProgressListener(snapshot -> {
                    long bytesTransferred = snapshot.getBytesTransferred();
                    long totalBytes = snapshot.getTotalByteCount();
                    int progress = (int) ((100.0 * bytesTransferred) / totalBytes);
                    // Progress bar is already visible
                    Log.d(TAG, "Upload progress: " + progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();

                        // Calculate elapsed time
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        long remainingTime = Math.max(0, 500 - elapsedTime); // Ensure min 500ms display

                        // Add a small delay before hiding progress bar
                        new android.os.Handler().postDelayed(() -> {
                            // Update user photoUrl in Firestore
                            updateUserPhotoUrl(imageUrl);
                        }, remainingTime);
                    }).addOnFailureListener(e -> {
                        // Hide progress bar if download URL fails
                        progressBar.setVisibility(View.GONE);
                        if (isAdded()) {
                            Toast.makeText(getContext(),
                                    "Failed to get download URL: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // Hide progress bar
                    progressBar.setVisibility(View.GONE);

                    if (isAdded()) {
                        Toast.makeText(getContext(),
                                "Failed to upload image: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Image upload failed", e);
                    }
                });
    }
    private void updateUserPhotoUrl(String imageUrl) {
        if (currentUser == null || !isAdded()) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("photoUrl", imageUrl);

        db.collection("users").document(currentUser.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Hide progress bar
                    progressBar.setVisibility(View.GONE);

                    if (isAdded()) {
                        Toast.makeText(getContext(),
                                "Profile photo updated successfully",
                                Toast.LENGTH_SHORT).show();

                        // Update local user object
                        currentUser.setPhotoUrl(imageUrl);

                        // Update navigation drawer header
                        updateNavigationHeader(imageUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    // Hide progress bar
                    progressBar.setVisibility(View.GONE);

                    if (isAdded()) {
                        Toast.makeText(getContext(),
                                "Failed to update profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Profile update failed", e);
                    }
                });
    }

    private void updateNavigationHeader(String imageUrl) {
        // Get reference to the activity
        if (getActivity() == null) return;

        // Find the navigation view in the activity
        NavigationView navigationView = getActivity().findViewById(R.id.nav_view);
        if (navigationView == null) return;

        // Get the header view
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;

        // Find the profile image view
        de.hdodenhof.circleimageview.CircleImageView ivProfile =
                headerView.findViewById(R.id.iv_profile);
        if (ivProfile == null) return;

        // Update the profile image
        Glide.with(getActivity())
                .load(imageUrl)
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .skipMemoryCache(true)  // Skip memory cache to force refresh
                .into(ivProfile);
    }
    private void openEditProfileDialog() {
        // Only admin should be able to get here due to button visibility,
        // but adding an extra check for safety
        if (currentUser != null && "admin".equals(currentUser.getRole()) && isAdded()) {
            UserDialog dialog = new UserDialog(getContext(), currentUser, new UserDialog.UserDialogListener() {
                @Override
                public void onUserAdded(User user) {
                    // Not applicable
                }

                @Override
                public void onUserUpdated(User updatedUser) {
                    // Refresh the UI with updated user data
                    currentUser = updatedUser;
                    displayUserData();
                }

                @Override
                public void onUserDeleted(User user) {
                    // Not applicable
                }
            });
            dialog.show();
        }
    }
}