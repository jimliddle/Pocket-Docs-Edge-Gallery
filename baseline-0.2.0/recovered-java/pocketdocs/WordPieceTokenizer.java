package com.jimliddle.pocketdocs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;

/* loaded from: classes3.dex */
public final class WordPieceTokenizer {
    private final Map<String, Integer> vocab = new HashMap();

    public WordPieceTokenizer(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        int i = 0;
        while (true) {
            try {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                int i2 = i + 1;
                this.vocab.put(line, Integer.valueOf(i));
                i = i2;
            } catch (Throwable th) {
                try {
                    bufferedReader.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
        bufferedReader.close();
        if (!this.vocab.containsKey("[CLS]") || !this.vocab.containsKey("[SEP]") || !this.vocab.containsKey("[UNK]")) {
            throw new IOException("Invalid vocabulary");
        }
    }

    private boolean chinese(int i) {
        return (i >= 19968 && i <= 40959) || (i >= 13312 && i <= 19903) || ((i >= 131072 && i <= 173791) || ((i >= 173824 && i <= 177983) || ((i >= 177984 && i <= 178207) || ((i >= 178208 && i <= 183983) || ((i >= 63744 && i <= 64255) || (i >= 194560 && i <= 195103))))));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$pieces$0(StringBuilder sb, int i) {
        int type = Character.getType(i);
        if (type == 6 || i == 0 || i == 65533) {
            return;
        }
        if (Character.isWhitespace(i) || Character.isSpaceChar(i)) {
            sb.append(' ');
            return;
        }
        if (type == 15 || type == 16) {
            return;
        }
        if (chinese(i) || punctuation(i)) {
            sb.append(' ').appendCodePoint(i).append(' ');
        } else {
            sb.appendCodePoint(i);
        }
    }

    private boolean punctuation(int i) {
        int type = Character.getType(i);
        return (i >= 33 && i <= 47) || (i >= 58 && i <= 64) || ((i >= 91 && i <= 96) || ((i >= 123 && i <= 126) || type == 23 || type == 20 || type == 21 || type == 22 || type == 29 || type == 30 || type == 24));
    }

    public List<Integer> pieces(String str) {
        StringBuilder sb;
        String str2;
        final StringBuilder sb2 = new StringBuilder();
        String strNormalize = Normalizer.normalize(str.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        strNormalize.codePoints().forEach(new IntConsumer() { // from class: com.jimliddle.pocketdocs.WordPieceTokenizer$$ExternalSyntheticLambda0
            @Override // java.util.function.IntConsumer
            public final void accept(int i) {
                this.f$0.lambda$pieces$0(sb2, i);
            }
        });
        ArrayList arrayList = new ArrayList();
        String[] strArrSplit = sb2.toString().trim().split("\\s+");
        int length = strArrSplit.length;
        int i = 0;
        int i2 = 0;
        while (i2 < length) {
            String str3 = strArrSplit[i2];
            if (str3.isEmpty()) {
                sb = sb2;
                str2 = strNormalize;
            } else if (str3.codePointCount(i, str3.length()) > 100) {
                arrayList.add(this.vocab.get("[UNK]"));
                sb = sb2;
                str2 = strNormalize;
            } else {
                ArrayList arrayList2 = new ArrayList();
                int i3 = 0;
                boolean z = false;
                while (true) {
                    if (i3 >= str3.length()) {
                        sb = sb2;
                        str2 = strNormalize;
                        break;
                    }
                    int length2 = str3.length();
                    Integer num = null;
                    while (true) {
                        if (length2 <= i3) {
                            sb = sb2;
                            str2 = strNormalize;
                            break;
                        }
                        sb = sb2;
                        str2 = strNormalize;
                        num = this.vocab.get((i3 == 0 ? "" : "##") + str3.substring(i3, length2));
                        if (num != null) {
                            break;
                        }
                        length2 = str3.offsetByCodePoints(length2, -1);
                        sb2 = sb;
                        strNormalize = str2;
                    }
                    if (num == null) {
                        z = true;
                        break;
                    }
                    arrayList2.add(num);
                    i3 = length2;
                    sb2 = sb;
                    strNormalize = str2;
                }
                if (z) {
                    arrayList.add(this.vocab.get("[UNK]"));
                } else {
                    arrayList.addAll(arrayList2);
                }
            }
            i2++;
            sb2 = sb;
            strNormalize = str2;
            i = 0;
        }
        return arrayList;
    }

    public List<long[]> windows(String str) {
        List<Integer> listPieces = pieces(str);
        ArrayList arrayList = new ArrayList();
        int i = 0;
        while (true) {
            int iMin = Math.min(i + 254, listPieces.size());
            long[] jArr = new long[(iMin - i) + 2];
            jArr[0] = this.vocab.get("[CLS]").intValue();
            for (int i2 = i; i2 < iMin; i2++) {
                jArr[(i2 - i) + 1] = listPieces.get(i2).intValue();
            }
            jArr[jArr.length - 1] = this.vocab.get("[SEP]").intValue();
            arrayList.add(jArr);
            if (iMin == listPieces.size()) {
                return arrayList;
            }
            i = iMin - 32;
        }
    }
}
