package com.buddyapps.videoedithub;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.List;

public class DrawMaskView extends View {
    public interface OnMaskChanged { void onChanged(boolean hasMask); }
    private Bitmap bitmap;
    private final Matrix baseMatrix = new Matrix();
    private final Matrix drawMatrix = new Matrix();
    private final Matrix inverse = new Matrix();
    private final Paint bmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint strokePaint = new Paint();
    private final Paint fillPaint = new Paint();
    private final Paint guidePaint = new Paint();
    private final Paint chipPaint = new Paint();
    private final Paint chipText = new Paint();
    private final Path currentPath = new Path();
    private final List<Path> paths = new ArrayList<>();
    private RectF drawnBounds = null;
    private OnMaskChanged listener;
    private boolean drawing = false;
    private boolean drawingEnabled = true;
    private float scaleFactor = 1f;
    private float transX = 0f, transY = 0f;
    private float lastX, lastY;
    private boolean isPanning = false;
    private final ScaleGestureDetector scaleDetector;

    public DrawMaskView(Context c) {
        super(c);
        strokePaint.setColor(Color.parseColor("#5EEAD4"));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(8f);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setAntiAlias(true);
        fillPaint.setColor(0x335EEAD4);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);
        guidePaint.setColor(Color.parseColor("#94A3B8"));
        guidePaint.setTextSize(32f);
        guidePaint.setAntiAlias(true);
        chipPaint.setColor(0xAA0F172A);
        chipText.setColor(Color.parseColor("#E2E8F0"));
        chipText.setTextSize(26f);
        chipText.setAntiAlias(true);
        setBackgroundColor(Color.parseColor("#020617"));
        setClickable(true);
        setLongClickable(true);
        scaleDetector = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float prev = scaleFactor;
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(1f, Math.min(6f, scaleFactor));
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                float ratio = scaleFactor / prev;
                transX = focusX - ratio * (focusX - transX);
                transY = focusY - ratio * (focusY - transY);
                clampTranslation();
                updateDrawMatrix();
                invalidate();
                return true;
            }
        });
    }

    public void setOnMaskChanged(OnMaskChanged l){ this.listener = l; }
    public void setDrawingEnabled(boolean enabled){ this.drawingEnabled = enabled; if (!enabled) { drawing = false; currentPath.reset(); invalidate(); } }
    public boolean isDrawingEnabled(){ return drawingEnabled; }

    public void setBitmap(Bitmap b) {
        this.bitmap = b;
        paths.clear();
        currentPath.reset();
        drawnBounds = null;
        scaleFactor = 1f; transX = 0f; transY = 0f;
        requestLayout();
        invalidate();
        notifyChanged();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) { super.onSizeChanged(w, h, oldw, oldh); updateBaseMatrix(); updateDrawMatrix(); }

    private void updateBaseMatrix() {
        baseMatrix.reset();
        if (bitmap == null || getWidth() == 0 || getHeight() == 0) return;
        float vw = getWidth(), vh = getHeight();
        float bw = bitmap.getWidth(), bh = bitmap.getHeight();
        float scale = Math.min(vw / bw, vh / bh);
        float dx = (vw - bw * scale) / 2f;
        float dy = (vh - bh * scale) / 2f;
        baseMatrix.setScale(scale, scale);
        baseMatrix.postTranslate(dx, dy);
    }

    private void updateDrawMatrix() {
        drawMatrix.set(baseMatrix);
        drawMatrix.postScale(scaleFactor, scaleFactor);
        drawMatrix.postTranslate(transX, transY);
        if (bitmap != null) drawMatrix.invert(inverse);
    }

    private void clampTranslation() {
        if (bitmap == null) return;
        RectF r = new RectF(0,0,bitmap.getWidth(), bitmap.getHeight());
        drawMatrix.set(baseMatrix);
        drawMatrix.postScale(scaleFactor, scaleFactor);
        drawMatrix.postTranslate(transX, transY);
        drawMatrix.mapRect(r);
        float vw = getWidth(), vh = getHeight();
        float fixX = 0, fixY = 0;
        if (r.width() <= vw) fixX = (vw - (r.left + r.right)) / 2f; else { if (r.left > 0) fixX = -r.left; if (r.right < vw) fixX = vw - r.right; }
        if (r.height() <= vh) fixY = (vh - (r.top + r.bottom)) / 2f; else { if (r.top > 0) fixY = -r.top; if (r.bottom < vh) fixY = vh - r.bottom; }
        transX += fixX; transY += fixY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) {
            Paint p = new Paint(guidePaint); p.setColor(Color.parseColor("#CBD5E1")); p.setTextSize(34f);
            canvas.drawText("Selecione um vídeo para começar", 40, getHeight()/2f, p);
            return;
        }
        updateDrawMatrix();
        canvas.drawBitmap(bitmap, drawMatrix, bmpPaint);
        for (Path pth : paths) {
            Path vp = new Path(pth); vp.transform(drawMatrix);
            canvas.drawPath(vp, fillPaint);
            canvas.drawPath(vp, strokePaint);
        }
        if (!currentPath.isEmpty()) {
            Path vp = new Path(currentPath); vp.transform(drawMatrix);
            canvas.drawPath(vp, fillPaint);
            canvas.drawPath(vp, strokePaint);
        }
        if (paths.isEmpty() && currentPath.isEmpty()) {
            Paint p = new Paint(guidePaint); p.setColor(Color.parseColor("#F1F5F9")); p.setTextSize(30f);
            String msg = drawingEnabled ? "Toque em Desenhar e marque a tatuagem" : "Ative Desenhar para marcar a tatuagem";
            canvas.drawText(msg, 24, 52, p);
            Paint p2 = new Paint(guidePaint); p2.setColor(Color.parseColor("#94A3B8")); p2.setTextSize(26f);
            canvas.drawText("2 dedos: zoom e mover", 24, 88, p2);
        }
        String z = String.format("Zoom %.1fx", scaleFactor);
        float tw = chipText.measureText(z);
        canvas.drawRoundRect(getWidth()-tw-40, 16, getWidth()-16, 56, 18, 18, chipPaint);
        canvas.drawText(z, getWidth()-tw-28, 44, chipText);
        // mode chip
        String mode = drawingEnabled ? "Desenho: ON" : "Desenho: OFF";
        float mw = chipText.measureText(mode);
        Paint mp = new Paint(chipPaint); mp.setColor(drawingEnabled ? 0xAA064E3B : 0xAA1F2937);
        canvas.drawRoundRect(16, 16, 16+mw+24, 56, 18, 18, mp);
        Paint mt = new Paint(chipText); mt.setColor(drawingEnabled ? Color.parseColor("#D1FAE5") : Color.parseColor("#CBD5E1"));
        canvas.drawText(mode, 28, 44, mt);
    }

    private void disallowParents(boolean disallow) {
        ViewParent p = getParent();
        while (p != null) { p.requestDisallowInterceptTouchEvent(disallow); p = p.getParent(); }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (bitmap == null) return false;
        disallowParents(true);
        scaleDetector.onTouchEvent(e);
        int action = e.getActionMasked();

        if (e.getPointerCount() >= 2) {
            if (drawing) { currentPath.reset(); drawing = false; invalidate(); }
            isPanning = true;
            if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_MOVE) {
                float ax = (e.getX(0)+e.getX(1))/2f, ay = (e.getY(0)+e.getY(1))/2f;
                if (action == MotionEvent.ACTION_POINTER_DOWN) { lastX = ax; lastY = ay; }
                else { transX += ax - lastX; transY += ay - lastY; lastX = ax; lastY = ay; clampTranslation(); updateDrawMatrix(); invalidate(); }
            }
            return true;
        }

        if (isPanning && action != MotionEvent.ACTION_DOWN) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) { isPanning = false; disallowParents(false); }
            return true;
        }
        isPanning = false;

        if (!drawingEnabled) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) disallowParents(false);
            return true;
        }

        float[] pts = {e.getX(), e.getY()};
        updateDrawMatrix();
        inverse.mapPoints(pts);
        float x = Math.max(0, Math.min(bitmap.getWidth()-1, pts[0]));
        float y = Math.max(0, Math.min(bitmap.getHeight()-1, pts[1]));
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                drawing = true;
                currentPath.reset();
                currentPath.moveTo(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (drawing) { currentPath.lineTo(x, y); invalidate(); }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (drawing) {
                    currentPath.lineTo(x, y);
                    RectF tb = new RectF(); currentPath.computeBounds(tb, true);
                    if (tb.width() < 8 && tb.height() < 8) {
                        currentPath.reset();
                        float r = Math.max(12f, Math.min(bitmap.getWidth(), bitmap.getHeight())*0.02f);
                        currentPath.addCircle(x, y, r, Path.Direction.CW);
                    }
                    currentPath.close();
                    Path saved = new Path(currentPath);
                    paths.add(saved);
                    RectF b = new RectF();
                    saved.computeBounds(b, true);
                    if (drawnBounds == null) drawnBounds = new RectF(b); else drawnBounds.union(b);
                    currentPath.reset();
                    invalidate();
                    notifyChanged();
                }
                drawing = false;
                disallowParents(false);
                return true;
        }
        return true;
    }

    private void notifyChanged(){ if (listener != null) listener.onChanged(hasMask()); }
    public void clear() { paths.clear(); currentPath.reset(); drawnBounds = null; invalidate(); notifyChanged(); }
    public void undo() { if (!paths.isEmpty()) paths.remove(paths.size()-1); recomputeBounds(); invalidate(); notifyChanged(); }
    private void recomputeBounds(){
        drawnBounds = null;
        for (Path p: paths){ RectF b=new RectF(); p.computeBounds(b,true); if(drawnBounds==null) drawnBounds=new RectF(b); else drawnBounds.union(b);}        
    }
    public RectF getDrawnBoundsInBitmap() {
        if (drawnBounds == null || bitmap == null) return null;
        RectF r = new RectF(drawnBounds);
        r.left = Math.max(0, r.left); r.top = Math.max(0, r.top);
        r.right = Math.min(bitmap.getWidth(), r.right);
        r.bottom = Math.min(bitmap.getHeight(), r.bottom);
        if (r.width() < 5 || r.height() < 5) return null;
        return r;
    }
    public boolean hasMask() { return drawnBounds != null && !paths.isEmpty(); }
}
