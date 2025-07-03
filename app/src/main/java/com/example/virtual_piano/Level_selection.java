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
        // 1) verifica as sub-pastas de dificuldade dentro de "levels"
        String[] difficulties = am.list("levels");
        if (difficulties == null) return;

        // opcional: garante ordem crescente das pastas ("1","2","3","4")
        Arrays.sort(difficulties);

        int counter = 1;
        for (String difficulty : difficulties) {
            String folderPath = "levels/" + difficulty;
            // 2) lista arquivos dentro de cada pasta de dificuldade
            String[] levelFiles = am.list(folderPath);
            if (levelFiles == null) continue;

            // opcional: ordena alfabeticamente os arquivos de cada pasta
            Arrays.sort(levelFiles);

            for (String fileName : levelFiles) {
                // 3) monta o caminho relativo e cria o objeto Level
                String fullPath = difficulty + "/" + fileName;
                Level level = new Level(counter, fullPath, false, 0);
                listaDeNiveis.add(level);
                counter++;
            }
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
        //medir o popup e obter as coordenadas da anchor na tela:

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
        int popupX = (anchorX + (anchorWidth / 2) - (popupWidth / 2));
        // Para posicionar o popup *acima* do botão com um pequeno gap:
        int gap = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());

        int popupY = (anchorY + popupHeight + gap);

        // 4) Exibir o popup na tela)
        popupWindow.showAtLocation(
                anchorView,           // root
                Gravity.NO_GRAVITY,   // vamos usar coordenadas exatas
                popupX ,
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