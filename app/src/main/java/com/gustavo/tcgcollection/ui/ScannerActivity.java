package com.gustavo.tcgcollection.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.gustavo.tcgcollection.R;
import com.gustavo.tcgcollection.fonte.FonteCartas;
import com.gustavo.tcgcollection.fonte.Fontes;
import com.gustavo.tcgcollection.fonte.OnePieceFonte;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lê o código impresso na carta pela câmera (ML Kit, no aparelho, sem rede).
 * Analisa todo frame da preview; quando o MESMO código aparece em
 * {@link #LEITURAS_IGUAIS} frames seguidos, devolve ele em {@link #EXTRA_CODIGO}.
 * Exigir repetição evita aceitar uma leitura errada de um frame tremido.
 */
public class ScannerActivity extends AppCompatActivity {

    public static final String EXTRA_CODIGO = "codigo";

    private static final String TAG = "TcgScanner";
    private static final int LEITURAS_IGUAIS = 2;

    private final ExecutorService analise = Executors.newSingleThreadExecutor();
    private final AtomicBoolean terminou = new AtomicBoolean(false);
    private final TextRecognizer ocr = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    private FonteCartas fonte;
    private PreviewView preview;
    private TextView txtDica;
    private ImageButton btnLanterna;
    private Camera camera;
    private boolean lanternaLigada;

    // Só mexidos na thread de análise.
    private String ultimoCodigo;
    private int repeticoes;

    private final ActivityResultLauncher<String> pedirPermissao =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), ok -> {
                if (ok) iniciarCamera();
                else txtDica.setText(R.string.scanner_sem_permissao);
            });

    @Override
    protected void onCreate(Bundle salvo) {
        super.onCreate(salvo);
        setContentView(R.layout.activity_scanner);

        fonte = Fontes.porJogo(OnePieceFonte.JOGO);
        preview = findViewById(R.id.preview);
        txtDica = findViewById(R.id.txtDica);
        btnLanterna = findViewById(R.id.btnLanterna);

        findViewById(R.id.btnFechar).setOnClickListener(v -> finish());
        btnLanterna.setOnClickListener(v -> alternarLanterna());
        txtDica.setText(getString(R.string.scanner_dica, fonte.exemploCodigo()));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarCamera();
        } else {
            pedirPermissao.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        analise.shutdown();
        ocr.close();
    }

    private void iniciarCamera() {
        ListenableFuture<ProcessCameraProvider> futuro = ProcessCameraProvider.getInstance(this);
        futuro.addListener(() -> {
            try {
                ProcessCameraProvider provider = futuro.get();
                Preview p = new Preview.Builder().build();
                p.setSurfaceProvider(preview.getSurfaceProvider());

                ImageAnalysis analisador = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analisador.setAnalyzer(analise, this::analisar);

                provider.unbindAll();
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, p, analisador);
                boolean temFlash = camera.getCameraInfo().hasFlashUnit();
                btnLanterna.setVisibility(temFlash ? android.view.View.VISIBLE : android.view.View.GONE);
            } catch (Exception e) {
                Log.e(TAG, "falha ao abrir a câmera", e);
                txtDica.setText(getString(R.string.scanner_erro_camera, AdicionarActivity.descrever(e)));
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analisar(@NonNull ImageProxy frame) {
        if (terminou.get() || frame.getImage() == null) {
            frame.close();
            return;
        }
        InputImage img = InputImage.fromMediaImage(frame.getImage(), frame.getImageInfo().getRotationDegrees());
        ocr.process(img)
                .addOnSuccessListener(analise, texto -> conferir(fonte.acharCodigoNoTexto(texto.getText())))
                .addOnCompleteListener(analise, t -> frame.close());
    }

    /** Roda na thread de análise. */
    private void conferir(String codigo) {
        if (codigo == null) {
            repeticoes = 0;
            ultimoCodigo = null;
            return;
        }
        repeticoes = codigo.equals(ultimoCodigo) ? repeticoes + 1 : 1;
        ultimoCodigo = codigo;
        if (repeticoes >= LEITURAS_IGUAIS && terminou.compareAndSet(false, true)) {
            runOnUiThread(() -> devolver(codigo));
        } else {
            runOnUiThread(() -> txtDica.setText(getString(R.string.scanner_lendo, codigo)));
        }
    }

    private void devolver(String codigo) {
        preview.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_CODIGO, codigo));
        finish();
    }

    /** Carta brilhante reflete: às vezes ler com a lanterna ajuda, às vezes atrapalha. */
    private void alternarLanterna() {
        if (camera == null) return;
        lanternaLigada = !lanternaLigada;
        camera.getCameraControl().enableTorch(lanternaLigada);
        btnLanterna.setAlpha(lanternaLigada ? 1f : 0.6f);
    }
}
