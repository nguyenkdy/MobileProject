package com.example.mynoesapplication.Folder;

public class Folder {
    String id;
    public String name;
    int noteCount;

    public Folder(String id, String name) {
        this.id = id;
        this.name = name;
        this.noteCount = 0;
    }
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
