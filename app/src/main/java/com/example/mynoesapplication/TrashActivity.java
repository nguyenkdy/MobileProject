package com.example.mynoesapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrashActivity extends AppCompatActivity {

    // ================= UI =================
    ImageButton btnBack;
    TextView btnTrashNotes, btnTrashFolders, txtEmpty;

    // ⭐ bottom actions
    TextView btnRestoreAll, btnEmptyTrash;

    RecyclerView recyclerTrash;

    // ================= Firebase =================
    FirebaseFirestore db;
    String uid;

    // ================= Data =================
    final List<Note> trashNotes = new ArrayList<>();
    final List<Folder> trashFolders = new ArrayList<>();

    TrashNoteAdapter trashNoteAdapter;
    TrashFolderAdapter trashFolderAdapter;

    ListenerRegistration trashListener;

    enum Mode { NOTES, FOLDERS }
    Mode currentMode = Mode.NOTES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        // ================= UI =================
        btnBack = findViewById(R.id.btnBack);
        btnTrashNotes = findViewById(R.id.btnTrashNotes);
        btnTrashFolders = findViewById(R.id.btnTrashFolders);
        txtEmpty = findViewById(R.id.txtEmpty);
        recyclerTrash = findViewById(R.id.recyclerTrash);

        // ⭐ bottom bar
        btnRestoreAll = findViewById(R.id.btnRestoreAll);
        btnEmptyTrash = findViewById(R.id.btnEmptyTrash);

        recyclerTrash.setLayoutManager(new LinearLayoutManager(this));

        // ================= Firebase =================
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // ================= Adapters =================
        trashNoteAdapter = new TrashNoteAdapter(trashNotes, uid);
        trashFolderAdapter = new TrashFolderAdapter(trashFolders, uid);

        // ================= Events =================
        btnBack.setOnClickListener(v -> finish());
        btnTrashNotes.setOnClickListener(v -> switchMode(Mode.NOTES));
        btnTrashFolders.setOnClickListener(v -> switchMode(Mode.FOLDERS));

        btnRestoreAll.setOnClickListener(v -> confirmRestoreAll());
        btnEmptyTrash.setOnClickListener(v -> confirmEmptyTrash());

        // Default
        switchMode(Mode.NOTES);
    }

    // ==================================================
    // SWITCH MODE
    // ==================================================
    private void switchMode(Mode mode) {
        currentMode = mode;
        updateTabUI();
        removeListener();

        if (mode == Mode.NOTES) {
            recyclerTrash.setAdapter(trashNoteAdapter);
            loadTrashNotes();
        } else {
            recyclerTrash.setAdapter(trashFolderAdapter);
            loadTrashFolders();
        }
    }

    private void updateTabUI() {
        btnTrashNotes.setBackgroundResource(
                currentMode == Mode.NOTES ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive
        );
        btnTrashFolders.setBackgroundResource(
                currentMode == Mode.FOLDERS ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive
        );
    }

    // ==================================================
    // LOAD TRASH NOTES
    // ==================================================
    private void loadTrashNotes() {
        trashNotes.clear();
        trashNoteAdapter.notifyDataSetChanged();
        txtEmpty.setVisibility(View.GONE);

        trashListener = db.collection("users")
                .document(uid)
                .collection("notes")
                .whereEqualTo("deleted", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (value == null) return;

                    trashNotes.clear();
                    for (var doc : value.getDocuments()) {
                        Note n = doc.toObject(Note.class);
                        if (n != null) {
                            n.id = doc.getId();
                            trashNotes.add(n);
                        }
                    }

                    Collections.sort(trashNotes, (a, b) -> {
                        Timestamp ta = a.deletedAt;
                        Timestamp tb = b.deletedAt;
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return tb.compareTo(ta);
                    });

                    trashNoteAdapter.notifyDataSetChanged();
                    txtEmpty.setVisibility(trashNotes.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    // ==================================================
    // LOAD TRASH FOLDERS
    // ==================================================
    private void loadTrashFolders() {
        trashFolders.clear();
        trashFolderAdapter.notifyDataSetChanged();
        txtEmpty.setVisibility(View.GONE);

        trashListener = db.collection("users")
                .document(uid)
                .collection("folders")
                .whereEqualTo("deleted", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (value == null) return;

                    trashFolders.clear();
                    for (var doc : value.getDocuments()) {
                        Folder f = doc.toObject(Folder.class);
                        if (f != null) {
                            f.id = doc.getId();
                            trashFolders.add(f);
                        }
                    }

                    Collections.sort(trashFolders, (a, b) -> {
                        Timestamp ta = a.deletedAt;
                        Timestamp tb = b.deletedAt;
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return tb.compareTo(ta);
                    });

                    trashFolderAdapter.notifyDataSetChanged();
                    txtEmpty.setVisibility(trashFolders.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    // ==================================================
    // ACTIONS
    // ==================================================
    private void confirmRestoreAll() {
        if (currentMode == Mode.NOTES && trashNotes.isEmpty()) return;
        if (currentMode == Mode.FOLDERS && trashFolders.isEmpty()) return;

        new AlertDialog.Builder(this)
                .setTitle("Hoàn tác tất cả?")
                .setMessage("Tất cả mục trong thùng rác sẽ được khôi phục.")
                .setPositiveButton("Đồng ý", (d, w) -> restoreAll())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void restoreAll() {
        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();

        if (currentMode == Mode.NOTES) {
            for (Note n : trashNotes) {
                batch.update(
                        db.collection("users")
                                .document(uid)
                                .collection("notes")
                                .document(n.id),
                        "deleted", false,
                        "deletedAt", null,
                        "updatedAt", now
                );
            }
        } else {
            for (Folder f : trashFolders) {
                batch.update(
                        db.collection("users")
                                .document(uid)
                                .collection("folders")
                                .document(f.id),
                        "deleted", false,
                        "deletedAt", null,
                        "updatedAt", now
                );
            }
        }

        batch.commit();
    }

    private void confirmEmptyTrash() {
        if (currentMode == Mode.NOTES && trashNotes.isEmpty()) return;
        if (currentMode == Mode.FOLDERS && trashFolders.isEmpty()) return;

        new AlertDialog.Builder(this)
                .setTitle("Xóa vĩnh viễn?")
                .setMessage("Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (d, w) -> emptyTrash())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void emptyTrash() {
        WriteBatch batch = db.batch();

        if (currentMode == Mode.NOTES) {
            for (Note n : trashNotes) {
                batch.delete(
                        db.collection("users")
                                .document(uid)
                                .collection("notes")
                                .document(n.id)
                );
            }
        } else {
            for (Folder f : trashFolders) {
                batch.delete(
                        db.collection("users")
                                .document(uid)
                                .collection("folders")
                                .document(f.id)
                );
            }
        }

        batch.commit().addOnSuccessListener(v -> {
            if (currentMode == Mode.NOTES) {
                for (Note n : trashNotes) {
                    ThumbnailCache.delete(this, n.id);
                }
            }
        });
    }

    // ==================================================
    // CLEANUP
    // ==================================================
    private void removeListener() {
        if (trashListener != null) {
            trashListener.remove();
            trashListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListener();
    }
}
