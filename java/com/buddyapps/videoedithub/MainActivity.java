package com.buddyapps.videoedithub;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("VideoEdit — Edição por IA e Local");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 8);
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Remover tatuagens (rastreio por IA) e borrar rosto mantendo a boca visível.");
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, 20);
        box.addView(sub);

        Button localBtn = new Button(this);
        localBtn.setText("Ferramentas locais");
        localBtn.setTextSize(16);
        localBtn.setOnClickListener(v -> startActivity(new Intent(this, LocalActivity.class)));
        box.addView(localBtn);

        Button aiBtn = new Button(this);
        aiBtn.setText("Ferramentas com IA");
        aiBtn.setTextSize(16);
        aiBtn.setOnClickListener(v -> startActivity(new Intent(this, AiActivity.class)));
        box.addView(aiBtn);

        TextView note = new TextView(this);
        note.setText("\nObservação: rastrear e remover tatuagem automaticamente em vídeo e borrar rosto preservando a boca exigem modelos de IA. Este app reúne fluxos locais e com IA para ajudar nessas tarefas.");
        note.setTextSize(12);
        box.addView(note);

        scroll.addView(box);
        setContentView(scroll);
    }
}
