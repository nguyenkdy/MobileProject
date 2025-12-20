package com.example.mynoesapplication.Data;

import java.util.List;

public class AiResponse {

    public List<Candidate> candidates;

    public String getSummary() {
        if (candidates == null || candidates.isEmpty()) return null;
        return candidates.get(0)
                .content
                .parts
                .get(0)
                .text;
    }

    public static class Candidate {
        public Content content;
    }

    public static class Content {
        public List<Part> parts;
    }

    public static class Part {
        public String text;
    }
}
