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
        // "Removal-like" cover: sample surrounding skin/border and fill inside with blended patches (multi-pass), then soften edges
        Bitmap out = src.copy(Bitmap.Config.ARGB_8888, true);
        int w = out.getWidth(), h = out.getHeight();
        int x1 = Math.max(0, Math.round(rect.left));
        int y1 = Math.max(0, Math.round(rect.top));
        int x2 = Math.min(w, Math.round(rect.right));
        int y2 = Math.min(h, Math.round(rect.bottom));
        if (x2<=x1||y2<=y1) return out;
        int rw = x2-x1, rh = y2-y1;
        int margin = Math.max(6, Math.min(Math.min(rw,rh)/3, 24));
        int bx1 = Math.max(0, x1 - margin), by1 = Math.max(0, y1 - margin);
        int bx2 = Math.min(w, x2 + margin), by2 = Math.min(h, y2 + margin);

        int[] pixels = new int[w*h];
        out.getPixels(pixels,0,w,0,0,w,h);
        int[] res = pixels.clone();

        // average border ring color
        long br=0,bg=0,bb=0,ba=0; int bc=0;
        for (int y=by1;y<by2;y++){
            for(int x=bx1;x<bx2;x++){
                boolean inside = (x>=x1 && x<x2 && y>=y1 && y<y2);
                if (inside) continue;
                int p = pixels[y*w+x];
                ba+=(p>>>24)&0xff; br+=(p>>16)&0xff; bg+=(p>>8)&0xff; bb+=p&0xff; bc++;
            }
        }
        int ar=(int)(bc>0?br/bc:200), ag=(int)(bc>0?bg/bc:170), ab=(int)(bc>0?bb/bc:150), aa=(int)(bc>0?ba/bc:255);

        int r = Math.max(3, Math.min(radius + 6, 22));
        // pass 1 & 2: inpaint-like fill by pulling from nearest border/outside samples with smoothing
        for (int pass=0; pass<2; pass++) {
            int[] base = res.clone();
            for (int y=y1;y<y2;y++){
                for(int x=x1;x<x2;x++){
                    // sample from a ring just outside the rect biased by position
                    int best = sampleBorder(base, w, h, x, y, x1,y1,x2,y2, margin);
                    int sr=(best>>16)&0xff, sg=(best>>8)&0xff, sb=(best)&0xff, sa=(best>>>24)&0xff;
                    // blend toward border average to reduce tattoo darkness/pattern
                    float blend = 0.55f + 0.15f*pass;
                    int nr=(int)(sr*(1-blend)+ar*blend);
                    int ng=(int)(sg*(1-blend)+ag*blend);
                    int nb=(int)(sb*(1-blend)+ab*blend);
                    int na=(int)(sa*(1-blend)+aa*blend);
                    base[y*w+x] = (clamp255(na)<<24)|(clamp255(nr)<<16)|(clamp255(ng)<<8)|clamp255(nb);
                }
            }
            res = base;
        }
        // local smoothing inside to make it look natural
        int[] smooth = res.clone();
        int sr2 = Math.max(2, Math.min(radius, 12));
        for (int y=y1;y<y2;y++){
            for(int x=x1;x<x2;x++){
                long tr=0,tg=0,tb=0,ta=0; int c=0;
                for(int dy=-sr2;dy<=sr2;dy++){int yy=y+dy; if(yy<by1||yy>=by2) continue;
                    for(int dx=-sr2;dx<=sr2;dx++){int xx=x+dx; if(xx<bx1||xx>=bx2) continue; int p=res[yy*w+xx]; ta+=(p>>>24)&0xff; tr+=(p>>16)&0xff; tg+=(p>>8)&0xff; tb+=p&0xff; c++;}}
                if(c>0) smooth[y*w+x]=((int)(ta/c)<<24)|((int)(tr/c)<<16)|((int)(tg/c)<<8)|(int)(tb/c);
            }
        }
        // feather edges for seamless blend
        int feather = Math.max(3, Math.min(Math.min(rw,rh)/4, 10));
        for (int y=y1;y<y2;y++){
            for(int x=x1;x<x2;x++){
                int dxm = Math.min(x-x1, x2-1-x);
                int dym = Math.min(y-y1, y2-1-y);
                int d = Math.min(dxm,dym);
                if (d < feather) {
                    float a = (float)d / (float)feather; // 0 edge -> 1 inside
                    int op = pixels[y*w+x];
                    int np = smooth[y*w+x];
                    int or=(op>>16)&0xff, og=(op>>8)&0xff, ob=op&0xff, oa=(op>>>24)&0xff;
                    int nr=(np>>16)&0xff, ng=(np>>8)&0xff, nb=np&0xff, na=(np>>>24)&0xff;
                    int fr=(int)(or*(1-a)+nr*a), fg=(int)(og*(1-a)+ng*a), fb=(int)(ob*(1-a)+nb*a), fa=(int)(oa*(1-a)+na*a);
                    smooth[y*w+x]=(clamp255(fa)<<24)|(clamp255(fr)<<16)|(clamp255(fg)<<8)|clamp255(fb);
                }
            }
        }
        out.setPixels(smooth,0,w,0,0,w,h);
        return out;
    }

    private static int sampleBorder(int[] img, int w, int h, int x, int y, int x1,int y1,int x2,int y2,int margin){
        // find nearest point on border and sample outward
        int cx = Math.min(Math.max(x, x1), x2-1);
        int cy = Math.min(Math.max(y, y1), y2-1);
        int dl = x - x1, dr = x2-1 - x, dt = y - y1, db = y2-1 - y;
        int m = Math.min(Math.min(dl,dr), Math.min(dt,db));
        int sx=x, sy=y;
        if (m==dl) sx = x1 - 1 - (dl % Math.max(1,margin));
        else if (m==dr) sx = x2 + (dr % Math.max(1,margin));
        else if (m==dt) sy = y1 - 1 - (dt % Math.max(1,margin));
        else sy = y2 + (db % Math.max(1,margin));
        // also try mirrored offset
        if (sx < 0 || sx >= w || sy < 0 || sy >= h) {
            // fallback: sample from just outside closest edge
            if (dl<=dr && dl<=dt && dl<=db) { sx = Math.max(0,x1-2); sy = y; }
            else if (dr<=dt && dr<=db) { sx = Math.min(w-1,x2+1); sy = y; }
            else if (dt<=db) { sx = x; sy = Math.max(0,y1-2); }
            else { sx = x; sy = Math.min(h-1,y2+1); }
        }
        sx = Math.max(0, Math.min(w-1, sx)); sy = Math.max(0, Math.min(h-1, sy));
        return img[sy*w + sx];
    }
    private static int clamp255(int v){ return v<0?0:(v>255?255:v); }
}
