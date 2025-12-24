package com.example.mynoesapplication.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mynoesapplication.Data.ScreenMode;
import com.example.mynoesapplication.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FolderCreateFragment extends DialogFragment {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String uid;
    private ScreenMode currentMode;

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Calculate width = screen width - (32dp * 2)
            float density = getResources().getDisplayMetrics().density;
            int horizMarginDp = 32;
            int horizMarginPx = (int) (horizMarginDp * density + 0.5f);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int desiredWidth = screenWidth - horizMarginPx * 2;

            int width = desiredWidth;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            getDialog().getWindow().setLayout(width, height);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    // No-arg constructor required by the framework
    public FolderCreateFragment() {}

    // Convenience constructor (existing usage in project)
    public FolderCreateFragment(String uid, ScreenMode mode) {
        this.uid = uid;
        this.currentMode = mode;
    }

    // Optional factory if you want to switch to safe fragment creation
    public static FolderCreateFragment newInstance(String uid, ScreenMode mode) {
        FolderCreateFragment f = new FolderCreateFragment();
        Bundle args = new Bundle();
        args.putString("uid", uid);
        args.putString("mode", mode == null ? null : mode.name());
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // retrieve values from arguments if provided and fields are not set
        if ((uid == null || currentMode == null) && getArguments() != null) {
            if (uid == null) uid = getArguments().getString("uid");
            if (currentMode == null) {
                String m = getArguments().getString("mode");
                if (m != null) {
                    try {
                        currentMode = ScreenMode.valueOf(m);
                    } catch (Exception ignored) {}
                }
            }
        }

        View view = inflater.inflate(R.layout.folder_create_frag, container, false);

        EditText input = view.findViewById(R.id.folderNameInput);
        Button createBtn = view.findViewById(R.id.createBtn);
        Button cancelBtn = view.findViewById(R.id.cancelBtn);

        createBtn.setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError("Tên thư mục không được để trống");
                return;
            }

            Map<String, Object> folder = new HashMap<>();
            folder.put("name", name);
            folder.put("createdAt", Timestamp.now());
            folder.put("deleted", false);
            folder.put("deletedAt", null);

            if (uid == null || uid.trim().isEmpty()) {
                Toast.makeText(getContext(), "UID not available", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").document(uid).collection("folders")
                    .add(folder)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(getContext(), "Tạo thư mục thành công", Toast.LENGTH_SHORT).show();
                        if (currentMode == ScreenMode.FOLDERS) {
                            if (getActivity() instanceof FolderCallback) {
                                ((FolderCallback) getActivity()).loadFolders();
                            }
                        }
                        dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Tạo thư mục lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        cancelBtn.setOnClickListener(v -> dismiss());

        return view;
    }

    public interface FolderCallback {
        void loadFolders();
    }
}
