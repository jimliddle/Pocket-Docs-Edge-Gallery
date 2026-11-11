package com.jimliddle.pocketdocs;

import static org.junit.Assert.*;

import android.content.*;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import java.time.Duration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class RefreshUiTest {
  ActivityController<MainActivity> controller;
  MainActivity activity;
  Store store;
  String uri = "content://docs/tree/rag";

  @Before
  public void setup() {
    Context c = RuntimeEnvironment.getApplication();
    c.deleteDatabase("documents.db");
    c.getSharedPreferences("settings", 0)
        .edit()
        .remove("readable_search")
        .remove("broader_search")
        .commit();
    c.getContentResolver()
        .takePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION);
    store =
        new Store(
            c,
            new EmbeddingProvider() {
              public float[] embed(String s) {
                return new float[] {1, 0};
              }

              public String error() {
                return "";
              }
            });
    ReflectionHelpers.setStaticField(Store.class, "instance", store);
    store.addRoot(uri, "RAG");
    DocsService.indexing = false;
    DocsService.running = true;
    DocsService.progress = "Ready on this phone";
    controller = Robolectric.buildActivity(MainActivity.class).setup();
    activity = controller.get();
  }

  @After
  public void cleanup() {
    controller.pause().stop().destroy();
    DocsService.indexing = false;
    DocsService.running = false;
    store.close();
    ReflectionHelpers.setStaticField(Store.class, "instance", null);
  }

  private TextView text(int id) {
    return activity.findViewById(id);
  }

  @Test
  public void refreshProgressUpdatesNextToButtonWithoutOpeningDialog() {
    ((Button) activity.findViewById(R.id.refresh_folders)).performClick();
    assertEquals("Starting folder refresh…", text(R.id.folder_progress).getText().toString());
    DocsService.indexing = true;
    DocsService.progress = "OCR scan.pdf · page 2 / 4";
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));
    assertEquals(DocsService.progress, text(R.id.folder_progress).getText().toString());
    assertEquals(View.VISIBLE, activity.findViewById(R.id.refresh_progress).getVisibility());
    assertFalse(activity.findViewById(R.id.refresh_folders).isEnabled());
    assertTrue(text(R.id.folder_coverage).getText().toString().contains("RAG"));
  }

  @Test
  public void completionAndCoverageRemainInlineAfterReopening() {
    ContentValues values = new ContentValues();
    values.put("refreshed", "2026-11-06T18:00:00Z");
    values.put("report", "2 indexed files; 12 passages. 3 pages read with OCR");
    store.getWritableDatabase().update("roots", values, "uri=?", new String[] {uri});
    DocsService.indexing = false;
    DocsService.progress = "Refresh complete. Check folder coverage.";
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));
    assertTrue(
        text(R.id.folder_coverage).getText().toString().contains("2 indexed files; 12 passages"));
    assertEquals(View.GONE, activity.findViewById(R.id.refresh_progress).getVisibility());
    assertTrue(activity.findViewById(R.id.refresh_folders).isEnabled());
    controller.pause().stop().destroy();
    controller = Robolectric.buildActivity(MainActivity.class).setup();
    activity = controller.get();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));
    assertTrue(text(R.id.folder_coverage).getText().toString().contains("3 pages read with OCR"));
  }

  @Test
  public void responseFormatDefaultsToReadableAndPersistsWithoutResettingToken() {
    CheckBox toggle = activity.findViewById(R.id.readable_search);
    assertTrue(toggle.isChecked());
    String token = DocsService.token(activity);
    toggle.setChecked(false);
    assertFalse(activity.getSharedPreferences("settings", 0).getBoolean("readable_search", true));
    controller.pause().stop().destroy();
    controller = Robolectric.buildActivity(MainActivity.class).setup();
    activity = controller.get();
    assertFalse(((CheckBox) activity.findViewById(R.id.readable_search)).isChecked());
    assertEquals(token, DocsService.token(activity));
    assertEquals(1, store.roots().size());
  }

  @Test
  public void evidenceSettingPersistsWithoutChangingTokenOrFolders() {
    CheckBox toggle = activity.findViewById(R.id.broader_search);
    assertTrue(toggle.isChecked());
    String token = DocsService.token(activity);
    toggle.setChecked(false);
    controller.pause().stop().destroy();
    controller = Robolectric.buildActivity(MainActivity.class).setup();
    activity = controller.get();
    assertFalse(((CheckBox) activity.findViewById(R.id.broader_search)).isChecked());
    assertEquals(token, DocsService.token(activity));
    assertEquals(1, store.roots().size());
  }
}
