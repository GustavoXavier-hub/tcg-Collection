package com.gustavo.tcgcollection.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gustavo.tcgcollection.model.Carta;
import com.gustavo.tcgcollection.model.ItemColecao;

import java.util.ArrayList;
import java.util.List;

/** Tudo que lê/grava a coleção passa por aqui. Chamar fora da thread principal. */
public class ColecaoRepo {

    private final BancoDados banco;

    public ColecaoRepo(Context ctx) {
        this.banco = BancoDados.get(ctx);
    }

    /** Resumo para o topo da tela. */
    public static class Resumo {
        public int totalCartas;
        public int linhas;
        public double totalPago;
    }

    /**
     * Adiciona à coleção. Se já existe a mesma versão + idioma + condição,
     * soma a quantidade (e faz média ponderada do preço pago).
     */
    public void adicionar(Carta carta, int quantidade, String idioma, String condicao, Double precoPagoUnit) {
        SQLiteDatabase db = banco.getWritableDatabase();
        db.beginTransaction();
        try {
            salvarCarta(db, carta);

            Cursor c = db.rawQuery(
                    "SELECT id, quantidade, preco_pago_unit FROM colecao "
                            + "WHERE jogo=? AND versao=? AND idioma=? AND condicao=?",
                    new String[]{carta.jogo, carta.versao, idioma, condicao});
            try {
                if (c.moveToFirst()) {
                    long id = c.getLong(0);
                    int qtdAntiga = c.getInt(1);
                    Double precoAntigo = c.isNull(2) ? null : c.getDouble(2);
                    int qtdNova = qtdAntiga + quantidade;

                    ContentValues v = new ContentValues();
                    v.put("quantidade", qtdNova);
                    Double preco = mediaPonderada(precoAntigo, qtdAntiga, precoPagoUnit, quantidade);
                    if (preco != null) v.put("preco_pago_unit", preco);
                    db.update("colecao", v, "id=?", new String[]{String.valueOf(id)});
                } else {
                    ContentValues v = new ContentValues();
                    v.put("jogo", carta.jogo);
                    v.put("versao", carta.versao);
                    v.put("quantidade", quantidade);
                    v.put("idioma", idioma);
                    v.put("condicao", condicao);
                    if (precoPagoUnit != null) v.put("preco_pago_unit", precoPagoUnit);
                    v.put("criado_em", System.currentTimeMillis());
                    db.insertOrThrow("colecao", null, v);
                }
            } finally {
                c.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Média ponderada; se só um lado tem preço, usa ele. Pública para testes. */
    public static Double mediaPonderada(Double p1, int q1, Double p2, int q2) {
        if (p1 == null) return p2;
        if (p2 == null) return p1;
        int total = q1 + q2;
        if (total <= 0) return p2;
        return (p1 * q1 + p2 * q2) / total;
    }

    private void salvarCarta(SQLiteDatabase db, Carta c) {
        ContentValues v = new ContentValues();
        v.put("jogo", c.jogo);
        v.put("versao", c.versao);
        v.put("codigo", c.codigo);
        v.put("nome", c.nome);
        v.put("colecao", c.colecao);
        v.put("raridade", c.raridade);
        v.put("tipo", c.tipo);
        v.put("cor", c.cor);
        v.put("imagem_url", c.imagemUrl);
        if (Double.isNaN(c.precoMercadoUsd)) v.putNull("preco_mercado_usd");
        else v.put("preco_mercado_usd", c.precoMercadoUsd);
        v.put("extras", c.extrasJson != null ? c.extrasJson : "{}");
        v.put("atualizado_em", System.currentTimeMillis());
        // UPSERT de verdade: REPLACE apagaria a linha e quebraria a FK da coleção.
        int n = db.update("cartas", v, "jogo=? AND versao=?", new String[]{c.jogo, c.versao});
        if (n == 0) db.insertOrThrow("cartas", null, v);
    }

    public List<ItemColecao> listar() {
        List<ItemColecao> out = new ArrayList<>();
        SQLiteDatabase db = banco.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT co.id, co.quantidade, co.idioma, co.condicao, co.preco_pago_unit, co.criado_em,"
                        + " ca.jogo, ca.versao, ca.codigo, ca.nome, ca.colecao, ca.raridade, ca.tipo,"
                        + " ca.cor, ca.imagem_url, ca.preco_mercado_usd, ca.extras"
                        + " FROM colecao co JOIN cartas ca ON ca.jogo = co.jogo AND ca.versao = co.versao"
                        + " ORDER BY co.criado_em DESC", null);
        try {
            while (c.moveToNext()) {
                ItemColecao i = new ItemColecao();
                i.id = c.getLong(0);
                i.quantidade = c.getInt(1);
                i.idioma = c.getString(2);
                i.condicao = c.getString(3);
                i.precoPagoUnit = c.isNull(4) ? null : c.getDouble(4);
                i.criadoEm = c.getLong(5);

                Carta ca = new Carta();
                ca.jogo = c.getString(6);
                ca.versao = c.getString(7);
                ca.codigo = c.getString(8);
                ca.nome = c.getString(9);
                ca.colecao = c.getString(10);
                ca.raridade = c.getString(11);
                ca.tipo = c.getString(12);
                ca.cor = c.getString(13);
                ca.imagemUrl = c.getString(14);
                ca.precoMercadoUsd = c.isNull(15) ? Double.NaN : c.getDouble(15);
                ca.extrasJson = c.getString(16);
                i.carta = ca;
                out.add(i);
            }
        } finally {
            c.close();
        }
        return out;
    }

    public Resumo resumo() {
        Resumo r = new Resumo();
        Cursor c = banco.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(quantidade),0), COUNT(*),"
                        + " COALESCE(SUM(quantidade * preco_pago_unit),0) FROM colecao", null);
        try {
            if (c.moveToFirst()) {
                r.totalCartas = c.getInt(0);
                r.linhas = c.getInt(1);
                r.totalPago = c.getDouble(2);
            }
        } finally {
            c.close();
        }
        return r;
    }

    /** Soma (ou subtrai) da quantidade. Nunca deixa chegar a zero: para isso, use remover(). */
    public void alterarQuantidade(long id, int delta) {
        banco.getWritableDatabase().execSQL(
                "UPDATE colecao SET quantidade = MAX(1, quantidade + ?) WHERE id = ?",
                new Object[]{delta, id});
    }

    public void remover(long id) {
        banco.getWritableDatabase().delete("colecao", "id=?", new String[]{String.valueOf(id)});
    }
}
