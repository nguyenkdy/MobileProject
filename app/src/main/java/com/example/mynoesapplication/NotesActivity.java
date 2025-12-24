package com.example.mynoesapplication;
import com.example.mynoesapplication.Data.ScreenMode;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.Fragment.FolderCreateFragment;
import com.example.mynoesapplication.Fragment.FolderSharingFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NotesActivity extends AppCompatActivity {

    // ================= UI =================
    ImageButton btnOption, btnSetting, btnAdd, btnSearch, btnEdit;
    TextView txtFolderTitle;
    RecyclerView recyclerNotes;

    ImageButton btnOpenChat;     // ✅ đúng id trong XML
    FrameLayout chatContainer;   // ✅ đúng id: chat_container

    LinearLayout bottomActionBar;
    LinearLayout layoutEmpty;

    // bottom action buttons
    View btnSelectAll, btnMove, btnDelete;
    TextView txtSelectAll;

    // SEARCH
    LinearLayout layoutSearch;
    EditText edtSearch;
    ImageButton btnClearSearch;

    // ================= Firebase =================
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String uid;

    boolean isLoadingFolders = false;

    // ================= Data =================
    final List<Note> noteList = new ArrayList<>();
    final List<Folder> folderList = new ArrayList<>();

    NotesAdapter notesAdapter;
    FoldersAdapter foldersAdapter;

    ListenerRegistration notesListener;
    ListenerRegistration foldersListener;

    // ================= STATE =================
    private static final String ROOT = "ROOT";
    String currentFolderId = ROOT;

    private ScreenMode currentMode = ScreenMode.NOTES;

    boolean isEditMode = false;

    ActivityResultLauncher<Intent> pickPdfLauncher;

    List<Note> fullNoteList = new ArrayList<>();
    List<Folder> fullFolderList = new ArrayList<>();
    boolean isSearchMode = false;

    // ============================================================
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
        btnSearch = findViewById(R.id.btnSearch);
        btnEdit = findViewById(R.id.btnEdit);

        txtFolderTitle = findViewById(R.id.txtFolderTitle);

        recyclerNotes = findViewById(R.id.recyclerNotes);
        recyclerNotes.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotes.setItemAnimator(new DefaultItemAnimator());

        // ✅ đúng theo XML bạn gửi
        btnOpenChat = findViewById(R.id.btnOpenChat);
        chatContainer = findViewById(R.id.chat_container);

        bottomActionBar = findViewById(R.id.bottomActionBar);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnMove = findViewById(R.id.btnMove);
        btnDelete = findViewById(R.id.btnDeleteAll);
        if (btnSelectAll != null) txtSelectAll = btnSelectAll.findViewById(R.id.txtSelectAll);

        layoutSearch = findViewById(R.id.layoutSearch);
        edtSearch = findViewById(R.id.edtSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);

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
            if (folder == null || folder.id == null) return;
            txtFolderTitle.setText(folder.name);
            loadNotesInFolder(folder.id);
        });
        foldersAdapter.setOnFolderPinChangedListener(() -> {
            sortFolders();
            foldersAdapter.notifyDataSetChanged();
        });
        foldersAdapter.setUid(uid);
        recyclerNotes.setAdapter(notesAdapter);

        // ================= EVENTS =================
        if (btnSetting != null) btnSetting.setOnClickListener(v -> showSettingMenu());
        if (btnAdd != null) btnAdd.setOnClickListener(v -> showCreateMenu());
        if (btnSearch != null) btnSearch.setOnClickListener(v -> showSearchBar());
        if (btnClearSearch != null) btnClearSearch.setOnClickListener(v -> hideSearchBar());
        if (btnEdit != null) btnEdit.setOnClickListener(v -> toggleEditMode());

        if (btnSelectAll != null) btnSelectAll.setOnClickListener(v -> onSelectAllClicked());
        if (btnMove != null) btnMove.setOnClickListener(v -> onMoveClicked());
        if (btnDelete != null) btnDelete.setOnClickListener(v -> onDeleteClicked());

        if (btnOpenChat != null) btnOpenChat.setOnClickListener(v -> toggleChatOverlay());

        // BACK pressed
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {

                // 1) close chat overlay
                if (chatContainer != null && chatContainer.getVisibility() == View.VISIBLE) {
                    closeChatOverlay();
                    return;
                }

                // 2) exit edit mode
                if (isEditMode) {
                    toggleEditMode();
                    return;
                }

                // 3) inside folder -> go folders list
                if (!ROOT.equals(currentFolderId)) {
                    loadFolders();
                    return;
                }

                finish();
            }
        });

        // PDF picker
        pickPdfLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri pdfUri = result.getData().getData();
                        if (pdfUri != null) importPdfIntoApp(pdfUri);
                    }
                }
        );

        // Search listener
        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (!isSearchMode) return;
                    if (currentMode == ScreenMode.NOTES) filterNotesByTitle(s.toString());
                    else filterFoldersByName(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // DEFAULT
        loadAllNotes();
        updateBottomActionBar();
        updateLeftButton();
    }

    // ==================================================
    // CHAT overlay
    // ==================================================
    private void toggleChatOverlay() {
        if (chatContainer == null) return;

        FragmentManager fm = getSupportFragmentManager();
        Fragment existing = fm.findFragmentByTag("chat_overlay");

        if (chatContainer.getVisibility() == View.VISIBLE) {
            if (existing != null) fm.beginTransaction().remove(existing).commitAllowingStateLoss();
            chatContainer.setVisibility(View.GONE);
        } else {
            fm.beginTransaction()
                    .replace(R.id.chat_container, new com.example.mynoesapplication.Fragment.ChatFragment(), "chat_overlay")
                    .commitAllowingStateLoss();
            chatContainer.setVisibility(View.VISIBLE);
        }
    }

    private void closeChatOverlay() {
        if (chatContainer == null) return;
        Fragment f = getSupportFragmentManager().findFragmentByTag("chat_overlay");
        if (f != null) getSupportFragmentManager().beginTransaction().remove(f).commitAllowingStateLoss();
        chatContainer.setVisibility(View.GONE);
    }

    // ==================================================
    // LEFT BUTTON
    // ==================================================
    private void updateLeftButton() {
        if (btnOption == null) return;

        if (!ROOT.equals(currentFolderId)) {
            btnOption.setImageResource(R.drawable.ic_back);
            btnOption.setOnClickListener(v -> {
                if (isEditMode) toggleEditMode();
                hideSearchBar();
                loadFolders();
            });
        } else {
            btnOption.setImageResource(R.drawable.ic_option);
            btnOption.setOnClickListener(v -> showOptionMenu());
        }
    }

    // ==================================================
    // LOAD ALL NOTES
    // ==================================================
    private void loadAllNotes() {
        currentMode = ScreenMode.NOTES;
        currentFolderId = ROOT;
        if (txtFolderTitle != null) txtFolderTitle.setText("Tất cả ghi chú");

        recyclerNotes.setAdapter(notesAdapter);
        updateLeftButton();
        removeListeners();

        noteList.clear();
        notesAdapter.notifyDataSetChanged();
        updateEmptyState();

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
    // LOAD FOLDERS + joined
    // ==================================================
    private void loadFolders() {
        currentMode = ScreenMode.FOLDERS;
        currentFolderId = ROOT;
        if (txtFolderTitle != null) txtFolderTitle.setText("Thư mục");

        recyclerNotes.setAdapter(foldersAdapter);
        updateLeftButton();

        // stop previous listeners (notes + folders)
        removeListeners();

        // nếu chưa login / uid null thì out an toàn
        if (uid == null || uid.trim().isEmpty()) {
            isLoadingFolders = false;
            folderList.clear();
            foldersAdapter.notifyDataSetChanged();
            updateEmptyState();
            return;
        }

        // đang load
        isLoadingFolders = true;

        // clear UI trước
        folderList.clear();
        foldersAdapter.notifyDataSetChanged();

        // không hiện empty khi đang load
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
        if (recyclerNotes != null) recyclerNotes.setVisibility(View.VISIBLE);

        // ✅ realtime listener cho folders của mình
        foldersListener = db.collection("users")
                .document(uid)
                .collection("folders")
                .whereEqualTo("deleted", false)
                .addSnapshotListener(this, (snapshot, error) -> {
                    if (error != null) {
                        isLoadingFolders = false;
                        Toast.makeText(this, "Load folders lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                        return;
                    }
                    if (snapshot == null) {
                        isLoadingFolders = false;
                        updateEmptyState();
                        return;
                    }

                    // 1) Build list folders OWNED (realtime)
                    List<Folder> temp = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Folder f = doc.toObject(Folder.class);
                        if (f == null) f = new Folder();
                        f.id = doc.getId();
                        f.ownerId = null; // owned folder
                        temp.add(f);
                    }

                    // 2) Load joinedFolders (shared) - one-shot, failure tolerant
                    db.collection("users")
                            .document(uid)
                            .collection("joinedFolders")
                            .get()
                            .addOnSuccessListener(joinedSnap -> {

                                for (DocumentSnapshot jd : joinedSnap.getDocuments()) {
                                    String ownerUid = jd.getString("ownerUid");
                                    String ownerFolderId = jd.getString("folderId");
                                    if (ownerUid == null || ownerFolderId == null) continue;

                                    Folder shared = new Folder();
                                    shared.id = "shared_" + jd.getId();

                                    String name = jd.getString("folderName");
                                    if (name == null) name = "Thư mục chia sẻ";
                                    if (!name.endsWith(" (Chia sẻ)")) name += " (Chia sẻ)";

                                    shared.name = name;
                                    shared.ownerId = ownerUid;
                                    shared.roomCode = jd.getId();
                                    shared.originalFolderId = ownerFolderId;
                                    shared.deleted = false;
                                    shared.pinned = false; // shared không pin ở phía user này

                                    // createdAt có thể null, sortFolders đã handle null
                                    temp.add(shared);
                                }

                                // 3) apply -> sort -> notify
                                folderList.clear();
                                folderList.addAll(temp);

                                // nếu đang search, update fullFolderList để filter không bị lệch
                                if (isSearchMode) {
                                    fullFolderList.clear();
                                    fullFolderList.addAll(folderList);
                                    filterFoldersByName(edtSearch != null ? edtSearch.getText().toString() : "");
                                } else {
                                    finishLoadFolders(); // sortFolders + notify + updateEmptyState + isLoadingFolders=false
                                }
                            })
                            .addOnFailureListener(e -> {
                                // joinedFolders fail vẫn phải show owned folders
                                folderList.clear();
                                folderList.addAll(temp);

                                if (isSearchMode) {
                                    fullFolderList.clear();
                                    fullFolderList.addAll(folderList);
                                    filterFoldersByName(edtSearch != null ? edtSearch.getText().toString() : "");
                                } else {
                                    finishLoadFolders();
                                }
                            });
                });
    }

    private void finishLoadFolders() {
        sortFolders();
        foldersAdapter.notifyDataSetChanged();

        // 🔥 đánh dấu load xong
        isLoadingFolders = false;
        updateEmptyState();
    }


    private void sortFolders() {
        Collections.sort(folderList, (a, b) -> {

            // 1️⃣ Folder của mình lên trước folder chia sẻ
            boolean aShared = a.ownerId != null && !a.ownerId.equals(uid);
            boolean bShared = b.ownerId != null && !b.ownerId.equals(uid);

            if (aShared && !bShared) return 1;
            if (!aShared && bShared) return -1;

            // 2️⃣ PINNED lên đầu (chỉ áp dụng cho folder của mình)
            if (a.pinned && !b.pinned) return -1;
            if (!a.pinned && b.pinned) return 1;

            // 3️⃣ Cùng pinned → sort theo createdAt (mới trước)
            if (a.createdAt != null && b.createdAt != null) {
                return b.createdAt.compareTo(a.createdAt);
            }

            if (a.createdAt == null && b.createdAt != null) return 1;
            if (a.createdAt != null && b.createdAt == null) return -1;

            return 0;
        });
    }


    // ==================================================
    // LOAD NOTES IN FOLDER
    // ==================================================
    private void loadNotesInFolder(String folderId) {
        currentMode = ScreenMode.NOTES;
        currentFolderId = folderId;

        recyclerNotes.setAdapter(notesAdapter);
        updateLeftButton();
        removeListeners();

        noteList.clear();
        notesAdapter.notifyDataSetChanged();
        updateEmptyState();

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
    // APPLY NOTES SNAPSHOT
    // ==================================================
    private void applyNotesSnapshot(QuerySnapshot value) {
        List<Note> newList = new ArrayList<>();

        for (DocumentSnapshot doc : value.getDocuments()) {
            Note n = doc.toObject(Note.class);
            if (n != null) {
                n.id = doc.getId();
                newList.add(n);
            }
        }

        // sort giống adapter
        Collections.sort(newList, (a, b) -> {
            boolean ap = a.isPinned;
            boolean bp = b.isPinned;
            if (ap && !bp) return -1;
            if (!ap && bp) return 1;

            if (ap && bp) {
                return Long.compare(b.pinnedAt, a.pinnedAt);
            }

            long au = a.updatedAt != null ? a.updatedAt.toDate().getTime() : 0;
            long bu = b.updatedAt != null ? b.updatedAt.toDate().getTime() : 0;
            return Long.compare(bu, au);
        });

        // 🔥 DUY NHẤT DÒNG NÀY
        notesAdapter.replaceAllNotes(newList);

        updateEmptyState();
    }



    private void updateEmptyState() {
        if (layoutEmpty == null || recyclerNotes == null) return;

        // 🔥 QUAN TRỌNG: đang load folder → KHÔNG hiện empty
        if (currentMode == ScreenMode.FOLDERS && isLoadingFolders) {
            layoutEmpty.setVisibility(View.GONE);
            recyclerNotes.setVisibility(View.VISIBLE);
            return;
        }

        boolean isEmpty;
        if (isEditMode) {
            isEmpty = false;
        } else {
            isEmpty = (currentMode == ScreenMode.NOTES)
                    ? noteList.isEmpty()
                    : folderList.isEmpty();
        }

        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerNotes.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // ==================================================
    // CREATE MENU
    // ==================================================
    private void showCreateMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_create, null);

        view.findViewById(R.id.optCreateNote).setOnClickListener(v -> {
            dialog.dismiss();
            createNewNote();
        });

        view.findViewById(R.id.optCreateFolder).setOnClickListener(v -> {
            dialog.dismiss();
            showCreateFolderFragment();
        });

        view.findViewById(R.id.optCreatePdf).setOnClickListener(v -> {
            dialog.dismiss();
            openPdfImporter();
        });

        dialog.setContentView(view);
        dialog.show();
    }

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

        db.collection("users").document(uid).collection("notes")
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

    private void showCreateFolderFragment() {
        FolderCreateFragment fragment = new FolderCreateFragment(uid, currentMode);
        fragment.show(getSupportFragmentManager(), "folder_create_fragment");
    }

    private void refreshCurrentListAfterCreate() {
        if (currentMode == ScreenMode.FOLDERS) return;

        if (ROOT.equals(currentFolderId)) loadAllNotes();
        else loadNotesInFolder(currentFolderId);
    }

    // ==================================================
    // OPTION MENU
    // ==================================================
    private void showOptionMenu() {
        View view = getLayoutInflater().inflate(R.layout.popup_main_option, null);

        PopupWindow popup = new PopupWindow(
                view,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );

        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        popup.setElevation(12f);
        popup.setAnimationStyle(R.style.PopupSlideAnim);

        int[] loc = new int[2];
        btnOption.getLocationOnScreen(loc);

        popup.showAtLocation(
                btnOption,
                Gravity.NO_GRAVITY,
                loc[0],
                loc[1] + btnOption.getHeight()
        );

        view.findViewById(R.id.optAllNotes).setOnClickListener(v -> {
            popup.dismiss();
            hideSearchBar();
            if (isEditMode) toggleEditMode();
            loadAllNotes();
        });

        view.findViewById(R.id.optShared).setOnClickListener(v -> {
            popup.dismiss();
            hideSearchBar();

            FolderSharingFragment frag = FolderSharingFragment.newInstance(
                    null, null, null,
                    false, null
            );
            frag.show(getSupportFragmentManager(), "folder_sharing");

            if (isEditMode) toggleEditMode();
        });




        view.findViewById(R.id.optFolders).setOnClickListener(v -> {
            popup.dismiss();
            hideSearchBar();
            if (isEditMode) toggleEditMode();
            loadFolders();
        });

        view.findViewById(R.id.optTrash).setOnClickListener(v -> {
            popup.dismiss();
            startActivity(new Intent(this, TrashActivity.class));
        });
    }

    private void showSettingMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_setting, null);

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

        // reattach đúng màn hình
        if (currentMode == ScreenMode.FOLDERS) {
            loadFolders();
        } else {
            if (ROOT.equals(currentFolderId)) loadAllNotes();
            else loadNotesInFolder(currentFolderId);
        }
    }

    // ==================================================
    // EDIT MODE
    // ==================================================
    private void toggleEditMode() {
        isEditMode = !isEditMode;

        if (currentMode == ScreenMode.NOTES) {
            notesAdapter.setEditMode(isEditMode);

            if (!isEditMode) {
                notesAdapter.clearSelection();
                if (txtSelectAll != null) txtSelectAll.setText("Chọn tất cả");
            }

            updateBottomActionBar();
            return;
        }

        if (currentMode == ScreenMode.FOLDERS) {
            foldersAdapter.setEditMode(isEditMode);

            if (!isEditMode) {
                foldersAdapter.clearSelection();
                if (txtSelectAll != null) txtSelectAll.setText("Chọn tất cả");
            }

            updateBottomActionBar();
        }
    }

    private void updateBottomActionBar() {
        if (bottomActionBar == null) return;

        if (!isEditMode) {
            if (bottomActionBar.getVisibility() == View.VISIBLE) {
                bottomActionBar.animate()
                        .translationY(bottomActionBar.getHeight())
                        .setDuration(180)
                        .withEndAction(() -> bottomActionBar.setVisibility(View.GONE))
                        .start();
            }
            updateEmptyState();
            return;
        }

        if (bottomActionBar.getVisibility() != View.VISIBLE) {
            bottomActionBar.setVisibility(View.VISIBLE);
            bottomActionBar.setTranslationY(bottomActionBar.getHeight());
            bottomActionBar.animate().translationY(0).setDuration(220).start();
        }

        if (btnSelectAll != null) btnSelectAll.setVisibility(View.VISIBLE);

        if (currentMode == ScreenMode.NOTES) {
            if (btnMove != null) btnMove.setVisibility(View.VISIBLE);
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
        } else {
            if (btnMove != null) btnMove.setVisibility(View.GONE);
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
        }

        updateEmptyState();
    }

    private void exitEditMode() {
        isEditMode = false;
        if (notesAdapter != null) {
            notesAdapter.setEditMode(false);
            notesAdapter.clearSelection();
        }
        if (txtSelectAll != null) txtSelectAll.setText("Chọn tất cả");
        updateBottomActionBar();
    }

    private void exitFolderEditMode() {
        isEditMode = false;
        if (foldersAdapter != null) {
            foldersAdapter.setEditMode(false);
            foldersAdapter.clearSelection();
        }
        if (txtSelectAll != null) txtSelectAll.setText("Chọn tất cả");
        updateBottomActionBar();
    }

    // ==================================================
    // ACTIONS
    // ==================================================
    private void onSelectAllClicked() {
        if (!isEditMode) return;

        if (currentMode == ScreenMode.NOTES) {
            int selectedCount = notesAdapter.getSelectedNotes().size();
            boolean selectAll = selectedCount != noteList.size();
            notesAdapter.selectAll(selectAll);
            if (txtSelectAll != null) txtSelectAll.setText(selectAll ? "Bỏ chọn" : "Chọn tất cả");
            return;
        }

        if (currentMode == ScreenMode.FOLDERS) {
            int selectedCount = foldersAdapter.getSelectedFolders().size();
            boolean selectAll = selectedCount != folderList.size();
            foldersAdapter.selectAll(selectAll);
            if (txtSelectAll != null) txtSelectAll.setText(selectAll ? "Bỏ chọn" : "Chọn tất cả");
        }
    }

    private void onDeleteClicked() {
        if (!isEditMode) return;

        if (currentMode == ScreenMode.NOTES) {
            List<Note> selected = notesAdapter.getSelectedNotes();
            if (selected == null || selected.isEmpty()) {
                Toast.makeText(this, "Chưa chọn ghi chú nào", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Xóa ghi chú")
                    .setMessage("Chuyển " + selected.size() + " ghi chú vào Thùng rác?")
                    .setPositiveButton("Xóa", (d, w) -> {
                        Timestamp now = Timestamp.now();
                        for (Note n : selected) {
                            if (n == null || n.id == null) continue;
                            db.collection("users").document(uid).collection("notes")
                                    .document(n.id)
                                    .update("deleted", true, "deletedAt", now);
                        }
                        exitEditMode();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
            return;
        }

        if (currentMode == ScreenMode.FOLDERS) {
            List<Folder> selected = foldersAdapter.getSelectedFolders();
            if (selected == null || selected.isEmpty()) {
                Toast.makeText(this, "Chưa chọn thư mục nào", Toast.LENGTH_SHORT).show();
                return;
            }
            confirmDeleteFolders(selected);
        }
    }

    private void confirmDeleteFolders(List<Folder> folders) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa thư mục")
                .setMessage("Tất cả ghi chú trong các thư mục đã chọn sẽ được chuyển vào Thùng rác.\n\nTiếp tục?")
                .setPositiveButton("Xóa", (d, w) -> {
                    Timestamp now = Timestamp.now();

                    final int total = folders.size();
                    final int[] finished = {0};

                    for (Folder folder : folders) {
                        if (folder == null || folder.id == null) {
                            finished[0]++;
                            continue;
                        }

                        // ⚠️ nếu là shared folder (synthetic id) thì không cho xóa ở đây
                        if (folder.ownerId != null && !folder.ownerId.equals(uid)) {
                            finished[0]++;
                            Toast.makeText(this, "Không thể xóa thư mục chia sẻ", Toast.LENGTH_SHORT).show();
                            continue;
                        }

                        db.collection("users")
                                .document(uid)
                                .collection("notes")
                                .whereEqualTo("folderId", folder.id)
                                .whereEqualTo("deleted", false)
                                .get()
                                .addOnSuccessListener(snapshot -> {
                                    WriteBatch batch = db.batch();

                                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                        batch.update(doc.getReference(), "deleted", true, "deletedAt", now);
                                    }

                                    batch.update(
                                            db.collection("users").document(uid).collection("folders").document(folder.id),
                                            "deleted", true,
                                            "deletedAt", now
                                    );

                                    batch.commit()
                                            .addOnSuccessListener(v -> {
                                                finished[0]++;
                                                if (finished[0] == total) exitFolderEditMode();
                                            })
                                            .addOnFailureListener(e -> {
                                                finished[0]++;
                                                Toast.makeText(this, "Lỗi xóa thư mục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    finished[0]++;
                                    Toast.makeText(this, "Lỗi load ghi chú: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void onMoveClicked() {
        if (!isEditMode) return;

        List<Note> selected = notesAdapter.getSelectedNotes();
        if (selected == null || selected.isEmpty()) {
            Toast.makeText(this, "Chưa chọn ghi chú nào", Toast.LENGTH_SHORT).show();
            return;
        }

        ensureFolderListThenShowMoveDialog(selected);
    }

    // ==================================================
    // Load folders ẩn để move (không đổi màn)
    // ==================================================
    private void ensureFolderListThenShowMoveDialog(List<Note> selected) {
        // nếu đã có -> show luôn
        if (!folderList.isEmpty()) {
            showMoveDialog(selected);
            return;
        }

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
                        f.ownerId = null;
                        folderList.add(f);
                    }

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
                                    shared.id = "shared_" + roomCode;

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

                                // không đổi màn, chỉ load để chọn
                                showMoveDialog(selected);
                            })
                            .addOnFailureListener(e -> showMoveDialog(selected));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load folders: " + e.getMessage(), Toast.LENGTH_SHORT).show()
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
                        if (n == null || n.id == null) continue;

                        // shared folder owned by another user => copy note into owner's collection
                        if (target.ownerId != null && !target.ownerId.equals(uid)) {
                            Map<String, Object> copy = new HashMap<>();
                            copy.put("title", n.title == null ? "" : n.title);
                            copy.put("content", n.content == null ? "" : n.content);

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
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Copy failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                        } else {
                            // local folder => update folderId
                            db.collection("users")
                                    .document(uid)
                                    .collection("notes")
                                    .document(n.id)
                                    .update("folderId", target.id, "updatedAt", now);
                        }
                    }

                    exitEditMode();
                })
                .show();
    }

    // ==================================================
    // PDF import
    // ==================================================
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
            while (in != null && (len = in.read(buf)) > 0) out.write(buf, 0, len);

            if (in != null) in.close();
            out.close();

            Timestamp now = Timestamp.now();
            Map<String, Object> note = new HashMap<>();
            note.put("title", fileName);
            note.put("type", "pdf");
            note.put("pdfPath", outFile.getAbsolutePath());
            note.put("folderId", currentFolderId);
            note.put("createdAt", now);
            note.put("updatedAt", now);
            note.put("deleted", false);

            db.collection("users")
                    .document(uid)
                    .collection("notes")
                    .add(note)
                    .addOnSuccessListener(doc -> openPdfEditor(doc.getId(), outFile.getAbsolutePath()))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Tạo note PDF lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );

        } catch (Exception e) {
            Toast.makeText(this, "Import PDF lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openPdfEditor(String noteId, String path) {
        Intent i = new Intent(this, PdfEditorActivity.class);
        i.putExtra("noteId", noteId);
        i.putExtra("pdfPath", path);
        startActivity(i);
    }

    // ==================================================
    // SEARCH UI
    // ==================================================
    private void showSearchBar() {
        if (isEditMode) return;
        if (isSearchMode) return;
        if (layoutSearch == null || recyclerNotes == null) return;

        isSearchMode = true;

        layoutSearch.setVisibility(View.VISIBLE);
        layoutSearch.setAlpha(0f);

        // đo height chắc chắn (tránh = 0 gây animation sai)
        layoutSearch.post(() -> {
            int h = layoutSearch.getHeight();
            layoutSearch.setTranslationY(-h);
            layoutSearch.animate().translationY(0).alpha(1f).setDuration(220).start();

            recyclerNotes.setTranslationY(-h);
            recyclerNotes.animate().translationY(0).setDuration(220).start();
        });

        if (edtSearch != null) {
            edtSearch.requestFocus();
            showKeyboard(edtSearch);
        }

        // backup data để filter
        if (currentMode == ScreenMode.NOTES) {
            fullNoteList.clear();
            fullNoteList.addAll(noteList);
        } else {
            fullFolderList.clear();
            fullFolderList.addAll(folderList);
        }
    }

    private void hideSearchBar() {
        if (!isSearchMode) return;
        if (layoutSearch == null || recyclerNotes == null) return;

        isSearchMode = false;

        int h = layoutSearch.getHeight();

        recyclerNotes.animate().translationY(-h).setDuration(180).start();
        layoutSearch.animate()
                .translationY(-h)
                .alpha(0f)
                .setDuration(180)
                .withEndAction(() -> {
                    layoutSearch.setVisibility(View.GONE);
                    if (edtSearch != null) edtSearch.setText("");
                    recyclerNotes.setTranslationY(0);
                })
                .start();

        // restore list
        if (currentMode == ScreenMode.NOTES) {
            noteList.clear();
            noteList.addAll(fullNoteList);
            notesAdapter.notifyDataSetChanged();
        } else {
            folderList.clear();
            folderList.addAll(fullFolderList);
            foldersAdapter.notifyDataSetChanged();
        }

        updateEmptyState();
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
        updateEmptyState();
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
        updateEmptyState();
    }

    private void showKeyboard(View view) {
        view.post(() -> {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
    }
}
