package com.kuponum.app;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
        @JavascriptInterface
        public void loadFixtures(String date) {
            if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                sendError("Geçersiz tarih.");
                return;
            }
            executor.execute(() -> {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL("https://www.thesportsdb.com/api/v1/json/123/eventsday.php?d=" + date + "&s=Soccer");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(12000);
                    connection.setReadTimeout(15000);
                    connection.setRequestProperty("Accept", "application/json");
                    int code = connection.getResponseCode();
                    InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                    StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                    reader.close();
                    if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
                    final String result = body.toString();
                    runOnUiThread(() -> webView.evaluateJavascript("window.onFixturesLoaded(" + org.json.JSONObject.quote(result) + ")", null));
                } catch (Exception e) {
                    sendError(e.getMessage() == null ? "Bağlantı hatası." : e.getMessage());
                } finally {
                    if (connection != null) connection.disconnect();
                }
            });
        }
    }

    private void sendError(String message) {
        runOnUiThread(() -> webView.evaluateJavascript("window.onFixturesError(" + org.json.JSONObject.quote(message) + ")", null));
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
