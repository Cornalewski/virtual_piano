package com.example.virtual_piano;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
            public void onLevelClicked(Level level) {
                // Abrir a tela de Play_music, por exemplo
                Intent it = new Intent(Level_selection.this, Play_music.class);
                it.putExtra("Partitura", level.getPartituraPath());
                startActivity(it);
            }
        });
        rvLevels.setAdapter(adapter);
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
    }}

