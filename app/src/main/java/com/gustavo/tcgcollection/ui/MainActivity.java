package com.gustavo.tcgcollection.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.gustavo.tcgcollection.R;
import com.gustavo.tcgcollection.data.ColecaoRepo;
import com.gustavo.tcgcollection.model.ItemColecao;
import com.gustavo.tcgcollection.util.Moeda;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TcgMain";

    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ColecaoRepo repo;
    private ColecaoAdapter adapter;
    private TextView txtResumo;
    private View vazio;
    private RecyclerView lista;

    @Override
    protected void onCreate(Bundle salvo) {
        super.onCreate(salvo);
        setContentView(R.layout.activity_main);

        repo = new ColecaoRepo(this);
        txtResumo = findViewById(R.id.txtResumo);
        vazio = findViewById(R.id.vazio);
        lista = findViewById(R.id.lista);

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
    protected void onDestroy() {
        super.onDestroy();
        bg.shutdown();
    }

    private void recarregar() {
        bg.execute(() -> {
            try {
                List<ItemColecao> itens = repo.listar();
                ColecaoRepo.Resumo r = repo.resumo();
                main.post(() -> mostrar(itens, r));
            } catch (Exception e) {
                Log.e(TAG, "erro ao ler coleção", e);
            }
        });
    }

    private void mostrar(List<ItemColecao> itens, ColecaoRepo.Resumo r) {
        adapter.definir(itens);
        boolean semNada = itens.isEmpty();
        vazio.setVisibility(semNada ? View.VISIBLE : View.GONE);
        lista.setVisibility(semNada ? View.GONE : View.VISIBLE);

        String resumo = r.totalCartas + (r.totalCartas == 1 ? " carta" : " cartas")
                + " · " + r.linhas + (r.linhas == 1 ? " registro" : " registros");
        if (r.totalPago > 0) resumo += " · " + Moeda.reais(r.totalPago) + " pagos";
        txtResumo.setText(resumo);
    }

    /** Toque numa carta: +1, −1 ou remover (remover sempre confirma). */
    private void abrirOpcoes(ItemColecao item) {
        String titulo = item.carta.nome + "  ×" + item.quantidade;
        String[] opcoes = {"Mais uma (+1)", "Menos uma (−1)", "Remover da coleção"};
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setItems(opcoes, (d, qual) -> {
                    if (qual == 0) {
                        alterar(item, +1);
                    } else if (qual == 1) {
                        if (item.quantidade <= 1) confirmarRemocao(item);
                        else alterar(item, -1);
                    } else {
                        confirmarRemocao(item);
                    }
                })
                .show();
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
