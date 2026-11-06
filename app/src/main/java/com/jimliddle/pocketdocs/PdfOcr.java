package com.jimliddle.pocketdocs;

import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.*;
import java.util.concurrent.*;

/** Bundled Latin-script OCR. Page bitmaps are bounded and never sent to a remote OCR service. */
public final class PdfOcr implements AutoCloseable {
  private final ParcelFileDescriptor descriptor;
  private final PdfRenderer renderer;
  private final TextRecognizer recognizer;

  public PdfOcr(File file) throws IOException {
    descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    try {
      renderer = new PdfRenderer(descriptor);
    } catch (IOException | RuntimeException e) {
      descriptor.close();
      throw e;
    }
    recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
  }

  public String page(int number) throws Exception {
    try (PdfRenderer.Page page = renderer.openPage(number - 1)) {
      int[] size = OcrPolicy.renderSize(page.getWidth(), page.getHeight());
      Bitmap bitmap = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888);
      bitmap.eraseColor(Color.WHITE);
      Task<Text> task = null;
      try {
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        task = recognizer.process(InputImage.fromBitmap(bitmap, 0));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        while (true) {
          if (Thread.currentThread().isInterrupted()) throw new CancellationException();
          try {
            return Tasks.await(task, 1, TimeUnit.SECONDS).getText();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CancellationException();
          } catch (TimeoutException e) {
            if (System.nanoTime() > deadline) throw new IOException("OCR timed out");
          }
        }
      } finally {
        if (task == null || task.isComplete()) bitmap.recycle();
        else task.addOnCompleteListener(Runnable::run, t -> bitmap.recycle());
      }
    }
  }

  public void close() {
    recognizer.close();
    renderer.close();
    try {
      descriptor.close();
    } catch (IOException ignored) {
    }
  }
}
