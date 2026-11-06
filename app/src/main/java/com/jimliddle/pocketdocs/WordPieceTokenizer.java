package com.jimliddle.pocketdocs;

import java.io.*;
import java.text.Normalizer;
import java.util.*;

public final class WordPieceTokenizer {
  private final Map<String, Integer> vocab = new HashMap<>();

  public WordPieceTokenizer(Reader reader) throws IOException {
    try (BufferedReader r = new BufferedReader(reader)) {
      String word;
      int i = 0;
      while ((word = r.readLine()) != null) vocab.put(word, i++);
    }
    for (String key : Arrays.asList("[CLS]", "[SEP]", "[UNK]"))
      if (!vocab.containsKey(key)) throw new IOException("Invalid vocabulary");
  }

  private boolean chinese(int cp) {
    return (cp >= 0x4e00 && cp <= 0x9fff)
        || (cp >= 0x3400 && cp <= 0x4dbf)
        || (cp >= 0x20000 && cp <= 0x2a6df)
        || (cp >= 0x2a700 && cp <= 0x2b73f)
        || (cp >= 0x2b740 && cp <= 0x2b81f)
        || (cp >= 0x2b820 && cp <= 0x2ceaf)
        || (cp >= 0xf900 && cp <= 0xfaff)
        || (cp >= 0x2f800 && cp <= 0x2fa1f);
  }

  private boolean punctuation(int cp) {
    int t = Character.getType(cp);
    return (cp >= 33 && cp <= 47)
        || (cp >= 58 && cp <= 64)
        || (cp >= 91 && cp <= 96)
        || (cp >= 123 && cp <= 126)
        || t == 23
        || t == 20
        || t == 21
        || t == 22
        || t == 29
        || t == 30
        || t == 24;
  }

  public List<Integer> pieces(String text) {
    StringBuilder clean = new StringBuilder();
    Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
        .codePoints()
        .forEach(
            cp -> {
              int t = Character.getType(cp);
              if (t == Character.NON_SPACING_MARK || cp == 0 || cp == 0xfffd) return;
              if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
                clean.append(' ');
                return;
              }
              if (t == Character.CONTROL || t == Character.FORMAT) return;
              if (chinese(cp) || punctuation(cp)) clean.append(' ').appendCodePoint(cp).append(' ');
              else clean.appendCodePoint(cp);
            });
    List<Integer> result = new ArrayList<>();
    for (String word : clean.toString().trim().split("\\s+")) {
      if (word.isEmpty()) continue;
      if (word.codePointCount(0, word.length()) > 100) {
        result.add(vocab.get("[UNK]"));
        continue;
      }
      List<Integer> parts = new ArrayList<>();
      int start = 0;
      boolean unknown = false;
      while (start < word.length()) {
        int end = word.length();
        Integer id = null;
        while (end > start) {
          id = vocab.get((start == 0 ? "" : "##") + word.substring(start, end));
          if (id != null) break;
          end = word.offsetByCodePoints(end, -1);
        }
        if (id == null) {
          unknown = true;
          break;
        }
        parts.add(id);
        start = end;
      }
      if (unknown) result.add(vocab.get("[UNK]"));
      else result.addAll(parts);
    }
    return result;
  }

  public List<long[]> windows(String text) {
    List<Integer> tokens = pieces(text);
    List<long[]> out = new ArrayList<>();
    for (int start = 0; ; ) {
      int end = Math.min(start + 254, tokens.size());
      long[] ids = new long[end - start + 2];
      ids[0] = vocab.get("[CLS]");
      for (int i = start; i < end; i++) ids[i - start + 1] = tokens.get(i);
      ids[ids.length - 1] = vocab.get("[SEP]");
      out.add(ids);
      if (end == tokens.size()) break;
      start = end - 32;
    }
    return out;
  }
}
