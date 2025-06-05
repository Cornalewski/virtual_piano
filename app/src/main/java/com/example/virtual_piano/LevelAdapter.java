package com.example.virtual_piano;
import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.LevelVH> {

    private static List<Level> listaDeNiveis;
    private static OnLevelClickListener listener; // interface para clique

    public LevelAdapter(List<Level> lista) {
        this.listaDeNiveis = lista;
    }

    public void setOnLevelClickListener(OnLevelClickListener l) {
        this.listener = l;
    }

    @Override
    public void onBindViewHolder(@NonNull LevelVH holder, int position) {
        Level lvl = listaDeNiveis.get(position);
        holder.bind(lvl);

        DisplayMetrics metrics = holder.itemView.getContext()
                .getResources()
                .getDisplayMetrics();

        RecyclerView.LayoutParams params =
                (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();

        // 1) Espaçamento ENTRE iténs, na direção de rolagem (agora HORIZONTAL):
        int spacingDp = 20;
        int spacingPx = (int) dpToPx(spacingDp, metrics);
        params.leftMargin = spacingPx;
        if (position == listaDeNiveis.size() - 1) {
            params.rightMargin = spacingPx;
        } else {
            params.rightMargin = 0;
        }

        // 2) Zig-zag VERTICAL: item par em cima, ímpar embaixo (ou vice-versa).
        int zigzagDp = 180;
        int zigzagPx = (int) dpToPx(zigzagDp, metrics);

        // Zera margens verticais antes de definir:
        params.topMargin = 0;
        params.bottomMargin = 0;

        if (position % 2 == 0) {
            // posição par → empurra para CIMA (diminuindo espaço em cima → maior space embaixo)
            params.topMargin = 250;
            params.bottomMargin = zigzagPx;
        } else {
            // posição ímpar → empurra para BAIXO
            params.topMargin = zigzagPx;
            params.bottomMargin = 0;
        }

        holder.itemView.setLayoutParams(params);
    }


    @NonNull
    @Override
    public LevelVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.level_item, parent, false);
        return new LevelVH(view);
    }

    @Override
    public int getItemCount() {
        return listaDeNiveis.size();
    }

    static class LevelVH extends RecyclerView.ViewHolder {
        ImageView imgBg, imgStarsOrCrown, imgFriend;
        TextView tvNumber;

        public LevelVH(@NonNull View itemView) {
            super(itemView);
            imgBg = itemView.findViewById(R.id.imgLevelBg);
            tvNumber = itemView.findViewById(R.id.tvLevelNumber);
            imgStarsOrCrown = itemView.findViewById(R.id.imgStarsOrCrown);
            imgFriend = itemView.findViewById(R.id.imgFriendBadge);

            itemView.setOnClickListener(v -> {
                int idx = getAdapterPosition();
                if (idx != RecyclerView.NO_POSITION && listener != null) {
                    listener.onLevelClicked(listaDeNiveis.get(idx));
                }
            });
        }

        public void bind(Level lvl) {
            // 1) Número
            tvNumber.setText(String.valueOf(lvl.numero));

            // 2) Estado “trancado X desbloqueado”
            if (lvl.estrelas == 3) {
                imgStarsOrCrown.setVisibility(View.VISIBLE);
                //substutuir por ic_crown
                imgStarsOrCrown.setImageResource(R.drawable.ic_star_filled);
            } else if (lvl.estrelas > 0) {
                imgStarsOrCrown.setVisibility(View.VISIBLE);
                switch (lvl.estrelas) {
                    case 1:
                        //substutuir por ic_1star
                        imgStarsOrCrown.setImageResource(R.drawable.ic_star_filled);
                        break;
                    case 2:
                        //substutuir por ic_2star
                        imgStarsOrCrown.setImageResource(R.drawable.ic_star_filled);
                        break;
                }
            } else {
                imgStarsOrCrown.setVisibility(View.GONE);
            }
        }
    }
        public interface OnLevelClickListener {
            void onLevelClicked(Level level);
        }
}
