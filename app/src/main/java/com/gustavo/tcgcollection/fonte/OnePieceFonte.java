package com.gustavo.tcgcollection.fonte;

import com.gustavo.tcgcollection.model.Carta;
import com.gustavo.tcgcollection.net.Rede;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One Piece Card Game via OPTCG API (https://optcgapi.com).
 * Projeto da comunidade, dados da versão em inglês.
 * O dono paga o servidor do bolso: a coleção guarda os dados no banco
 * local e as imagens ficam em cache, então cada carta é buscada uma vez só.
 */
public class OnePieceFonte implements FonteCartas {

    public static final String JOGO = "onepiece";
    private static final String BASE = "https://optcgapi.com/api";

    /*
     * Aceita: OP12-034, op12034, OP12 34, op-12-34, ST10-001, EB02-010, PRB01-001.
     * SEM \b e SEM (?U): o Android usa ICU e quebra com eles.
     */
    static final Pattern CODIGO = Pattern.compile(
            "^\\s*(OP|ST|EB|PRB)\\s*-?\\s*(\\d{1,2})\\s*-?\\s*(\\d{1,3})\\s*$",
            Pattern.CASE_INSENSITIVE);

    /*
     * Código no meio do texto da câmera. Mais frouxo que CODIGO por causa do OCR:
     * aceita O/0, I/l/1, S/5, B/8, Z/2 trocados e travessão no lugar do hífen.
     * Exige o hífen antes do número (sempre impresso na carta) para não confundir
     * com outros números da carta (custo, poder). Sem \b e sem (?U).
     */
    static final Pattern CODIGO_OCR = Pattern.compile(
            "(?<![A-Z0-9])(0P|OP|5T|ST|E8|EB|PR8|PRB)\\s?-?\\s?([0-9OILSBZ]{2})\\s?[-\u2013\u2014_]\\s?([0-9OILSBZ]{3})(?![0-9])");

    // Campos do JSON da OPTCG API — se a API mudar, é só mexer aqui.
    static final String F_NOME = "card_name";
    static final String F_CODIGO = "card_set_id";
    static final String F_VERSAO = "card_image_id";
    static final String F_IMAGEM = "card_image";
    static final String F_COLECAO = "set_name";
    static final String F_SET_ID = "set_id";
    static final String F_RARIDADE = "rarity";
    static final String F_TIPO = "card_type";
    static final String F_COR = "card_color";
    static final String F_CUSTO = "card_cost";
    static final String F_PODER = "card_power";
    static final String F_COUNTER = "counter_amount";
    static final String F_ATRIBUTO = "attribute";
    static final String F_SUBTIPOS = "sub_types";
    static final String F_TEXTO = "card_text";
    static final String F_PRECO = "market_price";

    @Override public String jogo() { return JOGO; }
    @Override public String nomeJogo() { return "One Piece"; }
    @Override public String exemploCodigo() { return "OP12-034"; }

    @Override
    public String normalizarCodigo(String entrada) {
        if (entrada == null) return null;
        Matcher m = CODIGO.matcher(entrada);
        if (!m.matches()) return null;
        int set = Integer.parseInt(m.group(2));
        int num = Integer.parseInt(m.group(3));
        if (set == 0 || num == 0) return null;
        return String.format(Locale.ROOT, "%s%02d-%03d",
                m.group(1).toUpperCase(Locale.ROOT), set, num);
    }

    @Override
    public String acharCodigoNoTexto(String textoOcr) {
        if (textoOcr == null) return null;
        Matcher m = CODIGO_OCR.matcher(textoOcr.toUpperCase(Locale.ROOT));
        while (m.find()) {
            String prefixo = m.group(1).replace('0', 'O').replace('5', 'S').replace('8', 'B');
            String codigo = normalizarCodigo(prefixo + digitos(m.group(2)) + "-" + digitos(m.group(3)));
            if (codigo != null) return codigo;
        }
        return null;
    }

    /** Letras que o OCR costuma ler no lugar de dígitos. */
    static String digitos(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case 'O': sb.append('0'); break;
                case 'I': case 'L': sb.append('1'); break;
                case 'S': sb.append('5'); break;
                case 'B': sb.append('8'); break;
                case 'Z': sb.append('2'); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public List<Carta> buscarPorCodigo(String codigo) throws IOException {
        // Starter decks (ST) ficam num endpoint separado na OPTCG API.
        String rota = codigo.startsWith("ST") ? "/decks/card/" : "/sets/card/";
        String json;
        try {
            json = Rede.getTexto(BASE + rota + codigo + "/");
        } catch (Rede.HttpErro e) {
            if (e.status == 404) return new ArrayList<>();
            throw e;
        }
        try {
            return parse(json, codigo);
        } catch (JSONException e) {
            throw new IOException("Resposta inesperada da OPTCG API: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Carta> checklistDoSet(String setId) throws IOException {
        // Mesmo esquema da busca por carta: starter decks têm endpoint próprio.
        String rota = setId.startsWith("ST") ? "/decks/" : "/sets/";
        String json;
        try {
            json = Rede.getTexto(BASE + rota + setId + "/");
        } catch (Rede.HttpErro e) {
            if (e.status == 404) return new ArrayList<>();
            throw e;
        }
        try {
            return checklist(parse(json, ""), setId);
        } catch (JSONException e) {
            throw new IOException("Resposta inesperada da OPTCG API: " + e.getMessage(), e);
        }
    }

    /**
     * Do JSON inteiro do set, fica só o que "completa" o set: arte normal e código
     * do próprio set ("OP-05" → OP05-xxx). A OP-09, por exemplo, traz a Wanted Poster
     * da OP05-119, que conta para a OP-05, não para a OP-09. Público para os testes.
     */
    public static List<Carta> checklist(List<Carta> todas, String setId) {
        String prefixo = setId.replace("-", "").toUpperCase(Locale.ROOT) + "-";
        List<Carta> out = new ArrayList<>();
        for (Carta c : todas) {
            if (!c.ehVersaoPadrao()) continue;
            if (!c.codigo.startsWith(prefixo)) continue;
            boolean repetida = false;
            for (Carta o : out) if (o.codigo.equals(c.codigo)) { repetida = true; break; }
            if (!repetida) out.add(c);
        }
        out.sort((a, b) -> a.codigo.compareTo(b.codigo));
        return out;
    }

    /** Público para os testes. Aceita lista ou objeto único. */
    public static List<Carta> parse(String json, String codigoBuscado) throws JSONException {
        List<Carta> out = new ArrayList<>();
        if (json == null) return out;
        String t = json.trim();
        if (t.isEmpty()) return out;

        if (t.startsWith("[")) {
            JSONArray arr = new JSONArray(t);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                Carta c = o != null ? converter(o, codigoBuscado) : null;
                if (c != null && !jaTem(out, c.versao)) out.add(c);
            }
        } else if (t.startsWith("{")) {
            Carta c = converter(new JSONObject(t), codigoBuscado);
            if (c != null) out.add(c);
        }
        // Versão normal primeiro, artes alternativas depois.
        out.sort((a, b) -> {
            if (a.ehVersaoPadrao() != b.ehVersaoPadrao()) return a.ehVersaoPadrao() ? -1 : 1;
            return a.versao.compareTo(b.versao);
        });
        return out;
    }

    private static Carta converter(JSONObject o, String codigoBuscado) throws JSONException {
        String nome = texto(o, F_NOME);
        if (nome == null) return null; // ex.: {"error": "..."}

        Carta c = new Carta();
        c.jogo = JOGO;
        c.codigo = valorOu(texto(o, F_CODIGO), codigoBuscado).toUpperCase(Locale.ROOT);
        c.versao = valorOu(texto(o, F_VERSAO), c.codigo);
        c.nome = nome;
        c.colecao = texto(o, F_COLECAO);
        c.setId = texto(o, F_SET_ID);
        c.raridade = texto(o, F_RARIDADE);
        c.tipo = texto(o, F_TIPO);
        c.cor = texto(o, F_COR);
        c.imagemUrl = Rede.forcarHttps(texto(o, F_IMAGEM));
        c.precoMercadoUsd = numero(o, F_PRECO);

        JSONObject extras = new JSONObject();
        extras.put("custo", valorOu(texto(o, F_CUSTO), ""));
        extras.put("poder", valorOu(texto(o, F_PODER), ""));
        extras.put("counter", valorOu(texto(o, F_COUNTER), ""));
        extras.put("atributo", valorOu(texto(o, F_ATRIBUTO), ""));
        extras.put("subtipos", valorOu(texto(o, F_SUBTIPOS), ""));
        extras.put("texto", valorOu(texto(o, F_TEXTO), ""));
        c.extrasJson = extras.toString();
        return c;
    }

    private static boolean jaTem(List<Carta> lista, String versao) {
        for (Carta c : lista) if (c.versao.equalsIgnoreCase(versao)) return true;
        return false;
    }

    /** String limpa, ou null se ausente / "null" / "NULL" / vazia. */
    static String texto(JSONObject o, String campo) {
        if (!o.has(campo) || o.isNull(campo)) return null;
        String s = String.valueOf(o.opt(campo)).trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return null;
        return s;
    }

    /** Aceita número ou string ("1.25", "$1.25"). NaN se não der. */
    static double numero(JSONObject o, String campo) {
        String s = texto(o, campo);
        if (s == null) return Double.NaN;
        s = s.replace("$", "").replace(",", "").trim();
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static String valorOu(String v, String padrao) {
        return v != null ? v : padrao;
    }
}
