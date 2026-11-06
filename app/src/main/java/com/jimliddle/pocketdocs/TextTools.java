package com.jimliddle.pocketdocs;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;

public final class TextTools {
  private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}_-]*");
  private static final Set<String> STOP =
      new HashSet<>(
          Arrays.asList(
              "a", "an", "the", "is", "are", "was", "were", "of", "for", "to", "in", "on", "and",
              "or", "my", "me", "find", "what", "which", "where", "how", "does", "do", "did", "it",
              "its", "with", "from", "that", "this", "have", "has", "i", "can", "you", "please",
              "tell", "about"));

  public static List<String> chunks(String text) {
    text = text.replace('\0', ' ').replaceAll("[\\t\\x0B\\f\\r ]+", " ").trim();
    List<String> out = new ArrayList<>();
    for (int start = 0; start < text.length(); ) {
      int end = Math.min(start + 1600, text.length());
      if (end < text.length()) {
        int space = text.lastIndexOf(' ', end);
        if (space > start + 1000) end = space;
      }
      out.add(text.substring(start, end));
      if (end == text.length()) break;
      start = Math.max(start + 1, end - 180);
    }
    return out;
  }

  static Set<String> words(String text) {
    Set<String> out = new LinkedHashSet<>();
    Matcher m = WORD.matcher(text.toLowerCase(Locale.ROOT));
    while (m.find() && out.size() < 16)
      if (!STOP.contains(m.group()) && m.group().length() <= 80) out.add(m.group());
    return out;
  }

  public static String ftsQuery(String query) {
    List<String> q = new ArrayList<>();
    for (String w : words(query)) q.add("\"" + w + "\"");
    return String.join(" OR ", q);
  }

  public static int score(String query, String title, String body) {
    title = title.toLowerCase(Locale.ROOT);
    body = body.toLowerCase(Locale.ROOT);
    int n = 0;
    for (String w : words(query)) {
      if (title.contains(w)) n += 4;
      if (body.contains(w)) n += 2;
    }
    return n;
  }

  static String hash(String text) {
    try {
      byte[] bytes =
          MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder b = new StringBuilder();
      for (byte x : bytes) b.append(String.format(Locale.ROOT, "%02x", x & 255));
      return b.toString();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
