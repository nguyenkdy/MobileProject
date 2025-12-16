// language: java
package com.example.mynoesapplication;

import com.example.mynoesapplication.ClassData.Note;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import com.example.mynoesapplication.Adapter.NoteAdapter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class FolderEditingActivity extends AppCompatActivity {

    private static final String TAG = "FolderEditingActivity";

    private ImageButton btnBack;
    private FloatingActionButton btnAddNote;

    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;

    private List<Note> noteList;
    private NoteRepository noteRepository;
    private String folderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_editing);

        btnBack = findViewById(R.id.btnBack);
        btnAddNote = findViewById(R.id.btnAddFolder);
        recyclerView = findViewById(R.id.recyclerViewNotes);

        noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(noteList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(noteAdapter);

        noteRepository = new NoteRepository();
        folderId = getIntent().getStringExtra("folderId");
        Log.d(TAG, "Received folderId: " + folderId);

        if (folderId == null) {
            Toast.makeText(this, "Folder ID is missing", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Folder ID is null");
            finish();
            return;
        }

        loadNotes();

        btnBack.setOnClickListener(v -> finish());

        btnAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(FolderEditingActivity.this, NoteEditingActivity.class);
            startActivityForResult(intent, 1);
        });

        noteAdapter.setOnItemClickListener(position -> {
            Note note = noteList.get(position);
            String noteContent = note.getContent();
            String noteId = note.getId();

            Intent intent = new Intent(FolderEditingActivity.this, NoteEditingActivity.class);
            intent.putExtra("noteContent", noteContent);
            intent.putExtra("noteId", noteId);
            startActivityForResult(intent, 1);
        });
    }

    private void loadNotes() {
        Log.d(TAG, "Loading notes for folderId: " + folderId);
        noteRepository.getNotesForFolder(folderId, new NoteRepository.OnNotesLoaded() {
            @Override
            public void onLoaded(List<Note> notes) {
                Log.d(TAG, "Notes loaded: " + notes.size());
                runOnUiThread(() -> {
                    noteList.clear();
                    noteList.addAll(notes);
                    noteAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading notes", e);
                runOnUiThread(() -> Toast.makeText(FolderEditingActivity.this, "Failed to load notes", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String noteContent = data.getStringExtra("noteContent");
            String noteId = data.getStringExtra("noteId");

            if (noteContent != null && !noteContent.isEmpty()) {
                if (noteId == null) {
                    noteId = java.util.UUID.randomUUID().toString();
                    Note note = new Note(noteId, noteContent, folderId);
                    noteRepository.saveNote(note); // repository logs success/failure
                    noteList.add(note);
                } else {
                    for (int i = 0; i < noteList.size(); i++) {
                        if (noteList.get(i).getId().equals(noteId)) {
                            noteList.remove(i);
                            break;
                        }
                    }
                    Note updatedNote = new Note(noteId, noteContent, folderId);
                    noteRepository.saveNote(updatedNote);
                    noteList.add(updatedNote);
                }
                noteAdapter.notifyDataSetChanged();
            }
        }
    }
}
