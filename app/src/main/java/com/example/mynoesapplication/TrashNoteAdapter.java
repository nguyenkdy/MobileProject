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

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TrashNoteAdapter extends RecyclerView.Adapter<TrashNoteAdapter.ViewHolder> {

    private final List<Note> notes;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid;

    public TrashNoteAdapter(List<Note> notes, String uid) {
        this.notes = notes;
        this.uid = uid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trash_note, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Note n = notes.get(position);

        h.txtTitle.setText(
                n.title == null || n.title.isEmpty()
                        ? "Không tiêu đề"
                        : n.title
        );

        if (n.deletedAt != null) {
            String time = new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale.getDefault()
            ).format(n.deletedAt.toDate());
            h.txtDeletedAt.setText("Đã xóa: " + time);
        }

        // ♻️ RESTORE
        h.btnRestore.setOnClickListener(v -> restoreNote(h.itemView.getContext(), n));

        // 🗑️ DELETE FOREVER
        h.btnDelete.setOnClickListener(v -> confirmDeleteForever(h.itemView.getContext(), n));
    }

    // ==================================================
    // RESTORE NOTE
    // ==================================================
    private void restoreNote(Context ctx, Note note) {
        db.collection("users")
                .document(uid)
                .collection("notes")
                .document(note.id)
                .update(
                        "deleted", false,
                        "deletedAt", null
                );
    }

    // ==================================================
    // DELETE FOREVER
    // ==================================================
    private void confirmDeleteForever(Context ctx, Note note) {
        new AlertDialog.Builder(ctx)
                .setTitle("Xóa vĩnh viễn?")
                .setMessage("Ghi chú sẽ bị xóa hoàn toàn và không thể khôi phục.")
                .setPositiveButton("Xóa", (d, w) -> {
                    db.collection("users")
                            .document(uid)
                            .collection("notes")
                            .document(note.id)
                            .delete();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return notes == null ? 0 : notes.size();
    }

    // ==================================================
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtDeletedAt;
        ImageButton btnRestore, btnDelete;

        ViewHolder(@NonNull View v) {
            super(v);
            txtTitle = v.findViewById(R.id.txtTitle);
            txtDeletedAt = v.findViewById(R.id.txtDeletedAt);
            btnRestore = v.findViewById(R.id.btnRestore);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
