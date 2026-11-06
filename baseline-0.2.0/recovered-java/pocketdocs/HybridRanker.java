package com.jimliddle.pocketdocs;

import com.jimliddle.pocketdocs.HybridRanker;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/* loaded from: classes3.dex */
public final class HybridRanker {
    public static final double MIN_COSINE = 0.25d;

    public static final class Hit {
        public final String id;
        public final double score;

        public Hit(String str, double d) {
            this.id = str;
            this.score = d;
        }
    }

    public static double cosine(float[] fArr, byte[] bArr) {
        if (bArr == null || bArr.length != fArr.length * 4) {
            return -1.0d;
        }
        ByteBuffer byteBufferOrder = ByteBuffer.wrap(bArr).order(ByteOrder.LITTLE_ENDIAN);
        double d = 0.0d;
        for (float f : fArr) {
            d += byteBufferOrder.getFloat() * f;
        }
        if (Double.isFinite(d)) {
            return Math.max(-1.0d, Math.min(1.0d, d));
        }
        return -1.0d;
    }

    public static byte[] encode(float[] fArr) {
        ByteBuffer byteBufferOrder = ByteBuffer.allocate(fArr.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : fArr) {
            byteBufferOrder.putFloat(f);
        }
        return byteBufferOrder.array();
    }

    public static List<Hit> fuse(List<Hit> list, List<Hit> list2, int i) {
        HashMap map = new HashMap();
        for (List list3 : Arrays.asList(list, list2)) {
            for (int i2 = 0; i2 < list3.size(); i2++) {
                map.merge(((Hit) list3.get(i2)).id, Double.valueOf(1.0d / ((i2 + 60) + 1)), new BiFunction() { // from class: com.jimliddle.pocketdocs.HybridRanker$$ExternalSyntheticLambda2
                    @Override // java.util.function.BiFunction
                    public final Object apply(Object obj, Object obj2) {
                        return Double.valueOf(Double.sum(((Double) obj).doubleValue(), ((Double) obj2).doubleValue()));
                    }
                });
            }
        }
        ArrayList arrayList = new ArrayList();
        for (Map.Entry entry : map.entrySet()) {
            arrayList.add(new Hit((String) entry.getKey(), ((Double) entry.getValue()).doubleValue()));
        }
        return sorted(arrayList, i);
    }

    public static float[] normalize(float[] fArr) {
        double d = 0.0d;
        for (float f : fArr) {
            if (!Float.isFinite(f)) {
                throw new IllegalArgumentException("Non-finite vector");
            }
            d += r4 * r4;
        }
        if (d == 0.0d) {
            throw new IllegalArgumentException("Empty vector");
        }
        float fSqrt = (float) (1.0d / Math.sqrt(d));
        for (int i = 0; i < fArr.length; i++) {
            fArr[i] = fArr[i] * fSqrt;
        }
        return fArr;
    }

    public static List<Hit> sorted(Collection<Hit> collection, int i) {
        ArrayList arrayList = new ArrayList(collection);
        arrayList.sort(Comparator.comparingDouble(new ToDoubleFunction() { // from class: com.jimliddle.pocketdocs.HybridRanker$$ExternalSyntheticLambda0
            @Override // java.util.function.ToDoubleFunction
            public final double applyAsDouble(Object obj) {
                return ((HybridRanker.Hit) obj).score;
            }
        }).reversed().thenComparing(new Function() { // from class: com.jimliddle.pocketdocs.HybridRanker$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((HybridRanker.Hit) obj).id;
            }
        }));
        return new ArrayList(arrayList.subList(0, Math.min(i, arrayList.size())));
    }
}
