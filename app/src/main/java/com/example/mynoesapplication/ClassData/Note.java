// language: java
package com.example.mynoesapplication.ClassData;

public class Note {
    private String id;
    private String content;
    private String folderId;

    public Note() {
        // Default constructor required for Firestore
    }

    public Note(String id, String content, String folderId) {
        this.id = id;
        this.content = content;
        this.folderId = folderId;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
}
