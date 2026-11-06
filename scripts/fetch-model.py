#!/usr/bin/env python3
"""Fetch the exact model used by the APK. Refuse changed or incomplete downloads."""
from pathlib import Path
import hashlib
import urllib.request

assets = Path(__file__).resolve().parents[1] / 'app/src/main/assets'
files = {
    'minilm-q8.onnx': ('onnx/model_quantized.onnx', 'afdb6f1a0e45b715d0bb9b11772f032c399babd23bfc31fed1c170afc848bdb1'),
    'vocab.txt': ('vocab.txt', '07eced375cec144d27c900241f3e339478dec958f92fddbc551f295c992038a3'),
}
assets.mkdir(parents=True, exist_ok=True)
for name, (source, digest) in files.items():
    target = assets / name
    if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest() == digest:
        print(f'{name}: verified')
        continue
    pending = target.with_suffix(target.suffix + '.download')
    try:
        urllib.request.urlretrieve('https://huggingface.co/Xenova/all-MiniLM-L6-v2/resolve/main/' + source, pending)
        if hashlib.sha256(pending.read_bytes()).hexdigest() != digest:
            raise RuntimeError(f'{name}: checksum mismatch; refusing to replace asset')
        pending.replace(target)
        print(f'{name}: downloaded and verified')
    finally:
        pending.unlink(missing_ok=True)
