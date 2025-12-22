package com.example.mynoesapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends AppCompatActivity {

    // ===== UI =====
    TextInputEditText edtEmailUp, edtPasswordUp, edtConfirmUp;
    TextInputLayout layoutPassword, layoutConfirm;
    MaterialButton btnSignUp;
    TextView txtGoToSignIn;

    // ===== Firebase =====
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // ===== Firebase =====
        auth = FirebaseAuth.getInstance();

        // ===== FIND VIEW =====
        edtEmailUp = findViewById(R.id.edtEmailUp);
        edtPasswordUp = findViewById(R.id.edtPasswordUp);
        edtConfirmUp = findViewById(R.id.edtConfirm);

        layoutPassword = findViewById(R.id.layoutPassword);
        layoutConfirm = findViewById(R.id.layoutConfirm);

        btnSignUp = findViewById(R.id.btnSignUp);
        txtGoToSignIn = findViewById(R.id.txtGoToSignIn);

        // ===== GO TO SIGN IN =====
        txtGoToSignIn.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });

        // ===== SIGN UP =====
        btnSignUp.setOnClickListener(v -> validateAndRegister());
    }

    // ================= VALIDATE + REGISTER =================
    private void validateAndRegister() {

        String email = edtEmailUp.getText() != null
                ? edtEmailUp.getText().toString().trim()
                : "";

        String password = edtPasswordUp.getText() != null
                ? edtPasswordUp.getText().toString().trim()
                : "";

        String confirm = edtConfirmUp.getText() != null
                ? edtConfirmUp.getText().toString().trim()
                : "";

        // Reset error
        layoutPassword.setError(null);
        layoutConfirm.setError(null);

        // ===== CHECK EMPTY =====
        if (TextUtils.isEmpty(email)
                || TextUtils.isEmpty(password)
                || TextUtils.isEmpty(confirm)) {

            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== RULE: PASSWORD >= 6 =====
        if (password.length() < 6) {
            layoutPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            edtPasswordUp.requestFocus();
            return;
        }

        // ===== CONFIRM PASSWORD =====
        if (!password.equals(confirm)) {
            layoutConfirm.setError("Mật khẩu xác nhận không khớp");
            edtConfirmUp.requestFocus();
            return;
        }

        // ===== FIREBASE SIGN UP =====
        btnSignUp.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnSignUp.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Tạo tài khoản thành công", Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                        overridePendingTransition(
                                R.anim.slide_in_left,
                                R.anim.slide_out_right
                        );
                        finish();

                    } else {
                        Toast.makeText(
                                this,
                                task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Đăng ký thất bại",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}
