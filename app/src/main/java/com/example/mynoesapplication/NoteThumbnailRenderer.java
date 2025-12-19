package com.example.mynoesapplication;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

public class NoteThumbnailRenderer {

    /**
     * Render preview chỉ chứa NÉT VẼ
     */
    public static Bitmap renderDrawingPreview(Bitmap drawingBitmap) {

        int width = 600;
        int height = 320;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // ===== Background =====
        canvas.drawColor(Color.WHITE);

        if (drawingBitmap != null) {
            Rect src = cropCenter(drawingBitmap);
            Rect dst = new Rect(
                    24,
                    24,
                    width - 24,
                    height - 24
            );
            canvas.drawBitmap(drawingBitmap, src, dst, null);
        }

        return bmp;
    }

    // Crop phần trung tâm của drawing
    private static Rect cropCenter(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int size = Math.min(w, h);
        int left = (w - size) / 2;
        int top = (h - size) / 2;
        return new Rect(left, top, left + size, top + size);
    }
}
