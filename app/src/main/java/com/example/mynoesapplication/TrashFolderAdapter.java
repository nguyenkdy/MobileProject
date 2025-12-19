package com.example.mynoesapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TrashFolderAdapter extends RecyclerView.Adapter<TrashFolderAdapter.ViewHolder> {

    private final List<Folder> folders;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid;

    public TrashFolderAdapter(List<Folder> folders, String uid) {
        this.folders = folders;
        this.uid = uid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trash_folder, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Folder f = folders.get(position);

        h.txtName.setText(f.name);

        if (f.deletedAt != null) {
            String time = new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale.getDefault()
            ).format(f.deletedAt.toDate());
            h.txtDeletedAt.setText("Đã xóa: " + time);
        }

        // ♻️ RESTORE
        h.btnRestore.setOnClickListener(v -> restoreFolder(h.itemView.getContext(), f));

        // 🗑️ DELETE FOREVER
        h.btnDelete.setOnClickListener(v -> confirmDeleteForever(h.itemView.getContext(), f));
    }

    // ==================================================
    // RESTORE FOLDER + NOTES
    // ==================================================
    private void restoreFolder(Context ctx, Folder folder) {
        db.collection("users")
                .document(uid)
                .collection("notes")
                .whereEqualTo("folderId", folder.id)
                .get()
                .addOnSuccessListener(notesSnap -> {

                    WriteBatch batch = db.batch();

                    // restore folder
                    batch.update(
                            db.collection("users")
                                    .document(uid)
                                    .collection("folders")
                                    .document(folder.id),
                            "deleted", false,
                            "deletedAt", null
                    );

                    // restore notes
                    for (DocumentSnapshot doc : notesSnap) {
                        batch.update(doc.getReference(),
                                "deleted", false,
                                "deletedAt", null
                        );
                    }

                    batch.commit();
                });
    }

    // ==================================================
    // DELETE FOREVER FOLDER + NOTES
    // ==================================================
    private void confirmDeleteForever(Context ctx, Folder folder) {
        new AlertDialog.Builder(ctx)
                .setTitle("Xóa vĩnh viễn?")
                .setMessage("Thư mục và toàn bộ ghi chú bên trong sẽ bị xóa hoàn toàn.")
                .setPositiveButton("Xóa", (d, w) -> {

                    db.collection("users")
                            .document(uid)
                            .collection("notes")
                            .whereEqualTo("folderId", folder.id)
                            .get()
                            .addOnSuccessListener(notesSnap -> {

                                WriteBatch batch = db.batch();

                                // delete notes
                                for (DocumentSnapshot doc : notesSnap) {
                                    batch.delete(doc.getReference());
                                }

                                // delete folder
                                batch.delete(
                                        db.collection("users")
                                                .document(uid)
                                                .collection("folders")
                                                .document(folder.id)
                                );

                                batch.commit();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return folders == null ? 0 : folders.size();
    }

    // ==================================================
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtDeletedAt;
        ImageButton btnRestore, btnDelete;

        ViewHolder(@NonNull View v) {
            super(v);
            txtName = v.findViewById(R.id.txtName);
            txtDeletedAt = v.findViewById(R.id.txtDeletedAt);
            btnRestore = v.findViewById(R.id.btnRestore);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
