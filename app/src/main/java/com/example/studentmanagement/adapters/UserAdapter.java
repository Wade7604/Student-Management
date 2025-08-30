package com.example.studentmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentmanagement.R;
import com.example.studentmanagement.models.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserClickListener listener;

    public void removeItem(User user) {
        int position = userList.indexOf(user);  // Lấy vị trí của user trong danh sách
        if (position != -1) {  // Kiểm tra nếu user tồn tại trong danh sách
            userList.remove(position);  // Xóa user khỏi danh sách
            notifyItemRemoved(position);  // Thông báo RecyclerView về việc xóa item
        }
    }


    public interface OnUserClickListener {
        void onUserClick(User user, int position);
        void onDeleteClick(User user, int position);
    }

    public UserAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user, position);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvFullName, tvEmail, tvRole, tvStatus;
        ImageButton btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullName = itemView.findViewById(R.id.tv_user_name);
            tvEmail = itemView.findViewById(R.id.tv_user_email);
            tvRole = itemView.findViewById(R.id.tv_user_role);
            tvStatus = itemView.findViewById(R.id.tv_user_status);
            btnDelete = itemView.findViewById(R.id.btn_delete_user);
        }

        public void bind(User user, int position) {
            tvFullName.setText(user.getFullName());
            tvEmail.setText(user.getEmail());
            tvRole.setText(user.getRole());
            tvStatus.setText(user.isLocked() ? "Locked" : "Active");

            // Set tag for easier access to position
            itemView.setTag(position);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user, position);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(user, position);
                }
            });
        }
    }
}