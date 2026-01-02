package com.elib.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.facebook.shimmer.ShimmerFrameLayout;

public class FineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Book> books;
    private boolean isLoading = false;
    
    private static final int TYPE_FINE = 0;
    private static final int TYPE_SKELETON = 1;

    public FineAdapter(List<Book> books) {
        this.books = books;
    }
    
    public void setLoading(boolean loading) {
        this.isLoading = loading;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SKELETON) {
            View view = inflater.inflate(R.layout.item_fine_shimmer, parent, false);
            return new SkeletonViewHolder(view);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fine, parent, false);
            return new FineViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_SKELETON) {
            // Skeleton items don't need binding
            return;
        }
        
        FineViewHolder fineHolder = (FineViewHolder) holder;
        Book b = books.get(position);
        fineHolder.title.setText(b.getTitle());
        double fine = b.getFine() != null ? b.getFine() : 0.0;
        fineHolder.amount.setText("₱ " + String.format("%.2f", fine));
        
        long now = System.currentTimeMillis();
        long due = b.getDueDate() != null ? b.getDueDate() : now;
        long diff = now - due;
        long days = diff / (24 * 60 * 60 * 1000);
        
        if (days > 0) {
            fineHolder.daysOverdue.setText(days + " days overdue");
        } else {
            fineHolder.daysOverdue.setText("Overdue");
        }
        
        fineHolder.rate.setText("Fine rate: ₱ 10.0/day");
    }

    @Override
    public int getItemCount() {
        if (isLoading) {
            return 3; // Show 3 skeleton items
        }
        return books.size();
    }
    
    @Override
    public int getItemViewType(int position) {
        if (isLoading) {
            return TYPE_SKELETON;
        }
        return TYPE_FINE;
    }

    static class SkeletonViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerContainer;

        SkeletonViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerContainer = itemView.findViewById(R.id.shimmer_container);
            shimmerContainer.startShimmer();
        }
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
