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
              "tell", "about", "at", "than", "rather", "through", "more", "once", "one"));

  public static List<String> chunks(String text) {
    text = text.replace('\0', ' ').replaceAll("[\\t\\x0B\\f\\r ]+", " ").trim();
    List<Integer> boundaries = new ArrayList<>();
    Matcher sentence = Pattern.compile("[.!?]\\s+(?=[\\p{Lu}0-9])").matcher(text);
    while (sentence.find()) boundaries.add(sentence.end());
    List<String> out = new ArrayList<>();
    for (int start = 0; start < text.length(); ) {
      int end = Math.min(start + 1600, text.length());
      if (end < text.length()) {
        int sentenceEnd = -1;
        for (int at : boundaries) if (at > start + 800 && at <= end) sentenceEnd = at;
        if (sentenceEnd > 0) end = sentenceEnd;
        else {
          int space = text.lastIndexOf(' ', end);
          if (space > start) end = space;
        }
      }
      out.add(text.substring(start, end).trim());
      if (end == text.length()) break;
      int next = end;
      for (int at : boundaries)
        if (at >= end - 300 && at > start && at < end) {
          next = at;
          break;
        }
      if (next == end && !boundaries.contains(end)) {
        next = Math.max(start + 1, end - 180);
        while (next < end && !Character.isWhitespace(text.charAt(next - 1))) next++;
      }
      start = next;
      while (start < text.length() && Character.isWhitespace(text.charAt(start))) start++;
    }
    return out;
  }

  static double referenceWeight(String query, String text) {
    if (query
        .toLowerCase(Locale.ROOT)
        .matches(".*\\b(who|cite|cited|citation|citations|reference|references|bibliography)\\b.*"))
      return 1;
    Matcher entries = Pattern.compile("(?m)^\\s*\\[\\d+\\]").matcher(text);
    int count = 0;
    while (entries.find()) count++;
    Matcher years = Pattern.compile("\\b(?:19|20)\\d{2}\\b").matcher(text);
    int dates = 0;
    while (years.find()) dates++;
    return count >= 3 && dates >= 2 ? .35 : 1;
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
    for (String w : words(query)) {
      String stem = w.length() > 4 && w.endsWith("s") ? w.substring(0, w.length() - 1) : w;
      q.add("\"" + stem + "\"*");
    }
    return String.join(" OR ", q);
  }

  public static int score(String query, String title, String body) {
    title = title.toLowerCase(Locale.ROOT);
    body = body.toLowerCase(Locale.ROOT);
    int n = 0;
    for (String w : words(query)) {
      if (containsWord(title, w)) n += 4;
      if (containsWord(body, w)) n += 2;
    }
    return n;
  }

  static boolean containsWord(String text, String word) {
    String stem =
        word.length() > 4 && word.endsWith("s") ? word.substring(0, word.length() - 1) : word;
    return Pattern.compile("(?<![\\p{L}\\p{N}_])" + Pattern.quote(stem) + "s?(?![\\p{L}\\p{N}_])")
        .matcher(text)
        .find();
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
