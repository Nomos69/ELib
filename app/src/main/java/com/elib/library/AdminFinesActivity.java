package com.elib.library;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminFinesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private UserFineAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final List<UserFine> userFines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_fines);
        recyclerView = findViewById(R.id.recycler_admin_fines);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserFineAdapter(userFines);
        recyclerView.setAdapter(adapter);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_fines);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_fines) {
                return true;
            } else if (id == R.id.nav_catalog) {
                startActivity(new android.content.Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_borrow) {
                startActivity(new android.content.Intent(this, BorrowActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new android.content.Intent(this, AccountActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return true;
        });

        loadUserFines();
    }

    private void loadUserFines() {
        db.collection("books")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Double> totals = new HashMap<>();
                    for (QueryDocumentSnapshot d : snapshot) {
                        Book b = d.toObject(Book.class);
                        String uid = b.getLastBorrowerId();
                        Double f = b.getFine();
                        if (uid != null && f != null && f > 0) {
                            totals.put(uid, totals.getOrDefault(uid, 0.0) + f);
                        }
                    }
                    if (totals.isEmpty()) {
                        userFines.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }
                    db.collection("users").get()
                            .addOnSuccessListener(usersSnap -> {
                                Map<String, Map<String, Object>> users = new HashMap<>();
                                for (QueryDocumentSnapshot u : usersSnap) {
                                    users.put(u.getId(), u.getData());
                                }
                                userFines.clear();
                                for (Map.Entry<String, Double> e : totals.entrySet()) {
                                    String uid = e.getKey();
                                    Double total = e.getValue();
                                    Map<String, Object> info = users.get(uid);
                                    String name = info != null ? (String) info.get("username") : "Unknown";
                                    String email = info != null ? (String) info.get("email") : "";
                                    userFines.add(new UserFine(uid, name, email, total));
                                }
                                userFines.sort((a, b) -> Double.compare(b.totalFine, a.totalFine));
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(err -> {
                                ToastHelper.showError(this, "Error loading users: " + err.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    ToastHelper.showError(this, "Error loading fines: " + e.getMessage());
                });
    }
}

class UserFine {
    final String uid;
    final String name;
    final String email;
    final Double totalFine;
    UserFine(String uid, String name, String email, Double totalFine) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.totalFine = totalFine != null ? totalFine : 0.0;
    }
}
