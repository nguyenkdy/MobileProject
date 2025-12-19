package com.example.mynoesapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class FoldersAdapter extends RecyclerView.Adapter<FoldersAdapter.FolderViewHolder> {

    // ================= INTERFACE =================
    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
    }

    // ================= DATA =================
    private final List<Folder> folders;
    private final OnFolderClickListener listener;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String uid;

    // ================= EDIT MODE =================
    private boolean isEditMode = false;

    // ================= CONSTRUCTOR =================
    public FoldersAdapter(List<Folder> folders, OnFolderClickListener listener) {
        this.folders = folders;
        this.listener = listener;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    // ================= ADAPTER =================
    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder h, int position) {
        Folder f = folders.get(position);
        Context ctx = h.itemView.getContext();

        h.txtFolderName.setText(f.name);

        // ===== EDIT MODE UI =====
        h.chkSelect.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        h.btnOption.setVisibility(isEditMode ? View.GONE : View.VISIBLE);

        // ❗ tránh bug listener bị gọi lại
        h.chkSelect.setOnCheckedChangeListener(null);
        h.chkSelect.setChecked(f.selected);
        h.chkSelect.setOnCheckedChangeListener((b, checked) -> f.selected = checked);

        // ===== CLICK ITEM =====
        h.itemView.setOnClickListener(v -> {

            if (isEditMode) {
                f.selected = !f.selected;
                h.chkSelect.setChecked(f.selected);
                return;
            }

            // 🔥 GIỮ ANIMATION CŨ
            v.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80);
                        if (listener != null) listener.onFolderClick(f);
                    });
        });

        // ===== OPTION MENU =====
        h.btnOption.setOnClickListener(v -> showFolderPopup(v, f));
    }

    @Override
    public int getItemCount() {
        return folders == null ? 0 : folders.size();
    }

    // ==================================================
    // POPUP OPTION (RENAME / PIN / DELETE)
    // ==================================================
    private void showFolderPopup(View anchor, Folder folder) {
        Context ctx = anchor.getContext();

        View popupView = LayoutInflater.from(ctx)
                .inflate(R.layout.popup_folder_option, null);

        PopupWindow popup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popup.setElevation(10f);
        popup.showAsDropDown(anchor, -200, 0);

        popupView.findViewById(R.id.optRenameFolder).setOnClickListener(v -> {
            popup.dismiss();
            showRenameDialog(ctx, folder);
        });

        popupView.findViewById(R.id.optPinFolder).setOnClickListener(v -> {
            popup.dismiss();
            togglePin(folder);
        });

        popupView.findViewById(R.id.optDeleteFolder).setOnClickListener(v -> {
            popup.dismiss();
            moveFolderToTrash(ctx, folder);
        });
    }

    // ==================================================
    // RENAME
    // ==================================================
    private void showRenameDialog(Context ctx, Folder folder) {
        EditText input = new EditText(ctx);
        input.setText(folder.name);

        new AlertDialog.Builder(ctx)
                .setTitle("Đổi tên thư mục")
                .setView(input)
                .setPositiveButton("Lưu", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) return;

                    db.collection("users")
                            .document(uid)
                            .collection("folders")
                            .document(folder.id)
                            .update("name", newName);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ==================================================
    // PIN / UNPIN
    // ==================================================
    private void togglePin(Folder folder) {
        boolean newValue = !folder.pinned;

        db.collection("users")
                .document(uid)
                .collection("folders")
                .document(folder.id)
                .update("pinned", newValue);
    }

    // ==================================================
    // MOVE FOLDER TO TRASH (1 FOLDER)
    // ==================================================
    private void moveFolderToTrash(Context ctx, Folder folder) {

        db.collection("users")
                .document(uid)
                .collection("notes")
                .whereEqualTo("folderId", folder.id)
                .whereEqualTo("deleted", false)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        moveFolderOnly(folder);
                        return;
                    }

                    new AlertDialog.Builder(ctx)
                            .setTitle("Chuyển vào Thùng rác")
                            .setMessage("Toàn bộ ghi chú trong thư mục cũng sẽ bị chuyển.")
                            .setPositiveButton("Đồng ý", (d, w) ->
                                    moveFolderAndNotes(folder, snapshot))
                            .setNegativeButton("Hủy", null)
                            .show();
                });
    }

    private void moveFolderOnly(Folder folder) {
        db.collection("users")
                .document(uid)
                .collection("folders")
                .document(folder.id)
                .update(
                        "deleted", true,
                        "deletedAt", Timestamp.now()
                );
    }

    private void moveFolderAndNotes(Folder folder, QuerySnapshot notesSnapshot) {
        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();

        for (DocumentSnapshot doc : notesSnapshot.getDocuments()) {
            batch.update(doc.getReference(),
                    "deleted", true,
                    "deletedAt", now);
        }

        batch.update(
                db.collection("users")
                        .document(uid)
                        .collection("folders")
                        .document(folder.id),
                "deleted", true,
                "deletedAt", now
        );

        batch.commit();
    }

    // ==================================================
    // ===== EDIT MODE API (CHO NotesActivity) =====
    // ==================================================
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    public void clearSelection() {
        for (Folder f : folders) {
            f.selected = false;
        }
        notifyDataSetChanged();
    }

    public void selectAll(boolean value) {
        for (Folder f : folders) {
            f.selected = value;
        }
        notifyDataSetChanged();
    }

    public List<Folder> getSelectedFolders() {
        List<Folder> result = new ArrayList<>();
        for (Folder f : folders) {
            if (f.selected) result.add(f);
        }
        return result;
    }

    public boolean hasSelection() {
        for (Folder f : folders) {
            if (f.selected) return true;
        }
        return false;
    }

    // ==================================================
    // VIEW HOLDER
    // ==================================================
    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView txtFolderName;
        ImageButton btnOption;
        CheckBox chkSelect;

        FolderViewHolder(@NonNull View v) {
            super(v);
            txtFolderName = v.findViewById(R.id.txtFolderName);
            btnOption = v.findViewById(R.id.btnFolderOption);
            chkSelect = v.findViewById(R.id.chkSelectFolder);
        }
    }
}
