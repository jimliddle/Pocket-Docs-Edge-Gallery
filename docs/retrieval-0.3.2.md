# Retrieval check: 0.3.2

Test document: [Attention Is All You Need, arXiv v7](https://arxiv.org/pdf/1706.03762v7).
PDF SHA-256: `bdfaa68d8984f0dc02beaca527b76f207d99b666d31d1da728ee0728182df697`.

The reported question was:

> how do the authors let a model process more of a sentence at once, rather than working through it one word at a time?

With 0.3.1 retrieval settings, the first two results were page 7 (training batches) and page 1 (abstract). The response omitted the explanation of self-attention. With 0.3.2 the final response includes page 2's explanation as well. Equal keyword scores now receive equal fusion weight, rather than different weights determined by file hashes. The batching passage still ranks first; this change improves evidence coverage rather than guaranteeing the best passage ranks first.

## Method

`scripts/test-retrieval.py` downloads checksum-verified inputs and runs the actual quantized MiniLM model through the same tokenizer, encoder, chunker, ranking and response compaction classes used by the app. Desktop PDFBox 2.0.27 supplies extracted page text. Checks look for expected evidence terms in the **final returned text**, not just a successful tool call or a page number. They are smoke checks, not a general benchmark of semantic understanding.

The script also generates an uncommitted test fixture containing the paper chunks and actual model vectors. `PaperRetrievalTest` uses it to exercise Android SQLite FTS, Store ranking and the MCP response. CI generates this fixture before unit tests. Running unit tests alone skips that one fixture-dependent test. The real PDF and its full extracted text are not committed to this repository.

## Results from the native benchmark

| Question | 0.3.1 | 0.3.2 |
| --- | --- | --- |
| Original plain-language question | Missing evidence | Evidence present |
| Why recurrent training is sequential | Present | Present |
| Preventing attention to future words | Present | Present |
| How the model knows word order | Missing | Missing |
| Scaled dot-product calculation | Present | Present |
| Training hardware and duration | Missing | Present |
| Grouping sentence pairs into batches | Missing | Present |
| English–German BLEU score | Present | Present |
| Number of parallel attention heads | Missing | Present |
| Warmup steps | Present | Present |
| Focused query: positional encoding word order | Missing | Present |
| Focused query: multi-head attention number of heads h | Present | Present |
| Base-model dropout | Present | Present |

Totals: **7/13 before, 12/13 after**. Excluding the two explicitly focused follow-ups: **6/11 before, 10/11 after**. The unresolved word-order query is labelled `KNOWN MISS` in the report; it is not counted as a pass. Any new miss among the other checks fails the build. The focused follow-ups demonstrate recovery, not automatic query rewriting.

## Limits and phone checks

This paper exposed the failure and informed these changes; it is not an independent held-out dataset. These results do not establish quality across other documents. The model and cosine threshold are unchanged, and related but unanswerable questions can still return passages. Returned text is evidence to inspect, not proof that an answer exists.

The benchmark does not run Gemma or prove that Edge Gallery will explain or cite the evidence correctly. It also does not exercise Android PDF rendering, OCR inference or device performance. On a phone: refresh the folder, start a new Edge Gallery conversation, use the previously successful max-tokens setting around 8,500, and repeat the original question. Check the tool payload as well as the generated answer.

## Android build verification

[GitHub Actions run 42402360453](https://github.com/jimliddle/Pocket-Docs-Edge-Gallery/actions/runs/42402360453) built commit `e451011b24699b08f535e7861d1a09b33ec33794` successfully: 30 tests passed, none skipped, lint reported zero errors and 12 warnings, and the APK was produced. This includes the real-vector SQLite/MCP test, the production connection probe and refresh UI tests.

The SQLite/MCP test returned pages 7, 1 and 2 for the original question, including page 2's self-attention explanation. It checks the same required evidence and response budget; minor ordering differences from the standalone benchmark can occur. The word-order miss remains explicitly excluded from the required evidence assertions and visible in the benchmark report.

The [APK ZIP](https://github.com/jimliddle/Pocket-Docs-Edge-Gallery/actions/runs/42402360453/artifacts/13586270803) has archive SHA-256 `0688ee51b897ef522005b6cd2ff8b0f203383bce73ca882a6943e6f37b6e72d0` (this is the ZIP digest, not the APK digest). Extract `app-debug.apk`. The artifact expires on 5 February 2027.
