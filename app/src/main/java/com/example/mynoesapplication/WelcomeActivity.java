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
    Button btnSignIn, btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);


        imgLogo = findViewById(R.id.imgLogo);
        container = findViewById(R.id.container);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignUp = findViewById(R.id.btnSignUp);

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        Animation clickAnim = AnimationUtils.loadAnimation(this, R.anim.button_click);

        imgLogo.startAnimation(fadeIn);
        container.startAnimation(slideUp);

        // CLICK SIGN IN
        btnSignIn.setOnClickListener(v -> {
            v.startAnimation(clickAnim);

            // Delay nhỏ để animation click hiển thị rõ rồi mới chuyển trang
            v.postDelayed(() -> {
                startActivity(new Intent(WelcomeActivity.this, SignInActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }, 180); // bằng duration của button_click
        });

        // CLICK SIGN UP
        btnSignUp.setOnClickListener(v -> {
            v.startAnimation(clickAnim);

            v.postDelayed(() -> {
                startActivity(new Intent(WelcomeActivity.this, SignUpActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }, 180);
        });
    }
}
