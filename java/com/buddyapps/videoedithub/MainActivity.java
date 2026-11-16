package com.buddyapps.videoedithub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0B1220"));

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        box.setPadding(pad, dp(24), pad, pad);

        TextView title = new TextView(this);
        title.setText("VideoEdit");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Edição inteligente de vídeo: rastreie tatuagens, remova com precisão e borre o rosto preservando a boca.");
        sub.setTextColor(Color.parseColor("#94A3B8"));
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(6), 0, dp(20));
        box.addView(sub);

        box.addView(featureCard("🎯 Rastrear tatuagem", "Desenhe a área da tatuagem, rastreie automaticamente no vídeo e veja a pré-visualização.",
                "Abrir editor de tatuagem", () -> startActivity(new Intent(this, TattooTrackActivity.class)), true));

        box.addView(featureCard("🤖 IA local", "Processamento local no app com modelos incluídos para edição e borrão facial preservando a boca.",
                "Abrir IA local", () -> startActivity(new Intent(this, AiActivity.class)), false));

        box.addView(featureCard("🛠️ Ferramentas locais", "Edição manual/local no aparelho: seleção de vídeo, prévias e ajustes finos.",
                "Abrir ferramentas locais", () -> startActivity(new Intent(this, LocalActivity.class)), false));

        Button updateBtn = secondaryButton("Verificar atualização (GitHub)");
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ulp.topMargin = dp(12);
        updateBtn.setLayoutParams(ulp);
        updateBtn.setOnClickListener(v -> checkForUpdates(true));
        box.addView(updateBtn);

        TextView ver = new TextView(this);
        ver.setText("Versão " + getCurrentVersionName() + " • Atualização automática via GitHub Releases");
        ver.setTextColor(Color.parseColor("#64748B"));
        ver.setTextSize(12);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, dp(12), 0, dp(20));
        box.addView(ver);

        scroll.addView(box);
        root.addView(scroll);
        setContentView(root);

        checkForUpdates(false);
    }

    private LinearLayout featureCard(String title, String desc, String btnText, Runnable action, boolean accent) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(accent ? "#172554" : "#111827"));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.parseColor(accent ? "#2563EB" : "#1F2937"));
        c.setBackground(bg);
        int p = dp(16);
        c.setPadding(p,p,p,p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        c.setLayoutParams(lp);

        TextView t = new TextView(this);
        t.setText(title); t.setTextColor(Color.WHITE); t.setTextSize(18);
        c.addView(t);

        TextView d = new TextView(this);
        d.setText(desc); d.setTextColor(Color.parseColor("#CBD5E1")); d.setTextSize(13);
        d.setPadding(0, dp(6), 0, dp(12));
        c.addView(d);

        Button b = accent ? primaryButton(btnText) : secondaryButton(btnText);
        b.setOnClickListener(v -> action.run());
        c.addView(b);
        return c;
    }

    private Button primaryButton(String t) {
        Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        GradientDrawable g = new GradientDrawable(); g.setColor(Color.parseColor("#2563EB")); g.setCornerRadius(dp(12));
        b.setBackground(g); b.setPadding(dp(12), dp(10), dp(12), dp(10)); return b;
    }
    private Button secondaryButton(String t) {
        Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        GradientDrawable g = new GradientDrawable(); g.setColor(Color.parseColor("#334155")); g.setCornerRadius(dp(12));
        b.setBackground(g); b.setPadding(dp(12), dp(10), dp(12), dp(10)); return b;
    }
    private int dp(int v) { return Math.round(getResources().getDisplayMetrics().density * v); }

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
        try { PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0); return p.versionName != null ? p.versionName : "0"; }
        catch (Exception e) { return "0"; }
    }
    private String normalize(String v) { if (v == null) return ""; v = v.trim(); if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1); return v; }
    private boolean isNewer(String latest, String current) {
        if (latest.isEmpty()) return false; if (current.isEmpty()) return true;
        String[] a = latest.split("\\."); String[] b = current.split("\\.");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) { int ai = i < a.length ? parseIntSafe(a[i]) : 0; int bi = i < b.length ? parseIntSafe(b[i]) : 0; if (ai > bi) return true; if (ai < bi) return false; }
        return false;
    }
    private int parseIntSafe(String s) { try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); } catch (Exception e) { return 0; } }
    private String extract(String src, String start, String end) { int i = src.indexOf(start); if (i < 0) return null; i += start.length(); int j = src.indexOf(end, i); if (j < 0) return null; return src.substring(i, j); }
    private String extractApkUrl(String json) { String key = "\"browser_download_url\":\""; int idx = 0; while (true) { int i = json.indexOf(key, idx); if (i < 0) return null; i += key.length(); int j = json.indexOf("\"", i); if (j < 0) return null; String url = json.substring(i, j); if (url.endsWith(".apk")) return url; idx = j + 1; } }
    private void openUrl(String url) { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) { Toast.makeText(this, "Não foi possível abrir o link de atualização.", Toast.LENGTH_SHORT).show(); } }
}
