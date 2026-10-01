package com.gustavo.tcgcollection.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * ÚNICA classe do app que fala com a rede.
 * Auditar o que o app acessa = ler este arquivo.
 */
public final class Rede {

    private static final int TIMEOUT_MS = 15000;
    private static final String USER_AGENT = "tcg-collection-android/0.1";

    private Rede() {}

    /** Erro HTTP com o status (404, 500...). */
    public static class HttpErro extends IOException {
        public final int status;
        public HttpErro(int status, String url) {
            super("HTTP " + status + " em " + url);
            this.status = status;
        }
    }

    public static String getTexto(String url) throws IOException {
        return new String(getBytes(url), StandardCharsets.UTF_8);
    }

    public static byte[] getBytes(String url) throws IOException {
        String alvo = forcarHttps(url);
        HttpURLConnection con = (HttpURLConnection) new URL(alvo).openConnection();
        try {
            con.setConnectTimeout(TIMEOUT_MS);
            con.setReadTimeout(TIMEOUT_MS);
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setInstanceFollowRedirects(true);
            int status = con.getResponseCode();
            if (status < 200 || status >= 300) throw new HttpErro(status, alvo);
            try (InputStream in = con.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                return out.toByteArray();
            }
        } finally {
            con.disconnect();
        }
    }

    /** O app bloqueia HTTP puro (network_security_config); sobe pra HTTPS. */
    public static String forcarHttps(String url) {
        if (url == null) return null;
        String u = url.trim();
        if (u.regionMatches(true, 0, "http://", 0, 7)) return "https://" + u.substring(7);
        return u;
    }
}
