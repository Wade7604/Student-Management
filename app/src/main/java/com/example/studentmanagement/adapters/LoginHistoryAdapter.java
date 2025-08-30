package com.example.studentmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentmanagement.R;
import com.example.studentmanagement.models.LoginHistory;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class LoginHistoryAdapter extends RecyclerView.Adapter<LoginHistoryAdapter.ViewHolder> {

    private final List<LoginHistory> historyList;
    private boolean isAdmin;
    private FirebaseFirestore db;

    public LoginHistoryAdapter(List<LoginHistory> historyList, boolean isAdmin, FirebaseFirestore db) {
        this.historyList = historyList;
        this.isAdmin = isAdmin;
        this.db = db;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_login_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LoginHistory loginHistory = historyList.get(position);

        // Display user ID if admin, otherwise hide it
        if (isAdmin) {
            holder.tvUserId.setVisibility(View.VISIBLE);
            holder.tvUserId.setText("User ID: " + loginHistory.getUserId());

            // Get user email if possible
            db.collection("users").document(loginHistory.getUserId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String email = documentSnapshot.getString("email");
                            if (email != null && !email.isEmpty()) {
                                holder.tvUserId.setText("User: " + email);
                            }
                        }
                    });
        } else {
            holder.tvUserId.setVisibility(View.GONE);
        }

        holder.tvDate.setText(loginHistory.getLoginDate());
        holder.tvDevice.setText(loginHistory.getDeviceInfo());

        // Set status indicator
        if (loginHistory.isSuccessful()) {
            holder.tvStatus.setText("Successful");
            holder.tvStatus.setTextColor(holder.itemView.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.tvStatus.setText("Failed");
            holder.tvStatus.setTextColor(holder.itemView.getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserId, tvDate, tvDevice, tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvUserId = itemView.findViewById(R.id.tv_user_id);
            tvDate = itemView.findViewById(R.id.tv_login_date);
            tvDevice = itemView.findViewById(R.id.tv_device_info);
            tvStatus = itemView.findViewById(R.id.tv_login_status);
        }
    }
}