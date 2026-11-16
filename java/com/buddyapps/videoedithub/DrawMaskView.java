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
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawMaskView extends View {
    private Bitmap bitmap;
    private final Matrix matrix = new Matrix();
    private final Matrix inverse = new Matrix();
    private final Paint bmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint strokePaint = new Paint();
    private final Paint fillPaint = new Paint();
    private final Path currentPath = new Path();
    private final List<Path> paths = new ArrayList<>();
    private RectF imageRect = new RectF();
    private RectF drawnBounds = null;

    public DrawMaskView(Context c) {
        super(c);
        strokePaint.setColor(Color.parseColor("#FF5252"));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(8f);
        strokePaint.setAntiAlias(true);
        fillPaint.setColor(0x55FF5252);
        fillPaint.setStyle(Paint.Style.FILL);
        setBackgroundColor(Color.parseColor("#0B1220"));
    }

    public void setBitmap(Bitmap b) {
        this.bitmap = b;
        paths.clear();
        currentPath.reset();
        drawnBounds = null;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateMatrix();
    }

    private void updateMatrix() {
        if (bitmap == null || getWidth() == 0 || getHeight() == 0) return;
        float vw = getWidth(), vh = getHeight();
        float bw = bitmap.getWidth(), bh = bitmap.getHeight();
        float scale = Math.min(vw / bw, vh / bh);
        float dx = (vw - bw * scale) / 2f;
        float dy = (vh - bh * scale) / 2f;
        matrix.reset();
        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);
        matrix.invert(inverse);
        imageRect.set(0, 0, bw, bh);
        matrix.mapRect(imageRect);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) return;
        updateMatrix();
        canvas.drawBitmap(bitmap, matrix, bmpPaint);
        for (Path p : paths) {
            canvas.drawPath(transformToView(p), fillPaint);
            canvas.drawPath(transformToView(p), strokePaint);
        }
        if (!currentPath.isEmpty()) {
            canvas.drawPath(transformToView(currentPath), fillPaint);
            canvas.drawPath(transformToView(currentPath), strokePaint);
        }
    }

    private Path transformToView(Path srcInBitmap) {
        Path p = new Path(srcInBitmap);
        p.transform(matrix);
        return p;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (bitmap == null) return false;
        float[] pts = {e.getX(), e.getY()};
        inverse.mapPoints(pts);
        float x = pts[0], y = pts[1];
        // clamp to bitmap
        x = Math.max(0, Math.min(bitmap.getWidth()-1, x));
        y = Math.max(0, Math.min(bitmap.getHeight()-1, y));
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath.reset();
                currentPath.moveTo(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                currentPath.lineTo(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                currentPath.lineTo(x, y);
                currentPath.close();
                Path saved = new Path(currentPath);
                paths.add(saved);
                RectF b = new RectF();
                saved.computeBounds(b, true);
                if (drawnBounds == null) drawnBounds = new RectF(b); else drawnBounds.union(b);
                currentPath.reset();
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }

    public void clear() {
        paths.clear();
        currentPath.reset();
        drawnBounds = null;
        invalidate();
    }

    public RectF getDrawnBoundsInBitmap() {
        if (drawnBounds == null) return null;
        RectF r = new RectF(drawnBounds);
        // clamp
        r.left = Math.max(0, r.left); r.top = Math.max(0, r.top);
        r.right = Math.min(bitmap != null ? bitmap.getWidth() : r.right, r.right);
        r.bottom = Math.min(bitmap != null ? bitmap.getHeight() : r.bottom, r.bottom);
        if (r.width() < 5 || r.height() < 5) return null;
        return r;
    }

    public boolean hasMask() { return drawnBounds != null; }
}
