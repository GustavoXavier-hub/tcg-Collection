package com.gustavo.tcgcollection.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

/** Nomes reais da OPTCG API (OP05-119 e variações, consultados em 30/09/2026). */
public class TipoArteTest {

    @Test
    public void normalNaoTemEtiqueta() {
        TipoArte.Leitura l = TipoArte.ler("Monkey.D.Luffy (119)");
        assertEquals("Monkey.D.Luffy (119)", l.nomeBase);
        assertTrue(l.etiquetas.isEmpty());
        assertNull(l.rotulo());
    }

    @Test
    public void arteAlternativa() {
        TipoArte.Leitura l = TipoArte.ler("Monkey.D.Luffy (119) (Alternate Art)");
        assertEquals("Monkey.D.Luffy (119)", l.nomeBase);
        assertEquals("Arte alternativa", l.rotulo());
    }

    @Test
    public void variasEtiquetasNaOrdem() {
        assertEquals(Arrays.asList("Arte alternativa", "Mangá"),
                TipoArte.ler("Monkey.D.Luffy (119) (Alternate Art) (Manga)").etiquetas);
        assertEquals("SP · Gold", TipoArte.ler("Monkey.D.Luffy (119) (SP) (Gold)").rotulo());
    }

    @Test
    public void codigoFicaNoNomeBase() {
        TipoArte.Leitura l = TipoArte.ler("Monkey.D.Luffy (OP05-119) (Reprint)");
        assertEquals("Monkey.D.Luffy (OP05-119)", l.nomeBase);
        assertEquals("Reimpressão", l.rotulo());
    }

    @Test
    public void semNumeroEDesconhecidaPassaComoVeio() {
        TipoArte.Leitura l = TipoArte.ler("Monkey.D.Luffy (Wanted Poster)");
        assertEquals("Monkey.D.Luffy", l.nomeBase);
        assertEquals("Wanted Poster", l.rotulo());
        assertEquals("Box Topper", TipoArte.ler("Nami (Box Topper)").rotulo());
    }

    @Test
    public void apelidoNaoEhEtiqueta() {
        // Nomes reais do OP-05: o parêntese é o codinome do personagem.
        TipoArte.Leitura l = TipoArte.ler("Mr.1 (Daz.Bonez)");
        assertEquals("Mr.1 (Daz.Bonez)", l.nomeBase);
        assertNull(l.rotulo());
        TipoArte.Leitura z = TipoArte.ler("Miss Doublefinger (Zala) (Alternate Art)");
        assertEquals("Miss Doublefinger (Zala)", z.nomeBase);
        assertEquals("Arte alternativa", z.rotulo());
    }

    @Test
    public void assinaturaDourada() {
        assertEquals("Arte alternativa · Assinatura dourada",
                TipoArte.ler("Monkey.D.Luffy (Alternate Art) (Gold-Stamped Signature)").rotulo());
    }

    @Test
    public void nomesEsquisitos() {
        assertEquals("", TipoArte.ler(null).nomeBase);
        assertEquals("Usopp", TipoArte.ler("  Usopp  ").nomeBase);
        // Nome inteiro entre parênteses: não vira etiqueta.
        assertEquals("(Teste)", TipoArte.ler("(Teste)").nomeBase);
        assertNull(TipoArte.ler("Nami ()").rotulo());
    }
}
