package com.buddyapps.videoedithub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final String REPO_API = "https://api.github.com/repos/renatozapelini/VideoEdit/releases/latest";

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
        sub.setText("Remover tatuagens (IA local) e borrar rosto mantendo a boca visível. Tudo roda no app.");
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
        aiBtn.setText("Ferramentas com IA (local)");
        aiBtn.setTextSize(16);
        aiBtn.setOnClickListener(v -> startActivity(new Intent(this, AiActivity.class)));
        box.addView(aiBtn);

        Button updateBtn = new Button(this);
        updateBtn.setText("Verificar atualização (GitHub)");
        updateBtn.setOnClickListener(v -> checkForUpdates(true));
        box.addView(updateBtn);

        TextView note = new TextView(this);
        note.setText("\nO app verifica automaticamente a versão mais recente no GitHub para manter você atualizado.");
        note.setTextSize(12);
        box.addView(note);

        scroll.addView(box);
        setContentView(scroll);

        checkForUpdates(false);
    }

    private void checkForUpdates(boolean manual) {
        if (manual) Toast.makeText(this, "Verificando atualização...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(REPO_API).openConnection();
                c.setRequestProperty("Accept", "application/vnd.github+json");
                c.setRequestProperty("User-Agent", "VideoEdit-App");
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                int code = c.getResponseCode();
                if (code != 200) throw new Exception("HTTP " + code);
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                String json = sb.toString();
                String tag = extract(json, "\"tag_name\":\"", "\"");
                String html = extract(json, "\"html_url\":\"", "\"");
                String apkUrl = extractApkUrl(json);
                if (apkUrl == null || apkUrl.isEmpty()) apkUrl = html;
                String latest = normalize(tag);
                String current = normalize(getCurrentVersionName());
                boolean newer = isNewer(latest, current);
                final String fTag = tag, fUrl = apkUrl, fHtml = html;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (newer) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Atualização disponível")
                                .setMessage("Nova versão " + fTag + " disponível no GitHub. Deseja atualizar?")
                                .setPositiveButton("Atualizar", (d, w) -> openUrl(fUrl != null ? fUrl : fHtml))
                                .setNegativeButton("Depois", null)
                                .show();
                    } else if (manual) {
                        Toast.makeText(MainActivity.this, "Você já está na versão mais recente (" + getCurrentVersionName() + ").", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                if (manual) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(MainActivity.this, "Não foi possível verificar atualização.", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private String getCurrentVersionName() {
        try {
            PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
            return p.versionName != null ? p.versionName : "0";
        } catch (Exception e) {
            return "0";
        }
    }

    private String normalize(String v) {
        if (v == null) return "";
        v = v.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        return v;
    }

    private boolean isNewer(String latest, String current) {
        if (latest.isEmpty()) return false;
        if (current.isEmpty()) return true;
        String[] a = latest.split("\\.");
        String[] b = current.split("\\.");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int ai = i < a.length ? parseIntSafe(a[i]) : 0;
            int bi = i < b.length ? parseIntSafe(b[i]) : 0;
            if (ai > bi) return true;
            if (ai < bi) return false;
        }
        return false;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); } catch (Exception e) { return 0; }
    }

    private String extract(String src, String start, String end) {
        int i = src.indexOf(start);
        if (i < 0) return null;
        i += start.length();
        int j = src.indexOf(end, i);
        if (j < 0) return null;
        return src.substring(i, j);
    }

    private String extractApkUrl(String json) {
        // find browser_download_url for an .apk asset
        String key = "\"browser_download_url\":\"";
        int idx = 0;
        while (true) {
            int i = json.indexOf(key, idx);
            if (i < 0) return null;
            i += key.length();
            int j = json.indexOf("\"", i);
            if (j < 0) return null;
            String url = json.substring(i, j);
            if (url.endsWith(".apk")) return url;
            idx = j + 1;
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir o link de atualização.", Toast.LENGTH_SHORT).show();
        }
    }
}
