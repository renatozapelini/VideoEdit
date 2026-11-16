package com.buddyapps.videoedithub;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
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
    private TextView status, maskStatus, previewStatus;
    private SeekBar frameSeek, intensitySeek, previewSeekRef;
    private Bitmap baseFrame;
    private final List<Bitmap> frames = new ArrayList<>();
    private final List<Bitmap> previewFrames = new ArrayList<>();
    private long durationMs = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean playing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(makeGradient());

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        box.setPadding(pad, dp(20), pad, dp(24));

        TextView title = new TextView(this);
        title.setText("Editor de Tatuagem");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Desenhe sobre a tatuagem • Rastreamento automático • Pré-visualização antes de aplicar");
        sub.setTextColor(Color.parseColor("#B6C2D9"));
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(6), 0, dp(16));
        box.addView(sub);

        // Step 1
        LinearLayout step1 = card("1. Vídeo", "Selecione o vídeo e escolha o quadro onde a tatuagem aparece bem.");
        Button pick = primaryButton("Selecionar vídeo");
        pick.setOnClickListener(v -> pickVideo());
        step1.addView(pick);
        status = label("Nenhum vídeo selecionado.");
        status.setPadding(0, dp(8), 0, 0);
        step1.addView(status);
        TextView fs = small("Posição do quadro");
        fs.setPadding(0, dp(10), 0, dp(4));
        step1.addView(fs);
        frameSeek = new SeekBar(this); frameSeek.setMax(100);
        frameSeek.setOnSeekBarChangeListener(simpleSeek(() -> loadSelectedFrame()));
        step1.addView(frameSeek);
        box.addView(step1);

        // Step 2 draw
        LinearLayout step2 = card("2. Desenhe a tatuagem", "Passe o dedo por cima da tatuagem para marcar a área. Você pode fazer vários traços.");
        FrameLayout drawContainer = new FrameLayout(this);
        GradientDrawable dBg = bg("#020617", 16); dBg.setStroke(dp(1), Color.parseColor("#334155"));
        drawContainer.setBackground(dBg);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(360));
        dlp.topMargin = dp(8);
        drawContainer.setLayoutParams(dlp);
        drawView = new DrawMaskView(this);
        drawView.setOnMaskChanged(has -> maskStatus.setText(has ? "✓ Área da tatuagem marcada. Pronto para rastrear." : "Nenhuma marcação ainda. Desenhe sobre a tatuagem."));
        drawContainer.addView(drawView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        step2.addView(drawContainer);

        maskStatus = label("Nenhuma marcação ainda. Desenhe sobre a tatuagem.");
        maskStatus.setTextColor(Color.parseColor("#22D3EE"));
        maskStatus.setPadding(0, dp(8), 0, 0);
        step2.addView(maskStatus);

        LinearLayout drawBtns = new LinearLayout(this); drawBtns.setOrientation(LinearLayout.HORIZONTAL); drawBtns.setPadding(0, dp(8), 0, 0);
        Button undo = secondaryButton("Desfazer"); undo.setOnClickListener(v -> drawView.undo());
        Button clear = secondaryButton("Limpar"); clear.setOnClickListener(v -> drawView.clear());
        Button loadF = secondaryButton("Atualizar quadro"); loadF.setOnClickListener(v -> loadSelectedFrame());
        LinearLayout.LayoutParams w1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); w1.rightMargin=dp(6);
        LinearLayout.LayoutParams w2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); w2.rightMargin=dp(6);
        LinearLayout.LayoutParams w3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        drawBtns.addView(undo,w1); drawBtns.addView(clear,w2); drawBtns.addView(loadF,w3);
        step2.addView(drawBtns);
        box.addView(step2);

        // Step 3 preview
        LinearLayout step3 = card("3. Rastrear e pré-visualizar", "O app segue a área desenhada ao longo do vídeo e mostra como ficará.");
        TextView il = small("Intensidade da cobertura/remoção"); il.setPadding(0,0,0,dp(4)); step3.addView(il);
        intensitySeek = new SeekBar(this); intensitySeek.setMax(25); intensitySeek.setProgress(12); step3.addView(intensitySeek);
        Button trackBtn = primaryButton("Gerar rastreio + pré-visualização");
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(10);
        trackBtn.setLayoutParams(tlp);
        trackBtn.setOnClickListener(v -> runTrackingPreview());
        step3.addView(trackBtn);

        previewStatus = label("Pré-visualização aparecerá aqui.");
        previewStatus.setPadding(0, dp(10), 0, dp(6));
        step3.addView(previewStatus);

        FrameLayout pvBox = new FrameLayout(this);
        GradientDrawable pBg = bg("#020617", 16); pBg.setStroke(dp(1), Color.parseColor("#334155"));
        pvBox.setBackground(pBg);
        LinearLayout.LayoutParams pvlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(340));
        pvBox.setLayoutParams(pvlp);
        previewView = new ImageView(this);
        previewView.setAdjustViewBounds(true);
        previewView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pvBox.addView(previewView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        TextView pvHint = new TextView(this);
        pvHint.setText("Pré-visualização");
        pvHint.setTextColor(Color.parseColor("#475569"));
        pvHint.setGravity(Gravity.CENTER);
        pvBox.addView(pvHint, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        step3.addView(pvBox);

        previewSeekRef = new SeekBar(this); previewSeekRef.setMax(0);
        previewSeekRef.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !previewFrames.isEmpty() && progress < previewFrames.size()) previewView.setImageBitmap(previewFrames.get(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { playing = false; }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        step3.addView(previewSeekRef);

        LinearLayout pvBtns = new LinearLayout(this); pvBtns.setOrientation(LinearLayout.HORIZONTAL); pvBtns.setPadding(0, dp(6), 0, 0);
        Button play = primaryButton("▶ Reproduzir prévia"); play.setOnClickListener(v -> playPreview());
        Button stop = secondaryButton("Parar"); stop.setOnClickListener(v -> playing = false);
        LinearLayout.LayoutParams pb1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); pb1.rightMargin=dp(6);
        LinearLayout.LayoutParams pb2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        pvBtns.addView(play,pb1); pvBtns.addView(stop,pb2);
        step3.addView(pvBtns);

        box.addView(step3);

        scroll.addView(box);
        root.addView(scroll);
        setContentView(root);
    }

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
        if (videoUri == null) { Toast.makeText(this, "Selecione um vídeo primeiro.", Toast.LENGTH_SHORT).show(); return; }
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
                        status.setText("Quadro pronto. Desenhe sobre a tatuagem na área abaixo.");
                    } else status.setText("Não foi possível carregar o quadro.");
                });
            } catch (Exception e) {
                handler.post(() -> status.setText("Erro ao ler vídeo: " + e.getMessage()));
            }
        }).start();
    }

    private void runTrackingPreview() {
        if (videoUri == null) { Toast.makeText(this, "Selecione um vídeo.", Toast.LENGTH_SHORT).show(); return; }
        RectF bounds = drawView.getDrawnBoundsInBitmap();
        if (bounds == null) { Toast.makeText(this, "Desenhe sobre a tatuagem primeiro (passo 2).", Toast.LENGTH_LONG).show(); return; }
        playing = false;
        previewStatus.setText("Rastreando a tatuagem e gerando pré-visualização...");
        previewFrames.clear();
        frames.clear();
        new Thread(() -> {
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(this, videoUri);
                String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long dms = dur != null ? Long.parseLong(dur) : 0;
                int count = 14;
                long step = dms > 0 ? (dms * 1000L / count) : 400000L;
                long startUs = dms > 0 ? (dms * 1000L * frameSeek.getProgress() / 100L) : 0;
                Bitmap first = mmr.getFrameAtTime(startUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (first == null) first = mmr.getFrameAtTime(startUs, MediaMetadataRetriever.OPTION_CLOSEST);
                if (first == null) throw new Exception("Sem quadros");
                frames.add(first);
                long t = startUs;
                for (int i = 1; i < count; i++) {
                    t += step;
                    if (dms > 0 && t > dms * 1000L) t = Math.max(0, dms * 1000L - 1000);
                    Bitmap b = mmr.getFrameAtTime(t, MediaMetadataRetriever.OPTION_CLOSEST);
                    if (b != null) frames.add(b);
                }
                mmr.release();

                RectF rect = new RectF(bounds);
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
                        if (previewSeekRef != null) { previewSeekRef.setMax(previewFrames.size()-1); previewSeekRef.setProgress(0); }
                        previewStatus.setText("✓ Pré-visualização pronta (" + previewFrames.size() + " quadros). Arraste ou toque em Reproduzir para ver o rastreio.");
                    } else previewStatus.setText("Falha ao gerar prévia.");
                });
            } catch (Exception e) {
                handler.post(() -> previewStatus.setText("Erro no rastreio: " + e.getMessage()));
            }
        }).start();
    }

    private void playPreview() {
        if (previewFrames.isEmpty()) { Toast.makeText(this, "Gere a pré-visualização primeiro (passo 3).", Toast.LENGTH_SHORT).show(); return; }
        if (playing) { playing = false; return; }
        playing = true;
        new Thread(() -> {
            int i = previewSeekRef != null ? previewSeekRef.getProgress() : 0;
            while (playing && i < previewFrames.size()) {
                final int idx = i;
                handler.post(() -> { previewView.setImageBitmap(previewFrames.get(idx)); if (previewSeekRef != null) previewSeekRef.setProgress(idx); });
                try { Thread.sleep(320); } catch (InterruptedException ignored) {}
                i++;
                if (i >= previewFrames.size()) i = 0; // loop
            }
            playing = false;
        }).start();
    }

    private LinearLayout card(String title, String desc) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = bg("#111827", 20);
        g.setStroke(dp(1), Color.parseColor("#1F2A44"));
        c.setBackground(g);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        c.setLayoutParams(lp);
        TextView t = new TextView(this); t.setText(title); t.setTextColor(Color.WHITE); t.setTextSize(18); t.setTypeface(Typeface.DEFAULT_BOLD);
        c.addView(t);
        TextView d = new TextView(this); d.setText(desc); d.setTextColor(Color.parseColor("#9FB0CC")); d.setTextSize(13); d.setPadding(0, dp(4), 0, dp(10));
        c.addView(d);
        return c;
    }
    private TextView label(String t){ TextView v=new TextView(this); v.setText(t); v.setTextColor(Color.parseColor("#E2E8F0")); v.setTextSize(14); return v; }
    private TextView small(String t){ TextView v=new TextView(this); v.setText(t); v.setTextColor(Color.parseColor("#94A3B8")); v.setTextSize(12); return v; }
    private Button primaryButton(String t){ Button b=new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTypeface(Typeface.DEFAULT_BOLD); b.setBackground(bg("#2563EB",14)); b.setPadding(dp(14),dp(12),dp(14),dp(12)); return b; }
    private Button secondaryButton(String t){ Button b=new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.parseColor("#E2E8F0")); b.setBackground(bg("#243041",12)); b.setPadding(dp(10),dp(10),dp(10),dp(10)); return b; }
    private GradientDrawable bg(String color,int r){ GradientDrawable g=new GradientDrawable(); g.setColor(Color.parseColor(color)); g.setCornerRadius(dp(r)); return g; }
    private GradientDrawable makeGradient(){ GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.parseColor("#0B1220"), Color.parseColor("#0F172A"), Color.parseColor("#111827")}); g.setCornerRadius(0); return g; }
    private int dp(int v){ return Math.round(getResources().getDisplayMetrics().density * v); }

    private SeekBar.OnSeekBarChangeListener simpleSeek(Runnable onStop){ return new SeekBar.OnSeekBarChangeListener(){ @Override public void onProgressChanged(SeekBar seekBar,int progress,boolean fromUser){} @Override public void onStartTrackingTouch(SeekBar seekBar){} @Override public void onStopTrackingTouch(SeekBar seekBar){ onStop.run(); } }; }
}
