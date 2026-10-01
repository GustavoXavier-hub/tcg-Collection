package com.gustavo.tcgcollection.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.gustavo.tcgcollection.util.MugiwaraPersona.Contexto;

import org.junit.Test;

import java.util.Random;

public class MugiwaraPersonaTest {

    @Test
    public void todoContextoTemFrases() {
        for (Contexto c : Contexto.values()) {
            assertTrue(c + " sem frases", MugiwaraPersona.quantasFrases(c) > 0);
            Random r = new Random(42);
            for (int i = 0; i < 50; i++) {
                String f = MugiwaraPersona.frase(c, r);
                assertNotNull(f);
                assertFalse(f.trim().isEmpty());
            }
        }
    }

    @Test
    public void easterEggPeloNome() {
        assertTrue(MugiwaraPersona.easterEgg("Usopp", 1).contains("CAPITÃO USOPP"));
        assertTrue(MugiwaraPersona.easterEgg("MONKEY.D.LUFFY", 1).contains("capitão"));
        assertTrue(MugiwaraPersona.easterEgg("Kaido", 1).contains("doença"));
        assertTrue(MugiwaraPersona.easterEgg("Going Merry", 1).contains("Merry"));
    }

    @Test
    public void sogekingEYasoppVencemUsopp() {
        // "Yasopp" não contém "usopp", mas "Sogeking (Usopp)" sim: o mais específico vem antes.
        assertTrue(MugiwaraPersona.easterEgg("Sogeking", 1).startsWith("Sogeking"));
        assertTrue(MugiwaraPersona.easterEgg("Yasopp", 1).startsWith("Meu pai"));
    }

    @Test
    public void quatroCopias() {
        assertTrue(MugiwaraPersona.easterEgg("Nami", 4).contains("Quatro"));
        assertNull(MugiwaraPersona.easterEgg("Nami", 3));
    }

    @Test
    public void semMatchOuNomeNulo() {
        assertNull(MugiwaraPersona.easterEgg("Trafalgar Law", 1));
        assertNull(MugiwaraPersona.easterEgg(null, 1));
        assertEquals(MugiwaraPersona.easterEgg("usopp", 1), MugiwaraPersona.easterEgg("USOPP", 1));
    }
}
