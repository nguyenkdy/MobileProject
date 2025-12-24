package com.example.mynoesapplication;

import com.google.firebase.Timestamp;

public class Note {

    // ================= CORE =================
    public String id;
    public String title;
    public String content;

    // ================= OWNER / AUTHOR =================
    /**
     * UID người tạo note
     * - KHÔNG dùng để phân quyền folder
     * - chỉ để hiển thị / audit
     */
    public String authorId;

    // ================= FOLDER =================
    /**
     * ID folder chứa note
     * - folder cá nhân: users/{uid}/folders/{folderId}
     * - folder chia sẻ: users/{ownerUid}/folders/{folderId}
     */
    public String folderId;

    // ================= SHARING =================
    /**
     * Note có thuộc folder chia sẻ không
     */
    public boolean isShared = false;

    /**
     * roomCode của folder chia sẻ
     * -> notes sẽ nằm trong rooms/{roomCode}/notes
     */
    public String roomCode;

    // ================= PIN =================
    public boolean isPinned = false;
    public long pinnedAt = 0; // dùng sort pinned

    // ================= TYPE =================
    /**
     * "note" | "pdf"
     */
    public String type = "note";

    /**
     * Path PDF (nếu type == "pdf")
     */
    public String pdfPath;

    // ================= TIME =================
    public Timestamp createdAt;
    public Timestamp updatedAt;

    // ================= TRASH =================
    public boolean deleted = false;
    public Timestamp deletedAt;

    // ================= UI STATE (LOCAL ONLY) =================
    public boolean selected = false;

    // ================= CONSTRUCTOR =================
    // ⚠️ BẮT BUỘC cho Firestore
    public Note() {}

    // ================= HELPER =================

    /** Note này có thuộc folder chia sẻ không */
    public boolean isSharedNote() {
        return isShared && roomCode != null && !roomCode.trim().isEmpty();
    }

    /** Cập nhật updatedAt (dùng cho realtime sync) */
    public void touch() {
        this.updatedAt = Timestamp.now();
    }
}
