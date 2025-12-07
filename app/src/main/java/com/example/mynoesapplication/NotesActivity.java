package com.example.mynoesapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;

public class NotesActivity extends AppCompatActivity {

    ImageButton btnOption, btnSetting;
    TextView txtFolderTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notes);

        // ÁNH XẠ VIEW
        btnOption = findViewById(R.id.btnOption);
        btnSetting = findViewById(R.id.btnSetting);
        txtFolderTitle = findViewById(R.id.txtFolderTitle);

        // MENU OPTION (Folder)
        btnOption.setOnClickListener(v -> showOptionMenu());

        // MENU SETTING
        btnSetting.setOnClickListener(v -> showSettingMenu());
    }

    // ===========================================
    //   MENU OPTION (Tất cả ghi chú / chia sẻ / ...)
    // ===========================================
    private void showOptionMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(NotesActivity.this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_options, null);

        LinearLayout optAll = view.findViewById(R.id.optAllNotes);
        LinearLayout optShared = view.findViewById(R.id.optShared);
        LinearLayout optTrash = view.findViewById(R.id.optTrash);
        LinearLayout optFolders = view.findViewById(R.id.optFolders);

        optAll.setOnClickListener(v -> {
            txtFolderTitle.setText("Tất cả ghi chú");
            dialog.dismiss();
        });

        optShared.setOnClickListener(v -> {
            txtFolderTitle.setText("Ghi chú chia sẻ");
            dialog.dismiss();
        });

        optTrash.setOnClickListener(v -> {
            txtFolderTitle.setText("Thùng rác");
            dialog.dismiss();
        });

        optFolders.setOnClickListener(v -> {
            txtFolderTitle.setText("Thư mục");
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    // ===========================================
    //   MENU SETTING (Sửa / Ghim / Logout)
    // ===========================================
    private void showSettingMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(NotesActivity.this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_setting, null);

        LinearLayout optEdit = view.findViewById(R.id.optEdit);
        LinearLayout optPin = view.findViewById(R.id.optPin);
        LinearLayout optLogout = view.findViewById(R.id.optLogout);

        // SỬA
        optEdit.setOnClickListener(v -> {
            // TODO: xử lý
            dialog.dismiss();
        });

        // GHIM
        optPin.setOnClickListener(v -> {
            // TODO: xử lý
            dialog.dismiss();
        });

        // ĐĂNG XUẤT
        optLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            SharedPreferences pref = getSharedPreferences("MyNoteApp", MODE_PRIVATE);
            pref.edit().putBoolean("remember", false).apply();

            startActivity(new Intent(NotesActivity.this, SignInActivity.class));
            finish();
        });

        dialog.setContentView(view);
        dialog.show();
    }
}
