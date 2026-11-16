package com.buddyapps.videoedithub;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String CAPCUT_PKG = "com.lemon.lvoverseas";
    private static final String INSHOT_PKG = "com.cameraside.instagram_collage_maker_editor"; // InShot (pode variar)
    private static final String VN_PKG = "com.vlog.editor"; // VN (pode variar)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("Vídeo Edita Hub");
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 8);
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Reúne as melhores ferramentas grátis para editar vídeo, desfocar rosto e remover objetos.");
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, 16);
        box.addView(sub);

        Button btnCapcut = btn("CapCut — desfocar / mascarar rosto", CAPCUT_PKG,
                "https://play.google.com/store/apps/details?id=com.lemon.lvoverseas");
        Button btnInshot = btn("InShot — blur / mosaico no rosto", INSHOT_PKG,
                "https://play.google.com/store/apps/details?id=com.cameraside.instagram_collage_maker_editor");
        Button btnVn = btn("VN Video Editor", VN_PKG,
                "https://play.google.com/store/apps/details?id=com.vlog.editor");
        Button btnRemover = btn("Removedor de objeto / tatuagem (online)", null,
                "https://www.google.com/search?q=removedor+de+objeto+video+IA+gratis+online");
        Button btnInstrucoes = btn("Passo a passo de edição", null, null);

        btnInstrucoes.setOnClickListener(v -> mostrarInstrucoes(box));

        box.addView(btnCapcut);
        box.addView(btnInshot);
        box.addView(btnVn);
        box.addView(btnRemover);
        box.addView(btnInstrucoes);

        TextView aviso = new TextView(this);
        aviso.setText("Aviso: este app é um hub. Ele reúne e abre ferramentas grátis; o processamento de vídeo acontece em cada ferramenta, não aqui.");
        aviso.setTextSize(12);
        aviso.setPadding(0, 16, 0, 0);
        box.addView(aviso);

        scroll.addView(box);
        root.addView(scroll);
        setContentView(root);
    }

    private Button btn(String texto, String pacote, String url) {
        Button b = new Button(this);
        b.setText(texto);
        b.setTextSize(14);
        b.setPadding(12, 12, 12, 12);
        b.setOnClickListener(v -> abrir(pacote, url));
        return b;
    }

    private void abrir(String pacote, String url) {
        if (pacote != null) {
            Intent i = getPackageManager().getLaunchIntentForPackage(pacote);
            if (i != null) {
                startActivity(i);
                return;
            }
        }
        Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(web);
    }

    private void mostrarInstrucoes(LinearLayout box) {
        // remove instruções anteriores se existirem
        for (int i = 0; i < box.getChildCount(); i++) {
            View v = box.getChildAt(i);
            if (v instanceof TextView && "instrucoes".equals(v.getTag())) {
                box.removeViewAt(i);
                return;
            }
        }
        TextView t = new TextView(this);
        t.setTag("instrucoes");
        t.setTextSize(13);
        t.setPadding(0, 12, 0, 0);
        t.setText(
            "Como fazer:\n\n"
            + "1. Abra o vídeo no CapCut, InShot ou VN.\n"
            + "2. Para esconder o rosto: use máscara/desfoque (blur) com rastreamento de rosto, segue automaticamente.\n"
            + "3. Para remover tatuagem: use \"remover objeto\" ou máscara quadro a quadro (clone/estampa) cobrindo a tatuagem em cada frame.\n"
            + "4. Exporte e salve.\n\n"
            + "Aviso: remoção de tatuagem em vídeo ainda é difícil de fazer 100% automático e grátis; costuma exigir edição manual quadro a quadro."
        );
        box.addView(t);
    }
}
