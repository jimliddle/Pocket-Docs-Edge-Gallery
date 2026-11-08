package com.jimliddle.pocketdocs;

import android.app.*;
import android.content.*;
import android.graphics.drawable.Icon;
import android.os.*;
import android.util.Base64;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import fi.iki.elonen.NanoHTTPD;
import java.security.SecureRandom;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DocsService extends Service {
  public static volatile boolean running = false, indexing = false;
  public static volatile String progress = "Server stopped";
  private static final AtomicBoolean indexLock = new AtomicBoolean();
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler timer = new Handler(Looper.getMainLooper());
  private LocalServer server;
  private volatile boolean destroyed;

  public static synchronized String token(Context c) {
    SharedPreferences p = c.getSharedPreferences("connection", 0);
    String t = p.getString("token", null);
    if (t == null) {
      byte[] bytes = new byte[32];
      new SecureRandom().nextBytes(bytes);
      t = Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP);
      p.edit().putString("token", t).commit();
    }
    return t;
  }

  public IBinder onBind(Intent i) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    getSystemService(NotificationManager.class)
        .createNotificationChannel(
            new NotificationChannel("docs", "Document server", NotificationManager.IMPORTANCE_LOW));
    PendingIntent stop =
        PendingIntent.getService(
            this,
            1,
            new Intent(this, DocsService.class).setAction("stop"),
            PendingIntent.FLAG_IMMUTABLE);
    startForeground(
        12,
        new Notification.Builder(this, "docs")
            .setSmallIcon(R.drawable.ic_docs)
            .setContentTitle("Pocket Docs is running")
            .setContentText("Local document search · stops after 30 minutes")
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 2, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE))
            .addAction(new Notification.Action.Builder((Icon) null, "Stop", stop).build())
            .setOngoing(true)
            .build());
    PDFBoxResourceLoader.init(getApplicationContext());
    try {
      server =
          new LocalServer(
              token(this),
              Store.get(this),
              () -> getSharedPreferences("settings", 0).getBoolean("readable_search", true));
      server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
      running = true;
      progress = "Ready on this phone";
    } catch (Exception e) {
      running = false;
      progress = "Cannot start server; port 8766 may be in use.";
      stopSelf();
    }
    timer.postDelayed(
        () -> {
          progress = "Session ended after 30 minutes. Start again when needed.";
          stopSelf();
        },
        1800000);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int id) {
    if (intent != null && "stop".equals(intent.getAction())) {
      stopSelf();
      return START_NOT_STICKY;
    }
    if (intent != null
        && "index".equals(intent.getAction())
        && running
        && indexLock.compareAndSet(false, true)) {
      indexing = true;
      progress = "Starting folder refresh…";
      worker.submit(
          () -> {
            try {
              new Indexer(this).refresh();
              if (!destroyed) progress = "Refresh complete. Check folder coverage.";
            } catch (CancellationException e) {
              if (!destroyed) progress = "Refresh cancelled";
            } catch (Exception e) {
              if (!destroyed) progress = "Refresh failed; previous index may be stale.";
            } finally {
              indexing = false;
              indexLock.set(false);
            }
          });
    }
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    destroyed = true;
    running = false;
    timer.removeCallbacksAndMessages(null);
    worker.shutdownNow();
    if (server != null) server.stop();
    stopForeground(STOP_FOREGROUND_REMOVE);
    super.onDestroy();
  }

  @Override
  public void onTimeout(int id, int type) {
    stopSelf();
  }
}
