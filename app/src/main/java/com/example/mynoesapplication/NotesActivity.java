package com.example.mynoesapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
    ImageButton btnOption, btnSetting, btnAdd, btnSearch;
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
    LinearLayout layoutEmpty;


    // ✅ BƯỚC 5: 3 NÚT ACTION
    View btnSelectAll, btnMove, btnDelete;
    TextView txtSelectAll;


    // ================= STATE =================
    private static final String ROOT = "ROOT";
    String currentFolderId = ROOT;

    // 🔥 NEW: đang xem gì? NOTES / FOLDERS
    private enum ScreenMode { NOTES, FOLDERS }
    private ScreenMode currentMode = ScreenMode.NOTES;

    boolean isEditMode = false;
    private ActivityResultLauncher<Intent> pickPdfLauncher;

    // ================= SEARCH =================
    LinearLayout layoutSearch;
    EditText edtSearch;
    ImageButton btnClearSearch;

    List<Note> fullNoteList = new ArrayList<>();
    List<Folder> fullFolderList = new ArrayList<>();

    boolean isSearchMode = false;


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
        txtSelectAll = btnSelectAll.findViewById(R.id.txtSelectAll);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        layoutSearch = findViewById(R.id.layoutSearch);
        edtSearch = findViewById(R.id.edtSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        btnSearch = findViewById(R.id.btnSearch);

        btnSearch.setOnClickListener(v -> showSearchBar());
        btnClearSearch.setOnClickListener(v -> hideSearchBar());


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
        pickPdfLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri pdfUri = result.getData().getData();
                        if (pdfUri != null) {
                            importPdfIntoApp(pdfUri);
                        }
                    }
                }
        );

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (currentMode == ScreenMode.NOTES) {
                    filterNotesByTitle(s.toString());
                } else if (currentMode == ScreenMode.FOLDERS) {
                    filterFoldersByName(s.toString());
                }
            }

            @Override public void afterTextChanged(Editable s) {}
        });

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

        // 1) load user's own folders
        db.collection("users")
                .document(uid)
                .collection("folders")
                .whereEqualTo("deleted", false)
                .get()
                .addOnSuccessListener(value -> {
                    folderList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Folder f = doc.toObject(Folder.class);
                        if (f == null) f = new Folder();
                        f.id = doc.getId();
                        // mark as local/owned
                        f.ownerId = null;
                        folderList.add(f);
                    }

                    // 2) append joined (shared) folders
                    db.collection("users")
                            .document(uid)
                            .collection("joinedFolders")
                            .get()
                            .addOnSuccessListener(joinedSnap -> {
                                for (DocumentSnapshot jd : joinedSnap.getDocuments()) {
                                    String roomCode = jd.getId();
                                    String ownerUid = jd.getString("ownerUid");
                                    String ownerFolderId = jd.getString("folderId");
                                    String folderName = jd.getString("folderName");

                                    if (ownerUid == null || ownerFolderId == null) continue;

                                    Folder shared = new Folder();
                                    shared.id = "shared_" + roomCode; // synthetic id

                                    // only append the suffix if it's not already present
                                    String baseName = folderName != null ? folderName : "Thư mục chia sẻ";
                                    String displayName = baseName.endsWith(" (Chia sẻ)")
                                            ? baseName
                                            : baseName + " (Chia sẻ)";

                                    shared.name = displayName;
                                    shared.ownerId = ownerUid;
                                    shared.roomCode = roomCode;
                                    shared.originalFolderId = ownerFolderId;
                                    shared.deleted = false;
                                    folderList.add(shared);
                                }

                                Collections.sort(folderList, (a, b) -> {
                                    if (a.createdAt == null && b.createdAt == null) return 0;
                                    if (a.createdAt == null) return 1;
                                    if (b.createdAt == null) return -1;
                                    return a.createdAt.compareTo(b.createdAt);
                                });

                                foldersAdapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                // still show local folders on error
                                Collections.sort(folderList, (a, b) -> {
                                    if (a.createdAt == null && b.createdAt == null) return 0;
                                    if (a.createdAt == null) return 1;
                                    if (b.createdAt == null) return -1;
                                    return a.createdAt.compareTo(b.createdAt);
                                });
                                foldersAdapter.notifyDataSetChanged();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load folders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        // ===== EMPTY STATE =====
        boolean isEmpty = noteList.isEmpty()
                && currentMode == ScreenMode.NOTES
                && ROOT.equals(currentFolderId)
                && !isEditMode;

        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }

        recyclerNotes.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        Collections.sort(noteList, (a, b) -> {
            if (a.updatedAt == null && b.updatedAt == null) return 0;
            if (a.updatedAt == null) return 1;
            if (b.updatedAt == null) return -1;

            // 1) pinned trước
            boolean ap = a.isPinned;
            boolean bp = b.isPinned;
            if (ap && !bp) return -1;
            if (!ap && bp) return 1;

            // 2) trong nhóm pinned: pinnedAt mới nhất lên đầu
            if (ap && bp) {
                long at = a.pinnedAt; // nếu field chưa có thì bạn phải thêm (long pinnedAt = 0)
                long bt = b.pinnedAt;
                int cmp = Long.compare(bt, at);
                if (cmp != 0) return cmp;

                // fallback nếu pinnedAt bằng nhau -> updatedAt
                long au = (a.updatedAt != null) ? a.updatedAt.toDate().getTime() : 0L;
                long bu = (b.updatedAt != null) ? b.updatedAt.toDate().getTime() : 0L;
                return Long.compare(bu, au);
            }

            // 3) nhóm không pinned: updatedAt mới nhất lên đầu
            long au = (a.updatedAt != null) ? a.updatedAt.toDate().getTime() : 0L;
            long bu = (b.updatedAt != null) ? b.updatedAt.toDate().getTime() : 0L;
            return Long.compare(bu, au);
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
        view.findViewById(R.id.optCreatePdf).setOnClickListener(v -> {
            dialog.dismiss();
            openPdfImporter();   // ✅ IMPORT PDF
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

        View view = getLayoutInflater()
                .inflate(R.layout.popup_main_option, null);

        PopupWindow popup = new PopupWindow(
                view,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );

        popup.setOutsideTouchable(true);
        popup.setElevation(12f);
        popup.setAnimationStyle(R.style.PopupSlideAnim);

        // vị trí: bên trái nút option, trượt sang phải
        int[] loc = new int[2];
        btnOption.getLocationOnScreen(loc);

        popup.showAtLocation(
                btnOption,
                Gravity.NO_GRAVITY,
                loc[0],
                loc[1] + btnOption.getHeight()
        );

        // ===== CLICK EVENTS =====
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
                if (btnSelectAll != null) txtSelectAll.setText("Chọn tất cả");
            }

            updateBottomActionBar();
            return;
        }

        // ================= EDIT FOLDER =================
        if (currentMode == ScreenMode.FOLDERS) {

            foldersAdapter.setEditMode(isEditMode);

            if (!isEditMode) {
                foldersAdapter.clearSelection();
                if (btnSelectAll != null) txtSelectAll.setText("Chọn tất cả");
            }

            updateBottomActionBar();
        }
    }


    private void updateBottomActionBar() {

        if (bottomActionBar == null) return;

        // ================= EXIT EDIT MODE =================
        if (!isEditMode) {

            if (bottomActionBar.getVisibility() == View.VISIBLE) {
                bottomActionBar.animate()
                        .translationY(bottomActionBar.getHeight())
                        .setDuration(180)
                        .withEndAction(() -> bottomActionBar.setVisibility(View.GONE))
                        .start();
            }
            return;
        }

        // ================= ENTER EDIT MODE =================
        if (bottomActionBar.getVisibility() != View.VISIBLE) {
            bottomActionBar.setVisibility(View.VISIBLE);
            bottomActionBar.setTranslationY(bottomActionBar.getHeight());
            bottomActionBar.animate()
                    .translationY(0)
                    .setDuration(220)
                    .start();
        }

        // ================= BUTTON VISIBILITY =================
        if (btnSelectAll != null) btnSelectAll.setVisibility(View.VISIBLE);

        if (currentMode == ScreenMode.NOTES) {

            // NOTES: đủ 3 nút
            if (btnMove != null) btnMove.setVisibility(View.VISIBLE);
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);

        } else if (currentMode == ScreenMode.FOLDERS) {

            // FOLDERS: không cho move
            if (btnMove != null) btnMove.setVisibility(View.GONE);
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
        }
    }


    private void exitEditMode() {
        isEditMode = false;
        notesAdapter.setEditMode(false);
        notesAdapter.clearSelection();
        if (btnSelectAll != null) txtSelectAll.setText("Chọn tất cả");
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
                txtSelectAll.setText(selectAll ? "Bỏ chọn" : "Chọn tất cả");
            }
            return;
        }

        // ================= FOLDER MODE =================
        if (currentMode == ScreenMode.FOLDERS) {

            int selectedCount = foldersAdapter.getSelectedFolders().size();
            boolean selectAll = selectedCount != folderList.size();

            foldersAdapter.selectAll(selectAll);

            if (btnSelectAll != null) {
                txtSelectAll.setText(selectAll ? "Bỏ chọn" : "Chọn tất cả");
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
        // if already loaded -> show
        if (!folderList.isEmpty()) {
            showMoveDialog(selected);
            return;
        }

        if (uid == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) load user's own folders
        db.collection("users")
                .document(uid)
                .collection("folders")
                .whereEqualTo("deleted", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    folderList.clear();
                    for (DocumentSnapshot d : snapshot.getDocuments()) {
                        Folder f = d.toObject(Folder.class);
                        if (f == null) f = new Folder();
                        f.id = d.getId();
                        // ensure ownerId null for local folders
                        f.ownerId = null;
                        folderList.add(f);
                    }

                    // 2) load joined folders (shared by others) and append
                    db.collection("users")
                            .document(uid)
                            .collection("joinedFolders")
                            .get()
                            .addOnSuccessListener(joinedSnap -> {
                                for (DocumentSnapshot jd : joinedSnap.getDocuments()) {
                                    String roomCode = jd.getId();
                                    String ownerUid = jd.getString("ownerUid");
                                    String ownerFolderId = jd.getString("folderId");
                                    String folderName = jd.getString("folderName");

                                    if (ownerUid == null || ownerFolderId == null) continue;

                                    Folder shared = new Folder();
                                    // use a synthetic id so it won't conflict with local folder ids
                                    shared.id = "shared_" + roomCode;

                                    // only append the suffix if it's not already present
                                    String baseName = folderName != null ? folderName : "Thư mục chia sẻ";
                                    String displayName = baseName.endsWith(" (Chia sẻ)")
                                            ? baseName
                                            : baseName + " (Chia sẻ)";

                                    shared.name = displayName;
                                    shared.ownerId = ownerUid;
                                    shared.roomCode = roomCode;
                                    shared.originalFolderId = ownerFolderId;
                                    shared.deleted = false;
                                    folderList.add(shared);
                                }

                                // notify adapter and show dialog
                                foldersAdapter.notifyDataSetChanged();
                                showMoveDialog(selected);
                            })
                            .addOnFailureListener(e -> {
                                // still show dialog with local folders
                                foldersAdapter.notifyDataSetChanged();
                                showMoveDialog(selected);
                            });

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load folders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }    private void showMoveDialog(List<Note> selected) {
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

                        // If target is owned by another user -> copy note into owner's collection (read\-only sharing)
                        if (target.ownerId != null && !target.ownerId.equals(uid)) {
                            Map<String, Object> copy = new HashMap<>();
                            copy.put("title", n.title == null ? "" : n.title);
                            copy.put("content", n.content == null ? "" : n.content);
                            // target.originalFolderId is the owner's folder id stored on the room/folder object,
                            // fall back to target.id if originalFolderId is not present.
                            String ownerFolderId = (target.originalFolderId != null && !target.originalFolderId.isEmpty())
                                    ? target.originalFolderId
                                    : target.id;
                            copy.put("folderId", ownerFolderId);
                            copy.put("createdAt", now);
                            copy.put("updatedAt", now);
                            copy.put("deleted", false);
                            copy.put("deletedAt", null);

                            db.collection("users")
                                    .document(target.ownerId)
                                    .collection("notes")
                                    .add(copy)
                                    .addOnFailureListener(e -> Toast.makeText(this, "Copy failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                            // Optionally: remove or mark local note deleted if you want move semantics across accounts.
                            // To keep a "copy" behavior, do nothing to the local note.
                        } else {
                            // Local folder -> just update folderId
                            db.collection("users")
                                    .document(uid)
                                    .collection("notes")
                                    .document(n.id)
                                    .update(
                                            "folderId", target.id,
                                            "updatedAt", now
                                    );
                        }
                    }

                    exitEditMode();
                })
                .show();
    }

    private void openPdfImporter() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");

        pickPdfLauncher.launch(intent);
    }

    private void importPdfIntoApp(Uri uri) {
        try {
            File dir = new File(getFilesDir(), "pdfs");
            if (!dir.exists()) dir.mkdirs();

            String fileName = "pdf_" + System.currentTimeMillis() + ".pdf";
            File outFile = new File(dir, fileName);

            InputStream in = getContentResolver().openInputStream(uri);
            FileOutputStream out = new FileOutputStream(outFile);

            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close(); out.close();

            // ✅ TẠO NOTE PDF
            Timestamp now = Timestamp.now();
            Map<String, Object> note = new HashMap<>();
            note.put("title", fileName);
            note.put("type", "pdf");
            note.put("pdfPath", outFile.getAbsolutePath());
            note.put("folderId", currentFolderId); // 🔥 FIX Ở ĐÂY
            note.put("createdAt", now);
            note.put("updatedAt", now);
            note.put("deleted", false);

            db.collection("users")
                    .document(uid)
                    .collection("notes")
                    .add(note)
                    .addOnSuccessListener(doc -> {
                        openPdfEditor(doc.getId(), outFile.getAbsolutePath());
                    });


        } catch (Exception e) {
            Toast.makeText(this, "Import PDF lỗi", Toast.LENGTH_SHORT).show();
        }
    }
    private void openPdfEditor(String noteId, String path) {
        Intent i = new Intent(this, PdfEditorActivity.class);
        i.putExtra("noteId", noteId);
        i.putExtra("pdfPath", path);
        startActivity(i);
    }

    private void showSearchBar() {
        if (isEditMode) return;
        if (isSearchMode) return;

        isSearchMode = true;

        layoutSearch.setVisibility(View.VISIBLE);
        layoutSearch.setTranslationY(-layoutSearch.getHeight());
        layoutSearch.setAlpha(0f);

        layoutSearch.animate()
                .translationY(0)
                .alpha(1f)
                .setDuration(220)
                .start();

        // đẩy recycler xuống (đã làm)
        recyclerNotes.setTranslationY(-layoutSearch.getHeight());
        recyclerNotes.animate()
                .translationY(0)
                .setDuration(220)
                .start();

        edtSearch.requestFocus();
        showKeyboard(edtSearch);

        // ===== BACKUP DATA =====
        if (currentMode == ScreenMode.NOTES) {
            fullNoteList.clear();
            fullNoteList.addAll(noteList);
        } else if (currentMode == ScreenMode.FOLDERS) {
            fullFolderList.clear();
            fullFolderList.addAll(folderList);
        }
    }

    private void hideSearchBar() {
        if (!isSearchMode) return;

        isSearchMode = false;

        recyclerNotes.animate()
                .translationY(-layoutSearch.getHeight())
                .setDuration(180)
                .start();

        layoutSearch.animate()
                .translationY(-layoutSearch.getHeight())
                .alpha(0f)
                .setDuration(180)
                .withEndAction(() -> {
                    layoutSearch.setVisibility(View.GONE);
                    edtSearch.setText("");
                    recyclerNotes.setTranslationY(0);
                })
                .start();

        // ===== RESTORE DATA =====
        if (currentMode == ScreenMode.NOTES) {
            noteList.clear();
            noteList.addAll(fullNoteList);
            notesAdapter.notifyDataSetChanged();
        } else if (currentMode == ScreenMode.FOLDERS) {
            folderList.clear();
            folderList.addAll(fullFolderList);
            foldersAdapter.notifyDataSetChanged();
        }
    }

    private void filterNotesByTitle(String keyword) {
        noteList.clear();

        if (keyword == null || keyword.trim().isEmpty()) {
            noteList.addAll(fullNoteList);
        } else {
            String lower = keyword.toLowerCase();
            for (Note n : fullNoteList) {
                if (n.title != null && n.title.toLowerCase().contains(lower)) {
                    noteList.add(n);
                }
            }
        }

        notesAdapter.notifyDataSetChanged();
    }
    private void showKeyboard(View view) {
        view.post(() -> {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void filterFoldersByName(String keyword) {
        folderList.clear();

        if (keyword == null || keyword.trim().isEmpty()) {
            folderList.addAll(fullFolderList);
        } else {
            String lower = keyword.toLowerCase();
            for (Folder f : fullFolderList) {
                if (f.name != null && f.name.toLowerCase().contains(lower)) {
                    folderList.add(f);
                }
            }
        }

        foldersAdapter.notifyDataSetChanged();
    }


}
