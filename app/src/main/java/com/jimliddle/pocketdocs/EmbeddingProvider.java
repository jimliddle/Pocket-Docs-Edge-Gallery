package com.jimliddle.pocketdocs;

interface EmbeddingProvider {
  float[] embed(String str);

  String error();
}
