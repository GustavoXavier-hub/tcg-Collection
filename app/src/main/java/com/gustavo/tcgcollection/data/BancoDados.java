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
    private static final int VERSAO = 2;

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
                + "set_id TEXT,"
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

        criarSetsBaixados(db);
    }

    /**
     * Quando o checklist de cada set foi baixado. As cartas do checklist vão
     * para a tabela cartas (é um cache); aqui só fica a data, para não
     * baixar de novo toda vez que abrir as faltantes.
     */
    private static void criarSetsBaixados(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sets_baixados ("
                + "jogo TEXT NOT NULL,"
                + "set_id TEXT NOT NULL,"
                + "total INTEGER NOT NULL,"
                + "baixado_em INTEGER NOT NULL,"
                + "PRIMARY KEY (jogo, set_id))");
        // Quais versões (arte normal) compõem cada set. Dados da carta ficam em cartas.
        db.execSQL("CREATE TABLE set_checklist ("
                + "jogo TEXT NOT NULL,"
                + "set_id TEXT NOT NULL,"
                + "codigo TEXT NOT NULL,"
                + "versao TEXT NOT NULL,"
                + "PRIMARY KEY (jogo, set_id, codigo),"
                + "FOREIGN KEY (jogo, versao) REFERENCES cartas(jogo, versao))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int antiga, int nova) {
        // Nunca editar um passo já publicado: sempre acrescentar o próximo.
        if (antiga < 2) {
            // v2: set_id para as cartas faltantes do set. Cartas antigas ficam
            // com null até serem salvas de novo (o menu lateral usa o nome do set).
            db.execSQL("ALTER TABLE cartas ADD COLUMN set_id TEXT");
            criarSetsBaixados(db);
        }
    }
}
