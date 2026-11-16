package com.buddyapps.videoedithub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
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
    private boolean updatePromptShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#060816"), Color.parseColor("#0B1220"), Color.parseColor("#0F172A")}));

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        box.setPadding(pad, dp(28), pad, pad);

        TextView title = new TextView(this);
        title.setText("VideoEdit");
        title.setTextColor(Color.WHITE);
        title.setTextSize(32);
        title.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Estúdio inteligente no seu celular");
        sub.setTextColor(Color.parseColor("#A5B4FC"));
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(4), 0, dp(6));
        box.addView(sub);

        TextView desc = new TextView(this);
        desc.setText("Desenhe a tatuagem, dê zoom, rastreie no vídeo e veja a pré-visualização. Borrão facial preservando a boca, tudo local no app.");
        desc.setTextColor(Color.parseColor("#9DB0D0"));
        desc.setTextSize(13);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, 0, 0, dp(22));
        box.addView(desc);

        box.addView(featureCard("✏️  Editor de tatuagem", "Marque a tatuagem com o dedo, use zoom para precisão, rastreie quadro a quadro e confira a pré-visualização.",
                "Abrir editor", () -> startActivity(new Intent(this, TattooTrackActivity.class)),
                new int[]{Color.parseColor("#1E1B4B"), Color.parseColor("#172554")}, Color.parseColor("#6366F1")));

        box.addView(featureCard("🤖  IA local", "Processamento no aparelho com modelos incluídos, sem abrir sites.",
                "Abrir IA local", () -> startActivity(new Intent(this, AiActivity.class)),
                new int[]{Color.parseColor("#111827"), Color.parseColor("#0F172A")}, Color.parseColor("#334155")));

        box.addView(featureCard("🛠️  Ferramentas locais", "Seleção de vídeo, prévias e ajustes manuais direto no dispositivo.",
                "Abrir ferramentas", () -> startActivity(new Intent(this, LocalActivity.class)),
                new int[]{Color.parseColor("#111827"), Color.parseColor("#0F172A")}, Color.parseColor("#334155")));

        Button updateBtn = secondaryButton("Verificar atualização");
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ulp.topMargin = dp(14);
        updateBtn.setLayoutParams(ulp);
        updateBtn.setOnClickListener(v -> checkForUpdates(true));
        box.addView(updateBtn);

        TextView ver = new TextView(this);
        ver.setText("Versão instalada: " + getCurrentVersionName() + "  •  Código " + getCurrentVersionCode() + "\nAtualizações automáticas via GitHub Releases");
        ver.setTextColor(Color.parseColor("#7C8CA8"));
        ver.setTextSize(12);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, dp(14), 0, dp(24));
        box.addView(ver);

        scroll.addView(box);
        root.addView(scroll);
        setContentView(root);

        checkForUpdates(false);
    }

    private LinearLayout featureCard(String title, String desc, String btnText, Runnable action, int[] colors, int stroke) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        bg.setCornerRadius(dp(22));
        bg.setStroke(dp(1), stroke);
        c.setBackground(bg);
        int p = dp(18);
        c.setPadding(p,p,p,p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        c.setLayoutParams(lp);

        TextView t = new TextView(this);
        t.setText(title); t.setTextColor(Color.WHITE); t.setTextSize(19); t.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        c.addView(t);

        TextView d = new TextView(this);
        d.setText(desc); d.setTextColor(Color.parseColor("#C6D3E6")); d.setTextSize(13);
        d.setPadding(0, dp(8), 0, dp(14));
        c.addView(d);

        Button b = stroke == Color.parseColor("#6366F1") ? primaryButton(btnText) : secondaryButton(btnText);
        b.setOnClickListener(v -> action.run());
        c.addView(b);
        return c;
    }

    private Button primaryButton(String t) {
        Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{Color.parseColor("#2563EB"), Color.parseColor("#7C3AED")}); g.setCornerRadius(dp(16));
        b.setBackground(g); b.setPadding(dp(14), dp(12), dp(14), dp(12)); return b;
    }
    private Button secondaryButton(String t) {
        Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        GradientDrawable g = new GradientDrawable(); g.setColor(Color.parseColor("#334155")); g.setCornerRadius(dp(16)); g.setStroke(dp(1), Color.parseColor("#475569"));
        b.setBackground(g); b.setPadding(dp(14), dp(12), dp(14), dp(12)); return b;
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

                String currentRaw = getCurrentVersionName();
                String latestNorm = normalize(tag);
                String currentNorm = normalize(currentRaw);
                // Consider up-to-date if same or if current is newer/equal by comparison
                boolean hasUpdate = isNewer(latestNorm, currentNorm);

                final String fTag = tag != null ? tag : latestNorm;
                final String fUrl = apkUrl, fHtml = html;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (hasUpdate) {
                        if (!manual && updatePromptShown) return;
                        updatePromptShown = true;
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Atualização disponível")
                                .setMessage("Instalada: " + currentRaw + " (código " + getCurrentVersionCode() + ")\nDisponível: " + fTag + "\n\nDeseja atualizar?")
                                .setPositiveButton("Atualizar", (d, w) -> openUrl(fUrl != null ? fUrl : fHtml))
                                .setNegativeButton("Depois", null)
                                .show();
                    } else {
                        if (manual) {
                            Toast.makeText(MainActivity.this, "Você já está na última versão: " + currentRaw + " (código " + getCurrentVersionCode() + ").", Toast.LENGTH_LONG).show();
                        }
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
    private long getCurrentVersionCode() {
        try {
            PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (android.os.Build.VERSION.SDK_INT >= 28) return p.getLongVersionCode();
            return p.versionCode;
        } catch (Exception e) { return 0; }
    }
    private String normalize(String v) {
        if (v == null) return "";
        v = v.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        // keep digits and dots only
        v = v.replaceAll("[^0-9.]", "");
        // trim trailing dots
        while (v.endsWith(".")) v = v.substring(0, v.length()-1);
        return v;
    }
    private boolean isNewer(String latest, String current) {
        if (latest == null || latest.isEmpty()) return false;
        if (current == null || current.isEmpty()) return true;
        if (latest.equals(current)) return false;
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
        if (s == null || s.isEmpty()) return 0;
        try {
            String d = s.replaceAll("[^0-9]", "");
            if (d.isEmpty()) return 0;
            return Integer.parseInt(d);
        } catch (Exception e) { return 0; }
    }
    private String extract(String src, String start, String end) { int i = src.indexOf(start); if (i < 0) return null; i += start.length(); int j = src.indexOf(end, i); if (j < 0) return null; return src.substring(i, j); }
    private String extractApkUrl(String json) { String key = "\"browser_download_url\":\""; int idx = 0; while (true) { int i = json.indexOf(key, idx); if (i < 0) return null; i += key.length(); int j = json.indexOf("\"", i); if (j < 0) return null; String url = json.substring(i, j); if (url.endsWith(".apk")) return url; idx = j + 1; } }
    private void openUrl(String url) { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) { Toast.makeText(this, "Não foi possível abrir o link de atualização.", Toast.LENGTH_SHORT).show(); } }
}
