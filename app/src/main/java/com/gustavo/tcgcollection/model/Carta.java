package com.gustavo.tcgcollection.model;

/**
 * Carta genérica, de qualquer TCG.
 * O que é comum a todos os jogos vira campo; o que é específico
 * (custo, poder, counter, HP, mana...) vai em {@code extrasJson}.
 */
public class Carta {

    /** Identificador do jogo: "onepiece", "magic", "pokemon"... */
    public String jogo;
    /** Código impresso na carta, ex.: OP12-034. */
    public String codigo;
    /** Versão específica (arte alternativa etc.). Ex.: OP12-034_p1. Nunca nula. */
    public String versao;
    public String nome;
    public String colecao;
    public String raridade;
    public String tipo;
    public String cor;
    public String imagemUrl;
    /** Preço de mercado em USD informado pela fonte. NaN = desconhecido. */
    public double precoMercadoUsd = Double.NaN;
    /** Dados específicos do jogo, em JSON. */
    public String extrasJson = "{}";

    /** true quando é a arte padrão (versão igual ao código). */
    public boolean ehVersaoPadrao() {
        return versao == null || versao.equalsIgnoreCase(codigo);
    }

    /** Rótulo curto da versão para mostrar na tela. */
    public String rotuloVersao() {
        if (ehVersaoPadrao()) return "Normal";
        int i = versao.lastIndexOf("_p");
        if (i >= 0 && i + 2 < versao.length()) {
            return "Arte alternativa " + versao.substring(i + 2);
        }
        return versao;
    }
}
