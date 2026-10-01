package com.gustavo.tcgcollection.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gustavo.tcgcollection.R;
import com.gustavo.tcgcollection.fonte.Fontes;
import com.gustavo.tcgcollection.model.Carta;
import com.gustavo.tcgcollection.model.ItemColecao;

import java.util.ArrayList;
import java.util.List;

public class ColecaoAdapter extends RecyclerView.Adapter<ColecaoAdapter.VH> {

    public interface AoTocar {
        void tocou(ItemColecao item);
    }

    private final List<ItemColecao> itens = new ArrayList<>();
    private final ImagemLoader imagens;
    private final AoTocar aoTocar;

    public ColecaoAdapter(ImagemLoader imagens, AoTocar aoTocar) {
        this.imagens = imagens;
        this.aoTocar = aoTocar;
    }

    public void definir(List<ItemColecao> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        View v = LayoutInflater.from(pai.getContext()).inflate(R.layout.item_carta, pai, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ItemColecao i = itens.get(pos);
        Carta c = i.carta;
        h.nome.setText(c.nomeBase());
        etiqueta(h.arte, c.tipoArte());
        h.linhaCodigo.setText(juntar(c.codigo, c.raridade, c.cor));
        h.linhaEstado.setText(juntar(Fontes.nomeDoJogo(c.jogo), i.idioma, i.condicao));
        h.qtd.setText("×" + i.quantidade);
        imagens.carregar(c.imagemUrl, h.imagem);
        h.itemView.setOnClickListener(v -> aoTocar.tocou(i));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    /** Mostra a etiqueta de arte, ou esconde se for a arte normal. */
    static void etiqueta(TextView v, String texto) {
        v.setText(texto);
        v.setVisibility(texto != null ? View.VISIBLE : View.GONE);
    }

    /** "a · b · c", pulando vazios. */
    static String juntar(String... partes) {
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p == null || p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append(p);
        }
        return sb.toString();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView imagem;
        final TextView nome, arte, linhaCodigo, linhaEstado, qtd;

        VH(View v) {
            super(v);
            imagem = v.findViewById(R.id.imgCarta);
            nome = v.findViewById(R.id.txtNome);
            arte = v.findViewById(R.id.txtArte);
            linhaCodigo = v.findViewById(R.id.txtCodigo);
            linhaEstado = v.findViewById(R.id.txtEstado);
            qtd = v.findViewById(R.id.txtQtd);
        }
    }
}
