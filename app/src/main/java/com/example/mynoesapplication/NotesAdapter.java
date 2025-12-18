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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Note> notes;
    private FirebaseFirestore db;
    private String uid;

    public NotesAdapter(List<Note> notes) {
        this.notes = notes;
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
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

        h.txtTitle.setText(note.title == null ? "Không tiêu đề" : note.title);
        h.txtContent.setText(note.content == null ? "" : note.content);

        // ================= CLICK NOTE =================
        h.itemView.setOnClickListener(v -> {
            v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80);

                        Intent i = new Intent(v.getContext(), EditNoteActivity.class);
                        i.putExtra("noteId", note.id);
                        i.putExtra("title", note.title);
                        i.putExtra("content", note.content);
                        v.getContext().startActivity(i);
                    });
        });


        // ================= DELETE =================
        h.btnDelete.setOnClickListener(v -> {
            if (uid == null || note.id == null) return;

            db.collection("users")
                    .document(uid)
                    .collection("notes")
                    .document(note.id)
                    .delete();
        });

        // ================= RENAME =================
        h.btnRename.setOnClickListener(v -> {
            showRenameDialog(v.getContext(), note);
        });
    }

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

                    db.collection("users")
                            .document(uid)
                            .collection("notes")
                            .document(note.id)
                            .update("title", newTitle);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return notes == null ? 0 : notes.size();
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
}
