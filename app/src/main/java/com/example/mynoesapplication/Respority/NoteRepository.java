// language: java
package com.example.mynoesapplication;

import com.example.mynoesapplication.ClassData.Note;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class NoteRepository {
    private static final String TAG = "NoteRepository";
    private final FirebaseFirestore db;

    public NoteRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void saveNote(Note note, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("notes").document(note.getId())
                .set(note)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // convenience overload used by existing code
    public void saveNote(Note note) {
        saveNote(note,
                aVoid -> Log.d(TAG, "Note saved: " + note.getId()),
                e -> Log.e(TAG, "Failed to save note: " + note.getId(), e));
    }

    public void deleteNote(String noteId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("notes").document(noteId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public interface OnNotesLoaded {
        void onLoaded(List<Note> notes);
        void onError(Exception e);
    }

    public void getNotesForFolder(String folderId, OnNotesLoaded callback) {
        db.collection("notes")
                .whereEqualTo("folderId", folderId)
                .get()
                .addOnSuccessListener((QuerySnapshot qs) -> {
                    List<Note> notes = new ArrayList<>();
                    for (DocumentSnapshot ds : qs.getDocuments()) {
                        Note note = ds.toObject(Note.class);
                        if (note != null) notes.add(note);
                    }
                    callback.onLoaded(notes);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching notes for folder " + folderId, e);
                    callback.onError(new Exception(e));
                });
    }
}
