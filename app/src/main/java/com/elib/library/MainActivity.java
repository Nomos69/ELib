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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageButton;
import android.widget.Button;
import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.view.Gravity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

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
    private String currentFilter = "all";

    // Cloudinary Variables
    private Uri selectedPdfUri;
    private Uri selectedCoverUri;
    private TextView pdfStatusText;
    private TextView coverStatusText;
    private ActivityResultLauncher<String> pdfPickerLauncher;
    private ActivityResultLauncher<String> coverPickerLauncher;
    private boolean isCloudinaryInitialized = false;

    // Loading Dialog
    private AlertDialog loadingDialog;
    private TextView loadingMessage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Cloudinary (Ideally this goes in a custom Application class)
        try {
            Map config = new HashMap();
            config.put("cloud_name", "dsv58tr5j"); // Replace with your Cloud Name
            config.put("secure", true);
            MediaManager.init(this, config);
            isCloudinaryInitialized = true;
        } catch (IllegalStateException e) {
            // Already initialized
            isCloudinaryInitialized = true;
        }

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new android.content.Intent(this, LoginActivity.class));
        }
        db = FirebaseFirestore.getInstance();
        books = new ArrayList<>();
        allBooks = new ArrayList<>();

        // Initialize file pickers
        pdfPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedPdfUri = uri;
                if (pdfStatusText != null) pdfStatusText.setText("PDF Selected");
            }
        });

        coverPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedCoverUri = uri;
                if (coverStatusText != null) coverStatusText.setText("Cover Selected");
            }
        });

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
            @Override
            public void onReadClick(Book book) {
                if (book.getPdfUrl() != null && !book.getPdfUrl().isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(book.getPdfUrl()), "application/pdf");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                         // Fallback to browser if no PDF viewer app
                         Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(book.getPdfUrl()));
                         startActivity(browserIntent);
                    }
                } else {
                    ToastHelper.showInfo(MainActivity.this, "No PDF available");
                }
            }
            @Override
            public void onDownloadClick(Book book) {
                if (book.getPdfUrl() != null && !book.getPdfUrl().isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(book.getPdfUrl()));
                    startActivity(intent);
                } else {
                    ToastHelper.showInfo(MainActivity.this, "No PDF available");
                }
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
        bottomNav.setSelectedItemId(R.id.nav_catalog);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_catalog) {
                // already on books
                return true;
            } else if (id == R.id.nav_borrow) {
                startActivity(new android.content.Intent(this, BorrowActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_fines) {
                if (isAdmin) {
                    startActivity(new android.content.Intent(this, AdminFinesActivity.class));
                    overridePendingTransition(0, 0);
                } else {
                    startActivity(new android.content.Intent(this, FinesActivity.class));
                    overridePendingTransition(0, 0);
                }
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new android.content.Intent(this, AccountActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return true;
        });

        updateAdminStatus();
        setupFilters();
        loadBooks();
    }

    private void setupFilters() {
        ImageButton btnGrid = findViewById(R.id.btn_view_grid);
        ImageButton btnList = findViewById(R.id.btn_view_list);
        Button btnAll = findViewById(R.id.filter_all);
        Button btnAvailable = findViewById(R.id.filter_available);
        Button btnUnavailable = findViewById(R.id.filter_unavailable);

        int colorActive = ContextCompat.getColor(this, R.color.button_active);
        int colorInactive = ContextCompat.getColor(this, R.color.button_inactive);

        // View Toggle Butto
        btnGrid.setOnClickListener(v -> {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            btnGrid.setBackgroundTintList(ColorStateList.valueOf(colorActive));
            btnList.setBackgroundTintList(ColorStateList.valueOf(colorInactive));
        });

        btnList.setOnClickListener(v -> {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            btnList.setBackgroundTintList(ColorStateList.valueOf(colorActive));
            btnGrid.setBackgroundTintList(ColorStateList.valueOf(colorInactive));
        });

        // Status Filters
        View.OnClickListener filterListener = v -> {
            btnAll.setBackgroundTintList(ColorStateList.valueOf(colorInactive));
            btnAvailable.setBackgroundTintList(ColorStateList.valueOf(colorInactive));
            btnUnavailable.setBackgroundTintList(ColorStateList.valueOf(colorInactive));
            v.setBackgroundTintList(ColorStateList.valueOf(colorActive));

            if (v.getId() == R.id.filter_all) currentFilter = "all";
            else if (v.getId() == R.id.filter_available) currentFilter = "available";
            else if (v.getId() == R.id.filter_unavailable) currentFilter = "unavailable";
            
            filterBooks(searchEditText.getText().toString());
        };

        btnAll.setOnClickListener(filterListener);
        btnAvailable.setOnClickListener(filterListener);
        btnUnavailable.setOnClickListener(filterListener);
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
        
        // Load users first
        db.collection("users").get()
                .addOnSuccessListener(snapshot -> {
                    java.util.List<String> userLabels = new java.util.ArrayList<>();
                    java.util.List<String> uids = new java.util.ArrayList<>();
                    java.util.List<String> emailsLower = new java.util.ArrayList<>();
                    
                    for (com.google.firebase.firestore.QueryDocumentSnapshot d : snapshot) {
                        String uid = d.getId();
                        String name = d.getString("username");
                        String email = d.getString("email");
                        String el = email != null ? email.trim().toLowerCase() : "";
                        String label = (name != null ? name : "") + " (" + (email != null ? email : "") + ")";
                        userLabels.add(label);
                        uids.add(uid);
                        emailsLower.add(el);
                    }

                    if (userLabels.isEmpty()) {
                        ToastHelper.showError(this, "No users found");
                        return;
                    }

                    // Prepare available books list (plus the current one if it's available or not, but logic says issue implies picking available)
                    // If the passed 'book' is not available, we can't issue it, but maybe user wants to switch to another book.
                    // Let's gather all available books + current book (if not null)
                    List<Book> issueableBooks = new ArrayList<>();
                    for(Book b : allBooks) {
                        if (b.isAvailable() || (book != null && b.getId().equals(book.getId()))) {
                            issueableBooks.add(b);
                        }
                    }
                    
                    if (issueableBooks.isEmpty()) {
                         Toast.makeText(this, "No books available to issue", Toast.LENGTH_SHORT).show();
                         return;
                    }

                    List<String> bookLabels = new ArrayList<>();
                    int selectedBookIndex = 0;
                    for (int i=0; i<issueableBooks.size(); i++) {
                        Book b = issueableBooks.get(i);
                        bookLabels.add(b.getTitle() + " (" + b.getAuthor() + ")");
                        if (book != null && b.getId().equals(book.getId())) {
                            selectedBookIndex = i;
                        }
                    }

                    // Show Custom Dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    View view = LayoutInflater.from(this).inflate(R.layout.dialog_issue_book, null);
                    builder.setView(view);
                    AlertDialog dialog = builder.create();
                    // Transparent background for rounded corners if needed, but card_bg handles it in layout
                    if (dialog.getWindow() != null) {
                         dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    }

                    android.widget.Spinner spinnerBooks = view.findViewById(R.id.spinner_books);
                    android.widget.Spinner spinnerUsers = view.findViewById(R.id.spinner_users);
                    
                    android.widget.ArrayAdapter<String> bookAdapter = new android.widget.ArrayAdapter<>(this, R.layout.spinner_item, bookLabels);
                    bookAdapter.setDropDownViewResource(R.layout.spinner_item);
                    spinnerBooks.setAdapter(bookAdapter);
                    spinnerBooks.setSelection(selectedBookIndex);

                    android.widget.ArrayAdapter<String> userAdapter = new android.widget.ArrayAdapter<>(this, R.layout.spinner_item, userLabels);
                    userAdapter.setDropDownViewResource(R.layout.spinner_item);
                    spinnerUsers.setAdapter(userAdapter);

                    view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
                    view.findViewById(R.id.btn_issue).setOnClickListener(v -> {
                        int bookIdx = spinnerBooks.getSelectedItemPosition();
                        int userIdx = spinnerUsers.getSelectedItemPosition();
                        
                        if (bookIdx < 0 || userIdx < 0) return;

                        Book selectedBook = issueableBooks.get(bookIdx);
                        String uid = uids.get(userIdx);
                        String emailLower = emailsLower.get(userIdx);

                        if (!selectedBook.isAvailable()) {
                            Toast.makeText(this, "Selected book is already issued", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        long now = System.currentTimeMillis();
                        long due = now + 14L * 24 * 60 * 60 * 1000;
                        
                        db.collection("books")
                                .document(selectedBook.getId())
                                .update("available", false, "issueDate", now, "dueDate", due, "returnDate", null, "fine", 0.0, "borrowerId", uid, "borrowerEmail", emailLower)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Book issued successfully", Toast.LENGTH_SHORT).show();
                                    loadBooks();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Error issuing book: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        
                        db.collection("users").document(uid).update("emailLowercase", emailLower);
                    });

                    dialog.show();

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void returnBook(Book book) {
        if (!isAdmin) {
            ToastHelper.showError(this, "Only admin can mark return");
            return;
        }
        if (book.isAvailable()) {
            ToastHelper.showInfo(this, "Book is already available.");
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
                ToastHelper.showInfo(this, "Book returned. Fine: ₱ " + fineValue);
            } else {
                ToastHelper.showSuccess(this, "Book returned successfully");
            }
            loadBooks();
        })
                .addOnFailureListener(e -> {
            ToastHelper.showError(this, "Error returning book: " + e.getMessage());
        });
    }

    private void loadBooks() {
        showLoading("Loading library...");
        db.collection("books")
                .get()
                .addOnCompleteListener(task -> {
                    hideLoading();
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
                        ToastHelper.showError(this, "Error loading books: " + task.getException());
                    }
                });
    }

    private void filterBooks(String query) {
        books.clear();
        
        List<Book> statusFiltered = new ArrayList<>();
        for (Book book : allBooks) {
            if ("available".equals(currentFilter)) {
                if (book.isAvailable()) statusFiltered.add(book);
            } else if ("unavailable".equals(currentFilter)) {
                if (!book.isAvailable()) statusFiltered.add(book);
            } else {
                statusFiltered.add(book);
            }
        }

        if (query.isEmpty()) {
            books.addAll(statusFiltered);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Book book : statusFiltered) {
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

        selectedPdfUri = null;
        selectedCoverUri = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_book, null);
        builder.setView(view);

        EditText editTitle = view.findViewById(R.id.edit_title);
        EditText editAuthor = view.findViewById(R.id.edit_author);
        EditText editIsbn = view.findViewById(R.id.edit_isbn);
        android.widget.Spinner spinnerYear = view.findViewById(R.id.spinner_year);
        EditText editDescription = view.findViewById(R.id.edit_description);
        View layoutDescription = view.findViewById(R.id.layout_description);
        android.widget.CheckBox checkAvailable = view.findViewById(R.id.check_available);

        Button btnSelectCover = view.findViewById(R.id.btn_select_cover);
        Button btnSelectPdf = view.findViewById(R.id.btn_select_pdf);
        coverStatusText = view.findViewById(R.id.text_cover_status);
        pdfStatusText = view.findViewById(R.id.text_pdf_status);

        // Setup Year Spinner
        java.util.List<Integer> years = new java.util.ArrayList<>();
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        for (int i = currentYear; i >= 1900; i--) {
            years.add(i);
        }
        android.widget.ArrayAdapter<Integer> yearAdapter = new android.widget.ArrayAdapter<>(this, R.layout.spinner_item, years);
        yearAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerYear.setAdapter(yearAdapter);

        if (book != null) {
            editTitle.setText(book.getTitle());
            editAuthor.setText(book.getAuthor());
            editIsbn.setText(book.getIsbn());
            
            int yearIndex = years.indexOf(book.getYear());
            if (yearIndex >= 0) {
                spinnerYear.setSelection(yearIndex);
            }
            
            editDescription.setText(book.getDescription() != null ? book.getDescription() : "");
            checkAvailable.setChecked(book.isAvailable());
            
            if (book.getCoverUrl() != null && !book.getCoverUrl().isEmpty()) {
                coverStatusText.setText("Current cover kept");
            }
            if (book.getPdfUrl() != null && !book.getPdfUrl().isEmpty()) {
                pdfStatusText.setText("Current PDF kept");
            }
        } else {
            checkAvailable.setChecked(true);
        }

        if (!isAdmin && layoutDescription != null) {
            layoutDescription.setVisibility(View.GONE);
        }

        btnSelectCover.setOnClickListener(v -> coverPickerLauncher.launch("image/*"));
        btnSelectPdf.setOnClickListener(v -> pdfPickerLauncher.launch("application/pdf"));

        AlertDialog dialog = builder.create();

        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String title = editTitle.getText().toString().trim();
            String author = editAuthor.getText().toString().trim();
            String isbn = editIsbn.getText().toString().trim();
            Integer selectedYear = (Integer) spinnerYear.getSelectedItem();
            boolean available = checkAvailable.isChecked();
            String description = editDescription.getText().toString().trim();

            if (title.isEmpty() || author.isEmpty() || isbn.isEmpty() || selectedYear == null) {
                ToastHelper.showError(this, "Please fill all fields");
                return;
            }
            
            int year = selectedYear;

            v.setEnabled(false);

            if (selectedPdfUri != null) {
                    // Check file size (50MB limit)
                    long pdfSize = getFileSize(selectedPdfUri);
                    if (pdfSize > 50 * 1024 * 1024) { // 50MB
                        ToastHelper.showError(this, "PDF too large. Max 50MB.");
                        v.setEnabled(true);
                        return;
                    }

                    showLoading("Uploading PDF...");
                    MediaManager.get().upload(selectedPdfUri)
                            .unsigned("elib_preset") // Replace with your Unsigned Upload Preset
                            .option("resource_type", "raw") // Use raw to support files > 10MB
                            .callback(new UploadCallback() {
                                @Override
                                public void onStart(String requestId) {}
                                @Override
                                public void onProgress(String requestId, long bytes, long totalBytes) {}
                                @Override
                                public void onSuccess(String requestId, Map resultData) {
                                    String newPdfUrl = (String) resultData.get("secure_url");
                                    uploadCoverAndSave(book, title, author, isbn, year, available, description, newPdfUrl, dialog, v);
                                }
                                @Override
                                public void onError(String requestId, ErrorInfo error) {
                                    runOnUiThread(() -> {
                                        hideLoading();
                                        Toast.makeText(MainActivity.this, "PDF Upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                                        v.setEnabled(true);
                                    });
                                }
                                @Override
                                public void onReschedule(String requestId, ErrorInfo error) {}
                            }).dispatch();
                } else {
                    String currentPdfUrl = (book != null) ? book.getPdfUrl() : "";
                    uploadCoverAndSave(book, title, author, isbn, year, available, description, currentPdfUrl, dialog, v);
                }
        });

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showResultDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLoading(String message) {
        if (loadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(50, 50, 50, 50);
            layout.setGravity(Gravity.CENTER_VERTICAL);

            ProgressBar progressBar = new ProgressBar(this);
            progressBar.setIndeterminate(true);
            layout.addView(progressBar);

            loadingMessage = new TextView(this);
            loadingMessage.setText(message);
            loadingMessage.setPadding(30, 0, 0, 0);
            loadingMessage.setTextSize(16);
            layout.addView(loadingMessage);

            builder.setView(layout);
            loadingDialog = builder.create();
        }

        if (loadingMessage != null) {
            loadingMessage.setText(message);
        }
        
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void uploadCoverAndSave(Book book, String title, String author, String isbn, int year, boolean available, String description, String pdfUrl, AlertDialog dialog, View saveBtn) {
        if (selectedCoverUri != null) {
            runOnUiThread(() -> showLoading("Uploading Cover..."));
            MediaManager.get().upload(selectedCoverUri)
                    .unsigned("elib_preset") // Replace with your Unsigned Upload Preset
                    .option("resource_type", "image")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}
                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String newCoverUrl = (String) resultData.get("secure_url");
                            saveBookToFirestore(book, title, author, isbn, year, available, description, pdfUrl, newCoverUrl, dialog, saveBtn);
                        }
                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            runOnUiThread(() -> {
                                hideLoading();
                            showResultDialog("Upload Failed", "Cover Upload failed: " + error.getDescription());
                            saveBtn.setEnabled(true);
                            });
                        }
                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();
        } else {
            String currentCoverUrl = (book != null) ? book.getCoverUrl() : "";
            saveBookToFirestore(book, title, author, isbn, year, available, description, pdfUrl, currentCoverUrl, dialog, saveBtn);
        }
    }

    private void saveBookToFirestore(Book book, String title, String author, String isbn, int year, boolean available, String description, String pdfUrl, String coverUrl, AlertDialog dialog, View saveBtn) {
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("title", title);
        bookData.put("author", author);
        bookData.put("isbn", isbn);
        bookData.put("year", year);
        bookData.put("available", available);
        if (isAdmin && !description.isEmpty()) bookData.put("description", description);
        if (pdfUrl != null && !pdfUrl.isEmpty()) bookData.put("pdfUrl", pdfUrl);
        if (coverUrl != null && !coverUrl.isEmpty()) bookData.put("coverUrl", coverUrl);

        runOnUiThread(() -> {
            showLoading("Saving book...");
            if (book == null) {
                db.collection("books")
                        .add(bookData)
                        .addOnSuccessListener(documentReference -> {
                            hideLoading();
                            ToastHelper.showSuccess(this, "Book added successfully");
                            loadBooks();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            hideLoading();
                            ToastHelper.showError(this, "Error adding book: " + e.getMessage());
                            saveBtn.setEnabled(true);
                        });
            } else {
                db.collection("books")
                        .document(book.getId())
                        .update(bookData)
                        .addOnSuccessListener(aVoid -> {
                            hideLoading();
                            showResultDialog("Success", "Book updated successfully");
                            loadBooks();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            hideLoading();
                            showResultDialog("Error", "Error updating book: " + e.getMessage());
                            saveBtn.setEnabled(true);
                        });
            }
        });
    }

    private long getFileSize(Uri uri) {
        android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        long size = 0;
        if (cursor != null) {
            int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
            if (cursor.moveToFirst() && sizeIndex != -1) {
                size = cursor.getLong(sizeIndex);
            }
            cursor.close();
        }
        return size;
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
                        // Menu items are static now: Catalog, Borrow, Fines, Profile
                        // We can optionally hide/show items based on admin status if needed,
                        // but the user requested a specific 4-tab layout.
                        // For now, we'll keep all 4 visible and handle logic in OnItemSelectedListener.
                    }
                })
                .addOnFailureListener(e -> {
                    isAdmin = false;
                    adapter.setAdmin(false);
                    adapter.setShowAddCard(false);
                    // No menu changes needed on failure
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
                                ToastHelper.showSuccess(this, "Book deleted successfully");
                                loadBooks();
                            })
                            .addOnFailureListener(e -> {
                                ToastHelper.showError(this, "Error deleting book: " + e.getMessage());
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

