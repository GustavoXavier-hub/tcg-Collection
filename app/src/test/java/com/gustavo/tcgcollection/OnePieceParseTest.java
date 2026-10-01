package com.gustavo.tcgcollection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gustavo.tcgcollection.fonte.OnePieceFonte;
import com.gustavo.tcgcollection.model.Carta;

import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

/**
 * Formato baseado na documentação da OPTCG API (campos card_*).
 * Se a busca no celular vier vazia, compare a resposta real
 * (abra https://optcgapi.com/api/sets/card/OP12-001/ no navegador) com estes JSONs.
 */
public class OnePieceParseTest {

    private static final String DUAS_VERSOES = "["
            + "{\"card_name\":\"Teste (Parallel)\",\"card_set_id\":\"OP12-034\",\"card_image_id\":\"OP12-034_p1\","
            + "\"card_image\":\"http://exemplo.com/OP12-034_p1.jpg\",\"rarity\":\"SR\",\"market_price\":\"25.10\"},"
            + "{\"card_name\":\"Teste\",\"card_set_id\":\"OP12-034\",\"card_image_id\":\"OP12-034\","
            + "\"card_image\":\"https://exemplo.com/OP12-034.jpg\",\"set_name\":\"Set Teste\",\"rarity\":\"SR\","
            + "\"card_type\":\"Character\",\"card_color\":\"Red\",\"card_cost\":\"5\",\"card_power\":6000,"
            + "\"counter_amount\":null,\"market_price\":1.5}"
            + "]";

    @Test
    public void lerListaComArteAlternativa() throws Exception {
        List<Carta> l = OnePieceFonte.parse(DUAS_VERSOES, "OP12-034");
        assertEquals(2, l.size());
        // normal vem primeiro mesmo estando em segundo no JSON
        Carta normal = l.get(0);
        assertTrue(normal.ehVersaoPadrao());
        assertEquals("Teste", normal.nome);
        assertEquals("Red", normal.cor);
        assertEquals(1.5, normal.precoMercadoUsd, 0.0001);
        assertEquals("Normal", normal.rotuloVersao());

        Carta alt = l.get(1);
        assertFalse(alt.ehVersaoPadrao());
        assertEquals("Arte alternativa 1", alt.rotuloVersao());
        assertEquals(25.10, alt.precoMercadoUsd, 0.0001);
        // http vira https
        assertEquals("https://exemplo.com/OP12-034_p1.jpg", alt.imagemUrl);
    }

    @Test
    public void extrasGuardamDadosDoJogo() throws Exception {
        Carta c = OnePieceFonte.parse(DUAS_VERSOES, "OP12-034").get(0);
        JSONObject e = new JSONObject(c.extrasJson);
        assertEquals("5", e.getString("custo"));
        assertEquals("6000", e.getString("poder"));
        assertEquals("", e.getString("counter"));
    }

    @Test
    public void objetoUnico() throws Exception {
        List<Carta> l = OnePieceFonte.parse("{\"card_name\":\"Solo\",\"card_set_id\":\"ST10-001\"}", "ST10-001");
        assertEquals(1, l.size());
        assertEquals("ST10-001", l.get(0).versao); // sem card_image_id usa o código
    }

    @Test
    public void erroDaApiViraListaVazia() throws Exception {
        assertTrue(OnePieceFonte.parse("{\"error\":\"not found\"}", "OP99-999").isEmpty());
        assertTrue(OnePieceFonte.parse("[]", "OP99-999").isEmpty());
        assertTrue(OnePieceFonte.parse("", "OP99-999").isEmpty());
    }

    @Test
    public void versoesDuplicadasSaoUnificadas() throws Exception {
        String dup = "[{\"card_name\":\"A\",\"card_image_id\":\"OP01-001\"},"
                + "{\"card_name\":\"A\",\"card_image_id\":\"OP01-001\"}]";
        assertEquals(1, OnePieceFonte.parse(dup, "OP01-001").size());
    }

    @Test
    public void precoInvalidoViraNaN() throws Exception {
        Carta c = OnePieceFonte.parse("{\"card_name\":\"A\",\"market_price\":\"N/A\"}", "OP01-001").get(0);
        assertTrue(Double.isNaN(c.precoMercadoUsd));
    }
}
