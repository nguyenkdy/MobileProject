package com.example.mynoesapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    ImageView imgLogo;
    TextView txtTitle, txtTagline;
    Button btnSignIn, btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome); // liên kết đúng file XML bạn gửi

        // Ánh xạ View
        imgLogo = findViewById(R.id.imgLogo);
        txtTitle = findViewById(R.id.txtTitle);     // bạn có thể thêm id này trong XML
        txtTagline = findViewById(R.id.txtTagline); // hoặc thay bằng findViewById trực tiếp
        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignUp = findViewById(R.id.btnSignUp);

        // Animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        // Áp animation
        imgLogo.startAnimation(fadeIn);
        btnSignIn.startAnimation(slideUp);
        btnSignUp.startAnimation(slideUp);

        // Nếu bạn thêm id cho TextView thì animation sẽ đẹp hơn
        if (txtTitle != null) txtTitle.startAnimation(slideUp);
        if (txtTagline != null) txtTagline.startAnimation(slideUp);

        // Chuyển sang màn đăng nhập
        btnSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, SignInActivity.class);
            startActivity(intent);
        });

        // Chuyển sang màn đăng ký
        btnSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }
}
