package com.elib.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BorrowedBookAdapter extends RecyclerView.Adapter<BorrowedBookAdapter.ViewHolder> {

    private List<Book> books;

    public BorrowedBookAdapter(List<Book> books) {
        this.books = books;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_borrowed_book, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Book book = books.get(position);
        
        holder.titleText.setText(book.getTitle());
        holder.authorText.setText("by " + book.getAuthor());
        
        long now = System.currentTimeMillis();
        long due = book.getDueDate() != null ? book.getDueDate() : now;
        long issue = book.getIssueDate() != null ? book.getIssueDate() : now;
        
        // Days left calculation
        long diffMillis = due - now;
        long daysLeft = TimeUnit.MILLISECONDS.toDays(diffMillis);
        
        if (daysLeft < 0) {
            holder.daysLeftChip.setText("Overdue by " + Math.abs(daysLeft) + " days");
            holder.daysLeftChip.setTextColor(0xFFFF5252); // Red
        } else {
            holder.daysLeftChip.setText(daysLeft + " days left");
            holder.daysLeftChip.setTextColor(0xFF00E676); // Green
        }
        
        // Progress calculation
        long totalDuration = due - issue;
        long elapsed = now - issue;
        int progress = 0;
        if (totalDuration > 0) {
            progress = (int) ((elapsed * 100) / totalDuration);
        }
        progress = Math.min(100, Math.max(0, progress));
        
        holder.progressBar.setProgress(progress);
        holder.progressText.setText(progress + "%");
        
        // Date formatting
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String issueStr = issue > 0 ? sdf.format(issue) : "N/A";
        String dueStr = due > 0 ? sdf.format(due) : "N/A";
        holder.dateText.setText("Borrowed: " + issueStr + " • Due: " + dueStr);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView authorText;
        TextView daysLeftChip;
        ProgressBar progressBar;
        TextView progressText;
        TextView dateText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.text_borrowed_title);
            authorText = itemView.findViewById(R.id.text_borrowed_author);
            daysLeftChip = itemView.findViewById(R.id.chip_days_left);
            progressBar = itemView.findViewById(R.id.progress_return);
            progressText = itemView.findViewById(R.id.text_progress_percent);
            dateText = itemView.findViewById(R.id.text_borrow_dates);
        }
    }
}
