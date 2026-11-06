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
  private OrtEnvironment env;
  private OrtSession session;
  private WordPieceTokenizer tokenizer;
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
    if (session != null) return;
    try (InputStream in = context.getAssets().open("vocab.txt")) {
      tokenizer = new WordPieceTokenizer(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
    byte[] model;
    try (InputStream in = context.getAssets().open("minilm-q8.onnx");
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] b = new byte[16384];
      int n;
      while ((n = in.read(b)) != -1) out.write(b, 0, n);
      model = out.toByteArray();
    }
    env = OrtEnvironment.getEnvironment();
    try (OrtSession.SessionOptions o = new OrtSession.SessionOptions()) {
      o.setIntraOpNumThreads(2);
      o.setInterOpNumThreads(1);
      session = env.createSession(model, o);
    }
  }

  public synchronized float[] embed(String text) {
    try {
      initialize();
      float[] sum = new float[384];
      int total = 0;
      for (long[] ids : tokenizer.windows(text)) {
        if (Thread.currentThread().isInterrupted())
          throw new java.util.concurrent.CancellationException();
        long[] mask = new long[ids.length];
        Arrays.fill(mask, 1);
        try (OnnxTensor input = OnnxTensor.createTensor(env, new long[][] {ids});
            OnnxTensor attention = OnnxTensor.createTensor(env, new long[][] {mask});
            OnnxTensor types = OnnxTensor.createTensor(env, new long[][] {new long[ids.length]})) {
          Map<String, OnnxTensor> inputs = new HashMap<>();
          inputs.put("input_ids", input);
          inputs.put("attention_mask", attention);
          inputs.put("token_type_ids", types);
          try (OrtSession.Result result = session.run(inputs)) {
            float[][][] output = (float[][][]) result.get(0).getValue();
            for (float[] token : output[0]) {
              if (token.length != 384) throw new IllegalStateException("Unexpected dimensions");
              for (int i = 0; i < 384; i++) sum[i] += token[i];
              total++;
            }
          }
        }
      }
      for (int i = 0; i < 384; i++) sum[i] /= Math.max(total, 1);
      HybridRanker.normalize(sum);
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
