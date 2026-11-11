package com.jimliddle.pocketdocs;

import org.json.*;

/** Presentation only: keeps selected source text, citations and result order unchanged. */
public final class PassageText {
  private PassageText() {}

  public static String format(JSONArray passages) throws JSONException {
    if (passages.length() == 0) return "No matching passages were returned.";
    StringBuilder out = new StringBuilder("Retrieved passages: ").append(passages.length());
    for (int i = 0; i < passages.length(); i++) {
      JSONObject p = passages.getJSONObject(i);
      out.append("\n\n--- Passage ").append(i + 1).append(" ---\n");
      out.append("Filename: ").append(p.getString("filename")).append('\n');
      if (!p.isNull("page")) out.append("Page: ").append(p.get("page")).append('\n');
      out.append("Citation: (").append(p.getString("filename"));
      if (!p.isNull("page")) out.append(", page ").append(p.get("page"));
      else out.append(", chunk ").append(p.getInt("chunk"));
      out.append(")\n");
      out.append("Chunk: ").append(p.getInt("chunk")).append('\n');
      out.append("Passage ID: ").append(p.getString("passage_id")).append('\n');
      out.append("Extraction: ").append(p.optString("extraction", "text")).append('\n');
      if (p.optBoolean("excerpt", false)) out.append("Shortened: yes\n");
      if (p.has("warning"))
        out.append("Retrieval warning: ").append(p.getString("warning")).append('\n');
      out.append("Text:\n").append(p.getString("text"));
      out.append("\n--- End passage ").append(i + 1).append(" ---");
    }
    return out.toString();
  }
}
