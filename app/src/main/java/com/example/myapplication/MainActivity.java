package com.example.myapplication;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    Button Bdo;
    private HashMap<Integer, MediaPlayer> players = new HashMap<>();
    private final Handler handler = new Handler();
    private static final int DELAY_MS = 1400;

    private void configurarBotao(int botaoId, int somId) {
        View botao = findViewById(botaoId);

        botao.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    tocarSom(somId);
                    v.performClick();
                    break;

                case MotionEvent.ACTION_UP:
                    agendarParada(somId);
                    break;
            }
            return true;
        });
    }
    private void tocarSom(int som) {
        pararSomAtrasado(som);

        MediaPlayer player = new MediaPlayer();
        try {
            AssetFileDescriptor afd = getResources().openRawResourceFd(som);
            if (afd != null) {
                player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();

                player.prepare();
                player.start();
            }
            players.put(som, player);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pararSom(int somId) {
        MediaPlayer player = players.get(somId);
        if (player != null) {
            player.stop();
            player.release();
            players.remove(somId);
        }
    }

    private void pararSomAtrasado(int somId) {
        handler.removeCallbacksAndMessages(somId);
    }
    private void agendarParada(int somId) {
        handler.postDelayed(() -> pararSom(somId), DELAY_MS);
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        configurarBotao(R.id.c4, R.raw.c4);
        configurarBotao(R.id.d4, R.raw.d4);
        configurarBotao(R.id.e4, R.raw.e4);
        configurarBotao(R.id.f4, R.raw.f4);
        configurarBotao(R.id.g4, R.raw.g4);
        configurarBotao(R.id.a4, R.raw.a4);
        configurarBotao(R.id.b4, R.raw.b4);
        configurarBotao(R.id.c5, R.raw.c5);
        configurarBotao(R.id.d5, R.raw.d5);
        configurarBotao(R.id.e5, R.raw.e5);
        configurarBotao(R.id.c4sharp, R.raw.c4sharp);
        configurarBotao(R.id.d4sharp, R.raw.d4sharp);
        configurarBotao(R.id.f4sharp, R.raw.f4sharp);
        configurarBotao(R.id.g4sharp, R.raw.g4sharp);
        configurarBotao(R.id.a4sharp, R.raw.a4sharp);
        configurarBotao(R.id.c5sharp, R.raw.c5sharp);
        configurarBotao(R.id.d5sharp, R.raw.d5sharp);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


}
