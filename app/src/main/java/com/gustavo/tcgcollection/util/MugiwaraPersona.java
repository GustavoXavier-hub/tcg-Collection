package com.gustavo.tcgcollection.util;

import java.util.Locale;
import java.util.Random;

/**
 * Usopp, o atirador da tripulação, é quem "cuida" da coleção.
 * Tom: gabola, mentiroso de bom coração, medroso cômico, corajoso quando importa.
 * Sem rede e sem android.* — testável no PC.
 */
public final class MugiwaraPersona {

    public enum Contexto { SPLASH, VAZIO, SUCESSO, NAO_ENCONTRADA, ERRO, SET_COMPLETO }

    // Mesma ordem do enum Contexto.
    private static final String[][] FRASES = {
            { // SPLASH
                    "Eu sou o grande Capitão Usopp, dono de 8000 cartas! ...Bom, quase.",
                    "Hissatsu: Organização Perfeita! Carregando...",
                    "Nenhuma carta escapa da mira do maior atirador dos mares.",
                    "Uma vez eu vi uma carta tão rara que brilhava no escuro. Juro!",
                    "A doença de não-posso-abrir-esse-app... passou. Vamos lá!",
                    "Cada carta tem uma história. A minha é sempre a mais heroica.",
            },
            { // VAZIO
                    "Nenhuma carta ainda? Eu tinha 8000... um gigante roubou todas!",
                    "Coleção vazia. Até o Capitão Usopp começou com uma carta só.",
                    "Nada aqui... ainda. Toda lenda começa com o primeiro tiro.",
            },
            { // SUCESSO
                    "Na mosca! Mais uma pra coleção do Capitão Usopp.",
                    "Acertei de primeira. Como sempre. (Não conta pra ninguém.)",
                    "Guardada! Os 8000 seguidores do Capitão Usopp aplaudem.",
                    "Mais uma! Essa eu acertei a 5 km de distância.",
            },
            { // NAO_ENCONTRADA
                    "Errei o tiro... essa carta não apareceu. Confere o código no canto dela.",
                    "Nem com o Kabuto eu achei essa. O código tá certo?",
                    "Essa carta não existe. E olha que quem tá dizendo é o rei da mentira.",
            },
            { // ERRO
                    "Ugh! Ataque da doença de não-posso-acessar-a-internet!",
                    "O mar tá revolto e a conexão afundou. Tenta de novo daqui a pouco.",
                    "Algo deu errado. Mas um bravo guerreiro do mar não desiste!",
            },
            { // SET_COMPLETO
                    "SET COMPLETO! Contem pra todos: o Capitão Usopp nunca erra um tiro!",
                    "Nenhuma faltando! Os 8000 seguidores estão chorando de emoção.",
                    "Completo! Essa história eu nem preciso aumentar.",
            },
    };

    private static final Random RAND = new Random();

    private MugiwaraPersona() {}

    public static String frase(Contexto ctx) {
        return frase(ctx, RAND);
    }

    static String frase(Contexto ctx, Random r) {
        String[] pool = FRASES[ctx.ordinal()];
        return pool[r.nextInt(pool.length)];
    }

    static int quantasFrases(Contexto ctx) {
        return FRASES[ctx.ordinal()].length;
    }

    /**
     * Frase especial ao salvar uma carta, pelo nome dela ou pela quantidade.
     * @return a frase, ou null se nada combinar
     */
    public static String easterEgg(String nomeCarta, int quantidade) {
        String n = nomeCarta == null ? "" : nomeCarta.toLowerCase(Locale.ROOT);
        if (n.contains("sogeking"))
            return "Sogeking? Nunca ouvi falar. Deve ser um herói da ilha dos atiradores.";
        if (n.contains("yasopp"))
            return "Meu pai! O maior atirador do mundo... depois de mim, claro.";
        if (n.contains("usopp"))
            return "O GRANDE CAPITÃO USOPP! Obviamente a carta mais forte do jogo.";
        if (n.contains("kaya"))
            return "K-Kaya?! Guarda essa com cuidado. Com MUITO cuidado.";
        if (n.contains("merry"))
            return "A Merry... ela nunca vai ser só uma carta.";
        if (n.contains("luffy"))
            return "O capitão! ...Tá, o outro capitão. O Capitão Usopp sou eu.";
        if (n.contains("kaido") || n.contains("big mom") || n.contains("linlin") || n.contains("akainu"))
            return "Ai, ai... me deu a doença de não-posso-encostar-nessa-carta.";
        if (quantidade >= 4)
            return "Quatro de uma vez?! Até o Capitão Usopp ficou impressionado.";
        return null;
    }
}
