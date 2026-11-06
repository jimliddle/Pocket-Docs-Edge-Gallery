package com.jimliddle.pocketdocs;

public final class OcrPolicy {
  public static boolean needsOcr(String text, String mode) {
    if ("off".equals(mode)) return false;
    if ("all".equals(mode)) return true;
    int letters = 0, bad = 0;
    for (int cp : text.codePoints().toArray()) {
      if (Character.isLetterOrDigit(cp)) letters++;
      if (cp == 0xfffd) bad++;
    }
    return letters < 40 || bad > Math.max(10, text.length() / 5);
  }

  public static int[] renderSize(int width, int height) {
    if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid page dimensions");
    double scale = Math.min(2.5, 2000.0 / Math.max(width, height));
    return new int[] {Math.max(1, (int) (width * scale)), Math.max(1, (int) (height * scale))};
  }
}
