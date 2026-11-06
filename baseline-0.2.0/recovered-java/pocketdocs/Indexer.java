package com.jimliddle.pocketdocs;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.documentfile.provider.DocumentFile;
import com.jimliddle.pocketdocs.Store;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

/* loaded from: classes3.dex */
public final class Indexer {
    private final Context ctx;
    private final Store store;

    public Indexer(Context context) {
        this.ctx = context;
        this.store = Store.get(context);
    }

    private void add(List<Store.Passage> list, String str, String str2, int i, String str3) {
        int i2 = 1;
        for (String str4 : TextTools.chunks(str3)) {
            check();
            if (list.size() >= 5000) {
                return;
            }
            list.add(new Store.Passage(str, str2, i, i2, str4));
            i2++;
        }
    }

    private void check() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException();
        }
    }

    private List<DocumentFile> children(DocumentFile documentFile) throws IOException {
        ArrayList arrayList = new ArrayList();
        Uri uri = documentFile.getUri();
        Cursor cursorQuery = this.ctx.getContentResolver().query(DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri)), new String[]{"document_id"}, null, null, null);
        try {
            if (cursorQuery == null) {
                throw new IOException("Folder listing unavailable");
            }
            while (cursorQuery.moveToNext()) {
                check();
                DocumentFile documentFileFromSingleUri = DocumentFile.fromSingleUri(this.ctx, DocumentsContract.buildDocumentUriUsingTree(uri, cursorQuery.getString(0)));
                if (documentFileFromSingleUri != null) {
                    arrayList.add(documentFileFromSingleUri);
                }
                if (arrayList.size() > 2000) {
                    break;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return arrayList;
        } catch (Throwable th) {
            if (cursorQuery != null) {
                try {
                    cursorQuery.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private void issue(List<String> list, String str) {
        if (list.size() < 30) {
            list.add(str);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:101:0x0249, code lost:
    
        if (r4 < 1000000) goto L103;
     */
    /* JADX WARN: Code restructure failed: missing block: B:103:0x024c, code lost:
    
        r3 = r10;
        r2 = r4;
     */
    /* JADX WARN: Code restructure failed: missing block: B:104:0x024e, code lost:
    
        r1 = r21 + 1;
        r6 = r15;
        r4 = r19;
        r14 = r24;
        r5 = r28;
        r15 = r29;
        r24 = r12;
        r12 = r26;
     */
    /* JADX WARN: Code restructure failed: missing block: B:105:0x0261, code lost:
    
        issue(r35, r15 + ": first 1,000,000 characters only");
     */
    /* JADX WARN: Code restructure failed: missing block: B:106:0x0277, code lost:
    
        r10 = r3;
        r28 = r5;
        r26 = r12;
        r12 = r24;
        r15 = r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:107:0x0286, code lost:
    
        r10 = r3;
        r28 = r5;
        r26 = r12;
        r12 = r24;
        r15 = r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:108:0x0296, code lost:
    
        if (r10 <= 0) goto L110;
     */
    /* JADX WARN: Code restructure failed: missing block: B:109:0x0298, code lost:
    
        issue(r35, r15 + ": " + r10 + " pages without extractable text; OCR is not included");
     */
    /* JADX WARN: Code restructure failed: missing block: B:110:0x02b6, code lost:
    
        if (r28 == null) goto L137;
     */
    /* JADX WARN: Code restructure failed: missing block: B:111:0x02b8, code lost:
    
        r28.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:113:0x02bd, code lost:
    
        r28 = r5;
     */
    /* JADX WARN: Code restructure failed: missing block: B:115:0x02cd, code lost:
    
        throw new java.io.IOException("PDF disallows extraction");
     */
    /* JADX WARN: Code restructure failed: missing block: B:116:0x02ce, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:117:0x02cf, code lost:
    
        r1 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:118:0x02d1, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:119:0x02d2, code lost:
    
        r28 = r5;
        r1 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:120:0x02dc, code lost:
    
        if (r28 == null) goto L281;
     */
    /* JADX WARN: Code restructure failed: missing block: B:121:0x02de, code lost:
    
        r28.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:123:0x02e2, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:125:0x02e4, code lost:
    
        r1.addSuppressed(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:126:0x02e7, code lost:
    
        throw r1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:127:0x02e8, code lost:
    
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:128:0x02e9, code lost:
    
        r1 = r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:129:0x02f3, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:130:0x02f4, code lost:
    
        r26 = r12;
        r12 = r24;
        r15 = r6;
        r1 = r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:131:0x02fe, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:133:0x0309, code lost:
    
        r26 = r12;
        r12 = r24;
        r15 = r6;
        r1 = java.nio.charset.StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(java.nio.file.Files.readAllBytes(r2.toPath()))).toString();
     */
    /* JADX WARN: Code restructure failed: missing block: B:134:0x0337, code lost:
    
        if (r1.length() <= 1000000) goto L136;
     */
    /* JADX WARN: Code restructure failed: missing block: B:135:0x0339, code lost:
    
        r1 = r1.substring(0, 1000000);
        issue(r35, r15 + ": first 1,000,000 characters only");
     */
    /* JADX WARN: Code restructure failed: missing block: B:136:0x0353, code lost:
    
        add(r34, r13.getUri().toString(), r15, 0, r1);
     */
    /* JADX WARN: Code restructure failed: missing block: B:138:0x036a, code lost:
    
        if (r34.size() <= r12) goto L140;
     */
    /* JADX WARN: Code restructure failed: missing block: B:139:0x036c, code lost:
    
        r36[1] = r36[1] + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:140:0x0373, code lost:
    
        r36[2] = r36[2] + 1;
        issue(r35, r15 + ": no text extracted");
     */
    /* JADX WARN: Code restructure failed: missing block: B:141:0x038f, code lost:
    
        if (r2 == null) goto L143;
     */
    /* JADX WARN: Code restructure failed: missing block: B:142:0x0391, code lost:
    
        r2.delete();
     */
    /* JADX WARN: Code restructure failed: missing block: B:144:0x0398, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:145:0x0399, code lost:
    
        r2 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:170:0x03e2, code lost:
    
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:171:0x03e3, code lost:
    
        r1 = r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:172:0x03e8, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:173:0x03e9, code lost:
    
        r1 = r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:174:0x03ec, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:281:?, code lost:
    
        throw r1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:282:?, code lost:
    
        throw r1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:286:?, code lost:
    
        throw r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:287:?, code lost:
    
        throw r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:288:?, code lost:
    
        throw r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x0128, code lost:
    
        throw new java.io.IOException("File exceeds 20 MiB");
     */
    /* JADX WARN: Code restructure failed: missing block: B:57:0x013a, code lost:
    
        r2.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:58:0x013d, code lost:
    
        if (r1 == null) goto L67;
     */
    /* JADX WARN: Code restructure failed: missing block: B:59:0x013f, code lost:
    
        r1.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:61:0x0143, code lost:
    
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:62:0x0144, code lost:
    
        r1 = r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:63:0x014e, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x014f, code lost:
    
        r1 = r2;
        r26 = r12;
        r12 = r24;
        r15 = r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:65:0x0159, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:68:0x0166, code lost:
    
        if (r16 == false) goto L133;
     */
    /* JADX WARN: Code restructure failed: missing block: B:70:0x017a, code lost:
    
        r5 = com.tom_roush.pdfbox.pdmodel.PDDocument.load(r2, com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly().setTempDir(r31.ctx.getCacheDir()));
     */
    /* JADX WARN: Code restructure failed: missing block: B:72:0x0183, code lost:
    
        if (r5.getCurrentAccessPermission().canExtractContent() == false) goto L268;
     */
    /* JADX WARN: Code restructure failed: missing block: B:73:0x0185, code lost:
    
        r4 = java.lang.Math.min(200, r5.getNumberOfPages());
        r2 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:74:0x0196, code lost:
    
        if (r5.getNumberOfPages() <= r4) goto L79;
     */
    /* JADX WARN: Code restructure failed: missing block: B:75:0x0198, code lost:
    
        issue(r35, r6 + ": first 200 pages only");
     */
    /* JADX WARN: Code restructure failed: missing block: B:77:0x01af, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:78:0x01b0, code lost:
    
        r1 = r0;
        r28 = r5;
     */
    /* JADX WARN: Code restructure failed: missing block: B:79:0x01bc, code lost:
    
        r14 = new com.tom_roush.pdfbox.text.PDFTextStripper();
        r3 = 0;
        r1 = 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:80:0x01c8, code lost:
    
        if (r1 > r4) goto L277;
     */
    /* JADX WARN: Code restructure failed: missing block: B:81:0x01ca, code lost:
    
        r19 = r4;
     */
    /* JADX WARN: Code restructure failed: missing block: B:82:0x01d2, code lost:
    
        if (r34.size() >= 5000) goto L278;
     */
    /* JADX WARN: Code restructure failed: missing block: B:83:0x01d4, code lost:
    
        check();
        r14.setStartPage(r1);
        r14.setEndPage(r1);
        r4 = r14.getText(r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:84:0x01e9, code lost:
    
        if (r4.trim().isEmpty() == false) goto L86;
     */
    /* JADX WARN: Code restructure failed: missing block: B:85:0x01eb, code lost:
    
        r3 = r3 + 1;
        r21 = r1;
        r28 = r5;
        r26 = r12;
        r29 = r15;
        r12 = r24;
        r15 = r6;
        r24 = r14;
     */
    /* JADX WARN: Code restructure failed: missing block: B:86:0x01fb, code lost:
    
        r10 = 1000000 - r2;
        r21 = r1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:87:0x0206, code lost:
    
        if (r4.length() <= r10) goto L89;
     */
    /* JADX WARN: Code restructure failed: missing block: B:88:0x0208, code lost:
    
        r1 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:89:0x020a, code lost:
    
        r1 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:90:0x020b, code lost:
    
        r22 = r1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:91:0x020d, code lost:
    
        if (r22 == false) goto L95;
     */
    /* JADX WARN: Code restructure failed: missing block: B:93:0x0210, code lost:
    
        r25 = r4.substring(0, r10);
     */
    /* JADX WARN: Code restructure failed: missing block: B:95:0x0217, code lost:
    
        r25 = r4;
     */
    /* JADX WARN: Code restructure failed: missing block: B:96:0x0219, code lost:
    
        r4 = r2 + r25.length();
     */
    /* JADX WARN: Code restructure failed: missing block: B:97:0x0227, code lost:
    
        r10 = r3;
        r26 = r12;
        r12 = r24;
        r24 = r14;
        r28 = r5;
        r29 = r15;
        r15 = r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:98:0x0241, code lost:
    
        add(r34, r13.getUri().toString(), r6, r21, r25);
     */
    /* JADX WARN: Code restructure failed: missing block: B:99:0x0244, code lost:
    
        if (r22 != false) goto L279;
     */
    /* JADX WARN: Removed duplicated region for block: B:211:0x04a5  */
    /* JADX WARN: Removed duplicated region for block: B:215:0x02de A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:246:0x03d8 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:281:? A[Catch: all -> 0x03e2, Exception -> 0x03e8, CancellationException -> 0x03ec, SYNTHETIC, TryCatch #28 {CancellationException -> 0x03ec, Exception -> 0x03e8, all -> 0x03e2, blocks: (B:169:0x03e1, B:168:0x03de, B:111:0x02b8, B:137:0x0366, B:139:0x036c, B:140:0x0373, B:126:0x02e7, B:125:0x02e4, B:133:0x0309, B:135:0x0339, B:136:0x0353), top: B:249:0x02b8 }] */
    /* JADX WARN: Removed duplicated region for block: B:284:? A[Catch: all -> 0x03e2, Exception -> 0x03e8, CancellationException -> 0x03ec, SYNTHETIC, TRY_LEAVE, TryCatch #28 {CancellationException -> 0x03ec, Exception -> 0x03e8, all -> 0x03e2, blocks: (B:169:0x03e1, B:168:0x03de, B:111:0x02b8, B:137:0x0366, B:139:0x036c, B:140:0x0373, B:126:0x02e7, B:125:0x02e4, B:133:0x0309, B:135:0x0339, B:136:0x0353), top: B:249:0x02b8 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void walk(androidx.documentfile.provider.DocumentFile r32, java.lang.String r33, java.util.List<com.jimliddle.pocketdocs.Store.Passage> r34, java.util.List<java.lang.String> r35, int[] r36, int r37) throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 1196
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.jimliddle.pocketdocs.Indexer.walk(androidx.documentfile.provider.DocumentFile, java.lang.String, java.util.List, java.util.List, int[], int):void");
    }

    public void refresh() throws IOException {
        DocumentFile documentFileFromTreeUri;
        this.store.purgeRevoked();
        for (String[] strArr : this.store.roots()) {
            check();
            DocsService.progress = "Reading " + strArr[1];
            try {
                documentFileFromTreeUri = DocumentFile.fromTreeUri(this.ctx, Uri.parse(strArr[0]));
            } catch (CancellationException e) {
                throw e;
            } catch (Exception e2) {
                this.store.reportFailure(strArr[0], "Refresh failed (" + e2.getClass().getSimpleName() + ").");
            }
            if (documentFileFromTreeUri == null || !documentFileFromTreeUri.canRead()) {
                throw new IOException("Folder unreadable");
            }
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            int[] iArr = {0, 0, 0};
            walk(documentFileFromTreeUri, "", arrayList, arrayList2, iArr, 0);
            check();
            if (arrayList.size() >= 5000) {
                issue(arrayList2, "Passage limit reached (5,000)");
            }
            String str = iArr[1] + " indexed files; " + iArr[2] + " skipped/failed. " + arrayList.size() + " passages.";
            if (!arrayList2.isEmpty()) {
                str = str + " Issues: " + String.join("; ", arrayList2);
            }
            this.store.replaceRoot(strArr[0], strArr[4], arrayList, str);
        }
    }
}
