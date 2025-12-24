package com.example.mynoesapplication;
import com.google.firebase.Timestamp;

public class Note {
    public String id;
    public String title;
    public String content;
    public boolean isPinned = false;
    public long pinnedAt; // thời điểm ghim (quan trọng)
    public boolean deleted;
    public Timestamp deletedAt;
    public boolean selected = false;
    public String folderId; // null = không thuộc folder
    public Timestamp createdAt;
    public Timestamp updatedAt;
    public String type;      // "note" | "pdf"
    public String pdfPath;   // ❗ RẤT QUAN TRỌNG
    public boolean isShared = false;   // ⭐ note này có thuộc folder chia sẻ không
    public String roomCode = null;     // ⭐ room chứa note (nếu shared)

    public Note() {}
}
