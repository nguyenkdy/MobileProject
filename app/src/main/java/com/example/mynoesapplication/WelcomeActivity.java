package com.example.mynoesapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    ImageView imgLogo;
    LinearLayout container;
    Button btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // ===== FIND VIEW =====
        imgLogo = findViewById(R.id.imgLogo);
        container = findViewById(R.id.container);
        btnStart = findViewById(R.id.btnStart);

        // ===== LOAD ANIMATIONS =====
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        Animation clickAnim = AnimationUtils.loadAnimation(this, R.anim.button_click);

        // ===== START ANIMATION =====
        imgLogo.startAnimation(fadeIn);
        container.startAnimation(slideUp);

        // ===== CLICK "BẮT ĐẦU" =====
        btnStart.setOnClickListener(v -> {
            v.startAnimation(clickAnim);

            // Delay để animation click chạy xong
            v.postDelayed(() -> {
                Intent intent = new Intent(WelcomeActivity.this, SignInActivity.class);
                startActivity(intent);

                // Animation chuyển màn
                overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                );

                // Không quay lại Welcome khi back
                finish();
            }, 180);
        });
    }
}
