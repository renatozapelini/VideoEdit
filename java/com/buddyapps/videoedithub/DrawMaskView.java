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
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.List;

public class DrawMaskView extends View {
    public interface OnMaskChanged { void onChanged(boolean hasMask); }
    private Bitmap bitmap;
    private final Matrix matrix = new Matrix();
    private final Matrix inverse = new Matrix();
    private final Paint bmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint strokePaint = new Paint();
    private final Paint fillPaint = new Paint();
    private final Paint guidePaint = new Paint();
    private final Path currentPath = new Path();
    private final List<Path> paths = new ArrayList<>();
    private RectF imageRect = new RectF();
    private RectF drawnBounds = null;
    private OnMaskChanged listener;

    public DrawMaskView(Context c) {
        super(c);
        strokePaint.setColor(Color.parseColor("#22D3EE"));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(10f);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setAntiAlias(true);
        fillPaint.setColor(0x4422D3EE);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);
        guidePaint.setColor(Color.parseColor("#64748B"));
        guidePaint.setTextSize(42f);
        guidePaint.setAntiAlias(true);
        setBackgroundColor(Color.parseColor("#020617"));
        setClickable(true);
    }

    public void setOnMaskChanged(OnMaskChanged l){ this.listener = l; }

    public void setBitmap(Bitmap b) {
        this.bitmap = b;
        paths.clear();
        currentPath.reset();
        drawnBounds = null;
        requestLayout();
        invalidate();
        notifyChanged();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) { super.onSizeChanged(w, h, oldw, oldh); updateMatrix(); }

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
        if (bitmap == null) {
            Paint p = new Paint(guidePaint); p.setColor(Color.parseColor("#94A3B8")); p.setTextSize(36f);
            canvas.drawText("Selecione um vídeo para começar", 40, getHeight()/2f, p);
            return;
        }
        updateMatrix();
        canvas.drawBitmap(bitmap, matrix, bmpPaint);
        for (Path pth : paths) {
            Path vp = new Path(pth); vp.transform(matrix);
            canvas.drawPath(vp, fillPaint);
            canvas.drawPath(vp, strokePaint);
        }
        if (!currentPath.isEmpty()) {
            Path vp = new Path(currentPath); vp.transform(matrix);
            canvas.drawPath(vp, fillPaint);
            canvas.drawPath(vp, strokePaint);
        }
        if (paths.isEmpty() && currentPath.isEmpty()) {
            Paint p = new Paint(guidePaint); p.setColor(Color.parseColor("#E2E8F0")); p.setTextSize(34f);
            canvas.drawText("Desenhe com o dedo sobre a tatuagem", 30, 60, p);
        }
    }

    private void disallowParents(boolean disallow) {
        ViewParent p = getParent();
        while (p != null) {
            p.requestDisallowInterceptTouchEvent(disallow);
            p = p.getParent();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (bitmap == null) return false;
        float[] pts = {e.getX(), e.getY()};
        inverse.mapPoints(pts);
        float x = pts[0], y = pts[1];
        x = Math.max(0, Math.min(bitmap.getWidth()-1, x));
        y = Math.max(0, Math.min(bitmap.getHeight()-1, y));
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                disallowParents(true);
                currentPath.reset();
                currentPath.moveTo(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                currentPath.lineTo(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                currentPath.lineTo(x, y);
                RectF tb = new RectF(); currentPath.computeBounds(tb, true);
                if (tb.width() < 8 && tb.height() < 8) {
                    currentPath.reset();
                    currentPath.addCircle(x, y, Math.max(18f, Math.min(bitmap.getWidth(), bitmap.getHeight())*0.03f), Path.Direction.CW);
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
                disallowParents(false);
                return true;
        }
        return super.onTouchEvent(e);
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
