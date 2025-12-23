// java
package com.example.mynoesapplication;

import android.content.Context;
import android.util.Log;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfSummarizer {
    private static final String TAG = "PdfSummarizer";
    private static volatile boolean initialized = false;

    public static synchronized void init(Context ctx) {
        if (initialized) return;
        try {
            PDFBoxResourceLoader.init(ctx.getApplicationContext());
            initialized = true;
            Log.d(TAG, "PDFBox initialized");
        } catch (Throwable t) {
            Log.e(TAG, "PDFBox init failed", t);
            initialized = false;
        }
    }

    public static String extractText(String pdfPath) {
        if (!initialized) {
            Log.e(TAG, "extractText called before PdfSummarizer.init(...)");
            return null;
        }
        if (pdfPath == null || pdfPath.trim().isEmpty()) return null;
        File f = new File(pdfPath);
        if (!f.exists() || !f.canRead()) {
            Log.e(TAG, "PDF file not accessible: " + pdfPath);
            return null;
        }

        PDDocument doc = null;
        try {
            doc = PDDocument.load(f);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            return text == null ? "" : text.trim();
        } catch (Throwable t) {
            // catch Throwable to avoid OOM / linkage / other fatal runtime problems crashing app
            Log.e(TAG, "Failed to extract PDF text (catch Throwable)", t);
            return null;
        } finally {
            if (doc != null) {
                try {
                    doc.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public static String summarize(String text, int maxSentences) {
        if (text == null || text.trim().isEmpty()) return "";
        if (maxSentences <= 0) maxSentences = 3;

        String[] parts = text.split("(?<=[\\.\\!\\?])\\s+");
        List<String> sentences = new ArrayList<>();
        for (String s : parts) {
            String t = s.trim();
            if (!t.isEmpty()) sentences.add(t);
            if (sentences.size() >= maxSentences) break;
        }

        if (!sentences.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sentences.size(); i++) {
                sb.append(sentences.get(i));
                if (i != sentences.size() - 1) sb.append(" ");
            }
            return sb.toString();
        }

        return text.length() <= 800 ? text : text.substring(0, 800) + "...";
    }
}
