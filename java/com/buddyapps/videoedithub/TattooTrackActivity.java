package com.buddyapps.videoedithub;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TattooTrackActivity extends Activity {
    private static final int PICK_VIDEO = 3001;
    private Uri videoUri;
    private DrawMaskView drawView;
    private ImageView previewView;
    private TextView status;
    private SeekBar frameSeek;
    private SeekBar intensitySeek;
    private Bitmap baseFrame;
    private final List<Bitmap> frames = new ArrayList<>();
    private final List<Bitmap> previewFrames = new ArrayList<>();
    private long durationMs = 0;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0B1220"));

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        box.setPadding(pad,pad,pad,pad);

        TextView title = new TextView(this);
        title.setText("Rastrear e remover tatuagem");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Desenhe sobre a tatuagem, rastreie no vídeo e veja a pré-visualização.");
        sub.setTextColor(Color.parseColor("#CBD5E1"));
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(4), 0, dp(12));
        box.addView(sub);

        LinearLayout card = makeCard();
        Button pick = primaryButton("Selecionar vídeo");
        pick.setOnClickListener(v -> pickVideo());
        card.addView(pick);

        status = new TextView(this);
        status.setTextColor(Color.parseColor("#E2E8F0"));
        status.setText("Nenhum vídeo selecionado.");
        status.setPadding(0, dp(8), 0, 0);
        card.addView(status);
        box.addView(card);

        TextView drawLabel = sectionTitle("1) Desenhe a área da tatuagem");
        box.addView(drawLabel);
        FrameLayout drawContainer = new FrameLayout(this);
        drawContainer.setBackground(makeBg("#111827", 14));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(320));
        dlp.topMargin = dp(8);
        drawContainer.setLayoutParams(dlp);
        drawView = new DrawMaskView(this);
        drawContainer.addView(drawView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        box.addView(drawContainer);

        LinearLayout drawBtns = new LinearLayout(this);
        drawBtns.setOrientation(LinearLayout.HORIZONTAL);
        drawBtns.setPadding(0, dp(8), 0, 0);
        Button clear = secondaryButton("Limpar desenho");
        clear.setOnClickListener(v -> { drawView.clear(); status.setText("Desenho limpo. Marque a tatuagem novamente."); });
        Button useFrame = secondaryButton("Usar quadro atual");
        useFrame.setOnClickListener(v -> loadSelectedFrame());
        LinearLayout.LayoutParams hb = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        hb.rightMargin = dp(6);
        drawBtns.addView(clear, hb);
        LinearLayout.LayoutParams hb2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        drawBtns.addView(useFrame, hb2);
        box.addView(drawBtns);

        TextView seekLabel = smallLabel("Escolha o quadro para desenhar:");
        seekLabel.setPadding(0, dp(8), 0, 0);
        box.addView(seekLabel);
        frameSeek = new SeekBar(this);
        frameSeek.setMax(100);
        frameSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { loadSelectedFrame(); }
        });
        box.addView(frameSeek);

        TextView t2 = sectionTitle("2) Rastrear e pré-visualizar");
        t2.setPadding(0, dp(14), 0, 0);
        box.addView(t2);
        TextView intLabel = smallLabel("Intensidade da remoção/cobertura:");
        box.addView(intLabel);
        intensitySeek = new SeekBar(this); intensitySeek.setMax(25); intensitySeek.setProgress(12); box.addView(intensitySeek);

        Button trackBtn = primaryButton("Rastrear tatuagem e gerar prévia");
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tLp.topMargin = dp(8);
        trackBtn.setLayoutParams(tLp);
        trackBtn.setOnClickListener(v -> runTrackingPreview());
        box.addView(trackBtn);

        previewView = new ImageView(this);
        previewView.setAdjustViewBounds(true);
        previewView.setMinimumHeight(dp(300));
        previewView.setBackground(makeBg("#111827", 14));
        LinearLayout.LayoutParams pvlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pvlp.topMargin = dp(10);
        previewView.setLayoutParams(pvlp);
        box.addView(previewView);

        SeekBar previewSeek = new SeekBar(this);
        previewSeek.setMax(0);
        previewSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !previewFrames.isEmpty() && progress < previewFrames.size()) previewView.setImageBitmap(previewFrames.get(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        box.addView(previewSeek);
        this.previewSeekRef = previewSeek;

        Button play = secondaryButton("Reproduzir prévia");
        play.setOnClickListener(v -> playPreview());
        box.addView(play);

        TextView note = smallLabel("A prévia mostra o rastreio da área desenhada quadro a quadro e a cobertura aplicada. Para vídeo completo, exporte após validar.");
        note.setPadding(0, dp(10), 0, dp(20));
        box.addView(note);

        scroll.addView(box);
        root.addView(scroll);
        setContentView(root);
    }

    private SeekBar previewSeekRef;

    private void pickVideo() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("video/*");
        startActivityForResult(i, PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            videoUri = data.getData();
            if (videoUri != null) {
                try { getContentResolver().takePersistableUriPermission(videoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
                status.setText("Vídeo selecionado. Carregando quadro...");
                loadSelectedFrame();
            }
        }
    }

    private void loadSelectedFrame() {
        if (videoUri == null) return;
        new Thread(() -> {
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(this, videoUri);
                String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                durationMs = dur != null ? Long.parseLong(dur) : 0;
                long timeUs = durationMs <= 0 ? 0 : (durationMs * 1000L * frameSeek.getProgress() / 100L);
                Bitmap b = mmr.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST);
                mmr.release();
                baseFrame = b;
                handler.post(() -> {
                    if (b != null) {
                        drawView.setBitmap(b);
                        status.setText("Quadro carregado. Desenhe sobre a tatuagem para marcar a área.");
                    } else {
                        status.setText("Não foi possível carregar o quadro.");
                    }
                });
            } catch (Exception e) {
                handler.post(() -> status.setText("Erro ao ler vídeo: " + e.getMessage()));
            }
        }).start();
    }

    private void runTrackingPreview() {
        if (videoUri == null) { Toast.makeText(this, "Selecione um vídeo.", Toast.LENGTH_SHORT).show(); return; }
        RectF bounds = drawView.getDrawnBoundsInBitmap();
        if (bounds == null) { Toast.makeText(this, "Desenhe a área da tatuagem no quadro.", Toast.LENGTH_SHORT).show(); return; }
        status.setText("Rastreando e gerando pré-visualização...");
        previewFrames.clear();
        frames.clear();
        new Thread(() -> {
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(this, videoUri);
                String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long dms = dur != null ? Long.parseLong(dur) : 0;
                int count = 12; // sample frames for preview
                long step = dms > 0 ? (dms * 1000L / count) : 500000L;
                // determine start time near selected seek
                long startUs = dms > 0 ? (dms * 1000L * frameSeek.getProgress() / 100L) : 0;
                Bitmap first = mmr.getFrameAtTime(startUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (first == null) first = mmr.getFrameAtTime(startUs, MediaMetadataRetriever.OPTION_CLOSEST);
                if (first == null) throw new Exception("Sem quadros");
                frames.add(first);
                // gather frames forward and backward-ish around for a short preview window
                long t = startUs;
                for (int i = 1; i < count; i++) {
                    t += step;
                    if (dms > 0 && t > dms * 1000L) t = (i * step) % Math.max(1, dms * 1000L);
                    Bitmap b = mmr.getFrameAtTime(t, MediaMetadataRetriever.OPTION_CLOSEST);
                    if (b != null) frames.add(b);
                }
                mmr.release();

                RectF rect = new RectF(bounds);
                // scale rect if first frame size differs from draw base
                if (baseFrame != null && (first.getWidth()!=baseFrame.getWidth() || first.getHeight()!=baseFrame.getHeight())) {
                    float sx = (float)first.getWidth()/baseFrame.getWidth();
                    float sy = (float)first.getHeight()/baseFrame.getHeight();
                    rect = new RectF(rect.left*sx, rect.top*sy, rect.right*sx, rect.bottom*sy);
                }
                int radius = Math.max(6, intensitySeek.getProgress());
                Bitmap prev = frames.get(0);
                RectF cur = rect;
                for (int i = 0; i < frames.size(); i++) {
                    Bitmap f = frames.get(i);
                    if (i > 0) cur = SimpleTracker.track(prev, f, cur);
                    Bitmap out = SimpleTracker.applyBlur(f, cur, radius);
                    previewFrames.add(out);
                    prev = f;
                }
                handler.post(() -> {
                    if (!previewFrames.isEmpty()) {
                        previewView.setImageBitmap(previewFrames.get(0));
                        if (previewSeekRef != null) previewSeekRef.setMax(previewFrames.size()-1);
                        status.setText("Pré-visualização pronta: " + previewFrames.size() + " quadros rastreados. Use o controle para conferir.");
                    } else {
                        status.setText("Falha ao gerar prévia.");
                    }
                });
            } catch (Exception e) {
                handler.post(() -> status.setText("Erro no rastreio: " + e.getMessage()));
            }
        }).start();
    }

    private void playPreview() {
        if (previewFrames.isEmpty()) { Toast.makeText(this, "Gere a pré-visualização primeiro.", Toast.LENGTH_SHORT).show(); return; }
        new Thread(() -> {
            for (int i = 0; i < previewFrames.size(); i++) {
                final int idx = i;
                handler.post(() -> {
                    previewView.setImageBitmap(previewFrames.get(idx));
                    if (previewSeekRef != null) previewSeekRef.setProgress(idx);
                });
                try { Thread.sleep(350); } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private LinearLayout makeCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(makeBg("#111827", 16));
        c.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        c.setLayoutParams(lp);
        return c;
    }

    private TextView sectionTitle(String t) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextColor(Color.WHITE); v.setTextSize(16);
        return v;
    }
    private TextView smallLabel(String t) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextColor(Color.parseColor("#CBD5E1")); v.setTextSize(13);
        return v;
    }
    private Button primaryButton(String t) {
        Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        b.setBackground(makeBg("#2563EB", 12)); b.setPadding(dp(12), dp(10), dp(12), dp(10)); return b;
    }
    private Button secondaryButton(String t) {
        Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        b.setBackground(makeBg("#334155", 12)); b.setPadding(dp(10), dp(8), dp(10), dp(8)); return b;
    }
    private GradientDrawable makeBg(String color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(color));
        g.setCornerRadius(dp(radiusDp));
        return g;
    }
    private int dp(int v) { return Math.round(getResources().getDisplayMetrics().density * v); }
}
