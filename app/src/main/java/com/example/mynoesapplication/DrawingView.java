package com.example.mynoesapplication;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    // ================= TOOL =================
    public enum Tool { PEN, MARKER, ERASER, LASER }
    private Tool currentTool = Tool.PEN;

    // ================= DRAW =================
    private Path currentPath;
    private Paint currentPaint;

    private static class Stroke {
        Path path;
        Paint paint;
        boolean temporary; // for laser

        Stroke(Path p, Paint paint, boolean temp) {
            this.path = p;
            this.paint = paint;
            this.temporary = temp;
        }
    }

    private final List<Stroke> strokes = new ArrayList<>();
    private final List<Stroke> redoStrokes = new ArrayList<>();

    // ================= CONSTRUCTOR =================
    public DrawingView(Context context) {
        super(context);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // ================= INIT =================
    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null); // needed for eraser
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setPen();
    }

    // ==================================================
    // PAINT FACTORY
    // ==================================================
    private Paint createBasePaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        return p;
    }

    // ==================================================
    // TOOL SETTERS
    // ==================================================
    public void setPen() {
        currentTool = Tool.PEN;
        Paint p = createBasePaint();
        p.setColor(Color.BLACK);
        p.setStrokeWidth(6f);
        p.setAlpha(255);
        p.setXfermode(null);
        currentPaint = p;
    }

    public void setMarker() {
        currentTool = Tool.MARKER;
        Paint p = createBasePaint();
        p.setColor(Color.BLACK);
        p.setStrokeWidth(20f);
        p.setAlpha(120);
        p.setXfermode(null);
        currentPaint = p;
    }

    public void setEraser() {
        currentTool = Tool.ERASER;
        Paint p = createBasePaint();
        p.setStrokeWidth(30f);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        currentPaint = p;
    }

    public void setLaser() {
        currentTool = Tool.LASER;
        Paint p = createBasePaint();
        p.setColor(Color.RED);
        p.setStrokeWidth(8f);
        p.setAlpha(180);
        p.setXfermode(null);
        currentPaint = p;
    }

    // ==================================================
    // DRAW
    // ==================================================
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Stroke s : strokes) {
            canvas.drawPath(s.path, s.paint);
        }

        if (currentPath != null && currentPaint != null) {
            canvas.drawPath(currentPath, currentPaint);
        }
    }

    // ==================================================
    // TOUCH EVENT
    // ==================================================
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getVisibility() != VISIBLE) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                currentPath = new Path();
                currentPath.moveTo(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (currentPath != null && currentPaint != null) {
                    Paint strokePaint = new Paint(currentPaint);
                    boolean temp = (currentTool == Tool.LASER);

                    Stroke stroke = new Stroke(currentPath, strokePaint, temp);
                    strokes.add(stroke);
                    redoStrokes.clear();

                    if (temp) {
                        postDelayed(() -> {
                            strokes.remove(stroke);
                            invalidate();
                        }, 300);
                    }

                    currentPath = null;
                    invalidate();
                }
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }

        return false;
    }

    // ==================================================
    // UNDO / REDO (DRAWING)
    // ==================================================
    public void undo() {
        if (strokes.isEmpty()) return;
        Stroke s = strokes.remove(strokes.size() - 1);
        redoStrokes.add(s);
        invalidate();
    }

    public void redo() {
        if (redoStrokes.isEmpty()) return;
        Stroke s = redoStrokes.remove(redoStrokes.size() - 1);
        strokes.add(s);
        invalidate();
    }

    // ==================================================
    // EXPORT BITMAP (LOCAL USE – NO S3)
    // ==================================================
    public Bitmap exportBitmap() {
        if (getWidth() <= 0 || getHeight() <= 0) return null;

        Bitmap result = Bitmap.createBitmap(
                getWidth(),
                getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        for (Stroke s : strokes) {
            if (!s.temporary) {
                canvas.drawPath(s.path, s.paint);
            }
        }

        return result;
    }

    // ==================================================
    // STATE
    // ==================================================
    public boolean hasDrawing() {
        return !strokes.isEmpty();
    }

    public void clear() {
        strokes.clear();
        redoStrokes.clear();
        currentPath = null;
        invalidate();
    }
}
