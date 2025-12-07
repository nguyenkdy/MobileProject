package com.example.mynoesapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends AppCompatActivity {

    EditText edtEmailUp, edtPasswordUp, edtConfirmUp;
    Button btnSignUp;
    TextView txtGoToSignIn;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();

        edtEmailUp = findViewById(R.id.edtEmailUp);
        edtPasswordUp = findViewById(R.id.edtPasswordUp);
        edtConfirmUp = findViewById(R.id.edtConfirm);
        btnSignUp = findViewById(R.id.btnSignUp);
        txtGoToSignIn = findViewById(R.id.txtGoToSignIn);

        // Chuyển về trang đăng nhập
        txtGoToSignIn.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // Xử lý đăng ký Firebase
        btnSignUp.setOnClickListener(v -> {
            String email = edtEmailUp.getText().toString().trim();
            String pass = edtPasswordUp.getText().toString().trim();
            String confirm = edtConfirmUp.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                            // Quay lại Sign In
                            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                            finish();
                        } else {
                            Toast.makeText(this,
                                    "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
