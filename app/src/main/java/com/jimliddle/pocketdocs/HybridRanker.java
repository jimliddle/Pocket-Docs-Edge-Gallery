package com.jimliddle.pocketdocs;

import java.nio.*;
import java.util.*;

public final class HybridRanker {
  public static final double MIN_COSINE = .25;

  public static final class Hit {
    public final String id;
    public final double score;

    public Hit(String id, double score) {
      this.id = id;
      this.score = score;
    }
  }

  public static List<Hit> sorted(Collection<Hit> hits, int limit) {
    List<Hit> a = new ArrayList<>(hits);
    a.sort(Comparator.comparingDouble((Hit h) -> h.score).reversed().thenComparing(h -> h.id));
    return new ArrayList<>(a.subList(0, Math.min(limit, a.size())));
  }

  public static List<Hit> fuse(List<Hit> a, List<Hit> b, int limit) {
    Map<String, Double> scores = new HashMap<>();
    for (List<Hit> list : Arrays.asList(a, b)) {
      int rank = 0;
      for (int i = 0; i < list.size(); i++) {
        // Equal relevance scores receive equal weight, independent of file/URI hashes.
        if (i == 0 || Double.compare(list.get(i).score, list.get(i - 1).score) != 0) rank = i;
        scores.merge(list.get(i).id, 1.0 / (61 + rank), Double::sum);
      }
    }
    List<Hit> out = new ArrayList<>();
    for (Map.Entry<String, Double> e : scores.entrySet())
      out.add(new Hit(e.getKey(), e.getValue()));
    return sorted(out, limit);
  }

  public static float[] normalize(float[] v) {
    double sum = 0;
    for (float f : v) {
      if (!Float.isFinite(f)) throw new IllegalArgumentException("Non-finite vector");
      sum += f * f;
    }
    if (sum == 0) throw new IllegalArgumentException("Empty vector");
    float scale = (float) (1 / Math.sqrt(sum));
    for (int i = 0; i < v.length; i++) v[i] *= scale;
    return v;
  }

  public static byte[] encode(float[] v) {
    ByteBuffer b = ByteBuffer.allocate(v.length * 4).order(ByteOrder.LITTLE_ENDIAN);
    for (float f : v) b.putFloat(f);
    return b.array();
  }

  public static double cosine(float[] q, byte[] blob) {
    if (blob == null || blob.length != q.length * 4) return -1;
    ByteBuffer b = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN);
    double dot = 0;
    for (float f : q) dot += f * b.getFloat();
    return Double.isFinite(dot) ? Math.max(-1, Math.min(1, dot)) : -1;
  }
}
