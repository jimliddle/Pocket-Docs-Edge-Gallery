package com.jimliddle.pocketdocs;

import static org.junit.Assert.*;

import org.junit.Test;

public class OcrPolicyTest {
  @Test
  public void blankAndSparsePagesUseOcr() {
    assertTrue(OcrPolicy.needsOcr("", "auto"));
    assertTrue(OcrPolicy.needsOcr("Page 3", "auto"));
  }

  @Test
  public void digitalTextStaysNative() {
    assertFalse(
        OcrPolicy.needsOcr(
            "Readable digital text with enough words to avoid recognition of an already searchable"
                + " page.",
            "auto"));
  }

  @Test
  public void explicitModesOverrideDetection() {
    assertFalse(OcrPolicy.needsOcr("", "off"));
    assertTrue(OcrPolicy.needsOcr("Normal text ".repeat(30), "all"));
  }

  @Test
  public void corruptTextUsesOcr() {
    assertTrue(OcrPolicy.needsOcr("Readable ".repeat(10) + "\ufffd".repeat(50), "auto"));
  }

  @Test
  public void bitmapMemoryIsBounded() {
    for (int[] p : new int[][] {{612, 792}, {10000, 20000}, {100, 100}}) {
      int[] s = OcrPolicy.renderSize(p[0], p[1]);
      assertTrue(s[0] <= 2000 && s[1] <= 2000);
      assertTrue(s[0] > 0 && s[1] > 0);
    }
  }
}
