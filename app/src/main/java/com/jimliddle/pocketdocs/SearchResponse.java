package com.jimliddle.pocketdocs;

import java.util.*;
import org.json.*;

/** Bounded extractive output: never generates text or rewrites a citation. */
public final class SearchResponse {
  public static final int DEFAULT_LIMIT = 2, MAX_EXCERPT = 650, MAX_TOTAL_TEXT = 2600;

  public static String excerpt(String text, String query, int max) {
    if (text.length() <= max) return text;
    Set<String> words = TextTools.words(query);
    int best = 0, bestScore = -1;
    for (int i = 0; i < text.length(); i += 160) {
      int end = Math.min(i + max, text.length());
      String window = text.substring(i, end).toLowerCase(Locale.ROOT);
      int score = 0;
      for (String w : words) if (window.contains(w)) score++;
      if (score > bestScore) {
        bestScore = score;
        best = i;
      }
      if (end == text.length()) break;
    }
    int end = Math.min(best + max, text.length());
    if (end < text.length()) {
      int space = text.lastIndexOf(' ', end);
      if (space > best + max / 2) end = space;
    }
    return text.substring(best, end).trim();
  }

  static Set<String> shingles(String text) {
    String[] words =
        text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim().split("\\s+");
    Set<String> result = new HashSet<>();
    for (int i = 0; i + 4 < words.length; i++)
      result.add(String.join(" ", Arrays.copyOfRange(words, i, i + 5)));
    return result;
  }

  static boolean duplicate(String a, String b) {
    Set<String> x = shingles(a), y = shingles(b);
    if (x.isEmpty() || y.isEmpty()) return a.equals(b);
    int small = Math.min(x.size(), y.size());
    x.retainAll(y);
    return (double) x.size() / small >= .65;
  }

  public static JSONArray compact(JSONArray input, String query, int limit) throws JSONException {
    JSONArray out = new JSONArray();
    List<String> accepted = new ArrayList<>();
    int remaining = MAX_TOTAL_TEXT;
    for (int i = 0; i < input.length() && out.length() < limit && remaining > 100; i++) {
      JSONObject p = input.getJSONObject(i);
      String text = p.getString("text");
      boolean duplicate = false;
      for (String prior : accepted)
        if (duplicate(text, prior)) {
          duplicate = true;
          break;
        }
      if (duplicate) continue;
      // Remove the exact suffix/prefix overlap created by chunking; keep the rest of an adjacent
      // passage.
      String reduced = text;
      for (String prior : accepted) {
        int max = Math.min(Math.min(prior.length(), reduced.length()), 500);
        for (int n = max; n >= 40; n--) {
          if (prior.endsWith(reduced.substring(0, n))) {
            reduced = reduced.substring(n).trim();
            break;
          }
          if (prior.startsWith(reduced.substring(reduced.length() - n))) {
            reduced = reduced.substring(0, reduced.length() - n).trim();
            break;
          }
        }
      }
      if (reduced.isEmpty()) continue;
      String excerpt = excerpt(reduced, query, Math.min(MAX_EXCERPT, remaining));
      JSONObject result =
          new JSONObject()
              .put("passage_id", p.getString("passage_id"))
              .put("filename", p.getString("filename"))
              .put("page", p.opt("page"))
              .put("chunk", p.getInt("chunk"))
              .put("text", excerpt)
              .put("extraction", p.optString("extraction", "text"))
              .put("excerpt", !excerpt.equals(text));
      if (p.has("retrieval_warning")) result.put("warning", p.getString("retrieval_warning"));
      out.put(result);
      remaining -= excerpt.length();
      accepted.add(text);
    }
    return out;
  }
}
