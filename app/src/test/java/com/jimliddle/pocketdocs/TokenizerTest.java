package com.jimliddle.pocketdocs;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import org.junit.Test;

public class TokenizerTest {
  @Test
  public void actualVocabularyMatchesBertTokens() throws Exception {
    WordPieceTokenizer t = new WordPieceTokenizer(new FileReader("src/main/assets/vocab.txt"));
    assertEquals(Arrays.asList(7592, 1010, 2088, 999), t.pieces("Héllo, world!"));
  }

  @Test
  public void longPassagesAreWindowedInsteadOfTruncated() throws Exception {
    WordPieceTokenizer t = new WordPieceTokenizer(new FileReader("src/main/assets/vocab.txt"));
    List<long[]> windows = t.windows("hello ".repeat(600));
    assertEquals(3, windows.size());
    int recovered = 0;
    for (int i = 0; i < windows.size(); i++) {
      long[] w = windows.get(i);
      assertTrue(w.length <= 256);
      assertEquals(101, w[0]);
      assertEquals(102, w[w.length - 1]);
      recovered += w.length - 2 - (i == 0 ? 0 : 32);
    }
    assertEquals(600, recovered);
  }
}
