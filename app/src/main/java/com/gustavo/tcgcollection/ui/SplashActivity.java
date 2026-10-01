package com.gustavo.tcgcollection.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.gustavo.tcgcollection.R;
import com.gustavo.tcgcollection.util.MugiwaraPersona;

/** Abertura com o cartaz do Usopp e uma frase dele. 1,5 s e segue pra coleção. */
public class SplashActivity extends AppCompatActivity {

    private static final long DURACAO_MS = 1500;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable seguir = this::seguir;

    @Override
    protected void onCreate(Bundle salvo) {
        super.onCreate(salvo);
        setContentView(R.layout.activity_splash);

        ImageView imagem = findViewById(R.id.splashImagem);
        if (PersonaImagens.existe(this, PersonaImagens.SPLASH)) {
            imagem.getLayoutParams().height = (int) (380 * getResources().getDisplayMetrics().density);
        }
        PersonaImagens.aplicar(imagem, PersonaImagens.SPLASH, R.drawable.ic_launcher_foreground);

        TextView frase = findViewById(R.id.splashFrase);
        frase.setText(MugiwaraPersona.frase(MugiwaraPersona.Contexto.SPLASH));

        main.postDelayed(seguir, DURACAO_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Se o usuário sair durante a splash, não abre a MainActivity depois.
        main.removeCallbacks(seguir);
    }

    private void seguir() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
