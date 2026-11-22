package com.kuponum.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private WebView webView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new FixtureBridge(), "Android");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private class FixtureBridge {
        @JavascriptInterface public void loadResults(String date) { loadEvents(date, true); }
        @JavascriptInterface public void loadFixtures(String date) { loadEvents(date, false); }

        private void loadEvents(String date, boolean results) {
            if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                sendError("Geçersiz tarih."); return;
            }
            executor.execute(() -> {
                HttpURLConnection connection = null;
                try {
                    String[] p = date.split("-");
                    String trDate = p[2] + "/" + p[1] + "/" + p[0];
                    String endpoint = "https://vd.mackolik.com/livedata?date=" +
                        URLEncoder.encode(trDate, "UTF-8") + "&_=" + System.currentTimeMillis();
                    connection = (HttpURLConnection) new URL(endpoint).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(20000);
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    int code = connection.getResponseCode();
                    InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                    StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                    reader.close();
                    if (code < 200 || code >= 300) throw new Exception("Maçkolik bağlantısı HTTP " + code);

                    JSONObject source = new JSONObject(body.toString());
                    JSONArray raw = source.optJSONArray("m");
                    JSONArray events = new JSONArray();
                    if (raw != null) {
                        for (int i = 0; i < raw.length(); i++) {
                            JSONArray m = raw.optJSONArray(i);
                            if (m == null || m.length() < 17) continue;
                            JSONArray group = m.optJSONArray(36);
                            if (group != null && group.optInt(11, 1) != 1) continue;
                            String status = m.optString(6, "");
                            boolean finished = "MS".equalsIgnoreCase(status) || "Bitti".equalsIgnoreCase(status);
                            if (results && !finished) continue;
                            JSONObject e = new JSONObject();
                            e.put("idEvent", m.optString(14, m.optString(0, "")));
                            e.put("strHomeTeam", m.optString(2, "Ev sahibi"));
                            e.put("strAwayTeam", m.optString(4, "Deplasman"));
                            e.put("strLeague", group == null ? "Futbol" : group.optString(1, "Futbol"));
                            e.put("dateEvent", date);
                            e.put("strTime", m.optString(16, ""));
                            e.put("intHomeScore", m.optString(12, ""));
                            e.put("intAwayScore", m.optString(13, ""));
                            e.put("strStatus", status);
                            e.put("strProgress", status);
                            events.put(e);
                        }
                    }
                    JSONObject output = new JSONObject();
                    output.put("events", events);
                    final String result = output.toString();
                    runOnUiThread(() -> webView.evaluateJavascript(
                        (results ? "window.onResultsLoaded(" : "window.onFixturesLoaded(") +
                        JSONObject.quote(result) + ")", null));
                } catch (Exception e) {
                    final String error = e.getMessage() == null ? "Maçkolik veri bağlantısı başarısız." : e.getMessage();
                    runOnUiThread(() -> webView.evaluateJavascript(
                        (results ? "window.onResultsError(" : "window.onFixturesError(") +
                        JSONObject.quote(error) + ")", null));
                } finally {
                    if (connection != null) connection.disconnect();
                }
            });
        }
    }

    private void sendError(String message) {
        runOnUiThread(() -> webView.evaluateJavascript("window.onFixturesError(" + JSONObject.quote(message) + ")", null));
    }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        executor.shutdownNow(); super.onDestroy();
    }
}