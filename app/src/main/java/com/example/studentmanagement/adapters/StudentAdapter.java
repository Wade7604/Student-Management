package com.example.studentmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentmanagement.R;
import com.example.studentmanagement.models.Student;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private List<Student> studentList;
    private OnStudentClickListener listener;
    private String userRole = "employee"; // Default role

    public StudentAdapter(List<Student> studentList, OnStudentClickListener listener) {
        this.studentList = studentList;
        this.listener = listener;
    }

    // Set user role from fragment
    public void setUserRole(String userRole) {
        this.userRole = userRole;
        notifyDataSetChanged(); // Refresh to update button visibility
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.bind(student, position);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStudentId, tvEmail, tvClass;
        ImageButton btnEdit, btnDelete;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_student_name);
            tvStudentId = itemView.findViewById(R.id.tv_student_id);
            tvEmail = itemView.findViewById(R.id.tv_student_email);
            tvClass = itemView.findViewById(R.id.tv_student_class);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(Student student, int position) {
            tvName.setText(student.getName());
            tvStudentId.setText(student.getStudentId());

            // Handle possibly null values
            String email = student.getEmail();
            tvEmail.setText(email != null ? email : "N/A");

            String className = student.getClassName();
            tvClass.setText(className != null ? className : "N/A");

            // Set click listener for the entire item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStudentClick(student, position);
                }
            });

            // Show/hide buttons based on permissions
            btnEdit.setVisibility(canEdit() ? View.VISIBLE : View.GONE);
            btnDelete.setVisibility(canDelete() ? View.VISIBLE : View.GONE);

            // Set click listeners for buttons
            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(student, position);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(student, position);
                }
            });
        }

        // Permission checks
        private boolean canEdit() {
            return "admin".equals(userRole) || "manager".equals(userRole);
        }

        private boolean canDelete() {
            return "admin".equals(userRole) || "manager".equals(userRole);
        }
    }

    public interface OnStudentClickListener {
        void onStudentClick(Student student, int position);
        void onEditClick(Student student, int position);
        void onDeleteClick(Student student, int position);
    }
}