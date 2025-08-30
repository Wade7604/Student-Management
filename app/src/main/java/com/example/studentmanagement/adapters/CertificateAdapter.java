package com.example.studentmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentmanagement.R;
import com.example.studentmanagement.models.Certificate;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CertificateAdapter extends RecyclerView.Adapter<CertificateAdapter.CertificateViewHolder> {

    private List<Certificate> certificateList;
    private OnCertificateClickListener listener;
    private String userRole = "employee"; // Default role

    public void setUserRole(String userRole) {
        this.userRole = userRole;
        notifyDataSetChanged(); // Refresh to update button visibility
    }

    public interface OnCertificateClickListener {
        void onCertificateClick(Certificate certificate, int position);
        void onDeleteClick(Certificate certificate, int position);
    }

    public CertificateAdapter(List<Certificate> certificateList, OnCertificateClickListener listener) {
        this.certificateList = certificateList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CertificateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_certificate, parent, false);
        return new CertificateViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull CertificateViewHolder holder, int position) {
        Certificate certificate = certificateList.get(position);
        holder.tvCertificateName.setText(certificate.getCertificateName());
        holder.tvIssuingAuthority.setText(certificate.getIssuingAuthority());
        holder.tvIssueDate.setText("Issued: " + certificate.getIssueDate());

        // Initialize with ID in case fetching the name fails
        holder.tvStudentId.setText("Student: " + certificate.getStudentId());

        // Fetch student name from Firestore
        String studentId = certificate.getStudentId();
        fetchStudentName(studentId, holder.tvStudentId);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCertificateClick(certificate, position);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(certificate, position);
            }
        });
        holder.btnDelete.setVisibility(canDelete() ? View.VISIBLE : View.GONE);
    }
    private void fetchStudentName(String studentId, final TextView textView) {
        if (studentId == null || studentId.isEmpty()) {
            textView.setText("Student: Unknown");
            return;
        }

        FirebaseFirestore.getInstance().collection("students")
                .document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Assuming the student document has a "name" field
                        String studentName = documentSnapshot.getString("name");
                        if (studentName != null && !studentName.isEmpty()) {
                            textView.setText("Student: " + studentName);
                        } else {
                            textView.setText("Student: " + studentId);
                        }
                    } else {
                        textView.setText("Student: " + studentId);
                    }
                })
                .addOnFailureListener(e -> {
                    textView.setText("Student: " + studentId);
                });
    }
    @Override
    public int getItemCount() {
        return certificateList.size();
    }
    private boolean canDelete() {
        return "admin".equals(userRole) || "manager".equals(userRole);
    }
    static class CertificateViewHolder extends RecyclerView.ViewHolder {
        TextView tvCertificateName, tvIssuingAuthority, tvIssueDate, tvStudentId;
        ImageButton btnDelete;

        public CertificateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCertificateName = itemView.findViewById(R.id.tv_certificate_name);
            tvIssuingAuthority = itemView.findViewById(R.id.tv_issuing_authority);
            tvIssueDate = itemView.findViewById(R.id.tv_issue_date);
            tvStudentId = itemView.findViewById(R.id.tv_student_id);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}