package com.buddyapps.videoedithub;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class LocalActivity extends Activity {
    private static final int PICK_VIDEO = 1001;
    private Uri selectedVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("Ferramentas locais");
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 12);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText("Processamento local no aparelho (sem enviar vídeo). Ideal para privacidade. \n\n• Rosto: aplicar borrão leve na região do rosto (ajuste manual) tentando preservar a boca.\n• Tatuagem: marcação manual da área para mascarar/borrar quadro a quadro (apoio), pois rastreio/remoção automática exige modelos de IA.");
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

        Button blurFace = new Button(this);
        blurFace.setText("Borrar rosto (leve, preservar boca) — ajuste manual");
        blurFace.setOnClickListener(v -> openEditor("face"));
        box.addView(blurFace);

        Button hideTattoo = new Button(this);
        hideTattoo.setText("Cobrir/remover tatuagem (marcação manual)");
        hideTattoo.setOnClickListener(v -> openEditor("tattoo"));
        box.addView(hideTattoo);

        Button openExternal = new Button(this);
        openExternal.setText("Abrir editor local (galeria/apps instalados)");
        openExternal.setOnClickListener(v -> openExternalEditor());
        box.addView(openExternal);

        TextView hint = new TextView(this);
        hint.setText("\nDica: para resultado automático (rastrear tatuagem e tirar; borrão com boca visível) use a seção 'Ferramentas com IA'.");
        hint.setTextSize(12);
        box.addView(hint);

        scroll.addView(box);
        setContentView(scroll);
    }

    private void openEditor(String mode) {
        if (selectedVideo == null) {
            Toast.makeText(this, "Selecione um vídeo primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, LocalEditorActivity.class);
        i.putExtra("mode", mode);
        i.setData(selectedVideo);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(i);
    }

    private void openExternalEditor() {
        Intent i = new Intent(Intent.ACTION_EDIT);
        i.setType("video/*");
        if (selectedVideo != null) {
            i.setData(selectedVideo);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        try {
            startActivity(Intent.createChooser(i, "Abrir editor"));
        } catch (Exception e) {
            Toast.makeText(this, "Nenhum editor de vídeo encontrado.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            selectedVideo = data.getData();
            if (selectedVideo != null) {
                try {
                    getContentResolver().takePersistableUriPermission(selectedVideo, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
            }
            Toast.makeText(this, "Vídeo selecionado.", Toast.LENGTH_SHORT).show();
        }
    }
}
