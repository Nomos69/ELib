package com.elib.library;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        android.view.View icon = findViewById(R.id.splash_icon);
        if (icon != null) {
            android.view.animation.Animation fadeIn = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in);
            icon.startAnimation(fadeIn);
        }
        android.view.View overlay = findViewById(R.id.splash_overlay);
        if (overlay != null) {
            android.view.animation.AlphaAnimation overlayAnim = new android.view.animation.AlphaAnimation(0f, 0.35f);
            overlayAnim.setDuration(700);
            overlayAnim.setFillAfter(true);
            overlay.startAnimation(overlayAnim);
        }
        android.os.Handler handler = new android.os.Handler();
        handler.postDelayed(() -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            Class<?> next = (auth.getCurrentUser() == null) ? LoginActivity.class : MainActivity.class;
            android.content.Intent intent = new android.content.Intent(this, next);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, 1200);
    }
}
