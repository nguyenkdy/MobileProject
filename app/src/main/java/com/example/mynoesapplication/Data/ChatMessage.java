package com.example.mynoesapplication.Data;

public class ChatMessage {
    public String id;
    public String text;
    public boolean isUser;
    public long ts;

    public ChatMessage(String id, String text, boolean isUser, long ts) {
        this.id = id;
        this.text = text;
        this.isUser = isUser;
        this.ts = ts;
    }
}
