package com.jimliddle.pocketdocs;

import ai.onnxruntime.*;
import java.io.Reader;
import java.util.*;

/** Shared by Android and the native retrieval regression benchmark. */
public final class MiniLmEncoder implements AutoCloseable {
  private final OrtEnvironment env = OrtEnvironment.getEnvironment();
  private final OrtSession session;
  private final WordPieceTokenizer tokenizer;

  public MiniLmEncoder(Reader vocabulary, byte[] model) throws Exception {
    tokenizer = new WordPieceTokenizer(vocabulary);
    try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
      options.setIntraOpNumThreads(2);
      options.setInterOpNumThreads(1);
      session = env.createSession(model, options);
    }
  }

  public synchronized float[] embed(String text) throws Exception {
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
    return sum;
  }

  @Override
  public void close() throws OrtException {
    session.close();
  }
}
