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
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
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
    private final List<Long> previewTimesUs = new ArrayList<>();
    private long durationMs = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean playing = false;
    private Button drawToggleBtn;
    private Button generateBtn;
    private Button previewBtn;
    private Button saveBtn;
    private Bitmap lastPreviewBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(makeGradient());

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(12);
        box.setPadding(pad, dp(14), pad, dp(16));

        TextView title = new TextView(this);
        title.setText("Rastreio de Tatuagem");
        title.setTextColor(Color.WHITE);
        title.setTextSize(23);
        title.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Desenhe • Zoom com 2 dedos • Rastreie • Prévia contínua");
        sub.setTextColor(Color.parseColor("#A5B4FC"));
        sub.setTextSize(12);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(2), 0, dp(10));
        box.addView(sub);

        // Step 1
        LinearLayout step1 = card("① Vídeo", "Selecione o vídeo e o quadro inicial.");
        Button pick = primaryButton("Selecionar vídeo");
        pick.setOnClickListener(v -> pickVideo());
        step1.addView(pick);
        status = label("Nenhum vídeo selecionado.");
        status.setPadding(0, dp(6), 0, 0);
        step1.addView(status);
        TextView fs = small("Quadro inicial"); fs.setPadding(0, dp(6), 0, dp(2)); step1.addView(fs);
        frameSeek = styledSeek(100); frameSeek.setOnSeekBarChangeListener(simpleSeek(this::loadSelectedFrame)); step1.addView(frameSeek);
        box.addView(step1);

        // Step 2 draw
        LinearLayout step2 = card("② Marcar tatuagem", "Toque em Desenhar para começar, marque a tatuagem e confirme para parar. 2 dedos: zoom e mover (tela fixa).");
        FrameLayout drawContainer = new FrameLayout(this);
        GradientDrawable dBg = bg("#050816", 16); dBg.setStroke(dp(1), Color.parseColor("#334155"));
        drawContainer.setBackground(dBg);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(240));
        dlp.topMargin = dp(6);
        drawContainer.setLayoutParams(dlp);
        drawView = new DrawMaskView(this);
        drawView.setDrawingEnabled(false);
        drawView.setOnMaskChanged(has -> {
            if (drawView.isDrawingEnabled()) {
                maskStatus.setText(has ? "✓ Área marcada. Confirme para parar de desenhar." : "Modo desenho ativo: marque a tatuagem.");
                maskStatus.setTextColor(Color.parseColor(has ? "#34D399" : "#A5B4FC"));
            } else {
                maskStatus.setText(has ? "✓ Tatuagem marcada. Ative Desenhar para ajustar." : "Toque em Desenhar para marcar a tatuagem.");
                maskStatus.setTextColor(Color.parseColor(has ? "#34D399" : "#A5B4FC"));
            }
        });
        drawContainer.addView(drawView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        step2.addView(drawContainer);

        maskStatus = label("Toque em Desenhar para marcar a tatuagem.");
        maskStatus.setTextColor(Color.parseColor("#A5B4FC"));
        maskStatus.setPadding(0, dp(6), 0, 0);
        step2.addView(maskStatus);

        LinearLayout drawBtns = new LinearLayout(this); drawBtns.setOrientation(LinearLayout.HORIZONTAL); drawBtns.setPadding(0, dp(6), 0, 0);
        drawToggleBtn = primaryButton("✏️ Desenhar"); drawToggleBtn.setOnClickListener(v -> toggleDrawing());
        Button undo = secondaryButton("Desfazer"); undo.setOnClickListener(v -> drawView.undo());
        Button clear = secondaryButton("Limpar"); clear.setOnClickListener(v -> drawView.clear());
        LinearLayout.LayoutParams w1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); w1.rightMargin=dp(6);
        LinearLayout.LayoutParams w2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); w2.rightMargin=dp(6);
        LinearLayout.LayoutParams w3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        drawBtns.addView(drawToggleBtn,w1); drawBtns.addView(undo,w2); drawBtns.addView(clear,w3);
        step2.addView(drawBtns);

        LinearLayout drawBtns2 = new LinearLayout(this); drawBtns2.setOrientation(LinearLayout.HORIZONTAL); drawBtns2.setPadding(0, dp(6), 0, 0);
        Button loadF = accentButton("Atualizar quadro"); loadF.setOnClickListener(v -> loadSelectedFrame());
        drawBtns2.addView(loadF, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        step2.addView(drawBtns2);
        box.addView(step2);

        // Step 3 preview
        LinearLayout step3 = card("③ Pré-visualização", "Rastreio contínuo no estilo do vídeo.");
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        TextView il = small("Intensidade"); il.setPadding(0, dp(6), dp(8), 0);
        intensitySeek = styledSeek(25); intensitySeek.setProgress(12);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams isp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        row.addView(il, ilp); row.addView(intensitySeek, isp);
        step3.addView(row);

        previewBtn = secondaryButton("Ver preview");
        previewBtn.setOnClickListener(v -> runSingleFramePreview());
        step3.addView(previewBtn);

        generateBtn = primaryButton("Gerar");
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(8);
        generateBtn.setLayoutParams(tlp);
        generateBtn.setOnClickListener(v -> runTrackingPreview());
        step3.addView(generateBtn);

        saveBtn = accentButton("Salvar");
        saveBtn.setOnClickListener(v -> savePreviewBitmap());
        step3.addView(saveBtn);

        previewStatus = label("Prévia aparecerá aqui.");
        previewStatus.setPadding(0, dp(8), 0, dp(4));
        step3.addView(previewStatus);

        FrameLayout pvBox = new FrameLayout(this);
        GradientDrawable pBg = bg("#050816", 16); pBg.setStroke(dp(1), Color.parseColor("#334155"));
        pvBox.setBackground(pBg);
        LinearLayout.LayoutParams pvlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220));
        pvBox.setLayoutParams(pvlp);
        previewView = new ImageView(this);
        previewView.setAdjustViewBounds(true);
        previewView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pvBox.addView(previewView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        TextView pvHint = new TextView(this);
        pvHint.setText("Pré-visualização");
        pvHint.setTextColor(Color.parseColor("#475569"));
        pvHint.setTextSize(14);
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

        LinearLayout pvBtns = new LinearLayout(this); pvBtns.setOrientation(LinearLayout.HORIZONTAL); pvBtns.setPadding(0, dp(4), 0, 0);
        Button play = primaryButton("▶ Reproduzir"); play.setOnClickListener(v -> playPreview());
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

    private void toggleDrawing() {
        boolean now = !drawView.isDrawingEnabled();
        drawView.setDrawingEnabled(now);
        if (now) {
            drawToggleBtn.setText("✓ Confirmar desenho");
            GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{Color.parseColor("#059669"), Color.parseColor("#10B981")}); g.setCornerRadius(dp(14));
            drawToggleBtn.setBackground(g);
            maskStatus.setText(drawView.hasMask() ? "✓ Área marcada. Ajuste ou confirme para parar." : "Modo desenho ativo: marque a tatuagem.");
            maskStatus.setTextColor(Color.parseColor("#A5B4FC"));
        } else {
            drawToggleBtn.setText("✏️ Desenhar");
            GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{Color.parseColor("#2563EB"), Color.parseColor("#7C3AED")}); g.setCornerRadius(dp(14));
            drawToggleBtn.setBackground(g);
            maskStatus.setText(drawView.hasMask() ? "✓ Tatuagem marcada. Pronto para rastrear." : "Toque em Desenhar para marcar a tatuagem.");
            maskStatus.setTextColor(Color.parseColor(drawView.hasMask() ? "#34D399" : "#A5B4FC"));
        }
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
                        status.setText("Quadro pronto. Dê zoom e marque a tatuagem.");
                    } else status.setText("Não foi possível carregar o quadro.");
                });
            } catch (Throwable e) {
                final String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                handler.post(() -> status.setText("Erro ao ler vídeo: " + msg));
            }
        }).start();
    }

    private RectF capRect(RectF r, Bitmap bmp, float maxFraction) {
        if (r.width() <= 0 || r.height() <= 0) return r;
        float maxW = Math.max(1f, bmp.getWidth() * maxFraction);
        float maxH = Math.max(1f, bmp.getHeight() * maxFraction);
        float w = r.width();
        float h = r.height();
        if (w > maxW || h > maxH) {
            float scale = Math.min(maxW / w, maxH / h);
            float newW = w * scale;
            float newH = h * scale;
            return new RectF(r.left, r.top, r.left + newW, r.top + newH);
        }
        return r;
    }

    private void runSingleFramePreview() {
        if (videoUri == null) { Toast.makeText(this, "Selecione um vídeo.", Toast.LENGTH_SHORT).show(); return; }
        RectF bounds = drawView.getDrawnBoundsInBitmap();
        if (bounds == null) { Toast.makeText(this, "Desenhe sobre a tatuagem primeiro.", Toast.LENGTH_LONG).show(); return; }
        previewBtn.setText("Gerando...");
        previewStatus.setText("Gerando prévia...");
        new Thread(() -> {
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(this, videoUri);
                String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long dms = dur != null ? Long.parseLong(dur) : 0;
                long timeUs = dms > 0 ? (dms * 1000L * frameSeek.getProgress() / 100L) : 0;
                Bitmap f = mmr.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST);
                mmr.release();
                if (f == null) throw new Exception("Sem quadro");
                RectF rect = new RectF(bounds);
                if (baseFrame != null && (f.getWidth() != baseFrame.getWidth() || f.getHeight() != baseFrame.getHeight())) {
                    float sx = (float) f.getWidth() / baseFrame.getWidth();
                    float sy = (float) f.getHeight() / baseFrame.getHeight();
                    rect = new RectF(rect.left * sx, rect.top * sy, rect.right * sx, rect.bottom * sy);
                }
                int radius = Math.max(6, intensitySeek.getProgress());
                rect = capRect(rect, f, 0.9f);
                Bitmap out = SimpleTracker.applyBlur(f, rect, radius);
                lastPreviewBitmap = out;
                handler.post(() -> {
                    previewFrames.clear();
                    previewFrames.add(out);
                    previewView.setImageBitmap(out);
                    previewSeekRef.setMax(0);
                    previewSeekRef.setProgress(0);
                    previewBtn.setText("Ver preview");
                    previewStatus.setText("✓ Prévia pronta (1 frame). Ajuste a intensidade e clique em Gerar.");
                });
            } catch (Throwable e) {
                final String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                handler.post(() -> {
                    previewBtn.setText("Ver preview");
                    previewStatus.setText("Erro na prévia: " + msg);
                });
            }
        }).start();
    }

    private void runTrackingPreview() {
        if (videoUri == null) { Toast.makeText(this, "Selecione um vídeo.", Toast.LENGTH_SHORT).show(); return; }
        RectF bounds = drawView.getDrawnBoundsInBitmap();
        if (bounds == null) { Toast.makeText(this, "Desenhe sobre a tatuagem primeiro.", Toast.LENGTH_LONG).show(); return; }
        playing = false;
        previewStatus.setText("Gerando prévia contínua...");
        generateBtn.setText("Gerando...");
        previewFrames.clear();
        frames.clear();
        previewTimesUs.clear();
        new Thread(() -> {
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(this, videoUri);
                String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long dms = dur != null ? Long.parseLong(dur) : 0;
                // denser sampling for smoother preview
                int count = 36;
                long startUs = dms > 0 ? (dms * 1000L * frameSeek.getProgress() / 100L) : 0;
                long windowMs = Math.min(4000L, Math.max(1500L, dms > 0 ? dms / 3 : 3000L));
                if (dms > 0 && startUs + windowMs * 1000L > dms * 1000L) startUs = Math.max(0, dms * 1000L - windowMs * 1000L);
                long stepUs = (windowMs * 1000L) / Math.max(1, count - 1);

                for (int i = 0; i < count; i++) {
                    long t = startUs + stepUs * i;
                    if (dms > 0) t = Math.min(t, dms * 1000L);
                    Bitmap b = (i == 0) ? mmr.getFrameAtTime(t, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) : null;
                    if (b == null) b = mmr.getFrameAtTime(t, MediaMetadataRetriever.OPTION_CLOSEST);
                    if (b != null) { frames.add(b); previewTimesUs.add(t); }
                }
                mmr.release();
                if (frames.isEmpty()) throw new Exception("Sem quadros");

                RectF rect = new RectF(bounds);
                Bitmap first = frames.get(0);
                if (baseFrame != null && (first.getWidth()!=baseFrame.getWidth() || first.getHeight()!=baseFrame.getHeight())) {
                    float sx = (float)first.getWidth()/baseFrame.getWidth();
                    float sy = (float)first.getHeight()/baseFrame.getHeight();
                    rect = new RectF(rect.left*sx, rect.top*sy, rect.right*sx, rect.bottom*sy);
                }
                int radius = Math.max(6, intensitySeek.getProgress());
                rect = capRect(rect, first, 0.9f);
                Bitmap prev = first;
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
                        generateBtn.setText("Gerar");
                        lastPreviewBitmap = previewFrames.get(0);
                        previewView.setImageBitmap(previewFrames.get(0));
                        if (previewSeekRef != null) { previewSeekRef.setMax(previewFrames.size()-1); previewSeekRef.setProgress(0); }
                        previewStatus.setText("✓ Prévia contínua pronta (" + previewFrames.size() + " quadros). Reproduza para conferir o rastreio.");
                    } else previewStatus.setText("Falha ao gerar prévia.");
                });
            } catch (Throwable e) {
                final String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                handler.post(() -> previewStatus.setText("Erro no rastreio: " + msg));
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
                long delay = 80;
                if (idx + 1 < previewTimesUs.size()) {
                    long d = (previewTimesUs.get(idx + 1) - previewTimesUs.get(idx)) / 1000L;
                    if (d >= 30 && d <= 250) delay = d;
                }
                try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
                i++;
                if (i >= previewFrames.size()) i = 0;
            }
            playing = false;
        }).start();
    }

    private void savePreviewBitmap() {
        if (lastPreviewBitmap == null) { Toast.makeText(this, "Gere uma prévia antes de salvar.", Toast.LENGTH_SHORT).show(); return; }
        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (dir == null) dir = new File(getExternalFilesDir(null), "Pictures");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "videodedit_preview_" + System.currentTimeMillis() + ".png");
            FileOutputStream fos = new FileOutputStream(f);
            lastPreviewBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Toast.makeText(this, "Salvo em: " + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout card(String title, String desc) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#151B2E"), Color.parseColor("#101827")});
        g.setCornerRadius(dp(18));
        g.setStroke(dp(1), Color.parseColor("#26324A"));
        c.setBackground(g);
        c.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        c.setLayoutParams(lp);
        TextView t = new TextView(this); t.setText(title); t.setTextColor(Color.WHITE); t.setTextSize(16); t.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        c.addView(t);
        TextView d = new TextView(this); d.setText(desc); d.setTextColor(Color.parseColor("#9DB0D0")); d.setTextSize(12); d.setPadding(0, dp(2), 0, dp(8));
        c.addView(d);
        return c;
    }
    private TextView label(String t){ TextView v=new TextView(this); v.setText(t); v.setTextColor(Color.parseColor("#E6EDF8")); v.setTextSize(13); return v; }
    private TextView small(String t){ TextView v=new TextView(this); v.setText(t); v.setTextColor(Color.parseColor("#8FA3C4")); v.setTextSize(12); return v; }
    private Button primaryButton(String t){ Button b=new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{Color.parseColor("#2563EB"), Color.parseColor("#7C3AED")}); g.setCornerRadius(dp(14));
        b.setBackground(g); b.setPadding(dp(12),dp(10),dp(12),dp(10)); return b; }
    private Button secondaryButton(String t){ Button b=new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.parseColor("#E2E8F0"));
        GradientDrawable g=bg("#243044",12); g.setStroke(dp(1), Color.parseColor("#334155")); b.setBackground(g); b.setPadding(dp(8),dp(8),dp(8),dp(8)); return b; }
    private Button accentButton(String t){ Button b=new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.parseColor("#E0F2FE"));
        GradientDrawable g=bg("#0F766E",12); b.setBackground(g); b.setPadding(dp(8),dp(8),dp(8),dp(8)); return b; }
    private SeekBar styledSeek(int max){ SeekBar s=new SeekBar(this); s.setMax(max); return s; }
    private GradientDrawable bg(String color,int r){ GradientDrawable g=new GradientDrawable(); g.setColor(Color.parseColor(color)); g.setCornerRadius(dp(r)); return g; }
    private GradientDrawable makeGradient(){ return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.parseColor("#070B18"), Color.parseColor("#0B1220"), Color.parseColor("#111827")}); }
    private int dp(int v){ return Math.round(getResources().getDisplayMetrics().density * v); }
    private SeekBar.OnSeekBarChangeListener simpleSeek(Runnable onStop){ return new SeekBar.OnSeekBarChangeListener(){ @Override public void onProgressChanged(SeekBar seekBar,int progress,boolean fromUser){} @Override public void onStartTrackingTouch(SeekBar seekBar){} @Override public void onStopTrackingTouch(SeekBar seekBar){ onStop.run(); } }; }
}
