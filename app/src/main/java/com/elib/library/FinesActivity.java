package com.elib.library;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FinesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FineAdapter adapter;
    private TextView totalText;
    private final List<Book> finedBooks = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fines);
        recyclerView = findViewById(R.id.recycler_fines);
        totalText = findViewById(R.id.text_total_fines);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FineAdapter(finedBooks);
        recyclerView.setAdapter(adapter);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        loadFines();
    }

    private void loadFines() {
        if (auth.getCurrentUser() == null) {
            totalText.setText("Total: Rs. 0");
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();
        db.collection("admins").document(uid).get()
                .addOnSuccessListener(doc -> {
                    boolean isAdmin = doc.exists() || "admin@gmail.com".equalsIgnoreCase(email);
                    if (isAdmin) {
                        finedBooks.clear();
                        adapter.notifyDataSetChanged();
                        totalText.setText("Total: Rs. 0");
                        return;
                    }
                    db.collection("books")
                            .whereEqualTo("lastBorrowerId", uid)
                            .get()
                            .addOnCompleteListener(task -> {
                                finedBooks.clear();
                                double total = 0.0;
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot d : task.getResult()) {
                                        Book b = d.toObject(Book.class);
                                        b.setId(d.getId());
                                        Double f = b.getFine() != null ? b.getFine() : 0.0;
                                        if (f > 0) total += f;
                                        if (b.getReturnDate() != null) finedBooks.add(b);
                                    }
                                }
                                finedBooks.sort((a, b2) -> {
                                    Long ar = a.getReturnDate();
                                    Long br = b2.getReturnDate();
                                    if (ar == null && br == null) return 0;
                                    if (ar == null) return 1;
                                    if (br == null) return -1;
                                    return Long.compare(br, ar);
                                });
                                adapter.notifyDataSetChanged();
                                totalText.setText("Total: Rs. " + total);
                            });
                })
                .addOnFailureListener(e -> {
                    db.collection("books")
                            .whereEqualTo("lastBorrowerId", uid)
                            .get()
                            .addOnCompleteListener(task -> {
                                finedBooks.clear();
                                double total = 0.0;
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot d : task.getResult()) {
                                        Book b = d.toObject(Book.class);
                                        b.setId(d.getId());
                                        Double f = b.getFine() != null ? b.getFine() : 0.0;
                                        if (f > 0) total += f;
                                        if (b.getReturnDate() != null) finedBooks.add(b);
                                    }
                                }
                                finedBooks.sort((a, b2) -> {
                                    Long ar = a.getReturnDate();
                                    Long br = b2.getReturnDate();
                                    if (ar == null && br == null) return 0;
                                    if (ar == null) return 1;
                                    if (br == null) return -1;
                                    return Long.compare(br, ar);
                                });
                                adapter.notifyDataSetChanged();
                                totalText.setText("Total: Rs. " + total);
                            });
                });
    }
}
