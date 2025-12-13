package com.elib.library;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
// import androidx.appcompat.widget.Toolbar; // Unused import removed
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private BookAdapter adapter;
    private List<Book> books;
    private List<Book> allBooks;
    private FirebaseFirestore db;
    private EditText searchEditText;
    private FirebaseAuth auth;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new android.content.Intent(this, LoginActivity.class));
        }
        db = FirebaseFirestore.getInstance();
        books = new ArrayList<>();
        allBooks = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_books);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookAdapter(books, new BookAdapter.OnBookClickListener() {
            @Override
            public void onEditClick(Book book) {
                showAddEditDialog(book);
            }
            @Override
            public void onAddClick() {
                showAddEditDialog(null);
            }

            @Override
            public void onDeleteClick(Book book) {
                deleteBook(book);
            }

            @Override
            public void onIssueClick(Book book) {
                promptIssueToUser(book);
            }

            @Override
            public void onReturnClick(Book book) {
                returnBook(book);
            }
            @Override
            public void onDetailsClick(Book book) {
                showBookDetails(book);
            }
            @Override
            public void onAdminOptions(Book book) {
                showAdminOptions(book);
            }
        });



        recyclerView.setAdapter(adapter);

        searchEditText = findViewById(R.id.edit_search);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBooks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        adapter.setShowAddCard(isAdmin);

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_books) {
                // already on books
                return true;
            } else if (id == R.id.nav_users) {
                if (isAdmin) {
                    startActivity(new android.content.Intent(this, AdminFinesActivity.class));
                } else {
                    Toast.makeText(this, "Only admin can view users", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (id == R.id.nav_notifications) {
                startActivity(new android.content.Intent(this, NotificationsActivity.class));
                return true;
            } else if (id == R.id.nav_fines) {
                if (isAdmin) {
                    startActivity(new android.content.Intent(this, AdminFinesActivity.class));
                } else {
                    startActivity(new android.content.Intent(this, FinesActivity.class));
                }
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new android.content.Intent(this, AccountActivity.class));
                return true;
            }
            return true;
        });

        updateAdminStatus();
        loadBooks();
    }

    private void showAdminOptions(Book book) {
        if (!isAdmin) return;
        String[] options = new String[]{"Edit", "Delete", book.isAvailable() ? "Issue to user" : "Mark return"};
        new AlertDialog.Builder(this)
                .setTitle(book.getTitle())
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        showAddEditDialog(book);
                    } else if (which == 1) {
                        deleteBook(book);
                    } else {
                        if (book.isAvailable()) {
                            promptIssueToUser(book);
                        } else {
                            returnBook(book);
                        }
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth.getCurrentUser() == null) {
            android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        updateAdminStatus();
    }

    // --- Book Issue & Return Logic ---
    private void promptIssueToUser(Book book) {
        if (!isAdmin) {
            Toast.makeText(this, "Only admin can issue books", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!book.isAvailable()) {
            Toast.makeText(this, "Book is already issued.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users").get()
                .addOnSuccessListener(snapshot -> {
                    java.util.List<String> labels = new java.util.ArrayList<>();
                    java.util.List<String> uids = new java.util.ArrayList<>();
                    java.util.List<String> emailsLower = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot d : snapshot) {
                        String uid = d.getId();
                        String name = d.getString("username");
                        String email = d.getString("email");
                        String el = email != null ? email.trim().toLowerCase() : "";
                        String label = (name != null ? name : "") + " (" + (email != null ? email : "") + ")";
                        labels.add(label);
                        uids.add(uid);
                        emailsLower.add(el);
                    }
                    if (labels.isEmpty()) {
                        Toast.makeText(this, "No users found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Assign Borrower")
                            .setItems(labels.toArray(new CharSequence[0]), (d, which) -> {
                                String uid = uids.get(which);
                                String emailLower = emailsLower.get(which);
                                long now = System.currentTimeMillis();
                                long due = now + 14L * 24 * 60 * 60 * 1000;
                                db.collection("books")
                                        .document(book.getId())
                                        .update("available", false, "issueDate", now, "dueDate", due, "returnDate", null, "fine", 0.0, "borrowerId", uid, "borrowerEmail", emailLower)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Book issued", Toast.LENGTH_SHORT).show();
                                            loadBooks();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(this, "Error issuing book: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                db.collection("users").document(uid).update("emailLowercase", emailLower);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void returnBook(Book book) {
        if (!isAdmin) {
            Toast.makeText(this, "Only admin can mark return", Toast.LENGTH_SHORT).show();
            return;
        }
        if (book.isAvailable()) {
            Toast.makeText(this, "Book is already available.", Toast.LENGTH_SHORT).show();
            return;
        }
        long now = System.currentTimeMillis();
        long dueDate = book.getDueDate() != null ? book.getDueDate() : (book.getIssueDate() != null ? book.getIssueDate() + 14L * 24 * 60 * 60 * 1000 : now);
        final double fineValue = (now > dueDate)
                ? (((now - dueDate) / (24 * 60 * 60 * 1000)) * 10.0)
                : 0.0;
        db.collection("books")
                .document(book.getId())
                .update("available", true, "returnDate", now, "fine", fineValue, "lastBorrowerId", book.getBorrowerId(), "borrowerId", null, "borrowerEmail", null)
                .addOnSuccessListener(aVoid -> {
            if (fineValue > 0) {
                Toast.makeText(this, "Book returned. Fine: Rs. " + fineValue, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Book returned successfully", Toast.LENGTH_SHORT).show();
            }
            loadBooks();
        })
                .addOnFailureListener(e -> {
            Toast.makeText(this, "Error returning book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadBooks() {
        db.collection("books")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allBooks.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Book book = document.toObject(Book.class);
                            book.setId(document.getId());
                            allBooks.add(book);
                        }
                        filterBooks(searchEditText.getText().toString());
                        adapter.setAdmin(isAdmin);
                        Map<String, Integer> counts = new HashMap<>();
                        for (Book b : allBooks) {
                            if (b.isAvailable()) {
                                counts.put(b.getTitle(), counts.getOrDefault(b.getTitle(), 0) + 1);
                            }
                        }
                        adapter.setAvailabilityByTitle(counts);
                    } else {
                        Toast.makeText(this, "Error loading books: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterBooks(String query) {
        books.clear();
        if (query.isEmpty()) {
            books.addAll(allBooks);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Book book : allBooks) {
                if (book.getTitle().toLowerCase().contains(lowerQuery) ||
                    book.getAuthor().toLowerCase().contains(lowerQuery) ||
                    book.getIsbn().toLowerCase().contains(lowerQuery)) {
                    books.add(book);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showAddEditDialog(Book book) {
        if (book == null && !isAdmin) {
            Toast.makeText(this, "Only admin can add books", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_book, null);
        builder.setView(view);

        EditText editTitle = view.findViewById(R.id.edit_title);
        EditText editAuthor = view.findViewById(R.id.edit_author);
        EditText editIsbn = view.findViewById(R.id.edit_isbn);
        EditText editYear = view.findViewById(R.id.edit_year);
        EditText editDescription = view.findViewById(R.id.edit_description);
        View layoutDescription = view.findViewById(R.id.layout_description);
        android.widget.CheckBox checkAvailable = view.findViewById(R.id.check_available);

        if (book != null) {
            editTitle.setText(book.getTitle());
            editAuthor.setText(book.getAuthor());
            editIsbn.setText(book.getIsbn());
            editYear.setText(String.valueOf(book.getYear()));
            editDescription.setText(book.getDescription() != null ? book.getDescription() : "");
            checkAvailable.setChecked(book.isAvailable());
        } else {
            checkAvailable.setChecked(true);
        }
        if (!isAdmin && layoutDescription != null) {
            layoutDescription.setVisibility(View.GONE);
        }

        AlertDialog dialog = builder.create();

        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String title = editTitle.getText().toString().trim();
            String author = editAuthor.getText().toString().trim();
            String isbn = editIsbn.getText().toString().trim();
            String yearStr = editYear.getText().toString().trim();
            boolean available = checkAvailable.isChecked();
            String description = editDescription.getText().toString().trim();

            if (title.isEmpty() || author.isEmpty() || isbn.isEmpty() || yearStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int year = Integer.parseInt(yearStr);
                if (book == null) {
                    addBook(title, author, isbn, year, available, description);
                } else {
                    updateBook(book.getId(), title, author, isbn, year, available, description);
                }
                dialog.dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid year", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateAdminStatus() {
        if (auth.getCurrentUser() == null) {
            isAdmin = false;
            adapter.setAdmin(false);
            adapter.setShowAddCard(false);
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();
        db.collection("admins").document(uid).get()
                .addOnSuccessListener(doc -> {
                    boolean docAdmin = doc.exists();
                    boolean emailAdmin = "admin@gmail.com".equalsIgnoreCase(email);
                    isAdmin = docAdmin || emailAdmin;
                    adapter.setAdmin(isAdmin);
                    adapter.setShowAddCard(isAdmin);
                    com.google.android.material.bottomnavigation.BottomNavigationView bar = findViewById(R.id.bottom_nav);
                    if (bar != null) {
                        android.view.Menu menu = bar.getMenu();
                        android.view.MenuItem users = menu.findItem(R.id.nav_users);
                        android.view.MenuItem bell = menu.findItem(R.id.nav_notifications);
                        android.view.MenuItem fines = menu.findItem(R.id.nav_fines);
                        if (users != null) users.setVisible(isAdmin);
                        if (bell != null) bell.setVisible(isAdmin);
                        if (fines != null) fines.setVisible(!isAdmin);
                    }
                })
                .addOnFailureListener(e -> {
                    isAdmin = false;
                    adapter.setAdmin(false);
                    adapter.setShowAddCard(false);
                    com.google.android.material.bottomnavigation.BottomNavigationView bar = findViewById(R.id.bottom_nav);
                    if (bar != null) {
                        android.view.Menu menu = bar.getMenu();
                        android.view.MenuItem users = menu.findItem(R.id.nav_users);
                        android.view.MenuItem bell = menu.findItem(R.id.nav_notifications);
                        android.view.MenuItem fines = menu.findItem(R.id.nav_fines);
                        if (users != null) users.setVisible(false);
                        if (bell != null) bell.setVisible(false);
                        if (fines != null) fines.setVisible(true);
                    }
                });
    }

    private void addBook(String title, String author, String isbn, int year, boolean available, String description) {
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("title", title);
        bookData.put("author", author);
        bookData.put("isbn", isbn);
        bookData.put("year", year);
        bookData.put("available", available);
        if (isAdmin && !description.isEmpty()) bookData.put("description", description);

        db.collection("books")
                .add(bookData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Book added successfully", Toast.LENGTH_SHORT).show();
                    loadBooks();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateBook(String id, String title, String author, String isbn, int year, boolean available, String description) {
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("title", title);
        bookData.put("author", author);
        bookData.put("isbn", isbn);
        bookData.put("year", year);
        bookData.put("available", available);
        if (isAdmin) {
            bookData.put("description", description);
        }

        db.collection("books")
                .document(id)
                .update(bookData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Book updated successfully", Toast.LENGTH_SHORT).show();
                    loadBooks();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showBookDetails(Book book) {
        String desc = book.getDescription() != null && !book.getDescription().isEmpty()
                ? book.getDescription()
                : "Author: " + book.getAuthor() + "\nISBN: " + book.getIsbn() + "\nYear: " + book.getYear();
        new AlertDialog.Builder(this)
                .setTitle(book.getTitle())
                .setMessage(desc)
                .setPositiveButton("OK", null)
                .show();
    }

    private void deleteBook(Book book) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Book")
                .setMessage("Are you sure you want to delete \"" + book.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("books")
                            .document(book.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Book deleted successfully", Toast.LENGTH_SHORT).show();
                                loadBooks();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error deleting book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            loadBooks();
            return true;
        } else if (item.getItemId() == R.id.menu_fines) {
            startActivity(new android.content.Intent(this, FinesActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

