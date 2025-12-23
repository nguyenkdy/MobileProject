package com.example.mynoesapplication.Fragment;

import android.app.Dialog;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mynoesapplication.R;
import com.example.mynoesapplication.ReadOnlyNotesActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RoomFragment extends DialogFragment {

    // ===== args =====
    private static final String ARG_OWNER_UID = "ownerUid";
    private static final String ARG_FOLDER_ID = "folderId";
    private static final String ARG_ROOM_CODE = "roomCode";
    private static final String ARG_IS_OWNER = "isOwner";
    private static final String ARG_FOLDER_NAME = "folderName";

    // ===== UI =====
    private View ownerContainer, joinContainer;
    private TextView txtRoomCode, txtOwnerFolderName;
    private EditText edtRoomCode;
    private Button btnCopyCode, btnJoin;

    // ===== data =====
    private String ownerUid, folderId, roomCode, folderName;
    private boolean isOwner;

    private FirebaseFirestore db;

    // ===== factory =====
    public static RoomFragment newInstance(
            String ownerUid,
            String folderId,
            String roomCode,
            boolean isOwner,
            String folderName
    ) {
        RoomFragment f = new RoomFragment();
        Bundle b = new Bundle();
        b.putString(ARG_OWNER_UID, ownerUid);
        b.putString(ARG_FOLDER_ID, folderId);
        b.putString(ARG_ROOM_CODE, roomCode);
        b.putBoolean(ARG_IS_OWNER, isOwner);
        b.putString(ARG_FOLDER_NAME, folderName);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            ownerUid = getArguments().getString(ARG_OWNER_UID);
            folderId = getArguments().getString(ARG_FOLDER_ID);
            roomCode = getArguments().getString(ARG_ROOM_CODE);
            folderName = getArguments().getString(ARG_FOLDER_NAME);
            isOwner = getArguments().getBoolean(ARG_IS_OWNER, false);
        }

        setStyle(STYLE_NO_TITLE, 0);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.room_detail, container, false);

        // ===== bind views =====
        ownerContainer = view.findViewById(R.id.owner_container);
        joinContainer = view.findViewById(R.id.join_container);
        txtRoomCode = view.findViewById(R.id.txtRoomCode);
        txtOwnerFolderName = view.findViewById(R.id.txtOwnerFolderName);
        edtRoomCode = view.findViewById(R.id.edtRoomCode);
        btnCopyCode = view.findViewById(R.id.btnCopyCode);
        btnJoin = view.findViewById(R.id.btnJoin);

        setupUI();

        return view;
    }

    private void setupUI() {
        if (isOwner || (roomCode != null && !roomCode.isEmpty())) {
            showOwnerView();
        } else {
            showJoinView();
        }
    }

    private void showOwnerView() {
        ownerContainer.setVisibility(View.VISIBLE);
        joinContainer.setVisibility(View.GONE);

        txtRoomCode.setText(roomCode);
        txtOwnerFolderName.setText(folderName);

        btnCopyCode.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager)
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(
                        ClipData.newPlainText("roomCode", roomCode)
                );
                toast("Đã copy room code");
            }
        });
    }

    private void showJoinView() {
        ownerContainer.setVisibility(View.GONE);
        joinContainer.setVisibility(View.VISIBLE);

        btnJoin.setOnClickListener(v -> {
            String code = edtRoomCode.getText().toString().trim();
            if (code.isEmpty()) {
                toast("Nhập room code");
                return;
            }
            joinByCode(code);
        });
    }

    private void joinByCode(String code) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            toast("Chưa đăng nhập");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("rooms").document(code).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        toast("Room không tồn tại");
                        return;
                    }

                    Map<String, Object> joined = new HashMap<>();
                    joined.put("ownerUid", doc.getString("ownerUid"));
                    joined.put("folderId", doc.getString("folderId"));
                    joined.put("folderName", doc.getString("folderName"));
                    joined.put("joinedAt", Timestamp.now());

                    db.collection("users")
                            .document(uid)
                            .collection("joinedFolders")
                            .document(code)
                            .set(joined)
                            .addOnSuccessListener(v -> {
                                Intent i = new Intent(
                                        requireContext(),
                                        ReadOnlyNotesActivity.class
                                );
                                i.putExtra("ownerUid", doc.getString("ownerUid"));
                                i.putExtra("folderId", doc.getString("folderId"));
                                i.putExtra("folderName", doc.getString("folderName"));
                                startActivity(i);
                                dismiss();
                            });
                });
    }

    private void toast(String s) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }

    // ===== dialog behavior =====
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog d = super.onCreateDialog(savedInstanceState);
        d.setCanceledOnTouchOutside(true);
        d.setCancelable(true);
        return d;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window w = dialog.getWindow();
            w.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            w.setGravity(Gravity.CENTER);
            w.setBackgroundDrawableResource(android.R.color.transparent);

            WindowManager.LayoutParams p = w.getAttributes();
            p.dimAmount = 0.4f;
            w.setAttributes(p);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }
}
