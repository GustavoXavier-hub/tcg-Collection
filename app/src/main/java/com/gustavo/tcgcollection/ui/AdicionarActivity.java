package com.gustavo.tcgcollection.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gustavo.tcgcollection.R;
import com.gustavo.tcgcollection.data.ColecaoRepo;
import com.gustavo.tcgcollection.fonte.FonteCartas;
import com.gustavo.tcgcollection.fonte.Fontes;
import com.gustavo.tcgcollection.model.Carta;
import com.gustavo.tcgcollection.net.Rede;
import com.gustavo.tcgcollection.util.Moeda;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Digita o código → busca na API → escolhe versão/quantidade/idioma/condição → salva. */
public class AdicionarActivity extends AppCompatActivity {

    private static final String TAG = "TcgAdicionar";

    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private FonteCartas fonte;
    private List<Carta> versoes = new ArrayList<>();
    private Carta selecionada;
    private int quantidade = 1;

    private Spinner spJogo, spVersao, spIdioma, spCondicao;
    private EditText edtCodigo, edtPreco;
    private Button btnBuscar, btnSalvar;
    private TextView txtStatus, txtNome, txtDetalhes, txtQtd, lblVersao;
    private ImageView imgCarta;
    private View painel;

    @Override
    protected void onCreate(Bundle salvo) {
        super.onCreate(salvo);
        setContentView(R.layout.activity_adicionar);

        spJogo = findViewById(R.id.spJogo);
        spVersao = findViewById(R.id.spVersao);
        spIdioma = findViewById(R.id.spIdioma);
        spCondicao = findViewById(R.id.spCondicao);
        edtCodigo = findViewById(R.id.edtCodigo);
        edtPreco = findViewById(R.id.edtPreco);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnSalvar = findViewById(R.id.btnSalvar);
        txtStatus = findViewById(R.id.txtStatus);
        txtNome = findViewById(R.id.txtNomeCarta);
        txtDetalhes = findViewById(R.id.txtDetalhes);
        txtQtd = findViewById(R.id.txtQtd);
        lblVersao = findViewById(R.id.lblVersao);
        imgCarta = findViewById(R.id.imgCartaGrande);
        painel = findViewById(R.id.painelCarta);

        configurarJogos();
        spIdioma.setAdapter(adapterDe(R.array.idiomas_rotulos));
        spCondicao.setAdapter(adapterDe(R.array.condicoes_rotulos));

        btnBuscar.setOnClickListener(v -> buscar());
        edtCodigo.setOnEditorActionListener((v, acao, ev) -> {
            boolean enter = ev != null && ev.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && ev.getAction() == KeyEvent.ACTION_DOWN;
            if (acao == EditorInfo.IME_ACTION_SEARCH || enter) {
                buscar();
                return true;
            }
            return false;
        });

        findViewById(R.id.btnMenos).setOnClickListener(v -> mudarQtd(-1));
        findViewById(R.id.btnMais).setOnClickListener(v -> mudarQtd(+1));
        btnSalvar.setOnClickListener(v -> salvar());

        spVersao.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (pos >= 0 && pos < versoes.size()) mostrarCarta(versoes.get(pos));
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bg.shutdown();
    }

    private void configurarJogos() {
        List<FonteCartas> todas = Fontes.todas();
        fonte = todas.get(0);
        List<String> nomes = new ArrayList<>();
        for (FonteCartas f : todas) nomes.add(f.nomeJogo());
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nomes);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spJogo.setAdapter(a);
        // Com um jogo só, o seletor nem aparece.
        View blocoJogo = findViewById(R.id.blocoJogo);
        blocoJogo.setVisibility(todas.size() > 1 ? View.VISIBLE : View.GONE);
        spJogo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                fonte = todas.get(pos);
                edtCodigo.setHint(fonte.exemploCodigo());
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        edtCodigo.setHint(fonte.exemploCodigo());
    }

    private ArrayAdapter<CharSequence> adapterDe(int arrayRes) {
        ArrayAdapter<CharSequence> a = ArrayAdapter.createFromResource(
                this, arrayRes, android.R.layout.simple_spinner_item);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private void buscar() {
        String codigo = fonte.normalizarCodigo(edtCodigo.getText().toString());
        if (codigo == null) {
            status("Código inválido. Use o formato impresso na carta, ex.: " + fonte.exemploCodigo());
            return;
        }
        edtCodigo.setText(codigo);
        edtCodigo.setSelection(codigo.length());
        esconderTeclado();
        painel.setVisibility(View.GONE);
        btnBuscar.setEnabled(false);
        status("Buscando " + codigo + "…");

        final FonteCartas f = fonte;
        bg.execute(() -> {
            try {
                List<Carta> achadas = f.buscarPorCodigo(codigo);
                main.post(() -> resultado(codigo, achadas));
            } catch (IOException e) {
                Log.w(TAG, "falha na busca", e);
                main.post(() -> falha(e));
            }
        });
    }

    private void resultado(String codigo, List<Carta> achadas) {
        btnBuscar.setEnabled(true);
        if (achadas.isEmpty()) {
            status("Não encontrei " + codigo + " em " + fonte.nomeJogo()
                    + ". Confira o código no canto da carta.");
            return;
        }
        versoes = achadas;
        quantidade = 1;
        txtQtd.setText("1");
        edtPreco.setText("");

        List<String> rotulos = new ArrayList<>();
        for (Carta c : achadas) rotulos.add(c.rotuloVersao());
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rotulos);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spVersao.setAdapter(a);
        boolean variasVersoes = achadas.size() > 1;
        spVersao.setVisibility(variasVersoes ? View.VISIBLE : View.GONE);
        lblVersao.setVisibility(variasVersoes ? View.VISIBLE : View.GONE);

        mostrarCarta(achadas.get(0));
        painel.setVisibility(View.VISIBLE);
        status(variasVersoes
                ? achadas.size() + " versões encontradas — escolha a sua."
                : "Encontrada.");
    }

    private void falha(IOException e) {
        btnBuscar.setEnabled(true);
        if (e instanceof Rede.HttpErro) {
            status("A API respondeu com erro " + ((Rede.HttpErro) e).status + ". Tente de novo mais tarde.");
        } else {
            status("Sem conexão ou API fora do ar.\n" + descrever(e));
        }
    }

    private void mostrarCarta(Carta c) {
        selecionada = c;
        txtNome.setText(c.nome);
        String preco = Double.isNaN(c.precoMercadoUsd) ? null : "mercado " + Moeda.dolares(c.precoMercadoUsd);
        txtDetalhes.setText(ColecaoAdapter.juntar(c.codigo, c.raridade, c.cor, c.tipo)
                + (c.colecao != null ? "\n" + c.colecao : "")
                + (preco != null ? "\n" + preco : ""));
        ImagemLoader.get(this).carregar(c.imagemUrl, imgCarta);
    }

    private void mudarQtd(int delta) {
        quantidade = Math.max(1, Math.min(99, quantidade + delta));
        txtQtd.setText(String.valueOf(quantidade));
    }

    private void salvar() {
        if (selecionada == null) return;
        String precoTxt = edtPreco.getText().toString();
        Double preco = Moeda.lerReais(precoTxt);
        if (!precoTxt.trim().isEmpty() && preco == null) {
            edtPreco.setError("Valor inválido (ex.: 12,50)");
            return;
        }
        String idioma = getResources().getStringArray(R.array.idiomas_codigos)[spIdioma.getSelectedItemPosition()];
        String condicao = getResources().getStringArray(R.array.condicoes_codigos)[spCondicao.getSelectedItemPosition()];
        final Carta carta = selecionada;
        final int qtd = quantidade;
        btnSalvar.setEnabled(false);

        bg.execute(() -> {
            try {
                new ColecaoRepo(this).adicionar(carta, qtd, idioma, condicao, preco);
                main.post(() -> {
                    Toast.makeText(this, carta.nome + " ×" + qtd + " adicionada", Toast.LENGTH_SHORT).show();
                    prepararProxima();
                });
            } catch (Exception e) {
                Log.e(TAG, "erro ao salvar", e);
                main.post(() -> {
                    btnSalvar.setEnabled(true);
                    status("Erro ao salvar: " + descrever(e));
                });
            }
        });
    }

    /** Depois de salvar, já deixa pronto pra próxima carta (cadastro em sequência). */
    private void prepararProxima() {
        btnSalvar.setEnabled(true);
        painel.setVisibility(View.GONE);
        selecionada = null;
        versoes = new ArrayList<>();
        edtCodigo.setText("");
        edtCodigo.requestFocus();
        status("Salva! Digite o próximo código ou volte para ver a coleção.");
    }

    private void status(String msg) {
        txtStatus.setText(msg);
        txtStatus.setVisibility(View.VISIBLE);
    }

    private void esconderTeclado() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(edtCodigo.getWindowToken(), 0);
    }

    /** Desembrulha causas aninhadas para a mensagem ficar útil. */
    static String descrever(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable atual = t;
        int voltas = 0;
        while (atual != null && voltas++ < 4) {
            if (sb.length() > 0) sb.append(" ← ");
            sb.append(atual.getClass().getSimpleName());
            if (atual.getMessage() != null) sb.append(": ").append(atual.getMessage());
            atual = atual.getCause();
        }
        return sb.toString();
    }
}
