// language: java
package com.example.mynoesapplication;

import com.example.mynoesapplication.ClassData.Folder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class FolderRepository {
    private static final String TAG = "FolderRepository";
    private final FirebaseFirestore db;

    public FolderRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void saveFolder(Folder folder) {
        db.collection("folders").document(folder.getId())
                .set(folder)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Folder saved: " + folder.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save folder: " + folder.getId(), e));
    }

    public void deleteFolder(String folderId) {
        db.collection("folders").document(folderId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Folder deleted: " + folderId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete folder: " + folderId, e));
    }

    public interface OnFoldersLoaded {
        void onLoaded(List<Folder> folders);
        void onError(Exception e);
    }

    public void getAllFolders(OnFoldersLoaded callback) {
        db.collection("folders")
                .get()
                .addOnSuccessListener((QuerySnapshot qs) -> {
                    List<Folder> folders = new ArrayList<>();
                    for (DocumentSnapshot ds : qs.getDocuments()) {
                        Folder folder = ds.toObject(Folder.class);
                        if (folder != null) folders.add(folder);
                    }
                    callback.onLoaded(folders);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching folders", e);
                    callback.onError(new Exception(e));
                });
    }
}
