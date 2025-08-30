package com.example.studentmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.studentmanagement.activity.LoginActivity;
import com.example.studentmanagement.fragments.DashboardFragment;
import com.example.studentmanagement.fragments.LoginHistoryFragment;
import com.example.studentmanagement.fragments.StudentFragment;
import com.example.studentmanagement.fragments.UserDetailFragment;
import com.example.studentmanagement.fragments.UserFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Set up UI components
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Set up the navigation drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            // User is not logged in, redirect to login activity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Retrieve user role and update navigation
        getUserRoleAndUpdateNavigation(savedInstanceState);
    }

    private void getUserRoleAndUpdateNavigation(Bundle savedInstanceState) {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userRole = documentSnapshot.getString("role");
                        if (userRole == null) userRole = "employee"; // Default role

                        // Update navigation header
                        updateNavigationHeader();

                        // Configure menu items based on user role
                        updateMenuItemsByRole();

                        // Load default fragment
                        if (savedInstanceState == null) {
                            loadFragment(new DashboardFragment());
                            navigationView.setCheckedItem(R.id.nav_dashboard);
                            setTitle("Dashboard");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    userRole = "employee"; // Default to lowest permission
                    updateMenuItemsByRole();
                });
    }

    private void updateMenuItemsByRole() {
        // Get menu from navigation view
        Menu menu = navigationView.getMenu();

        // Configure menu items based on user role
        MenuItem usersMenuItem = menu.findItem(R.id.nav_users);

        // Configure visibility based on role
        if (usersMenuItem != null) {
            // Only admin can see User Management
            usersMenuItem.setVisible("admin".equals(userRole));
        }

        // Pass role information to fragments that need it
        Bundle args = new Bundle();
        args.putString("USER_ROLE", userRole);

        // Set arguments for currently displayed fragment if any
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null) {
            currentFragment.setArguments(args);
        }
    }

    private void updateNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView tvName = headerView.findViewById(R.id.tv_user_name);
        TextView tvEmail = headerView.findViewById(R.id.tv_user_email);
        // Get reference to CircleImageView instead of ImageView
        CircleImageView ivProfile = headerView.findViewById(R.id.iv_profile);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Fetch additional user data from Firestore
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            tvName.setText(name != null ? name : getString(R.string.app_name));
                            tvEmail.setText(currentUser.getEmail());

                            // Load profile image if available
                            String photoUrl = documentSnapshot.getString("photoUrl");
                            if (photoUrl != null && !photoUrl.isEmpty() && ivProfile != null) {
                                // Use Glide to load the image
                                Glide.with(MainActivity.this)
                                        .load(photoUrl)
                                        .placeholder(R.drawable.default_profile) // Default image while loading
                                        .error(R.drawable.default_profile) // Show default image on error
                                        .into(ivProfile);
                            } else if (ivProfile != null) {
                                // If no photo URL, just show the default image
                                ivProfile.setImageResource(R.drawable.default_profile);
                            }
                        }
                    });
        }
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_dashboard) {
            loadFragment(new DashboardFragment());
            setTitle("Dashboard");
        } else if (itemId == R.id.nav_students) {
            // Create bundle with user role
            Bundle args = new Bundle();
            args.putString("USER_ROLE", userRole);

            // Create and configure fragment
            StudentFragment fragment = new StudentFragment();
            fragment.setArguments(args);

            // Load the fragment
            loadFragment(fragment);
            setTitle("Student Management");
        } else if (itemId == R.id.nav_users) {
            // Only admin should be able to access this
            if ("admin".equals(userRole)) {
                loadFragment(new UserFragment());
                setTitle("User Management");
            } else {
                Toast.makeText(this, "You don't have permission to access this feature",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.nav_profile) {
            loadFragment(new UserDetailFragment());
            setTitle("My Profile");
        } else if (itemId == R.id.nav_import_export) {
            loadFragment(new LoginHistoryFragment());
            setTitle("Login History");
        } else if (itemId == R.id.nav_logout) {
            logOut();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment) {
        // If fragment needs role info, pass it
        if (fragment instanceof StudentFragment || fragment instanceof DashboardFragment) {
            Bundle args = new Bundle();
            args.putString("USER_ROLE", userRole);
            fragment.setArguments(args);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void logOut() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}