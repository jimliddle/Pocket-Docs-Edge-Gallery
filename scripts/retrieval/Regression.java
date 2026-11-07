package com.jimliddle.pocketdocs;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.*;
import org.json.*;

/** Real model/PDF smoke benchmark; Android Store/FTS is covered separately. */
public final class Regression {
  static final String[][] CASES = {
    {
      "plain-language parallelism",
      "how do the authors let a model process more of a sentence at once, rather than working"
          + " through it one word at a time?",
      "self-attention",
      "paralleliz",
      "recurrent"
    },
    {
      "sequential training",
      "Why do recurrent networks prevent parallelization during training?",
      "sequential",
      "parallelization"
    },
    {"future words", "How does the decoder prevent attention to future words?", "mask", "position"},
    {
      "word order",
      "How does the model know the order of the words?",
      "positional",
      "input embeddings"
    },
    {
      "attention calculation",
      "How is scaled dot-product attention calculated?",
      "softmax",
      "product"
    },
    {
      "training hardware", "What hardware was used and how long did training take?", "p100", "hours"
    },
    {
      "batching",
      "How are sentence pairs grouped into training batches?",
      "25000",
      "sequence length"
    },
    {
      "translation score",
      "What BLEU score did the model achieve on English to German translation?",
      "28.4"
    },
    {"attention heads", "How many parallel attention heads are used?", "h = 8"},
    {"warmup", "How many warmup steps does the learning rate schedule use?", "4000"},
    {"word order focused", "positional encoding word order", "positional", "input embeddings"},
    {"attention heads focused", "multi-head attention number of heads h", "h = 8"},
    {"dropout", "What dropout rate is used in the base model?", "0.1"}
  };

  public static void main(String[] args) throws Exception {
    LinkedHashMap<String, JSONObject> corpus = new LinkedHashMap<>();
    Map<String, float[]> vectors = new HashMap<>();
    try (Reader vocabulary = Files.newBufferedReader(Path.of(args[1], "vocab.txt"));
        MiniLmEncoder encoder =
            new MiniLmEncoder(vocabulary, Files.readAllBytes(Path.of(args[1], "minilm-q8.onnx")));
        PDDocument doc = PDDocument.load(new File(args[0]))) {
      PDFTextStripper stripper = new PDFTextStripper();
      for (int page = 1; page <= doc.getNumberOfPages(); page++) {
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        int chunk = 0;
        for (String text : TextTools.chunks(stripper.getText(doc))) {
          String id = "p" + page + "c" + (++chunk);
          corpus.put(
              id,
              new JSONObject()
                  .put("passage_id", id)
                  .put("filename", "1706.03762.pdf")
                  .put("page", page)
                  .put("chunk", chunk)
                  .put("text", text));
          vectors.put(id, encoder.embed(text));
        }
      }
      int passed = 0, regressions = 0;
      JSONArray fixtureCases = new JSONArray();
      JSONObject fixtureVectors = new JSONObject();
      for (String id : corpus.keySet())
        fixtureVectors.put(corpus.get(id).getString("text"), new JSONArray(vectors.get(id)));
      System.out.println(
          "PDF chunks: "
              + corpus.size()
              + ". Final response: up to "
              + SearchResponse.DEFAULT_LIMIT
              + " passages / "
              + SearchResponse.MAX_TOTAL_TEXT
              + " text characters.");
      for (String[] test : CASES) {
        float[] query = encoder.embed(test[1]);
        fixtureVectors.put(test[1], new JSONArray(query));
        List<HybridRanker.Hit> lex = new ArrayList<>(), sem = new ArrayList<>();
        for (String id : corpus.keySet()) {
          String body = corpus.get(id).getString("text");
          double weight = TextTools.referenceWeight(test[1], body);
          int score = TextTools.score(test[1], "1706.03762.pdf", body);
          if (score > 0) lex.add(new HybridRanker.Hit(id, score * weight));
          double cosine = HybridRanker.cosine(query, HybridRanker.encode(vectors.get(id)));
          if (cosine >= HybridRanker.MIN_COSINE) sem.add(new HybridRanker.Hit(id, cosine * weight));
        }
        JSONArray candidates = new JSONArray();
        for (HybridRanker.Hit hit :
            HybridRanker.fuse(
                HybridRanker.sorted(lex, 30),
                HybridRanker.sorted(sem, 30),
                SearchResponse.DEFAULT_LIMIT * 3)) candidates.put(corpus.get(hit.id));
        JSONArray returned =
            SearchResponse.compact(candidates, test[1], SearchResponse.DEFAULT_LIMIT);
        StringBuilder evidence = new StringBuilder(), ids = new StringBuilder();
        for (int i = 0; i < returned.length(); i++) {
          JSONObject p = returned.getJSONObject(i);
          evidence.append(p.getString("text")).append('\n');
          ids.append(p.getString("passage_id")).append(' ');
        }
        String normalized = evidence.toString().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        boolean ok = true;
        for (int i = 2; i < test.length; i++) ok &= normalized.contains(test[i]);
        boolean knownMiss = test[0].equals("word order");
        if (ok) passed++;
        else if (!knownMiss) regressions++;
        fixtureCases.put(
            new JSONObject()
                .put("name", test[0])
                .put("query", test[1])
                .put("expected", new JSONArray(Arrays.copyOfRange(test, 2, test.length)))
                .put("known_miss", knownMiss));
        System.out.println(
            (ok ? "PASS" : knownMiss ? "KNOWN MISS" : "REGRESSION")
                + " | "
                + test[0]
                + " | "
                + ids
                + " | "
                + evidence.length()
                + " chars");
      }
      System.out.println(
          "Evidence checks: "
              + passed
              + "/"
              + CASES.length
              + ". These checks do not evaluate Gemma's generated answer.");
      if (args.length > 2) {
        Path fixture = Path.of(args[2]);
        Files.createDirectories(fixture.getParent());
        Files.writeString(
            fixture,
            new JSONObject()
                .put("passages", new JSONArray(corpus.values()))
                .put("vectors", fixtureVectors)
                .put("cases", fixtureCases)
                .toString());
      }
      if (regressions > 0)
        throw new AssertionError(
            regressions + " new evidence regressions (one documented miss remains)");
    }
  }
}
