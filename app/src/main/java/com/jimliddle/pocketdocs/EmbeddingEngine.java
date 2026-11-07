package com.jimliddle.pocketdocs;

import ai.onnxruntime.*;
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class EmbeddingEngine implements EmbeddingProvider {
  public static final String MODEL_ID = "all-MiniLM-L6-v2-q8-v1";
  private static EmbeddingEngine instance;
  private final Context context;
  private MiniLmEncoder encoder;
  private volatile String error = "";

  private EmbeddingEngine(Context c) {
    context = c.getApplicationContext();
  }

  public static synchronized EmbeddingEngine get(Context c) {
    if (instance == null) instance = new EmbeddingEngine(c);
    return instance;
  }

  public String error() {
    return error;
  }

  private void initialize() throws Exception {
    if (encoder != null) return;
    byte[] model;
    try (InputStream in = context.getAssets().open("minilm-q8.onnx");
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] b = new byte[16384];
      int n;
      while ((n = in.read(b)) != -1) out.write(b, 0, n);
      model = out.toByteArray();
    }
    try (InputStream in = context.getAssets().open("vocab.txt");
        Reader vocabulary = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      encoder = new MiniLmEncoder(vocabulary, model);
    }
  }

  public synchronized float[] embed(String text) {
    try {
      initialize();
      float[] sum = encoder.embed(text);
      error = "";
      return sum;
    } catch (java.util.concurrent.CancellationException e) {
      throw e;
    } catch (Exception | LinkageError e) {
      error =
          "Local embedding unavailable ("
              + e.getClass().getSimpleName()
              + "). Keyword search remains available.";
      throw new IllegalStateException(error, e);
    }
  }
}
