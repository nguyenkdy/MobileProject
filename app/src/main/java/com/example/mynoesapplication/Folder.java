package com.example.mynoesapplication;

import com.google.firebase.Timestamp;

public class Folder {

    public String id;
    public String name;
    public boolean pinned = false;      // ⭐ default
    public boolean selected = false;
    public Timestamp createdAt;

    // 🔥 THÊM 2 FIELD NÀY
    public boolean deleted = false;     // ⭐ default
    public Timestamp deletedAt;

    // ⚠️ BẮT BUỘC constructor rỗng cho Firestore
    public Folder() {}

    // ✅ Add fields used by FolderSharingAdapter / sharing features
    public String roomCode; // optional: code for shared room
    public String ownerId;  // optional: uid of folder owner

    // 🔥 NEW: store the original folder id on the owner's side
    public String originalFolderId;
}
