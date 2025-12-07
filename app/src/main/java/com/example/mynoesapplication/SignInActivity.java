package com.example.mynoesapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignInActivity extends AppCompatActivity {

    EditText edtEmail, edtPassword;
    Button btnLogin;
    TextView txtGoToSignUp;
    CheckBox checkRemember;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signin);

        auth = FirebaseAuth.getInstance();

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtGoToSignUp = findViewById(R.id.txtGoToSignUp);
        checkRemember = findViewById(R.id.checkRemember);

        SharedPreferences pref = getSharedPreferences("MyNoteApp", MODE_PRIVATE);

        // ===== TỰ ĐIỀN EMAIL + PASS =====
        edtEmail.setText(pref.getString("saved_email", ""));
        edtPassword.setText(pref.getString("saved_pass", ""));

        // ===== AUTO LOGIN nếu remember = true =====
        if (pref.getBoolean("remember", false) && auth.getCurrentUser() != null) {
            startActivity(new Intent(SignInActivity.this, NotesActivity.class));
            finish();
        }

        // Chuyển sang Sign Up
        txtGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // XỬ LÝ LOGIN
        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass = edtPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter email & password", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            SharedPreferences.Editor editor = pref.edit();

                            // LƯU EMAIL + PASS
                            editor.putString("saved_email", email);
                            editor.putString("saved_pass", pass);

                            // LƯU TRẠNG THÁI REMEMBER
                            editor.putBoolean("remember", checkRemember.isChecked());

                            editor.apply();

                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignInActivity.this, NotesActivity.class));
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            finish();

                        } else {
                            Toast.makeText(this,
                                    "Login failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}

