package com.example.mynoesapplication.Fragment;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mynoesapplication.ReadOnlyNotesActivity;
import com.example.mynoesapplication.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FolderSharingFragment extends DialogFragment {

    private static final String ARG_OWNER_UID = "ownerUid";
    private static final String ARG_FOLDER_ID = "folderId";
    private static final String ARG_ROOM_CODE = "roomCode";
    private static final String ARG_IS_OWNER = "isOwner";
    private static final String ARG_FOLDER_NAME = "folderName";

    private View cardContainer;
    private EditText edtRoomCode;
    private Button btnJoin;
    private Button btnCopyCode;
    private TextView txtRoomCode;
    private TextView txtOwnerFolderName;

    private String roomCode;
    private boolean isOwner;
    private String folderName;
    private String ownerUid;
    private String folderId;
    private FirebaseFirestore db;

    public static FolderSharingFragment newInstance(@Nullable String ownerUid,
                                                    @Nullable String folderId,
                                                    @Nullable String roomCode,
                                                    boolean isOwner,
                                                    @Nullable String folderName) {
        FolderSharingFragment f = new FolderSharingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_OWNER_UID, ownerUid);
        args.putString(ARG_FOLDER_ID, folderId);
        args.putString(ARG_ROOM_CODE, roomCode);
        args.putBoolean(ARG_IS_OWNER, isOwner);
        args.putString(ARG_FOLDER_NAME, folderName);
        f.setArguments(args);
        return f;
    }

    private static float dpToPx(Context ctx, float dp) {
        return dp * ctx.getResources().getDisplayMetrics().density;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        Bundle a = getArguments();
        if (a != null) {
            ownerUid = a.getString(ARG_OWNER_UID);
            folderId = a.getString(ARG_FOLDER_ID);
            roomCode = a.getString(ARG_ROOM_CODE);
            isOwner = a.getBoolean(ARG_IS_OWNER, false);
            folderName = a.getString(ARG_FOLDER_NAME);
        }
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.activity_folder_sharing,
                container,
                false
        );

        // bind views từ XML
        edtRoomCode = view.findViewById(R.id.edtRoomCode);
        btnJoin = view.findViewById(R.id.btnJoinRoom);
        TextView title = view.findViewById(R.id.txtSharingTitle);

        btnJoin.setOnClickListener(v -> {
            String code = edtRoomCode.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "Enter room code", Toast.LENGTH_SHORT).show();
                return;
            }
            performJoinByCode(code);
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog d = super.onCreateDialog(savedInstanceState);
        d.setCanceledOnTouchOutside(true);
        return d;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }


    private void performJoinByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a code", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUid == null) {
            Toast.makeText(requireContext(), "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("rooms")
                .document(code)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(requireContext(), "Room not found", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String ownerUid = doc.getString("ownerUid");
                    String folderId = doc.getString("folderId");
                    String folderName = doc.getString("folderName");

                    if (ownerUid == null || folderId == null) {
                        Toast.makeText(requireContext(), "Invalid room data", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Map<String, Object> joined = new HashMap<>();
                    joined.put("ownerUid", ownerUid);
                    joined.put("folderId", folderId);
                    joined.put("folderName", folderName != null ? folderName : "");
                    joined.put("joinedAt", Timestamp.now());

                    db.collection("users")
                            .document(currentUid)
                            .collection("joinedFolders")
                            .document(code)
                            .set(joined)
                            .addOnSuccessListener(aVoid -> {
                                Intent i = new Intent(requireContext(), ReadOnlyNotesActivity.class);
                                i.putExtra("ownerUid", ownerUid);
                                i.putExtra("folderId", folderId);
                                i.putExtra("folderName", folderName);
                                startActivity(i);
                                dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireContext(), "Join failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(err -> {
                    String msg = err.getMessage() == null ? "Join failed" : err.getMessage();
                    Toast.makeText(requireContext(), "Join failed: " + msg, Toast.LENGTH_LONG).show();
                });
    }
}