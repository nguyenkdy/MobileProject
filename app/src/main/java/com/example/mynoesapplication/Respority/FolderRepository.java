// File: app/src/main/java/com/example/mynoesapplication/FolderRepository.java
package com.example.mynoesapplication;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FolderRepository {
    private static final String PREFS_NAME = "FoldersPrefs";
    private static final String FOLDERS_KEY = "FoldersKey";
    private final SharedPreferences sharedPreferences;

    public FolderRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveFolders(Map<String, List<String>> folders) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (Map.Entry<String, List<String>> entry : folders.entrySet()) {
            Set<String> notesSet = new HashSet<>(entry.getValue());
            editor.putStringSet(entry.getKey(), notesSet);
        }
        editor.putStringSet(FOLDERS_KEY, folders.keySet());
        editor.apply();
    }

    public Map<String, List<String>> loadFolders() {
        Map<String, List<String>> folders = new HashMap<>();
        Set<String> folderNames = sharedPreferences.getStringSet(FOLDERS_KEY, new HashSet<>());
        for (String folderName : folderNames) {
            Set<String> notesSet = sharedPreferences.getStringSet(folderName, new HashSet<>());
            folders.put(folderName, new ArrayList<>(notesSet));
        }
        return folders;
    }
}
