package com.example.mynoesapplication;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    // ================= TOOL =================
    public enum Tool { PEN, MARKER, ERASER, LASER }
    private Tool currentTool = Tool.PEN;
    private int currentColor = Color.BLACK;
    private float penStrokeWidth = 6f;
    private float markerStrokeWidth = 20f;



    // ================= CALLBACK =================
    public interface OnDrawingChangeListener {
        void onDrawingChanged();
    }
    private OnDrawingChangeListener changeListener;

    public void setOnDrawingChangeListener(OnDrawingChangeListener l) {
        this.changeListener = l;
    }

    // ================= DRAW =================
    private Path currentPath;
    private Paint currentPaint;

    private static class Stroke {
        Path path;
        Paint paint;
        boolean temporary; // laser
        List<PointF> points;

        Stroke(Path p, Paint paint, boolean temp, List<PointF> pts) {
            this.path = p;
            this.paint = paint;
            this.temporary = temp;
            this.points = pts;
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
        setLayerType(LAYER_TYPE_SOFTWARE, null); // REQUIRED for eraser
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setPen();
    }

    // ================= PAINT FACTORY =================
    private Paint basePaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        return p;
    }

    // ==================================================
// BASE PAINT (DÙNG CHUNG)
// ==================================================
    private Paint createBasePaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        return p;
    }


    // ================= TOOL SETTERS =================
    public void setPen() {
        currentTool = Tool.PEN;

        Paint p = createBasePaint();
        p.setColor(currentColor);
        p.setStrokeWidth(penStrokeWidth);
        p.setAlpha(255);
        p.setXfermode(null);

        currentPaint = p;
    }

    public void setMarker() {
        currentTool = Tool.MARKER;

        Paint p = createBasePaint();
        p.setColor(currentColor);
        p.setStrokeWidth(markerStrokeWidth);
        p.setAlpha(120);
        p.setXfermode(null);

        currentPaint = p;
    }

    public void setEraser() {
        currentTool = Tool.ERASER;
        Paint p = basePaint();
        p.setStrokeWidth(50f);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        currentPaint = p;
    }

    public void setLaser() {
        currentTool = Tool.LASER;
        Paint p = basePaint();
        p.setColor(Color.RED);
        p.setStrokeWidth(8f);
        p.setAlpha(180);
        p.setXfermode(null);
        currentPaint = p;
    }

    // ================= DRAW =================
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

    // ================= TOUCH =================
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 🔒 READ ONLY → KHÔNG NHẬN TOUCH → CHO SCROLL
        if (!isEnabled()) {
            return false;
        }

        // ✏️ EDIT MODE → CHẶN SCROLL → VẼ
        getParent().requestDisallowInterceptTouchEvent(true);
//        if (getVisibility() != VISIBLE) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                currentPath = new Path();
                currentPath.moveTo(x, y);
                currentPoints = new ArrayList<>();
                currentPoints.add(new PointF(x, y));
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                    currentPoints.add(new PointF(x, y));
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (currentPath != null && currentPaint != null) {

                    boolean isLaser = (currentTool == Tool.LASER);
                    Stroke stroke = new Stroke(
                            currentPath,
                            new Paint(currentPaint),
                            isLaser,
                            currentPoints
                    );

                    if (!isLaser) {
                        strokes.add(stroke);
                        redoStrokes.clear();
                        if (changeListener != null) changeListener.onDrawingChanged();
                    } else {
                        strokes.add(stroke);
                        postDelayed(() -> {
                            strokes.remove(stroke);
                            invalidate();
                        }, 300);
                    }

                    currentPath = null;
                    currentPoints = null;
                    invalidate();
                }
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }

        return false;
    }

    private List<PointF> currentPoints;

    // ================= UNDO / REDO =================
    public void undo() {
        if (strokes.isEmpty()) return;

        Stroke last = strokes.get(strokes.size() - 1);
        if (last.temporary) return;

        strokes.remove(strokes.size() - 1);
        redoStrokes.add(last);
        invalidate();
        if (changeListener != null) changeListener.onDrawingChanged();
    }

    public void redo() {
        if (redoStrokes.isEmpty()) return;

        Stroke s = redoStrokes.remove(redoStrokes.size() - 1);
        strokes.add(s);
        invalidate();
        if (changeListener != null) changeListener.onDrawingChanged();
    }

    // ================= EXPORT / IMPORT (JSON) =================
    public String exportToJson() {
        try {
            JSONArray arr = new JSONArray();

            for (Stroke s : strokes) {
                if (s.temporary) continue;

                JSONObject o = new JSONObject();
                o.put("color", s.paint.getColor());
                o.put("width", s.paint.getStrokeWidth());
                o.put("alpha", s.paint.getAlpha());
                o.put("xfer", s.paint.getXfermode() != null ? "CLEAR" : "NORMAL");

                JSONArray pts = new JSONArray();
                for (PointF p : s.points) {
                    JSONArray xy = new JSONArray();
                    xy.put(p.x);
                    xy.put(p.y);
                    pts.put(xy);
                }
                o.put("points", pts);

                arr.put(o);
            }

            return arr.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public void importFromJson(String json) {
        try {
            strokes.clear();
            redoStrokes.clear();

            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                Paint p = basePaint();
                p.setColor(o.getInt("color"));
                p.setStrokeWidth((float) o.getDouble("width"));
                p.setAlpha(o.getInt("alpha"));
                if ("CLEAR".equals(o.getString("xfer"))) {
                    p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                }

                JSONArray pts = o.getJSONArray("points");
                Path path = new Path();
                List<PointF> points = new ArrayList<>();

                for (int j = 0; j < pts.length(); j++) {
                    JSONArray xy = pts.getJSONArray(j);
                    float x = (float) xy.getDouble(0);
                    float y = (float) xy.getDouble(1);
                    if (j == 0) path.moveTo(x, y);
                    else path.lineTo(x, y);
                    points.add(new PointF(x, y));
                }

                strokes.add(new Stroke(path, p, false, points));
            }

            invalidate();
        } catch (Exception ignored) {}
    }

    // ================= STATE =================
    public boolean hasDrawing() {
        for (Stroke s : strokes) {
            if (!s.temporary) return true;
        }
        return false;
    }

    public void clear() {
        strokes.clear();
        redoStrokes.clear();
        currentPath = null;
        invalidate();
        if (changeListener != null) changeListener.onDrawingChanged();
    }

    public Bitmap exportPreviewBitmap(int width, int height) {
        if (!hasDrawing()) return null;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        float scaleX = width / (float) getWidth();
        float scaleY = height / (float) getHeight();
        canvas.scale(scaleX, scaleY);

        for (Stroke s : strokes) {
            if (!s.temporary) {
                canvas.drawPath(s.path, s.paint);
            }
        }
        return bmp;
    }

    public void setColor(int color) {
        this.currentColor = color;

        if (currentTool == Tool.PEN) {
            setPen();
        } else if (currentTool == Tool.MARKER) {
            setMarker();
        }
    }

    public void setPenStrokeWidth(float width) {
        penStrokeWidth = Math.max(2f, Math.min(width, 12f));
        if (currentTool == Tool.PEN) setPen();
    }

    public void setMarkerStrokeWidth(float width) {
        markerStrokeWidth = Math.max(10f, Math.min(width, 40f));
        if (currentTool == Tool.MARKER) setMarker();
    }


}
