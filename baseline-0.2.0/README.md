# Pocket Docs Semantic 0.2.0 recovery baseline

This archive preserves the exact user-tested APK before response compaction or OCR changes.

APK SHA-256: 167b2f4bfae53cb1f3237b43cf251fe3f0c20ae38ea4711784538ed4f93fc717

Recovered 6 November 2026 with JADX 1.5.3. The original APK is authoritative. The recovered Java is a decompiler snapshot, not yet a buildable source project. Six methods require reconstruction; build files, original tests and the signing private key cannot be recovered from an APK.

Existing configuration: com.jimliddle.pocketdocs.semantic, version 0.2.0, Android API 26 minimum / 35 target, ARM64, localhost MCP port 8766. Bundled quantised all-MiniLM-L6-v2 model, 384-dimensional embeddings, ONNX Runtime Android 1.20.0, local FTS4 plus semantic reciprocal-rank fusion. Default six search results, each up to 1,600 characters. No OCR in this baseline.

Preserve this baseline before implementing compact responses, overlapping-excerpt reduction, smaller default result counts and on-device PDF OCR.
