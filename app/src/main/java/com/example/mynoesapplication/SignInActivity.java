package com.example.mynoesapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN";

    EditText edtEmail, edtPassword;
    Button btnLogin;
    TextView txtGoToSignUp;
    CheckBox checkRemember;

    TextView txtForgotPassword;

    FirebaseAuth auth;
    boolean isLoggingIn = false;

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
        txtForgotPassword = findViewById(R.id.txtForgotPassword);

        txtForgotPassword.setOnClickListener(v -> {

            String email = edtEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(
                        this,
                        "Vui lòng nhập email để khôi phục mật khẩu",
                        Toast.LENGTH_LONG
                ).show();
                edtEmail.requestFocus();
                return;
            }

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {
                            Toast.makeText(
                                    this,
                                    "Đã gửi email đặt lại mật khẩu. Vui lòng kiểm tra hộp thư.",
                                    Toast.LENGTH_LONG
                            ).show();
                        } else {
                            Toast.makeText(
                                    this,
                                    task.getException() != null
                                            ? task.getException().getMessage()
                                            : "Không thể gửi email",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });


        SharedPreferences pref = getSharedPreferences("MyNoteApp", MODE_PRIVATE);

        // ===== Fill saved data =====
        edtEmail.setText(pref.getString("saved_email", ""));
        edtPassword.setText(pref.getString("saved_pass", ""));
        checkRemember.setChecked(pref.getBoolean("remember", false));

        // ===== SAFE AUTO LOGIN =====
        FirebaseUser currentUser = auth.getCurrentUser();
        boolean remember = pref.getBoolean("remember", false);

        Log.d(TAG, "remember=" + remember + ", user=" + (currentUser != null));

        if (remember && currentUser != null) {

            if (currentUser.isEmailVerified()) {
                Log.d(TAG, "AUTO LOGIN VERIFIED -> NotesActivity");
                startActivity(new Intent(this, NotesActivity.class));
                finish();
                return;
            } else {
                // ❌ chưa verify → logout
                auth.signOut();
                Log.d(TAG, "AUTO LOGIN BLOCKED (email not verified)");
            }
        }

        // ===== Go to SignUp =====
        txtGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // ===== LOGIN BUTTON =====
        btnLogin.setOnClickListener(v -> {
            if (isLoggingIn) return;

            String email = edtEmail.getText().toString().trim();
            String pass = edtPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter email & password", Toast.LENGTH_SHORT).show();
                return;
            }

            isLoggingIn = true;
            btnLogin.setEnabled(false);

            Log.d(TAG, "signInWithEmailAndPassword()");

            auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {

                        isLoggingIn = false;
                        btnLogin.setEnabled(true);

                        Log.d(TAG, "onComplete called");
                        Log.d(TAG, "success=" + task.isSuccessful());

                        if (task.getException() != null) {
                            Log.e(TAG, "error", task.getException());
                        }

                        if (task.isSuccessful()) {

                            FirebaseUser user = auth.getCurrentUser();

                            if (user == null) {
                                Toast.makeText(this, "Lỗi đăng nhập", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // ❌ CHƯA VERIFY EMAIL
                            if (!user.isEmailVerified()) {

                                Toast.makeText(
                                        this,
                                        "Email chưa được xác nhận. Vui lòng kiểm tra hộp thư.",
                                        Toast.LENGTH_LONG
                                ).show();

                                auth.signOut();
                                return;
                            }

                            // ✅ ĐÃ VERIFY → SAVE REMEMBER
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putString("saved_email", email);
                            editor.putString("saved_pass", pass);
                            editor.putBoolean("remember", checkRemember.isChecked());
                            editor.apply();

                            Log.d(TAG, "LOGIN OK (VERIFIED) -> NotesActivity");

                            startActivity(new Intent(this, NotesActivity.class));
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            finish();
                        }
                        else {
                            String msg = "Login failed";
                            if (task.getException() != null &&
                                    task.getException().getMessage() != null) {
                                msg = task.getException().getMessage();
                            }
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
