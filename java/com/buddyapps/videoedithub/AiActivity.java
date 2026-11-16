package com.buddyapps.videoedithub;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
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

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0B1220"));
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        box.setPadding(pad, pad, pad, pad);

        TextView title = header("IA local no app");
        box.addView(title);
        TextView info = sub("Processamento local no aparelho com modelos incluídos. Sem links externos.");
        box.addView(info);

        LinearLayout card = card();
        Button pick = primary("Selecionar vídeo");
        pick.setOnClickListener(v -> pickVideo());
        card.addView(pick);
        box.addView(card);

        box.addView(actionCard("Rastrear e remover tatuagem", "Desenhe onde está a tatuagem, o app reconhece e segue a região no vídeo com pré-visualização.",
                "Abrir rastreio de tatuagem", () -> startActivity(new Intent(this, TattooTrackActivity.class))));

        box.addView(actionCard("Borrar rosto preservando boca", "Aplica borrão leve no rosto mantendo a região da boca visível (processamento local).",
                "Abrir editor facial", () -> open("face_ai")));

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

    private void open(String mode) {
        if (selectedVideo == null) { Toast.makeText(this, "Selecione um vídeo primeiro.", Toast.LENGTH_SHORT).show(); return; }
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
            if (selectedVideo != null) try { getContentResolver().takePersistableUriPermission(selectedVideo, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
            Toast.makeText(this, "Vídeo selecionado.", Toast.LENGTH_SHORT).show();
        }
    }

    private TextView header(String t){ TextView v=new TextView(this); v.setText(t); v.setTextColor(Color.WHITE); v.setTextSize(22); v.setGravity(Gravity.CENTER); v.setPadding(0,0,0,dp(6)); return v; }
    private TextView sub(String t){ TextView v=new TextView(this); v.setText(t); v.setTextColor(Color.parseColor("#94A3B8")); v.setTextSize(14); v.setGravity(Gravity.CENTER); v.setPadding(0,0,0,dp(14)); return v; }
    private LinearLayout card(){ LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setBackground(bg("#111827",16)); c.setPadding(dp(14),dp(14),dp(14),dp(14)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.bottomMargin=dp(12); c.setLayoutParams(lp); return c; }
    private LinearLayout actionCard(String title,String desc,String btn, Runnable r){ LinearLayout c=card(); TextView t=new TextView(this); t.setText(title); t.setTextColor(Color.WHITE); t.setTextSize(17); c.addView(t); TextView d=new TextView(this); d.setText(desc); d.setTextColor(Color.parseColor("#CBD5E1")); d.setTextSize(13); d.setPadding(0,dp(6),0,dp(10)); c.addView(d); Button b=primary(btn); b.setOnClickListener(v->r.run()); c.addView(b); return c; }
    private Button primary(String t){ Button b=new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setBackground(bg("#2563EB",12)); b.setPadding(dp(12),dp(10),dp(12),dp(10)); return b; }
    private GradientDrawable bg(String color,int r){ GradientDrawable g=new GradientDrawable(); g.setColor(Color.parseColor(color)); g.setCornerRadius(dp(r)); return g; }
    private int dp(int v){ return Math.round(getResources().getDisplayMetrics().density * v); }
}
