package com.gustavo.tcgcollection.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Banco local. Duas tabelas:
 *  - cartas:  cache dos dados de cada carta (vem da API uma vez só).
 *             Os decks, no futuro, também vão apontar pra cá.
 *  - colecao: o que você TEM — quantidade, idioma, condição, preço pago.
 */
public class BancoDados extends SQLiteOpenHelper {

    private static final String NOME = "tcg.db";
    private static final int VERSAO = 1;

    private static BancoDados instancia;

    public static synchronized BancoDados get(Context ctx) {
        if (instancia == null) instancia = new BancoDados(ctx.getApplicationContext());
        return instancia;
    }

    private BancoDados(Context ctx) {
        super(ctx, NOME, null, VERSAO);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE cartas ("
                + "jogo TEXT NOT NULL,"
                + "versao TEXT NOT NULL,"
                + "codigo TEXT NOT NULL,"
                + "nome TEXT NOT NULL,"
                + "colecao TEXT,"
                + "raridade TEXT,"
                + "tipo TEXT,"
                + "cor TEXT,"
                + "imagem_url TEXT,"
                + "preco_mercado_usd REAL,"
                + "extras TEXT NOT NULL DEFAULT '{}',"
                + "atualizado_em INTEGER NOT NULL,"
                + "PRIMARY KEY (jogo, versao))");

        db.execSQL("CREATE TABLE colecao ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "jogo TEXT NOT NULL,"
                + "versao TEXT NOT NULL,"
                + "quantidade INTEGER NOT NULL CHECK (quantidade > 0),"
                + "idioma TEXT NOT NULL,"
                + "condicao TEXT NOT NULL,"
                + "preco_pago_unit REAL,"
                + "criado_em INTEGER NOT NULL,"
                + "FOREIGN KEY (jogo, versao) REFERENCES cartas(jogo, versao))");

        // Mesma carta + mesmo idioma + mesma condição = uma linha só (soma quantidade).
        db.execSQL("CREATE UNIQUE INDEX ux_colecao ON colecao(jogo, versao, idioma, condicao)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int antiga, int nova) {
        // Migrações entram aqui quando a VERSAO subir (ex.: tabela de decks).
    }
}
