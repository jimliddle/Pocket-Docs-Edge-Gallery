#!/usr/bin/env python3
"""Pinned PDF/model regression. Run fetch-model.py first. Requires JDK 17."""
from pathlib import Path
import hashlib
import subprocess
import urllib.request

root = Path(__file__).resolve().parents[1]
work = root / 'app/build/retrieval-benchmark'
work.mkdir(parents=True, exist_ok=True)
files = {
 'paper.pdf': ('https://arxiv.org/pdf/1706.03762v7', 'bdfaa68d8984f0dc02beaca527b76f207d99b666d31d1da728ee0728182df697'),
 'pdfbox.jar': ('https://repo.maven.apache.org/maven2/org/apache/pdfbox/pdfbox-app/2.0.27/pdfbox-app-2.0.27.jar', '2c9e46fdc92328361b21b04921f3bd26435b4c472234e283bce278f66c4c2fbc'),
 'onnxruntime.jar': ('https://repo.maven.apache.org/maven2/com/microsoft/onnxruntime/onnxruntime/1.20.0/onnxruntime-1.20.0.jar', 'c46608681692b3693914defb42fa3119ad7cb6146870581ece247e0ed5793fe8'),
 'json.jar': ('https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar', '3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed'),
}
for name, (url, sha) in files.items():
    path = work/name
    if not path.exists() or hashlib.sha256(path.read_bytes()).hexdigest() != sha:
        urllib.request.urlretrieve(url, path)
    if hashlib.sha256(path.read_bytes()).hexdigest() != sha:
        raise RuntimeError(f'{name}: checksum mismatch')
classes = work/'classes'
classes.mkdir(exist_ok=True)
import os
cp = os.pathsep.join(str(work/name) for name in files if name.endswith('.jar'))
src = root/'app/src/main/java/com/jimliddle/pocketdocs'
subprocess.run(['java', 'com.sun.tools.javac.Main', '-cp', cp, '-d', str(classes),
    *[str(src/(name+'.java')) for name in ['TextTools','WordPieceTokenizer','HybridRanker','SearchResponse','MiniLmEncoder']],
    str(root/'scripts/retrieval/Regression.java')], check=True)
(work/'tmp').mkdir(exist_ok=True)
result = subprocess.run(['java', '-Djava.io.tmpdir='+str(work/'tmp'), '-cp', str(classes)+os.pathsep+cp,
    'com.jimliddle.pocketdocs.Regression', str(work/'paper.pdf'), str(root/'app/src/main/assets'),
    str(root/'app/build/retrieval-fixtures/paper.json')], text=True, stdout=subprocess.PIPE)
report = root/'app/build/reports/retrieval.txt'
report.parent.mkdir(parents=True, exist_ok=True)
report.write_text(result.stdout)
print(result.stdout)
result.check_returncode()
