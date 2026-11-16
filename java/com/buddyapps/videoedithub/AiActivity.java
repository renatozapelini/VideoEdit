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
        title.setText("IA local (no app)");
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 12);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText("Processamento por IA rodando localmente no aparelho, sem abrir sites. Selecione o vídeo e escolha a função.");
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

        Button face = new Button(this);
        face.setText("IA: borrar rosto (leve) preservando boca");
        face.setOnClickListener(v -> open("face_ai"));
        box.addView(face);

        Button tattoo = new Button(this);
        tattoo.setText("IA: rastrear e remover tatuagem");
        tattoo.setOnClickListener(v -> open("tattoo_ai"));
        box.addView(tattoo);

        TextView tip = new TextView(this);
        tip.setText("\nComo usar: selecione o vídeo, marque a área (rosto/tatuagem) quando solicitado e inicie o processamento local. Os modelos de IA são carregados do próprio app.");
        tip.setTextSize(12);
        box.addView(tip);

        scroll.addView(box);
        setContentView(scroll);
    }

    private void open(String mode) {
        if (selectedVideo == null) {
            Toast.makeText(this, "Selecione um vídeo primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, AiEditorActivity.class);
        i.putExtra("mode", mode);
        i.setData(selectedVideo);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            selectedVideo = data.getData();
            if (selectedVideo != null) {
                try { getContentResolver().takePersistableUriPermission(selectedVideo, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
            }
            Toast.makeText(this, "Vídeo selecionado.", Toast.LENGTH_SHORT).show();
        }
    }
}
