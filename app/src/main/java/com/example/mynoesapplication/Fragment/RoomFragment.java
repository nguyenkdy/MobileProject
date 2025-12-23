package com.example.mynoesapplication.Fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mynoesapplication.R;

public class RoomFragment extends DialogFragment {
    private static final String ARG_OWNER_UID = "ownerUid";
    private static final String ARG_FOLDER_ID = "folderId";
    private static final String ARG_ROOM_CODE = "roomCode";
    private static final String ARG_IS_OWNER = "isOwner";
    private static final String ARG_FOLDER_NAME = "folderName";

    private String ownerUid;
    private String folderId;
    private String roomCode;
    private boolean isOwner;
    private String folderName;

    // UI
    private View ownerContainer, joinContainer;
    private TextView txtRoomCode, txtOwnerFolderName;
    private EditText edtRoomCode;
    private Button btnCopyCode, btnJoin;

    public static RoomFragment newInstance(
            String ownerUid,
            String folderId,
            String roomCode,
            boolean isOwner,
            String folderName
    ) {
        RoomFragment f = new RoomFragment();
        Bundle args = new Bundle();
        args.putString(ARG_OWNER_UID, ownerUid);
        args.putString(ARG_FOLDER_ID, folderId);
        args.putString(ARG_ROOM_CODE, roomCode);
        args.putBoolean(ARG_IS_OWNER, isOwner);
        args.putString(ARG_FOLDER_NAME, folderName);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle a = getArguments();
        if (a != null) {
            ownerUid = a.getString(ARG_OWNER_UID);
            folderId = a.getString(ARG_FOLDER_ID);
            roomCode = a.getString(ARG_ROOM_CODE);
            isOwner = a.getBoolean(ARG_IS_OWNER, false);
            folderName = a.getString(ARG_FOLDER_NAME);
        }
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.room_detail, container, false);

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
        if (isOwner) showOwnerView(); else showJoinView();
    }

    private void showOwnerView() {
        if (ownerContainer != null) ownerContainer.setVisibility(View.VISIBLE);
        if (joinContainer != null) joinContainer.setVisibility(View.GONE);
        if (txtRoomCode != null) txtRoomCode.setText(roomCode != null ? roomCode : "");
        if (txtOwnerFolderName != null) txtOwnerFolderName.setText(folderName != null ? folderName : "");
        if (btnCopyCode != null) {
            btnCopyCode.setOnClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && roomCode != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("roomCode", roomCode));
                    toast("Đã copy room code");
                }
            });
        }
    }

    private void showJoinView() {
        if (ownerContainer != null) ownerContainer.setVisibility(View.GONE);
        if (joinContainer != null) joinContainer.setVisibility(View.VISIBLE);
        if (btnJoin != null && edtRoomCode != null) {
            btnJoin.setOnClickListener(v -> {
                String code = edtRoomCode.getText().toString().trim();
                if (code.isEmpty()) {
                    toast("Nhập room code");
                    return;
                }
                joinByCode(code);
            });
        }
    }

    // placeholder: implement actual join logic
    private void joinByCode(String code) {
        toast("Joining: " + code);
        dismiss();
    }

    private void toast(String t) {
        if (getContext() != null) android.widget.Toast.makeText(getContext(), t, android.widget.Toast.LENGTH_SHORT).show();
    }
}
