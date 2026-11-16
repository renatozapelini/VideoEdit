package com.buddyapps.videoedithub;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class LocalEditorActivity extends Activity {

    private ImageView preview;
    private SeekBar blurSeek;
    private SeekBar mouthKeepSeek;
    private TextView status;
    private Uri videoUri;
    private String mode;
    private Bitmap frame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = getIntent().getStringExtra("mode");
        videoUri = getIntent().getData();

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16, 16, 16, 16);

        TextView title = new TextView(this);
        title.setText("face".equals(mode) ? "Borrão no rosto (preservar boca)" : "Cobrir tatuagem (manual)");
        title.setTextSize(18);
        title.setPadding(0,0,0,8);
        box.addView(title);

        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setMinimumHeight(400);
        box.addView(preview);

        status = new TextView(this);
        status.setText("Carregando prévia do vídeo...");
        status.setPadding(0,8,0,8);
        box.addView(status);

        TextView blurLabel = new TextView(this);
        blurLabel.setText("Intensidade do borrão (leve recomendado):");
        box.addView(blurLabel);
        blurSeek = new SeekBar(this);
        blurSeek.setMax(25);
        blurSeek.setProgress(8);
        box.addView(blurSeek);

        if ("face".equals(mode)) {
            TextView mouthLabel = new TextView(this);
            mouthLabel.setText("Área inferior preservada (boca/região) — maior = mais boca visível:");
            box.addView(mouthLabel);
            mouthKeepSeek = new SeekBar(this);
            mouthKeepSeek.setMax(60);
            mouthKeepSeek.setProgress(25);
            box.addView(mouthKeepSeek);
        }

        Button apply = new Button(this);
        apply.setText("Aplicar prévia no quadro");
        apply.setOnClickListener(v -> applyPreview());
        box.addView(apply);

        Button export = new Button(this);
        export.setText("Exportar vídeo (local)");
        export.setOnClickListener(v -> Toast.makeText(this,
                "Exportação de vídeo com processamento local completo requer codecs/modelos avançados. Use um editor local instalado ou a opção com IA para rastreio automático.",
                Toast.LENGTH_LONG).show());
        box.addView(export);

        TextView note = new TextView(this);
        note.setTextSize(12);
        if ("face".equals(mode)) {
            note.setText("\nNota: sem modelo de detecção facial local, o borrão é aplicado na região central-superior e a parte inferior indicada é preservada para manter a boca visível. Para detecção automática use 'Ferramentas com IA'.");
        } else {
            note.setText("\nNota: rastrear e remover tatuagem automaticamente exige IA. Aqui você pode marcar/cobrir manualmente na prévia; para remoção com rastreio use 'Ferramentas com IA'.");
        }
        box.addView(note);

        scroll.addView(box);
        setContentView(scroll);

        loadFrame();
    }

    private void loadFrame() {
        if (videoUri == null) {
            status.setText("Nenhum vídeo selecionado.");
            return;
        }
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(this, videoUri);
            frame = mmr.getFrameAtTime(0);
            mmr.release();
            if (frame != null) {
                preview.setImageBitmap(frame);
                status.setText("Prévia carregada (primeiro quadro). Ajuste e aplique.");
            } else {
                status.setText("Não foi possível ler o quadro do vídeo.");
            }
        } catch (Exception e) {
            status.setText("Erro ao abrir vídeo: " + e.getMessage());
        }
    }

    private void applyPreview() {
        if (frame == null) {
            Toast.makeText(this, "Sem quadro para editar.", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap work = frame.copy(Bitmap.Config.ARGB_8888, true);
        int w = work.getWidth();
        int h = work.getHeight();
        int radius = Math.max(1, blurSeek.getProgress());
        if ("face".equals(mode)) {
            // approximate face region: upper-center; keep bottom part for mouth
            int keepPercent = mouthKeepSeek != null ? mouthKeepSeek.getProgress() : 25; // percent of region height to keep at bottom
            int rx = (int)(w * 0.2);
            int ry = (int)(h * 0.08);
            int rw = (int)(w * 0.6);
            int rh = (int)(h * 0.45);
            int keepH = rh * keepPercent / 100;
            int blurBottom = ry + rh - keepH;
            boxBlurRegion(work, rx, ry, rw, Math.max(1, blurBottom - ry), radius);
            status.setText("Borrão leve aplicado na região do rosto (parte inferior preservada para a boca).");
        } else {
            // tattoo manual approx: center area as placeholder mask
            int rx = (int)(w * 0.3);
            int ry = (int)(h * 0.35);
            int rw = (int)(w * 0.4);
            int rh = (int)(h * 0.25);
            boxBlurRegion(work, rx, ry, rw, rh, Math.max(6, radius));
            status.setText("Área central coberta/borrada como marcação manual (ajuste fino e rastreio automático via IA).");
        }
        preview.setImageBitmap(work);
    }

    // Simple box blur on a region (pure Java, no external libs)
    private void boxBlurRegion(Bitmap bmp, int rx, int ry, int rw, int rh, int radius) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        int x1 = Math.max(0, rx), y1 = Math.max(0, ry);
        int x2 = Math.min(w, rx + rw), y2 = Math.min(h, ry + rh);
        if (x2 <= x1 || y2 <= y1) return;
        int[] pixels = new int[w * h];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);
        int[] out = pixels.clone();
        int r = Math.max(1, Math.min(radius, 20));
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                long sr=0, sg=0, sb=0, sa=0; int c=0;
                for (int dy = -r; dy <= r; dy++) {
                    int yy = y + dy; if (yy < y1 || yy >= y2) continue;
                    for (int dx = -r; dx <= r; dx++) {
                        int xx = x + dx; if (xx < x1 || xx >= x2) continue;
                        int p = pixels[yy * w + xx];
                        sa += (p >>> 24) & 0xff; sr += (p >> 16) & 0xff; sg += (p >> 8) & 0xff; sb += p & 0xff; c++;
                    }
                }
                if (c > 0) out[y*w + x] = ((int)(sa/c)<<24) | ((int)(sr/c)<<16) | ((int)(sg/c)<<8) | (int)(sb/c);
            }
        }
        bmp.setPixels(out, 0, w, 0, 0, w, h);
    }
}
