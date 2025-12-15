package com.example.mynoesapplication;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.Dialog;
import android.view.ViewGroup;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.Folder.Folder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class NotesActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    List<Folder> folderList;
    FolderAdapter folderAdapter;


    Adapter adapter;

    ImageButton btnOption, btnSetting,btnAdd;
    TextView txtFolderTitle;
    private Animation rotateOpen;
    private Animation rotateClose;
    private Animation fromBottom;
    private Animation toBottom;
    private FloatingActionButton bttn_org,bttn_fun1,bttn_fun2;
    private Boolean clicked = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_note);

        // ÁNH XẠ VIEW
        btnOption = findViewById(R.id.btnOption);
        btnSetting = findViewById(R.id.btnSetting);

        txtFolderTitle = findViewById(R.id.txtFolderTitle);

        // MENU OPTION (Folder)
        btnOption.setOnClickListener(v -> showOptionMenu());

        // MENU SETTING
        btnSetting.setOnClickListener(v -> showSettingMenu());



        //BUTTONS FLOATING
        bttn_org = findViewById(R.id.floatingActionButton2);
        bttn_fun1 = findViewById(R.id.floatingActionButton3);
        bttn_fun2 = findViewById(R.id.floatingActionButton4);
        // ANIMATION FOR BUTTONS FLOATINGS
        rotateOpen  = AnimationUtils.loadAnimation(this, R.anim.rotate_open_animation);
        rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close_animation);
        fromBottom  = AnimationUtils.loadAnimation(this, R.anim.from_bottom_animation);
        toBottom    = AnimationUtils.loadAnimation(this, R.anim.to_bottom_animation);


        bttn_org.setOnClickListener(v->{
        Button_click();

        });
        bttn_fun1.setOnClickListener(v->{
            showSetNameFolder();
        });
        bttn_fun2.setOnClickListener(v->{

        });


        //ADAPDER
        recyclerView = findViewById(R.id.recyclerNotes);

        folderList = new ArrayList<>();

        folderAdapter = new FolderAdapter(folderList);


        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(folderAdapter);

        //1
        folderAdapter.setOnItemClickListener(folder -> {
            Intent intent = new Intent(NotesActivity.this, FolderEditingActivity.class);
            intent.putExtra("folderId", folder.getId());
            intent.putExtra("folderName", folder.getName());
            startActivity(intent);
        });




    }
    private void Button_click(){
        SetVisibiliity(clicked);
        SetAnimation(clicked);
        clicked = !clicked;
    }
    private void SetVisibiliity( Boolean clicked){
        if ( !clicked ) {
            bttn_fun1.setVisibility(View.VISIBLE);
            bttn_fun2.setVisibility(View.VISIBLE);
        }
        else {
            bttn_fun1.setVisibility(View.INVISIBLE);
            bttn_fun2.setVisibility(View.INVISIBLE);
        }
    }
    private void SetAnimation(Boolean clicked){
        if ( !clicked ) {
            bttn_fun1.startAnimation(fromBottom);
            bttn_fun2.startAnimation(fromBottom);
            bttn_org.startAnimation(rotateOpen);
        }
        else {
            bttn_fun1.startAnimation(toBottom);
            bttn_fun2.startAnimation(toBottom);
            bttn_org.startAnimation(rotateClose);
        }
    }

    private void Clickable(Boolean clicked){
        if ( !clicked ){
            bttn_fun1.setClickable(true);
            bttn_fun2.setClickable(true);
        }
        else {
            bttn_fun1.setClickable(false);
            bttn_fun2.setClickable(false);
        }
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
            dialog.dismiss();
        });

        // GHIM
        optPin.setOnClickListener(v -> {
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

    private void showSetNameFolder() {
        // Create a standard Dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.set_name_option);

        // Set the dialog to appear in the center with margins
        dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.radius);

        // Initialize views inside the dialog
        TextInputEditText edtName = dialog.findViewById(R.id.edtFolderName);
        Button btnCreate = dialog.findViewById(R.id.btnCreate);

        btnCreate.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();

            if (name.isEmpty()) {
                edtName.setError("Nhập tên thư mục");
                return;
            }

            // Add folder to RecyclerView
            folderList.add(new Folder(UUID.randomUUID().toString(), name));
            folderAdapter.notifyItemInserted(folderList.size() - 1);

            dialog.dismiss();
        });




        // Show the dialog
        dialog.show();
    }




}
