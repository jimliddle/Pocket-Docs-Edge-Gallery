package com.jimliddle.pocketdocs;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** The connection test used by the app button and the integration tests. */
final class ConnectionProbe {
  private static JSONObject rpc(String token, String method, JSONObject params) throws Exception {
    HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:8766/mcp").openConnection();
    c.setConnectTimeout(5000);
    c.setReadTimeout(10000);
    c.setRequestMethod("POST");
    c.setDoOutput(true);
    c.setRequestProperty("Authorization", "Bearer " + token);
    c.setRequestProperty("Content-Type", "application/json");
    // Android's default Accept value is not a reliable MCP contract.
    c.setRequestProperty("Accept", "application/json, text/event-stream");
    byte[] body = new JSONObject().put("jsonrpc", "2.0").put("id", 1)
        .put("method", method).put("params", params).toString().getBytes(StandardCharsets.UTF_8);
    c.setFixedLengthStreamingMode(body.length);
    try {
      try (OutputStream out = c.getOutputStream()) { out.write(body); }
      int code = c.getResponseCode();
      if (code != 200) throw new IOException("Connection returned HTTP " + code);
      try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        byte[] buffer = new byte[4096]; int count;
        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        JSONObject response = new JSONObject(out.toString("UTF-8"));
        if (response.has("error")) throw new IOException("MCP returned an error: " + response.getJSONObject("error").optString("message"));
        return response.getJSONObject("result");
      }
    } finally { c.disconnect(); }
  }

  static String run(String token) throws Exception {
    JSONObject init = rpc(token, "initialize", new JSONObject().put("protocolVersion", "2025-03-26")
        .put("capabilities", new JSONObject()).put("clientInfo", new JSONObject().put("name", "pocket-docs-test").put("version", "0.3.1")));
    if (!init.has("protocolVersion")) throw new IOException("Missing MCP protocol version");
    JSONArray tools = rpc(token, "tools/list", new JSONObject()).getJSONArray("tools");
    Set<String> names = new HashSet<>();
    for (int i=0;i<tools.length();i++) names.add(tools.getJSONObject(i).getString("name"));
    if (!names.containsAll(Arrays.asList("index_status", "search_documents", "get_passage")))
      throw new IOException("Expected document tools were not returned");
    JSONObject status = rpc(token, "tools/call", new JSONObject().put("name", "index_status").put("arguments", new JSONObject()));
    if (status.optBoolean("isError")) throw new IOException("index_status failed");
    new JSONObject(status.getJSONArray("content").getJSONObject(0).getString("text"));
    return "Connected. Authentication, initialization, all three tools and index_status passed.";
  }
}
