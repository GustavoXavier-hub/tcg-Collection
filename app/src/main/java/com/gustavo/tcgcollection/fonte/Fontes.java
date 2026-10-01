package com.gustavo.tcgcollection.fonte;

import java.util.Arrays;
import java.util.List;

/** Registro dos jogos suportados. Jogo novo = nova FonteCartas aqui. */
public final class Fontes {

    private static final List<FonteCartas> TODAS = Arrays.asList(
            new OnePieceFonte()
    );

    private Fontes() {}

    public static List<FonteCartas> todas() {
        return TODAS;
    }

    public static FonteCartas porJogo(String jogo) {
        for (FonteCartas f : TODAS) if (f.jogo().equals(jogo)) return f;
        return null;
    }

    public static String nomeDoJogo(String jogo) {
        FonteCartas f = porJogo(jogo);
        return f != null ? f.nomeJogo() : jogo;
    }
}
