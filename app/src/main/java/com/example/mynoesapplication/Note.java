package com.example.mynoesapplication;
import com.google.firebase.Timestamp;

public class Note {
    public String id;
    public String title;
    public String content;
    public boolean isPinned = false;
    public boolean deleted;
    public Timestamp deletedAt;
    public boolean selected = false;
    public String folderId; // null = không thuộc folder
    public Timestamp createdAt;
    public Timestamp updatedAt;
    public String type;      // "note" | "pdf"
    public String pdfPath;   // ❗ RẤT QUAN TRỌNG

    public Note() {}
}
