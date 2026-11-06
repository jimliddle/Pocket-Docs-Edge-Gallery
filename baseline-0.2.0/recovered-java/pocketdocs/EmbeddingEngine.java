package com.jimliddle.pocketdocs;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/* loaded from: classes3.dex */
public final class EmbeddingEngine implements EmbeddingProvider {
    public static final String MODEL_ID = "all-MiniLM-L6-v2-q8-v1";
    private static EmbeddingEngine instance;
    private final Context context;
    private OrtEnvironment env;
    private volatile String error = "";
    private OrtSession session;
    private WordPieceTokenizer tokenizer;

    private EmbeddingEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized EmbeddingEngine get(Context context) {
        if (instance == null) {
            instance = new EmbeddingEngine(context);
        }
        return instance;
    }

    private void initialize() throws Exception {
        if (this.session != null) {
            return;
        }
        InputStream inputStreamOpen = this.context.getAssets().open("vocab.txt");
        try {
            this.tokenizer = new WordPieceTokenizer(new InputStreamReader(inputStreamOpen, StandardCharsets.UTF_8));
            if (inputStreamOpen != null) {
                inputStreamOpen.close();
            }
            inputStreamOpen = this.context.getAssets().open("minilm-q8.onnx");
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    byte[] bArr = new byte[16384];
                    while (true) {
                        int i = inputStreamOpen.read(bArr);
                        if (i == -1) {
                            break;
                        } else {
                            byteArrayOutputStream.write(bArr, 0, i);
                        }
                    }
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    byteArrayOutputStream.close();
                    if (inputStreamOpen != null) {
                        inputStreamOpen.close();
                    }
                    this.env = OrtEnvironment.getEnvironment();
                    OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
                    try {
                        sessionOptions.setIntraOpNumThreads(2);
                        sessionOptions.setInterOpNumThreads(1);
                        this.session = this.env.createSession(byteArray, sessionOptions);
                        sessionOptions.close();
                    } catch (Throwable th) {
                        try {
                            sessionOptions.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                        throw th;
                    }
                } finally {
                }
            } finally {
            }
        } finally {
        }
    }

    public synchronized void close() {
        if (this.session != null) {
            try {
                this.session.close();
            } catch (OrtException e) {
            }
            this.session = null;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:111:0x00ff A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:117:0x0125 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:129:0x0112 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:151:? A[Catch: all -> 0x0109, SYNTHETIC, TRY_LEAVE, TryCatch #5 {all -> 0x0109, blocks: (B:68:0x0108, B:67:0x0105, B:63:0x00ff), top: B:111:0x00ff, inners: #0 }] */
    /* JADX WARN: Removed duplicated region for block: B:153:? A[Catch: all -> 0x011c, SYNTHETIC, TRY_LEAVE, TryCatch #18 {all -> 0x011c, blocks: (B:79:0x011b, B:78:0x0118, B:74:0x0112), top: B:129:0x0112, inners: #13 }] */
    /* JADX WARN: Removed duplicated region for block: B:155:? A[Catch: all -> 0x0152, LinkageError -> 0x0154, Exception -> 0x0156, CancellationException -> 0x0182, SYNTHETIC, TryCatch #19 {LinkageError -> 0x0154, CancellationException -> 0x0182, Exception -> 0x0156, blocks: (B:4:0x0003, B:5:0x0017, B:7:0x001d, B:9:0x002d, B:90:0x012e, B:89:0x012b, B:47:0x00dc, B:91:0x012f, B:92:0x0136, B:94:0x0138, B:96:0x013b, B:97:0x0149), top: B:126:0x0003, outer: #11 }] */
    @Override // com.jimliddle.pocketdocs.EmbeddingProvider
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public synchronized float[] embed(java.lang.String r22) {
        /*
            Method dump skipped, instructions count: 390
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.jimliddle.pocketdocs.EmbeddingEngine.embed(java.lang.String):float[]");
    }

    @Override // com.jimliddle.pocketdocs.EmbeddingProvider
    public String error() {
        return this.error;
    }
}
