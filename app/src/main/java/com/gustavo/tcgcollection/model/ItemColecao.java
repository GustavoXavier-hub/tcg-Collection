package com.gustavo.tcgcollection.model;

/** Uma linha da coleção: qual carta, quantas, em que idioma e estado. */
public class ItemColecao {
    public long id;
    public Carta carta;
    public int quantidade;
    /** EN, JP, PT, OUTRO */
    public String idioma;
    /** NM, LP, MP, HP, DMG */
    public String condicao;
    /** Preço pago por unidade em R$. null = não informado. */
    public Double precoPagoUnit;
    public long criadoEm;
}
