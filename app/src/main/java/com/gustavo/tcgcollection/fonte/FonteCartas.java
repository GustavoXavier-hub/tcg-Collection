package com.gustavo.tcgcollection.fonte;

import com.gustavo.tcgcollection.model.Carta;

import java.io.IOException;
import java.util.List;

/**
 * Adaptador de um jogo. Cada TCG implementa isto, e o resto
 * do app não precisa saber de onde vêm os dados.
 * Implementações NÃO podem importar nada de android.* (para rodar nos testes do PC).
 */
public interface FonteCartas {

    /** Identificador gravado no banco: "onepiece", "magic"... */
    String jogo();

    /** Nome para a tela: "One Piece", "Magic"... */
    String nomeJogo();

    /** Exemplo de código para dicas na tela. */
    String exemploCodigo();

    /** Normaliza o que o usuário digitou. Retorna null se não for um código válido. */
    String normalizarCodigo(String entrada);

    /**
     * Busca todas as versões de uma carta (normal + artes alternativas).
     * Lista vazia = não encontrada. Roda fora da thread principal.
     */
    List<Carta> buscarPorCodigo(String codigoNormalizado) throws IOException;
}
