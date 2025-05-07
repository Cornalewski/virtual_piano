package com.example.virtual_piano;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    Button Bdo;
    private HashMap<Integer, MediaPlayer> players = new HashMap<>();
    private final Handler handler = new Handler();
    private final Handler Thandler = new Handler();
    private static final int DELAY_MS = 1400;
    private int Tempo_musica;
    PartituraView partituraView;
    boolean partituraJaIniciada = false;



    List<Nota> notas = new ArrayList<>();
    int t = 0;
    public List<Nota> carregarNotasDeAssets(Context context, String nomeArquivo) {

        long tempoAtual = 0;
        long duracaoNota = 700;

        Pattern padraoNota = Pattern.compile("([A-Ga-g]#?[0-9])"); // Ex: C4, D#5

        try {
            InputStream is = context.getAssets().open(nomeArquivo);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String linha;

            while ((linha = br.readLine()) != null) {
                boolean ligada = false;

                // Se houver notas ligadas (entre |), separa manualmente
                String[] partes = linha.split("(?=\\|)|(?<=\\|)");
                for (String parte : partes) {
                    ligada = parte.startsWith("|") && parte.endsWith("|");

                    // Remove os | antes de aplicar regex
                    parte = parte.replace("|", "");

                    Matcher matcher = padraoNota.matcher(parte);
                    while (matcher.find()) {
                        String nota = matcher.group(1).toUpperCase();
                        boolean visivel = estaNaTela(nota);
                        notas.add(new Nota(nota, ligada, visivel, tempoAtual, duracaoNota));
                        tempoAtual += ligada ? 0 : duracaoNota;
                        Tempo_musica += tempoAtual;

                    }
                }
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return notas;
    }


    private boolean estaNaTela(String nota) {
        // Considera C3 até E5
        String[] visiveis = {"C4", "C#4", "D4", "D#4", "E4", "F4", "F#4", "G4", "G#4",
                             "A4", "A#4", "B4","C5", "C#5", "D5", "D#5", "E5"};
        return Arrays.asList(visiveis).contains(nota);
    }


    private void configurarBotao(int botaoId, int somId) {
        View botao = findViewById(botaoId);

        botao.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    if (!partituraJaIniciada) {
                        partituraView.iniciarPartitura();
                        partituraJaIniciada = true;
                    }
                    tocarSom(somId);
                    String nomeNota = getNomeNotaPorId(somId);
                    ativarNotaTocada(nomeNota);
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
    private void ativarNotaTocada(String notaTocada) {
        if (!partituraJaIniciada || partituraView == null) return;

        long tempoAtual = System.currentTimeMillis() - partituraView.getTempoInicial();
        final long tolerancia = 200; // 200 ms para frente/atrás

        for (Nota nota : notas) {
            if (!nota.visivel || nota.tocando) continue;

            long inicio = nota.tempoInicio;
            if (Math.abs(tempoAtual - inicio) <= tolerancia && nota.nome.equals(notaTocada)) {
                nota.tocando = true;
                partituraView.invalidate(); // Redesenha com destaque rosa
                break; // Considera só a primeira nota compatível
            }
        }
    }
    private String getNomeNotaPorId(int somId) {

        if (somId == R.raw.c4) {
            return "C4";
        } else if (somId == R.raw.c4sharp) {
            return "C#4";
        } else if (somId == R.raw.d4) {
            return "D4";
        } else if (somId == R.raw.d4sharp) {
            return "D#4";
        } else if (somId == R.raw.e4) {
            return "E4";
        } else if (somId == R.raw.f4) {
            return "F4";
        } else if (somId == R.raw.f4sharp) {
            return "F#4";
        } else if (somId == R.raw.g4) {
            return "G4";
        } else if (somId == R.raw.g4sharp) {
            return "G#4";
        } else if (somId == R.raw.a4) {
            return "A4";
        } else if (somId == R.raw.a4sharp) {
            return "A#4";
        } else if (somId == R.raw.b4) {
            return "B4";
        } else if (somId == R.raw.c5) {
            return "C5";
        } else if (somId == R.raw.c5sharp) {
            return "C#5";
        } else if (somId == R.raw.d5) {
            return "D5";
        } else if (somId == R.raw.d5sharp) {
            return "D#5";
        } else if (somId == R.raw.e5) {
            return "E5";
        } else {
            return "";
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);



        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    public void run() {
                        Log.i("tag", "This'll run 300 milliseconds later");
                        t++;
                    }
                },
                500);


        List<Nota> listaNotas = carregarNotasDeAssets(this, "ode_alegria.txt");
        partituraView = findViewById(R.id.partituraView);
        partituraView.setNotas(listaNotas);
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
