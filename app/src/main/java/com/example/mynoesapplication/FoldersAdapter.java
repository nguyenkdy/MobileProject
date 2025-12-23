 package com.example.mynoesapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.Folder;
import com.example.mynoesapplication.FolderSharingActivity;
import com.example.mynoesapplication.R;
import com.example.mynoesapplication.ReadOnlyNotesActivity;
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

        boolean isOwned = uid != null && f.ownerId != null && f.ownerId.equals(uid);
        if (f.ownerId == null) isOwned = true;
        final boolean isSharedReadOnly = !isOwned;

        // avoid adding the suffix twice: only append when not already present
        String displayName = f.name == null ? "" : f.name;
        if (isSharedReadOnly) {
            if (!displayName.endsWith(" (Chia sẻ)")) {
                displayName = displayName + " (Chia sẻ)";
            }
        }
        h.txtFolderName.setText(displayName);

        // ===== CREATED AT =====
        if (f.createdAt != null) {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            h.txtFolderTime.setText(sdf.format(f.createdAt.toDate()));
            h.txtFolderTime.setVisibility(View.VISIBLE);
        } else {
            h.txtFolderTime.setVisibility(View.GONE);
        }

        // ===== EDIT MODE UI =====
        h.chkSelect.setVisibility((isEditMode && !isSharedReadOnly) ? View.VISIBLE : View.GONE);
        h.btnOption.setVisibility(isEditMode ? View.GONE : (isSharedReadOnly ? View.GONE : View.VISIBLE));

        // manage checkbox only for owned folders
        h.chkSelect.setOnCheckedChangeListener(null);
        if (!isSharedReadOnly) {
            h.chkSelect.setChecked(f.selected);
            h.chkSelect.setOnCheckedChangeListener((b, checked) -> f.selected = checked);
        } else {
            h.chkSelect.setChecked(false);
        }

        // ===== CLICK ITEM =====
        h.itemView.setOnClickListener(v -> {

            if (isEditMode && !isSharedReadOnly) {
                f.selected = !f.selected;
                h.chkSelect.setChecked(f.selected);
                return;
            }

            if (isSharedReadOnly) {
                String ownerFolderId = (f.originalFolderId != null && !f.originalFolderId.isEmpty()) ? f.originalFolderId : f.id;
                // pass a clean name (without duplicated suffix)
                String cleanName = (f.name == null) ? "" : f.name;
                if (cleanName.endsWith(" (Chia sẻ)")) {
                    cleanName = cleanName.substring(0, cleanName.length() - " (Chia sẻ)".length()).trim();
                }
                openReadOnly(ctx, f.ownerId, ownerFolderId, cleanName);
                return;
            }

            v.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80);
                        if (listener != null) listener.onFolderClick(f);
                    });
        });

        // ===== PINNED UI =====
        if (f.pinned) {
            h.itemView.setBackgroundResource(R.drawable.bg_folder_pinned);
            if (h.imgPinned != null) h.imgPinned.setVisibility(View.VISIBLE);
        } else {
            h.itemView.setBackgroundResource(R.drawable.bg_folder_normal);
            if (h.imgPinned != null) h.imgPinned.setVisibility(View.GONE);
        }



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

        final boolean owned = (folder.ownerId == null) || (uid != null && folder.ownerId != null && folder.ownerId.equals(uid));
        if (!owned) {
            // Use originalFolderId for read-only access
            String ownerFolderId = (folder.originalFolderId != null && !folder.originalFolderId.isEmpty()) ? folder.originalFolderId : folder.id;
            openReadOnly(ctx, folder.ownerId, ownerFolderId, folder.name);
            return;
        }

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

        popupView.findViewById(R.id.optShareFolder).setOnClickListener(v -> {
            popup.dismiss();
            // Owner: create (or reuse) room code and open sharing view showing the room code.
            createRoomForFolder(ctx, folder);
        });
    }

    // Helper: create room code if missing, save to Firestore, then open FolderSharingActivity with roomCode
    private void createRoomForFolder(Context ctx, Folder folder) {
        if (uid == null || folder == null || folder.id == null) {
            android.widget.Toast.makeText(ctx, "Cannot create room: missing data", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // If folder already has a roomCode, ensure rooms collection exists and open
        if (folder.roomCode != null && !folder.roomCode.trim().isEmpty()) {
            // ensure rooms/{roomCode} exists (best-effort, no blocking)
            java.util.Map<String, Object> roomDoc = new java.util.HashMap<>();
            roomDoc.put("ownerUid", uid);
            roomDoc.put("folderId", folder.id);
            roomDoc.put("folderName", folder.name);
            roomDoc.put("createdAt", com.google.firebase.Timestamp.now());

            db.collection("rooms")
                    .document(folder.roomCode)
                    .set(roomDoc)
                    .addOnFailureListener(e -> {
                        android.widget.Toast.makeText(ctx, "Warning: failed to sync room metadata: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    });

            openFolderSharingActivity(ctx, uid, folder.id, folder.name, folder.roomCode, true);
            return;
        }

        final String roomCode = generateRoomCode(6);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("roomCode", roomCode);
        if (folder.originalFolderId == null || folder.originalFolderId.isEmpty()) {
            updates.put("originalFolderId", folder.id);
        }

        // 1) update folder document
        db.collection("users")
                .document(uid)
                .collection("folders")
                .document(folder.id)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // update local model
                    folder.roomCode = roomCode;

                    // 2) write top-level rooms collection for fast lookup
                    java.util.Map<String, Object> roomDoc = new java.util.HashMap<>();
                    roomDoc.put("ownerUid", uid);
                    roomDoc.put("folderId", folder.id);
                    roomDoc.put("folderName", folder.name);
                    roomDoc.put("createdAt", com.google.firebase.Timestamp.now());

                    db.collection("rooms")
                            .document(roomCode)
                            .set(roomDoc)
                            .addOnSuccessListener(v -> {
                                openFolderSharingActivity(ctx, uid, folder.id, folder.name, roomCode, true);
                            })
                            .addOnFailureListener(e -> {
                                // still open sharing view even if rooms write failed
                                android.widget.Toast.makeText(ctx, "Room created but rooms index write failed: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                                openFolderSharingActivity(ctx, uid, folder.id, folder.name, roomCode, true);
                            });
                })
                .addOnFailureListener(e -> android.widget.Toast.makeText(ctx, "Create room failed: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show());
    }
    private void openFolderSharingActivity(Context ctx, String ownerUid, String folderId, String folderName, String roomCode, boolean isOwner) {
        Intent i = new Intent(ctx, FolderSharingActivity.class);
        if (ownerUid != null) i.putExtra("ownerUid", ownerUid);
        if (folderId != null) i.putExtra("folderId", folderId);
        if (folderName != null) i.putExtra("folderName", folderName);
        if (roomCode != null) i.putExtra("roomCode", roomCode);
        i.putExtra("isOwner", isOwner);
        if (!(ctx instanceof Activity)) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        ctx.startActivity(i);
    }

    // simple alphanumeric room code
    private String generateRoomCode(int length) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        java.util.Random rnd = new java.util.Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
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

        // 1️⃣ update local ngay
        folder.pinned = newValue;

        // 2️⃣ báo cho Activity sort lại
        if (pinChangedListener != null) {
            pinChangedListener.onFolderPinChanged();
        }

        // 3️⃣ sync Firestore
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
                )
                .addOnSuccessListener(aVoid -> {
                    // ✅ chỉ OWNER mới cleanup
                    if (uid != null && (folder.ownerId == null || uid.equals(folder.ownerId))) {
                        cleanupJoinedFolders(folder, uid);
                    }
                })
                .addOnFailureListener(e ->
                        android.util.Log.w("FoldersAdapter", "Delete folder failed: " + e.getMessage())
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

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // ✅ chỉ OWNER mới cleanup
                    if (uid != null && (folder.ownerId == null || uid.equals(folder.ownerId))) {
                        cleanupJoinedFolders(folder, uid);
                    }
                })
                .addOnFailureListener(e ->
                        android.util.Log.w("FoldersAdapter",
                                "Failed to commit moveFolderAndNotes: " + e.getMessage())
                );
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
        ImageView imgPinned; // ⭐ ADD

        TextView txtFolderTime;   // ⭐ ADD



        FolderViewHolder(@NonNull View v) {
            super(v);
            txtFolderName = v.findViewById(R.id.txtFolderName);
            btnOption = v.findViewById(R.id.btnFolderOption);
            chkSelect = v.findViewById(R.id.chkSelectFolder);
            imgPinned = v.findViewById(R.id.imgPinned); // ⭐ ADD
            txtFolderTime = v.findViewById(R.id.txtFolderTime); // ⭐ ADD


        }
    }

    // ==================================================
    // HELPERS
    // ==================================================
    private void openReadOnly(Context ctx, String ownerUid, String folderId, String folderName) {
        Intent i = new Intent(ctx, ReadOnlyNotesActivity.class);
        i.putExtra("ownerUid", ownerUid);
        i.putExtra("folderId", folderId);
        i.putExtra("folderName", folderName);
        if (!(ctx instanceof Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }
    // Forwarding overload that uses this adapter's uid
    private void cleanupJoinedFolders(Folder folder) {
        cleanupJoinedFolders(folder, folder.ownerId);
    }
    // Main cleanup implementation: delete rooms/{roomCode} and all users/*/joinedFolders entries
    private void cleanupJoinedFolders(Folder folder, String ownerUid) {
        if (folder == null || folder.roomCode == null || folder.roomCode.isEmpty()) return;

        String roomCode = folder.roomCode;

        // 1️⃣ XÓA TẤT CẢ joinedFolders/{roomCode}
        db.collectionGroup("joinedFolders")
                .get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot ds : snapshot.getDocuments()) {
                        if (roomCode.equals(ds.getId())) {
                            batch.delete(ds.getReference());
                        }
                    }

                    batch.commit().addOnSuccessListener(v -> {
                        // 2️⃣ SAU KHI joinedFolders đã xóa → xóa rooms
                        db.collection("rooms")
                                .document(roomCode)
                                .delete();
                    });
                });
    }

    public interface OnFolderPinChangedListener {
        void onFolderPinChanged();
    }

    private OnFolderPinChangedListener pinChangedListener;

    public void setOnFolderPinChangedListener(OnFolderPinChangedListener l) {
        this.pinChangedListener = l;
    }

    private void cleanupStaleJoinedFolders() {
        if (uid == null) return;

        db.collection("users")
                .document(uid)
                .collection("joinedFolders")
                .get()
                .addOnSuccessListener(joinedSnap -> {
                    for (com.google.firebase.firestore.DocumentSnapshot jd : joinedSnap.getDocuments()) {
                        final String roomCode = jd.getId();
                        if (roomCode == null || roomCode.isEmpty()) continue;

                        db.collection("rooms")
                                .document(roomCode)
                                .get()
                                .addOnSuccessListener(roomDoc -> {
                                    if (roomDoc == null || !roomDoc.exists()) {
                                        // rooms/{roomCode} removed -> delete this user's joinedFolders entry
                                        db.collection("users")
                                                .document(uid)
                                                .collection("joinedFolders")
                                                .document(roomCode)
                                                .delete()
                                                .addOnFailureListener(e -> android.util.Log.w(
                                                        "NotesActivity",
                                                        "Failed to delete stale joinedFolders/" + roomCode + ": " + e.getMessage()
                                                ));
                                    }
                                })
                                .addOnFailureListener(e -> android.util.Log.w("NotesActivity", "Failed to check rooms/" + roomCode + ": " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> android.util.Log.w("NotesActivity", "Failed to load joinedFolders: " + e.getMessage()));
    }

}
