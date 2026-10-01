package com.gustavo.tcgcollection.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A OPTCG API não tem campo de tipo de arte: ele vem como sufixo no nome.
 *   "Monkey.D.Luffy (119) (Alternate Art) (Manga)" → base "Monkey.D.Luffy (119)",
 *   etiquetas [Arte alternativa, Mangá].
 * Só vira etiqueta o parêntese com palavra de arte conhecida ({@link #PALAVRAS_DE_ARTE}).
 * O resto fica no nome: número "(119)", código "(OP05-119)" e apelidos como
 * "Mr.1 (Daz.Bonez)" ou "Miss Doublefinger (Zala)". Etiqueta reconhecida mas sem
 * tradução passa como veio (ex.: "Box Topper").
 * SEM \b e SEM (?U): o Android usa ICU.
 */
public final class TipoArte {

    /** Último "(...)" do texto. */
    static final Pattern SUFIXO = Pattern.compile("^(.*?)\\s*\\(([^()]*)\\)\\s*$");
    /** Separa palavras: "Gold-Stamped Signature" → gold, stamped, signature. */
    static final Pattern SEPARADOR = Pattern.compile("[^A-Za-z]+");

    /** Basta UMA dessas palavras para o parêntese ser tipo de arte. */
    static final Set<String> PALAVRAS_DE_ARTE = new HashSet<>(Arrays.asList(
            "alternate", "alt", "art", "parallel", "manga", "sp", "gold", "silver",
            "foil", "textured", "reprint", "wanted", "poster", "signature", "stamped",
            "promo", "topper", "prerelease", "pre", "release", "judge", "winner",
            "championship", "treasure", "serial", "numbered", "full", "anniversary"));

    private TipoArte() {}

    /** Resultado da leitura do nome. */
    public static final class Leitura {
        public final String nomeBase;
        public final List<String> etiquetas;

        Leitura(String nomeBase, List<String> etiquetas) {
            this.nomeBase = nomeBase;
            this.etiquetas = Collections.unmodifiableList(etiquetas);
        }

        /** "Arte alternativa · Mangá", ou null se for a arte normal. */
        public String rotulo() {
            return etiquetas.isEmpty() ? null : String.join(" · ", etiquetas);
        }
    }

    public static Leitura ler(String nome) {
        List<String> etiquetas = new ArrayList<>();
        if (nome == null) return new Leitura("", etiquetas);

        String resto = nome.trim();
        List<String> identificadores = new ArrayList<>();
        Matcher m;
        while ((m = SUFIXO.matcher(resto)).matches()) {
            String dentro = m.group(2).trim();
            String antes = m.group(1);
            if (antes.isEmpty()) break; // o nome inteiro entre parênteses: não mexe
            if (ehArte(dentro)) {
                etiquetas.add(0, traduzir(dentro));
            } else if (!dentro.isEmpty()) {
                identificadores.add(0, "(" + dentro + ")");
            }
            resto = antes;
        }
        StringBuilder base = new StringBuilder(resto);
        for (String id : identificadores) base.append(' ').append(id);
        return new Leitura(base.toString(), etiquetas);
    }

    static boolean ehArte(String dentro) {
        for (String p : SEPARADOR.split(dentro.toLowerCase(Locale.ROOT))) {
            if (PALAVRAS_DE_ARTE.contains(p)) return true;
        }
        return false;
    }

    static String traduzir(String etiqueta) {
        switch (etiqueta.toLowerCase(Locale.ROOT)) {
            case "alternate art":
            case "alt art":
                return "Arte alternativa";
            case "parallel":
                return "Paralela";
            case "manga":
                return "Mangá";
            case "reprint":
                return "Reimpressão";
            case "full art":
                return "Full art";
            case "gold-stamped signature":
                return "Assinatura dourada";
            default:
                // SP, Gold, Wanted Poster, foils, promos...: nome original, que é como se fala.
                return etiqueta;
        }
    }
}
