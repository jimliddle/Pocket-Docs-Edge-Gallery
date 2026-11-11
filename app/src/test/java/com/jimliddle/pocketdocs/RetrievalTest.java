package com.jimliddle.pocketdocs;

import static org.junit.Assert.*;

import org.json.*;
import org.junit.Test;

public class RetrievalTest {
  JSONObject passage(String id, String text) throws Exception {
    return new JSONObject()
        .put("passage_id", id)
        .put("filename", "1706.03762.pdf")
        .put("page", 2)
        .put("chunk", 1)
        .put("text", text);
  }

  @Test
  public void excerptsAreExactAndBounded() throws Exception {
    String text =
        "Background text about a separate topic. ".repeat(40)
            + "Self-attention enables parallel computation. ".repeat(15);
    JSONObject p =
        SearchResponse.compact(new JSONArray().put(passage("id", text)), "parallel computation", 2)
            .getJSONObject(0);
    assertTrue(p.getString("text").length() <= SearchResponse.MAX_EXCERPT);
    assertTrue(text.contains(p.getString("text")));
    assertTrue(p.getString("text").contains("parallel"));
    assertEquals("1706.03762.pdf", p.getString("filename"));
    assertEquals(2, p.getInt("page"));
    assertTrue(p.getBoolean("excerpt"));
  }

  @Test
  public void repeatedPassagesAreRemoved() throws Exception {
    String text =
        "The warranty covers repairs for damaged hinges and broken locks during the first year.";
    JSONArray out =
        SearchResponse.compact(
            new JSONArray()
                .put(passage("a", text))
                .put(passage("b", text))
                .put(passage("c", "Returns must be sent within thirty days of purchase.")),
            "warranty",
            2);
    assertEquals(2, out.length());
    assertEquals("c", out.getJSONObject(1).getString("passage_id"));
  }

  @Test
  public void totalTextBudgetIsEnforced() throws Exception {
    JSONArray input = new JSONArray();
    for (int i = 0; i < 10; i++) input.put(passage("id" + i, ("unique" + i + " ").repeat(200)));
    JSONArray out = SearchResponse.compact(input, "unique", 10);
    int n = 0;
    for (int i = 0; i < out.length(); i++) n += out.getJSONObject(i).getString("text").length();
    assertTrue(n <= SearchResponse.MAX_TOTAL_TEXT);
  }

  @Test
  public void mcpDefaultOmitsStatusAndFullPassageStillAvailable() throws Exception {
    String text = "alpha ".repeat(200);
    DocRepository repo =
        new DocRepository() {
          public JSONObject status() {
            throw new AssertionError("Search must not fetch status");
          }

          public JSONObject passage(String id) throws Exception {
            return RetrievalTest.this.passage(id, text);
          }

          public JSONArray search(String q, int n) throws Exception {
            return new JSONArray()
                .put(passage("a"))
                .put(RetrievalTest.this.passage("b", "beta ".repeat(200)))
                .put(RetrievalTest.this.passage("c", "gamma ".repeat(200)));
          }
        };
    McpEngine e = new McpEngine(repo, () -> false);
    JSONObject req =
        new JSONObject(
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"search_documents\",\"arguments\":{\"query\":\"alpha\"}}}");
    JSONObject answer =
        new JSONObject(
            e.handle(req)
                .getJSONObject("result")
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text"));
    assertEquals(3, answer.getJSONArray("passages").length());
    assertFalse(answer.has("status"));
    req.getJSONObject("params")
        .put("name", "get_passage")
        .put("arguments", new JSONObject().put("passage_id", "a"));
    answer =
        new JSONObject(
            e.handle(req)
                .getJSONObject("result")
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text"));
    assertEquals(text, answer.getString("text"));
  }

  @Test
  public void invalidLimitRejected() throws Exception {
    JSONObject req =
        new JSONObject(
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"search_documents\",\"arguments\":{\"query\":\"alpha\",\"limit\":2.5}}}");
    assertEquals(-32602, new McpEngine(null).handle(req).getJSONObject("error").getInt("code"));
  }

  @Test
  public void budgetSkipsChunkRatherThanCuttingItsExplanation() throws Exception {
    String first = "alpha ".repeat(180), second = "beta ".repeat(180);
    JSONArray out =
        SearchResponse.compact(
            new JSONArray()
                .put(passage("a", first))
                .put(passage("b", second))
                .put(passage("c", "A short complete explanation.")),
            "alpha",
            8,
            1600);
    assertEquals(2, out.length());
    assertEquals(first, out.getJSONObject(0).getString("text"));
    assertEquals("c", out.getJSONObject(1).getString("passage_id"));
    assertFalse(out.getJSONObject(1).getBoolean("excerpt"));
  }

  @Test
  public void evidenceSettingChangesNextCallAndExplicitArgumentsOverrideDefaults()
      throws Exception {
    boolean[] broader = {true};
    DocRepository repo =
        new DocRepository() {
          public JSONObject status() {
            return new JSONObject();
          }

          public JSONObject passage(String id) {
            return null;
          }

          public JSONArray search(String q, int n) throws Exception {
            JSONArray out = new JSONArray();
            for (int i = 0; i < 12; i++)
              out.put(RetrievalTest.this.passage("id" + i, ("unique" + i + " ").repeat(180)));
            return out;
          }
        };
    McpEngine engine = new McpEngine(repo, () -> false, () -> broader[0]);
    JSONObject req =
        new JSONObject(
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"search_documents\",\"arguments\":{\"query\":\"unique\"}}}");
    assertEquals(8, passages(engine, req).length());
    broader[0] = false;
    assertEquals(3, passages(engine, req).length());
    req.getJSONObject("params").getJSONObject("arguments").put("limit", 8).put("max_chars", 12800);
    assertEquals(8, passages(engine, req).length());
    req.getJSONObject("params").getJSONObject("arguments").put("max_chars", 1600);
    assertEquals(1, passages(engine, req).length());
    for (Object invalid : new Object[] {1599, 12801, 2000.5, "4800", JSONObject.NULL}) {
      req.getJSONObject("params").getJSONObject("arguments").put("max_chars", invalid);
      assertEquals(-32602, engine.handle(req).getJSONObject("error").getInt("code"));
    }
  }

  private JSONArray passages(McpEngine engine, JSONObject request) throws Exception {
    return new JSONObject(
            engine
                .handle(request)
                .getJSONObject("result")
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text"))
        .getJSONArray("passages");
  }
}
