package com.example.mynoesapplication;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

    private ImageButton btnKeyboard, btnPen, btnMarker, btnEraser;
    private ImageButton btnUndo, btnRedo, btnLaser;

    private DrawingView drawingView;

    // ================= Firebase =================
    private FirebaseFirestore db;
    private String uid, noteId;

    // ================= Tool =================
    enum Tool { KEYBOARD, PEN, MARKER, ERASER, LASER }
    private Tool currentTool = Tool.KEYBOARD;

    // ================= Undo / Redo (TEXT) =================
    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private boolean internalChange = false;

    // ================= Debounce save =================
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSaveText = null;

    // ================= Safety flags =================
    private boolean isFinishingOrDestroyed = false;

    // ================= S3 toggle (DISABLED for now) =================
    private static final boolean ENABLE_S3 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        // Register modern back callback to replace deprecated onBackPressed
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveAllAndExit();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // ================= Insets =================
        View root = findViewById(R.id.rootLayout);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        // ================= View =================
        btnBack = findViewById(R.id.btnBack);
        edtTitle = findViewById(R.id.edtTitle);
        edtContent = findViewById(R.id.edtContent);

        btnKeyboard = findViewById(R.id.btnKeyboard);
        btnPen = findViewById(R.id.btnPen);
        btnMarker = findViewById(R.id.btnMarker);
        btnEraser = findViewById(R.id.btnEraser);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnLaser = findViewById(R.id.btnLaser);

        drawingView = findViewById(R.id.drawingView);
        if (drawingView != null) drawingView.setVisibility(View.GONE);

        // ================= Firebase =================
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        noteId = getIntent().getStringExtra("noteId");
        if (noteId == null || noteId.trim().isEmpty()) {
            finish();
            return;
        }

        // ================= Load note (TEXT only for now) =================
        loadNoteFromFirestore();

        // ================= TextWatcher =================
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

                // Push "before" state into undo stack (avoid duplicates)
                if (undoStack.isEmpty() || !undoStack.peek().equals(before)) {
                    undoStack.push(before);
                }
                // Any new typing clears redo
                redoStack.clear();

                debounceSaveText();
            }
        });

        // ================= Toolbar =================
        btnKeyboard.setOnClickListener(v -> selectTool(Tool.KEYBOARD));
        btnPen.setOnClickListener(v -> selectTool(Tool.PEN));
        btnMarker.setOnClickListener(v -> selectTool(Tool.MARKER));
        btnEraser.setOnClickListener(v -> selectTool(Tool.ERASER));
        btnLaser.setOnClickListener(v -> selectTool(Tool.LASER));

        btnUndo.setOnClickListener(v -> undo());
        btnRedo.setOnClickListener(v -> redo());

        // ================= BACK =================
        btnBack.setOnClickListener(v -> saveAllAndExit());

        // Default tool
        selectTool(Tool.KEYBOARD);
    }

    @Override
    protected void onDestroy() {
        isFinishingOrDestroyed = true;
        super.onDestroy();
        if (pendingSaveText != null) uiHandler.removeCallbacks(pendingSaveText);
    }

    // ==================================================
    // LOAD NOTE (TEXT; S3 disabled)
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

                    String title = doc.getString("title");
                    String content = doc.getString("content");

                    internalChange = true;
                    if (title != null) edtTitle.setText(title);
                    if (content != null) edtContent.setText(content);
                    internalChange = false;

                    // reset undo/redo baseline
                    undoStack.clear();
                    redoStack.clear();
                    undoStack.push(edtContent.getText().toString());

                    // S3 disabled: intentionally not loading drawingUrl at this time
                });
    }

    // ==================================================
    // SAVE TEXT NOTE (debounced)
    // ==================================================
    private void debounceSaveText() {
        if (pendingSaveText != null) uiHandler.removeCallbacks(pendingSaveText);
        pendingSaveText = this::saveNoteText;
        uiHandler.postDelayed(pendingSaveText, 500);
    }

    private void saveNoteText() {
        if (isFinishingOrDestroyed) return;

        Map<String, Object> data = new HashMap<>();
        data.put("title", edtTitle.getText().toString());
        data.put("content", edtContent.getText().toString());
        data.put("updatedAt", Timestamp.now());

        db.collection("users")
                .document(uid)
                .collection("notes")
                .document(noteId)
                .update(data);
    }

    // ==================================================
    // SAVE ALL THEN EXIT
    // ==================================================
    private void saveAllAndExit() {
        // flush pending debounce
        if (pendingSaveText != null) {
            uiHandler.removeCallbacks(pendingSaveText);
            pendingSaveText = null;
        }
        saveNoteText();

        // S3 disabled => just exit
        finish();
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

                // Show drawing only if user has drawn something
                if (drawingView != null) {
                    if (drawingView.hasDrawing()) drawingView.setVisibility(View.VISIBLE);
                    else drawingView.setVisibility(View.GONE);
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
    // UNDO / REDO (TEXT) - stable version
    // ==================================================
    private void undo() {
        if (undoStack.isEmpty()) return;

        internalChange = true;

        String current = edtContent.getText().toString();
        String prev = undoStack.pop();

        // push current to redo
        redoStack.push(current);

        edtContent.setText(prev);
        edtContent.setSelection(prev.length());

        internalChange = false;
        debounceSaveText();
    }

    private void redo() {
        if (redoStack.isEmpty()) return;

        internalChange = true;

        String current = edtContent.getText().toString();
        String next = redoStack.pop();

        // push current back to undo
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
}
