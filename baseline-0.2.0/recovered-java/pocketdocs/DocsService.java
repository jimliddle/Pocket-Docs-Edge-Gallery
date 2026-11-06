package com.jimliddle.pocketdocs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import fi.iki.elonen.NanoHTTPD;
import java.security.SecureRandom;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/* loaded from: classes3.dex */
public final class DocsService extends Service {
    private volatile boolean destroyed;
    private LocalServer server;
    public static volatile boolean running = false;
    public static volatile boolean indexing = false;
    public static volatile String progress = "Server stopped";
    private static final AtomicBoolean indexLock = new AtomicBoolean(false);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler timer = new Handler(Looper.getMainLooper());

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0() {
        progress = "Session ended after 30 minutes. Start again when needed.";
        stopSelf();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onStartCommand$1() {
        try {
            try {
                try {
                    new Indexer(this).refresh();
                    if (!this.destroyed) {
                        progress = "Refresh complete. Check coverage below.";
                    }
                } catch (CancellationException e) {
                    if (!this.destroyed) {
                        progress = "Refresh cancelled";
                    }
                }
            } catch (Exception e2) {
                if (!this.destroyed) {
                    progress = "Refresh failed; previous index may be stale.";
                }
            }
        } finally {
            indexing = false;
            indexLock.set(false);
        }
    }

    public static synchronized String token(Context context) {
        String string;
        SharedPreferences sharedPreferences = context.getSharedPreferences("connection", 0);
        string = sharedPreferences.getString("token", null);
        if (string == null) {
            byte[] bArr = new byte[32];
            new SecureRandom().nextBytes(bArr);
            string = Base64.encodeToString(bArr, 10);
            sharedPreferences.edit().putString("token", string).commit();
        }
        return string;
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        ((NotificationManager) getSystemService(NotificationManager.class)).createNotificationChannel(new NotificationChannel("docs", "Document server", 2));
        PendingIntent service = PendingIntent.getService(this, 1, new Intent(this, (Class<?>) DocsService.class).setAction("stop"), 67108864);
        startForeground(12, new Notification.Builder(this, "docs").setSmallIcon(R.drawable.ic_docs).setContentTitle("Pocket Docs Semantic is running").setContentText("Local document search · stops after 30 minutes").setContentIntent(PendingIntent.getActivity(this, 2, new Intent(this, (Class<?>) MainActivity.class), 67108864)).addAction(new Notification.Action.Builder((Icon) null, "Stop", service).build()).setOngoing(true).build());
        PDFBoxResourceLoader.init(getApplicationContext());
        try {
            this.server = new LocalServer(token(this), Store.get(this));
            this.server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            running = true;
            progress = "Ready on this phone";
        } catch (Exception e) {
            running = false;
            progress = "Could not start server. Port 8766 may be in use.";
            stopSelf();
        }
        this.timer.postDelayed(new Runnable() { // from class: com.jimliddle.pocketdocs.DocsService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onCreate$0();
            }
        }, 1800000L);
    }

    @Override // android.app.Service
    public void onDestroy() {
        this.destroyed = true;
        running = false;
        this.timer.removeCallbacksAndMessages(null);
        this.worker.shutdownNow();
        if (this.server != null) {
            this.server.stop();
        }
        stopForeground(1);
        super.onDestroy();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        if (intent != null && "stop".equals(intent.getAction())) {
            stopSelf();
            return 2;
        }
        if (intent != null && "index".equals(intent.getAction()) && running && indexLock.compareAndSet(false, true)) {
            indexing = true;
            this.worker.submit(new Runnable() { // from class: com.jimliddle.pocketdocs.DocsService$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.lambda$onStartCommand$1();
                }
            });
        }
        return 2;
    }

    public void onTimeout(int i, int i2) {
        stopSelf();
    }
}
