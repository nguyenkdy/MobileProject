package com.example.mynoesapplication;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.Adapter.ReadOnlyNotesAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReadOnlyNotesActivity extends AppCompatActivity {

    private TextView tvTitle;
    private RecyclerView rv;
    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private final List<Note> notes = new ArrayList<>();
    private ReadOnlyNotesAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readonly_notes);

        tvTitle = findViewById(R.id.tvReadOnlyTitle);
        rv = findViewById(R.id.rvReadOnlyNotes);

        db = FirebaseFirestore.getInstance();

        String ownerUid = getIntent().getStringExtra("ownerUid");
        String folderId = getIntent().getStringExtra("folderId");
        String folderName = getIntent().getStringExtra("folderName");

        if (folderName != null && !folderName.isEmpty()) tvTitle.setText(folderName);

        // Use ReadOnlyNotesAdapter
        adapter = new ReadOnlyNotesAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setAdapter(adapter);

        if (ownerUid == null || ownerUid.isEmpty() || folderId == null || folderId.isEmpty()) {
            Toast.makeText(this, "Missing folder data", Toast.LENGTH_SHORT).show();
            return;
        }
        attachNotesListener(ownerUid, folderId);
    }

    private void attachNotesListener(String ownerUid, String ownerFolderId) {
        removeListener();

        listener = db.collection("users")
                .document(ownerUid)
                .collection("notes")
                .whereEqualTo("folderId", ownerFolderId)
                .whereEqualTo("deleted", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Load notes error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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

                    Collections.sort(notes, (a, b) -> {
                        if (a.updatedAt == null && b.updatedAt == null) return 0;
                        if (a.updatedAt == null) return 1;
                        if (b.updatedAt == null) return -1;
                        return b.updatedAt.compareTo(a.updatedAt);
                    });

                    // Provide data to the read-only adapter
                    adapter.setNotes(notes);
                });
    }

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
