package com.elib.library;

import android.os.Bundle;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class AccountActivity extends AppCompatActivity {
    private TextView emailText;
    private TextView usernameText;
    private TextView bioText;
    private TextView memberSinceText;
    private TextView usernameHeaderText;
    private TextView initialText;
    private Button editBtn;
    private Button logoutBtn;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        emailText = findViewById(R.id.text_email);
        usernameText = findViewById(R.id.text_username);
        bioText = findViewById(R.id.text_bio);
        memberSinceText = findViewById(R.id.text_member_since);
        usernameHeaderText = findViewById(R.id.text_username_header);
        initialText = findViewById(R.id.text_initial);
        editBtn = findViewById(R.id.btn_edit);
        logoutBtn = findViewById(R.id.btn_logout);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() != null) {
            emailText.setText(auth.getCurrentUser().getEmail());
            String uid = auth.getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(this::applyUserDoc)
                    .addOnFailureListener(e -> {
                        usernameText.setText("");
                        usernameHeaderText.setText("");
                        initialText.setText(getInitialFromEmail(auth.getCurrentUser().getEmail()));
                        memberSinceText.setText("");
                    });
        }
        editBtn.setOnClickListener(v -> showEditDialog());
        logoutBtn.setOnClickListener(v -> {
            auth.signOut();
            android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void applyUserDoc(DocumentSnapshot doc) {
        String email = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "";
        String username = doc.exists() ? doc.getString("username") : null;
        String bio = doc.exists() ? doc.getString("bio") : null;
        Long createdAt = doc.exists() ? doc.getLong("createdAt") : null;
        emailText.setText(email != null ? email : "");
        usernameText.setText(username != null ? username : "");
        usernameHeaderText.setText(username != null ? username : "");
        bioText.setText(bio != null && !bio.isEmpty() ? bio : "No bio yet");
        String initial = username != null && !username.isEmpty()
                ? username.substring(0, 1).toUpperCase()
                : getInitialFromEmail(email);
        initialText.setText(initial);
        if (createdAt != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy");
            memberSinceText.setText(sdf.format(new java.util.Date(createdAt)));
        } else {
            memberSinceText.setText("");
        }
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
        editUsername.setText(usernameText.getText().toString());
        editBio.setText("No bio yet".equals(bioText.getText().toString()) ? "" : bioText.getText().toString());
        AlertDialog dialog = builder.create();
        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String newUsername = editUsername.getText().toString().trim();
            String newBio = editBio.getText().toString().trim();
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            if (!newUsername.isEmpty()) data.put("username", newUsername);
            data.put("bio", newBio);
            db.collection("users").document(uid).update(data)
                    .addOnSuccessListener(aVoid -> {
                        usernameText.setText(newUsername);
                        usernameHeaderText.setText(newUsername);
                        bioText.setText(!newBio.isEmpty() ? newBio : "No bio yet");
                        initialText.setText(newUsername.isEmpty() ? getInitialFromEmail(auth.getCurrentUser().getEmail()) : newUsername.substring(0,1).toUpperCase());
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
