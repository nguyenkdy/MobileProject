// java
package com.example.mynoesapplication;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class FolderSharingActivity extends AppCompatActivity {

    private View ownerContainer;
    private View joinContainer;
    private TextView txtRoomCode;
    private TextView txtOwnerFolderName;
    private EditText edtRoomCode;
    private Button btnCopyCode;
    private Button btnJoin;

    private String roomCode;
    private boolean isOwner;
    private String folderName;
    private String ownerUid;
    private String folderId;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_detail); // or your layout

        db = FirebaseFirestore.getInstance();

        ownerContainer = findViewById(R.id.owner_container);
        joinContainer = findViewById(R.id.join_container);
        txtRoomCode = findViewById(R.id.txtRoomCode);
        txtOwnerFolderName = findViewById(R.id.txtOwnerFolderName);
        edtRoomCode = findViewById(R.id.edtRoomCode);
        btnCopyCode = findViewById(R.id.btnCopyCode);
        btnJoin = findViewById(R.id.btnJoin);

        Intent intent = getIntent();
        if (intent != null) {
            ownerUid = intent.getStringExtra("ownerUid");
            folderId = intent.getStringExtra("folderId");
            roomCode = intent.getStringExtra("roomCode");
            isOwner = intent.getBooleanExtra("isOwner", false);
            folderName = intent.getStringExtra("folderName");
        }

        if (isOwner || (roomCode != null && !roomCode.trim().isEmpty())) {
            showOwnerView(roomCode, folderName);
        } else {
            showJoinView();
        }

        if (btnCopyCode != null) {
            btnCopyCode.setOnClickListener(v -> {
                if (roomCode == null || roomCode.isEmpty()) return;
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("roomCode", roomCode));
                    Toast.makeText(this, "Room code copied", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnJoin != null && edtRoomCode != null) {
            btnJoin.setOnClickListener(v -> {
                String code = edtRoomCode.getText().toString().trim();
                if (code.isEmpty()) {
                    Toast.makeText(this, "Enter room code", Toast.LENGTH_SHORT).show();
                    return;
                }
                performJoinByCode(code);
            });
        }
    }

    private void performJoinByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            Toast.makeText(this, "Please enter a code", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (currentUid == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Look up room metadata
        db.collection("rooms")
                .document(code)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(this, "Room not found", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String ownerUid = doc.getString("ownerUid");
                    String folderId = doc.getString("folderId");
                    String folderName = doc.getString("folderName");

                    if (ownerUid == null || folderId == null) {
                        Toast.makeText(this, "Invalid room data", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Persist joined folder reference under current user
                    java.util.Map<String, Object> joined = new java.util.HashMap<>();
                    joined.put("ownerUid", ownerUid);
                    joined.put("folderId", folderId);
                    joined.put("folderName", folderName != null ? folderName : "");
                    joined.put("joinedAt", com.google.firebase.Timestamp.now());

                    db.collection("users")
                            .document(currentUid)
                            .collection("joinedFolders")
                            .document(code)
                            .set(joined)
                            .addOnSuccessListener(aVoid -> {
                                // Open read-only notes view for the joined folder
                                Intent i = new Intent(this, ReadOnlyNotesActivity.class);
                                i.putExtra("ownerUid", ownerUid);
                                i.putExtra("folderId", folderId);
                                i.putExtra("folderName", folderName);
                                if (!(this instanceof android.app.Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Join saved but failed to persist: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(err -> {
                    String msg = err.getMessage() == null ? "Join failed" : err.getMessage();
                    Toast.makeText(this, "Join failed: " + msg, Toast.LENGTH_LONG).show();
                });
    }
    private void showOwnerView(String code, String folderName) {
        if (ownerContainer != null) ownerContainer.setVisibility(View.VISIBLE);
        if (joinContainer != null) joinContainer.setVisibility(View.GONE);

        if (txtRoomCode != null) txtRoomCode.setText(code != null ? code : "");
        if (txtOwnerFolderName != null) txtOwnerFolderName.setText(folderName != null ? folderName : "");
    }

    private void showJoinView() {
        if (ownerContainer != null) ownerContainer.setVisibility(View.GONE);
        if (joinContainer != null) joinContainer.setVisibility(View.VISIBLE);

        if (edtRoomCode != null && roomCode != null) edtRoomCode.setText(roomCode);
    }
}
