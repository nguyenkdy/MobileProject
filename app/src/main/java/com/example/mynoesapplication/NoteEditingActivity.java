package com.example.mynoesapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class NoteEditingActivity extends AppCompatActivity {

    private EditText edtNoteContent;
    private Button btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editing);

        // Initialize views
        edtNoteContent = findViewById(R.id.edtNoteContent);
        btnDone = findViewById(R.id.btnDone);

        // Handle Done button click
        btnDone.setOnClickListener(v -> {
            String noteContent = edtNoteContent.getText().toString().trim();

            // Return the note content to the previous activity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("noteContent", noteContent);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
