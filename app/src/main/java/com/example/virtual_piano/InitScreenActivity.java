package com.example.virtual_piano;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class InitScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.init_screen);

        // --- Deixa em tela cheia (opcional, conforme Level_selection) ---
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // --- Inicializa o Sound_Manager aqui, em vez de Level_selection ---
        Set<Integer> invisiveis = new HashSet<>(Arrays.asList(
                R.raw.a2,   R.raw.a2sharp, R.raw.a3,   R.raw.a3sharp,
                R.raw.a5,   R.raw.a5sharp, R.raw.a6,   R.raw.a6sharp,
                R.raw.b2,   R.raw.b3,      R.raw.b5,   R.raw.b6,
                R.raw.c2,   R.raw.c2sharp, R.raw.c3,   R.raw.c3sharp,
                R.raw.c5sharp, R.raw.c6,   R.raw.c6sharp,
                R.raw.d2,   R.raw.d2sharp, R.raw.d3,   R.raw.d3sharp,
                R.raw.d5sharp, R.raw.d6,   R.raw.d6sharp,
                R.raw.e2,   R.raw.e3,      R.raw.e6,
                R.raw.f2,   R.raw.f2sharp, R.raw.f3,   R.raw.f3sharp,
                R.raw.f5,   R.raw.f5sharp, R.raw.f6,   R.raw.f6sharp,
                R.raw.g2,   R.raw.g2sharp, R.raw.g3,   R.raw.g3sharp,
                R.raw.g5,   R.raw.g5sharp, R.raw.g6,   R.raw.g6sharp
        ));
        Set<Integer> visiveis = new HashSet<>(Arrays.asList(
                R.raw.c4,   R.raw.c4sharp, R.raw.d4,   R.raw.d4sharp,
                R.raw.e4,   R.raw.f4,      R.raw.f4sharp,
                R.raw.g4,   R.raw.g4sharp, R.raw.a4,   R.raw.a4sharp,
                R.raw.b4,   R.raw.c5,      R.raw.c5sharp,
                R.raw.d5,   R.raw.d5sharp, R.raw.e5
        ));

        Sound_Manager.getInstance()
                .initialize(this, invisiveis, visiveis);

        // --- Botão “Jogar” ---
        Button btnJogar = findViewById(R.id.next);
        btnJogar.setOnClickListener(v -> {
            Intent it = new Intent(InitScreenActivity.this, Level_selection.class);
            startActivity(it);
        });

        // --- Botão “Sair” ---
        Button btnSair = findViewById(R.id.quit);
        btnSair.setOnClickListener(v -> {
            // Encerra todas as Activities e sai do app
            finishAffinity();
        });
    }
}
