package com.example.mynoesapplication;

import com.google.firebase.Timestamp;

public class Folder {

    public String id;
    public String name;
    public boolean pinned;
    public boolean selected = false;
    public Timestamp createdAt;

    // 🔥 THÊM 2 FIELD NÀY
    public boolean deleted;
    public Timestamp deletedAt;

    // ⚠️ BẮT BUỘC constructor rỗng cho Firestore
    public Folder() {}
}
