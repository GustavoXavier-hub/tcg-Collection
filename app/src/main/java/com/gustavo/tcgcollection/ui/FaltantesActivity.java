package com.gustavo.tcgcollection.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gustavo.tcgcollection.R;
import com.gustavo.tcgcollection.data.ColecaoRepo;
import com.gustavo.tcgcollection.fonte.FonteCartas;
import com.gustavo.tcgcollection.fonte.Fontes;
import com.gustavo.tcgcollection.fonte.OnePieceFonte;
import com.gustavo.tcgcollection.model.Carta;
import com.gustavo.tcgcollection.util.MugiwaraPersona;
import com.gustavo.tcgcollection.util.MugiwaraPersona.Contexto;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Cartas do set que ainda não estão na coleção (em nenhuma versão).
 * O checklist do set é baixado uma vez e guardado no banco; só vai à rede
 * de novo depois de {@link #VALIDADE_MS} ou no "atualizar".
 * Tocar numa faltante abre o cadastro já com o código.
 */
public class FaltantesActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID = "set_id";
    public static final String EXTRA_SET_NOME = "set_nome";

    private static final String TAG = "TcgFaltantes";
    private static final long VALIDADE_MS = TimeUnit.DAYS.toMillis(30);

    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ColecaoRepo repo;
    private FonteCartas fonte;
    private String setId;
    private Adapter adapter;
    private TextView txtStatus, txtVazio, btnAtualizar;
    private boolean carregouUmaVez;

    @Override
    protected void onCreate(Bundle salvo) {
        super.onCreate(salvo);
        setContentView(R.layout.activity_faltantes);

        repo = new ColecaoRepo(this);
        fonte = Fontes.porJogo(OnePieceFonte.JOGO);
        setId = getIntent().getStringExtra(EXTRA_SET_ID);
        String nome = getIntent().getStringExtra(EXTRA_SET_NOME);

        ((TextView) findViewById(R.id.txtSet)).setText(
                ColecaoAdapter.juntar(nome, setId));
        txtStatus = findViewById(R.id.txtStatus);
        txtVazio = findViewById(R.id.txtVazio);
        btnAtualizar = findViewById(R.id.btnAtualizar);
        PersonaImagens.aplicar(findViewById(R.id.imgFundo), PersonaImagens.FUNDO, 0);

        adapter = new Adapter(ImagemLoader.get(this), this::adicionar);
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);

        btnAtualizar.setOnClickListener(v -> carregar(true));
        if (setId == null) {
            finish();
            return;
        }
        carregar(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Voltando do cadastro: recalcula só pelo banco, sem rede.
        if (carregouUmaVez) mostrarDoBanco(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bg.shutdown();
    }

    private void carregar(boolean forcar) {
        btnAtualizar.setEnabled(false);
        status("Mirando no checklist de " + setId + "…");
        bg.execute(() -> {
            ColecaoRepo.Checklist k = repo.checklist(fonte.jogo(), setId);
            boolean velho = k == null || System.currentTimeMillis() - k.baixadoEm > VALIDADE_MS;
            String aviso = null;
            if (forcar || velho) {
                try {
                    List<Carta> cartas = fonte.checklistDoSet(setId);
                    if (cartas.isEmpty()) {
                        aviso = MugiwaraPersona.frase(Contexto.NAO_ENCONTRADA)
                                + "\n(a API não tem o checklist de " + setId + ")";
                    } else {
                        repo.salvarChecklist(fonte.jogo(), setId, cartas);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "falha ao baixar checklist", e);
                    aviso = MugiwaraPersona.frase(Contexto.ERRO)
                            + (k != null ? "\nMostrando a lista guardada." : "\n" + AdicionarActivity.descrever(e));
                }
            }
            final String a = aviso;
            main.post(() -> {
                carregouUmaVez = true;
                btnAtualizar.setEnabled(true);
                mostrarDoBanco(a);
            });
        });
    }

    /** aviso != null fica no lugar do status normal (erro de rede etc.). */
    private void mostrarDoBanco(String aviso) {
        bg.execute(() -> {
            ColecaoRepo.Checklist k = repo.checklist(fonte.jogo(), setId);
            List<Carta> faltam = k != null ? repo.faltantes(fonte.jogo(), setId) : new ArrayList<>();
            main.post(() -> mostrar(k, faltam, aviso));
        });
    }

    private void mostrar(ColecaoRepo.Checklist k, List<Carta> faltam, String aviso) {
        adapter.definir(faltam);
        if (k == null) {
            txtVazio.setVisibility(View.GONE);
            status(aviso != null ? aviso : "Checklist indisponível.");
            return;
        }
        boolean completo = faltam.isEmpty();
        txtVazio.setText(MugiwaraPersona.frase(Contexto.SET_COMPLETO));
        txtVazio.setVisibility(completo ? View.VISIBLE : View.GONE);

        String data = new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(new Date(k.baixadoEm));
        int tenho = k.total - faltam.size();
        String resumo = completo
                ? "Completo: " + k.total + " de " + k.total
                : "Faltam " + faltam.size() + " de " + k.total + " · você tem " + tenho;
        status((aviso != null ? aviso + "\n" : "") + resumo + "\nchecklist de " + data);
    }

    private void adicionar(Carta c) {
        Intent i = new Intent(this, AdicionarActivity.class);
        i.putExtra(AdicionarActivity.EXTRA_CODIGO, c.codigo);
        startActivity(i);
    }

    private void status(String msg) {
        txtStatus.setText(msg);
    }

    /** Mesmo visual da lista da coleção, mas cada linha é uma carta que falta. */
    static class Adapter extends RecyclerView.Adapter<ColecaoAdapter.VH> {

        interface AoTocar { void tocou(Carta c); }

        private final List<Carta> itens = new ArrayList<>();
        private final ImagemLoader imagens;
        private final AoTocar aoTocar;

        Adapter(ImagemLoader imagens, AoTocar aoTocar) {
            this.imagens = imagens;
            this.aoTocar = aoTocar;
        }

        void definir(List<Carta> novos) {
            itens.clear();
            itens.addAll(novos);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ColecaoAdapter.VH onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
            View v = LayoutInflater.from(pai.getContext()).inflate(R.layout.item_carta, pai, false);
            return new ColecaoAdapter.VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ColecaoAdapter.VH h, int pos) {
            Carta c = itens.get(pos);
            h.nome.setText(c.nomeBase());
            ColecaoAdapter.etiqueta(h.arte, c.tipoArte());
            h.linhaCodigo.setText(ColecaoAdapter.juntar(c.codigo, c.raridade, c.cor));
            h.linhaEstado.setText("toque para adicionar");
            h.qtd.setText("+");
            imagens.carregar(c.imagemUrl, h.imagem);
            h.itemView.setOnClickListener(v -> aoTocar.tocou(c));
        }

        @Override
        public int getItemCount() {
            return itens.size();
        }
    }
}
