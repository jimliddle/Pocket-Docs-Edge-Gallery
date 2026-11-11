package com.jimliddle.pocketdocs;

import static org.junit.Assert.*;

import org.json.*;
import org.junit.Test;

public class PassageTextTest {
  static JSONObject passage(String id, int page, String text) throws Exception {
    return new JSONObject()
        .put("passage_id", id)
        .put("filename", "Research \"notes\" α.pdf")
        .put("page", page)
        .put("chunk", 3)
        .put("text", text)
        .put("extraction", "text")
        .put("excerpt", false);
  }

  @Test
  public void completeSourceTextAndCitationSurviveFormatting() throws Exception {
    String text =
        "First sentence.\n"
            + "Detailed source material. ".repeat(40)
            + "Final sentence: self-attention relates positions across the sequence.";
    JSONArray input = new JSONArray().put(passage("id-a", 2, text));
    String before = input.toString();
    String output = PassageText.format(input);
    assertTrue(
        output.contains(
            "Filename: Research \"notes\" α.pdf\n"
                + "Page: 2\n"
                + "Citation: (Research \"notes\" α.pdf, page 2)\n"
                + "Chunk: 3\n"));
    assertTrue(output.contains("Passage ID: id-a\n"));
    assertTrue(output.contains("Text:\n" + text + "\n--- End passage 1 ---"));
    assertFalse(output.contains("Candidate passages"));
    assertFalse(output.contains("\\n"));
    assertEquals(before, input.toString());
  }

  @Test
  public void passageOrderAndWarningsArePreserved() throws Exception {
    JSONArray input =
        new JSONArray()
            .put(passage("first", 7, "Training data."))
            .put(passage("second", 1, "Abstract."))
            .put(
                passage("third", 2, "Self-attention.")
                    .put("extraction", "ocr")
                    .put("excerpt", true)
                    .put("warning", "Keyword fallback"));
    String output = PassageText.format(input);
    assertTrue(output.indexOf("Training data.") < output.indexOf("Abstract."));
    assertTrue(output.indexOf("Abstract.") < output.indexOf("Self-attention."));
    assertTrue(
        output.contains("Extraction: ocr\nShortened: yes\nRetrieval warning: Keyword fallback\n"));
  }

  @Test
  public void emptyResultsDoNotClaimInformationIsAbsentFromDocuments() throws Exception {
    assertEquals("No matching passages were returned.", PassageText.format(new JSONArray()));
  }

  @Test
  public void textFilesWithNoPageKeepFilenameAndChunk() throws Exception {
    JSONObject p =
        passage("txt", 1, "Warranty details.")
            .put("filename", "warranty.txt")
            .put("page", JSONObject.NULL);
    String output = PassageText.format(new JSONArray().put(p));
    assertFalse(output.contains("Page:"));
    assertTrue(
        output.contains("Filename: warranty.txt\nCitation: (warranty.txt, chunk 3)\nChunk: 3\n"));
  }
}
