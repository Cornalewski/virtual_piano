package com.example.virtual_piano;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Level_selection extends AppCompatActivity {

    public Level_selection() throws IOException {
    }
    Level nives;
    List<Level> listaDeNiveis = new ArrayList<>();

    public void Carregar_Niveis(Context context, List<Level> listaDeNiveis) throws IOException {
        AssetManager am = context.getAssets();
        String[] aLevels = am.list("levels");
        for (int i = 0; i< Objects.requireNonNull(aLevels).length; i++){
            Level level = new Level(i+1,aLevels[i],false,0);
            listaDeNiveis.add(level);
        }
    }

    @Override
    public AssetManager getAssets() {
        return super.getAssets();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selection_screen);
        RecyclerView rvLevels = findViewById(R.id.rvLevels);
        PathCanvasView pathCanvas = findViewById(R.id.pathCanvas);
        Set<Integer> invisiveis = new HashSet<>(Arrays.asList(
               R.raw.a2,R.raw.a2sharp,R.raw.a3,R.raw.a3sharp,
                R.raw.a5,R.raw.a5sharp,R.raw.a6,R.raw.a6sharp,
                R.raw.b2,R.raw.b3,
                R.raw.b5,R.raw.b6,
                R.raw.c2,R.raw.c2sharp,R.raw.c3,R.raw.c3sharp,
                R.raw.c5sharp,R.raw.c6,R.raw.c6sharp,
                R.raw.d2,R.raw.d2sharp,R.raw.d3,R.raw.d3sharp,
                R.raw.d5sharp,R.raw.d6,R.raw.d6sharp,
                R.raw.e2,R.raw.e3,
                R.raw.e6,
                R.raw.f2,R.raw.f2sharp,R.raw.f3,R.raw.f3sharp,
                R.raw.f5,R.raw.f5sharp,R.raw.f6,R.raw.f6sharp,
                R.raw.g2,R.raw.g2sharp,R.raw.g3,R.raw.g3sharp,
                R.raw.g5,R.raw.g5sharp,R.raw.g6,R.raw.g6sharp
                ));
        Set<Integer> visiveis = new HashSet<>(Arrays.asList(
                R.raw.c4, R.raw.c4sharp, R.raw.d4,R.raw.d4sharp,
                R.raw.e4,R.raw.f4,R.raw.f4sharp,R.raw.g4,R.raw.g4sharp,
                R.raw.a4,R.raw.a4sharp,R.raw.b4,R.raw.c5,R.raw.c5sharp,
                R.raw.d5,R.raw.d5sharp, R.raw.e5
        ));

        Sound_Manager.getInstance().initialize(this,invisiveis,visiveis);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 1) Configurar LayoutManager
        rvLevels.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        // 2) Popular lista de níveis
        try {
            Carregar_Niveis(this, listaDeNiveis);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 3) Criar e associar o adapter *após* preencher a lista
        LevelAdapter adapter = new LevelAdapter(listaDeNiveis);
        adapter.setOnLevelClickListener(new LevelAdapter.OnLevelClickListener() {
            @Override
            public void onLevelClicked(View clickedView, Level level) {
                showLevelPopup(clickedView, level);
            }

            public void onLevelClicked(Level level) {
                Intent it = new Intent(Level_selection.this, Play_music.class);
                it.putExtra("Partitura", level.getPartituraPath());
                startActivity(it);
            }
        });
;
        rvLevels.setAdapter(adapter);
        rvLevels.setItemAnimator(new DefaultItemAnimator() {{
            setAddDuration(300);
            setRemoveDuration(300);
            setMoveDuration(400);
            setChangeDuration(300);
        }});

        // 4) “Linkar” o PathCanvasView para desenhar sobre o RecyclerView
        pathCanvas.bindRecyclerView(rvLevels, adapter);

        // 5) Garante que, a cada scroll/alteração de layout, a linha seja redesenhada
        rvLevels.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                pathCanvas.invalidate();
            }
        });
        rvLevels.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            pathCanvas.invalidate();
        });
    }
    private void showLevelPopup(View anchorView, Level level) {
        // 1) Inflar o layout do popup.
        View popupView = LayoutInflater.from(this)
                .inflate(R.layout.popup_level_info, null);

        TextView tvMusicName = popupView.findViewById(R.id.tvPopupMusicName);
        Button btnPlay = popupView.findViewById(R.id.btnPopupPlay);

        // Defina o texto (nome da música) como quiser.
        // Aqui, vou supor que você queira exibir apenas o nome do arquivo sem a extensão.
        String pathCompleto = level.getPartituraPath(); // ex: "levels/musica1.txt"
        String nomeArquivo = pathCompleto.contains("/")
                ? pathCompleto.substring(pathCompleto.lastIndexOf('/') + 1)
                : pathCompleto;
        // Retira a extensão “.txt”, se existir:
        String nomeSemExt = nomeArquivo.contains(".")
                ? nomeArquivo.substring(0, nomeArquivo.lastIndexOf('.'))
                : nomeArquivo;
        nomeSemExt = nomeSemExt.replace("_"," ");
        nomeSemExt = nomeSemExt.toUpperCase();
        tvMusicName.setText(nomeSemExt);

        // 2) Criar o PopupWindow
        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true // focusable, para que clique fora feche automaticamente
        );
        // Se quiser que o clique fora feche o popup:
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);

        // 3) Para posicionar *exatamente acima* da view clicada,
        // precisamos medir o popup e obter as coordenadas da anchor na tela:

        // Medir o conteúdo do popup para pegar sua largura/altura:
        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int popupWidth = popupView.getMeasuredWidth();
        int popupHeight = popupView.getMeasuredHeight();

        // Obter as coordenadas da anchorView na tela (x, y em pixels)
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int anchorX = location[0];
        int anchorY = location[1];
        int anchorWidth = anchorView.getWidth();
        int anchorHeight = anchorView.getHeight();

        // Calcular em que posição X o popup deve aparecer
        // Para centralizar o popup horizontalmente em relação ao botão clicado:
        int popupX = anchorX + (anchorWidth / 2) - (popupWidth / 2) -80;
        // Para posicionar o popup *acima* do botão com um pequeno gap:
        int gap = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        int popupY = anchorY - popupHeight - gap;

        // 4) Exibir o popup na tela
        popupWindow.showAtLocation(
                anchorView,           // root
                Gravity.NO_GRAVITY,   // vamos usar coordenadas exatas
                popupX,
                popupY
        );

        // 5) Configurar o clique no botão “Carregar Música” do popup
        btnPlay.setOnClickListener(v -> {
            // 5.1) Abre a Activity Play_music passando a partitura
            Intent it = new Intent(Level_selection.this, Play_music.class);
            it.putExtra("Partitura", level.getPartituraPath());
            startActivity(it);

            // 5.2) Fecha o popup
            popupWindow.dismiss();
        });

        // (Opcional) Se quiser animar a entrada do popup,
        // configure alguma Animation antes de chamar showAtLocation.
    }}


