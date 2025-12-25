package com.example.mynoesapplication.Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.Note;
import com.example.mynoesapplication.R;
import com.example.mynoesapplication.ThumbnailCache;

import java.util.ArrayList;
import java.util.List;

public class ReadOnlyNotesAdapter extends RecyclerView.Adapter<ReadOnlyNotesAdapter.VH> {

    // ===== Interface click =====
    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    private OnNoteClickListener listener;
    private final List<Note> items = new ArrayList<>();

    public ReadOnlyNotesAdapter() { }

    // ===== Set listener =====
    public void setOnNoteClickListener(OnNoteClickListener l) {
        this.listener = l;
    }

    // ===== Update data =====
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

        // ===== Title =====
        String title = (n.title == null || n.title.trim().isEmpty())
                ? "Không tiêu đề"
                : n.title.trim();
        h.txtTitle.setText(title);

        // ===== Content preview =====
        String preview = "";
        if (n.content != null && !n.content.trim().isEmpty()) {
            preview = n.content.trim().replaceAll("\\n{2,}", "\n");
            if (preview.length() > 150) {
                preview = preview.substring(0, 150) + "...";
            }
        }
        h.txtContent.setText(preview);

        // ===== Thumbnail =====
        Bitmap thumb = ThumbnailCache.load(ctx, n.id);
        if (thumb != null) {
            h.imgPreview.setVisibility(View.VISIBLE);
            h.imgPreview.setImageBitmap(thumb);
        } else {
            h.imgPreview.setVisibility(View.GONE);
        }

        // ===== Hide interactive controls =====
        if (h.btnOptions != null) h.btnOptions.setVisibility(View.GONE);
        if (h.chkSelect != null) h.chkSelect.setVisibility(View.GONE);

        // ===== Click → delegate to Activity =====
        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNoteClick(n);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ===== ViewHolder =====
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
}
