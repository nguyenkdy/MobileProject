package com.example.mynoesapplication.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.Folder;
import com.example.mynoesapplication.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FolderPickerFragment extends BottomSheetDialogFragment {

    public interface OnFolderSelectedListener {
        void onFolderSelected(Folder folder);
    }

    private static final String ARG_UID = "arg_uid";
    private String uid;
    private OnFolderSelectedListener listener;
    private final List<Folder> folderList = new ArrayList<>();
    private RecyclerView recycler;
    private ProgressBar progress;
    private TextView txtEmpty;
    private FolderAdapter adapter;
    private FirebaseFirestore db;
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            getDialog().getWindow().setGravity(android.view.Gravity.BOTTOM);
        }
    }

    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                (com.google.android.material.bottomsheet.BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            com.google.android.material.bottomsheet.BottomSheetDialog d =
                    (com.google.android.material.bottomsheet.BottomSheetDialog) dialogInterface;

            // 1) Clear design_bottom_sheet background so inner rounded drawable is the visible background
            android.widget.FrameLayout sheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.setBackground(null);
                sheet.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            // 2) Make the window background transparent too (removes default dialog background)
            if (d.getWindow() != null) {
                d.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
        });

        return dialog;
    }

    public static FolderPickerFragment newInstance(String uid) {
        FolderPickerFragment f = new FolderPickerFragment();
        Bundle b = new Bundle();
        b.putString(ARG_UID, uid);
        f.setArguments(b);
        return f;
    }

    public void setOnFolderSelectedListener(OnFolderSelectedListener l) {
        this.listener = l;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) uid = getArguments().getString(ARG_UID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_folder_picker, container, false);
        recycler = v.findViewById(R.id.recyclerFolders);
        progress = v.findViewById(R.id.progressFolders);
        txtEmpty = v.findViewById(R.id.txtEmptyFolders);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FolderAdapter(folderList, f -> {
            if (listener != null) listener.onFolderSelected(f);
            dismiss();
        });
        recycler.setAdapter(adapter);

        loadFolders();
        return v;
    }

    private void loadFolders() {
        progress.setVisibility(View.VISIBLE);
        folderList.clear();
        adapter.notifyDataSetChanged();
        txtEmpty.setVisibility(View.GONE);

        if (uid == null || uid.trim().isEmpty()) {
            progress.setVisibility(View.GONE);
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }

        db.collection("users")
                .document(uid)
                .collection("folders")
                .whereEqualTo("deleted", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot d : snapshot.getDocuments()) {
                        Folder f = d.toObject(Folder.class);
                        if (f == null) f = new Folder();
                        f.id = d.getId();
                        f.ownerId = null; // owned
                        folderList.add(f);
                    }

                    db.collection("users")
                            .document(uid)
                            .collection("joinedFolders")
                            .get()
                            .addOnSuccessListener(joinedSnap -> {
                                for (DocumentSnapshot jd : joinedSnap.getDocuments()) {
                                    String roomCode = jd.getId();
                                    String ownerUid = jd.getString("ownerUid");
                                    String ownerFolderId = jd.getString("folderId");
                                    String folderName = jd.getString("folderName");
                                    if (ownerUid == null || ownerFolderId == null) continue;

                                    Folder shared = new Folder();
                                    shared.id = "shared_" + roomCode;
                                    String baseName = folderName != null ? folderName : "Thư mục chia sẻ";
                                    shared.name = baseName.endsWith(" (Chia sẻ)") ? baseName : baseName + " (Chia sẻ)";
                                    shared.ownerId = ownerUid;
                                    shared.roomCode = roomCode;
                                    shared.originalFolderId = ownerFolderId;
                                    shared.deleted = false;
                                    folderList.add(shared);
                                }
                                progress.setVisibility(View.GONE);
                                updateEmpty();
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                progress.setVisibility(View.GONE);
                                updateEmpty();
                                adapter.notifyDataSetChanged();
                            });
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    updateEmpty();
                });
    }

    private void updateEmpty() {
        txtEmpty.setVisibility(folderList.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(folderList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // Adapter + ViewHolder
    private static class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.Holder> {
        private final List<Folder> items;
        private final java.util.function.Consumer<Folder> onClick;

        FolderAdapter(List<Folder> items, java.util.function.Consumer<Folder> onClick) {
            this.items = items;
            this.onClick = onClick;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Folder f = items.get(position);
            holder.name.setText(f.name != null ? f.name : "Thư mục");
            holder.itemView.setOnClickListener(v -> onClick.accept(f));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class Holder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView icon;
            Holder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.txtFolderName);
                icon = itemView.findViewById(R.id.imgFolderIcon);
            }
        }
    }
}
