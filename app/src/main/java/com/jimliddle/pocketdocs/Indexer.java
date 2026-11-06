package com.jimliddle.pocketdocs;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.documentfile.provider.DocumentFile;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CancellationException;

public final class Indexer {
  private final Context ctx;
  private final Store store;
  private final String mode;

  public Indexer(Context c) {
    ctx = c;
    store = Store.get(c);
    mode = c.getSharedPreferences("settings", 0).getString("ocr", "auto");
  }

  private void check() {
    if (Thread.currentThread().isInterrupted()) throw new CancellationException();
  }

  private void issue(List<String> notes, String text) {
    if (notes.size() < 30) notes.add(text);
  }

  private List<DocumentFile> children(DocumentFile folder) throws IOException {
    List<DocumentFile> result = new ArrayList<>();
    Uri u = folder.getUri();
    try (Cursor c =
        ctx.getContentResolver()
            .query(
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    u, DocumentsContract.getDocumentId(u)),
                new String[] {DocumentsContract.Document.COLUMN_DOCUMENT_ID},
                null,
                null,
                null)) {
      if (c == null) throw new IOException("Folder listing unavailable");
      while (c.moveToNext()) {
        check();
        DocumentFile f =
            DocumentFile.fromSingleUri(
                ctx, DocumentsContract.buildDocumentUriUsingTree(u, c.getString(0)));
        if (f != null) result.add(f);
        if (result.size() > 2000) break;
      }
    }
    return result;
  }

  private void add(
      List<Store.Passage> out, String doc, String name, int page, String text, String extraction) {
    int part = 1;
    for (String chunk : TextTools.chunks(text)) {
      check();
      if (out.size() >= 5000) return;
      out.add(new Store.Passage(doc, name, page, part++, chunk, extraction));
    }
  }

  private void walk(
      DocumentFile folder,
      String root,
      String prefix,
      List<Store.Passage> out,
      Map<String, Store.FileState> files,
      List<String> notes,
      int[] counts,
      int depth)
      throws Exception {
    if (depth > 12) {
      issue(notes, "Subfolder depth limit reached");
      return;
    }
    for (DocumentFile file : children(folder)) {
      check();
      if (++counts[0] > 2000) {
        issue(notes, "Entry limit reached (2,000)");
        return;
      }
      if (out.size() >= 5000) return;
      String name = file.getName();
      if (name == null) name = "Unnamed document";
      String title = prefix + name;
      if (file.isDirectory()) {
        walk(file, root, title + "/", out, files, notes, counts, depth + 1);
        continue;
      }
      String lower = name.toLowerCase(Locale.ROOT);
      boolean pdf = lower.endsWith(".pdf");
      if (!pdf
          && !lower.endsWith(".txt")
          && !lower.endsWith(".md")
          && !lower.endsWith(".markdown")) {
        counts[2]++;
        continue;
      }
      int start = out.size();
      File temp = null;
      String docId = file.getUri().toString();
      try {
        if (file.length() > 20L * 1024 * 1024) throw new IOException("File exceeds 20 MiB");
        temp = File.createTempFile("extract-", ".tmp", ctx.getCacheDir());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = ctx.getContentResolver().openInputStream(file.getUri());
            OutputStream target = new FileOutputStream(temp)) {
          if (in == null) throw new IOException("Cannot read file");
          byte[] b = new byte[16384];
          int n;
          long total = 0;
          while ((n = in.read(b)) != -1) {
            check();
            total += n;
            if (total > 20L * 1024 * 1024) throw new IOException("File exceeds 20 MiB");
            target.write(b, 0, n);
            digest.update(b, 0, n);
          }
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) hex.append(String.format(Locale.ROOT, "%02x", b & 255));
        String fingerprint = TextTools.hash(hex + "\n" + title + "\nocr-v1:" + mode);
        Store.CachedDocument cached = store.cachedDocument(root, docId, fingerprint);
        if (cached != null && out.size() + cached.passages.size() <= 5000) {
          out.addAll(cached.passages);
          files.put(docId, new Store.FileState(fingerprint, cached.note));
          counts[1]++;
          counts[3]++;
          if (!cached.note.isEmpty()) issue(notes, cached.note);
          continue;
        }
        List<String> fileNotes = new ArrayList<>();
        boolean cacheable = true;
        DocsService.progress = "Reading " + title;
        if (pdf) {
          PdfOcr ocr = null;
          try (PDDocument doc =
              PDDocument.load(
                  temp, MemoryUsageSetting.setupTempFileOnly().setTempDir(ctx.getCacheDir()))) {
            if (!doc.getCurrentAccessPermission().canExtractContent())
              throw new IOException("PDF disallows extraction");
            int pages = Math.min(200, doc.getNumberOfPages()), chars = 0, blank = 0, ocrPages = 0;
            if (doc.getNumberOfPages() > 200) issue(fileNotes, title + ": first 200 pages only");
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= pages && out.size() < 5000; page++) {
              check();
              stripper.setStartPage(page);
              stripper.setEndPage(page);
              String text = stripper.getText(doc);
              String extraction = "text";
              if (OcrPolicy.needsOcr(text, mode)) {
                DocsService.progress = "OCR " + title + " · page " + page + " / " + pages;
                try {
                  if (ocr == null) ocr = new PdfOcr(temp);
                  String recognized = ocr.page(page);
                  check();
                  if (!recognized.trim().isEmpty()) {
                    text = recognized;
                    extraction = "ocr";
                    ocrPages++;
                  } else {
                    issue(fileNotes, title + " page " + page + ": OCR found no text");
                    cacheable = false;
                  }
                } catch (CancellationException e) {
                  throw e;
                } catch (Exception | LinkageError e) {
                  check();
                  cacheable = false;
                  issue(
                      fileNotes,
                      title
                          + " page "
                          + page
                          + ": OCR failed ("
                          + e.getClass().getSimpleName()
                          + ")");
                }
              }
              if (text.trim().isEmpty()) {
                blank++;
                continue;
              }
              int available = 1_000_000 - chars;
              boolean truncated = text.length() > available;
              if (truncated) text = text.substring(0, available);
              chars += text.length();
              add(out, docId, title, page, text, extraction);
              if (truncated || chars >= 1_000_000) {
                issue(fileNotes, title + ": first 1,000,000 characters only");
                break;
              }
            }
            if (blank > 0) issue(fileNotes, title + ": " + blank + " pages with no readable text");
            if (ocrPages > 0) issue(fileNotes, title + ": " + ocrPages + " pages read with OCR");
          } finally {
            if (ocr != null) ocr.close();
          }
        } else {
          String text =
              StandardCharsets.UTF_8
                  .newDecoder()
                  .onMalformedInput(CodingErrorAction.REPORT)
                  .decode(ByteBuffer.wrap(Files.readAllBytes(temp.toPath())))
                  .toString();
          if (text.length() > 1_000_000) {
            text = text.substring(0, 1_000_000);
            issue(fileNotes, title + ": first 1,000,000 characters only");
          }
          add(out, docId, title, 0, text, "text");
        }
        if (out.size() > start) {
          counts[1]++;
          String note = String.join("; ", fileNotes);
          if (cacheable && out.size() < 5000)
            files.put(docId, new Store.FileState(fingerprint, note));
        } else {
          counts[2]++;
          issue(fileNotes, title + ": no text extracted");
        }
        for (String note : fileNotes) issue(notes, note);
      } catch (CancellationException e) {
        throw e;
      } catch (Exception e) {
        while (out.size() > start) out.remove(out.size() - 1);
        counts[2]++;
        issue(
            notes,
            title
                + ": "
                + e.getClass().getSimpleName()
                + " (unreadable, encrypted, unsupported or over limit)");
      } finally {
        if (temp != null) temp.delete();
      }
    }
  }

  public void refresh() {
    store.purgeRevoked();
    for (String[] root : store.roots()) {
      check();
      try {
        DocumentFile folder = DocumentFile.fromTreeUri(ctx, Uri.parse(root[0]));
        if (folder == null || !folder.canRead()) throw new IOException("Folder unreadable");
        List<Store.Passage> passages = new ArrayList<>();
        Map<String, Store.FileState> files = new HashMap<>();
        List<String> notes = new ArrayList<>();
        int[] counts = {0, 0, 0, 0};
        walk(folder, root[0], "", passages, files, notes, counts, 0);
        check();
        if (passages.size() >= 5000) issue(notes, "Passage limit reached (5,000)");
        String report =
            counts[1]
                + " indexed files; "
                + counts[2]
                + " skipped/failed. "
                + counts[3]
                + " files reused without extraction. "
                + passages.size()
                + " passages.";
        if (!notes.isEmpty()) report += " Issues: " + String.join("; ", notes);
        store.replaceRoot(root[0], root[4], passages, files, report);
      } catch (CancellationException e) {
        throw e;
      } catch (Exception e) {
        store.reportFailure(root[0], "Refresh failed (" + e.getClass().getSimpleName() + ").");
      }
    }
  }
}
