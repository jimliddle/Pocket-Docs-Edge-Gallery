package com.jimliddle.pocketdocs;

import java.util.*;
import org.json.*;

/** Bounded extractive output: never generates text or rewrites a citation. */
public final class SearchResponse {
  public static final int DEFAULT_LIMIT = 8, MAX_EXCERPT = 1600, MAX_TOTAL_TEXT = 12800;

  public static String excerpt(String text, String query, int max) {
    if (text.length() <= max) return text;
    // Normal indexed chunks already fit. For oversized input, choose a word-aligned
    // window and prefer complete sentences rather than cutting arbitrary characters.
    int best = 0, bestEnd = Math.min(max, text.length()), bestScore = -1;
    for (int start = 0; start < text.length(); ) {
      int end = Math.min(start + max, text.length());
      if (end < text.length()) {
        while (end > start && !Character.isWhitespace(text.charAt(end))) end--;
        if (end == start) end = Math.min(start + max, text.length());
      }
      int score = TextTools.score(query, "", text.substring(start, end));
      if (score > bestScore) {
        bestScore = score;
        best = start;
        bestEnd = end;
      }
      if (end == text.length()) break;
      int next = Math.min(start + 160, text.length());
      while (next < text.length() && !Character.isWhitespace(text.charAt(next - 1))) next++;
      start = next;
    }
    String result = text.substring(best, bestEnd).trim();
    int sentenceEnd =
        Math.max(
            result.lastIndexOf(". "), Math.max(result.lastIndexOf("! "), result.lastIndexOf("? ")));
    if (bestEnd < text.length() && sentenceEnd >= result.length() * 3 / 4)
      result = result.substring(0, sentenceEnd + 1);
    return result;
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
    return compact(input, query, limit, MAX_TOTAL_TEXT);
  }

  public static JSONArray compact(JSONArray input, String query, int limit, int maxChars)
      throws JSONException {
    if (maxChars < MAX_EXCERPT || maxChars > MAX_TOTAL_TEXT)
      throw new IllegalArgumentException("max_chars must be 1600–12800");
    JSONArray out = new JSONArray();
    List<String> accepted = new ArrayList<>();
    int remaining = maxChars;
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
      // Fit complete chunks where possible, rather than cutting the last one to fill a budget.
      String excerpt = excerpt(reduced, query, MAX_EXCERPT);
      if (excerpt.length() > remaining) continue;
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
