package com.buddyapps.videoedithub;

import android.graphics.Bitmap;
import android.graphics.RectF;

public class SimpleTracker {
    // Simple template matching tracker: searches around previous position for best SSD match on downscaled grayscale
    public static RectF track(Bitmap prevFrame, Bitmap nextFrame, RectF prevRect) {
        if (prevFrame == null || nextFrame == null || prevRect == null) return prevRect;
        int pw = prevFrame.getWidth(), ph = prevFrame.getHeight();
        int nw = nextFrame.getWidth(), nh = nextFrame.getHeight();
        RectF r = clamp(prevRect, pw, ph);
        int tx = Math.round(r.left), ty = Math.round(r.top), tw = Math.max(4, Math.round(r.width())), th = Math.max(4, Math.round(r.height()));
        if (tx + tw > pw) tw = pw - tx; if (ty + th > ph) th = ph - ty;
        if (tw < 4 || th < 4) return prevRect;

        // downscale factor for speed
        int ds = 2;
        int stw = Math.max(2, tw / ds), sth = Math.max(2, th / ds);
        int[] tpl = getGrayPatch(prevFrame, tx, ty, tw, th, stw, sth);

        int search = Math.max(20, Math.max(tw, th) / 3);
        int bestX = tx, bestY = ty;
        long bestScore = Long.MAX_VALUE;
        int step = Math.max(2, ds);
        for (int dy = -search; dy <= search; dy += step) {
            for (int dx = -search; dx <= search; dx += step) {
                int cx = tx + dx, cy = ty + dy;
                if (cx < 0 || cy < 0 || cx + tw > nw || cy + th > nh) continue;
                int[] cand = getGrayPatch(nextFrame, cx, cy, tw, th, stw, sth);
                long ssd = ssd(tpl, cand);
                if (ssd < bestScore) { bestScore = ssd; bestX = cx; bestY = cy; }
            }
        }
        // refine around best
        int rstep = 1;
        for (int dy = -step; dy <= step; dy += rstep) {
            for (int dx = -step; dx <= step; dx += rstep) {
                int cx = bestX + dx, cy = bestY + dy;
                if (cx < 0 || cy < 0 || cx + tw > nw || cy + th > nh) continue;
                int[] cand = getGrayPatch(nextFrame, cx, cy, tw, th, stw, sth);
                long ssd = ssd(tpl, cand);
                if (ssd < bestScore) { bestScore = ssd; bestX = cx; bestY = cy; }
            }
        }
        // if match is very bad, keep previous (scaled)
        RectF nr = new RectF(bestX, bestY, bestX + tw, bestY + th);
        // adjust if frame sizes differ
        if (nw != pw || nh != ph) {
            float sx = (float) nw / pw, sy = (float) nh / ph;
            nr = new RectF(bestX, bestY, bestX + tw * sx, bestY + th * sy);
        }
        return clamp(nr, nw, nh);
    }

    private static RectF clamp(RectF r, int w, int h) {
        float l = Math.max(0, r.left), t = Math.max(0, r.top);
        float rr = Math.min(w, r.right), b = Math.min(h, r.bottom);
        if (rr - l < 4) rr = Math.min(w, l + 4);
        if (b - t < 4) b = Math.min(h, t + 4);
        return new RectF(l,t,rr,b);
    }

    private static int[] getGrayPatch(Bitmap bmp, int x, int y, int w, int h, int sw, int sh) {
        int[] src = new int[w*h];
        bmp.getPixels(src, 0, w, x, y, w, h);
        int[] out = new int[sw*sh];
        for (int j = 0; j < sh; j++) {
            int sy = j * h / sh;
            for (int i = 0; i < sw; i++) {
                int sx = i * w / sw;
                int p = src[sy*w + sx];
                int g = ((p>>16)&0xff)*30 + ((p>>8)&0xff)*59 + (p&0xff)*11;
                out[j*sw + i] = g; // scaled by 100
            }
        }
        return out;
    }

    private static long ssd(int[] a, int[] b) {
        long s = 0; int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) { long d = a[i]-b[i]; s += d*d; }
        return s;
    }

    public static Bitmap applyBlur(Bitmap src, RectF rect, int radius) {
        Bitmap out = src.copy(Bitmap.Config.ARGB_8888, true);
        int w = out.getWidth(), h = out.getHeight();
        int x1 = Math.max(0, Math.round(rect.left));
        int y1 = Math.max(0, Math.round(rect.top));
        int x2 = Math.min(w, Math.round(rect.right));
        int y2 = Math.min(h, Math.round(rect.bottom));
        if (x2<=x1||y2<=y1) return out;
        int[] pixels = new int[w*h];
        out.getPixels(pixels,0,w,0,0,w,h);
        int[] res = pixels.clone();
        int r = Math.max(1, Math.min(radius, 20));
        for (int y=y1;y<y2;y++){
            for(int x=x1;x<x2;x++){
                long sr=0,sg=0,sb=0,sa=0; int c=0;
                for(int dy=-r;dy<=r;dy++){int yy=y+dy; if(yy<y1||yy>=y2) continue;
                    for(int dx=-r;dx<=r;dx++){int xx=x+dx; if(xx<x1||xx>=x2) continue; int p=pixels[yy*w+xx]; sa+=(p>>>24)&0xff; sr+=(p>>16)&0xff; sg+=(p>>8)&0xff; sb+=p&0xff; c++;}}
                if(c>0) res[y*w+x]=((int)(sa/c)<<24)|((int)(sr/c)<<16)|((int)(sg/c)<<8)|(int)(sb/c);
            }
        }
        out.setPixels(res,0,w,0,0,w,h);
        return out;
    }
}
