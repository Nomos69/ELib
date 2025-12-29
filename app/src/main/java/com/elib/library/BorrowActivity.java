package com.elib.library;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
    private Spinner spinnerBook, spinnerUser;
    private TextView inputIssueDate;
    private RecyclerView recyclerBorrowed;
    private BorrowedBookAdapter adapter;
    private List<Book> borrowedBooks = new ArrayList<>();
    
    private List<Book> availableBooks = new ArrayList<>();
    private List<String> availableBookTitles = new ArrayList<>();
    private List<Map<String, String>> users = new ArrayList<>();
    private List<String> userNames = new ArrayList<>();
    private Book selectedBook;
    private String selectedUserId;
    private String selectedUserEmail;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Calendar selectedDate = Calendar.getInstance();
    private boolean isAdmin = false;

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
        spinnerBook = findViewById(R.id.spinner_issue_book);
        spinnerUser = findViewById(R.id.spinner_issue_user);
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
        
        checkAdminAndLoad();
        loadBorrowedBooks();
    }

    private void checkAdminAndLoad() {
        if (auth.getCurrentUser() == null) return;
        
        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        String email = auth.getCurrentUser().getEmail();
                        
                        // Check if role is admin OR if email is specifically admin@gmail.com
                        isAdmin = "admin".equalsIgnoreCase(role) || "admin@gmail.com".equalsIgnoreCase(email);
                        
                        if (isAdmin) {
                            loadBooks();
                            loadUsers();
                        } else {
                            // If not admin, hide issue tab or show warning
                            // For now, we'll just disable the issue functionality visually or show a toast if they try
                            // But better to hide the issue tab content or replace with "Admin Only" text
                            viewIssue.setVisibility(View.GONE);
                            TextView adminMsg = new TextView(this);
                            adminMsg.setText("Only Administrators can issue books.");
                            adminMsg.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                            adminMsg.setGravity(android.view.Gravity.CENTER);
                            ((ViewGroup) viewIssue.getParent()).addView(adminMsg);
                            switchTab(false); // Default to Return tab for users
                            tabIssue.setVisibility(View.GONE); // Hide issue tab
                        }
                    }
                });
    }

    private void loadBooks() {
        db.collection("books").whereEqualTo("available", true).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    availableBooks.clear();
                    availableBookTitles.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Book book = doc.toObject(Book.class);
                        book.setId(doc.getId());
                        availableBooks.add(book);
                        availableBookTitles.add(book.getTitle() + " (" + book.getAuthor() + ")");
                    }
                    
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, availableBookTitles);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBook.setAdapter(adapter);
                    
                    spinnerBook.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position >= 0 && position < availableBooks.size()) {
                                selectedBook = availableBooks.get(position);
                            }
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
    }

    private void loadUsers() {
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    users.clear();
                    userNames.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, String> user = new HashMap<>();
                        user.put("id", doc.getId());
                        user.put("name", doc.getString("name"));
                        user.put("email", doc.getString("email"));
                        users.add(user);
                        userNames.add(doc.getString("name") + " (" + doc.getString("email") + ")");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, userNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerUser.setAdapter(adapter);

                    spinnerUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position >= 0 && position < users.size()) {
                                selectedUserId = users.get(position).get("id");
                                selectedUserEmail = users.get(position).get("email");
                            }
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
    }

    private void switchTab(boolean isIssue) {
        if (isIssue) {
            if (!isAdmin) {
                ToastHelper.showError(this, "Only admins can issue books");
                return;
            }
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
        if (!isAdmin) {
            Toast.makeText(this, "Only admins can issue books", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedBook == null) {
            Toast.makeText(this, "Please select a book", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedUserId == null) {
            Toast.makeText(this, "Please select a user", Toast.LENGTH_SHORT).show();
            return;
        }

        processIssue(selectedBook, selectedBook.getId());
    }

    private void processIssue(Book book, String docId) {
        if (!book.isAvailable()) {
            Toast.makeText(this, "Book is already issued", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();
        long due = selectedDate.getTimeInMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("available", false);
        updates.put("issueDate", now);
        updates.put("dueDate", due);
        updates.put("returnDate", null);
        updates.put("fine", 0.0);
        updates.put("borrowerId", selectedUserId);
        updates.put("borrowerEmail", selectedUserEmail);

        db.collection("books").document(docId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    ToastHelper.showSuccess(this, "Book issued successfully");
                    loadBooks(); // Refresh list to remove issued book
                    switchTab(false); // Switch to Return tab
                })
                .addOnFailureListener(e -> ToastHelper.showError(this, "Error: " + e.getMessage()));
    }

    private void loadBorrowedBooks() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        // If admin, show all borrowed books? Or just their own?
        // Typically "Return Book" tab shows what YOU have borrowed so you can return it.
        // Or if admin, maybe they can see all borrowed books to return them?
        // For now, let's keep it as "My Borrowed Books" unless the user specified otherwise.
        // User request: "only the admin can do it" (issue).
        // It's ambiguous if return is also admin only. Usually users return books or admins mark them returned.
        // In this app, "Return Book" seems to list books borrowed by the user.
        
        db.collection("books").whereEqualTo("borrowerId", uid).get()
                .addOnSuccessListener(snap -> {
                    borrowedBooks.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Book book = doc.toObject(Book.class);
                        book.setId(doc.getId());
                        borrowedBooks.add(book);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
