package com.elib.library;

import android.os.Bundle;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class AccountActivity extends AppCompatActivity {
    private TextView emailText;
    private TextView usernameText;
    private TextView bioText;
    private TextView memberSinceText;
    private TextView usernameHeaderText;
    private TextView roleText;
    private TextView initialText;
    private TextView statBorrowed;
    private TextView statReading;
    private TextView statCompleted;
    private TextView statReservations;
    private Button editBtn;
    private Button logoutBtn;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_account);
            emailText = findViewById(R.id.text_email);
            usernameText = findViewById(R.id.text_username);
            bioText = findViewById(R.id.text_bio);
            memberSinceText = findViewById(R.id.text_member_since);
            usernameHeaderText = findViewById(R.id.text_username_header);
            roleText = findViewById(R.id.text_role);
            initialText = findViewById(R.id.text_initial);
            statBorrowed = findViewById(R.id.stat_borrowed);
            statReading = findViewById(R.id.stat_reading);
            statCompleted = findViewById(R.id.stat_completed);
            statReservations = findViewById(R.id.stat_reservations);
            editBtn = findViewById(R.id.btn_edit);
            logoutBtn = findViewById(R.id.btn_logout);
            
            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_profile);
                bottomNav.setOnItemSelectedListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.nav_profile) {
                        return true;
                    } else if (id == R.id.nav_catalog) {
                        startActivity(new android.content.Intent(this, MainActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                    } else if (id == R.id.nav_borrow) {
                        startActivity(new android.content.Intent(this, BorrowActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                    } else if (id == R.id.nav_fines) {
                        startActivity(new android.content.Intent(this, FinesActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                    }
                    return true;
                });
            }

            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            if (auth.getCurrentUser() != null) {
                if (emailText != null) emailText.setText(auth.getCurrentUser().getEmail());
                
                // Set Role based on email
                String email = auth.getCurrentUser().getEmail();
                if ( roleText != null) {
                    if ("admin@gmail.com".equalsIgnoreCase(email)) {
                        roleText.setText("Admin");
                    } else {
                        roleText.setText("User");
                    }
                }

                String uid = auth.getCurrentUser().getUid();
                db.collection("users").document(uid).get()
                        .addOnSuccessListener(this::applyUserDoc)
                        .addOnFailureListener(e -> {
                            if (usernameText != null) usernameText.setText("");
                            if (usernameHeaderText != null) usernameHeaderText.setText("");
                            if (initialText != null) initialText.setText(getInitialFromEmail(auth.getCurrentUser().getEmail()));
                            if (memberSinceText != null) memberSinceText.setText("");
                        });
                fetchStats(uid);
            }
            if (editBtn != null) editBtn.setOnClickListener(v -> showEditDialog());
            if (logoutBtn != null) logoutBtn.setOnClickListener(v -> {
                auth.signOut();
                android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        } catch (Exception e) {
            e.printStackTrace();
            ToastHelper.showError(this, "Error initializing Account: " + e.getMessage());
            // Optional: navigate back safely if critical failure
        }
    }

    private void applyUserDoc(DocumentSnapshot doc) {
        String email = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "";
        String username = doc.exists() ? doc.getString("username") : null;
        String bio = doc.exists() ? doc.getString("bio") : null;
        Long createdAt = doc.exists() ? doc.getLong("createdAt") : null;
        if (emailText != null) emailText.setText(email != null ? email : "");
        if (usernameText != null) usernameText.setText(username != null ? username : "");
        if (usernameHeaderText != null) usernameHeaderText.setText(username != null ? username : "");
        if (bioText != null) bioText.setText(bio != null && !bio.isEmpty() ? bio : "No bio yet");
        String initial = username != null && !username.isEmpty()
                ? username.substring(0, 1).toUpperCase()
                : getInitialFromEmail(email);
        if (initialText != null) initialText.setText(initial);
        if (createdAt != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy");
            if (memberSinceText != null) memberSinceText.setText("Member since " + sdf.format(new java.util.Date(createdAt)));
        } else {
            if (memberSinceText != null) memberSinceText.setText("");
        }
    }

    private void fetchStats(String uid) {
        // Borrowed & Reading (assuming active borrows)
        db.collection("borrowed_books")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "borrowed")
                .get()
                .addOnSuccessListener(snapshots -> {
                    int count = snapshots.size();
                    if (statBorrowed != null) statBorrowed.setText(String.valueOf(count));
                    if (statReading != null) statReading.setText(String.valueOf(count));
                });

        // Completed (returned)
        db.collection("borrowed_books")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "returned")
                .get()
                .addOnSuccessListener(snapshots -> {
                    int count = snapshots.size();
                    if (statCompleted != null) statCompleted.setText(String.valueOf(count));
                });

        // Reservations (or Fines for now, checking active fines)
        db.collection("fines")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "unpaid")
                .get()
                .addOnSuccessListener(snapshots -> {
                    int count = snapshots.size();
                    if (statReservations != null) statReservations.setText(String.valueOf(count));
                });
    }

    private String getInitialFromEmail(String email) {
        if (email == null || email.isEmpty()) return "?";
        return email.substring(0, 1).toUpperCase();
    }

    private void showEditDialog() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.view.View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(view);
        EditText editUsername = view.findViewById(R.id.edit_username);
        EditText editBio = view.findViewById(R.id.edit_bio);
        if (usernameText != null) editUsername.setText(usernameText.getText().toString());
        if (bioText != null) editBio.setText("No bio yet".equals(bioText.getText().toString()) ? "" : bioText.getText().toString());
        AlertDialog dialog = builder.create();
        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String newUsername = editUsername.getText().toString().trim();
            String newBio = editBio.getText().toString().trim();
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            if (!newUsername.isEmpty()) data.put("username", newUsername);
            data.put("bio", newBio);
            db.collection("users").document(uid).update(data)
                    .addOnSuccessListener(aVoid -> {
                        if (usernameText != null) usernameText.setText(newUsername);
                        if (usernameHeaderText != null) usernameHeaderText.setText(newUsername);
                        if (bioText != null) bioText.setText(!newBio.isEmpty() ? newBio : "No bio yet");
                        if (initialText != null) initialText.setText(newUsername.isEmpty() ? getInitialFromEmail(auth.getCurrentUser().getEmail()) : newUsername.substring(0,1).toUpperCase());
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        dialog.dismiss();
                    });
        });
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
