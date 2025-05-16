package com.example.virtual_piano;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class PartituraView extends View {

    private List<Nota> notas;
    private Paint paintNota;
    private Paint paintLinha;
    private Paint paintDestaque;
    private Long tempoInicial = null;
    private static final float VELOCIDADE_PIXELS_POR_MS = 0.01f;
    private static final float Espaçamento_nota = 0.40f;
    private static final float raioNota = 42f;
    private static final float alturaNota = 120f;

    public PartituraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintNota = new Paint();
        paintNota.setColor(Color.WHITE);
        paintNota.setStyle(Paint.Style.FILL);

        paintLinha = new Paint();
        paintLinha.setColor(Color.LTGRAY);
        paintLinha.setStrokeWidth(3);

        paintDestaque = new Paint();
        paintDestaque.setColor(Color.parseColor("#DD448AFF")); // Rosa com transparência
        paintDestaque.setStyle(Paint.Style.FILL);
    }

    public void setNotas(List<Nota> notas) {
        this.notas = notas;
        invalidate();
    }
    public void iniciarPartitura() {
        tempoInicial = System.currentTimeMillis();
        invalidate();
    }
    private Map<Long, Float> beamBottomByTempo = new HashMap<>();

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float espacamentoEntreNotas = 140f;
        float alturaFeixe = 8f;// espessura do feixe
        float alturaHasteTotal = alturaNota + alturaFeixe;
        beamBottomByTempo.clear();


        if (tempoInicial == null || notas == null) return;

        long tempoAtual = System.currentTimeMillis() - tempoInicial;

        // Fundo
        canvas.drawColor(Color.parseColor("#FDFDFD"));

        // Pentagrama
        Paint paintLinha = new Paint();
        paintLinha.setColor(Color.parseColor("#CCCCCC"));
        paintLinha.setStrokeWidth(2);

        float linhaBaseY = getHeight() / 2 - 2 * 30;
        for (int i = 0; i < 5; i++) {
            float y = linhaBaseY + i * 50;
            canvas.drawLine(0, y, getWidth(), y, paintLinha);
        }

        // Paints gerais
        Paint paintNota = new Paint();
        paintNota.setColor(Color.WHITE);
        paintNota.setStyle(Paint.Style.FILL);

        Paint paintContorno = new Paint();
        paintContorno.setColor(Color.BLACK);
        paintContorno.setStyle(Paint.Style.STROKE);
        paintContorno.setStrokeWidth(3);

        Paint paintHaste = new Paint();
        paintHaste.setColor(Color.BLACK);

        Paint paintFundoVerde = new Paint();
        paintFundoVerde.setColor(Color.parseColor("#5534C759"));

        Paint paintBordaVerde = new Paint();
        paintBordaVerde.setColor(Color.parseColor("#34C759"));
        paintBordaVerde.setStyle(Paint.Style.STROKE);
        paintBordaVerde.setStrokeWidth(4);

        Paint linhaTempoPaint = new Paint();
        linhaTempoPaint.setColor(Color.GREEN);
        linhaTempoPaint.setStrokeWidth(6);
        float xIndicador = getWidth() / 2f;
        canvas.drawLine(xIndicador, 0, xIndicador, getHeight(), linhaTempoPaint);

        // Agrupar por tempo para desenhar conexões
        Map<Long, List<Nota>> notasPorTempo = new HashMap<>();
        for (Nota nota : notas) {
            if (!nota.visivel) continue;
            notasPorTempo
                    .computeIfAbsent(nota.tempoInicio, k -> new ArrayList<>())
                    .add(nota);
        }


        Paint paintFeixe = new Paint();
        paintFeixe.setColor(Color.BLACK);
        paintFeixe.setStyle(Paint.Style.FILL);

        for (Map.Entry<Long, List<Nota>> entry : notasPorTempo.entrySet()) {
            Long tempoDoGrupo = entry.getKey();
            List<Nota> grupo = entry.getValue();

            List<Float> xs = new ArrayList<>();
            List<Float> ysTopos = new ArrayList<>();
            for (Nota nota : grupo) {
                if (!nota.colcheia || !nota.visivel) continue;

                long rel = nota.tempoInicio - tempoAtual;
                float xCentro = getWidth()/2f
                        + rel * Espaçamento_nota
                        + (grupo.indexOf(nota) - (grupo.size() - 1)/2f)
                        * espacamentoEntreNotas;
                float yCentro = getYParaNota(nota.nome);

                // topo da haste estendido
                float yStemTop = yCentro - (alturaNota + alturaFeixe);
                xs.add(xCentro + raioNota - 2);
                ysTopos.add(yStemTop);
            }
            if (xs.size() < 2) continue;

            Collections.sort(xs);
            float xStart = xs.get(0);
            float xEnd   = xs.get(xs.size()-1);

            float yMinStemTop = Collections.min(ysTopos);
            // armazena o “bottom” do feixe (onde a haste deve tocar)
            float feixeBottom = yMinStemTop + alturaFeixe;
            beamBottomByTempo.put(tempoDoGrupo, feixeBottom);

            // desenha o feixe
            canvas.drawRect(
                    xStart,
                    yMinStemTop,
                    xEnd,
                    feixeBottom,
                    paintFeixe
            );
        }



        for (Nota nota : notas) {
            if (!nota.visivel) continue;

            long tempoRelativo = nota.tempoInicio - tempoAtual;

            // Verifica quantas notas existem neste tempo para deslocamento visual
            int indiceVisual = 0;
            int totalNoMesmoTempo = 1;
            List<Nota> grupoMesmoTempo = notasPorTempo.get(nota.tempoInicio);
            if (grupoMesmoTempo != null) {
                totalNoMesmoTempo = grupoMesmoTempo.size();
                indiceVisual = grupoMesmoTempo.indexOf(nota);
            }

            float x = getWidth() / 2 + tempoRelativo * Espaçamento_nota
                    + (indiceVisual - (totalNoMesmoTempo - 1) / 2f) * espacamentoEntreNotas;

            float y = getYParaNota(nota.nome);

            if (x > getWidth() || x < -100) continue;

            float largura = nota.duracao * VELOCIDADE_PIXELS_POR_MS;

            if (nota.tocando) {
                RectF fundo = new RectF(x, y - alturaNota / 2, x + largura, y + alturaNota / 2);
                canvas.drawRoundRect(fundo, 50, 50, paintFundoVerde);
                canvas.drawRoundRect(fundo, 50, 50, paintBordaVerde);
            }

            if (nota.nome.contains("#")) {
                Paint paintSustenido = new Paint();
                paintSustenido.setColor(Color.BLACK);
                paintSustenido.setTextSize(32f);
                paintSustenido.setFakeBoldText(true);
                canvas.drawText("♯", x - 35, y + 10, paintSustenido);
            }

            RectF corpo = new RectF(x - raioNota + 10, y - raioNota - 5, x + raioNota, y + raioNota - 34);
            canvas.drawOval(corpo, paintNota);
            canvas.drawOval(corpo, paintContorno);
            if (nota.colcheia) {
                Float yStemTop = beamBottomByTempo.get(nota.tempoInicio);
                if (yStemTop != null) {
                    // desenha haste do feixe até o corpo da nota
                    canvas.drawRect(
                            x + raioNota - 2,
                            yStemTop,
                            x + raioNota + 2,
                            y,
                            paintHaste
                    );
                }
            } else {
                // haste normal de p.ex. semicolcheia ou mínima
                canvas.drawRect(
                        x + raioNota - 2,
                        y - alturaNota,
                        x + raioNota + 2,
                        y,
                        paintHaste
                );
            }


        }

        postInvalidateDelayed(16);
    }


    private int getOffsetMeiaLinha(String nomeNota) {
        Map<String, Integer> posicoes = new HashMap<>();
        posicoes.put("E5", -3);
        posicoes.put("D#5", -3); posicoes.put("D5", -1);
        posicoes.put("C#5", -2); posicoes.put("C5", 0);
        posicoes.put("B4", 0);
        posicoes.put("A#4", 1); posicoes.put("A4", 3);
        posicoes.put("G#4", 2); posicoes.put("G4", 4);
        posicoes.put("F#4", 3); posicoes.put("F4", 5);
        posicoes.put("E4", 4);
        posicoes.put("D#4", 5); posicoes.put("D4", 7);
        posicoes.put("C#4", 6); posicoes.put("C4", 8);
        return posicoes.getOrDefault(nomeNota.toUpperCase(), 0);
    }

    private float getYParaNota(String nomeNota) {
        // Altura entre linhas (ex: 40px entre linhas → 20px por "meia linha")
        float espacamentoMeiaLinha = 20f;

        // Posição da linha do meio (B4)
        float yBase = getHeight() / 2;

        // Mapeia posições relativas em "meia linha" (0 = B4)
        Map<String, Integer> posicoes = new HashMap<>();

        posicoes.put("E5", 0);
        posicoes.put("D#5", 1);
        posicoes.put("D5", 1);
        posicoes.put("C#5", 2);
        posicoes.put("C5", 2);
        posicoes.put("B4", 3);
        posicoes.put("A#4", 4);
        posicoes.put("A4", 5);
        posicoes.put("G#4", 6);
        posicoes.put("G4", 6);
        posicoes.put("F#4", 7);
        posicoes.put("F4", 7);
        posicoes.put("E4", 8);
        posicoes.put("D#4", 9);
        posicoes.put("D4", 9);
        posicoes.put("C#4", 10);
        posicoes.put("C4", 10);

        // Verifica posição
        Integer offset = posicoes.get(nomeNota.toUpperCase());
        if (offset == null) return yBase; // default: B4 no centro

        return yBase + offset * espacamentoMeiaLinha;
    }
    public long getTempoInicial() {
        return tempoInicial != null ? tempoInicial : 0;
    }
}
