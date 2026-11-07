package com.jimliddle.pocketdocs;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

public class TextToolsTest {
  @Test
  public void keywordsDoNotMatchInsideUnrelatedWords() {
    assertEquals(0, TextTools.score("cat", "", "education category"));
    assertEquals(2, TextTools.score("cat", "", "The cat sleeps."));
    assertEquals(2, TextTools.score("warranties", "", "Warranties apply."));
    assertEquals(2, TextTools.score("repairs", "", "The repair is free."));
    assertEquals(2, TextTools.score("repair", "", "Repairs are free."));
    assertFalse(TextTools.words("at once rather than one word").contains("at"));
  }

  @Test
  public void chunksRetainAllWordsAndPreferCompleteSentences() {
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < 140; i++)
      text.append("Sentence ")
          .append(i)
          .append(" explains a separate topic with enough detail to identify it. ");
    List<String> chunks = TextTools.chunks(text.toString());
    assertTrue(chunks.size() > 1);
    for (String chunk : chunks) {
      assertTrue(chunk.length() <= 1600);
      assertTrue(chunk.startsWith("Sentence "));
      assertTrue(chunk.endsWith("."));
    }
    for (int i = 0; i < 140; i++) {
      String sentence =
          "Sentence " + i + " explains a separate topic with enough detail to identify it.";
      assertTrue(sentence, chunks.stream().anyMatch(c -> c.contains(sentence)));
    }
  }

  @Test
  public void longUnpunctuatedInputMakesProgressWithoutLosingText() {
    String text = "x".repeat(4000);
    List<String> chunks = TextTools.chunks(text);
    assertEquals(text, String.join("", chunks));
    assertEquals(3, chunks.size());
    for (String chunk : chunks) assertTrue(chunk.length() <= 1600);
  }

  @Test
  public void bibliographyPenaltyDoesNotHideCitationQueries() {
    String refs = "[1] Alpha. 2012.\n[2] Beta. 2015.\n[3] Gamma. 2017.";
    assertEquals(.35, TextTools.referenceWeight("parallel computation", refs), .001);
    assertEquals(
        1, TextTools.referenceWeight("Who is cited for parallel computation?", refs), .001);
    assertEquals(
        1,
        TextTools.referenceWeight("parallel computation", "This model [1] improved in 2017."),
        .001);
  }

  @Test
  public void completeChunkKeepsLateExplanation() throws Exception {
    String text =
        "Some preliminary context. ".repeat(35)
            + "Self-attention relates positions throughout the sequence.";
    org.json.JSONObject p =
        new org.json.JSONObject()
            .put("passage_id", "a")
            .put("filename", "paper.pdf")
            .put("page", 2)
            .put("chunk", 1)
            .put("text", text);
    org.json.JSONArray out = SearchResponse.compact(new org.json.JSONArray().put(p), "sequence", 3);
    assertEquals(text, out.getJSONObject(0).getString("text"));
    assertFalse(out.getJSONObject(0).getBoolean("excerpt"));
  }
}
