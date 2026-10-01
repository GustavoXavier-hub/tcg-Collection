package com.gustavo.tcgcollection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.gustavo.tcgcollection.fonte.OnePieceFonte;

import org.junit.Test;

/** Texto como o ML Kit devolve da carta inteira: várias linhas, com erros de leitura. */
public class CodigoNoTextoTest {

    private final OnePieceFonte f = new OnePieceFonte();

    @Test
    public void codigoLimpoNoMeioDoTexto() {
        String ocr = "Monkey.D.Luffy\n12000\nStraw Hat Crew/The Four Emperors\nSEC\nOP05-119\n10";
        assertEquals("OP05-119", f.acharCodigoNoTexto(ocr));
    }

    @Test
    public void confusoesDoOcr() {
        assertEquals("OP05-119", f.acharCodigoNoTexto("0P05-119"));
        assertEquals("OP05-119", f.acharCodigoNoTexto("OPO5-1I9"));
        assertEquals("OP12-034", f.acharCodigoNoTexto("op12 - O34"));
        assertEquals("ST10-001", f.acharCodigoNoTexto("5T10-00l"));
        assertEquals("EB02-010", f.acharCodigoNoTexto("E802-010"));
        assertEquals("PRB01-001", f.acharCodigoNoTexto("PR801-001"));
        assertEquals("OP05-119", f.acharCodigoNoTexto("OP05\u2014119")); // travessão
    }

    @Test
    public void naoPegaOutrosNumerosDaCarta() {
        assertNull(f.acharCodigoNoTexto("Monkey.D.Luffy\n12000\n10\n2000"));
        assertNull(f.acharCodigoNoTexto("OP05119"));          // sem hífen: arriscado demais
        assertNull(f.acharCodigoNoTexto("XOP05-119"));        // colado em outra palavra
        assertNull(f.acharCodigoNoTexto("OP05-1190"));        // número longo demais
        assertNull(f.acharCodigoNoTexto(null));
    }

    @Test
    public void primeiroCodigoValidoGanha() {
        // OP00 é inválido (set zero); segue para o próximo.
        assertEquals("OP01-001", f.acharCodigoNoTexto("OP00-001 OP01-001"));
    }
}
