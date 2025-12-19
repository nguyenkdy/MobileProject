package com.example.mynoesapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

public class ThumbnailCache {

    public static void save(Context ctx, String noteId, Bitmap bmp) {
        if (ctx == null || noteId == null || bmp == null) return;

        try {
            File f = new File(ctx.getCacheDir(), noteId + ".png");
            try (FileOutputStream out = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
                out.flush();
            }
        } catch (Exception ignored) {}
    }

    public static Bitmap load(Context ctx, String noteId) {
        if (ctx == null || noteId == null) return null;

        try {
            File f = new File(ctx.getCacheDir(), noteId + ".png");
            if (!f.exists()) return null;
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Exception e) {
            return null;
        }
    }

    // (Optional) nếu bạn muốn xóa thumbnail khi xóa note
    public static void delete(Context ctx, String noteId) {
        if (ctx == null || noteId == null) return;
        try {
            File f = new File(ctx.getCacheDir(), noteId + ".png");
            if (f.exists()) f.delete();
        } catch (Exception ignored) {}
    }
}
