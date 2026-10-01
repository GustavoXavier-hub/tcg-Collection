package com.gustavo.tcgcollection.util;

import java.text.NumberFormat;
import java.util.Locale;

/** Dinheiro em formato brasileiro. Sem android.* (testável no PC). */
public final class Moeda {

    private static final Locale PT_BR = new Locale("pt", "BR");

    private Moeda() {}

    /**
     * Lê "12,50", "R$ 1.153,95", "12.5", "7".
     * Retorna null se vazio ou inválido.
     */
    public static Double lerReais(String entrada) {
        if (entrada == null) return null;
        String s = entrada.replace("R$", "").replace(" ", "").replace("\u00A0", "").trim();
        if (s.isEmpty()) return null;
        if (s.contains(",")) {
            // Formato BR: ponto é milhar, vírgula é decimal.
            s = s.replace(".", "").replace(',', '.');
        }
        try {
            double v = Double.parseDouble(s);
            if (v < 0 || Double.isNaN(v) || Double.isInfinite(v)) return null;
            return v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String reais(double valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    public static String dolares(double valor) {
        return String.format(Locale.ROOT, "US$ %.2f", valor);
    }
}
