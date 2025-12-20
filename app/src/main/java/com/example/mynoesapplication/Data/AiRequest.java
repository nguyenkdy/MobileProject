package com.example.mynoesapplication.Data;


import java.util.Collections;
import java.util.List;

public class AiRequest {

    public List<Content> contents;

    public AiRequest(String prompt) {
        this.contents = Collections.singletonList(
                new Content(prompt)
        );
    }

    public static class Content {
        public List<Part> parts;

        public Content(String text) {
            this.parts = Collections.singletonList(new Part(text));
        }
    }

    public static class Part {
        public String text;

        public Part(String text) {
            this.text = text;
        }
    }
}

