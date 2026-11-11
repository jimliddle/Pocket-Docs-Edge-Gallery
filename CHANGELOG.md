# Changelog

Version history and build validation for Pocket Docs. See the [README](README.md) for setup, usage and build instructions.

## 0.3.4 broader evidence

PrivateLLMLens showed that the same model could answer this question when given a broader selection of passages. This build applies that finding to Pocket Docs:

- **More evidence**, enabled by default, allows eight passages and 12,800 characters of passage text. Switch it off for three passages and 4,800 characters. The next search uses the setting; no folder refresh or server restart is needed.
- `search_documents` accepts an optional `max_chars` budget. It keeps fitting chunks complete instead of cutting the final chunk to use the remaining space. Oversized chunks still use the existing extractive excerpt handling.
- Readable results include an exact citation line alongside each passage. Filenames, page numbers and lookup IDs remain available.

This does not copy PrivateLLMLens's entire pipeline: ranking and embeddings are unchanged, and Pocket Docs cannot insert a system message or render source links in Edge Gallery. It cannot know how much context Edge Gallery has left. The wider selection can add irrelevant passages and use more context; the compact setting is available for comparison.

The real PDF/ONNX benchmark passes all 13 evidence checks with the wider setting. With the same strengthened checks, compact mode passes 11/13: it misses the explicit sequential-processing explanation for the original question and the positional-encoding evidence for the word-order question. The original question now includes page 2 chunk 1, but still does not retrieve the page 4 matrix-calculation passage seen in PrivateLLMLens. These tests check retrieved evidence, not Gemma's final answer.

[Download the 0.3.4 test APK ZIP](https://github.com/jimliddle/Pocket-Docs-Edge-Gallery/actions/runs/42984213283/artifacts/13838433439) (GitHub sign-in required; extract `app-debug.apk`). It expires on 9 February 2027. [Build 42984213283](https://github.com/jimliddle/Pocket-Docs-Edge-Gallery/actions/runs/42984213283) built commit `8237c86ea98c79c7925b4cb01a38139ebf73528e`: 39 tests passed, none skipped; lint reported zero errors and 14 warnings. Tests include real-vector SQLite/MCP retrieval, budget validation, changing the evidence setting, persisted UI settings, citations, transport, indexing and OCR policy. No phone inference test has been run. This build uses the existing temporary debug-signing workflow; it is not guaranteed to install over a previous Actions APK. See the [build note](README.md#build).

## 0.3.3 response-format comparison

[Download the 0.3.3 test APK ZIP](https://github.com/jimliddle/Pocket-Docs-Edge-Gallery/actions/runs/42505958562/artifacts/13626659423) (GitHub sign-in required; extract `app-debug.apk`). The artifact expires on 6 February 2027. See the signing-key note under [Build in the README](README.md#build) before installing over a previous test build.

[Build 42505958562](https://github.com/jimliddle/Pocket-Docs-Edge-Gallery/actions/runs/42505958562) produced the APK from commit `cd5279b85c510d43d1dabcba03aa4847860125c6`. All 36 automated tests passed with none skipped; Android lint reported zero errors and 13 warnings. The unchanged retrieval benchmark remains at 12/13 evidence checks. These results verify the transport and response content, not the quality of Gemma's generated answer.

This test build changes how `search_documents` presents its result. By default it returns labelled passages with actual line breaks, exact filenames, page/chunk numbers and passage IDs. It does not add the previous advisory note. Source text, result order, retrieval model, chunking, three-passage default and text budgets remain unchanged from 0.3.2. `index_status` and `get_passage` keep their existing formats.

Under **Search response format**, leave **Readable passages** checked for the new format. Uncheck it to reproduce the previous JSON response, including its original note. The switch takes effect on the next tool call without restarting the server, changing the token or refreshing folders. This allows both formats to be compared within one installation.

The phone screenshots confirmed that 0.3.2 delivered all three passages, including the relevant page 2 explanation, but Gemma still gave an incorrect answer. The new format is an experiment to address that failure, not a confirmed fix. For a useful comparison, use the same question and model settings in a fresh conversation for each format. Do not change token limits or the system prompt between those two runs.

The automated tests check exact source-text preservation, citation fields, empty results, fallback warnings, switching formats over a running authenticated connection, and both formats through the real-vector SQLite/MCP regression test. They do not run Gemma or establish whether its answer improves.

## 0.3.2 retrieval changes

- Chunks prefer sentence boundaries. Keyword matching uses whole words with simple singular/plural matching, so short terms do not match inside unrelated words.
- Search returns three passages by default, with up to 1,600 characters each and a 4,800 character total text budget. This retains explanations that the previous 650-character excerpts cut off.
- Tied keyword scores receive equal ranking weight instead of depending on file hashes.
- Bibliography-heavy chunks receive less weight unless the question asks about citations or references. The embedding model is unchanged.
- **Refresh folders after installing** to rebuild the chunks. The extraction cache version changes automatically; there is no need to clear the index first.
- The [retrieval report](docs/retrieval-0.3.2.md) includes the reported failure, additional questions and the remaining word-order retrieval miss. Tests use the actual model, then exercise the SQLite/MCP path with those vectors.

## 0.3.1 fixes

- The local in app MCP connection test explicitly sends the required `Accept` header. It now checks initialization, discovery of the three expected tools, and an actual `index_status` call.
- Refresh progress, a busy indicator, and each folder's latest coverage report appear directly below **Refresh folders**. They update while this screen is open; completed reports remain visible when it is reopened.
- Regression tests exercise the same connection probe used by the button and the activity's progress/coverage views. The original transport tests constructed their own request and missed the app's missing header.
- The GitHub build requests supported Android SDK packages rather than the retired `tools` package.

## 0.3.0 OCR and response budgets

Added on-device OCR for scanned PDFs and smaller search responses to leave more room for the model's answer. The response budgets were subsequently revised in 0.3.2.

The 0.3.0 test build passed 17 automated tests and Android lint (zero errors; warnings remain for development-only ARM64 support, English UI text, synchronous token persistence, an application context singleton, and unused TLS code inside a PDF dependency). Automated tests cover excerpt budgets, citation preservation, duplicate removal, full-passage access, argument validation, OCR selection/render bounds, extraction cache invalidation and folder removal. Android OCR inference and Edge Gallery interaction still require testing on a phone.
