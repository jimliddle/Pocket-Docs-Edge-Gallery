package com.jimliddle.pocketdocs;

import fi.iki.elonen.NanoHTTPD;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes3.dex */
public final class LocalServer extends NanoHTTPD {
    public static final int PORT = 8766;
    private final McpEngine engine;
    private final byte[] secret;

    public LocalServer(String str, DocRepository docRepository) {
        super("127.0.0.1", PORT);
        this.secret = ("Bearer " + str).getBytes(StandardCharsets.UTF_8);
        this.engine = new McpEngine(docRepository);
    }

    private NanoHTTPD.Response json(JSONObject jSONObject) {
        return response(NanoHTTPD.Response.Status.OK, jSONObject.toString());
    }

    private NanoHTTPD.Response response(NanoHTTPD.Response.Status status, String str) {
        NanoHTTPD.Response responseNewFixedLengthResponse = newFixedLengthResponse(status, "application/json; charset=utf-8", str);
        responseNewFixedLengthResponse.addHeader("Cache-Control", "no-store");
        responseNewFixedLengthResponse.addHeader("X-Content-Type-Options", "nosniff");
        return responseNewFixedLengthResponse;
    }

    @Override // fi.iki.elonen.NanoHTTPD
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession iHTTPSession) throws NumberFormatException, IOException {
        Map<String, String> headers = iHTTPSession.getHeaders();
        String lowerCase = headers.getOrDefault("host", "").toLowerCase(Locale.ROOT);
        if (!lowerCase.equals("127.0.0.1:8766") && !lowerCase.equals("localhost:8766")) {
            return response(NanoHTTPD.Response.Status.FORBIDDEN, "Invalid Host");
        }
        if (headers.containsKey("origin")) {
            return response(NanoHTTPD.Response.Status.FORBIDDEN, "Browser origins are not permitted");
        }
        if (!MessageDigest.isEqual(this.secret, headers.getOrDefault("authorization", "").getBytes(StandardCharsets.UTF_8))) {
            NanoHTTPD.Response response = response(NanoHTTPD.Response.Status.UNAUTHORIZED, "Bearer token required");
            response.addHeader("WWW-Authenticate", "Bearer");
            return response;
        }
        if (!"/mcp".equals(iHTTPSession.getUri())) {
            return response(NanoHTTPD.Response.Status.NOT_FOUND, "Use /mcp");
        }
        if (iHTTPSession.getMethod() != NanoHTTPD.Method.POST) {
            NanoHTTPD.Response response2 = response(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, "Use POST; stateless JSON transport has no SSE stream");
            response2.addHeader("Allow", "POST");
            return response2;
        }
        if (!headers.getOrDefault("content-type", "").toLowerCase(Locale.ROOT).startsWith("application/json")) {
            return response(NanoHTTPD.Response.Status.UNSUPPORTED_MEDIA_TYPE, "Use application/json");
        }
        String orDefault = headers.getOrDefault("accept", "");
        if (!orDefault.contains("application/json") && !orDefault.contains("*/*")) {
            return response(NanoHTTPD.Response.Status.NOT_ACCEPTABLE, "Accept application/json");
        }
        if (headers.containsKey("transfer-encoding")) {
            return response(NanoHTTPD.Response.Status.BAD_REQUEST, "Chunked request bodies are not supported");
        }
        String str = headers.get("mcp-protocol-version");
        if (str != null && !Arrays.asList("2024-11-05", "2025-03-26", "2025-06-18").contains(str)) {
            return response(NanoHTTPD.Response.Status.BAD_REQUEST, "Unsupported MCP protocol version");
        }
        try {
            int i = Integer.parseInt(headers.getOrDefault("content-length", "-1"));
            if (i < 0 || i > 65536) {
                return response(NanoHTTPD.Response.Status.BAD_REQUEST, "Body must have Content-Length <= 65536");
            }
            byte[] bArr = new byte[i];
            int i2 = 0;
            while (i2 < i) {
                int i3 = iHTTPSession.getInputStream().read(bArr, i2, i - i2);
                if (i3 < 0) {
                    throw new EOFException();
                }
                i2 += i3;
            }
            try {
                JSONObject jSONObjectHandle = this.engine.handle(new JSONObject(new String(bArr, StandardCharsets.UTF_8)));
                return jSONObjectHandle == null ? response(NanoHTTPD.Response.Status.ACCEPTED, "") : json(jSONObjectHandle);
            } catch (JSONException e) {
                return json(McpEngine.error(null, -32700, "Expected a JSON-RPC object"));
            }
        } catch (Exception e2) {
            return response(NanoHTTPD.Response.Status.BAD_REQUEST, "Invalid or incomplete request");
        }
    }
}
