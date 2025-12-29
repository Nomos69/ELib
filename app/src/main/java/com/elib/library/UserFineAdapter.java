package com.elib.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UserFineAdapter extends RecyclerView.Adapter<UserFineAdapter.UserFineViewHolder> {
    private final List<UserFine> items;
    public UserFineAdapter(List<UserFine> items) {
        this.items = items;
    }
    @NonNull
    @Override
    public UserFineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_fine, parent, false);
        return new UserFineViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull UserFineViewHolder holder, int position) {
        UserFine uf = items.get(position);
        holder.name.setText(uf.name != null ? uf.name : "Unknown");
        holder.email.setText(uf.email != null ? uf.email : "");
        holder.total.setText("Total: ₱ " + String.format(java.util.Locale.getDefault(), "%.2f", uf.totalFine));
    }
    @Override
    public int getItemCount() {
        return items.size();
    }
    static class UserFineViewHolder extends RecyclerView.ViewHolder {
        TextView name, email, total;
        UserFineViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_user_name);
            email = itemView.findViewById(R.id.text_user_email);
            total = itemView.findViewById(R.id.text_user_total);
        }
    }
}
