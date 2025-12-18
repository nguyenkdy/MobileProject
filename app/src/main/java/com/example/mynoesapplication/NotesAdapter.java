package com.example.mynoesapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private final List<Note> notes;
    private final FirebaseFirestore db;
    private final String uid;

    public NotesAdapter(List<Note> notes) {
        this.notes = notes;
        this.db = FirebaseFirestore.getInstance();
        this.uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    // ================= VIEW HOLDER =================
    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtContent;
        ImageButton btnDelete, btnRename;

        public NoteViewHolder(@NonNull View v) {
            super(v);
            txtTitle = v.findViewById(R.id.txtTitle);
            txtContent = v.findViewById(R.id.txtContent);
            btnDelete = v.findViewById(R.id.btnDelete);
            btnRename = v.findViewById(R.id.btnRename);
        }
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder h, int position) {
        Note note = notes.get(position);

        h.txtTitle.setText(
                note.title == null || note.title.trim().isEmpty()
                        ? "Không tiêu đề"
                        : note.title
        );
        h.txtContent.setText(note.content == null ? "" : note.content);

        // ================= CLICK NOTE =================
        h.itemView.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80);

                        Context ctx = v.getContext();
                        Intent i = new Intent(ctx, EditNoteActivity.class);
                        i.putExtra("noteId", note.id);
                        ctx.startActivity(i);
                    });
        });

        // ================= DELETE NOTE =================
        h.btnDelete.setOnClickListener(v -> {
            if (uid == null || note.id == null) return;

            new AlertDialog.Builder(v.getContext())
                    .setTitle("Xóa ghi chú?")
                    .setMessage("Ghi chú sẽ bị xóa vĩnh viễn.")
                    .setPositiveButton("Xóa", (d, w) -> {

                        // 1️⃣ TÌM POSITION TRƯỚC
                        int pos = findPositionById(note.id);
                        if (pos == -1) return;

                        // 2️⃣ XÓA LOCAL TRƯỚC (🔥 UI BIẾN NGAY)
                        notes.remove(pos);
                        notifyItemRemoved(pos);

                        // 3️⃣ XÓA FIRESTORE (SYNC NGẦM)
                        db.collection("users")
                                .document(uid)
                                .collection("notes")
                                .document(note.id)
                                .delete();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });


        // ================= RENAME NOTE (🔥 FIX CHÍNH) =================
        h.btnRename.setOnClickListener(v -> showRenameDialog(v.getContext(), note));
    }

    // ================= RENAME DIALOG =================
    private void showRenameDialog(Context ctx, Note note) {
        if (uid == null || note.id == null) return;

        EditText edt = new EditText(ctx);
        edt.setText(note.title);
        edt.setSelection(edt.getText().length());

        new AlertDialog.Builder(ctx)
                .setTitle("Đổi tên ghi chú")
                .setView(edt)
                .setPositiveButton("Lưu", (d, w) -> {
                    String newTitle = edt.getText().toString().trim();
                    if (newTitle.isEmpty()) return;

                    // 1️⃣ UPDATE FIRESTORE
                    db.collection("users")
                            .document(uid)
                            .collection("notes")
                            .document(note.id)
                            .update(
                                    "title", newTitle,
                                    "updatedAt", Timestamp.now()
                            );

                    // 2️⃣ UPDATE LOCAL DATA (🔥 QUAN TRỌNG)
                    note.title = newTitle;

                    // 3️⃣ UPDATE UI NGAY LẬP TỨC
                    int pos = findPositionById(note.id);
                    if (pos != -1) {
                        notifyItemChanged(pos);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ================= FIND POSITION =================
    private int findPositionById(String noteId) {
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).id != null && notes.get(i).id.equals(noteId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return notes == null ? 0 : notes.size();
    }
}
