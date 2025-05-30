package com.example.virtual_piano;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.media.SoundPool;
import android.media.AudioAttributes;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Play_music extends AppCompatActivity {

    private SoundPool soundPool;
    private final Map<Integer, Integer> soundIdMap = new HashMap<>();  // rawResId -> soundPool soundId
    private final Map<Integer, Integer> streamIdMap = new HashMap<>(); // rawResId -> current streamId
    private int totalToLoad;
    private AtomicInteger loadedCount = new AtomicInteger(0);
    Intent it;
    private final Handler handler = new Handler();
    private static final int DELAY_MS = 1400;
    private int Tempo_musica;
    private PartituraView partituraView;
    private boolean partituraJaIniciada = false;
    private List<Nota> notas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.teclas_c4_e5);

        Intent it = getIntent();
        String path = it.getStringExtra("Partitura");
        notas = carregarNotasDeAssets(this, path);
        partituraView = findViewById(R.id.partituraView);
        partituraView.setNotas(notas);

        Set<Integer> invisiveisParaLoad = new HashSet<>();
        for (Nota n : notas) {
            if (!n.visivel) {
                int raw = getRawIdPorNome(n.nome);
                if (raw != 0) invisiveisParaLoad.add(raw);
            }
        }
        totalToLoad = invisiveisParaLoad.size();

        // Initialize SoundPool
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(attrs)
                .build();

        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0 && loadedCount.incrementAndGet() == totalToLoad) {
                // todas invisíveis carregadas → agenda sons automáticos
                runOnUiThread(this::agendarTocadoresAutomaticos);
            }
        });
        for (int resId : invisiveisParaLoad) {
            int soundId = soundPool.load(this, resId, 1);
            soundIdMap.put(resId, soundId);
        }
        // Preload visible key sounds
        int[] visiveis = {
                R.raw.c4, R.raw.c4sharp, R.raw.d4, R.raw.d4sharp, R.raw.e4,
                R.raw.f4, R.raw.f4sharp, R.raw.g4, R.raw.g4sharp,
                R.raw.a4, R.raw.a4sharp, R.raw.b4,
                R.raw.c5, R.raw.c5sharp, R.raw.d5, R.raw.d5sharp, R.raw.e5
        };
        for (int resId : visiveis) {
            int soundId = soundPool.load(this, resId, 1);
            soundIdMap.put(resId, soundId);
        }

        // Configure button touch to play with SoundPool
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

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
    private void agendarTocadoresAutomaticos() {
        for (Nota n : notas) {
            if (!n.visivel) {
                final int raw = getRawIdPorNome(n.nome);
                handler.postDelayed(() -> {
                    tocarSom(raw);
                    handler.postDelayed(() -> pararSom(raw), DELAY_MS);
                }, n.tempoInicio);
            }
        }
    }
    public List<Nota> carregarNotasDeAssets(Context context, String nomeArquivo) {
        long tempoAtual = 0;
        int duracaoPadrao = 800;
        int duracaoCurta = 600;

        Pattern padraoNota = Pattern.compile("([A-Ga-g]#?[0-9])");
        List<Nota> lista = new ArrayList<>();

        try (InputStream is = context.getAssets().open(nomeArquivo);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.trim().split(" ");
                for (String parte : partes) {
                    boolean ligada = parte.startsWith("|") && parte.endsWith("|");
                    String trecho = parte.replace("|", "");
                    Matcher matcher = padraoNota.matcher(trecho);
                    List<String> notasNoBloco = new ArrayList<>();
                    while (matcher.find()) notasNoBloco.add(matcher.group(1).toUpperCase());
                    if (notasNoBloco.isEmpty()) continue;
                    int duracao = (notasNoBloco.size() > 1 && !parte.contains(" ")) ? duracaoCurta : duracaoPadrao;
                    for (String notaTexto : notasNoBloco) {
                        boolean visivel = estaNaTela(notaTexto);
                        Nota nota = new Nota(notaTexto, ligada, visivel, tempoAtual, duracao);
                        nota.colcheia = (duracao == duracaoCurta);
                        lista.add(nota);
                        tempoAtual += ligada ? 0 : duracao;
                        Tempo_musica += tempoAtual;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lista;
    }

    private boolean estaNaTela(String nota) {
        String[] visiveis = {"C4", "C#4", "D4", "D#4", "E4", "F4", "F#4", "G4", "G#4",
                "A4", "A#4", "B4", "C5", "C#5", "D5", "D#5", "E5"};
        return Arrays.asList(visiveis).contains(nota);
    }

    private void configurarBotao(int botaoId, int somId) {
        View botao = findViewById(botaoId);
        botao.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!partituraJaIniciada) {
                        // 1º toque: inicia animação
                        partituraView.iniciarPartitura();
                        partituraJaIniciada = true;
                        // se invisíveis já carregadas, agenda reprodução
                        if (loadedCount.get() == totalToLoad) {
                            agendarTocadoresAutomaticos();
                        }
                    }
                    tocarSom(somId);
                    ativarNotaTocada(getNomeNotaPorId(somId));
                    v.performClick();
                    break;
                case MotionEvent.ACTION_UP:
                    handler.postDelayed(() -> pararSom(somId), DELAY_MS);
                    break;
            }
            return true;
        });
    }

    private void tocarSom(int resId) {
        handler.removeCallbacksAndMessages(resId);
        Integer soundId = soundIdMap.get(resId);
        if (soundId != null) {
            int stream = soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
            streamIdMap.put(resId, stream);
        }
    }

    private void pararSom(int resId) {
        Integer stream = streamIdMap.get(resId);
        if (stream != null) {
            soundPool.stop(stream);
            streamIdMap.remove(resId);
        }
    }

    private void agendarParada(int somId) {
        handler.postDelayed(() -> pararSom(somId), DELAY_MS);
    }

    private void ativarNotaTocada(String notaTocada) {
        if (!partituraJaIniciada || partituraView == null) return;
        long agora = System.currentTimeMillis();
        long tempoAtual = agora - partituraView.getTempoInicial();
        float halfW = partituraView.getWidth() / 2f;
        float hitRange = 40f;
        for (Nota nota : notas) {
            if (!nota.visivel || nota.tocando) continue;
            if (!nota.nome.equals(notaTocada)) continue;
            List<Nota> grupo = new ArrayList<>();
            for (Nota n : notas) if (n.visivel && n.tempoInicio == nota.tempoInicio) grupo.add(n);
            int idx = grupo.indexOf(nota);
            int total = grupo.size();
            float xBase = halfW + nota.tempoInicio * PartituraView.TIME_TO_PX;
            float deslocamento = tempoAtual * PartituraView.SCROLL_SPEED;
            float chordOffset = (idx - (total - 1) / 2f) * partituraView.espacamentoEntreNotas;
            float xNota = xBase - deslocamento + chordOffset;
            float xIndicador = halfW;
            if (Math.abs(xNota - xIndicador) <= hitRange) {
                nota.tocando = true;
                partituraView.invalidate();
                break;
            }
        }
    }

    private String getNomeNotaPorId(int somId) {
        if (somId == R.raw.c4) return "C4";
        else if (somId == R.raw.c4sharp) return "C#4";
        else if (somId == R.raw.d4) return "D4";
        else if (somId == R.raw.d4sharp) return "D#4";
        else if (somId == R.raw.e4) return "E4";
        else if (somId == R.raw.f4) return "F4";
        else if (somId == R.raw.f4sharp) return "F#4";
        else if (somId == R.raw.g4) return "G4";
        else if (somId == R.raw.g4sharp) return "G#4";
        else if (somId == R.raw.a4) return "A4";
        else if (somId == R.raw.a4sharp) return "A#4";
        else if (somId == R.raw.b4) return "B4";
        else if (somId == R.raw.c5) return "C5";
        else if (somId == R.raw.c5sharp) return "C#5";
        else if (somId == R.raw.d5) return "D5";
        else if (somId == R.raw.d5sharp) return "D#5";
        else if (somId == R.raw.e5) return "E5";
        return "";
    }

    private int getRawIdPorNome(String nomeNota) {
        switch (nomeNota) {
            case "C2":
                return R.raw.c2;
            case "C#2":
                return R.raw.c2sharp;
            case "D2":
                return R.raw.d2;
            case "D#2":
                return R.raw.d2sharp;
            case "E2":
                return R.raw.e2;
            case "F2":
                return R.raw.f2;
            case "F#2":
                return R.raw.f2sharp;
            case "G2":
                return R.raw.g2;
            case "G#2":
                return R.raw.g2sharp;
            case "A2":
                return R.raw.a2;
            case "A#2":
                return R.raw.a2sharp;
            case "B2":
                return R.raw.b2;
            case "C3":
                return R.raw.c3;
            case "C#3":
                return R.raw.c3sharp;
            case "D3":
                return R.raw.d3;
            case "D#3":
                return R.raw.d3sharp;
            case "E3":
                return R.raw.e3;
            case "F3":
                return R.raw.f3;
            case "F#3":
                return R.raw.f3sharp;
            case "G3":
                return R.raw.g3;
            case "G#3":
                return R.raw.g3sharp;
            case "A3":
                return R.raw.a3;
            case "A#3":
                return R.raw.a3sharp;
            case "B3":
                return R.raw.b3;
            case "C4":
                return R.raw.c4;
            case "C#4":
                return R.raw.c4sharp;
            case "D4":
                return R.raw.d4;
            case "D#4":
                return R.raw.d4sharp;
            case "E4":
                return R.raw.e4;
            case "F4":
                return R.raw.f4;
            case "F#4":
                return R.raw.f4sharp;
            case "G4":
                return R.raw.g4;
            case "G#4":
                return R.raw.g4sharp;
            case "A4":
                return R.raw.a4;
            case "A#4":
                return R.raw.a4sharp;
            case "B4":
                return R.raw.b4;
            case "C5":
                return R.raw.c5;
            case "C#5":
                return R.raw.c5sharp;
            case "D5":
                return R.raw.d5;
            case "D#5":
                return R.raw.d5sharp;
            case "E5":
                return R.raw.e5;
            case "F5":
                return R.raw.f5;
            case "F#5":
                return R.raw.f5sharp;
            case "G5":
                return R.raw.g5;
            case "G#5":
                return R.raw.g5sharp;
            case "A5":
                return R.raw.a5;
            case "A#5":
                return R.raw.a5sharp;
            case "B5":
                return R.raw.b5;
            case "C6":
                return R.raw.c6;
            case "C#6":
                return R.raw.c6sharp;
            case "D6":
                return R.raw.d6;
            case "D#6":
                return R.raw.d6sharp;
            case "E6":
                return R.raw.e6;
            case "F6":
                return R.raw.f6;
            case "F#6":
                return R.raw.f6sharp;
            case "G6":
                return R.raw.g6;
            case "G#6":
                return R.raw.g6sharp;
            case "A6":
                return R.raw.a6;
            case "A#6":
                return R.raw.a6sharp;
            case "B6":
                return R.raw.b6;
            case "C7":
                return R.raw.c7;
            default:
                return 0;
        }
    }

    private void iniciarPartitura() {
        String path = getIntent().getStringExtra("Partitura");
        notas = carregarNotasDeAssets(this, path);
        partituraView.setNotas(notas);
        partituraView.iniciarPartitura();

        // usa o handler de instância e seus métodos tocarSom/pararSom
        for (Nota nota : notas) {
            if(!nota.visivel) {
                final int rawResId = getRawIdPorNome(nota.nome);
                handler.postDelayed(() -> {
                    // este tocarSom já coloca o streamId no mapa
                    tocarSom(rawResId);
                    // e aqui ele para corretamente
                    handler.postDelayed(() -> pararSom(rawResId), DELAY_MS);
                }, nota.tempoInicio);
            }
        }
    }

}
