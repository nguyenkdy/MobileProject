package com.example.mynoesapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import com.example.mynoesapplication.NoteAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class FolderEditingActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private FloatingActionButton btnAddNote;

    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;

    private List<String> noteList; // Initialize the note list

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_editing);

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        btnAddNote = findViewById(R.id.btnAddFolder);
        recyclerView = findViewById(R.id.recyclerViewNotes);

        // Initialize note list and adapter
        noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(noteList);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(noteAdapter);

        // Handle back button click
        btnBack.setOnClickListener(v -> finish());

        // Handle add note button click
        btnAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(FolderEditingActivity.this, NoteEditingActivity.class);
            startActivityForResult(intent, 1);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String noteContent = data.getStringExtra("noteContent");

            // Add the note to the RecyclerView
            if (noteContent != null && !noteContent.isEmpty()) {
                noteList.add(noteContent);
                noteAdapter.notifyItemInserted(noteList.size() - 1);
            }
        }
    }
}
