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
        box.setPadding(pad, dp(22), pad, dp(28));

        TextView title = new TextView(this);
        title.setText("Rastreio de Tatuagem");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Desenhe com precisão • Dê zoom no vídeo • Rastreie e visualize o resultado");
        sub.setTextColor(Color.parseColor("#A5B4FC"));
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(6), 0, dp(18));
        box.addView(sub);

        // Step 1
        LinearLayout step1 = card("①  Selecionar vídeo", "Escolha o vídeo e o quadro onde a tatuagem está mais visível.");
        Button pick = primaryButton("Selecionar vídeo");
        pick.setOnClickListener(v -> pickVideo());
        step1.addView(pick);
        status = label("Nenhum vídeo selecionado.");
        status.setPadding(0, dp(10), 0, 0);
        step1.addView(status);
        TextView fs = small("Posição do quadro"); fs.setPadding(0, dp(12), 0, dp(4)); step1.addView(fs);
        frameSeek = styledSeek(100); frameSeek.setOnSeekBarChangeListener(simpleSeek(this::loadSelectedFrame)); step1.addView(frameSeek);
        box.addView(step1);

        // Step 2 draw
        LinearLayout step2 = card("②  Desenhar a tatuagem", "Use 1 dedo para desenhar sobre a tatuagem. Use 2 dedos para dar zoom e mover o vídeo.");
        FrameLayout drawContainer = new FrameLayout(this);
        GradientDrawable dBg = bg("#050816", 18); dBg.setStroke(dp(1), Color.parseColor("#334155"));
        drawContainer.setBackground(dBg);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(380));
        dlp.topMargin = dp(8);
        drawContainer.setLayoutParams(dlp);
        drawView = new DrawMaskView(this);
        drawView.setOnMaskChanged(has -> {
            maskStatus.setText(has ? "✓ Tatuagem marcada. Você já pode rastrear." : "Desenhe sobre a tatuagem para marcar a área.");
            maskStatus.setTextColor(Color.parseColor(has ? "#34D399" : "#A5B4FC"));
        });
        drawContainer.addView(drawView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        step2.addView(drawContainer);

        maskStatus = label("Desenhe sobre a tatuagem para marcar a área.");
        maskStatus.setTextColor(Color.parseColor("#A5B4FC"));
        maskStatus.setPadding(0, dp(10), 0, 0);
        step2.addView(maskStatus);

        LinearLayout drawBtns = new LinearLayout(this); drawBtns.setOrientation(LinearLayout.HORIZONTAL); drawBtns.setPadding(0, dp(10), 0, 0);
        Button undo = secondaryButton("Desfazer"); undo.setOnClickListener(v -> drawView.undo());
        Button clear = secondaryButton("Limpar"); clear.setOnClickListener(v -> drawView.clear());
        Button loadF = accentButton("Atualizar quadro"); loadF.setOnClickListener(v -> loadSelectedFrame());
        LinearLayout.LayoutParams w1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); w1.rightMargin=dp(8);
        LinearLayout.LayoutParams w2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); w2.rightMargin=dp(8);
        LinearLayout.LayoutParams w3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        drawBtns.addView(undo,w1); drawBtns.addView(clear,w2); drawBtns.addView(loadF,w3);
        step2.addView(drawBtns);
        box.addView(step2);

        // Step 3 preview
        LinearLayout step3 = card("③  Rastrear e pré-visualizar", "O app segue a marcação quadro a quadro e mostra como ficará o resultado.");
        TextView il = small("Intensidade da cobertura"); il.setPadding(0,0,0,dp(4)); step3.addView(il);
        intensitySeek = styledSeek(25); intensitySeek.setProgress(12); step3.addView(intensitySeek);
        Button trackBtn = primaryButton("Gerar rastreio e pré-visualização");
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(12);
        trackBtn.setLayoutParams(tlp);
        trackBtn.setOnClickListener(v -> runTrackingPreview());
        step3.addView(trackBtn);

        previewStatus = label("A pré-visualização aparecerá aqui.");
        previewStatus.setPadding(0, dp(12), 0, dp(8));
        step3.addView(previewStatus);

        FrameLayout pvBox = new FrameLayout(this);
        GradientDrawable pBg = bg("#050816", 18); pBg.setStroke(dp(1), Color.parseColor("#334155"));
        pvBox.setBackground(pBg);
        LinearLayout.LayoutParams pvlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(350));
        pvBox.setLayoutParams(pvlp);
        previewView = new ImageView(this);
        previewView.setAdjustViewBounds(true);
        previewView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pvBox.addView(previewView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        TextView pvHint = new TextView(this);
        pvHint.setText("Pré-visualização do resultado");
        pvHint.setTextColor(Color.parseColor("#475569"));
        pvHint.setTextSize(15);
        pvHint.setGravity(Gravity.CENTER);
        pvBox.addView(pvHint, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        step3.addView(pvBox);

        previewSeekRef = styledSeek(0);
        previewSeekRef.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !previewFrames.isEmpty() && progress < previewFrames.size()) previewView.setImageBitmap(previewFrames.get(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { playing = false; }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        step3.addView(previewSeekRef);

        LinearLayout pvBtns = new LinearLayout(this); pvBtns.setOrientation(LinearLayout.HORIZONTAL); pvBtns.setPadding(0, dp(8), 0, 0);
        Button play = primaryButton("▶ Reproduzir"); play.setOnClickListener(v -> playPreview());
        Button stop = secondaryButton("Parar"); stop.setOnClickListener(v -> playing = false);
        LinearLayout.LayoutParams pb1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); pb1.rightMargin=dp(8);
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
                        status.setText("Quadro pronto. Dê zoom e desenhe sobre a tatuagem.");
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
        if (bounds == null) { Toast.makeText(this, "Desenhe sobre a tatuagem primeiro.", Toast.LENGTH_LONG).show(); return; }
        playing = false;
        previewStatus.setText("Analisando quadros e rastreando a tatuagem...");
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
                        previewStatus.setText("✓ Pré-visualização pronta (" + previewFrames.size() + " quadros). Confira se o rastreio ficou correto.");
                    } else previewStatus.setText("Falha ao gerar prévia.");
                });
            } catch (Exception e) {
                handler.post(() -> previewStatus.setText("Erro no rastreio: " + e.getMessage()));
            }
        }).start();
    }

    private void playPreview() {
        if (previewFrames.isEmpty()) { Toast.makeText(this, "Gere a pré-visualização primeiro.", Toast.LENGTH_SHORT).show(); return; }
        if (playing) { playing = false; return; }
        playing = true;
        new Thread(() -> {
            int i = previewSeekRef != null ? previewSeekRef.getProgress() : 0;
            while (playing && i < previewFrames.size()) {
                final int idx = i;
                handler.post(() -> { previewView.setImageBitmap(previewFrames.get(idx)); if (previewSeekRef != null) previewSeekRef.setProgress(idx); });
                try { Thread.sleep(320); } catch (InterruptedException ignored) {}
                i++;
                if (i >= previewFrames.size()) i = 0;
            }
            playing = false;
        }).start();
    }

    private LinearLayout card(String title, String desc) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#151B2E"), Color.parseColor("#101827")});
        g.setCornerRadius(dp(22));
        g.setStroke(dp(1), Color.parseColor("#26324A"));
        c.setBackground(g);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        c.setLayoutParams(lp);
        TextView t = new TextView(this); t.setText(title); t.setTextColor(Color.WHITE); t.setTextSize(18); t.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        c.addView(t);
        TextView d = new TextView(this); d.setText(desc); d.setTextColor(Color.parseColor("#9DB0D0")); d.setTextSize(13); d.setPadding(0, dp(5), 0, dp(12));
        c.addView(d);
        return c;
    }
    private TextView label(String t){ TextView v=new TextView(this); v.setText(t); v.setTextColor(Color.parseColor("#E6EDF8")); v.setTextSize(14); return v; }
    private TextView small(String t){ TextView v=new TextView(this); v.setText(t); v.setTextColor(Color.parseColor("#8FA3C4")); v.setTextSize(12); return v; }
    private Button primaryButton(String t){ Button b=new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{Color.parseColor("#2563EB"), Color.parseColor("#7C3AED")}); g.setCornerRadius(dp(16));
        b.setBackground(g); b.setPadding(dp(14),dp(12),dp(14),dp(12)); return b; }
    private Button secondaryButton(String t){ Button b=new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.parseColor("#E2E8F0"));
        GradientDrawable g=bg("#243044",14); g.setStroke(dp(1), Color.parseColor("#334155")); b.setBackground(g); b.setPadding(dp(10),dp(10),dp(10),dp(10)); return b; }
    private Button accentButton(String t){ Button b=new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.parseColor("#E0F2FE"));
        GradientDrawable g=bg("#0F766E",14); b.setBackground(g); b.setPadding(dp(10),dp(10),dp(10),dp(10)); return b; }
    private SeekBar styledSeek(int max){ SeekBar s=new SeekBar(this); s.setMax(max); return s; }
    private GradientDrawable bg(String color,int r){ GradientDrawable g=new GradientDrawable(); g.setColor(Color.parseColor(color)); g.setCornerRadius(dp(r)); return g; }
    private GradientDrawable makeGradient(){ return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.parseColor("#070B18"), Color.parseColor("#0B1220"), Color.parseColor("#111827")}); }
    private int dp(int v){ return Math.round(getResources().getDisplayMetrics().density * v); }
    private SeekBar.OnSeekBarChangeListener simpleSeek(Runnable onStop){ return new SeekBar.OnSeekBarChangeListener(){ @Override public void onProgressChanged(SeekBar seekBar,int progress,boolean fromUser){} @Override public void onStartTrackingTouch(SeekBar seekBar){} @Override public void onStopTrackingTouch(SeekBar seekBar){ onStop.run(); } }; }
}
