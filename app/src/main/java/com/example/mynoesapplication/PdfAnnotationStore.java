package com.example.mynoesapplication;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class PdfAnnotationStore {

    private final Context ctx;
    private final String noteId;

    public PdfAnnotationStore(Context ctx, String noteId) {
        this.ctx = ctx.getApplicationContext();
        this.noteId = noteId;
    }

    private File pageFile(int pageIndex) {
        File root = new File(ctx.getFilesDir(), "pdf_annotations");
        File noteDir = new File(root, noteId);
        if (!noteDir.exists()) noteDir.mkdirs();
        return new File(noteDir, "page_" + pageIndex + ".json");
    }

    public String loadPageJson(int pageIndex) {
        try {
            File f = pageFile(pageIndex);
            if (!f.exists()) return null;

            FileInputStream in = new FileInputStream(f);
            byte[] data = new byte[(int) f.length()];
            int read = in.read(data);
            in.close();

            if (read <= 0) return null;
            return new String(data, 0, read, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public void savePageJson(int pageIndex, String json) {
        try {
            File f = pageFile(pageIndex);

            if (json == null || json.trim().isEmpty()) {
                // Nếu không có nét vẽ -> xóa file để gọn
                if (f.exists()) f.delete();
                return;
            }

            FileOutputStream out = new FileOutputStream(f, false);
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
        } catch (Exception ignored) {}
    }
}
