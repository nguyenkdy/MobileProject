package com.example.mynoesapplication;

import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;

public class PdfEditorActivity extends AppCompatActivity {

    private RecyclerView recyclerPdf;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;

    private String pdfPath;
    private String noteId;

    private boolean isReadOnly = false;

    // Tool state (để biết slider đang chỉnh cái gì)
    private DrawingView.Tool currentTool = DrawingView.Tool.PEN;

    // Lưu size hiện tại để show trên dialog (UX)
    private int penSize = 6;       // 2..12
    private int markerSize = 20;   // 10..40

    // 🔥 adapter field
    private PdfPageAdapter adapter;

    // UI refs
    private LinearLayout drawToolbar;
    private ImageButton btnPen, btnMarker, btnEraser, btnUndo, btnRedo, btnColor, btnReadOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_editor);

        // ===== BACK =====
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // ===== RECYCLER =====
        recyclerPdf = findViewById(R.id.recyclerPdf);
        recyclerPdf.setLayoutManager(new LinearLayoutManager(this));

        // ===== TOOLBAR =====
        drawToolbar = findViewById(R.id.drawToolbar);

        btnPen = findViewById(R.id.btnPen);
        btnMarker = findViewById(R.id.btnMarker);
        btnEraser = findViewById(R.id.btnEraser);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnColor = findViewById(R.id.btnColor);
        btnReadOnly = findViewById(R.id.btnReadOnly);

        // ===== INTENT DATA =====
        pdfPath = getIntent().getStringExtra("pdfPath");
        noteId = getIntent().getStringExtra("noteId");

        if (pdfPath == null || pdfPath.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy PDF", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (noteId == null || noteId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu noteId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            Toast.makeText(this, "File PDF không tồn tại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ===== OPEN PDF =====
        openPdf(pdfFile);

        // ===== READ ONLY TOGGLE =====
        if (btnReadOnly != null) {
            btnReadOnly.setOnClickListener(v -> toggleReadOnly());
        }

        // ===== TOOL ACTIONS =====
        if (btnPen != null) {
            btnPen.setOnClickListener(v -> {
                currentTool = DrawingView.Tool.PEN;
                if (adapter != null) adapter.setPen();
                updateToolUi();
            });
        }

        if (btnMarker != null) {
            btnMarker.setOnClickListener(v -> {
                currentTool = DrawingView.Tool.MARKER;
                if (adapter != null) adapter.setMarker();
                updateToolUi();
            });
        }

        if (btnEraser != null) {
            btnEraser.setOnClickListener(v -> {
                currentTool = DrawingView.Tool.ERASER;
                if (adapter != null) adapter.setEraser();
                updateToolUi();
            });
        }

        if (btnUndo != null) {
            btnUndo.setOnClickListener(v -> {
                if (adapter != null) adapter.undo();
            });
        }

        if (btnRedo != null) {
            btnRedo.setOnClickListener(v -> {
                if (adapter != null) adapter.redo();
            });
        }

        if (btnColor != null) {
            btnColor.setOnClickListener(v -> showColorAndSizePicker());
        }

        // Mặc định: edit mode (toolbar hiện)
        drawToolbar.setVisibility(View.VISIBLE);
        updateToolUi();
    }

    private void openPdf(File file) {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);

            PdfAnnotationStore store = new PdfAnnotationStore(this, noteId);
            adapter = new PdfPageAdapter(this, pdfRenderer, store);

            recyclerPdf.setAdapter(adapter);

            // Apply trạng thái hiện tại cho adapter ngay khi mở
            adapter.setReadOnly(isReadOnly);

            // Apply size mặc định cho tool (tránh trường hợp user mở dialog trước khi chạm page)
            // Lưu ý: adapter chỉ apply vào activeDrawingView, nên user cần chạm vào page trước để active.
            // Nhưng set này không gây lỗi.
            adapter.setPenStrokeWidth(penSize);
            adapter.setMarkerStrokeWidth(markerSize);

        } catch (IOException e) {
            Toast.makeText(this, "Mở PDF lỗi", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void toggleReadOnly() {
        isReadOnly = !isReadOnly;

        if (btnReadOnly != null) btnReadOnly.setSelected(isReadOnly);

        if (adapter != null) adapter.setReadOnly(isReadOnly);

        if (drawToolbar != null) {
            drawToolbar.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);
        }
    }

    // ===== Dialog: Color + Size (Pen/Marker riêng) =====
    private void showColorAndSizePicker() {

        // Nếu đang eraser thì vẫn cho chọn màu/size, nhưng size chỉ áp dụng cho pen/marker.
        // UX: nếu eraser đang active, mình vẫn cho chỉnh pen/marker theo last tool.
        // Bạn muốn khóa khi eraser thì mình sửa dễ.

        final int[] colors = {
                Color.BLACK,
                Color.RED,
                Color.BLUE,
                Color.GREEN,
                Color.YELLOW,
                Color.MAGENTA,
                Color.CYAN
        };

        final String[] names = {
                "Đen", "Đỏ", "Xanh dương", "Xanh lá",
                "Vàng", "Tím", "Cyan"
        };

        View v = getLayoutInflater().inflate(R.layout.dialog_color_size, null);

        TextView txtSizeLabel = v.findViewById(R.id.txtSizeLabel);
        SeekBar seekSize = v.findViewById(R.id.seekSize);

        // Set range + progress theo tool hiện tại
        int min, max, current;
        if (currentTool == DrawingView.Tool.MARKER) {
            min = 10; max = 40; current = markerSize;
            if (txtSizeLabel != null) txtSizeLabel.setText("Độ to Marker: " + markerSize);
        } else {
            // default PEN cho cả trường hợp ERASER (để user chỉnh pen chuẩn bị)
            min = 2; max = 12; current = penSize;
            if (txtSizeLabel != null) txtSizeLabel.setText("Độ to Pen: " + penSize);
        }

        // SeekBar không hỗ trợ min chuẩn trên mọi API, ta dùng max-min & offset
        if (seekSize != null) {
            seekSize.setMax(max - min);
            seekSize.setProgress(Math.max(0, Math.min(current, max)) - min);

            seekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    if (!fromUser) return;

                    int value = min + progress;

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

        new AlertDialog.Builder(this)
                .setTitle("Màu & độ to")
                .setItems(names, (d, which) -> {
                    if (adapter != null) {
                        adapter.setColor(colors[which]);
                    }
                    // Sau khi chọn màu, dialog items sẽ tự đóng.
                    // Nếu bạn muốn dialog giữ lại để chỉnh size thì phải làm custom list,
                    // hiện tại theo kiểu đơn giản nhất để bạn chạy ổn định trước.
                })
                .setView(v)
                .setNegativeButton("Đóng", null)
                .show();
    }

    // ===== UI tool highlight đơn giản (đẹp hơn bạn hiện tại) =====
    private void updateToolUi() {
        if (btnPen == null || btnMarker == null || btnEraser == null) return;

        float off = 0.4f;
        float on = 1f;

        btnPen.setAlpha(off);
        btnMarker.setAlpha(off);
        btnEraser.setAlpha(off);

        if (currentTool == DrawingView.Tool.MARKER) {
            btnMarker.setAlpha(on);
        } else if (currentTool == DrawingView.Tool.ERASER) {
            btnEraser.setAlpha(on);
        } else {
            btnPen.setAlpha(on);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (pdfRenderer != null) pdfRenderer.close();
            if (fileDescriptor != null) fileDescriptor.close();
        } catch (IOException ignored) {}
        super.onDestroy();
    }
}
