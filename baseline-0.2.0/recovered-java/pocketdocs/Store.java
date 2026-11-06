package com.jimliddle.pocketdocs;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriPermission;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import com.jimliddle.pocketdocs.HybridRanker;
import com.tom_roush.fontbox.ttf.NamingTable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import org.bouncycastle.i18n.MessageBundle;
import org.bouncycastle.i18n.TextBundle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes3.dex */
public final class Store extends SQLiteOpenHelper implements DocRepository {
    private static Store instance;
    private final Context context;
    private final EmbeddingProvider encoder;

    public static final class Passage {
        public final String doc;
        public final int page;
        public final int part;
        public final String text;
        public final String title;

        public Passage(String str, String str2, int i, int i2, String str3) {
            this.doc = str;
            this.title = str2;
            this.page = i;
            this.part = i2;
            this.text = str3;
        }

        String id(String str) {
            return TextTools.hash(str + '\n' + this.doc + '\n' + this.title + '\n' + this.page + '\n' + this.part + '\n' + this.text);
        }
    }

    private Store(Context context) {
        this(context, EmbeddingEngine.get(context));
    }

    Store(Context context, EmbeddingProvider embeddingProvider) {
        super(context, "documents.db", (SQLiteDatabase.CursorFactory) null, 2);
        this.context = context;
        this.encoder = embeddingProvider;
    }

    /* JADX WARN: Can't wrap try/catch for region: R(3:(3:29|6|32)|4|25) */
    /* JADX WARN: Code restructure failed: missing block: B:14:0x0036, code lost:
    
        r2 = th;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private synchronized java.util.Map<java.lang.String, byte[]> cache(java.lang.String r8) {
        /*
            r7 = this;
            monitor-enter(r7)
            java.util.HashMap r0 = new java.util.HashMap     // Catch: java.lang.Throwable -> L42
            r0.<init>()     // Catch: java.lang.Throwable -> L42
            android.database.sqlite.SQLiteDatabase r1 = r7.getReadableDatabase()     // Catch: java.lang.Throwable -> L42
            java.lang.String r2 = "SELECT id,embedding FROM passages WHERE root=? AND model=? AND embedding IS NOT NULL"
            r3 = 2
            java.lang.String[] r3 = new java.lang.String[r3]     // Catch: java.lang.Throwable -> L42
            r4 = 0
            r3[r4] = r8     // Catch: java.lang.Throwable -> L42
            java.lang.String r5 = "all-MiniLM-L6-v2-q8-v1"
            r6 = 1
            r3[r6] = r5     // Catch: java.lang.Throwable -> L42
            android.database.Cursor r1 = r1.rawQuery(r2, r3)     // Catch: java.lang.Throwable -> L42
        L1b:
            boolean r2 = r1.moveToNext()     // Catch: java.lang.Throwable -> L36
            if (r2 == 0) goto L2f
            java.lang.String r2 = r1.getString(r4)     // Catch: java.lang.Throwable -> L2d
            byte[] r3 = r1.getBlob(r6)     // Catch: java.lang.Throwable -> L2d
            r0.put(r2, r3)     // Catch: java.lang.Throwable -> L2d
            goto L1b
        L2d:
            r2 = move-exception
            goto L37
        L2f:
            if (r1 == 0) goto L34
            r1.close()     // Catch: java.lang.Throwable -> L42
        L34:
            monitor-exit(r7)
            return r0
        L36:
            r2 = move-exception
        L37:
            if (r1 == 0) goto L41
            r1.close()     // Catch: java.lang.Throwable -> L3d
            goto L41
        L3d:
            r3 = move-exception
            r2.addSuppressed(r3)     // Catch: java.lang.Throwable -> L42
        L41:
            throw r2     // Catch: java.lang.Throwable -> L42
        L42:
            r8 = move-exception
            monitor-exit(r7)     // Catch: java.lang.Throwable -> L42
            throw r8
        */
        throw new UnsupportedOperationException("Method not decompiled: com.jimliddle.pocketdocs.Store.cache(java.lang.String):java.util.Map");
    }

    private void deletePassages(SQLiteDatabase sQLiteDatabase, String str) throws SQLException {
        sQLiteDatabase.execSQL("DELETE FROM passage_fts WHERE id IN (SELECT id FROM passages WHERE root=?)", new Object[]{str});
        sQLiteDatabase.delete("passages", "root=?", new String[]{str});
    }

    public static synchronized Store get(Context context) {
        if (instance == null) {
            instance = new Store(context.getApplicationContext());
        }
        return instance;
    }

    private JSONObject json(Cursor cursor) throws JSONException {
        return new JSONObject().put("passage_id", cursor.getString(0)).put("filename", cursor.getString(3)).put("page", cursor.getInt(4) == 0 ? JSONObject.NULL : Integer.valueOf(cursor.getInt(4))).put("chunk", cursor.getInt(5)).put(TextBundle.TEXT_ENTRY, cursor.getString(6)).put("indexed_at", cursor.getString(7));
    }

    public synchronized void addRoot(String str, String str2) {
        if (roots().size() >= 8) {
            throw new IllegalStateException("Maximum eight folders");
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("uri", str);
        contentValues.put(NamingTable.TAG, str2);
        contentValues.put("generation", UUID.randomUUID().toString());
        getWritableDatabase().insertWithOnConflict("roots", null, contentValues, 4);
    }

    public boolean allowed(String str) {
        for (UriPermission uriPermission : this.context.getContentResolver().getPersistedUriPermissions()) {
            if (uriPermission.isReadPermission() && uriPermission.getUri().toString().equals(str)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void clearIndex() {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            writableDatabase.delete("passage_fts", null, null);
            writableDatabase.delete("passages", null, null);
            writableDatabase.execSQL("UPDATE roots SET refreshed='',report='Index cleared; refresh to search',generation=lower(hex(randomblob(16)))");
            writableDatabase.setTransactionSuccessful();
        } finally {
            writableDatabase.endTransaction();
        }
    }

    public synchronized JSONArray keywordSearch(String str, int i) throws JSONException {
        JSONArray jSONArray;
        purgeRevoked();
        String strFtsQuery = TextTools.ftsQuery(str);
        ArrayList arrayList = new ArrayList();
        if (!strFtsQuery.isEmpty()) {
            Cursor cursorRawQuery = getReadableDatabase().rawQuery("SELECT p.id,p.title,p.body FROM passages p JOIN passage_fts f ON p.id=f.id WHERE passage_fts MATCH ?", new String[]{strFtsQuery});
            while (cursorRawQuery.moveToNext()) {
                try {
                    try {
                        arrayList.add(new HybridRanker.Hit(cursorRawQuery.getString(0), TextTools.score(str, cursorRawQuery.getString(1), cursorRawQuery.getString(2))));
                    } catch (Throwable th) {
                        th = th;
                        if (cursorRawQuery != null) {
                            try {
                                cursorRawQuery.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
            if (cursorRawQuery != null) {
                cursorRawQuery.close();
            }
        }
        jSONArray = new JSONArray();
        Iterator<HybridRanker.Hit> it = HybridRanker.sorted(arrayList, i).iterator();
        while (it.hasNext()) {
            JSONObject jSONObjectPassage = passage(it.next().id);
            if (jSONObjectPassage != null) {
                jSONArray.put(jSONObjectPassage);
            }
        }
        return jSONArray;
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase sQLiteDatabase) throws SQLException {
        sQLiteDatabase.execSQL("CREATE TABLE roots(uri TEXT PRIMARY KEY,name TEXT,refreshed TEXT DEFAULT '',report TEXT DEFAULT 'Not indexed',generation TEXT)");
        sQLiteDatabase.execSQL("CREATE TABLE passages(id TEXT PRIMARY KEY,root TEXT,doc TEXT,title TEXT,page INTEGER,part INTEGER,body TEXT,indexed_at TEXT,embedding BLOB,model TEXT)");
        sQLiteDatabase.execSQL("CREATE INDEX passage_root ON passages(root)");
        sQLiteDatabase.execSQL("CREATE VIRTUAL TABLE passage_fts USING fts4(id,title,body,tokenize=unicode61)");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) throws SQLException {
        if (i == 1) {
            sQLiteDatabase.execSQL("ALTER TABLE roots ADD COLUMN generation TEXT");
            sQLiteDatabase.execSQL("UPDATE roots SET generation=lower(hex(randomblob(16)))");
            sQLiteDatabase.execSQL("ALTER TABLE passages ADD COLUMN embedding BLOB");
            sQLiteDatabase.execSQL("ALTER TABLE passages ADD COLUMN model TEXT");
        }
    }

    @Override // com.jimliddle.pocketdocs.DocRepository
    public synchronized JSONObject passage(String str) throws JSONException {
        JSONObject jSONObjectJson;
        purgeRevoked();
        Cursor cursorRawQuery = getReadableDatabase().rawQuery("SELECT * FROM passages WHERE id=?", new String[]{str});
        try {
            if (cursorRawQuery.moveToFirst()) {
                try {
                    jSONObjectJson = json(cursorRawQuery);
                } catch (Throwable th) {
                    th = th;
                    if (cursorRawQuery != null) {
                        try {
                            cursorRawQuery.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } else {
                jSONObjectJson = null;
            }
            if (cursorRawQuery != null) {
                cursorRawQuery.close();
            }
        } catch (Throwable th3) {
            th = th3;
        }
        return jSONObjectJson;
    }

    public synchronized void purgeRevoked() {
        for (String[] strArr : roots()) {
            if (!allowed(strArr[0])) {
                removeRoot(strArr[0]);
            }
        }
    }

    public synchronized void removeRoot(String str) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            deletePassages(writableDatabase, str);
            writableDatabase.delete("roots", "uri=?", new String[]{str});
            writableDatabase.setTransactionSuccessful();
            try {
                this.context.getContentResolver().releasePersistableUriPermission(Uri.parse(str), 1);
            } catch (Exception e) {
            }
        } finally {
            writableDatabase.endTransaction();
        }
    }

    public void replaceRoot(String str, String str2, List<Passage> list, String str3) {
        Map<String, byte[]> mapCache = cache(str);
        HashMap map = new HashMap();
        boolean z = false;
        int i = 0;
        int i2 = 0;
        for (Passage passage : list) {
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException();
            }
            String strId = passage.id(str);
            byte[] bArrEncode = mapCache.get(strId);
            if (bArrEncode != null) {
                i2++;
            } else if (!z) {
                try {
                    bArrEncode = HybridRanker.encode(this.encoder.embed(passage.text));
                    i++;
                } catch (IllegalStateException e) {
                    z = true;
                }
            }
            if (bArrEncode != null) {
                map.put(strId, bArrEncode);
            }
            DocsService.progress = "Embedding passages: " + map.size() + " / " + list.size();
        }
        String str4 = str3 + " Embeddings: " + i + " new, " + i2 + " reused, " + (list.size() - map.size()) + " missing.";
        if (z) {
            str4 = str4 + " " + this.encoder.error();
        }
        String str5 = str4;
        synchronized (this) {
            try {
                try {
                } catch (Throwable th) {
                    th = th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException();
            }
            if (allowed(str)) {
                SQLiteDatabase writableDatabase = getWritableDatabase();
                Cursor cursorRawQuery = writableDatabase.rawQuery("SELECT generation FROM roots WHERE uri=?", new String[]{str});
                try {
                    if (cursorRawQuery.moveToFirst()) {
                        try {
                            if (Objects.equals(str2, cursorRawQuery.getString(0))) {
                                if (cursorRawQuery != null) {
                                    try {
                                        cursorRawQuery.close();
                                    } catch (Throwable th3) {
                                        th = th3;
                                    }
                                }
                                try {
                                    String string = Instant.now().toString();
                                    writableDatabase.beginTransaction();
                                    try {
                                        deletePassages(writableDatabase, str);
                                        Iterator<Passage> it = list.iterator();
                                        while (it.hasNext()) {
                                            Passage next = it.next();
                                            String strId2 = next.id(str);
                                            ContentValues contentValues = new ContentValues();
                                            contentValues.put("id", strId2);
                                            contentValues.put("root", str);
                                            Iterator<Passage> it2 = it;
                                            contentValues.put("doc", next.doc);
                                            contentValues.put(MessageBundle.TITLE_ENTRY, next.title);
                                            contentValues.put("page", Integer.valueOf(next.page));
                                            contentValues.put("part", Integer.valueOf(next.part));
                                            contentValues.put("body", next.text);
                                            contentValues.put("indexed_at", string);
                                            contentValues.put("embedding", (byte[]) map.get(strId2));
                                            contentValues.put("model", EmbeddingEngine.MODEL_ID);
                                            writableDatabase.insertOrThrow("passages", null, contentValues);
                                            ContentValues contentValues2 = new ContentValues();
                                            contentValues2.put("id", strId2);
                                            Map<String, byte[]> map2 = mapCache;
                                            try {
                                                contentValues2.put(MessageBundle.TITLE_ENTRY, next.title);
                                                contentValues2.put("body", next.text);
                                                writableDatabase.insertOrThrow("passage_fts", null, contentValues2);
                                                it = it2;
                                                mapCache = map2;
                                            } catch (Throwable th4) {
                                                th = th4;
                                                writableDatabase.endTransaction();
                                                throw th;
                                            }
                                        }
                                        ContentValues contentValues3 = new ContentValues();
                                        contentValues3.put("refreshed", string);
                                        contentValues3.put("report", str5);
                                        writableDatabase.update("roots", contentValues3, "uri=?", new String[]{str});
                                        writableDatabase.setTransactionSuccessful();
                                        writableDatabase.endTransaction();
                                        return;
                                    } catch (Throwable th5) {
                                        th = th5;
                                    }
                                } catch (Throwable th6) {
                                    th = th6;
                                    throw th;
                                }
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            Throwable th8 = th;
                            if (cursorRawQuery == null) {
                                throw th8;
                            }
                            try {
                                cursorRawQuery.close();
                                throw th8;
                            } catch (Throwable th9) {
                                th8.addSuppressed(th9);
                                throw th8;
                            }
                        }
                    }
                    if (cursorRawQuery != null) {
                        cursorRawQuery.close();
                    }
                    return;
                } catch (Throwable th10) {
                    th = th10;
                }
            } else {
                try {
                    return;
                } catch (Throwable th11) {
                    th = th11;
                }
            }
            throw th;
        }
    }

    public synchronized void reportFailure(String str, String str2) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("report", str2 + " Previous index retained; may be stale.");
        getWritableDatabase().update("roots", contentValues, "uri=?", new String[]{str});
    }

    public synchronized List<String[]> roots() {
        ArrayList arrayList;
        arrayList = new ArrayList();
        Cursor cursorRawQuery = getReadableDatabase().rawQuery("SELECT uri,name,refreshed,report,generation FROM roots ORDER BY name", null);
        while (cursorRawQuery.moveToNext()) {
            try {
                try {
                    arrayList.add(new String[]{cursorRawQuery.getString(0), cursorRawQuery.getString(1), cursorRawQuery.getString(2), cursorRawQuery.getString(3), cursorRawQuery.getString(4)});
                } catch (Throwable th) {
                    th = th;
                    if (cursorRawQuery != null) {
                        try {
                            cursorRawQuery.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
        if (cursorRawQuery != null) {
            cursorRawQuery.close();
        }
        return arrayList;
    }

    /* JADX WARN: Removed duplicated region for block: B:69:0x011b A[Catch: all -> 0x01d8, LOOP:1: B:67:0x0115->B:69:0x011b, LOOP_END, TryCatch #5 {, blocks: (B:4:0x0005, B:6:0x0012, B:9:0x0019, B:11:0x0037, B:18:0x0065, B:28:0x0076, B:27:0x0073, B:29:0x0077, B:32:0x008f, B:34:0x0099, B:58:0x00ed, B:57:0x00ea, B:48:0x00d8, B:66:0x0102, B:67:0x0115, B:69:0x011b, B:70:0x0133, B:71:0x013e, B:73:0x0144, B:74:0x0150, B:75:0x016a, B:77:0x0170, B:80:0x0183, B:82:0x0199, B:87:0x01a9, B:89:0x01b4, B:90:0x01bf, B:92:0x01c5, B:93:0x01ca, B:64:0x00f7, B:23:0x006d, B:13:0x0042, B:15:0x0048), top: B:109:0x0005, inners: #1, #6 }] */
    /* JADX WARN: Removed duplicated region for block: B:73:0x0144 A[Catch: all -> 0x01d8, LOOP:2: B:71:0x013e->B:73:0x0144, LOOP_END, TryCatch #5 {, blocks: (B:4:0x0005, B:6:0x0012, B:9:0x0019, B:11:0x0037, B:18:0x0065, B:28:0x0076, B:27:0x0073, B:29:0x0077, B:32:0x008f, B:34:0x0099, B:58:0x00ed, B:57:0x00ea, B:48:0x00d8, B:66:0x0102, B:67:0x0115, B:69:0x011b, B:70:0x0133, B:71:0x013e, B:73:0x0144, B:74:0x0150, B:75:0x016a, B:77:0x0170, B:80:0x0183, B:82:0x0199, B:87:0x01a9, B:89:0x01b4, B:90:0x01bf, B:92:0x01c5, B:93:0x01ca, B:64:0x00f7, B:23:0x006d, B:13:0x0042, B:15:0x0048), top: B:109:0x0005, inners: #1, #6 }] */
    /* JADX WARN: Removed duplicated region for block: B:77:0x0170 A[Catch: all -> 0x01d8, TryCatch #5 {, blocks: (B:4:0x0005, B:6:0x0012, B:9:0x0019, B:11:0x0037, B:18:0x0065, B:28:0x0076, B:27:0x0073, B:29:0x0077, B:32:0x008f, B:34:0x0099, B:58:0x00ed, B:57:0x00ea, B:48:0x00d8, B:66:0x0102, B:67:0x0115, B:69:0x011b, B:70:0x0133, B:71:0x013e, B:73:0x0144, B:74:0x0150, B:75:0x016a, B:77:0x0170, B:80:0x0183, B:82:0x0199, B:87:0x01a9, B:89:0x01b4, B:90:0x01bf, B:92:0x01c5, B:93:0x01ca, B:64:0x00f7, B:23:0x006d, B:13:0x0042, B:15:0x0048), top: B:109:0x0005, inners: #1, #6 }] */
    @Override // com.jimliddle.pocketdocs.DocRepository
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public synchronized org.json.JSONArray search(java.lang.String r22, int r23) throws org.json.JSONException {
        /*
            Method dump skipped, instructions count: 475
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.jimliddle.pocketdocs.Store.search(java.lang.String, int):org.json.JSONArray");
    }

    @Override // com.jimliddle.pocketdocs.DocRepository
    public synchronized JSONObject status() throws JSONException {
        SQLiteDatabase readableDatabase;
        JSONArray jSONArray;
        long jLongForQuery;
        long jLongForQuery2;
        purgeRevoked();
        readableDatabase = getReadableDatabase();
        jSONArray = new JSONArray();
        for (String[] strArr : roots()) {
            jSONArray.put(new JSONObject().put("folder", strArr[1]).put("last_refreshed", strArr[2]).put("coverage", strArr[3]));
        }
        jLongForQuery = DatabaseUtils.longForQuery(readableDatabase, "SELECT count(*) FROM passages", null);
        jLongForQuery2 = DatabaseUtils.longForQuery(readableDatabase, "SELECT count(*) FROM passages WHERE embedding IS NOT NULL AND model=?", new String[]{EmbeddingEngine.MODEL_ID});
        return new JSONObject().put("folders", jSONArray).put("indexed_documents", DatabaseUtils.longForQuery(readableDatabase, "SELECT count(DISTINCT doc) FROM passages", null)).put("passages", jLongForQuery).put("embedded_passages", jLongForQuery2).put("missing_embeddings", jLongForQuery - jLongForQuery2).put("embedding_model", EmbeddingEngine.MODEL_ID).put("embedding_warning", this.encoder.error()).put("indexing", DocsService.indexing).put("search_type", jLongForQuery2 > 0 ? "hybrid: local semantic embeddings plus keyword retrieval" : "keyword only; refresh to generate embeddings").put("freshness", "Manual refresh only. Changed or deleted files may remain until refresh. Citations identify retrieved passages, not full-document coverage.");
    }
}
