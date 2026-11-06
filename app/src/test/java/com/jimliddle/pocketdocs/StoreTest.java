package com.jimliddle.pocketdocs;

import static org.junit.Assert.*;

import android.content.*;
import android.net.Uri;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class StoreTest {
  Context c;
  Store store;
  String root = "content://docs/tree/test";
  int embeds;

  @Before
  public void setup() {
    c = RuntimeEnvironment.getApplication();
    c.deleteDatabase("documents.db");
    c.getContentResolver()
        .takePersistableUriPermission(Uri.parse(root), Intent.FLAG_GRANT_READ_URI_PERMISSION);
    store =
        new Store(
            c,
            new EmbeddingProvider() {
              public float[] embed(String s) {
                embeds++;
                return new float[] {1, 0, 0};
              }

              public String error() {
                return "";
              }
            });
    store.addRoot(root, "Test");
  }

  @After
  public void close() {
    store.close();
    c.getContentResolver()
        .releasePersistableUriPermission(Uri.parse(root), Intent.FLAG_GRANT_READ_URI_PERMISSION);
  }

  void insert(String fingerprint) {
    store.replaceRoot(
        root,
        store.roots().get(0)[4],
        Collections.singletonList(
            new Store.Passage(
                "doc", "scan.pdf", 3, 1, "The warranty covers hinge repairs.", "ocr")),
        Collections.singletonMap("doc", new Store.FileState(fingerprint, "1 OCR page")),
        "OK");
  }

  @Test
  public void extractionCacheAndEmbeddingsAreReused() throws Exception {
    insert("file-v1-mode-auto");
    Store.CachedDocument cached = store.cachedDocument(root, "doc", "file-v1-mode-auto");
    assertNotNull(cached);
    assertEquals("ocr", cached.passages.get(0).extraction);
    assertEquals("1 OCR page", cached.note);
    assertEquals(1, embeds);
    insert("file-v1-mode-auto");
    assertEquals(1, embeds);
    assertNull(store.cachedDocument(root, "doc", "file-v2-mode-auto"));
    assertNull(store.cachedDocument(root, "doc", "file-v1-mode-all"));
    assertEquals(1, store.status().getInt("ocr_passages"));
  }

  @Test
  public void removedFolderLeavesNoResults() throws Exception {
    insert("a");
    store.removeRoot(root);
    assertNull(store.cachedDocument(root, "doc", "a"));
    assertEquals(0, store.search("warranty", 2).length());
    assertEquals(0, store.status().getInt("passages"));
  }

  @Test
  public void clearPreventsOldRefreshFromRestoringData() throws Exception {
    String generation = store.roots().get(0)[4];
    insert("a");
    store.clearIndex();
    store.replaceRoot(
        root,
        generation,
        Collections.singletonList(new Store.Passage("doc", "a.pdf", 1, 1, "old contents")),
        "old");
    assertEquals(0, store.status().getInt("passages"));
    assertNull(store.cachedDocument(root, "doc", "a"));
  }
}
