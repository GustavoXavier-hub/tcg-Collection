package com.gustavo.tcgcollection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.gustavo.tcgcollection.fonte.OnePieceFonte;

import org.junit.Test;

public class CodigoOnePieceTest {

    private final OnePieceFonte f = new OnePieceFonte();

    @Test public void formatoOficial() { assertEquals("OP12-034", f.normalizarCodigo("OP12-034")); }
    @Test public void minusculo() { assertEquals("OP12-034", f.normalizarCodigo("op12-034")); }
    @Test public void semHifen() { assertEquals("OP12-034", f.normalizarCodigo("op12034")); }
    @Test public void comEspaco() { assertEquals("OP12-034", f.normalizarCodigo(" op12 34 ")); }
    @Test public void semZeros() { assertEquals("OP01-001", f.normalizarCodigo("op1-1")); }
    @Test public void hifenDepoisDoPrefixo() { assertEquals("OP12-034", f.normalizarCodigo("OP-12-034")); }
    @Test public void starterDeck() { assertEquals("ST10-001", f.normalizarCodigo("st10-1")); }
    @Test public void extraBooster() { assertEquals("EB02-010", f.normalizarCodigo("eb02-010")); }
    @Test public void premiumBooster() { assertEquals("PRB01-001", f.normalizarCodigo("prb1-1")); }

    @Test public void vazio() { assertNull(f.normalizarCodigo("")); }
    @Test public void nulo() { assertNull(f.normalizarCodigo(null)); }
    @Test public void prefixoErrado() { assertNull(f.normalizarCodigo("XX12-034")); }
    @Test public void numeroZero() { assertNull(f.normalizarCodigo("OP12-000")); }
    @Test public void colecaoZero() { assertNull(f.normalizarCodigo("OP00-001")); }
    @Test public void lixoNoFim() { assertNull(f.normalizarCodigo("OP12-034x")); }
    @Test public void numeroGrandeDemais() { assertNull(f.normalizarCodigo("OP12-0345")); }
}
