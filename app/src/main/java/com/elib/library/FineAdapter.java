package com.elib.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FineAdapter extends RecyclerView.Adapter<FineAdapter.FineViewHolder> {
    private final List<Book> books;

    public FineAdapter(List<Book> books) {
        this.books = books;
    }

    @NonNull
    @Override
    public FineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fine, parent, false);
        return new FineViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FineViewHolder holder, int position) {
        Book b = books.get(position);
        holder.title.setText(b.getTitle());
        double fine = b.getFine() != null ? b.getFine() : 0.0;
        holder.amount.setText("Rs. " + String.format("%.2f", fine));
        
        long now = System.currentTimeMillis();
        long due = b.getDueDate() != null ? b.getDueDate() : now;
        long diff = now - due;
        long days = diff / (24 * 60 * 60 * 1000);
        
        if (days > 0) {
            holder.daysOverdue.setText(days + " days overdue");
        } else {
            holder.daysOverdue.setText("Overdue");
        }
        
        holder.rate.setText("Fine rate: Rs. 10.0/day");
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class FineViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView amount;
        TextView daysOverdue;
        TextView rate;
        
        FineViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_fine_title);
            amount = itemView.findViewById(R.id.chip_amount);
            daysOverdue = itemView.findViewById(R.id.text_days_overdue);
            rate = itemView.findViewById(R.id.text_fine_rate);
        }
    }
}
