package com.jimliddle.pocketdocs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.documentfile.provider.DocumentFile;
import com.tom_roush.fontbox.ttf.NamingTable;
import fi.iki.elonen.NanoHTTPD;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.i18n.TextBundle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes3.dex */
public final class MainActivity extends Activity {
    private LinearLayout column;
    private TextView counts;
    private LinearLayout folders;
    private Button refresh;
    private TextView state;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String folderSnapshot = "";
    private final Runnable tick = new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity.1
        @Override // java.lang.Runnable
        public void run() {
            MainActivity.this.updateStatus();
            MainActivity.this.handler.postDelayed(this, 1500L);
        }
    };

    private Button button(String str, final Runnable runnable) {
        Button button = new Button(this);
        button.setText(str);
        button.setAllCaps(false);
        button.setOnClickListener(new View.OnClickListener() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda17
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                runnable.run();
            }
        });
        this.column.addView(button, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    private void confirm(String str, String str2, final Runnable runnable) {
        new AlertDialog.Builder(this).setTitle(str).setMessage(str2).setNegativeButton("Cancel", (DialogInterface.OnClickListener) null).setPositiveButton("Continue", new DialogInterface.OnClickListener() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                runnable.run();
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void connectionTest() throws IOException {
        try {
            rpc("ping", new JSONObject(), false);
            if (!rpc("initialize", new JSONObject().put("protocolVersion", "2025-03-26"), true).has("result")) {
                throw new IOException("Initialize failed");
            }
            if (rpc("tools/list", new JSONObject(), true).getJSONObject("result").getJSONArray("tools").length() != 3) {
                throw new IOException("Tool discovery failed");
            }
            if (rpc("tools/call", new JSONObject().put(NamingTable.TAG, "index_status").put("arguments", new JSONObject()), true).getJSONObject("result").optBoolean("isError")) {
                throw new IOException("Status failed");
            }
            show("Local MCP test", "PASS: authentication, initialization, three tools and index_status. Now connect Edge Gallery.");
        } catch (Exception e) {
            show("Local MCP test", "Failed: " + e.getMessage() + "\nStart the server and try again.");
        }
    }

    private void copy(String str, String str2, boolean z) {
        ClipData clipDataNewPlainText = ClipData.newPlainText(str, str2);
        if (z && Build.VERSION.SDK_INT >= 33) {
            PersistableBundle persistableBundle = new PersistableBundle();
            persistableBundle.putBoolean("android.content.extra.IS_SENSITIVE", true);
            clipDataNewPlainText.getDescription().setExtras(persistableBundle);
        }
        ((ClipboardManager) getSystemService(ClipboardManager.class)).setPrimaryClip(clipDataNewPlainText);
        toast("Copied");
    }

    private int dp(int i) {
        return Math.round(i * getResources().getDisplayMetrics().density);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void embeddingTest() {
        try {
            EmbeddingEngine embeddingEngine = EmbeddingEngine.get(this);
            long jNanoTime = System.nanoTime();
            float[] fArrEmbed = embeddingEngine.embed("Can I get this repaired for free?");
            double dCosine = HybridRanker.cosine(fArrEmbed, HybridRanker.encode(embeddingEngine.embed("The warranty covers defects. Parts and labour are supplied at no charge.")));
            double dCosine2 = HybridRanker.cosine(fArrEmbed, HybridRanker.encode(embeddingEngine.embed("The recipe requires flour, eggs and milk. Bake for twenty minutes.")));
            show("Semantic matching test", ((dCosine <= dCosine2 || dCosine < 0.25d) ? "CHECK REQUIRED" : "PASS") + String.format(Locale.ROOT, "\nWarranty similarity: %.3f\nRecipe similarity: %.3f\nTime: %.1f seconds\n\nThis test runs the bundled model locally. Higher similarity should select the warranty passage.", Double.valueOf(dCosine), Double.valueOf(dCosine2), Double.valueOf((System.nanoTime() - jNanoTime) / 1.0E9d)));
        } catch (Exception | LinkageError e) {
            show("Semantic matching test", "Failed: " + e.getMessage() + "\nKeyword search remains available.");
        }
    }

    private TextView label(String str, int i) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(i);
        textView.setTextColor(Color.rgb(24, 43, 39));
        textView.setPadding(0, dp(10), 0, dp(8));
        this.column.addView(textView);
        return textView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$10() {
        new Thread(new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda20
            @Override // java.lang.Runnable
            public final void run() throws IOException {
                this.f$0.connectionTest();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$11() {
        new Thread(new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.embeddingTest();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$12(EditText editText) {
        preview(editText.getText().toString(), false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$13(EditText editText) {
        preview(editText.getText().toString(), true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$14() {
        if (DocsService.indexing) {
            toast("Stop the refresh first");
        } else {
            Store.get(this).clearIndex();
            updateStatus();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$15() {
        confirm("Clear extracted text and embeddings?", "Original files and folder permissions stay in place. Refresh to rebuild.", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$14();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$16() {
        stopService(new Intent(this, (Class<?>) DocsService.class));
        getSharedPreferences("connection", 0).edit().remove("token").commit();
        toast("Token reset");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$17() {
        confirm("Reset token?", "This stops the server. Copy the new value into Gallery afterwards.", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$16();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ WindowInsets lambda$onCreate$3(View view, WindowInsets windowInsets) {
        this.column.setPadding(dp(24), windowInsets.getSystemWindowInsetTop() + dp(20), dp(24), windowInsets.getSystemWindowInsetBottom() + dp(24));
        return windowInsets;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$4() {
        startActivityForResult(new Intent("android.intent.action.OPEN_DOCUMENT_TREE").addFlags(193), 100);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$5() {
        startServer("index");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$6() {
        startServer("start");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$7() {
        stopService(new Intent(this, (Class<?>) DocsService.class));
        DocsService.progress = "Server stopped";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$8() {
        copy("MCP URL", "http://127.0.0.1:8766/mcp", false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$9() {
        copy("Authorization", "Bearer " + DocsService.token(this), true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$preview$19(boolean z, String str) throws JSONException {
        try {
            JSONArray jSONArrayKeywordSearch = z ? Store.get(this).keywordSearch(str, 6) : Store.get(this).search(str, 6);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < jSONArrayKeywordSearch.length(); i++) {
                JSONObject jSONObject = jSONArrayKeywordSearch.getJSONObject(i);
                sb.append(jSONObject.getString("filename")).append(jSONObject.isNull("page") ? " · chunk " + jSONObject.optInt("chunk") : " · page " + jSONObject.optInt("page")).append("\n").append(jSONObject.optString("matched_by", "keyword"));
                if (jSONObject.has("semantic_similarity")) {
                    sb.append(String.format(Locale.ROOT, " · similarity %.3f", Double.valueOf(jSONObject.getDouble("semantic_similarity"))));
                }
                sb.append("\n").append(jSONObject.getString(TextBundle.TEXT_ENTRY)).append("\n\n");
            }
            show(z ? "Keyword-only results" : "Hybrid results", sb.length() == 0 ? "No matching passages. Check index coverage or try another question." : sb.toString());
        } catch (Exception e) {
            show("Search unavailable", e.getClass().getSimpleName() + ". Check coverage and refresh.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$show$1(String str, String str2) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        new AlertDialog.Builder(this).setTitle(str).setMessage(str2).setPositiveButton("OK", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showLicences$20(DialogInterface dialogInterface, int i) throws IOException {
        try {
            InputStream inputStreamOpen = getAssets().open("licenses/" + new String[]{"Apache-2.0.txt", "NanoHTTPD-BSD.md", "Bouncy-Castle.html", "ONNX-Runtime-MIT.txt"}[i]);
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    byte[] bArr = new byte[PKIFailureInfo.certConfirmed];
                    while (true) {
                        int i2 = inputStreamOpen.read(bArr);
                        if (i2 == -1) {
                            break;
                        } else {
                            byteArrayOutputStream.write(bArr, 0, i2);
                        }
                    }
                    show("Licence", byteArrayOutputStream.toString("UTF-8"));
                    byteArrayOutputStream.close();
                    if (inputStreamOpen != null) {
                        inputStreamOpen.close();
                    }
                } finally {
                }
            } finally {
            }
        } catch (Exception e) {
            show("Licence", "Could not open licence");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateStatus$21(Store store, String[] strArr) {
        store.removeRoot(strArr[0]);
        this.folderSnapshot = "";
        updateStatus();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateStatus$22(final Store store, final String[] strArr, View view) {
        confirm("Remove folder access?", "Delete this folder's indexed text and embeddings, and release access. Original files are unchanged.", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$updateStatus$21(store, strArr);
            }
        });
    }

    private void preview(final String str, final boolean z) {
        if (str.trim().isEmpty()) {
            toast("Enter a question or keywords");
        } else if (str.length() > 2000) {
            toast("Use at most 2,000 characters");
        } else {
            new Thread(new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda19
                @Override // java.lang.Runnable
                public final void run() throws JSONException {
                    this.f$0.lambda$preview$19(z, str);
                }
            }).start();
        }
    }

    private JSONObject rpc(String str, JSONObject jSONObject, boolean z) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL("http://127.0.0.1:8766/mcp").openConnection();
        try {
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setConnectTimeout(NanoHTTPD.SOCKET_READ_TIMEOUT);
            httpURLConnection.setReadTimeout(10000);
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("Accept", "application/json, text/event-stream");
            if (z) {
                httpURLConnection.setRequestProperty("Authorization", "Bearer " + DocsService.token(this));
            }
            byte[] bytes = new JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", str).put("params", jSONObject).toString().getBytes(StandardCharsets.UTF_8);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setFixedLengthStreamingMode(bytes.length);
            OutputStream outputStream = httpURLConnection.getOutputStream();
            try {
                outputStream.write(bytes);
                if (outputStream != null) {
                    outputStream.close();
                }
                int responseCode = httpURLConnection.getResponseCode();
                if (!z) {
                    if (responseCode == 401) {
                        return new JSONObject();
                    }
                    throw new IOException("Unauthenticated request was not rejected");
                }
                if (responseCode != 200) {
                    throw new IOException("HTTP " + responseCode);
                }
                InputStream inputStream = httpURLConnection.getInputStream();
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    try {
                        byte[] bArr = new byte[PKIFailureInfo.certConfirmed];
                        while (true) {
                            int i = inputStream.read(bArr);
                            if (i == -1) {
                                break;
                            }
                            byteArrayOutputStream.write(bArr, 0, i);
                        }
                        JSONObject jSONObject2 = new JSONObject(byteArrayOutputStream.toString("UTF-8"));
                        byteArrayOutputStream.close();
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        return jSONObject2;
                    } finally {
                    }
                } finally {
                }
            } finally {
            }
        } finally {
            httpURLConnection.disconnect();
        }
    }

    private void show(final String str, final String str2) {
        runOnUiThread(new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$show$1(str, str2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: showLicences, reason: merged with bridge method [inline-methods] */
    public void lambda$onCreate$18() {
        new AlertDialog.Builder(this).setTitle("Open-source licences").setItems(new String[]{"Apache 2.0 — PDFBox, AndroidX, MiniLM model", "NanoHTTPD — BSD", "Bouncy Castle", "ONNX Runtime — MIT"}, new DialogInterface.OnClickListener() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda13
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) throws IOException {
                this.f$0.lambda$showLicences$20(dialogInterface, i);
            }
        }).show();
    }

    private void startServer(String str) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 10);
        }
        startForegroundService(new Intent(this, (Class<?>) DocsService.class).setAction(str));
    }

    private void toast(String str) {
        Toast.makeText(this, str, 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateStatus() {
        try {
            final Store store = Store.get(this);
            JSONObject jSONObjectStatus = store.status();
            this.state.setText((DocsService.running ? "● Running" : "○ Stopped") + " · " + DocsService.progress);
            this.counts.setText(jSONObjectStatus.optInt("indexed_documents") + " documents · " + jSONObjectStatus.optInt("passages") + " passages\n" + jSONObjectStatus.optInt("embedded_passages") + " embedded · " + jSONObjectStatus.optInt("missing_embeddings") + " missing\n" + jSONObjectStatus.optString("embedding_warning"));
            this.refresh.setEnabled(!DocsService.indexing);
            String string = jSONObjectStatus.getJSONArray("folders").toString();
            if (string.equals(this.folderSnapshot)) {
                return;
            }
            this.folderSnapshot = string;
            this.folders.removeAllViews();
            for (final String[] strArr : store.roots()) {
                TextView textView = new TextView(this);
                textView.setText(strArr[1] + "\n" + (strArr[2].isEmpty() ? "Not refreshed" : strArr[2]) + "\n" + strArr[3]);
                textView.setPadding(0, dp(12), 0, 0);
                this.folders.addView(textView);
                Button button = new Button(this);
                button.setText("Remove " + strArr[1]);
                button.setAllCaps(false);
                button.setOnClickListener(new View.OnClickListener() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda11
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        this.f$0.lambda$updateStatus$22(store, strArr, view);
                    }
                });
                this.folders.addView(button);
            }
            if (this.folders.getChildCount() == 0) {
                TextView textView2 = new TextView(this);
                textView2.setText("No folders selected.");
                this.folders.addView(textView2);
            }
        } catch (Exception e) {
            this.counts.setText("Index unavailable");
        }
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i != 100 || i2 != -1 || intent == null || intent.getData() == null) {
            return;
        }
        Uri data = intent.getData();
        try {
            getContentResolver().takePersistableUriPermission(data, 1);
            DocumentFile documentFileFromTreeUri = DocumentFile.fromTreeUri(this, data);
            Store.get(this).addRoot(data.toString(), (documentFileFromTreeUri == null || documentFileFromTreeUri.getName() == null) ? "Selected folder" : documentFileFromTreeUri.getName());
            this.folderSnapshot = "";
            updateStatus();
            toast("Folder added. Refresh to index it.");
        } catch (Exception e) {
            toast("Could not add folder: " + e.getMessage());
        }
    }

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(244, 246, 243));
        this.column = new LinearLayout(this);
        this.column.setOrientation(1);
        this.column.setPadding(dp(24), dp(32), dp(24), dp(32));
        scrollView.addView(this.column);
        setContentView(scrollView);
        scrollView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda22
            @Override // android.view.View.OnApplyWindowInsetsListener
            public final WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                return this.f$0.lambda$onCreate$3(view, windowInsets);
            }
        });
        label("POCKET DOCS SEMANTIC  /  0.2.0", 12);
        label("Find it by meaning.", 30);
        label("Search your documents with questions or keywords. The embedding model and index stay on this phone.", 16);
        this.state = label("Server stopped", 16);
        this.counts = label("", 14);
        button("Choose a folder", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$4();
            }
        });
        this.refresh = button("Refresh selected folders", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$5();
            }
        });
        button("Start server", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$6();
            }
        });
        button("Stop server / cancel refresh", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$7();
            }
        });
        label("Connect Edge Gallery", 20);
        label("URL: http://127.0.0.1:8766/mcp\nRequest header name: Authorization\nUse this build's token below.", 15);
        button("Copy MCP URL", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$8();
            }
        });
        button("Copy Authorization value", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$9();
            }
        });
        button("Test local MCP connection", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$10();
            }
        });
        button("Test semantic matching", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$11();
            }
        });
        label("Start the server before connecting. It stops after 30 minutes. This test build uses a separate index and port from Pocket Docs 0.1.", 14);
        label("Try your index", 20);
        final EditText editText = new EditText(this);
        editText.setHint("e.g. Can I get this repaired for free?");
        editText.setMaxLines(3);
        this.column.addView(editText);
        button("Search by meaning + keywords", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$12(editText);
            }
        });
        button("Compare keyword-only results", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda23
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$13(editText);
            }
        });
        label("Selected folders", 20);
        this.folders = new LinearLayout(this);
        this.folders.setOrientation(1);
        this.column.addView(this.folders);
        button("Clear index", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda24
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$15();
            }
        });
        button("Reset connection token", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$17();
            }
        });
        button("Open-source licences", new Runnable() { // from class: com.jimliddle.pocketdocs.MainActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$18();
            }
        });
        label("PDF text, UTF-8 TXT and Markdown. Scans need OCR, which is not included. Refresh after changing or deleting files; unchanged passages reuse their embeddings. Cloud folders may download files through their provider.", 14);
    }

    @Override // android.app.Activity
    protected void onPause() {
        this.handler.removeCallbacks(this.tick);
        super.onPause();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        this.handler.post(this.tick);
    }
}
