package com.elib.library;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FinesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FineAdapter adapter;
    private TextView totalText;
    private EditText inputDays, inputRate;
    private Button btnCalculate;
    private final List<Book> finedBooks = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private double totalOutstanding = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fines);

        // Initialize Views
        recyclerView = findViewById(R.id.recycler_fines);
        totalText = findViewById(R.id.text_total_outstanding);
        inputDays = findViewById(R.id.input_calc_days);
        inputRate = findViewById(R.id.input_calc_rate);
        btnCalculate = findViewById(R.id.btn_calculate);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FineAdapter(finedBooks);
        recyclerView.setAdapter(adapter);

        // Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Setup Navigation
        bottomNav.setSelectedItemId(R.id.nav_fines);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_fines) {
                return true;
            } else if (id == R.id.nav_catalog) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_borrow) {
                startActivity(new Intent(this, BorrowActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, AccountActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return true;
        });

        // Setup Calculator
        btnCalculate.setOnClickListener(v -> calculateFine());

        // Show skeleton loading immediately
        recyclerView.post(() -> {
            adapter.setLoading(true);
        });

        loadFines();
    }

    private void showResultDialog(String title, String message) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void calculateFine() {
        String daysStr = inputDays.getText().toString().trim();
        String rateStr = inputRate.getText().toString().trim();

        if (daysStr.isEmpty() || rateStr.isEmpty()) {
            ToastHelper.showError(this, "Please enter days and rate");
            return;
        }

        try {
            int days = Integer.parseInt(daysStr);
            double rate = Double.parseDouble(rateStr);
            double fine = days * rate;
            showResultDialog("Calculation Result", "Calculated Fine: ₱ " + String.format("%.2f", fine));
        } catch (NumberFormatException e) {
            ToastHelper.showError(this, "Invalid input");
        }
    }

    private void loadFines() {
        if (auth.getCurrentUser() == null) {
            totalText.setText("$0.00");
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        
        // Load currently borrowed books that are overdue
        db.collection("books")
                .whereEqualTo("borrowerId", uid)
                .get()
                .addOnCompleteListener(task -> {
                    finedBooks.clear();
                    totalOutstanding = 0.0;
                    if (task.isSuccessful()) {
                        long now = System.currentTimeMillis();
                        for (QueryDocumentSnapshot d : task.getResult()) {
                            Book b = d.toObject(Book.class);
                            b.setId(d.getId());
                            
                            Long due = b.getDueDate();
                            if (due != null && now > due) {
                                // Overdue
                                long diff = now - due;
                                long days = diff / (24 * 60 * 60 * 1000);
                                if (days > 0) {
                                    // Calculate dynamic fine for display
                                    double fine = days * 10.0; // Assuming ₱ 10 per day as default or fetched from somewhere
                                    
                                    b.setFine(fine); // Temporarily set fine for adapter
                                    finedBooks.add(b);
                                    totalOutstanding += fine;
                                }
                            }
                        }
                    }
                    
                    adapter.notifyDataSetChanged();
                    totalText.setText("₱ " + String.format("%.2f", totalOutstanding));
                    
                    // Hide skeleton loading
                    adapter.setLoading(false);
                    
                    // Show empty state if no overdue books
                    if (finedBooks.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                });
    }
    
    private void showEmptyState() {
        View emptyView = getLayoutInflater().inflate(R.layout.empty_fines, null);
        ((ViewGroup) recyclerView.getParent()).addView(emptyView);
        recyclerView.setVisibility(View.GONE);
    }
    
    private void hideEmptyState() {
        ViewGroup parent = (ViewGroup) recyclerView.getParent();
        View emptyView = parent.findViewById(R.id.empty_fines);
        if (emptyView != null) {
            parent.removeView(emptyView);
        }
        recyclerView.setVisibility(View.VISIBLE);
    }
}
