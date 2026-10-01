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
        assertEquals("Paralela", alt.rotuloVersao());
        assertEquals("Teste", alt.nomeBase());
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

    @Test
    public void lerSetId() throws Exception {
        String json = "[{\"card_name\":\"Luffy\",\"card_set_id\":\"OP05-119\","
                + "\"card_image_id\":\"OP05-119\",\"set_name\":\"Awakening of the New Era\","
                + "\"set_id\":\"OP-05\"}]";
        Carta c = OnePieceFonte.parse(json, "OP05-119").get(0);
        assertEquals("OP-05", c.setId);
        assertEquals("Awakening of the New Era", c.colecao);
    }

    private static Carta carta(String codigo, String versao) {
        Carta c = new Carta();
        c.codigo = codigo;
        c.versao = versao;
        return c;
    }

    @Test
    public void checklistSoArteNormalDoProprioSet() {
        List<Carta> todas = java.util.Arrays.asList(
                carta("OP09-002", "OP09-002"),
                carta("OP09-001", "OP09-001"),
                carta("OP09-001", "OP09-001_p1"),   // arte alternativa: fora
                carta("OP05-119", "OP05-119_p6"),   // Wanted Poster de outro set: fora
                carta("OP09-001", "OP09-001"));     // repetida: uma só
        List<Carta> k = OnePieceFonte.checklist(todas, "OP-09");
        assertEquals(2, k.size());
        assertEquals("OP09-001", k.get(0).codigo);   // ordenado
        assertEquals("OP09-002", k.get(1).codigo);
    }

    @Test
    public void checklistStarterEPremium() {
        List<Carta> st = java.util.Arrays.asList(carta("ST01-001", "ST01-001"));
        assertEquals(1, OnePieceFonte.checklist(st, "ST-01").size());
        List<Carta> prb = java.util.Arrays.asList(
                carta("PRB01-001", "PRB01-001"),
                carta("OP05-119", "OP05-119_r1"));  // reimpressão: fora
        assertEquals(1, OnePieceFonte.checklist(prb, "PRB-01").size());
    }
}
