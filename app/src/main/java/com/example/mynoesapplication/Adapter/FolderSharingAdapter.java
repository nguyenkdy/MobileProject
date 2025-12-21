package com.example.mynoesapplication.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.*;

import java.util.List;

public class FolderSharingAdapter extends RecyclerView.Adapter<FolderSharingAdapter.VH> {

    public interface OnFolderClick { void onClick(Folder folder); }

    private final List<Folder> items;
    private final OnFolderClick listener;

    public FolderSharingAdapter(List<Folder> items, OnFolderClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder_sharing, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Folder f = items.get(position);
        h.tvName.setText(f.name != null ? f.name : "Untitled");
        String meta = "Room: " + (f.roomCode != null ? f.roomCode : "-");
        if (f.ownerId != null) meta += " • Owner: " + f.ownerId;
        h.tvMeta.setText(meta);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(f);
        });
    }

    @Override
    public int getItemCount() { return items == null ? 0 : items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvMeta;
        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvFolderName);
            tvMeta = v.findViewById(R.id.tvFolderMeta);
        }
    }
}
