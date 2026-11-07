package com.jimliddle.pocketdocs;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.net.Uri;
import java.time.Instant;
import java.util.*;
import org.json.*;

public final class Store extends SQLiteOpenHelper implements DocRepository {
  private static Store instance;
  private final Context context;
  private final EmbeddingProvider encoder;

  public static final class Passage {
    public final String doc, title, text, extraction;
    public final int page, part;

    public Passage(String d, String t, int p, int n, String b) {
      this(d, t, p, n, b, "text");
    }

    public Passage(String d, String t, int p, int n, String b, String e) {
      doc = d;
      title = t;
      page = p;
      part = n;
      text = b;
      extraction = e;
    }

    String id(String root) {
      return TextTools.hash(
          root + '\n' + doc + '\n' + title + '\n' + page + '\n' + part + '\n' + text);
    }
  }

  public static final class FileState {
    public final String fingerprint, note;

    public FileState(String f, String n) {
      fingerprint = f;
      note = n;
    }
  }

  public static final class CachedDocument {
    public final List<Passage> passages;
    public final String note;

    CachedDocument(List<Passage> p, String n) {
      passages = p;
      note = n;
    }
  }

  private Store(Context c) {
    this(c, EmbeddingEngine.get(c));
  }

  Store(Context c, EmbeddingProvider e) {
    super(c, "documents.db", null, 3);
    context = c;
    encoder = e;
  }

  public static synchronized Store get(Context c) {
    if (instance == null) instance = new Store(c.getApplicationContext());
    return instance;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE roots(uri TEXT PRIMARY KEY,name TEXT,refreshed TEXT DEFAULT '',report TEXT"
            + " DEFAULT 'Not indexed',generation TEXT)");
    db.execSQL(
        "CREATE TABLE passages(id TEXT PRIMARY KEY,root TEXT,doc TEXT,title TEXT,page INTEGER,part"
            + " INTEGER,body TEXT,indexed_at TEXT,embedding BLOB,model TEXT,extraction TEXT DEFAULT"
            + " 'text')");
    db.execSQL("CREATE INDEX passage_root ON passages(root)");
    db.execSQL("CREATE VIRTUAL TABLE passage_fts USING fts4(id,title,body,tokenize=unicode61)");
    createFiles(db);
  }

  private void createFiles(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE files(root TEXT,doc TEXT,fingerprint TEXT,note TEXT,PRIMARY KEY(root,doc))");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int old, int next) {
    if (old == 1) {
      db.execSQL("ALTER TABLE roots ADD COLUMN generation TEXT");
      db.execSQL("UPDATE roots SET generation=lower(hex(randomblob(16)))");
      db.execSQL("ALTER TABLE passages ADD COLUMN embedding BLOB");
      db.execSQL("ALTER TABLE passages ADD COLUMN model TEXT");
    }
    if (old < 3) {
      db.execSQL("ALTER TABLE passages ADD COLUMN extraction TEXT DEFAULT 'text'");
      createFiles(db);
    }
  }

  public synchronized void addRoot(String uri, String name) {
    if (roots().size() >= 8) throw new IllegalStateException("Maximum eight folders");
    ContentValues v = new ContentValues();
    v.put("uri", uri);
    v.put("name", name);
    v.put("generation", UUID.randomUUID().toString());
    getWritableDatabase().insertWithOnConflict("roots", null, v, SQLiteDatabase.CONFLICT_IGNORE);
  }

  public boolean allowed(String uri) {
    for (UriPermission p : context.getContentResolver().getPersistedUriPermissions())
      if (p.isReadPermission() && p.getUri().toString().equals(uri)) return true;
    return false;
  }

  public synchronized List<String[]> roots() {
    List<String[]> out = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT uri,name,refreshed,report,generation FROM roots ORDER BY name", null)) {
      while (c.moveToNext())
        out.add(
            new String[] {
              c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)
            });
    }
    return out;
  }

  private void deletePassages(SQLiteDatabase db, String root) {
    db.execSQL(
        "DELETE FROM passage_fts WHERE id IN(SELECT id FROM passages WHERE root=?)",
        new Object[] {root});
    db.delete("passages", "root=?", new String[] {root});
    db.delete("files", "root=?", new String[] {root});
  }

  public synchronized void removeRoot(String root) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      deletePassages(db, root);
      db.delete("roots", "uri=?", new String[] {root});
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    try {
      context
          .getContentResolver()
          .releasePersistableUriPermission(Uri.parse(root), Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (Exception ignored) {
    }
  }

  public synchronized void purgeRevoked() {
    for (String[] r : roots()) if (!allowed(r[0])) removeRoot(r[0]);
  }

  public synchronized void clearIndex() {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      db.delete("passages", null, null);
      db.delete("passage_fts", null, null);
      db.delete("files", null, null);
      db.execSQL(
          "UPDATE roots SET refreshed='',report='Index cleared; refresh to"
              + " search',generation=lower(hex(randomblob(16)))");
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public synchronized void reportFailure(String root, String message) {
    ContentValues v = new ContentValues();
    v.put("report", message + " Previous index retained; may be stale.");
    getWritableDatabase().update("roots", v, "uri=?", new String[] {root});
  }

  public synchronized CachedDocument cachedDocument(String root, String doc, String hash) {
    if (!allowed(root)) return null;
    String note;
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT note FROM files WHERE root=? AND doc=? AND fingerprint=?",
                new String[] {root, doc, hash})) {
      if (!c.moveToFirst()) return null;
      note = c.getString(0);
    }
    List<Passage> p = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT doc,title,page,part,body,extraction FROM passages WHERE root=? AND doc=?"
                    + " ORDER BY page,part",
                new String[] {root, doc})) {
      while (c.moveToNext())
        p.add(
            new Passage(
                c.getString(0),
                c.getString(1),
                c.getInt(2),
                c.getInt(3),
                c.getString(4),
                c.getString(5)));
    }
    return new CachedDocument(p, note);
  }

  private synchronized Map<String, byte[]> cache(String root) {
    Map<String, byte[]> out = new HashMap<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT id,embedding FROM passages WHERE root=? AND model=? AND embedding IS NOT"
                    + " NULL",
                new String[] {root, EmbeddingEngine.MODEL_ID})) {
      while (c.moveToNext()) out.put(c.getString(0), c.getBlob(1));
    }
    return out;
  }

  public void replaceRoot(String root, String generation, List<Passage> p, String report) {
    replaceRoot(root, generation, p, Collections.emptyMap(), report);
  }

  public void replaceRoot(
      String root,
      String generation,
      List<Passage> passages,
      Map<String, FileState> files,
      String report) {
    Map<String, byte[]> old = cache(root), vectors = new HashMap<>();
    int reused = 0, created = 0;
    boolean failed = false;
    for (Passage p : passages) {
      if (Thread.currentThread().isInterrupted())
        throw new java.util.concurrent.CancellationException();
      String id = p.id(root);
      byte[] vector = old.get(id);
      if (vector != null) reused++;
      else if (!failed) {
        try {
          vector = HybridRanker.encode(encoder.embed(p.text));
          created++;
        } catch (IllegalStateException e) {
          failed = true;
        }
      }
      if (vector != null) vectors.put(id, vector);
      DocsService.progress = "Embedding passages: " + vectors.size() + " / " + passages.size();
    }
    report +=
        " Embeddings: "
            + created
            + " new, "
            + reused
            + " reused, "
            + (passages.size() - vectors.size())
            + " missing.";
    if (failed) report += " " + encoder.error();
    synchronized (this) {
      if (Thread.currentThread().isInterrupted())
        throw new java.util.concurrent.CancellationException();
      if (!allowed(root)) return;
      SQLiteDatabase db = getWritableDatabase();
      try (Cursor c =
          db.rawQuery("SELECT generation FROM roots WHERE uri=?", new String[] {root})) {
        if (!c.moveToFirst() || !Objects.equals(generation, c.getString(0))) return;
      }
      String now = Instant.now().toString();
      db.beginTransaction();
      try {
        deletePassages(db, root);
        for (Passage p : passages) {
          String id = p.id(root);
          ContentValues v = new ContentValues();
          v.put("id", id);
          v.put("root", root);
          v.put("doc", p.doc);
          v.put("title", p.title);
          v.put("page", p.page);
          v.put("part", p.part);
          v.put("body", p.text);
          v.put("indexed_at", now);
          v.put("embedding", vectors.get(id));
          v.put("model", EmbeddingEngine.MODEL_ID);
          v.put("extraction", p.extraction);
          db.insertOrThrow("passages", null, v);
          ContentValues f = new ContentValues();
          f.put("id", id);
          f.put("title", p.title);
          f.put("body", p.text);
          db.insertOrThrow("passage_fts", null, f);
        }
        for (Map.Entry<String, FileState> e : files.entrySet()) {
          ContentValues v = new ContentValues();
          v.put("root", root);
          v.put("doc", e.getKey());
          v.put("fingerprint", e.getValue().fingerprint);
          v.put("note", e.getValue().note);
          db.insertOrThrow("files", null, v);
        }
        ContentValues v = new ContentValues();
        v.put("refreshed", now);
        v.put("report", report);
        db.update("roots", v, "uri=?", new String[] {root});
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
      }
    }
  }

  private JSONObject json(Cursor c) throws JSONException {
    return new JSONObject()
        .put("passage_id", c.getString(0))
        .put("filename", c.getString(3))
        .put("page", c.getInt(4) == 0 ? JSONObject.NULL : c.getInt(4))
        .put("chunk", c.getInt(5))
        .put("text", c.getString(6))
        .put("indexed_at", c.getString(7))
        .put("extraction", c.getString(10));
  }

  @Override
  public synchronized JSONObject passage(String id) throws JSONException {
    purgeRevoked();
    try (Cursor c =
        getReadableDatabase().rawQuery("SELECT * FROM passages WHERE id=?", new String[] {id})) {
      return c.moveToFirst() ? json(c) : null;
    }
  }

  private List<HybridRanker.Hit> lexical(String query) {
    List<HybridRanker.Hit> hits = new ArrayList<>();
    String fts = TextTools.ftsQuery(query);
    if (!fts.isEmpty())
      try (Cursor c =
          getReadableDatabase()
              .rawQuery(
                  "SELECT p.id,p.title,p.body FROM passages p JOIN passage_fts f ON p.id=f.id WHERE"
                      + " passage_fts MATCH ?",
                  new String[] {fts})) {
        while (c.moveToNext()) {
          int score = TextTools.score(query, c.getString(1), c.getString(2));
          if (score > 0)
            hits.add(
                new HybridRanker.Hit(
                    c.getString(0), score * TextTools.referenceWeight(query, c.getString(2))));
        }
      }
    return HybridRanker.sorted(hits, 30);
  }

  public synchronized JSONArray keywordSearch(String query, int limit) throws JSONException {
    purgeRevoked();
    JSONArray out = new JSONArray();
    for (HybridRanker.Hit h : HybridRanker.sorted(lexical(query), limit)) {
      JSONObject p = passage(h.id);
      if (p != null) out.put(p);
    }
    return out;
  }

  @Override
  public synchronized JSONArray search(String query, int limit) throws JSONException {
    purgeRevoked();
    List<HybridRanker.Hit> l = lexical(query), s = new ArrayList<>();
    String warning = "", mode = "keyword";
    long count =
        DatabaseUtils.longForQuery(
            getReadableDatabase(),
            "SELECT count(*) FROM passages WHERE embedding IS NOT NULL AND model=?",
            new String[] {EmbeddingEngine.MODEL_ID});
    if (count > 0)
      try {
        float[] q = encoder.embed(query);
        mode = "hybrid";
        try (Cursor c =
            getReadableDatabase()
                .rawQuery(
                    "SELECT id,embedding,body FROM passages WHERE embedding IS NOT NULL AND"
                        + " model=?",
                    new String[] {EmbeddingEngine.MODEL_ID})) {
          while (c.moveToNext()) {
            double score = HybridRanker.cosine(q, c.getBlob(1));
            if (score >= HybridRanker.MIN_COSINE)
              s.add(
                  new HybridRanker.Hit(
                      c.getString(0), score * TextTools.referenceWeight(query, c.getString(2))));
          }
        }
      } catch (IllegalStateException e) {
        warning = encoder.error();
      }
    else warning = "No embeddings available; refresh folders.";
    s = HybridRanker.sorted(s, 30);
    JSONArray out = new JSONArray();
    for (HybridRanker.Hit h : HybridRanker.fuse(l, s, Math.max(1, Math.min(30, limit)))) {
      JSONObject p = passage(h.id);
      if (p != null) {
        p.put("retrieval", mode);
        if (!warning.isEmpty()) p.put("retrieval_warning", warning);
        out.put(p);
      }
    }
    return out;
  }

  @Override
  public synchronized JSONObject status() throws JSONException {
    purgeRevoked();
    SQLiteDatabase db = getReadableDatabase();
    JSONArray folders = new JSONArray();
    for (String[] r : roots())
      folders.put(
          new JSONObject().put("folder", r[1]).put("last_refreshed", r[2]).put("coverage", r[3]));
    long total = DatabaseUtils.longForQuery(db, "SELECT count(*) FROM passages", null),
        vectors =
            DatabaseUtils.longForQuery(
                db,
                "SELECT count(*) FROM passages WHERE embedding IS NOT NULL AND model=?",
                new String[] {EmbeddingEngine.MODEL_ID});
    return new JSONObject()
        .put("folders", folders)
        .put(
            "indexed_documents",
            DatabaseUtils.longForQuery(db, "SELECT count(DISTINCT doc) FROM passages", null))
        .put("passages", total)
        .put("embedded_passages", vectors)
        .put("missing_embeddings", total - vectors)
        .put(
            "ocr_passages",
            DatabaseUtils.longForQuery(
                db, "SELECT count(*) FROM passages WHERE extraction='ocr'", null))
        .put("embedding_model", EmbeddingEngine.MODEL_ID)
        .put("embedding_warning", encoder.error())
        .put("indexing", DocsService.indexing)
        .put(
            "search_type",
            vectors > 0 ? "hybrid local semantic + keyword" : "keyword; refresh for embeddings")
        .put(
            "freshness",
            "Manual refresh. Deleted or edited files can remain until refresh. Search excerpts do"
                + " not guarantee full-document coverage.");
  }
}
