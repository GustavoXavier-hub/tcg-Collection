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

    /** Um set no menu lateral. nome == null = cartas sem set informado pela API. */
    public static class Set {
        public String nome;
        /** null se nenhuma carta do set tem set_id (salvas antes da v2 do banco). */
        public String setId;
        public int totalCartas;
    }

    /** Checklist baixado de um set. */
    public static class Checklist {
        public int total;
        public long baixadoEm;
    }

    /**
     * Filtro da lista: null = todas as cartas; {@link #SEM_SET} = só as sem set;
     * qualquer outro valor = nome exato do set.
     */
    public static final String SEM_SET = "__sem_set__";

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
        if (c.setId != null) v.put("set_id", c.setId);
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

    /** Sets que têm carta na coleção, em ordem alfabética; "sem set" por último. */
    public List<Set> sets() {
        List<Set> out = new ArrayList<>();
        Cursor c = banco.getReadableDatabase().rawQuery(
                "SELECT ca.colecao, SUM(co.quantidade), MAX(ca.set_id)"
                        + " FROM colecao co JOIN cartas ca ON ca.jogo = co.jogo AND ca.versao = co.versao"
                        + " GROUP BY ca.colecao"
                        + " ORDER BY ca.colecao IS NULL, ca.colecao COLLATE NOCASE", null);
        try {
            while (c.moveToNext()) {
                Set s = new Set();
                s.nome = c.isNull(0) ? null : c.getString(0);
                s.totalCartas = c.getInt(1);
                s.setId = c.isNull(2) ? null : c.getString(2);
                out.add(s);
            }
        } finally {
            c.close();
        }
        return out;
    }

    /** Monta o WHERE do filtro de set. Os args são acrescentados em {@code args}. */
    private static String filtroSet(String set, List<String> args) {
        if (set == null) return "";
        if (SEM_SET.equals(set)) return " WHERE ca.colecao IS NULL";
        args.add(set);
        return " WHERE ca.colecao = ?";
    }

    public List<ItemColecao> listar() {
        return listar(null);
    }

    public List<ItemColecao> listar(String set) {
        List<ItemColecao> out = new ArrayList<>();
        SQLiteDatabase db = banco.getReadableDatabase();
        List<String> args = new ArrayList<>();
        String where = filtroSet(set, args);
        Cursor c = db.rawQuery(
                "SELECT co.id, co.quantidade, co.idioma, co.condicao, co.preco_pago_unit, co.criado_em,"
                        + " ca.jogo, ca.versao, ca.codigo, ca.nome, ca.colecao, ca.raridade, ca.tipo,"
                        + " ca.cor, ca.imagem_url, ca.preco_mercado_usd, ca.extras, ca.set_id"
                        + " FROM colecao co JOIN cartas ca ON ca.jogo = co.jogo AND ca.versao = co.versao"
                        + where
                        + " ORDER BY co.criado_em DESC", args.toArray(new String[0]));
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
                ca.setId = c.isNull(17) ? null : c.getString(17);
                i.carta = ca;
                out.add(i);
            }
        } finally {
            c.close();
        }
        return out;
    }

    public Resumo resumo() {
        return resumo(null);
    }

    public Resumo resumo(String set) {
        Resumo r = new Resumo();
        List<String> args = new ArrayList<>();
        String where = filtroSet(set, args);
        Cursor c = banco.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(co.quantidade),0), COUNT(*),"
                        + " COALESCE(SUM(co.quantidade * co.preco_pago_unit),0)"
                        + " FROM colecao co JOIN cartas ca ON ca.jogo = co.jogo AND ca.versao = co.versao"
                        + where, args.toArray(new String[0]));
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

    /** null se o checklist desse set nunca foi baixado. */
    public Checklist checklist(String jogo, String setId) {
        Cursor c = banco.getReadableDatabase().rawQuery(
                "SELECT total, baixado_em FROM sets_baixados WHERE jogo=? AND set_id=?",
                new String[]{jogo, setId});
        try {
            if (!c.moveToFirst()) return null;
            Checklist k = new Checklist();
            k.total = c.getInt(0);
            k.baixadoEm = c.getLong(1);
            return k;
        } finally {
            c.close();
        }
    }

    /** Troca o checklist guardado do set pelo que acabou de vir da fonte. */
    public void salvarChecklist(String jogo, String setId, List<Carta> cartas) {
        SQLiteDatabase db = banco.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("set_checklist", "jogo=? AND set_id=?", new String[]{jogo, setId});
            for (Carta carta : cartas) {
                salvarCarta(db, carta);
                ContentValues v = new ContentValues();
                v.put("jogo", jogo);
                v.put("set_id", setId);
                v.put("codigo", carta.codigo);
                v.put("versao", carta.versao);
                db.insertWithOnConflict("set_checklist", null, v, SQLiteDatabase.CONFLICT_IGNORE);
            }
            ContentValues v = new ContentValues();
            v.put("jogo", jogo);
            v.put("set_id", setId);
            v.put("total", cartas.size());
            v.put("baixado_em", System.currentTimeMillis());
            db.insertWithOnConflict("sets_baixados", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Cartas do checklist que você não tem em NENHUMA versão
     * (ter a arte alternativa já conta como ter a carta).
     */
    public List<Carta> faltantes(String jogo, String setId) {
        List<Carta> out = new ArrayList<>();
        Cursor c = banco.getReadableDatabase().rawQuery(
                "SELECT ca.codigo, ca.versao, ca.nome, ca.colecao, ca.raridade, ca.tipo, ca.cor,"
                        + " ca.imagem_url, ca.preco_mercado_usd"
                        + " FROM set_checklist sc"
                        + " JOIN cartas ca ON ca.jogo = sc.jogo AND ca.versao = sc.versao"
                        + " WHERE sc.jogo = ? AND sc.set_id = ?"
                        + " AND sc.codigo NOT IN ("
                        + "   SELECT ca2.codigo FROM colecao co"
                        + "   JOIN cartas ca2 ON ca2.jogo = co.jogo AND ca2.versao = co.versao"
                        + "   WHERE co.jogo = ?)"
                        + " ORDER BY sc.codigo",
                new String[]{jogo, setId, jogo});
        try {
            while (c.moveToNext()) {
                Carta ca = new Carta();
                ca.jogo = jogo;
                ca.setId = setId;
                ca.codigo = c.getString(0);
                ca.versao = c.getString(1);
                ca.nome = c.getString(2);
                ca.colecao = c.getString(3);
                ca.raridade = c.getString(4);
                ca.tipo = c.getString(5);
                ca.cor = c.getString(6);
                ca.imagemUrl = c.getString(7);
                ca.precoMercadoUsd = c.isNull(8) ? Double.NaN : c.getDouble(8);
                out.add(ca);
            }
        } finally {
            c.close();
        }
        return out;
    }

    /** Soma (ou subtrai) da quantidade. Nunca deixa chegar a zero: para isso, use remover(). */
    public void alterarQuantidade(long id, int delta) {
        banco.getWritableDatabase().execSQL(
                "UPDATE colecao SET quantidade = MAX(1, quantidade + ?) WHERE id = ?",
                new Object[]{delta, id});
    }

    /**
     * Troca a versão (tipo de arte) de uma linha da coleção. Se já existir uma linha
     * com a versão nova + mesmo idioma + mesma condição, junta as duas
     * (soma quantidade, média ponderada do preço) em vez de violar o índice único.
     */
    public void trocarVersao(long id, Carta nova) {
        SQLiteDatabase db = banco.getWritableDatabase();
        db.beginTransaction();
        try {
            salvarCarta(db, nova);
            Cursor atual = db.rawQuery(
                    "SELECT idioma, condicao, quantidade, preco_pago_unit FROM colecao WHERE id=?",
                    new String[]{String.valueOf(id)});
            String idioma, condicao;
            int qtd;
            Double preco;
            try {
                if (!atual.moveToFirst()) return;
                idioma = atual.getString(0);
                condicao = atual.getString(1);
                qtd = atual.getInt(2);
                preco = atual.isNull(3) ? null : atual.getDouble(3);
            } finally {
                atual.close();
            }

            Cursor destino = db.rawQuery(
                    "SELECT id, quantidade, preco_pago_unit FROM colecao"
                            + " WHERE jogo=? AND versao=? AND idioma=? AND condicao=? AND id<>?",
                    new String[]{nova.jogo, nova.versao, idioma, condicao, String.valueOf(id)});
            try {
                if (destino.moveToFirst()) {
                    long idDestino = destino.getLong(0);
                    int qtdDestino = destino.getInt(1);
                    Double precoDestino = destino.isNull(2) ? null : destino.getDouble(2);
                    ContentValues v = new ContentValues();
                    v.put("quantidade", qtdDestino + qtd);
                    Double media = mediaPonderada(precoDestino, qtdDestino, preco, qtd);
                    if (media != null) v.put("preco_pago_unit", media);
                    db.update("colecao", v, "id=?", new String[]{String.valueOf(idDestino)});
                    db.delete("colecao", "id=?", new String[]{String.valueOf(id)});
                } else {
                    ContentValues v = new ContentValues();
                    v.put("versao", nova.versao);
                    db.update("colecao", v, "id=?", new String[]{String.valueOf(id)});
                }
            } finally {
                destino.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void remover(long id) {
        banco.getWritableDatabase().delete("colecao", "id=?", new String[]{String.valueOf(id)});
    }
}
