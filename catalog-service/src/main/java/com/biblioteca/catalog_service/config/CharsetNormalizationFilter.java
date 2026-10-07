package com.biblioteca.catalog_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Si el cuerpo JSON no es UTF-8 válido (por ejemplo, un cliente que envía Windows-1252),
 * lo reinterpreta como Windows-1252 y lo convierte a UTF-8. Un cuerpo UTF-8 válido no se toca.
 */
@Component
public class CharsetNormalizationFilter extends OncePerRequestFilter {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final long MAX_BYTES = 1_048_576; // 1 MB

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        boolean conCuerpo = "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
        String contentType = request.getContentType();
        boolean esJson = contentType != null && contentType.toLowerCase().startsWith("application/json");
        return !conCuerpo || !esJson || request.getContentLengthLong() > MAX_BYTES;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        byte[] body = request.getInputStream().readAllBytes();
        byte[] normalizado = esUtf8Valido(body)
                ? body
                : new String(body, WINDOWS_1252).getBytes(StandardCharsets.UTF_8);
        filterChain.doFilter(new CuerpoRequest(request, normalizado), response);
    }

    private boolean esUtf8Valido(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private static class CuerpoRequest extends HttpServletRequestWrapper {

        private final byte[] cuerpo;

        CuerpoRequest(HttpServletRequest request, byte[] cuerpo) {
            super(request);
            this.cuerpo = cuerpo;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(cuerpo);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return in.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { }
                @Override public int read() { return in.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return cuerpo.length;
        }

        @Override
        public long getContentLengthLong() {
            return cuerpo.length;
        }
    }
}