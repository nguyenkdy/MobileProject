package com.example.mynoesapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PageViewHolder> {

    private final Context context;
    private final PdfRenderer pdfRenderer;
    private final PdfAnnotationStore store;

    private DrawingView activeDrawingView;
    private boolean isReadOnly = false;

    public PdfPageAdapter(Context ctx, PdfRenderer renderer, PdfAnnotationStore store) {
        this.context = ctx;
        this.pdfRenderer = renderer;
        this.store = store;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pdf_page, parent, false);
        return new PageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {

        holder.boundPageIndex = position;

        // ===== READ ONLY MODE =====
        holder.drawingView.setEnabled(!isReadOnly);
        holder.drawingView.setClickable(!isReadOnly);
        holder.drawingView.setFocusable(!isReadOnly);
        holder.drawingView.setFocusableInTouchMode(!isReadOnly);

        // ===== RENDER PDF PAGE =====
        PdfRenderer.Page page = pdfRenderer.openPage(position);

        Bitmap bitmap = Bitmap.createBitmap(
                page.getWidth(),
                page.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        page.render(bitmap, null, null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        holder.imgPdfPage.setImageBitmap(bitmap);
        page.close();

        // ===== LOAD ANNOTATION =====
        holder.isImporting = true;
        holder.drawingView.clear();

        String json = store.loadPageJson(position);
        if (json != null && !json.trim().isEmpty()) {
            holder.drawingView.importFromJson(json);
        }
        holder.isImporting = false;

        // ===== SAVE WHEN DRAW CHANGED =====
        holder.drawingView.setOnDrawingChangeListener(() -> {
            if (holder.isImporting) return;

            int pageIndex = holder.getAdapterPosition();
            if (pageIndex == RecyclerView.NO_POSITION) return;

            String outJson = holder.drawingView.exportToJson();
            store.savePageJson(pageIndex, outJson);
        });

        // ===== SET ACTIVE DRAWING VIEW (CHÍNH XÁC) =====
        holder.drawingView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                activeDrawingView = holder.drawingView;
            }
            return isReadOnly; // read-only thì cho scroll
        });

        holder.itemView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                activeDrawingView = holder.drawingView;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return pdfRenderer.getPageCount();
    }

    // ================= VIEW HOLDER =================
    static class PageViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPdfPage;
        DrawingView drawingView;

        int boundPageIndex = -1;
        boolean isImporting = false;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPdfPage = itemView.findViewById(R.id.imgPdfPage);
            drawingView = itemView.findViewById(R.id.drawingView);
        }
    }

    // ================= TOOL CONTROL =================
    public void setPen() {
        if (activeDrawingView != null) activeDrawingView.setPen();
    }

    public void setMarker() {
        if (activeDrawingView != null) activeDrawingView.setMarker();
    }

    public void setEraser() {
        if (activeDrawingView != null) activeDrawingView.setEraser();
    }

    public void undo() {
        if (activeDrawingView != null) activeDrawingView.undo();
    }

    public void redo() {
        if (activeDrawingView != null) activeDrawingView.redo();
    }

    public void setColor(int color) {
        if (activeDrawingView != null) {
            activeDrawingView.setColor(color);
        }
    }

    public void setPenStrokeWidth(float width) {
        if (activeDrawingView != null) {
            activeDrawingView.setPenStrokeWidth(width);
        }
    }

    public void setMarkerStrokeWidth(float width) {
        if (activeDrawingView != null) {
            activeDrawingView.setMarkerStrokeWidth(width);
        }
    }


    // ================= READ ONLY =================
    public void setReadOnly(boolean value) {
        isReadOnly = value;
        notifyDataSetChanged();
    }
}
