package com.example.mynoesapplication;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import java.io.File;

public class NoteThumbnailRenderer {

    private static final int PREVIEW_WIDTH = 600;
    private static final int PREVIEW_HEIGHT = 320;
    private static final int PADDING = 24;

    /**
     * ================================
     * Render preview chỉ chứa NÉT VẼ
     * ================================
     */
    public static Bitmap renderDrawingPreview(Bitmap drawingBitmap) {

        Bitmap bmp = Bitmap.createBitmap(
                PREVIEW_WIDTH,
                PREVIEW_HEIGHT,
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bmp);

        // Background trắng
        canvas.drawColor(Color.WHITE);

        if (drawingBitmap != null) {
            Rect src = cropCenter(drawingBitmap);
            Rect dst = new Rect(
                    PADDING,
                    PADDING,
                    PREVIEW_WIDTH - PADDING,
                    PREVIEW_HEIGHT - PADDING
            );
            canvas.drawBitmap(drawingBitmap, src, dst, null);
        }

        return bmp;
    }

    /**
     * ==========================================
     * Render preview PDF — CHỈ TRANG ĐẦU TIÊN
     * ==========================================
     */
    public static Bitmap renderPdfPreview(String pdfPath) {
        try {
            File file = new File(pdfPath);
            if (!file.exists()) return null;

            ParcelFileDescriptor fd =
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(fd);

            if (renderer.getPageCount() <= 0) {
                renderer.close();
                fd.close();
                return null;
            }

            // 👉 CHỈ LẤY TRANG 0
            PdfRenderer.Page page = renderer.openPage(0);

            int scale = 2; // giảm size để nhẹ RAM
            Bitmap pageBitmap = Bitmap.createBitmap(
                    page.getWidth() / scale,
                    page.getHeight() / scale,
                    Bitmap.Config.ARGB_8888
            );

            page.render(
                    pageBitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            );

            page.close();
            renderer.close();
            fd.close();

            // ==== Vẽ bitmap PDF vào khung preview ====
            Bitmap preview = Bitmap.createBitmap(
                    PREVIEW_WIDTH,
                    PREVIEW_HEIGHT,
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(preview);
            canvas.drawColor(Color.WHITE);

            Rect src = cropCenter(pageBitmap);
            Rect dst = new Rect(
                    PADDING,
                    PADDING,
                    PREVIEW_WIDTH - PADDING,
                    PREVIEW_HEIGHT - PADDING
            );

            canvas.drawBitmap(pageBitmap, src, dst, null);

            return preview;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Crop phần trung tâm bitmap (vuông)
     */
    private static Rect cropCenter(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int size = Math.min(w, h);
        int left = (w - size) / 2;
        int top = (h - size) / 2;
        return new Rect(left, top, left + size, top + size);
    }
}
