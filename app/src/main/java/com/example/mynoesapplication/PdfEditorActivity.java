package com.example.mynoesapplication;

import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.io.IOException;
import com.example.mynoesapplication.Fragment.ColorPickerFragment;

public class PdfEditorActivity extends AppCompatActivity {

    // ================= PDF =================
    private RecyclerView recyclerPdf;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;

    private String pdfPath;
    private String noteId;

    // ================= STATE =================
    private boolean isReadOnly = false;
    private DrawingView.Tool currentTool = DrawingView.Tool.PEN;

    private int penSize = 6;       // 2..12
    private int markerSize = 20;   // 10..40

    private PdfPageAdapter adapter;

    // ================= UI =================
    // ✅ FIX CRASH: XML là ConstraintLayout -> dùng View để không cast sai
    private View drawToolbar;

    private ImageButton btnPen, btnMarker, btnEraser, btnUndo, btnRedo, btnColor, btnReadOnly;

    // title
    private EditText edtTitle;
    private TextView txtTitle; // (không dùng trong layout hiện tại, giữ lại cho tương thích)

    // realtime title listener
    private ListenerRegistration noteListener;

    // ===== TITLE AUTOSAVE =====
    private final Handler titleHandler = new Handler(Looper.getMainLooper());
    private Runnable titleSaveRunnable;
    private boolean suppressTitleWatcher = false;
    private String lastSavedTitle = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pdf_editor);

        // ===== EDGE TO EDGE (an toàn) =====
        View root = findViewById(R.id.rootLayout);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, 0);
                return insets;
            });
        }

        // toolbar bottom inset
        View toolbarView = findViewById(R.id.drawToolbar);
        if (toolbarView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(toolbarView, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                int extra = dp(8);
                v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        bars.bottom + extra
                );
                return insets;
            });
        }

        // ================= UI =================
        ImageButton btnBack = findViewById(R.id.btnBack);
        recyclerPdf = findViewById(R.id.recyclerPdf);
        drawToolbar = findViewById(R.id.drawToolbar);

        btnPen = findViewById(R.id.btnPen);
        btnMarker = findViewById(R.id.btnMarker);
        btnEraser = findViewById(R.id.btnEraser);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnColor = findViewById(R.id.btnColor);
        btnReadOnly = findViewById(R.id.btnReadOnly);

        edtTitle = findViewById(R.id.edtTitle);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (recyclerPdf != null) recyclerPdf.setLayoutManager(new LinearLayoutManager(this));

        // ================= INTENT =================
        pdfPath = getIntent().getStringExtra("pdfPath");
        noteId = getIntent().getStringExtra("noteId");

        if (pdfPath == null || pdfPath.trim().isEmpty() || noteId == null || noteId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu dữ liệu PDF", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            Toast.makeText(this, "File PDF không tồn tại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        openPdf(pdfFile);

        // ================= LISTEN TITLE REALTIME =================
        listenNoteTitle();

        // ================= TITLE EDIT =================
        setupTitleEditing();

        // ================= READ ONLY =================
        if (btnReadOnly != null) btnReadOnly.setOnClickListener(v -> toggleReadOnly());

        // ================= TOOLS =================
        if (btnPen != null) btnPen.setOnClickListener(v -> {
            currentTool = DrawingView.Tool.PEN;
            if (adapter != null) adapter.setPen();
            updateToolUi();
        });

        if (btnMarker != null) btnMarker.setOnClickListener(v -> {
            currentTool = DrawingView.Tool.MARKER;
            if (adapter != null) adapter.setMarker();
            updateToolUi();
        });

        if (btnEraser != null) btnEraser.setOnClickListener(v -> {
            currentTool = DrawingView.Tool.ERASER;
            if (adapter != null) adapter.setEraser();
            updateToolUi();
        });

        if (btnUndo != null) btnUndo.setOnClickListener(v -> { if (adapter != null) adapter.undo(); });
        if (btnRedo != null) btnRedo.setOnClickListener(v -> { if (adapter != null) adapter.redo(); });

        if (btnColor != null) btnColor.setOnClickListener(v -> showColorAndSizePicker());

        if (drawToolbar != null) drawToolbar.setVisibility(View.VISIBLE);
        updateToolUi();
    }

    private void openPdf(File file) {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);

            PdfAnnotationStore store = new PdfAnnotationStore(this, noteId);
            adapter = new PdfPageAdapter(this, pdfRenderer, store);

            if (recyclerPdf != null) recyclerPdf.setAdapter(adapter);

            // apply state
            adapter.setReadOnly(isReadOnly);
            adapter.setPenStrokeWidth(penSize);
            adapter.setMarkerStrokeWidth(markerSize);

        } catch (Exception e) {
            Toast.makeText(this, "Mở PDF lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void toggleReadOnly() {
        isReadOnly = !isReadOnly;
        if (btnReadOnly != null) btnReadOnly.setSelected(isReadOnly);

        if (adapter != null) adapter.setReadOnly(isReadOnly);
        if (drawToolbar != null) drawToolbar.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);

        if (edtTitle != null) edtTitle.setEnabled(!isReadOnly);
    }

    // ================== TITLE ==================
    private void setupTitleEditing() {
        if (edtTitle == null) return;

        edtTitle.setEnabled(!isReadOnly);

        edtTitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressTitleWatcher || isReadOnly) return;

                String title = s.toString().trim();
                if (title.isEmpty() || title.equals(lastSavedTitle)) return;

                if (titleSaveRunnable != null)
                    titleHandler.removeCallbacks(titleSaveRunnable);

                titleSaveRunnable = () -> saveTitleToFirestore(title);
                titleHandler.postDelayed(titleSaveRunnable, 500); // debounce
            }
        });
    }


    private void showRenameTitleDialog() {
        final EditText input = new EditText(this);
        input.setHint("Nhập tiêu đề");
        input.setText(txtTitle != null ? txtTitle.getText().toString() : "");

        new AlertDialog.Builder(this)
                .setTitle("Đổi tiêu đề")
                .setView(input)
                .setPositiveButton("Lưu", (d, w) -> {
                    String t = input.getText().toString().trim();
                    if (!t.isEmpty()) saveTitleToFirestore(t);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveTitleFromEditText() {
        if (edtTitle == null) return;
        String title = edtTitle.getText().toString().trim();
        if (!title.isEmpty()) saveTitleToFirestore(title);
    }

    private void saveTitleToFirestore(String title) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || noteId == null) return;

        lastSavedTitle = title;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("notes")
                .document(noteId)
                .update(
                        "title", title,
                        "updatedAt", Timestamp.now()
                );
    }



    private void listenNoteTitle() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || noteId == null) return;

        noteListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("notes")
                .document(noteId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null || !snap.exists()) return;

                    String title = snap.getString("title");
                    if (title == null) title = "PDF";

                    lastSavedTitle = title;

                    if (edtTitle != null && !edtTitle.hasFocus()) {
                        suppressTitleWatcher = true;
                        edtTitle.setText(title);
                        edtTitle.setSelection(title.length());
                        suppressTitleWatcher = false;
                    }
                });
    }


    // ================== COLOR + SIZE ==================
    private void showColorAndSizePicker() {
        final int[] colors = {
                Color.BLACK, Color.RED, Color.BLUE, Color.GREEN,
                Color.YELLOW, Color.MAGENTA, Color.CYAN
        };

        final String[] names = {
                "Đen", "Đỏ", "Xanh dương", "Xanh lá",
                "Vàng", "Tím", "Cyan"
        };

        View v = getLayoutInflater().inflate(R.layout.dialog_color_size, null);
        TextView txtSizeLabel = v.findViewById(R.id.txtSizeLabel);
        SeekBar seekSize = v.findViewById(R.id.seekSize);

        int min, max, current;
        if (currentTool == DrawingView.Tool.MARKER) {
            min = 10; max = 40; current = markerSize;
            if (txtSizeLabel != null) txtSizeLabel.setText("Độ to Marker: " + markerSize);
        } else {
            min = 2; max = 36; current = penSize;
            if (txtSizeLabel != null) txtSizeLabel.setText("Độ to Pen: " + penSize);
        }

        if (seekSize != null) {
            seekSize.setMax(max - min);
            seekSize.setProgress(Math.max(0, current - min));

            seekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                    if (!fromUser) return;
                    int value = min + p;

                    if (currentTool == DrawingView.Tool.MARKER) {
                        markerSize = value;
                        if (txtSizeLabel != null) txtSizeLabel.setText("Độ to Marker: " + markerSize);
                        if (adapter != null) adapter.setMarkerStrokeWidth(markerSize);
                    } else {
                        penSize = value;
                        if (txtSizeLabel != null) txtSizeLabel.setText("Độ to Pen: " + penSize);
                        if (adapter != null) adapter.setPenStrokeWidth(penSize);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar sb) {}
                @Override public void onStopTrackingTouch(SeekBar sb) {}
            });
        }

        ColorPickerFragment f = ColorPickerFragment.newInstance(
                currentTool == DrawingView.Tool.MARKER,
                currentTool == DrawingView.Tool.MARKER ? markerSize : penSize
        );
        f.setOnColorSizeSelectedListener((color, size) -> {
            if (adapter != null) adapter.setColor(color);
            if (currentTool == DrawingView.Tool.MARKER) {
                markerSize = size;
                if (adapter != null) adapter.setMarkerStrokeWidth(markerSize);
            } else {
                penSize = size;
                if (adapter != null) adapter.setPenStrokeWidth(penSize);
            }
        });
        f.show(getSupportFragmentManager(), "color_picker");
    }

    // ================== TOOL UI ==================
    private void updateToolUi() {
        ImageButton[] buttons = {
                btnPen, btnMarker, btnEraser
        };

        // reset all
        for (ImageButton b : buttons) {
            if (b == null) continue;
            b.setBackgroundResource(R.drawable.bg_toolbar_item);
            b.setAlpha(0.6f);
        }

        // active
        ImageButton active = null;
        switch (currentTool) {
            case MARKER: active = btnMarker; break;
            case ERASER: active = btnEraser; break;
            default: active = btnPen; break;
        }

        if (active != null) {
            active.setBackgroundResource(R.drawable.bg_tool_button_active);
            active.setAlpha(1f);
        }
    }


    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    @Override
    protected void onDestroy() {
        if (noteListener != null) {
            noteListener.remove();
            noteListener = null;
        }

        try {
            if (pdfRenderer != null) pdfRenderer.close();
            if (fileDescriptor != null) fileDescriptor.close();
        } catch (IOException ignored) {}

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (edtTitle != null) {
            String title = edtTitle.getText().toString().trim();
            if (!title.isEmpty()) saveTitleToFirestore(title);
        }
    }

}
