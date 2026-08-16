package com.parkjuyong.rebarcalc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQUEST_OPEN = 1001;
    private static final int REQUEST_SAVE = 1002;
    private static final String PREFS = "rebar_project";
    private static final String KEY_STATE = "state_json";
    private static final String KEY_NAME = "project_name";
    private static final String KEY_URI = "project_uri";

    private WebView webView;
    private SharedPreferences preferences;
    private String pendingRequestId;
    private String pendingSaveState;
    private String pendingSaveName;
    private boolean launchedFromDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        importLaunchDocument(getIntent());

        webView = findViewById(R.id.webview);
        applySystemBarInsets(webView);
        configureWebView();
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void applySystemBarInsets(View view) {
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("file".equals(uri.getScheme()) && "android_asset".equals(uri.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { }
                return true;
            }
        });
    }

    private void importLaunchDocument(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction()) || intent.getData() == null) return;
        try {
            Uri uri = intent.getData();
            String state = readUri(uri);
            new JSONObject(state);
            String name = queryDisplayName(uri, "불러온 프로젝트.rebar");
            preferences.edit().putString(KEY_STATE, state).putString(KEY_NAME, name)
                    .putString(KEY_URI, uri.toString()).apply();
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION); }
            catch (Exception ignored) { }
            launchedFromDocument = true;
        } catch (Exception ignored) { }
    }

    private String queryDisplayName(Uri uri, String fallback) {
        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return fallback;
    }

    private String readUri(Uri uri) throws Exception {
        try (InputStream input = getContentResolver().openInputStream(uri); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IllegalStateException("파일을 열 수 없습니다.");
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private void writeUri(Uri uri, String state) throws Exception {
        try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
            if (output == null) throw new IllegalStateException("파일을 저장할 수 없습니다.");
            output.write(state.getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
    }

    private void resolveNative(String requestId, JSONObject result) {
        String script = "window.__resolveAndroidRequest(" + JSONObject.quote(requestId) + "," + JSONObject.quote(result.toString()) + ")";
        runOnUiThread(() -> webView.evaluateJavascript(script, null));
    }

    private void requestOpen(String requestId) {
        pendingRequestId = requestId;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/octet-stream", "application/json", "text/plain"});
        startActivityForResult(intent, REQUEST_OPEN);
    }

    private void requestSave(String requestId, JSONObject args) {
        pendingRequestId = requestId;
        pendingSaveState = args.optString("stateJson", "{}");
        pendingSaveName = safeFileName(args.optString("suggestedName", "철근가공물량"));
        boolean saveAs = args.optBoolean("saveAs", false);
        String savedUri = preferences.getString(KEY_URI, null);
        if (!saveAs && savedUri != null) {
            try {
                Uri uri = Uri.parse(savedUri);
                writeUri(uri, pendingSaveState);
                storeCurrentProject(pendingSaveState, pendingSaveName, uri);
                resolveNative(requestId, new JSONObject().put("ok", true).put("path", uri.toString()).put("name", pendingSaveName));
                clearPending();
                return;
            } catch (Exception ignored) { }
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, pendingSaveName);
        startActivityForResult(intent, REQUEST_SAVE);
    }

    private String safeFileName(String name) {
        String cleaned = name == null ? "철근가공물량" : name.trim().replaceAll("[\\/:*?\"<>|]", "_");
        if (cleaned.isEmpty()) cleaned = "철근가공물량";
        if (!cleaned.toLowerCase().endsWith(".rebar")) cleaned += ".rebar";
        return cleaned;
    }

    private void storeCurrentProject(String state, String name, Uri uri) {
        SharedPreferences.Editor editor = preferences.edit().putString(KEY_STATE, state).putString(KEY_NAME, name);
        if (uri != null) editor.putString(KEY_URI, uri.toString());
        editor.apply();
    }

    private void clearPending() {
        pendingRequestId = null;
        pendingSaveState = null;
        pendingSaveName = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String requestId = pendingRequestId;
        if (requestId == null) return;
        try {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) {
                resolveNative(requestId, new JSONObject().put("ok", false).put("cancelled", true));
                clearPending();
                return;
            }
            Uri uri = data.getData();
            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try { getContentResolver().takePersistableUriPermission(uri, flags); } catch (Exception ignored) { }
            if (requestCode == REQUEST_OPEN) {
                String state = readUri(uri);
                JSONObject parsed = new JSONObject(state);
                String name = queryDisplayName(uri, "불러온 프로젝트.rebar");
                storeCurrentProject(state, name, uri);
                resolveNative(requestId, new JSONObject().put("ok", true).put("path", uri.toString()).put("name", name).put("state", parsed));
            } else if (requestCode == REQUEST_SAVE) {
                writeUri(uri, pendingSaveState);
                String name = queryDisplayName(uri, pendingSaveName);
                storeCurrentProject(pendingSaveState, name, uri);
                resolveNative(requestId, new JSONObject().put("ok", true).put("path", uri.toString()).put("name", name));
            }
        } catch (Exception error) {
            try { resolveNative(requestId, new JSONObject().put("ok", false).put("error", error.getMessage())); }
            catch (Exception ignored) { }
        } finally {
            clearPending();
        }
    }

    @Override
    public void onBackPressed() {
        webView.evaluateJavascript("window.androidHandleBack ? window.androidHandleBack() : false", value -> {
            if (!"true".equals(value)) MainActivity.super.onBackPressed();
        });
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidBridge");
            webView.destroy();
        }
        super.onDestroy();
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void printPage() {
            runOnUiThread(() -> {
                PrintManager manager = (PrintManager) getSystemService(PRINT_SERVICE);
                if (manager != null) {
                    manager.print("철근가공물량", webView.createPrintDocumentAdapter("철근가공물량"),
                            new PrintAttributes.Builder().build());
                }
            });
        }

        @JavascriptInterface
        public void updateState(String stateJson, boolean dirty) {
            preferences.edit().putString(KEY_STATE, stateJson).apply();
        }

        @JavascriptInterface
        public void beginNewProject() {
            preferences.edit().remove(KEY_URI).remove(KEY_NAME).apply();
        }

        @JavascriptInterface
        public String loadLastProject() {
            try {
                String state = preferences.getString(KEY_STATE, null);
                if (state == null || state.isEmpty()) return new JSONObject().put("ok", false).put("not_found", true).toString();
                JSONObject result = new JSONObject().put("ok", true)
                        .put("path", preferences.getString(KEY_URI, "internal://autosave"))
                        .put("name", preferences.getString(KEY_NAME, "자동 저장 프로젝트.rebar"))
                        .put("state", new JSONObject(state));
                if (launchedFromDocument) {
                    result.put("launched", true);
                    launchedFromDocument = false;
                }
                return result.toString();
            } catch (Exception error) {
                try { return new JSONObject().put("ok", false).put("error", error.getMessage()).toString(); }
                catch (Exception ignored) { return "{\"ok\":false}"; }
            }
        }

        @JavascriptInterface
        public void request(String method, String requestId, String argsJson) {
            runOnUiThread(() -> {
                try {
                    JSONObject args = new JSONObject(argsJson);
                    if ("save".equals(method)) requestSave(requestId, args);
                    else if ("open".equals(method)) requestOpen(requestId);
                    else resolveNative(requestId, new JSONObject().put("ok", false).put("error", "지원하지 않는 요청입니다."));
                } catch (Exception error) {
                    try { resolveNative(requestId, new JSONObject().put("ok", false).put("error", error.getMessage())); }
                    catch (Exception ignored) { }
                }
            });
        }
    }
}
