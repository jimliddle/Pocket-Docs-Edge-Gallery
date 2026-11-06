package com.jimliddle.pocketdocs;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.*;
import android.widget.*;
import androidx.documentfile.provider.DocumentFile;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.json.*;

public final class MainActivity extends Activity {
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler handler = new Handler(Looper.getMainLooper());
  private TextView status;
  private LinearLayout body;
  private EditText query;
  private final Runnable refresh =
      new Runnable() {
        public void run() {
          updateStatus();
          handler.postDelayed(this, 1500);
        }
      };

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    PDFBoxResourceLoader.init(getApplicationContext());
    ScrollView scroll = new ScrollView(this);
    body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(28, 36, 28, 36);
    scroll.addView(body);
    setContentView(scroll);
    scroll.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          v.setPadding(
              insets.getSystemWindowInsetLeft(),
              insets.getSystemWindowInsetTop(),
              insets.getSystemWindowInsetRight(),
              insets.getSystemWindowInsetBottom());
          return insets;
        });
    text("Pocket Docs", 26);
    text(
        "Search selected folders from AI Edge Gallery. Documents and OCR are processed on this"
            + " phone.",
        16);
    status = text("", 15);
    button("Start server", () -> start("start"));
    button("Stop server", () -> stopService(new Intent(this, DocsService.class)));
    text("Edge Gallery connection", 21);
    text(
        "URL: http://127.0.0.1:8766/mcp\n"
            + "Header name: Authorization\n"
            + "Header value: use Copy below. Keep Pocket Docs running while you search.",
        15);
    button("Copy server URL", () -> copy("URL", "http://127.0.0.1:8766/mcp"));
    button(
        "Copy Authorization value",
        () -> copy("Authorization", "Bearer " + DocsService.token(this)));
    button("Test local MCP connection", () -> background(() -> testConnection()));
    text("Folders and indexing", 21);
    button(
        "Choose a folder",
        () -> {
          Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
          i.addFlags(
              Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
          startActivityForResult(i, 42);
        });
    button("Refresh folders", () -> start("index"));
    button("Folder coverage / remove folder", this::folders);
    text("PDF text recognition", 21);
    text(
        "Automatic OCR reads scanned pages with little extractable text. For mixed scans or broken"
            + " text, select All pages and refresh. Latin-script text only; check important numbers"
            + " against the original.",
        15);
    Spinner mode = new Spinner(this);
    mode.setAdapter(
        new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {"Automatic: scanned pages", "All PDF pages", "Off"}));
    String[] values = {"auto", "all", "off"};
    String saved = getSharedPreferences("settings", 0).getString("ocr", "auto");
    mode.setSelection(
        java.util.Arrays.asList(values).indexOf(saved) < 0
            ? 0
            : java.util.Arrays.asList(values).indexOf(saved));
    body.addView(mode);
    mode.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(android.widget.AdapterView<?> a) {}

          public void onItemSelected(
              android.widget.AdapterView<?> a, android.view.View v, int pos, long id) {
            getSharedPreferences("settings", 0).edit().putString("ocr", values[pos]).apply();
          }
        });
    button("Test bundled OCR", () -> background(() -> testOcr()));
    text("Try a search", 21);
    query = new EditText(this);
    query.setHint("Ask about your documents");
    body.addView(query);
    button(
        "Search excerpts",
        () -> {
          String q = query.getText().toString().trim();
          if (!q.isEmpty())
            background(
                () -> SearchResponse.compact(Store.get(this).search(q, 6), q, 2).toString(2));
        });
    button("Show index status", () -> background(() -> Store.get(this).status().toString(2)));
    button(
        "Clear index",
        () ->
            new AlertDialog.Builder(this)
                .setMessage(
                    "Clear extracted text and embeddings? Selected folders stay available for"
                        + " refresh.")
                .setPositiveButton("Clear", (d, w) -> Store.get(this).clearIndex())
                .setNegativeButton("Cancel", null)
                .show());
    button(
        "Reset connection token",
        () ->
            new AlertDialog.Builder(this)
                .setMessage(
                    "Stop the server and replace its token? Update the header value in Edge Gallery"
                        + " afterwards.")
                .setPositiveButton(
                    "Reset",
                    (d, w) -> {
                      stopService(new Intent(this, DocsService.class));
                      getSharedPreferences("connection", 0).edit().remove("token").commit();
                      DocsService.token(this);
                      show("Token reset. Copy the new Authorization value before restarting.");
                    })
                .setNegativeButton("Cancel", null)
                .show());
    text(
        "0.3.0 · Android 8+ · ARM64\n"
            + "PDF and UTF-8 text files. Manual refresh; changed or deleted files remain searchable"
            + " until refreshed. Server stops after 30 minutes. OCR can take time on long scans.\n"
            + "Includes PDFBox, ONNX Runtime, MiniLM, NanoHTTPD and Google ML Kit. ML Kit can send"
            + " usage and performance metrics to Google. See developers.google.com/ml-kit/terms."
            + " Third-party licence files are bundled in assets/licenses; ML Kit is governed by"
            + " Google's terms.",
        14);
    if (Build.VERSION.SDK_INT >= 33
        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 7);
  }

  private TextView text(String s, int size) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(size);
    t.setPadding(0, 12, 0, 12);
    body.addView(t);
    return t;
  }

  private void button(String s, Runnable r) {
    Button b = new Button(this);
    b.setText(s);
    body.addView(b);
    b.setOnClickListener(v -> r.run());
  }

  private void start(String action) {
    startForegroundService(new Intent(this, DocsService.class).setAction(action));
  }

  private void copy(String label, String value) {
    getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(label, value));
    Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
  }

  private void show(String s) {
    if (isFinishing() || isDestroyed()) return;
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextIsSelectable(true);
    t.setPadding(24, 20, 24, 20);
    ScrollView sc = new ScrollView(this);
    sc.addView(t);
    new AlertDialog.Builder(this).setView(sc).setPositiveButton("OK", null).show();
  }

  private interface Job {
    String run() throws Exception;
  }

  private void background(Job job) {
    Toast.makeText(this, "Working…", Toast.LENGTH_SHORT).show();
    worker.submit(
        () -> {
          String result;
          try {
            result = job.run();
          } catch (Exception e) {
            result = "Could not complete: " + e.getClass().getSimpleName() + ". " + e.getMessage();
          }
          String s = result;
          handler.post(() -> show(s));
        });
  }

  private void updateStatus() {
    if (status != null)
      status.setText(
          (DocsService.running ? "Server running" : "Server stopped")
              + "\n"
              + DocsService.progress);
  }

  private void folders() {
    java.util.List<String[]> roots = Store.get(this).roots();
    if (roots.isEmpty()) {
      show("No folders selected.");
      return;
    }
    String[] names = new String[roots.size()];
    for (int i = 0; i < names.length; i++) names[i] = roots.get(i)[1];
    new AlertDialog.Builder(this)
        .setTitle("Selected folders")
        .setItems(
            names,
            (d, n) -> {
              String[] r = roots.get(n);
              new AlertDialog.Builder(this)
                  .setTitle(r[1])
                  .setMessage("Refreshed: " + r[2] + "\n" + r[3])
                  .setPositiveButton("OK", null)
                  .setNeutralButton(
                      "Remove",
                      (x, w) ->
                          new AlertDialog.Builder(this)
                              .setMessage("Remove this folder and its indexed text?")
                              .setPositiveButton(
                                  "Remove", (y, z) -> Store.get(this).removeRoot(r[0]))
                              .setNegativeButton("Cancel", null)
                              .show())
                  .show();
            })
        .show();
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request == 42 && result == RESULT_OK && data != null && data.getData() != null) {
      try {
        Uri uri = data.getData();
        getContentResolver()
            .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        DocumentFile doc = DocumentFile.fromTreeUri(this, uri);
        Store.get(this)
            .addRoot(
                uri.toString(), doc == null || doc.getName() == null ? "Folder" : doc.getName());
        show("Folder selected. Tap Refresh folders to index it.");
      } catch (Exception e) {
        show("Could not add folder: " + e.getMessage());
      }
    }
  }

  private String testConnection() throws Exception {
    HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:8766/mcp").openConnection();
    c.setConnectTimeout(5000);
    c.setReadTimeout(10000);
    c.setRequestMethod("POST");
    c.setDoOutput(true);
    c.setRequestProperty("Authorization", "Bearer " + DocsService.token(this));
    c.setRequestProperty("Content-Type", "application/json");
    try {
      try (OutputStream out = c.getOutputStream()) {
        out.write(
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"
                .getBytes(StandardCharsets.UTF_8));
      }
      int code = c.getResponseCode();
      if (code != 200) return "Connection returned HTTP " + code;
      try (InputStream in = c.getInputStream()) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
        JSONObject response = new JSONObject(out.toString("UTF-8"));
        return "Connected. Available tools: "
            + response.getJSONObject("result").getJSONArray("tools").length();
      }
    } finally {
      c.disconnect();
    }
  }

  private String testOcr() throws Exception {
    File file = File.createTempFile("ocr-test-", ".pdf", getCacheDir());
    try {
      PdfDocument pdf = new PdfDocument();
      try {
        PdfDocument.Page page =
            pdf.startPage(new PdfDocument.PageInfo.Builder(600, 300, 1).create());
        page.getCanvas().drawColor(Color.WHITE);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        page.getCanvas().drawText("Warranty covers repairs", 40, 100, paint);
        pdf.finishPage(page);
        try (FileOutputStream out = new FileOutputStream(file)) {
          pdf.writeTo(out);
        }
      } finally {
        pdf.close();
      }
      try (PdfOcr ocr = new PdfOcr(file)) {
        String text = ocr.page(1);
        return (text.toLowerCase(java.util.Locale.ROOT).contains("warranty")
                ? "OCR test passed"
                : "OCR test needs checking")
            + "\nRecognised: "
            + text;
      }
    } finally {
      file.delete();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    handler.post(refresh);
  }

  @Override
  protected void onPause() {
    handler.removeCallbacks(refresh);
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    worker.shutdownNow();
    handler.removeCallbacksAndMessages(null);
    super.onDestroy();
  }
}
