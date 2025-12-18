package com.example.mynoesapplication;
import com.google.firebase.Timestamp;

public class Note {
    public String id;
    public String title;
    public String content;
    public String folderId; // null = không thuộc folder
    public Timestamp createdAt;
    public Timestamp updatedAt;

    public Note() {}
}
