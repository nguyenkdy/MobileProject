package com.example.mynoesapplication;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.PdfPageAdapter;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;

public class ReadOnlyOneNoteActivity extends AppCompatActivity {

    // ================= UI =================
    private ImageButton btnBack;
    private TextView txtTitle, txtContent;
    private DrawingView drawingView;
    private RecyclerView rvPdfPages;

    // ================= Firebase =================
    private FirebaseFirestore db;
    private String ownerUid, noteId;

    // ================= PDF =================
    private ParcelFileDescriptor pdfFd;
    private android.graphics.pdf.PdfRenderer pdfRenderer;
    private PdfPageAdapter pdfAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // ⭐ DÙNG LAYOUT RIÊNG
        setContentView(R.layout.activity_readonly_one_note);

        // ===== Insets =====
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // ===== Bind UI =====
        btnBack = findViewById(R.id.btnBack);
        txtTitle = findViewById(R.id.txtTitle);
        txtContent = findViewById(R.id.txtContent);
        drawingView = findViewById(R.id.drawingView);
        rvPdfPages = findViewById(R.id.rvPdfPages);

        btnBack.setOnClickListener(v -> finish());

        // ===== RecyclerView PDF =====
        rvPdfPages.setLayoutManager(new LinearLayoutManager(this));
        rvPdfPages.setNestedScrollingEnabled(false);
        rvPdfPages.setVisibility(View.GONE);

        // ===== Drawing readonly =====
        if (drawingView != null) {
            drawingView.setReadOnly(true);
        }

        // ===== Firebase =====
        db = FirebaseFirestore.getInstance();

        // ===== Intent =====
        ownerUid = getIntent().getStringExtra("ownerUid");
        noteId = getIntent().getStringExtra("noteId");

        if (ownerUid == null || ownerUid.isEmpty()
                || noteId == null || noteId.isEmpty()) {
            Toast.makeText(this, "Missing note data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ===== Load note =====
        loadNote();
    }

    // ================= LOAD NOTE =================
    private void loadNote() {
        db.collection("users")
                .document(ownerUid)
                .collection("notes")
                .document(noteId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) return;

                    // ===== TITLE & CONTENT =====
                    txtTitle.setText(doc.getString("title"));
                    txtContent.setText(doc.getString("content"));

                    // ===== DRAWING =====
                    String drawing = doc.getString("drawing");
                    if (drawingView != null && drawing != null && !drawing.isEmpty()) {
                        drawingView.importFromJson(drawing);
                        drawingView.setVisibility(View.VISIBLE);
                    } else if (drawingView != null) {
                        drawingView.setVisibility(View.GONE);
                    }

                    // ===== PDF (MULTI-PAGE) =====
                    String pdfPath = doc.getString("pdfPath");
                    if (pdfPath != null && !pdfPath.isEmpty()) {
                        setupPdf(pdfPath);
                    } else {
                        rvPdfPages.setVisibility(View.GONE);
                    }
                });
    }

    // ================= SETUP PDF =================
    private void setupPdf(String pdfPath) {
        try {
            File file = new File(pdfPath);
            if (!file.exists()) {
                rvPdfPages.setVisibility(View.GONE);
                return;
            }

            pdfFd = ParcelFileDescriptor.open(
                    file,
                    ParcelFileDescriptor.MODE_READ_ONLY
            );

            pdfRenderer = new android.graphics.pdf.PdfRenderer(pdfFd);

            // ⭐ Store annotation theo noteId (bạn đã có sẵn)
            PdfAnnotationStore store =
                    new PdfAnnotationStore(this, noteId);

            pdfAdapter = new PdfPageAdapter(
                    this,
                    pdfRenderer,
                    store
            );

            // ⭐ READ ONLY
            pdfAdapter.setReadOnly(true);

            rvPdfPages.setAdapter(pdfAdapter);
            rvPdfPages.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            e.printStackTrace();
            rvPdfPages.setVisibility(View.GONE);
        }
    }

    // ================= CLEANUP =================
    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (pdfRenderer != null) pdfRenderer.close();
            if (pdfFd != null) pdfFd.close();
        } catch (Exception ignored) {}
    }
}
