package com.gustavo.tcgcollection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.gustavo.tcgcollection.data.ColecaoRepo;
import com.gustavo.tcgcollection.util.Moeda;

import org.junit.Test;

public class MoedaTest {

    @Test public void virgula() { assertEquals(12.5, Moeda.lerReais("12,50"), 1e-9); }
    @Test public void comSimboloEMilhar() { assertEquals(1153.95, Moeda.lerReais("R$ 1.153,95"), 1e-9); }
    @Test public void ponto() { assertEquals(12.5, Moeda.lerReais("12.5"), 1e-9); }
    @Test public void inteiro() { assertEquals(7.0, Moeda.lerReais("7"), 1e-9); }
    @Test public void vazio() { assertNull(Moeda.lerReais("  ")); }
    @Test public void lixo() { assertNull(Moeda.lerReais("abc")); }

    @Test public void mediaPonderada() {
        // 2 cartas a R$ 10 + 1 a R$ 16 = R$ 12 em média
        assertEquals(12.0, ColecaoRepo.mediaPonderada(10.0, 2, 16.0, 1), 1e-9);
        assertEquals(16.0, ColecaoRepo.mediaPonderada(null, 2, 16.0, 1), 1e-9);
        assertEquals(10.0, ColecaoRepo.mediaPonderada(10.0, 2, null, 1), 1e-9);
    }
}
