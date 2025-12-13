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
        holder.amount.setText("Rs. " + (b.getFine() != null ? b.getFine() : 0.0));
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy");
        Long ret = b.getReturnDate();
        Long due = b.getDueDate();
        if (ret != null) {
            String text = "Returned: " + sdf.format(new java.util.Date(ret));
            if (due != null) {
                text += " • Due: " + sdf.format(new java.util.Date(due));
            }
            holder.date.setText(text);
            holder.date.setVisibility(View.VISIBLE);
        } else {
            holder.date.setText("");
            holder.date.setVisibility(View.GONE);
        }
        Long issue = b.getIssueDate();
        if (issue != null) {
            holder.borrowed.setText("Borrowed: " + sdf.format(new java.util.Date(issue)));
            holder.borrowed.setVisibility(View.VISIBLE);
        } else {
            holder.borrowed.setText("");
            holder.borrowed.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class FineViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView amount;
        TextView date;
        TextView borrowed;
        FineViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_fine_title);
            amount = itemView.findViewById(R.id.text_fine_amount);
            date = itemView.findViewById(R.id.text_fine_date);
            borrowed = itemView.findViewById(R.id.text_fine_borrowed);
        }
    }
}
