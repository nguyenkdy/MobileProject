// language: java
package com.example.mynoesapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class NoteEditingActivity extends AppCompatActivity {

    private EditText edtNoteContent;
    private Button btnDone;
    private String noteId; // hold existing note id if editing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editing);

        edtNoteContent = findViewById(R.id.edtNoteContent);
        btnDone = findViewById(R.id.btnDone);

        Intent intent = getIntent();
        if (intent != null) {
            String content = intent.getStringExtra("noteContent");
            noteId = intent.getStringExtra("noteId");
            if (content != null) {
                edtNoteContent.setText(content);
            }
        }

        btnDone.setOnClickListener(v -> {
            String noteContent = edtNoteContent.getText().toString().trim();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("noteContent", noteContent);
            if (noteId != null) {
                resultIntent.putExtra("noteId", noteId);
            }
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
