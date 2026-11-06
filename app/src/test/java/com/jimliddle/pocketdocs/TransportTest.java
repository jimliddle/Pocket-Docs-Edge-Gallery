package com.jimliddle.pocketdocs;

import static org.junit.Assert.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;
import org.junit.*;

public class TransportTest {
  LocalServer server;

  @Before
  public void start() throws Exception {
    server =
        new LocalServer(
            "test-token",
            new DocRepository() {
              public JSONObject status() {
                return new JSONObject();
              }

              public JSONObject passage(String id) {
                return null;
              }

              public JSONArray search(String q, int n) {
                return new JSONArray();
              }
            });
    server.start(5000, false);
  }

  @After
  public void stop() {
    server.stop();
  }

  String request(String auth, String extra, String body) throws Exception {
    try (Socket socket = new Socket("127.0.0.1", 8766)) {
      socket.setSoTimeout(5000);
      String head =
          "POST /mcp HTTP/1.1\r\n"
              + "Host: 127.0.0.1:8766\r\n"
              + "Accept: application/json\r\n"
              + "Content-Type: application/json\r\n"
              + "Connection: close\r\n"
              + "Content-Length: "
              + body.getBytes(StandardCharsets.UTF_8).length
              + "\r\nAuthorization: "
              + auth
              + "\r\n"
              + extra
              + "\r\n";
      socket.getOutputStream().write((head + body).getBytes(StandardCharsets.UTF_8));
      return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  public void authorizedToolDiscoveryWorks() throws Exception {
    String response =
        request(
            "Bearer test-token", "", "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");
    assertTrue(response.startsWith("HTTP/1.1 200"));
    assertTrue(response.contains("search_documents"));
    assertTrue(response.contains("get_passage"));
    assertTrue(response.contains("index_status"));
  }

  @Test
  public void badTokenAndBrowserOriginsRejected() throws Exception {
    assertTrue(request("Bearer wrong", "", "{}").startsWith("HTTP/1.1 401"));
    assertTrue(
        request("Bearer test-token", "Origin: https://example.com\r\n", "{}")
            .startsWith("HTTP/1.1 403"));
  }
  @Test
  public void actualAppConnectionProbeCompletesHandshakeAndStatus() throws Exception {
    assertTrue(ConnectionProbe.run("test-token").contains("index_status passed"));
  }

  @Test
  public void actualAppConnectionProbeReportsUnauthorized() throws Exception {
    try {
      ConnectionProbe.run("wrong-token");
      fail("Wrong token must not pass");
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("HTTP 401"));
    }
  }
  @Test
  public void appExplicitlySetsAcceptInsteadOfDependingOnPlatformDefault() throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:8766/mcp").openConnection();
    try {
      assertNull(connection.getRequestProperty("Accept"));
      ConnectionProbe.configure(connection, "test-token");
      assertEquals("application/json, text/event-stream", connection.getRequestProperty("Accept"));
      assertEquals("application/json", connection.getRequestProperty("Content-Type"));
      assertEquals("POST", connection.getRequestMethod());
    } finally { connection.disconnect(); }
  }
}
