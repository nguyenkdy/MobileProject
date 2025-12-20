package com.example.mynoesapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private final List<Note> notes;
    private final FirebaseFirestore db;
    private final String uid;
    private boolean isEditMode = false;

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
        ImageView imgPreview;
        ImageButton btnOptions;
        CheckBox chkSelect;

        public NoteViewHolder(@NonNull View v) {
            super(v);
            txtTitle = v.findViewById(R.id.txtTitle);
            txtContent = v.findViewById(R.id.txtContent);
            btnOptions = v.findViewById(R.id.btnOptions);
            chkSelect = v.findViewById(R.id.chkSelect);
            imgPreview = v.findViewById(R.id.imgPreview);
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

        // ================= TITLE =================
        h.txtTitle.setText(
                note.title == null || note.title.trim().isEmpty()
                        ? "Không tiêu đề"
                        : note.title.trim()
        );

        // ================= PREVIEW IMAGE =================
        Bitmap thumb = ThumbnailCache.load(
                h.itemView.getContext(),
                note.id
        );

        if (thumb != null) {
            h.imgPreview.setVisibility(View.VISIBLE);
            h.imgPreview.setImageBitmap(thumb);
        } else {
            h.imgPreview.setVisibility(View.GONE);

            // 🔥 CHỈ TẠO THUMBNAIL CHO PDF
            if ("pdf".equalsIgnoreCase(note.type)
                    && note.pdfPath != null
                    && !note.pdfPath.trim().isEmpty()) {

                new Thread(() -> {
                    Bitmap preview = NoteThumbnailRenderer
                            .renderPdfPreview(note.pdfPath);

                    if (preview != null) {
                        ThumbnailCache.save(
                                h.itemView.getContext(),
                                note.id,
                                preview
                        );

                        h.itemView.post(() -> {
                            h.imgPreview.setVisibility(View.VISIBLE);
                            h.imgPreview.setImageBitmap(preview);
                        });
                    }
                }).start();
            }
        }

        // ================= CONTENT PREVIEW =================
        if (note.content == null || note.content.trim().isEmpty()) {
            h.txtContent.setText("");
        } else {
            String previewText = note.content.trim().replaceAll("\\n{2,}", "\n");
            if (previewText.length() > 150) {
                previewText = previewText.substring(0, 150) + "...";
            }
            h.txtContent.setText(previewText);
        }

        // ================= EDIT MODE =================
        h.chkSelect.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        h.chkSelect.setChecked(note.selected);
        h.btnOptions.setVisibility(isEditMode ? View.GONE : View.VISIBLE);

        h.chkSelect.setOnClickListener(v ->
                note.selected = h.chkSelect.isChecked()
        );

        // ==================================================
        // ⭐ CLICK CARD
        // ==================================================
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

                        if (isEditMode) {
                            note.selected = !note.selected;
                            h.chkSelect.setChecked(note.selected);
                            return;
                        }

                        Context ctx = v.getContext();

                        // 🔥 PDF NOTE
                        if ("pdf".equalsIgnoreCase(note.type)) {

                            if (note.pdfPath == null || note.pdfPath.trim().isEmpty()) {
                                Toast.makeText(ctx, "Không tìm thấy PDF", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Intent i = new Intent(ctx, PdfEditorActivity.class);
                            i.putExtra("noteId", note.id);
                            i.putExtra("pdfPath", note.pdfPath);
                            ctx.startActivity(i);
                            return;
                        }

                        // 🔹 NORMAL NOTE
                        Intent i = new Intent(ctx, EditNoteActivity.class);
                        i.putExtra("noteId", note.id);
                        ctx.startActivity(i);
                    });
        });

        // ================= OPTIONS =================
        h.btnOptions.setOnClickListener(v -> showNoteOptions(v, note));
    }


    // ================= POPUP OPTIONS =================
    private void showNoteOptions(View anchor, Note note) {
        View popupView = LayoutInflater.from(anchor.getContext())
                .inflate(R.layout.popup_note_options, null);

        PopupWindow popup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popup.setElevation(12f);
        popup.setOutsideTouchable(true);
        popup.showAsDropDown(anchor, -120, 10);

        popupView.findViewById(R.id.optRename).setOnClickListener(v -> {
            popup.dismiss();
            showRenameDialog(anchor.getContext(), note);
        });

        popupView.findViewById(R.id.optPin).setOnClickListener(v -> {
            popup.dismiss();
            togglePin(note);
        });

        popupView.findViewById(R.id.optDelete).setOnClickListener(v -> {
            popup.dismiss();
            moveToTrash(anchor.getContext(), note);
        });
    }

    // ================= RENAME =================
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
                            .update(
                                    "title", newTitle,
                                    "updatedAt", Timestamp.now()
                            );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ================= MOVE TO TRASH =================
    private void moveToTrash(Context ctx, Note note) {
        if (uid == null || note.id == null) return;

        new AlertDialog.Builder(ctx)
                .setTitle("Chuyển vào Thùng rác?")
                .setPositiveButton("Đồng ý", (d, w) -> {
                    db.collection("users")
                            .document(uid)
                            .collection("notes")
                            .document(note.id)
                            .update(
                                    "deleted", true,
                                    "deletedAt", Timestamp.now()
                            );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ================= PIN =================
    private void togglePin(Note note) {
        if (uid == null || note.id == null) return;

        db.collection("users")
                .document(uid)
                .collection("notes")
                .document(note.id)
                .update(
                        "isPinned", !note.isPinned,
                        "updatedAt", Timestamp.now()
                );
    }

    // ================= MULTI SELECT =================
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    public List<Note> getSelectedNotes() {
        List<Note> res = new ArrayList<>();
        for (Note n : notes) if (n.selected) res.add(n);
        return res;
    }

    public void selectAll(boolean value) {
        for (Note n : notes) n.selected = value;
        notifyDataSetChanged();
    }

    public void clearSelection() {
        for (Note n : notes) n.selected = false;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return notes == null ? 0 : notes.size();
    }
}

