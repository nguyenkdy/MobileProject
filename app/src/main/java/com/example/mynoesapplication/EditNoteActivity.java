package com.example.mynoesapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class EditNoteActivity extends AppCompatActivity {

    // ================= UI =================
    private EditText edtTitle, edtContent;
    private ImageButton btnBack;

    private ImageButton btnKeyboard, btnPen, btnMarker, btnEraser, btnLaser;
    private ImageButton btnUndo, btnRedo;

    private DrawingView drawingView;

    // ================= Firebase =================
    private FirebaseFirestore db;
    private String uid, noteId;

    // ⭐ Lưu folderId để cập nhật thư mục ngay (không cần query lại)
    private String folderId = null;

    // ================= Tool =================
    enum Tool { KEYBOARD, PEN, MARKER, ERASER, LASER }
    private Tool currentTool = Tool.KEYBOARD;


    // ================= Undo / Redo (TEXT) =================
    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private boolean internalChange = false;

    // ================= Debounce =================
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSaveText;
    private Runnable pendingSaveDrawing;

    // ================= Safety =================
    private boolean isFinishingOrDestroyed = false;
    private boolean exitRequested = false;

    private ImageButton btnColor;
    private int selectedColor = Color.BLACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        // Predictive back
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                requestExitWithSave();
            }
        });

        // Insets
        View root = findViewById(R.id.rootLayout);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        // Views
        btnBack = findViewById(R.id.btnBack);
        edtTitle = findViewById(R.id.edtTitle);
        edtContent = findViewById(R.id.edtContent);

        btnKeyboard = findViewById(R.id.btnKeyboard);
        btnPen = findViewById(R.id.btnPen);
        btnMarker = findViewById(R.id.btnMarker);
        btnEraser = findViewById(R.id.btnEraser);
        btnLaser = findViewById(R.id.btnLaser);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnColor = findViewById(R.id.btnPalette);

        drawingView = findViewById(R.id.drawingView);
        if (drawingView != null) drawingView.setVisibility(View.GONE);

        // Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        noteId = getIntent().getStringExtra("noteId");
        if (noteId == null || noteId.trim().isEmpty()) { finish(); return; }

        // Load note
        loadNoteFromFirestore();

        // ================= TEXT WATCHERS =================
        edtTitle.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(android.text.Editable s) {
                if (!internalChange) debounceSaveText();
            }
        });

        edtContent.addTextChangedListener(new android.text.TextWatcher() {
            String before = "";

            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
                if (!internalChange) before = s.toString();
            }

            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (internalChange) return;

                if (undoStack.isEmpty() || !undoStack.peek().equals(before)) {
                    undoStack.push(before);
                }
                redoStack.clear();

                debounceSaveText();
            }
        });

        // ================= DRAWING CALLBACK =================
        if (drawingView != null) {
            drawingView.setOnDrawingChangeListener(this::debounceSaveDrawing);
        }

        // ================= TOOL BUTTONS =================
        btnKeyboard.setOnClickListener(v -> selectTool(Tool.KEYBOARD));
        btnPen.setOnClickListener(v -> selectTool(Tool.PEN));
        btnMarker.setOnClickListener(v -> selectTool(Tool.MARKER));
        btnEraser.setOnClickListener(v -> selectTool(Tool.ERASER));
        btnLaser.setOnClickListener(v -> selectTool(Tool.LASER));

        btnUndo.setOnClickListener(v -> {
            if (currentTool == Tool.KEYBOARD) undoText();
            else if (drawingView != null) drawingView.undo();
        });

        btnRedo.setOnClickListener(v -> {
            if (currentTool == Tool.KEYBOARD) redoText();
            else if (drawingView != null) drawingView.redo();
        });

        btnBack.setOnClickListener(v -> requestExitWithSave());

        selectTool(Tool.KEYBOARD);

        btnColor.setOnClickListener(v -> showColorPicker());
    }

    @Override
    protected void onDestroy() {
        isFinishingOrDestroyed = true;
        super.onDestroy();
        uiHandler.removeCallbacksAndMessages(null);
    }

    // ==================================================
    // LOAD
    // ==================================================
    private void loadNoteFromFirestore() {
        db.collection("users")
                .document(uid)
                .collection("notes")
                .document(noteId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isFinishingOrDestroyed) return;
                    if (doc == null || !doc.exists()) return;

                    // ⭐ Lấy folderId để sync folder ngay
                    folderId = doc.getString("folderId");

                    internalChange = true;
                    edtTitle.setText(doc.getString("title"));
                    edtContent.setText(doc.getString("content"));
                    internalChange = false;

                    String drawing = doc.getString("drawing");
                    if (drawingView != null && drawing != null && !drawing.isEmpty()) {
                        drawingView.importFromJson(drawing);
                        drawingView.setVisibility(View.VISIBLE);
                    }

                    undoStack.clear();
                    redoStack.clear();
                    undoStack.push(edtContent.getText().toString());
                });
    }

    // ==================================================
    // SAVE (DEBOUNCE)
    // ==================================================
    private void debounceSaveText() {
        if (isFinishingOrDestroyed) return;

        if (pendingSaveText != null) uiHandler.removeCallbacks(pendingSaveText);
        pendingSaveText = () -> saveTextAsync();
        uiHandler.postDelayed(pendingSaveText, 400);
    }

    private void debounceSaveDrawing() {
        if (isFinishingOrDestroyed) return;

        if (pendingSaveDrawing != null) uiHandler.removeCallbacks(pendingSaveDrawing);
        pendingSaveDrawing = () -> saveDrawingAsync();
        uiHandler.postDelayed(pendingSaveDrawing, 500);
    }

    private Task<Void> saveTextAsync() {
        if (isFinishingOrDestroyed) return Tasks.forResult(null);

        Map<String, Object> data = new HashMap<>();
        data.put("title", safe(edtTitle.getText()));
        data.put("content", safe(edtContent.getText()));
        data.put("updatedAt", Timestamp.now()); // ⭐ để list/folder sync

        Task<Void> t = db.collection("users")
                .document(uid)
                .collection("notes")
                .document(noteId)
                .update(data);

        // ⭐ “đụng” updatedAt folder để list folder update (không bắt buộc mỗi lần, nhưng giúp realtime tốt)
        t.addOnSuccessListener(v -> touchFolderUpdatedAt());

        return t;
    }

    private Task<Void> saveDrawingAsync() {
        if (isFinishingOrDestroyed) return Tasks.forResult(null);

        Map<String, Object> data = new HashMap<>();
        if (drawingView == null || !drawingView.hasDrawing()) {
            data.put("drawing", null);
        } else {
            data.put("drawing", drawingView.exportToJson());
        }

        // ⭐ rất quan trọng: drawing đổi cũng phải updatedAt
        data.put("updatedAt", Timestamp.now());

        Task<Void> t = db.collection("users")
                .document(uid)
                .collection("notes")
                .document(noteId)
                .update(data);

        t.addOnSuccessListener(v -> touchFolderUpdatedAt());

        return t;
    }

    // ==================================================
    // EXIT (save rồi finish)
    // ==================================================
    private void requestExitWithSave() {
        if (exitRequested) return;
        exitRequested = true;

        // Flush debounce
        uiHandler.removeCallbacksAndMessages(null);

        Tasks.whenAll(
                saveTextAsync(),
                saveDrawingAsync()
        ).addOnCompleteListener(t -> {
            // ⭐ thumbnail dựa trên nét vẽ
            generateAndSaveThumbnail();

            // ⭐ đảm bảo folder cập nhật (kể cả khi debounce chưa kịp)
            touchFolderUpdatedAt();

            finish();
        });
    }

    // ==================================================
    // TOOL SELECT
    // ==================================================
    private void selectTool(Tool tool) {
        currentTool = tool;

        ImageButton[] all = { btnKeyboard, btnPen, btnMarker, btnEraser, btnLaser };
        for (ImageButton b : all) if (b != null) b.setAlpha(0.4f);

        ImageButton active = null;

        switch (tool) {
            case KEYBOARD:
                active = btnKeyboard;
                edtContent.setEnabled(true);

                if (drawingView != null && !drawingView.hasDrawing()) {
                    drawingView.setVisibility(View.GONE);
                }

                showKeyboard();
                break;

            case PEN:
                active = btnPen;
                edtContent.setEnabled(false);
                if (drawingView != null) {
                    drawingView.setVisibility(View.VISIBLE);
                    drawingView.setPen();
                }
                hideKeyboard();
                break;

            case MARKER:
                active = btnMarker;
                edtContent.setEnabled(false);
                if (drawingView != null) {
                    drawingView.setVisibility(View.VISIBLE);
                    drawingView.setMarker();
                }
                hideKeyboard();
                break;

            case ERASER:
                active = btnEraser;
                edtContent.setEnabled(false);
                if (drawingView != null) {
                    drawingView.setVisibility(View.VISIBLE);
                    drawingView.setEraser();
                }
                hideKeyboard();
                break;

            case LASER:
                active = btnLaser;
                edtContent.setEnabled(false);
                if (drawingView != null) {
                    drawingView.setVisibility(View.VISIBLE);
                    drawingView.setLaser();
                }
                hideKeyboard();
                break;
        }

        if (active != null) active.setAlpha(1f);
    }

    // ==================================================
    // TEXT UNDO / REDO
    // ==================================================
    private void undoText() {
        if (undoStack.isEmpty()) return;

        internalChange = true;

        String current = safe(edtContent.getText());
        String prev = undoStack.pop();

        redoStack.push(current);

        edtContent.setText(prev);
        edtContent.setSelection(prev.length());

        internalChange = false;
        debounceSaveText();
    }

    private void redoText() {
        if (redoStack.isEmpty()) return;

        internalChange = true;

        String current = safe(edtContent.getText());
        String next = redoStack.pop();

        undoStack.push(current);

        edtContent.setText(next);
        edtContent.setSelection(next.length());

        internalChange = false;
        debounceSaveText();
    }

    // ==================================================
    // KEYBOARD
    // ==================================================
    private void showKeyboard() {
        edtContent.requestFocus();
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(edtContent, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(edtContent.getWindowToken(), 0);
    }

    // ==================================================
    // THUMBNAIL (drawing-only)
    // ==================================================
    private void generateAndSaveThumbnail() {
        if (noteId == null) return;

        // ❌ KHÔNG có nét vẽ → xóa thumbnail cũ nếu có
        if (drawingView == null || !drawingView.hasDrawing()) {
            ThumbnailCache.delete(this, noteId);
            return;
        }

        Bitmap drawingPreview = drawingView.exportPreviewBitmap(300, 180);
        Bitmap preview = NoteThumbnailRenderer.renderDrawingPreview(drawingPreview);

        ThumbnailCache.save(this, noteId, preview);
    }

    // ==================================================
    // FOLDER SYNC (touch updatedAt)
    // ==================================================
    private void touchFolderUpdatedAt() {
        if (uid == null || folderId == null || folderId.trim().isEmpty()) return;

        db.collection("users")
                .document(uid)
                .collection("folders")
                .document(folderId)
                .update("updatedAt", Timestamp.now());
    }

    // ==================================================
    // UTIL
    // ==================================================
    private static String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }

    private void showColorPicker() {

        // chỉ cho pen / marker
        if (currentTool != Tool.PEN && currentTool != Tool.MARKER) {
            Toast.makeText(this, "Chỉ áp dụng cho bút & marker", Toast.LENGTH_SHORT).show();
            return;
        }

        final int[] colors = {
                Color.BLACK,
                Color.BLUE,
                Color.RED,
                Color.GREEN,
                Color.MAGENTA,
                Color.CYAN,
                Color.YELLOW,
                Color.DKGRAY
        };

        String[] names = {
                "Đen", "Xanh dương", "Đỏ", "Xanh lá",
                "Tím", "Cyan", "Vàng", "Xám đậm"
        };

        new AlertDialog.Builder(this)
                .setTitle("Chọn màu")
                .setItems(names, (d, which) -> {
                    selectedColor = colors[which];
                    drawingView.setColor(selectedColor);
                })
                .show();
    }


    private abstract static class SimpleTextWatcher implements android.text.TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(@NonNull android.text.Editable s) {}
    }
}
