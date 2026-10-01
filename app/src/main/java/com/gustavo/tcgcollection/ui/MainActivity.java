package com.gustavo.tcgcollection.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.gustavo.tcgcollection.R;
import com.gustavo.tcgcollection.data.ColecaoRepo;
import com.gustavo.tcgcollection.fonte.FonteCartas;
import com.gustavo.tcgcollection.fonte.Fontes;
import com.gustavo.tcgcollection.fonte.OnePieceFonte;
import com.gustavo.tcgcollection.model.Carta;
import com.gustavo.tcgcollection.model.ItemColecao;
import com.gustavo.tcgcollection.util.MugiwaraPersona;
import com.gustavo.tcgcollection.util.MugiwaraPersona.Contexto;
import com.gustavo.tcgcollection.util.Moeda;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TcgMain";
    private static final String ESTADO_FILTRO = "filtro";
    private static final int ID_TODAS = 1;
    private static final int ID_PRIMEIRO_SET = 100;

    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ColecaoRepo repo;
    private ColecaoAdapter adapter;
    private TextView txtResumo;
    private View vazio;
    private RecyclerView lista;
    private DrawerLayout drawer;
    private NavigationView nav;
    private TextView txtFiltro, btnFaltantes;

    /** null = todas; senão o nome do set (ou ColecaoRepo.SEM_SET). */
    private String filtro;
    private List<ColecaoRepo.Set> sets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle salvo) {
        super.onCreate(salvo);
        setContentView(R.layout.activity_main);

        repo = new ColecaoRepo(this);
        txtResumo = findViewById(R.id.txtResumo);
        vazio = findViewById(R.id.vazio);
        lista = findViewById(R.id.lista);
        drawer = findViewById(R.id.drawer);
        nav = findViewById(R.id.navSets);
        txtFiltro = findViewById(R.id.txtFiltro);
        btnFaltantes = findViewById(R.id.btnFaltantes);
        if (salvo != null) filtro = salvo.getString(ESTADO_FILTRO);

        findViewById(R.id.btnMenu).setOnClickListener(v -> drawer.openDrawer(GravityCompat.START));
        nav.setNavigationItemSelectedListener(this::escolheuNoMenu);

        // Voltar fecha o menu antes de sair do app.
        OnBackPressedCallback fecharMenu = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() {
                drawer.closeDrawer(GravityCompat.START);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, fecharMenu);
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override public void onDrawerOpened(View v) { fecharMenu.setEnabled(true); }
            @Override public void onDrawerClosed(View v) { fecharMenu.setEnabled(false); }
        });
        // Sorteada uma vez por abertura (não a cada onResume, pra não ficar trocando).
        ((TextView) findViewById(R.id.txtVazioTitulo))
                .setText(MugiwaraPersona.frase(MugiwaraPersona.Contexto.VAZIO));
        PersonaImagens.aplicar(findViewById(R.id.imgVazio), PersonaImagens.VAZIO, 0);
        PersonaImagens.aplicar(findViewById(R.id.imgFundo), PersonaImagens.FUNDO, 0);

        adapter = new ColecaoAdapter(ImagemLoader.get(this), this::abrirOpcoes);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);

        ExtendedFloatingActionButton fab = findViewById(R.id.fabAdicionar);
        fab.setOnClickListener(v -> startActivity(new Intent(this, AdicionarActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        recarregar();
    }

    @Override
    protected void onSaveInstanceState(Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putString(ESTADO_FILTRO, filtro);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bg.shutdown();
    }

    private void recarregar() {
        final String pedido = filtro;
        bg.execute(() -> {
            try {
                List<ColecaoRepo.Set> todosSets = repo.sets();
                // Removeu a última carta do set: volta para "todas".
                String f = existe(todosSets, pedido) ? pedido : null;
                List<ItemColecao> itens = repo.listar(f);
                ColecaoRepo.Resumo r = repo.resumo(f);
                ColecaoRepo.Set set = acharSet(todosSets, f);
                String faltam = textoFaltantes(set);
                main.post(() -> {
                    filtro = f;
                    sets = todosSets;
                    montarMenu();
                    mostrar(itens, r, set, faltam);
                });
            } catch (Exception e) {
                Log.e(TAG, "erro ao ler coleção", e);
            }
        });
    }

    /** Faltantes já conhecidas (checklist baixado), sem ir à rede. null se não der para saber. */
    private String textoFaltantes(ColecaoRepo.Set set) {
        if (set == null || set.setId == null) return null;
        ColecaoRepo.Checklist k = repo.checklist(OnePieceFonte.JOGO, set.setId);
        if (k == null) return getString(R.string.btn_faltantes);
        int faltam = repo.faltantes(OnePieceFonte.JOGO, set.setId).size();
        if (faltam == 0) return "Set completo! " + k.total + " de " + k.total + " →";
        return "Faltam " + faltam + " de " + k.total + " →";
    }

    private static boolean existe(List<ColecaoRepo.Set> todos, String f) {
        return f == null || acharSet(todos, f) != null;
    }

    private static ColecaoRepo.Set acharSet(List<ColecaoRepo.Set> todos, String f) {
        if (f == null) return null;
        for (ColecaoRepo.Set s : todos) {
            if (chave(s).equals(f)) return s;
        }
        return null;
    }

    private static String chave(ColecaoRepo.Set s) {
        return s.nome != null ? s.nome : ColecaoRepo.SEM_SET;
    }

    private void montarMenu() {
        Menu m = nav.getMenu();
        m.clear();
        int total = 0;
        for (ColecaoRepo.Set s : sets) total += s.totalCartas;

        MenuItem todas = m.add(Menu.NONE, ID_TODAS, Menu.NONE, getString(R.string.drawer_todas));
        todas.setActionView(contador(total));
        todas.setCheckable(true).setChecked(filtro == null);

        if (sets.isEmpty()) return;
        SubMenu secao = m.addSubMenu(getString(R.string.drawer_secao_sets));
        for (int i = 0; i < sets.size(); i++) {
            ColecaoRepo.Set s = sets.get(i);
            String nome = s.nome != null ? s.nome : getString(R.string.drawer_sem_set);
            MenuItem it = secao.add(Menu.NONE, ID_PRIMEIRO_SET + i, Menu.NONE, nome);
            it.setActionView(contador(s.totalCartas));
            it.setCheckable(true).setChecked(chave(s).equals(filtro));
        }
    }

    private TextView contador(int n) {
        TextView t = new TextView(this);
        t.setText(String.valueOf(n));
        t.setTypeface(Typeface.MONOSPACE);
        t.setTextColor(ContextCompat.getColor(this, R.color.ink_soft));
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private boolean escolheuNoMenu(MenuItem item) {
        int id = item.getItemId();
        if (id == ID_TODAS) {
            filtro = null;
        } else {
            int i = id - ID_PRIMEIRO_SET;
            if (i < 0 || i >= sets.size()) return false;
            filtro = chave(sets.get(i));
        }
        drawer.closeDrawer(GravityCompat.START);
        lista.scrollToPosition(0);
        recarregar();
        return true;
    }

    private void abrirFaltantes(ColecaoRepo.Set set) {
        Intent i = new Intent(this, FaltantesActivity.class);
        i.putExtra(FaltantesActivity.EXTRA_SET_ID, set.setId);
        i.putExtra(FaltantesActivity.EXTRA_SET_NOME, set.nome);
        startActivity(i);
    }

    private void mostrar(List<ItemColecao> itens, ColecaoRepo.Resumo r,
                         ColecaoRepo.Set set, String faltam) {
        adapter.definir(itens);
        boolean semNada = itens.isEmpty();
        vazio.setVisibility(semNada ? View.VISIBLE : View.GONE);
        lista.setVisibility(semNada ? View.GONE : View.VISIBLE);

        String resumo = r.totalCartas + (r.totalCartas == 1 ? " carta" : " cartas")
                + " · " + r.linhas + (r.linhas == 1 ? " registro" : " registros");
        if (r.totalPago > 0) resumo += " · " + Moeda.reais(r.totalPago) + " pagos";
        txtResumo.setText(resumo);

        if (set == null) {
            txtFiltro.setVisibility(View.GONE);
            btnFaltantes.setVisibility(View.GONE);
            return;
        }
        txtFiltro.setText(set.nome != null ? set.nome : getString(R.string.drawer_sem_set));
        txtFiltro.setVisibility(View.VISIBLE);
        // Sem set_id (carta salva antes da v2) não dá para pedir o checklist.
        if (faltam == null) {
            btnFaltantes.setVisibility(View.GONE);
        } else {
            btnFaltantes.setText(faltam);
            btnFaltantes.setVisibility(View.VISIBLE);
            btnFaltantes.setOnClickListener(v -> abrirFaltantes(set));
        }
    }

    /** Toque numa carta: +1, −1, trocar versão ou remover (remover sempre confirma). */
    private void abrirOpcoes(ItemColecao item) {
        String arte = item.carta.tipoArte();
        String titulo = item.carta.nomeBase() + (arte != null ? " · " + arte : "")
                + "  ×" + item.quantidade;
        String[] opcoes = {"Mais uma (+1)", "Menos uma (−1)",
                "Trocar versão / tipo de arte", "Remover da coleção"};
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setItems(opcoes, (d, qual) -> {
                    if (qual == 0) {
                        alterar(item, +1);
                    } else if (qual == 1) {
                        if (item.quantidade <= 1) confirmarRemocao(item);
                        else alterar(item, -1);
                    } else if (qual == 2) {
                        buscarVersoes(item);
                    } else {
                        confirmarRemocao(item);
                    }
                })
                .show();
    }

    /** Busca as versões da carta na fonte (uma chamada) e deixa escolher outra. */
    private void buscarVersoes(ItemColecao item) {
        FonteCartas f = Fontes.porJogo(item.carta.jogo);
        if (f == null) return;
        Snackbar.make(lista, "Mirando nas versões de " + item.carta.codigo + "…",
                Snackbar.LENGTH_SHORT).show();
        bg.execute(() -> {
            try {
                List<Carta> versoes = f.buscarPorCodigo(item.carta.codigo);
                main.post(() -> escolherVersao(item, versoes));
            } catch (Exception e) {
                Log.w(TAG, "falha ao buscar versões", e);
                main.post(() -> avisar(MugiwaraPersona.frase(Contexto.ERRO)));
            }
        });
    }

    private void escolherVersao(ItemColecao item, List<Carta> versoes) {
        if (isFinishing()) return;
        if (versoes.size() <= 1) {
            avisar("Essa carta só tem uma versão. Nem o Capitão Usopp inventa outra!");
            return;
        }
        List<String> rotulos = AdicionarActivity.rotulosDasVersoes(versoes);
        int atual = -1;
        for (int i = 0; i < versoes.size(); i++) {
            if (versoes.get(i).versao.equalsIgnoreCase(item.carta.versao)) atual = i;
        }
        final int marcada = atual;
        new AlertDialog.Builder(this)
                .setTitle("Qual versão você tem?")
                .setSingleChoiceItems(rotulos.toArray(new String[0]), marcada, (d, qual) -> {
                    d.dismiss();
                    if (qual == marcada) return;
                    Carta nova = versoes.get(qual);
                    bg.execute(() -> {
                        repo.trocarVersao(item.id, nova);
                        main.post(() -> {
                            recarregar();
                            avisar("Trocada para: " + rotulos.get(qual));
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void avisar(String msg) {
        Snackbar.make(lista, msg, Snackbar.LENGTH_LONG).setTextMaxLines(4).show();
    }

    private void alterar(ItemColecao item, int delta) {
        bg.execute(() -> {
            repo.alterarQuantidade(item.id, delta);
            main.post(this::recarregar);
        });
    }

    private void confirmarRemocao(ItemColecao item) {
        new AlertDialog.Builder(this)
                .setTitle("Remover da coleção?")
                .setMessage(item.carta.nome + " (" + item.carta.codigo + ", "
                        + item.idioma + ", " + item.condicao + ") ×" + item.quantidade)
                .setPositiveButton("Remover", (d, w) -> bg.execute(() -> {
                    repo.remover(item.id);
                    main.post(this::recarregar);
                }))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
