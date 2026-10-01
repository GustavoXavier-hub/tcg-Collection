package com.gustavo.tcgcollection.ui;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

/**
 * Imagens da persona (cartaz WANTED, Sogeking) só existem na máquina do Gustavo
 * (app/src/main/res-local, fora do Git). Busca pelo nome em vez de R.drawable
 * para o projeto compilar sem elas.
 */
final class PersonaImagens {

    static final String SPLASH = "persona_splash";
    static final String VAZIO = "persona_vazio";
    static final String FUNDO = "persona_fundo";

    private PersonaImagens() {}

    /** Põe a imagem se ela existir; senão usa o fallback (ou esconde a view se fallback = 0). */
    static void aplicar(ImageView v, String nome, int fallback) {
        Context c = v.getContext();
        int id = c.getResources().getIdentifier(nome, "drawable", c.getPackageName());
        if (id != 0) {
            v.setImageResource(id);
        } else if (fallback != 0) {
            v.setImageResource(fallback);
        } else {
            v.setVisibility(View.GONE);
        }
    }

    static boolean existe(Context c, String nome) {
        return c.getResources().getIdentifier(nome, "drawable", c.getPackageName()) != 0;
    }
}
