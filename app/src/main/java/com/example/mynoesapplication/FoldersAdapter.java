package com.example.mynoesapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FoldersAdapter extends RecyclerView.Adapter<FoldersAdapter.FolderViewHolder> {

    // ================= CALLBACK =================
    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
    }

    private final List<Folder> folders;
    private final OnFolderClickListener listener;

    // ================= CONSTRUCTOR =================
    public FoldersAdapter(List<Folder> folders, OnFolderClickListener listener) {
        this.folders = folders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder h, int i) {
        Folder f = folders.get(i);
        h.txtFolderName.setText(f.name);

        // CLICK + ANIMATION + CALLBACK
        h.itemView.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(80);

                        // 🔥 CALLBACK TRẢ VỀ ACTIVITY
                        if (listener != null) {
                            listener.onFolderClick(f);
                        }
                    });
        });
    }

    @Override
    public int getItemCount() {
        return folders == null ? 0 : folders.size();
    }

    // ================= VIEW HOLDER =================
    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView txtFolderName;

        public FolderViewHolder(@NonNull View v) {
            super(v);
            txtFolderName = v.findViewById(R.id.txtFolderName);
        }
    }
}
