package com.jimliddle.pocketdocs;

import com.tom_roush.pdfbox.contentstream.operator.OperatorName;
import com.tom_roush.pdfbox.pdmodel.common.PDPageLabelRange;
import com.tom_roush.pdfbox.pdmodel.documentinterchange.taggedpdf.PDPrintFieldAttributeObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes3.dex */
public final class TextTools {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}_-]*");
    private static final Set<String> STOP = new HashSet(Arrays.asList(PDPageLabelRange.STYLE_LETTERS_LOWER, "an", "the", "is", "are", "was", "were", "of", "for", "to", "in", PDPrintFieldAttributeObject.CHECKED_STATE_ON, "and", "or", "my", "me", "find", "what", "which", "where", "how", "does", "do", "did", "it", "its", "with", "from", "that", "this", "have", "has", OperatorName.SET_FLATNESS, "can", "you", "please", "tell", "about"));

    public static List<String> chunks(String str) {
        int iLastIndexOf;
        String strTrim = str.replace((char) 0, ' ').replaceAll("[\\t\\x0B\\f\\r ]+", " ").trim();
        ArrayList arrayList = new ArrayList();
        int iMax = 0;
        while (iMax < strTrim.length()) {
            int iMin = Math.min(iMax + 1600, strTrim.length());
            if (iMin < strTrim.length() && (iLastIndexOf = strTrim.lastIndexOf(32, iMin)) > iMax + 1000) {
                iMin = iLastIndexOf;
            }
            arrayList.add(strTrim.substring(iMax, iMin));
            if (iMin == strTrim.length()) {
                break;
            }
            iMax = Math.max(iMax + 1, iMin - 180);
        }
        return arrayList;
    }

    public static String ftsQuery(String str) {
        ArrayList arrayList = new ArrayList();
        Iterator<String> it = words(str).iterator();
        while (it.hasNext()) {
            arrayList.add(OperatorName.SHOW_TEXT_LINE_AND_SPACE + it.next() + OperatorName.SHOW_TEXT_LINE_AND_SPACE);
        }
        return String.join(" OR ", arrayList);
    }

    static String hash(String str) {
        try {
            byte[] bArrDigest = MessageDigest.getInstance("SHA-256").digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bArrDigest) {
                sb.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(b & 255)));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static int score(String str, String str2, String str3) {
        String lowerCase = str2.toLowerCase(Locale.ROOT);
        String lowerCase2 = str3.toLowerCase(Locale.ROOT);
        int i = 0;
        for (String str4 : words(str)) {
            if (lowerCase.contains(str4)) {
                i += 4;
            }
            if (lowerCase2.contains(str4)) {
                i += 2;
            }
        }
        return i;
    }

    static Set<String> words(String str) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        Matcher matcher = WORD.matcher(str.toLowerCase(Locale.ROOT));
        while (matcher.find() && linkedHashSet.size() < 16) {
            if (!STOP.contains(matcher.group()) && matcher.group().length() <= 80) {
                linkedHashSet.add(matcher.group());
            }
        }
        return linkedHashSet;
    }
}
