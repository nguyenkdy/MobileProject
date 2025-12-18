package com.example.mynoesapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotesActivity extends AppCompatActivity {

    // ================= UI =================
    ImageButton btnOption, btnSetting, btnAdd;
    TextView txtFolderTitle;
    RecyclerView recyclerNotes;

    // ================= Firebase =================
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String uid;

    // ================= Adapters & Data =================
    NotesAdapter notesAdapter;
    FoldersAdapter foldersAdapter;

    List<Note> noteList;
    List<Folder> folderList;

    // ================= Firestore listeners =================
    ListenerRegistration notesListener;
    ListenerRegistration foldersListener;

    enum Mode { NOTES, FOLDERS }
    Mode currentMode = Mode.NOTES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notes);

        View root = findViewById(R.id.rootLayout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // ================= ÁNH XẠ VIEW =================
        btnOption = findViewById(R.id.btnOption);
        btnSetting = findViewById(R.id.btnSetting);
        btnAdd = findViewById(R.id.btnAdd);
        txtFolderTitle = findViewById(R.id.txtFolderTitle);
        recyclerNotes = findViewById(R.id.recyclerNotes);

        recyclerNotes.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotes.setItemAnimator(new DefaultItemAnimator());

        // ================= FIREBASE =================
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }
        uid = user.getUid();

        // ================= DATA =================
        noteList = new ArrayList<>();
        folderList = new ArrayList<>();

        notesAdapter = new NotesAdapter(noteList);
        foldersAdapter = new FoldersAdapter(folderList);

        recyclerNotes.setAdapter(notesAdapter);

        // ================= LOAD DEFAULT =================
        loadAllNotes();

        // ================= EVENTS =================
        btnOption.setOnClickListener(v -> showOptionMenu());
        btnSetting.setOnClickListener(v -> showSettingMenu());
        btnAdd.setOnClickListener(v -> showCreateMenu());
    }

    // ==================================================
    // LOAD NOTES
    // ==================================================
    private void loadAllNotes() {
        currentMode = Mode.NOTES;
        txtFolderTitle.setText("Tất cả ghi chú");
        recyclerNotes.setAdapter(notesAdapter);

        if (foldersListener != null) {
            foldersListener.remove();
            foldersListener = null;
        }

        if (notesListener != null) {
            notesListener.remove();
        }

        notesListener = db.collection("users")
                .document(uid)
                .collection("notes")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener(this, (value, error) -> {
                    if (error != null || value == null) return;

                    noteList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Note note = doc.toObject(Note.class);
                        if (note != null) {
                            note.id = doc.getId();
                            noteList.add(note);
                        }
                    }
                    notesAdapter.notifyDataSetChanged();
                });
    }

    // ==================================================
    // LOAD FOLDERS
    // ==================================================
    private void loadFolders() {
        currentMode = Mode.FOLDERS;
        txtFolderTitle.setText("Thư mục");
        recyclerNotes.setAdapter(foldersAdapter);

        if (notesListener != null) {
            notesListener.remove();
            notesListener = null;
        }

        if (foldersListener != null) {
            foldersListener.remove();
        }

        foldersListener = db.collection("users")
                .document(uid)
                .collection("folders")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener(this, (value, error) -> {
                    if (error != null || value == null) return;

                    folderList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Folder f = doc.toObject(Folder.class);
                        if (f != null) {
                            f.id = doc.getId();
                            folderList.add(f);
                        }
                    }
                    foldersAdapter.notifyDataSetChanged();
                });
    }

    // ==================================================
    // CREATE MENU (+)
    // ==================================================
    private void showCreateMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        var view = getLayoutInflater().inflate(R.layout.bottom_sheet_create, null);

        view.findViewById(R.id.optCreateNote).setOnClickListener(v -> {
            dialog.dismiss();
            createNewNote();
        });

        view.findViewById(R.id.optCreateFolder).setOnClickListener(v -> {
            dialog.dismiss();
            showCreateFolderDialog();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void createNewNote() {
        Map<String, Object> note = new HashMap<>();
        note.put("title", "Ghi chú mới");
        note.put("content", "");
        note.put("folderId", null);
        note.put("createdAt", Timestamp.now());
        note.put("updatedAt", Timestamp.now());

        db.collection("users")
                .document(uid)
                .collection("notes")
                .add(note);
    }

    private void showCreateFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tạo thư mục");

        EditText input = new EditText(this);
        input.setHint("Tên thư mục");
        builder.setView(input);

        builder.setPositiveButton("Tạo", (d, w) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) return;

            Map<String, Object> folder = new HashMap<>();
            folder.put("name", name);
            folder.put("createdAt", Timestamp.now());

            db.collection("users")
                    .document(uid)
                    .collection("folders")
                    .add(folder);
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    // ==================================================
    // OPTION MENU
    // ==================================================
    private void showOptionMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        var view = getLayoutInflater().inflate(R.layout.bottom_sheet_options, null);

        view.findViewById(R.id.optAllNotes).setOnClickListener(v -> {
            loadAllNotes();
            dialog.dismiss();
        });

        view.findViewById(R.id.optFolders).setOnClickListener(v -> {
            loadFolders();
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    // ==================================================
    // SETTING MENU
    // ==================================================
    private void showSettingMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        var view = getLayoutInflater().inflate(R.layout.bottom_sheet_setting, null);

        view.findViewById(R.id.optLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SharedPreferences pref = getSharedPreferences("MyNoteApp", MODE_PRIVATE);
            pref.edit().putBoolean("remember", false).apply();
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notesListener != null) notesListener.remove();
        if (foldersListener != null) foldersListener.remove();
    }
}
