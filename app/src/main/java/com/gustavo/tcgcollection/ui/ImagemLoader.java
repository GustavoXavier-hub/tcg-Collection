package com.gustavo.tcgcollection.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.gustavo.tcgcollection.net.Rede;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Carrega imagens de carta com cache em memória + disco.
 * Cada imagem é baixada uma vez só (poupa a OPTCG API e o seu 4G).
 * A rede em si continua passando pela classe Rede.
 */
public final class ImagemLoader {

    private static final String TAG = "TcgImagem";
    private static final int LADO_MAX_PX = 700;

    private static ImagemLoader instancia;

    private final File pasta;
    private final LruCache<String, Bitmap> memoria;
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());

    public static synchronized ImagemLoader get(Context ctx) {
        if (instancia == null) instancia = new ImagemLoader(ctx.getApplicationContext());
        return instancia;
    }

    private ImagemLoader(Context ctx) {
        pasta = new File(ctx.getCacheDir(), "imagens");
        if (!pasta.exists() && !pasta.mkdirs()) Log.w(TAG, "não criou " + pasta);
        int kb = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
        memoria = new LruCache<String, Bitmap>(kb) {
            @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount() / 1024; }
        };
    }

    public void carregar(String url, ImageView alvo) {
        alvo.setTag(url);
        alvo.setImageDrawable(null);
        if (url == null || url.isEmpty()) return;

        Bitmap emMemoria = memoria.get(url);
        if (emMemoria != null) {
            alvo.setImageBitmap(emMemoria);
            return;
        }
        pool.execute(() -> {
            Bitmap b = obter(url);
            if (b == null) return;
            memoria.put(url, b);
            main.post(() -> {
                if (url.equals(alvo.getTag())) alvo.setImageBitmap(b);
            });
        });
    }

    private Bitmap obter(String url) {
        File arq = new File(pasta, nomeArquivo(url));
        try {
            byte[] bytes;
            if (arq.exists()) {
                bytes = Files.readAllBytes(arq.toPath());
            } else {
                bytes = Rede.getBytes(url);
                try (FileOutputStream out = new FileOutputStream(arq)) {
                    out.write(bytes);
                }
            }
            return decodificar(bytes);
        } catch (IOException e) {
            Log.w(TAG, "falhou " + url + ": " + e.getMessage());
            return null;
        }
    }

    private static Bitmap decodificar(byte[] bytes) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
        int amostra = 1;
        while (Math.max(o.outWidth, o.outHeight) / (amostra * 2) >= LADO_MAX_PX) amostra *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = amostra;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
    }

    private static String nomeArquivo(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] h = md.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : h) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(url.hashCode());
        }
    }
}
