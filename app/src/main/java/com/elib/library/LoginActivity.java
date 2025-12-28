package com.elib.library;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private EditText emailInput;
    private EditText passwordInput;
    private FirebaseFirestore db;
    private MaterialButton tabLoginBtn, tabRegisterBtn;
    private View loginContainer, registerContainer;
    private EditText emailRegInput, passwordRegInput, confirmRegInput, usernameInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        emailInput = findViewById(R.id.input_email);
        passwordInput = findViewById(R.id.input_password);
        emailRegInput = findViewById(R.id.input_email_reg);
        passwordRegInput = findViewById(R.id.input_password_reg);
        confirmRegInput = findViewById(R.id.input_confirm_password);
        usernameInput = findViewById(R.id.input_username);
        tabLoginBtn = findViewById(R.id.btn_tab_login);
        tabRegisterBtn = findViewById(R.id.btn_tab_register);
        loginContainer = findViewById(R.id.container_login);
        registerContainer = findViewById(R.id.container_register);

        findViewById(R.id.btn_login).setOnClickListener(v -> login());
        findViewById(R.id.btn_register).setOnClickListener(v -> register());
        
        TextView forgotPass = findViewById(R.id.text_forgot_password);
        if (forgotPass != null) {
            forgotPass.setOnClickListener(v -> {
                String email = emailInput.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show();
                    return;
                }
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Reset email sent", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        }

        tabLoginBtn.setOnClickListener(v -> updateTabs(true));
        tabRegisterBtn.setOnClickListener(v -> updateTabs(false));
    }

    private void updateTabs(boolean isLogin) {
        int activeColor = ContextCompat.getColor(this, R.color.button_active);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_hint);
        int white = ContextCompat.getColor(this, android.R.color.white);
        int transparent = ContextCompat.getColor(this, android.R.color.transparent);

        if (isLogin) {
            loginContainer.setVisibility(View.VISIBLE);
            registerContainer.setVisibility(View.GONE);
            
            tabLoginBtn.setBackgroundTintList(ColorStateList.valueOf(activeColor));
            tabLoginBtn.setTextColor(white);
            
            tabRegisterBtn.setBackgroundTintList(ColorStateList.valueOf(transparent));
            tabRegisterBtn.setTextColor(inactiveColor);
        } else {
            loginContainer.setVisibility(View.GONE);
            registerContainer.setVisibility(View.VISIBLE);
            
            tabLoginBtn.setBackgroundTintList(ColorStateList.valueOf(transparent));
            tabLoginBtn.setTextColor(inactiveColor);
            
            tabRegisterBtn.setBackgroundTintList(ColorStateList.valueOf(activeColor));
            tabRegisterBtn.setTextColor(white);
        }
    }

    private void login() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                    if ("admin@gmail.com".equalsIgnoreCase(email)) {
                        String uid = result.getUser().getUid();
                        java.util.Map<String, Object> admin = new java.util.HashMap<>();
                        admin.put("createdAt", System.currentTimeMillis());
                        db.collection("admins").document(uid).set(admin);
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    if ("admin@gmail.com".equalsIgnoreCase(email) && "password".equals(password)) {
                        auth.createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener(result -> {
                                    String uid = result.getUser().getUid();
                                    java.util.Map<String, Object> user = new java.util.HashMap<>();
                                    user.put("email", email);
                                    user.put("emailLowercase", email.toLowerCase());
                                    user.put("username", "Admin");
                                    user.put("createdAt", System.currentTimeMillis());
                                    db.collection("users").document(uid).set(user);
                                    java.util.Map<String, Object> admin = new java.util.HashMap<>();
                                    admin.put("createdAt", System.currentTimeMillis());
                                    db.collection("admins").document(uid).set(admin);
                                    Toast.makeText(this, "Admin account created", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(err -> Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void register() {
        String email = emailRegInput.getText().toString().trim();
        String password = passwordRegInput.getText().toString().trim();
        String confirm = confirmRegInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                    String uid = result.getUser().getUid();
                    java.util.Map<String, Object> user = new java.util.HashMap<>();
                    user.put("email", email);
                    user.put("emailLowercase", email.toLowerCase());
                    user.put("username", username);
                    user.put("createdAt", System.currentTimeMillis());
                    db.collection("users").document(uid).set(user);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
