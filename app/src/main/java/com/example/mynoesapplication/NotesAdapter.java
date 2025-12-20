package com.example.mynoesapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
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

import com.example.mynoesapplication.Data.*;
import com.example.mynoesapplication.Fragment.*;
import com.example.mynoesapplication.RetrofitClient.*;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.Fragment.NoteSummaryFragment;
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
        ImageView imgPreview;   // ⭐ THÊM DÒNG NÀY

        ImageButton btnOptions;
        CheckBox chkSelect;

        public NoteViewHolder(@NonNull View v) {
            super(v);
            txtTitle = v.findViewById(R.id.txtTitle);
            txtContent = v.findViewById(R.id.txtContent);
            btnOptions = v.findViewById(R.id.btnOptions);
            chkSelect = v.findViewById(R.id.chkSelect);
            imgPreview = v.findViewById(R.id.imgPreview); // ⭐⭐⭐ THIẾU DÒNG NÀY
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

        h.txtTitle.setText(note.title);

        Bitmap thumb = ThumbnailCache.load(
                h.itemView.getContext(),
                note.id
        );

        if (thumb != null) {
            // ✅ Có nét vẽ → hiện preview
            h.imgPreview.setVisibility(View.VISIBLE);
            h.imgPreview.setImageBitmap(thumb);
        } else {
            // ❌ Không có nét vẽ → ẩn hoàn toàn preview
            h.imgPreview.setVisibility(View.GONE);
        }

        // ===== TITLE =====
        h.txtTitle.setText(
                note.title == null || note.title.trim().isEmpty()
                        ? "Không tiêu đề"
                        : note.title.trim()
        );

        // ===== CONTENT PREVIEW =====
        if (note.content == null || note.content.trim().isEmpty()) {
            h.txtContent.setText("");
        } else {
            String preview = note.content.trim().replaceAll("\\n{2,}", "\n");
            if (preview.length() > 150) {
                preview = preview.substring(0, 150) + "...";
            }
            h.txtContent.setText(preview);
        }


        // ===== EDIT MODE =====
        h.chkSelect.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        h.chkSelect.setChecked(note.selected);
        h.btnOptions.setVisibility(isEditMode ? View.GONE : View.VISIBLE);

        // CLICK CHECKBOX
        h.chkSelect.setOnClickListener(v -> {
            note.selected = h.chkSelect.isChecked();
        });

        // ===== CLICK CARD (GIỮ ANIMATION CŨ) =====
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
                        } else {
                            Context ctx = v.getContext();
                            Intent i = new Intent(ctx, EditNoteActivity.class);
                            i.putExtra("noteId", note.id);
                            ctx.startActivity(i);
                        }
                    });
        });

        // ===== OPTIONS =====
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

        popupView.findViewById(R.id.optSummary).setOnClickListener(v -> {
            popup.dismiss();
            handleOptSummary(anchor, note);
            //Toast.makeText(anchor.getContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
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

    // ================= SUMMARY =================

    private void handleOptSummary(View anchor, Note note) {
        Context ctx = anchor.getContext();
        if (!(ctx instanceof FragmentActivity)) {
            Toast.makeText(ctx, "Need FragmentActivity to show summary", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) Extract text data only (title + content)
        String title = note.title == null ? "" : note.title.trim();
        String content = note.content == null ? "" : note.content.trim();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Summarize the following note in 2-10 concise sentences or rows.\n\n");
        if (!title.isEmpty()) {
            prompt.append("Title: ").append(title).append("\n\n");
        }
        if (!content.isEmpty()) {
            prompt.append("Content:\n").append(content).append("\n\n");
        }
        prompt.append("Return a short, clear summary and in Vietnamese.");

        // 2) Show right-side fragment (or overlay) and display loading
        FragmentActivity activity = (FragmentActivity) ctx;
        NoteSummaryFragment frag = NoteSummaryFragment.findOrCreate(activity.getSupportFragmentManager());
        frag.showLoading();

        // 3) Call AI service (Retrofit) - text only
        AiRequest req = new AiRequest(prompt.toString());
        AiApiService.getApi().summarize(req)
                .enqueue(new Callback<AiResponse>() {
                    @Override
                    public void onResponse(Call<AiResponse> call, Response<AiResponse> response) {

                        Log.d("AI_API", "HTTP code = " + response.code());

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getSummary() != null) {

                            frag.showSummary(response.body().getSummary());

                        } else {
                            String msg = "AI error";
                            try {
                                if (response.errorBody() != null) {
                                    msg = response.errorBody().string();
                                    Log.e("AI_API", "Error body: " + msg);
                                } else {
                                    Log.e("AI_API", "Error body is null");
                                }
                            } catch (Exception e) {
                                Log.e("AI_API", "Read errorBody failed", e);
                            }

                            frag.showSummary("Failed to summarize: " + msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<AiResponse> call, Throwable t) {
                        Log.e("AI_API", "Call failed", t);
                        frag.showSummary("AI call failed: " + t.getMessage());
                    }
                });

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
