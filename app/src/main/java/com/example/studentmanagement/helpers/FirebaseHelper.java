package com.example.studentmanagement.helpers;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.studentmanagement.models.Certificate;
import com.example.studentmanagement.models.LoginHistory;
import com.example.studentmanagement.models.Student;
import com.example.studentmanagement.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";

    // Firebase Database references
    private DatabaseReference databaseRef;
    private DatabaseReference studentsRef;
    private DatabaseReference certificatesRef;
    private DatabaseReference usersRef;
    private DatabaseReference loginHistoryRef;

    // Firebase Authentication
    private FirebaseAuth firebaseAuth;

    // Firebase Storage
    private StorageReference storageRef;

    // Singleton instance
    private static FirebaseHelper instance;

    public static FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    private FirebaseHelper() {
        firebaseAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        studentsRef = databaseRef.child("students");
        certificatesRef = databaseRef.child("certificates");
        usersRef = databaseRef.child("users");
        loginHistoryRef = databaseRef.child("loginHistory");
        storageRef = FirebaseStorage.getInstance().getReference();
    }

    // User Authentication Methods
    public void registerUser(String email, String password, User user, final OnCompleteListener<AuthResult> listener) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Get the user ID from Firebase Auth
                        String userId = task.getResult().getUser().getUid();
                        // Set the user ID and save to database
                        user.setId(userId);
                        usersRef.child(userId).setValue(user);
                    }
                    listener.onComplete(task);
                });
    }

    public void loginUser(String email, String password, final OnCompleteListener<AuthResult> listener) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }

    public void logoutUser() {
        firebaseAuth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void resetPassword(String email, final OnCompleteListener<Void> listener) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(listener);
    }

    // User Management Methods
    public void saveUser(User user, final OnSuccessListener<Void> successListener, final OnFailureListener failureListener) {
        if (user.getId() == null || user.getId().isEmpty()) {
            // New user, generate ID
            String userId = usersRef.push().getKey();
            user.setId(userId);
        }

        usersRef.child(user.getId()).setValue(user)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getUser(String userId, final ValueEventListener listener) {
        usersRef.child(userId).addListenerForSingleValueEvent(listener);
    }

    public void getAllUsers(final ValueEventListener listener) {
        usersRef.addValueEventListener(listener);
    }

    public void deleteUser(String userId, final OnSuccessListener<Void> successListener, final OnFailureListener failureListener) {
        usersRef.child(userId).removeValue()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void searchUsers(String query, final ValueEventListener listener) {
        Query searchQuery = usersRef.orderByChild("fullName")
                .startAt(query)
                .endAt(query + "\uf8ff");
        searchQuery.addListenerForSingleValueEvent(listener);
    }

    // Student Management Methods
    public void saveStudent(Student student, final OnSuccessListener<Void> successListener, final OnFailureListener failureListener) {
        if (student.getId() == null || student.getId().isEmpty()) {
            // New student, generate ID
            String studentId = studentsRef.push().getKey();
            student.setId(studentId);
        }

        studentsRef.child(student.getId()).setValue(student)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getStudent(String studentId, final ValueEventListener listener) {
        studentsRef.child(studentId).addListenerForSingleValueEvent(listener);
    }

    public void getAllStudents(final ValueEventListener listener) {
        studentsRef.addValueEventListener(listener);
    }

    public void deleteStudent(String studentId, final OnSuccessListener<Void> successListener, final OnFailureListener failureListener) {
        studentsRef.child(studentId).removeValue()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void searchStudents(String query, final ValueEventListener listener) {
        Query searchQuery = studentsRef.orderByChild("fullName")
                .startAt(query)
                .endAt(query + "\uf8ff");
        searchQuery.addListenerForSingleValueEvent(listener);
    }

    public void filterStudentsByStatus(boolean isActive, final ValueEventListener listener) {
        Query filterQuery = studentsRef.orderByChild("isActive").equalTo(isActive);
        filterQuery.addListenerForSingleValueEvent(listener);
    }

    // Certificate Management Methods
    public void saveCertificate(Certificate certificate, final OnSuccessListener<Void> successListener, final OnFailureListener failureListener) {
        if (certificate.getId() == null || certificate.getId().isEmpty()) {
            // New certificate, generate ID
            String certificateId = certificatesRef.push().getKey();
            certificate.setId(certificateId);
        }

        // Add to certificates reference
        certificatesRef.child(certificate.getId()).setValue(certificate)
                .addOnSuccessListener(aVoid -> {
                    // Also add to student's certificates map
                    studentsRef.child(certificate.getStudentId())
                            .child("certificates")
                            .child(certificate.getId())
                            .setValue(certificate)
                            .addOnSuccessListener(successListener)
                            .addOnFailureListener(failureListener);
                })
                .addOnFailureListener(failureListener);
    }

    public void getCertificate(String certificateId, final ValueEventListener listener) {
        certificatesRef.child(certificateId).addListenerForSingleValueEvent(listener);
    }

    public void deleteCertificate(String certificateId, String studentId, final OnSuccessListener<Void> successListener, final OnFailureListener failureListener) {
        certificatesRef.child(certificateId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Also remove from student's certificates map
                    studentsRef.child(studentId)
                            .child("certificates")
                            .child(certificateId)
                            .removeValue()
                            .addOnSuccessListener(successListener)
                            .addOnFailureListener(failureListener);
                })
                .addOnFailureListener(failureListener);
    }

    public void getStudentCertificates(String studentId, final ValueEventListener listener) {
        Query query = certificatesRef.orderByChild("studentId").equalTo(studentId);
        query.addValueEventListener(listener);
    }

    // Login History Methods
    public void saveLoginHistory(LoginHistory loginHistory, final OnSuccessListener<Void> successListener, final OnFailureListener failureListener) {
        String loginHistoryId = loginHistoryRef.push().getKey();
        loginHistory.setId(loginHistoryId);

        loginHistoryRef.child(loginHistoryId).setValue(loginHistory)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getUserLoginHistory(String userId, final ValueEventListener listener) {
        Query query = loginHistoryRef.orderByChild("userId").equalTo(userId);
        query.addValueEventListener(listener);
    }

    // Image Upload Methods
    public void uploadImage(Uri imageUri, String folderName, final OnSuccessListener<Uri> successListener, final OnFailureListener failureListener) {
        // Create a storage reference for the image
        String fileName = System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(folderName + "/" + fileName);

        // Upload the file
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(successListener);
                })
                .addOnFailureListener(failureListener);
    }

    // Batch operations for import/export
    public void importStudents(List<Student> students, final OnSuccessListener<Void> successListener, final OnFailureListener failureListener) {
        Map<String, Object> updateMap = new HashMap<>();

        for (Student student : students) {
            if (student.getId() == null || student.getId().isEmpty()) {
                String studentId = studentsRef.push().getKey();
                student.setId(studentId);
            }
            updateMap.put("/students/" + student.getId(), student.toMap());
        }

        databaseRef.updateChildren(updateMap)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void importCertificates(List<Certificate> certificates, final OnSuccessListener<Void> successListener, final OnFailureListener failureListener) {
        Map<String, Object> updateMap = new HashMap<>();

        for (Certificate certificate : certificates) {
            if (certificate.getId() == null || certificate.getId().isEmpty()) {
                String certificateId = certificatesRef.push().getKey();
                certificate.setId(certificateId);
            }
            updateMap.put("/certificates/" + certificate.getId(), certificate.toMap());
            // Also add to student's certificates
            updateMap.put("/students/" + certificate.getStudentId() + "/certificates/" + certificate.getId(), certificate.toMap());
        }

        databaseRef.updateChildren(updateMap)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }
}