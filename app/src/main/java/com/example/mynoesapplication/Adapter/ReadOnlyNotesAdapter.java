package com.example.mynoesapplication.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.Note;
import com.example.mynoesapplication.R;

import java.util.List;

public class ReadOnlyNotesAdapter extends RecyclerView.Adapter<ReadOnlyNotesAdapter.VH> {

    private final List<Note> items;

    public ReadOnlyNotesAdapter(List<Note> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_readonly_note, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Note n = items.get(position);
        h.tvTitle.setText(n.title == null || n.title.trim().isEmpty() ? "Không tiêu đề" : n.title);
        String preview = n.content == null ? "" : n.content.trim().replaceAll("\\n{2,}", "\n");
        if (preview.length() > 150) preview = preview.substring(0, 150) + "...";
        h.tvPreview.setText(preview);
        // read-only: no click action to edit
    }

    @Override
    public int getItemCount() { return items == null ? 0 : items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPreview;
        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvROTitle);
            tvPreview = v.findViewById(R.id.tvROPreview);
        }
    }
}
