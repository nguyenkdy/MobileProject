package com.example.mynoesapplication.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.Note;
import com.example.mynoesapplication.R;
import com.example.mynoesapplication.ThumbnailCache;

import java.util.ArrayList;
import java.util.List;

public class ReadOnlyNotesAdapter extends RecyclerView.Adapter<ReadOnlyNotesAdapter.VH> {

    private final List<Note> items = new ArrayList<>();

    public ReadOnlyNotesAdapter() { }

    public void setNotes(List<Note> notes) {
        items.clear();
        if (notes != null) items.addAll(notes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Note n = items.get(position);
        Context ctx = h.itemView.getContext();

        String title = (n.title == null || n.title.trim().isEmpty()) ? "Không tiêu đề" : n.title.trim();
        h.txtTitle.setText(title);

        String preview = "";
        if (n.content != null && !n.content.trim().isEmpty()) {
            preview = n.content.trim().replaceAll("\\n{2,}", "\n");
            if (preview.length() > 150) preview = preview.substring(0, 150) + "...";
        }
        h.txtContent.setText(preview);

        Bitmap thumb = ThumbnailCache.load(ctx, n.id);
        if (thumb != null) {
            h.imgPreview.setVisibility(View.VISIBLE);
            h.imgPreview.setImageBitmap(thumb);
        } else {
            h.imgPreview.setVisibility(View.GONE);
        }

        // keep interactive controls hidden in read-only
        if (h.btnOptions != null) h.btnOptions.setVisibility(View.GONE);
        if (h.chkSelect != null) h.chkSelect.setVisibility(View.GONE);

        // tap -> show read-only dialog with image (if present) above title/content in vertical LinearLayout
        h.itemView.setOnClickListener(v -> {
            String fullTitle = (n.title == null ? "" : n.title);
            String fullContent = (n.content == null ? "" : n.content);

            // build message parts
            StringBuilder msg = new StringBuilder();
            if (!fullTitle.trim().isEmpty()) {
                msg.append(fullTitle.trim()).append("\n\n");
            }
            msg.append(fullContent);

            // create vertical container
            LinearLayout container = new LinearLayout(ctx);
            container.setOrientation(LinearLayout.VERTICAL);
            int pad = dpToPx(ctx, 12);
            container.setPadding(pad, pad, pad, pad);

            // add image if available (larger than list thumbnail)
            Bitmap dialogThumb = ThumbnailCache.load(ctx, n.id);
            if (dialogThumb != null) {
                ImageView iv = new ImageView(ctx);
                iv.setImageBitmap(dialogThumb);
                iv.setAdjustViewBounds(true);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dpToPx(ctx, 200) // cap height
                );
                ivLp.bottomMargin = dpToPx(ctx, 8);
                iv.setLayoutParams(ivLp);
                container.addView(iv);
            }

            // optional title view (bold)
            if (!fullTitle.trim().isEmpty()) {
                TextView tvTitle = new TextView(ctx);
                tvTitle.setText(fullTitle.trim());
                tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
                tvTitle.setTextSize(18f);
                tvTitle.setPadding(0, 0, 0, dpToPx(ctx, 6));
                container.addView(tvTitle);
            }

            // content in a ScrollView
            ScrollView sv = new ScrollView(ctx);
            LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            sv.setLayoutParams(svLp);

            TextView tvContent = new TextView(ctx);
            tvContent.setText(fullContent);
            tvContent.setTextSize(14f);
            tvContent.setLineSpacing(0f, 1.15f);

            int contentPadding = 0;
            tvContent.setPadding(contentPadding, 0, contentPadding, 0);

            sv.addView(tvContent);
            container.addView(sv);

            new AlertDialog.Builder(ctx)
                    .setView(container)
                    .setPositiveButton("Close", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtTitle, txtContent;
        ImageView imgPreview;
        android.widget.ImageButton btnOptions;
        android.widget.CheckBox chkSelect;

        VH(@NonNull View v) {
            super(v);
            txtTitle = v.findViewById(R.id.txtTitle);
            txtContent = v.findViewById(R.id.txtContent);
            imgPreview = v.findViewById(R.id.imgPreview);
            btnOptions = v.findViewById(R.id.btnOptions);
            chkSelect = v.findViewById(R.id.chkSelect);
        }
    }

    private static int dpToPx(Context c, int dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }
}
