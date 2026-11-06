package com.jimliddle.pocketdocs;

import org.json.JSONArray;
import org.json.JSONObject;

public interface DocRepository {
  JSONObject passage(String str) throws Exception;

  JSONArray search(String str, int i) throws Exception;

  JSONObject status() throws Exception;
}
