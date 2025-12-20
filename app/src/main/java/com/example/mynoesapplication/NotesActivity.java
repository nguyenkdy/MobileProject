package com.example.mynoesapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.mynoesapplication.Fragment.*;
import android.view.View;
import android.widget.FrameLayout;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class NotesActivity extends AppCompatActivity {

    // ================= UI =================
    ImageButton btnOption, btnSetting, btnAdd;
    TextView txtFolderTitle;
    RecyclerView recyclerNotes;
    ImageButton btnChatbot;
    FrameLayout chatContainer;

    // ================= Firebase =================
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String uid;

    // ================= Data =================
    final List<Note> noteList = new ArrayList<>();
    final List<Folder> folderList = new ArrayList<>();

    NotesAdapter notesAdapter;
    FoldersAdapter foldersAdapter;

    ListenerRegistration notesListener;
    ListenerRegistration foldersListener;

    LinearLayout bottomActionBar;

    // ✅ BƯỚC 5: 3 NÚT ACTION
    TextView btnSelectAll, btnMove, btnDelete;

    // ================= STATE =================
    private static final String ROOT = "ROOT";
    String currentFolderId = ROOT;

    // 🔥 NEW: đang xem gì? NOTES / FOLDERS
    private enum ScreenMode { NOTES, FOLDERS }
    private ScreenMode currentMode = ScreenMode.NOTES;

    boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notes);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // ================= UI =================
        btnOption = findViewById(R.id.btnOption);
        btnSetting = findViewById(R.id.btnSetting);
        btnAdd = findViewById(R.id.btnAdd);
        txtFolderTitle = findViewById(R.id.txtFolderTitle);
        recyclerNotes = findViewById(R.id.recyclerNotes);
        btnChatbot = findViewById(R.id.btnOpenChat);
        chatContainer = findViewById(R.id.chat_container);


        recyclerNotes.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotes.setItemAnimator(new DefaultItemAnimator());

        // ================= Firebase =================
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }
        uid = Objects.requireNonNull(user).getUid();

        // ================= Adapters =================
        notesAdapter = new NotesAdapter(noteList);
        foldersAdapter = new FoldersAdapter(folderList, folder -> {
            txtFolderTitle.setText(folder.name);
            loadNotesInFolder(folder.id);
        });
        foldersAdapter.setUid(uid);

        recyclerNotes.setAdapter(notesAdapter);

        // ================= DEFAULT =================
        loadAllNotes();

        // ================= EVENTS =================
        btnSetting.setOnClickListener(v -> showSettingMenu());
        btnAdd.setOnClickListener(v -> showCreateMenu());

        ImageButton btnEdit = findViewById(R.id.btnEdit);
        btnEdit.setOnClickListener(v -> toggleEditMode());

        bottomActionBar = findViewById(R.id.bottomActionBar);

        // ✅ BƯỚC 5: ánh xạ 3 nút
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnMove = findViewById(R.id.btnMove);
        btnDelete = findViewById(R.id.btnDeleteAll);

        // ✅ BƯỚC 5: click action
        if (btnSelectAll != null) btnSelectAll.setOnClickListener(v -> onSelectAllClicked());
        if (btnMove != null) btnMove.setOnClickListener(v -> onMoveClicked());
        if (btnDelete != null) btnDelete.setOnClickListener(v -> onDeleteClicked());
        if (btnChatbot != null) {
            btnChatbot.setOnClickListener(v -> {
                FragmentManager fm = getSupportFragmentManager();
                Fragment existing = fm.findFragmentByTag("chat_overlay");

                if (chatContainer != null && chatContainer.getVisibility() == View.VISIBLE) {
                    if (existing != null) {
                        fm.beginTransaction().remove(existing).commitAllowingStateLoss();
                    }
                    chatContainer.setVisibility(View.GONE);
                } else {
                    fm.beginTransaction()
                            .replace(R.id.chat_container, new com.example.mynoesapplication.Fragment.ChatFragment(), "chat_overlay")
                            .commitAllowingStateLoss();
                    if (chatContainer != null) chatContainer.setVisibility(View.VISIBLE);
                }
            });
        }

// Register back callback outside the btnChatbot null-check so it always runs
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (chatContainer != null && chatContainer.getVisibility() == View.VISIBLE) {
                    Fragment f = getSupportFragmentManager().findFragmentByTag("chat_overlay");
                    if (f != null) getSupportFragmentManager().beginTransaction().remove(f).commitAllowingStateLoss();
                    chatContainer.setVisibility(View.GONE);
                    return;
                }

                if (isEditMode) {
                    toggleEditMode();
                    return;
                }

                if (!ROOT.equals(currentFolderId)) {
                    loadFolders();
                    return;
                }

                finish();
            }
        });
        updateBottomActionBar();
    }

    // ==================================================
    // LEFT BUTTON
    // ==================================================
    private void updateLeftButton() {
        if (!ROOT.equals(currentFolderId)) {
            btnOption.setImageResource(R.drawable.ic_back);
            btnOption.setOnClickListener(v -> loadFolders());
        } else {
            btnOption.setImageResource(R.drawable.ic_option);
            btnOption.setOnClickListener(v -> showOptionMenu());
        }
    }

    // ==================================================
    // LOAD ALL NOTES (deleted = false)
    // ==================================================
    private void loadAllNotes() {
        currentMode = ScreenMode.NOTES;
        currentFolderId = ROOT;
        txtFolderTitle.setText("Tất cả ghi chú");

        recyclerNotes.setAdapter(notesAdapter);
        updateLeftButton();
        removeListeners();

        noteList.clear();
        notesAdapter.notifyDataSetChanged();

        notesListener = db.collection("users")
                .document(uid)
                .collection("notes")
                .whereEqualTo("deleted", false)
                .addSnapshotListener(this, (value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value == null) return;
                    applyNotesSnapshot(value);
                });
    }

    // ==================================================
    // LOAD FOLDERS (deleted = false)
    // ==================================================
    private void loadFolders() {
        currentMode = ScreenMode.FOLDERS;
        currentFolderId = ROOT;
        txtFolderTitle.setText("Thư mục");

        recyclerNotes.setAdapter(foldersAdapter);
        updateLeftButton();
        removeListeners();

        folderList.clear();
        foldersAdapter.notifyDataSetChanged();

        foldersListener = db.collection("users")
                .document(uid)
                .collection("folders")
                .whereEqualTo("deleted", false)
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

                    Collections.sort(folderList, (a, b) -> {
                        if (a.createdAt == null && b.createdAt == null) return 0;
                        if (a.createdAt == null) return 1;
                        if (b.createdAt == null) return -1;
                        return a.createdAt.compareTo(b.createdAt);
                    });

                    foldersAdapter.notifyDataSetChanged();
                });
    }

    // ==================================================
    // LOAD NOTES IN FOLDER (deleted = false)
    // ==================================================
    private void loadNotesInFolder(String folderId) {
        currentMode = ScreenMode.NOTES;
        currentFolderId = folderId;

        recyclerNotes.setAdapter(notesAdapter);
        updateLeftButton();
        removeListeners();

        noteList.clear();
        notesAdapter.notifyDataSetChanged();

        notesListener = db.collection("users")
                .document(uid)
                .collection("notes")
                .whereEqualTo("folderId", folderId)
                .whereEqualTo("deleted", false)
                .addSnapshotListener(this, (value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Load notes trong folder lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value == null) return;
                    applyNotesSnapshot(value);
                });
    }

    // ==================================================
    // APPLY SNAPSHOT (SORT updatedAt DESC)
    // ==================================================
    private void applyNotesSnapshot(QuerySnapshot value) {
        noteList.clear();

        for (DocumentSnapshot doc : value.getDocuments()) {
            Note n = doc.toObject(Note.class);
            if (n != null) {
                n.id = doc.getId();
                noteList.add(n);
            }
        }

        Collections.sort(noteList, (a, b) -> {
            if (a.updatedAt == null && b.updatedAt == null) return 0;
            if (a.updatedAt == null) return 1;
            if (b.updatedAt == null) return -1;
            return b.updatedAt.compareTo(a.updatedAt);
        });

        notesAdapter.notifyDataSetChanged();
    }


    // ==================================================
    // CREATE MENU
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

    // ==================================================
    // CREATE NOTE (FIX: refresh đúng mode)
    // ==================================================
    private void createNewNote() {
        Timestamp now = Timestamp.now();

        Map<String, Object> data = new HashMap<>();
        data.put("title", "Ghi chú mới");
        data.put("content", "");
        data.put("folderId", currentFolderId);
        data.put("createdAt", now);
        data.put("updatedAt", now);
        data.put("deleted", false);
        data.put("deletedAt", null);

        db.collection("users")
                .document(uid)
                .collection("notes")
                .add(data)
                .addOnSuccessListener(doc -> {
                    refreshCurrentListAfterCreate();

                    Intent i = new Intent(this, EditNoteActivity.class);
                    i.putExtra("noteId", doc.getId());
                    startActivity(i);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Tạo ghi chú lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ==================================================
    // CREATE FOLDER (FIX: nếu đang ở folder mode thì hiện liền)
    // ==================================================
    private void showCreateFolderDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Tạo thư mục");

        EditText input = new EditText(this);
        input.setHint("Tên thư mục");
        b.setView(input);

        b.setPositiveButton("Tạo", (d, w) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) return;

            Map<String, Object> folder = new HashMap<>();
            folder.put("name", name);
            folder.put("createdAt", Timestamp.now());
            folder.put("deleted", false);
            folder.put("deletedAt", null);

            db.collection("users")
                    .document(uid)
                    .collection("folders")
                    .add(folder)
                    .addOnSuccessListener(doc -> {
                        if (currentMode == ScreenMode.FOLDERS) {
                            loadFolders();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Tạo thư mục lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        b.setNegativeButton("Hủy", null);
        b.show();
    }

    // ==================================================
    // Refresh đúng danh sách sau khi tạo note
    // ==================================================
    private void refreshCurrentListAfterCreate() {
        if (currentMode == ScreenMode.FOLDERS) return;

        if (ROOT.equals(currentFolderId)) {
            loadAllNotes();
        } else {
            loadNotesInFolder(currentFolderId);
        }
    }

    // ==================================================
    // OPTION MENU
    // ==================================================
    private void showOptionMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        var view = getLayoutInflater().inflate(R.layout.bottom_sheet_options, null);

        view.findViewById(R.id.optAllNotes).setOnClickListener(v -> {
            dialog.dismiss();
            loadAllNotes();
        });

        view.findViewById(R.id.optFolders).setOnClickListener(v -> {
            dialog.dismiss();
            loadFolders();
        });

        view.findViewById(R.id.optTrash).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, TrashActivity.class));
        });

        dialog.setContentView(view);
        dialog.show();
    }

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

    // ==================================================
    // BACK
    // ==================================================
    @Override
    public void onBackPressed() {
        if (!ROOT.equals(currentFolderId)) {
            loadFolders();
        } else {
            super.onBackPressed();
        }
    }

    // ==================================================
    // CLEANUP
    // ==================================================
    private void removeListeners() {
        if (notesListener != null) {
            notesListener.remove();
            notesListener = null;
        }
        if (foldersListener != null) {
            foldersListener.remove();
            foldersListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ✅ Re-attach listener đúng màn hình đang xem
        if (currentMode == ScreenMode.FOLDERS) {
            loadFolders();
        } else { // NOTES mode
            if (ROOT.equals(currentFolderId)) {
                loadAllNotes();
            } else {
                loadNotesInFolder(currentFolderId);  // ⭐ cái bạn đang thiếu
            }
        }
    }


    // ==================================================
    // EDIT MODE
    // ==================================================
    private void toggleEditMode() {

        isEditMode = !isEditMode;

        // ================= EDIT NOTE =================
        if (currentMode == ScreenMode.NOTES) {

            notesAdapter.setEditMode(isEditMode);

            if (!isEditMode) {
                notesAdapter.clearSelection();
                if (btnSelectAll != null) btnSelectAll.setText("Chọn tất cả");
            }

            updateBottomActionBar();
            return;
        }

        // ================= EDIT FOLDER =================
        if (currentMode == ScreenMode.FOLDERS) {

            foldersAdapter.setEditMode(isEditMode);

            if (!isEditMode) {
                foldersAdapter.clearSelection();
                if (btnSelectAll != null) btnSelectAll.setText("Chọn tất cả");
            }

            updateBottomActionBar();
        }
    }


    private void updateBottomActionBar() {
        if (!isEditMode) {
            bottomActionBar.setVisibility(View.GONE);
            return;
        }

        bottomActionBar.setVisibility(View.VISIBLE);

        if (currentMode == ScreenMode.NOTES) {
            // NOTE: hiện đủ 3 nút
            btnSelectAll.setVisibility(View.VISIBLE);
            btnMove.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);

        } else if (currentMode == ScreenMode.FOLDERS) {
            // FOLDER: chỉ chọn tất cả + xóa
            btnSelectAll.setVisibility(View.VISIBLE);
            btnMove.setVisibility(View.GONE);
            btnDelete.setVisibility(View.VISIBLE);
        }
    }


    private void exitEditMode() {
        isEditMode = false;
        notesAdapter.setEditMode(false);
        notesAdapter.clearSelection();
        if (btnSelectAll != null) btnSelectAll.setText("Chọn tất cả");
        updateBottomActionBar();
    }

    // ==================================================
    // BƯỚC 5: ACTIONS
    // ==================================================
    private void onSelectAllClicked() {
        if (!isEditMode) return;

        // ================= NOTE MODE =================
        if (currentMode == ScreenMode.NOTES) {

            int selectedCount = notesAdapter.getSelectedNotes().size();
            boolean selectAll = selectedCount != noteList.size();

            notesAdapter.selectAll(selectAll);

            if (btnSelectAll != null) {
                btnSelectAll.setText(selectAll ? "Bỏ chọn" : "Chọn tất cả");
            }
            return;
        }

        // ================= FOLDER MODE =================
        if (currentMode == ScreenMode.FOLDERS) {

            int selectedCount = foldersAdapter.getSelectedFolders().size();
            boolean selectAll = selectedCount != folderList.size();

            foldersAdapter.selectAll(selectAll);

            if (btnSelectAll != null) {
                btnSelectAll.setText(selectAll ? "Bỏ chọn" : "Chọn tất cả");
            }
        }
    }

    private void onDeleteClicked() {
        if (!isEditMode) return;

        // ================= DELETE NOTES =================
        if (currentMode == ScreenMode.NOTES) {
            List<Note> selected = notesAdapter.getSelectedNotes();

            if (selected.isEmpty()) {
                Toast.makeText(this, "Chưa chọn ghi chú nào", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Xóa ghi chú")
                    .setMessage("Chuyển " + selected.size() + " ghi chú vào Thùng rác?")
                    .setPositiveButton("Xóa", (d, w) -> {
                        Timestamp now = Timestamp.now();
                        for (Note n : selected) {
                            if (n.id == null) continue;
                            db.collection("users")
                                    .document(uid)
                                    .collection("notes")
                                    .document(n.id)
                                    .update(
                                            "deleted", true,
                                            "deletedAt", now
                                    );
                        }
                        exitEditMode();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();

            return;
        }

        // ================= DELETE FOLDERS =================
        if (currentMode == ScreenMode.FOLDERS) {
            List<Folder> selected = foldersAdapter.getSelectedFolders();

            if (selected.isEmpty()) {
                Toast.makeText(this, "Chưa chọn thư mục nào", Toast.LENGTH_SHORT).show();
                return;
            }

            confirmDeleteFolders(selected);
        }
    }

    private void confirmDeleteFolders(List<Folder> folders) {

        if (folders == null || folders.isEmpty()) {
            Toast.makeText(this, "Chưa chọn thư mục nào", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xóa thư mục")
                .setMessage("Tất cả ghi chú trong các thư mục đã chọn sẽ được chuyển vào Thùng rác.\n\nTiếp tục?")
                .setPositiveButton("Xóa", (d, w) -> {

                    Timestamp now = Timestamp.now();

                    // 🔥 Đếm số folder đã xử lý xong
                    final int total = folders.size();
                    final int[] finished = {0};

                    for (Folder folder : folders) {
                        if (folder.id == null) {
                            finished[0]++;
                            continue;
                        }

                        // 1️⃣ LẤY NOTES TRONG FOLDER
                        db.collection("users")
                                .document(uid)
                                .collection("notes")
                                .whereEqualTo("folderId", folder.id)
                                .whereEqualTo("deleted", false)
                                .get()
                                .addOnSuccessListener(snapshot -> {

                                    WriteBatch batch = db.batch();

                                    // 2️⃣ XÓA NOTES
                                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                        batch.update(
                                                doc.getReference(),
                                                "deleted", true,
                                                "deletedAt", now
                                        );
                                    }

                                    // 3️⃣ XÓA FOLDER
                                    batch.update(
                                            db.collection("users")
                                                    .document(uid)
                                                    .collection("folders")
                                                    .document(folder.id),
                                            "deleted", true,
                                            "deletedAt", now
                                    );

                                    // 4️⃣ COMMIT 1 LẦN
                                    batch.commit()
                                            .addOnSuccessListener(v -> {
                                                finished[0]++;

                                                // ✅ KHI TẤT CẢ XONG → THOÁT EDIT MODE
                                                if (finished[0] == total) {
                                                    exitFolderEditMode();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                finished[0]++;
                                                Toast.makeText(
                                                        this,
                                                        "Lỗi xóa thư mục: " + e.getMessage(),
                                                        Toast.LENGTH_SHORT
                                                ).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    finished[0]++;
                                    Toast.makeText(
                                            this,
                                            "Lỗi load ghi chú: " + e.getMessage(),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void exitFolderEditMode() {
        isEditMode = false;

        if (foldersAdapter != null) {
            foldersAdapter.setEditMode(false);
            foldersAdapter.clearSelection();
        }

        updateBottomActionBar();
    }


    private void onMoveClicked() {
        if (!isEditMode) return;

        List<Note> selected = notesAdapter.getSelectedNotes();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Chưa chọn ghi chú nào", Toast.LENGTH_SHORT).show();
            return;
        }

        // đảm bảo có folderList để chọn
        ensureFolderListThenShowMoveDialog(selected);
    }

    // ==================================================
    // Load folders ẩn để move (không đổi màn)
    // ==================================================
    private void ensureFolderListThenShowMoveDialog(List<Note> selected) {
        // nếu đã có folderList -> show ngay
        if (!folderList.isEmpty()) {
            showMoveDialog(selected);
            return;
        }

        // load 1 lần (get) để có danh sách folder
        db.collection("users")
                .document(uid)
                .collection("folders")
                .whereEqualTo("deleted", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    folderList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Folder f = doc.toObject(Folder.class);
                        if (f != null) {
                            f.id = doc.getId();
                            folderList.add(f);
                        }
                    }

                    Collections.sort(folderList, (a, b) -> {
                        if (a.createdAt == null && b.createdAt == null) return 0;
                        if (a.createdAt == null) return 1;
                        if (b.createdAt == null) return -1;
                        return a.createdAt.compareTo(b.createdAt);
                    });

                    showMoveDialog(selected);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Load thư mục lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void showMoveDialog(List<Note> selected) {
        if (folderList.isEmpty()) {
            Toast.makeText(this, "Chưa có thư mục để di chuyển", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] folderNames = new String[folderList.size()];
        for (int i = 0; i < folderList.size(); i++) {
            folderNames[i] = folderList.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setTitle("Di chuyển vào thư mục")
                .setItems(folderNames, (d, which) -> {
                    Folder target = folderList.get(which);
                    if (target == null || target.id == null) return;

                    Timestamp now = Timestamp.now();
                    for (Note n : selected) {
                        if (n.id == null) continue;
                        db.collection("users")
                                .document(uid)
                                .collection("notes")
                                .document(n.id)
                                .update(
                                        "folderId", target.id,
                                        "updatedAt", now
                                );
                    }

                    exitEditMode();
                })
                .show();
    }

}
