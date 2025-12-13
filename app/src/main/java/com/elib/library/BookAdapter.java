package com.elib.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Book> books;
    private final OnBookClickListener listener;
    private boolean isAdmin = false;
    private boolean showAddCard = false;
    private java.util.Map<String, Integer> availabilityByTitle = new java.util.HashMap<>();

    public interface OnBookClickListener {
        void onEditClick(Book book);
        void onDeleteClick(Book book);
        void onIssueClick(Book book);
        void onReturnClick(Book book);
        void onDetailsClick(Book book);
        void onAdminOptions(Book book);
        void onAddClick();
    }

    private static final int TYPE_ADD = 0;
    private static final int TYPE_BOOK = 1;

    public BookAdapter(List<Book> books, OnBookClickListener listener) {
        this.books = books;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ADD) {
            View v = inflater.inflate(R.layout.item_add_book, parent, false);
            return new AddViewHolder(v);
        } else {
            View view = inflater.inflate(R.layout.item_book, parent, false);
            return new BookViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_ADD) {
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAddClick();
            });
            return;
        }
        int dataPos = showAddCard ? position - 1 : position;
        Book book = books.get(dataPos);
        BookViewHolder h = (BookViewHolder) holder;
        h.titleText.setText(book.getTitle());
        h.authorText.setText("Author: " + book.getAuthor());
        h.isbnText.setText("ISBN: " + book.getIsbn());
        h.yearText.setText("Year: " + book.getYear());

        View issueBtn = h.itemView.findViewById(R.id.btn_issue);
        View returnBtn = h.itemView.findViewById(R.id.btn_return);
        View editBtn = h.itemView.findViewById(R.id.btn_edit);
        View deleteBtn = h.itemView.findViewById(R.id.btn_delete);
        issueBtn.setVisibility(isAdmin && book.isAvailable() ? View.VISIBLE : View.GONE);
        returnBtn.setVisibility(isAdmin && !book.isAvailable() ? View.VISIBLE : View.GONE);
        editBtn.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        deleteBtn.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

        Integer count = availabilityByTitle.getOrDefault(book.getTitle(), 0);
        TextView countText = h.itemView.findViewById(R.id.text_available_count);
        if (countText != null) {
            countText.setText("Available: " + count);
        }
        h.authorText.setVisibility(View.VISIBLE);
        h.isbnText.setVisibility(View.VISIBLE);
        h.yearText.setVisibility(View.VISIBLE);

        h.itemView.findViewById(R.id.btn_edit).setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(book);
            }
        });

        h.itemView.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(book);
            }
        });

        issueBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIssueClick(book);
            }
        });

        returnBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReturnClick(book);
            }
        });

        TextView fineText = h.itemView.findViewById(R.id.text_fine);
        double fine = book.getFine() != null ? book.getFine() : 0.0;
        if (fine > 0) {
            fineText.setText("Fine: Rs. " + fine);
            fineText.setVisibility(View.VISIBLE);
        } else {
            fineText.setText("");
            fineText.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                if (isAdmin) {
                    listener.onAdminOptions(book);
                } else {
                    listener.onDetailsClick(book);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return books.size() + (showAddCard ? 1 : 0);
    }

    public void updateBooks(List<Book> newBooks) {
        this.books = newBooks;
        notifyDataSetChanged();
    }
    public void setAdmin(boolean admin) {
        this.isAdmin = admin;
        notifyDataSetChanged();
    }
    public void setShowAddCard(boolean show) {
        this.showAddCard = show;
        notifyDataSetChanged();
    }
    public void setAvailabilityByTitle(java.util.Map<String, Integer> map) {
        this.availabilityByTitle = map != null ? map : new java.util.HashMap<>();
        notifyDataSetChanged();
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, authorText, isbnText, yearText, fineText;

        BookViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.text_title);
            authorText = itemView.findViewById(R.id.text_author);
            isbnText = itemView.findViewById(R.id.text_isbn);
            yearText = itemView.findViewById(R.id.text_year);
            fineText = itemView.findViewById(R.id.text_fine);
        }
    }
    static class AddViewHolder extends RecyclerView.ViewHolder {
        AddViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (showAddCard && position == 0) return TYPE_ADD;
        return TYPE_BOOK;
    }
}

