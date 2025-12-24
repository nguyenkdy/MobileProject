package com.example.mynoesapplication;

import com.google.firebase.Timestamp;

public class Folder {

    // ================= CORE =================
    public String id;                 // document id
    public String name;               // folder name
    public Timestamp createdAt;

    // ================= UI STATE =================
    public boolean pinned = false;    // chỉ áp dụng cho folder của mình
    public boolean selected = false;  // dùng cho edit mode (local only)

    // ================= TRASH =================
    public boolean deleted = false;
    public Timestamp deletedAt;

    // ================= SHARING =================
    /**
     * Folder có phải là folder chia sẻ không
     * false -> folder cá nhân
     * true  -> folder thuộc room
     */
    public boolean isShared = false;

    /**
     * roomCode nếu là folder chia sẻ
     * null nếu là folder cá nhân
     */
    public String roomCode;

    /**
     * UID của người tạo folder (owner)
     * - folder cá nhân: ownerId == uid hiện tại
     * - folder chia sẻ: ownerId != uid hiện tại
     */
    public String ownerId;

    /**
     * ID folder gốc của owner
     * Chỉ dùng khi folder này là bản "joined"
     */
    public String originalFolderId;

    // ================= CONSTRUCTOR =================
    // ⚠️ BẮT BUỘC cho Firestore
    public Folder() {}

    // ================= HELPER =================
    /**
     * Folder này có phải của mình không
     */
    public boolean isOwnedBy(String uid) {
        return ownerId != null && ownerId.equals(uid);
    }

    /**
     * Folder này có phải folder chia sẻ không
     */
    public boolean isSharedFolder() {
        return isShared && roomCode != null && !roomCode.isEmpty();
    }
}
