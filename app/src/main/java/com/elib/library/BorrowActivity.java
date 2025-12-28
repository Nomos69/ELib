package com.elib.library;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BorrowActivity extends AppCompatActivity {

    private TextView tabIssue, tabReturn;
    private View viewIssue, viewReturn;
    private EditText inputIssueSearch;
    private TextView inputIssueDate;
    private RecyclerView recyclerBorrowed;
    private BorrowedBookAdapter adapter;
    private List<Book> borrowedBooks = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Calendar selectedDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_borrow);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize Views
        tabIssue = findViewById(R.id.tab_issue);
        tabReturn = findViewById(R.id.tab_return);
        viewIssue = findViewById(R.id.view_issue);
        viewReturn = findViewById(R.id.view_return);
        inputIssueSearch = findViewById(R.id.input_issue_search);
        inputIssueDate = findViewById(R.id.input_issue_date);
        recyclerBorrowed = findViewById(R.id.recycler_borrowed);
        View btnConfirmIssue = findViewById(R.id.btn_confirm_issue);

        // Setup RecyclerView
        recyclerBorrowed.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BorrowedBookAdapter(borrowedBooks);
        recyclerBorrowed.setAdapter(adapter);

        // Setup Tabs
        tabIssue.setOnClickListener(v -> switchTab(true));
        tabReturn.setOnClickListener(v -> switchTab(false));

        // Setup Issue Logic
        selectedDate.add(Calendar.DAY_OF_YEAR, 14); // Default 14 days
        updateDateDisplay();
        inputIssueDate.setOnClickListener(v -> showDatePicker());

        btnConfirmIssue.setOnClickListener(v -> issueBook());

        // Setup Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_borrow);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_borrow) {
                return true;
            } else if (id == R.id.nav_catalog) {
                startActivity(new android.content.Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_fines) {
                startActivity(new android.content.Intent(this, FinesActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new android.content.Intent(this, AccountActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return true;
        });
        
        loadBorrowedBooks();
    }

    private void switchTab(boolean isIssue) {
        if (isIssue) {
            tabIssue.setBackgroundResource(R.drawable.bg_button_gradient);
            tabIssue.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tabReturn.setBackgroundResource(android.R.color.transparent);
            tabReturn.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
            viewIssue.setVisibility(View.VISIBLE);
            viewReturn.setVisibility(View.GONE);
        } else {
            tabReturn.setBackgroundResource(R.drawable.bg_button_gradient);
            tabReturn.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tabIssue.setBackgroundResource(android.R.color.transparent);
            tabIssue.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
            viewReturn.setVisibility(View.VISIBLE);
            viewIssue.setVisibility(View.GONE);
            loadBorrowedBooks();
        }
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        inputIssueDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate.set(year, month, dayOfMonth);
            updateDateDisplay();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void issueBook() {
        String query = inputIssueSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter Title or ISBN", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Search by Title first, then ISBN
        db.collection("books").whereEqualTo("title", query).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        processIssue(snap.getDocuments().get(0).toObject(Book.class), snap.getDocuments().get(0).getId());
                    } else {
                        // Try ISBN
                        db.collection("books").whereEqualTo("isbn", query).get()
                                .addOnSuccessListener(snap2 -> {
                                    if (!snap2.isEmpty()) {
                                        processIssue(snap2.getDocuments().get(0).toObject(Book.class), snap2.getDocuments().get(0).getId());
                                    } else {
                                        Toast.makeText(this, "Book not found", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
    }

    private void processIssue(Book book, String docId) {
        if (!book.isAvailable()) {
            Toast.makeText(this, "Book is already issued", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();
        long now = System.currentTimeMillis();
        long due = selectedDate.getTimeInMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("available", false);
        updates.put("issueDate", now);
        updates.put("dueDate", due);
        updates.put("returnDate", null);
        updates.put("fine", 0.0);
        updates.put("borrowerId", uid);
        updates.put("borrowerEmail", email);

        db.collection("books").document(docId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Book issued successfully!", Toast.LENGTH_SHORT).show();
                    inputIssueSearch.setText("");
                    switchTab(false); // Switch to Return tab to show the new book
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadBorrowedBooks() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("books").whereEqualTo("borrowerId", uid).get()
                .addOnSuccessListener(snap -> {
                    borrowedBooks.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        Book b = d.toObject(Book.class);
                        b.setId(d.getId());
                        borrowedBooks.add(b);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void returnBook(Book book) {
        long now = System.currentTimeMillis();
        long dueDate = book.getDueDate() != null ? book.getDueDate() : now;
        double fine = 0.0;
        if (now > dueDate) {
            long diff = now - dueDate;
            long days = diff / (24 * 60 * 60 * 1000);
            fine = days * 10.0; // Rs. 10 per day
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("available", true);
        updates.put("returnDate", now);
        updates.put("fine", fine);
        updates.put("lastBorrowerId", book.getBorrowerId());
        updates.put("borrowerId", null);
        updates.put("borrowerEmail", null);

        final double finalFine = fine;

        db.collection("books").document(book.getId()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    String msg = "Book returned.";
                    if (finalFine > 0) msg += " Fine: Rs. " + finalFine;
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    loadBorrowedBooks();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Inner Adapter Class
    private class BorrowedBookAdapter extends RecyclerView.Adapter<BorrowedBookAdapter.ViewHolder> {
        private List<Book> books;

        BorrowedBookAdapter(List<Book> books) {
            this.books = books;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_borrowed_book, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Book book = books.get(position);
            holder.title.setText(book.getTitle());
            holder.author.setText("by " + book.getAuthor());
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String issueStr = book.getIssueDate() != null ? sdf.format(new Date(book.getIssueDate())) : "N/A";
            String dueStr = book.getDueDate() != null ? sdf.format(new Date(book.getDueDate())) : "N/A";
            holder.dates.setText("Borrowed: " + issueStr + " • Due: " + dueStr);

            // Calculate Days Left
            long now = System.currentTimeMillis();
            long due = book.getDueDate() != null ? book.getDueDate() : now;
            long diff = due - now;
            long daysLeft = diff / (24 * 60 * 60 * 1000);

            if (daysLeft < 0) {
                holder.daysLeft.setText(Math.abs(daysLeft) + " days overdue");
                holder.daysLeft.setTextColor(ContextCompat.getColor(BorrowActivity.this, android.R.color.holo_red_light));
            } else {
                holder.daysLeft.setText(daysLeft + " days left");
                holder.daysLeft.setTextColor(ContextCompat.getColor(BorrowActivity.this, android.R.color.holo_green_light));
            }

            // Progress (Fake for demo, or based on time elapsed)
            long totalDuration = due - (book.getIssueDate() != null ? book.getIssueDate() : now);
            if (totalDuration <= 0) totalDuration = 1; // Avoid divide by zero
            long elapsed = now - (book.getIssueDate() != null ? book.getIssueDate() : now);
            int progress = (int) ((elapsed * 100) / totalDuration);
            if (progress > 100) progress = 100;
            if (progress < 0) progress = 0;
            
            holder.progressText.setText(progress + "%");
            holder.progressBar.setProgress(progress);

            holder.btnReturn.setOnClickListener(v -> returnBook(book));
        }

        @Override
        public int getItemCount() {
            return books.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, author, daysLeft, progressText, dates;
            ProgressBar progressBar;
            View btnReturn;

            ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.text_borrowed_title);
                author = itemView.findViewById(R.id.text_borrowed_author);
                daysLeft = itemView.findViewById(R.id.chip_days_left);
                progressText = itemView.findViewById(R.id.text_progress_percent);
                dates = itemView.findViewById(R.id.text_borrow_dates);
                progressBar = itemView.findViewById(R.id.progress_return);
                btnReturn = itemView.findViewById(R.id.btn_return_book);
            }
        }
    }
}
