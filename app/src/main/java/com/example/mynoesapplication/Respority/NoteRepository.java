package com.example.mynoesapplication;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoteRepository {
    private static final String PREFS_NAME = "NotesPrefs";
    private static final String NOTES_KEY = "NotesKey";
    private final SharedPreferences sharedPreferences;

    public NoteRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveNotes(List<String> notes) {
        Set<String> noteSet = new HashSet<>(notes);
        sharedPreferences.edit().putStringSet(NOTES_KEY, noteSet).apply();
    }

    public List<String> loadNotes() {
        Set<String> noteSet = sharedPreferences.getStringSet(NOTES_KEY, new HashSet<>());
        return new ArrayList<>(noteSet);
    }
}
