package com.example.mynoesapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.Adapter.ReadOnlyNotesAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReadOnlyNotesActivity extends AppCompatActivity {

    // ================= UI =================
    private TextView tvTitle;
    private RecyclerView rv;
    private ImageButton btnBack;

    // ================= Firebase =================
    private FirebaseFirestore db;
    private ListenerRegistration listener;

    // ================= Data =================
    private final List<Note> notes = new ArrayList<>();
    private ReadOnlyNotesAdapter adapter;

    // ================= Intent data =================
    private String ownerUid;
    private String folderId;
    private String folderName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_readonly_notes);

        // ===== Insets =====
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // ===== Init UI =====
        tvTitle = findViewById(R.id.tvReadOnlyTitle);
        rv = findViewById(R.id.rvReadOnlyNotes);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // ===== Firebase =====
        db = FirebaseFirestore.getInstance();

        // ===== Get intent =====
        ownerUid = getIntent().getStringExtra("ownerUid");
        folderId = getIntent().getStringExtra("folderId");
        folderName = getIntent().getStringExtra("folderName");

        if (folderName != null && !folderName.isEmpty()) {
            tvTitle.setText(folderName);
        }

        if (ownerUid == null || ownerUid.isEmpty()
                || folderId == null || folderId.isEmpty()) {
            Toast.makeText(this, "Missing folder data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ===== Setup RecyclerView =====
        adapter = new ReadOnlyNotesAdapter();

        adapter.setOnNoteClickListener(note -> {
            Intent intent = new Intent(
                    ReadOnlyNotesActivity.this,
                    ReadOnlyOneNoteActivity.class
            );
            intent.putExtra("noteId", note.id);
            intent.putExtra("ownerUid", ownerUid);
            startActivity(intent);
        });


        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setAdapter(adapter);

        // ===== Load notes =====
        attachNotesListener();
    }

    // ================= Load notes =================
    private void attachNotesListener() {
        removeListener();

        listener = db.collection("users")
                .document(ownerUid)
                .collection("notes")
                .whereEqualTo("folderId", folderId)
                .whereEqualTo("deleted", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this,
                                "Load notes error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value == null) return;

                    notes.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Note n = doc.toObject(Note.class);
                        if (n != null) {
                            n.id = doc.getId();
                            notes.add(n);
                        }
                    }

                    // sort by updatedAt desc
                    Collections.sort(notes, (a, b) -> {
                        if (a.updatedAt == null && b.updatedAt == null) return 0;
                        if (a.updatedAt == null) return 1;
                        if (b.updatedAt == null) return -1;
                        return b.updatedAt.compareTo(a.updatedAt);
                    });

                    adapter.setNotes(notes);
                });
    }

    // ================= Cleanup =================
    private void removeListener() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListener();
    }
}
