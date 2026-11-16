package com.buddyapps.videoedithub;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class AiActivity extends Activity {
    private static final int PICK_VIDEO = 2001;
    private Uri selectedVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("Ferramentas com IA");
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 12);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText("Use IAs para: mostrar a tatuagem e ela ser rastreada e removida da pessoa; e aplicar borrão leve no rosto mantendo a boca visível.\n\nSelecione um vídeo e escolha uma ferramenta de IA abaixo.");
        info.setTextSize(14);
        info.setPadding(0, 0, 0, 16);
        box.addView(info);

        Button pick = new Button(this);
        pick.setText("Selecionar vídeo");
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("video/*");
            startActivityForResult(i, PICK_VIDEO);
        });
        box.addView(pick);

        addTool(box, "Remover objeto/tatuagem com IA (online)",
                "Ferramentas que permitem marcar objeto e remover com IA/inpainting (algumas com rastreio em vídeo ou por trechos).",
                "https://www.google.com/search?q=AI+video+object+removal+remove+tattoo+track+free+online");

        addTool(box, "Borrão de rosto com boca visível (IA)",
                "Ferramentas de anonimização facial que preservam expressão/boca quando possível.",
                "https://www.google.com/search?q=AI+face+blur+keep+mouth+visible+anonymize+video+free");

        addTool(box, "CapCut (máscara/rastreio e blur)",
                "Use máscara com rastreamento para seguir rosto/tatuagem; aplique blur ou adesivo/cobertura.",
                "com.lemon.lvoverseas",
                "https://play.google.com/store/apps/details?id=com.lemon.lvoverseas");

        addTool(box, "Runway / ferramentas de inpainting (web)",
                "Serviços de IA para remover objetos em vídeo (podem ter limites grátis).",
                "https://runwayml.com/");

        addTool(box, "Fotor / removedores online",
                "Removedores de objeto por IA (úteis para quadros/trechos; ver opções de vídeo).",
                "https://www.fotor.com/features/object-remover/");

        TextView tip = new TextView(this);
        tip.setText("\nComo usar: 1) Selecione o vídeo. 2) Abra a ferramenta de IA. 3) Marque/mostre a tatuagem para a IA rastrear e remover; para rosto, escolha borrão leve e ajuste para manter a boca visível. 4) Exporte o resultado.");
        tip.setTextSize(12);
        box.addView(tip);

        scroll.addView(box);
        setContentView(scroll);
    }

    private void addTool(LinearLayout box, String name, String desc, String url) {
        addTool(box, name, desc, null, url);
    }

    private void addTool(LinearLayout box, String name, String desc, String pkg, String url) {
        TextView t = new TextView(this);
        t.setText("\n" + name);
        t.setTextSize(16);
        box.addView(t);
        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextSize(13);
        box.addView(d);
        Button b = new Button(this);
        b.setText("Abrir");
        b.setOnClickListener(v -> open(pkg, url));
        box.addView(b);
    }

    private void open(String pkg, String url) {
        if (pkg != null) {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                startActivity(i);
                return;
            }
        }
        try {
            Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(web);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            selectedVideo = data.getData();
            if (selectedVideo != null) {
                try { getContentResolver().takePersistableUriPermission(selectedVideo, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
            }
            Toast.makeText(this, "Vídeo selecionado. Abra uma ferramenta de IA.", Toast.LENGTH_SHORT).show();
        }
    }
}
