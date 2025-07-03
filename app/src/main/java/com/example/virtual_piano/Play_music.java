package com.example.virtual_piano;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Play_music extends AppCompatActivity {

    private final Handler handler = new Handler();
    private static final int DELAY_MS = 1000;
    private PartituraView partituraView;
    private boolean partituraJaIniciada = false;
    private List<Nota> notas = new ArrayList<>();
    private long duraçao_musica = 0;
    int streamid = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.teclas_c4_e5);

        // Carrega notas e configura PartituraView (inicial parado)
        Intent it = getIntent();
        String path = it.getStringExtra("Partitura");
        notas = carregarNotasDeAssets(this, path);
        partituraView = findViewById(R.id.partituraView);
        partituraView.setNotas(notas);

        // Identifica notas invisíveis para carregar antes da reprodução automática
        Set<Integer> invisiveisParaLoad = new HashSet<>();
        for (Nota n : notas) {
            if (!n.visivel) {
                int raw = getRawIdPorNome(n.nome);
                if (raw != 0) invisiveisParaLoad.add(raw);
            }
        }
        // Configura botões para interação
        configurarBotao(R.id.c4, R.raw.c4);
        configurarBotao(R.id.c4sharp, R.raw.c4sharp);
        configurarBotao(R.id.d4, R.raw.d4);
        configurarBotao(R.id.d4sharp, R.raw.d4sharp);
        configurarBotao(R.id.e4, R.raw.e4);
        configurarBotao(R.id.f4, R.raw.f4);
        configurarBotao(R.id.f4sharp, R.raw.f4sharp);
        configurarBotao(R.id.g4, R.raw.g4);
        configurarBotao(R.id.g4sharp, R.raw.g4sharp);
        configurarBotao(R.id.a4, R.raw.a4);
        configurarBotao(R.id.a4sharp, R.raw.a4sharp);
        configurarBotao(R.id.b4, R.raw.b4);
        configurarBotao(R.id.c5, R.raw.c5);
        configurarBotao(R.id.c5sharp, R.raw.c5sharp);
        configurarBotao(R.id.d5, R.raw.d5);
        configurarBotao(R.id.d5sharp, R.raw.d5sharp);
        configurarBotao(R.id.e5, R.raw.e5);

        long delayTotal = duraçao_musica + DELAY_MS;

        handler.postDelayed(() -> {
            // inicia a Activity de seleção de níveis
            Intent intent = new Intent(Play_music.this, Level_selection.class);
            startActivity(intent);
            // opcional: fecha a tela de playback para removê-la da pilha
            finish();
        }, delayTotal + 1500);

        // Ajuste de Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });
    }

    private void agendarTocadoresAutomaticos() {
        for (Nota n : notas) {
            if (!n.visivel) {
                final int raw = getRawIdPorNome(n.nome);
                handler.postDelayed(() -> {
                    Sound_Manager.getInstance().play(raw);
                    handler.postDelayed(() -> Sound_Manager.getInstance().stop(raw), DELAY_MS);
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

        try (InputStream is = context.getAssets().open("levels/"+nomeArquivo);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.trim().split(" ");
                for (String parte : partes) {
                    boolean ligada = parte.startsWith("[") && parte.endsWith("]");
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
                        long fim = nota.tempoInicio + nota.duracao;
                        if (fim > duraçao_musica) {
                            duraçao_musica = fim;
                        }
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
                        partituraView.iniciarPartitura();
                        partituraJaIniciada = true;
                        // Só agenda as notas invisíveis se o preload já tiver sido concluído
                        if (Sound_Manager.getInstance().areUrgentesReady()) {
                            agendarTocadoresAutomaticos();
                        } else {
                            // Se ainda não estiver pronto, podemos checar de tempos em tempos:
                            handler.postDelayed(this::verificarParaAgendar, 100);
                        }
                    }
                    // Toca o som da tecla visível
                    Sound_Manager.getInstance().play(somId);
                    streamid = Sound_Manager.getInstance().play(somId);
                    ativarNotaTocada(getNomeNotaPorId(somId));
                    v.performClick();
                    break;

                case MotionEvent.ACTION_UP:
                    handler.postDelayed(() -> Sound_Manager.getInstance().stop(somId), DELAY_MS);
                    handler.postDelayed(() -> Sound_Manager.getInstance().stopStream(streamid),DELAY_MS);
                    break;
            }
            return true;
        });
    }
    private void verificarParaAgendar() {
        if (Sound_Manager.getInstance().areUrgentesReady() && partituraJaIniciada) {
            agendarTocadoresAutomaticos();
        } else if (partituraJaIniciada) {
            // Recheca daqui a 100ms até estar pronto
            handler.postDelayed(this::verificarParaAgendar, 100);
        }
    }

    private void ativarNotaTocada(String notaTocada) {
        if (!partituraJaIniciada || partituraView == null) return;
        float time_to_px = partituraView.getTIME_TO_PX();
        float scroll_speed = time_to_px;
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
            float xBase = halfW + nota.tempoInicio * time_to_px;
            float deslocamento = tempoAtual * scroll_speed;
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
}