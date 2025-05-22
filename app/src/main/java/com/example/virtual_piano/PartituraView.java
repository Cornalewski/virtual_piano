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

    private static final float raioNota = 42f;
    private static final float alturaNota = 120f;

    // mapeamento de tempo → espaçamento “fixo” entre notas
    public static final float TIME_TO_PX = 0.35f;
    // velocidade real de deslocamento da partitura (rolagem)
    public static final float SCROLL_SPEED = 0.35f;
    public float espacamentoEntreNotas = 140f;


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

                tempoAtual = System.currentTimeMillis() - tempoInicial;

                // 1️⃣ posição “base” da nota no layout (fixa, sem scroll)
                float xBase = getWidth()/2f
                        + nota.tempoInicio * TIME_TO_PX;

                // 2️⃣ quanto tod0 o sistema já rolou (scroll)
                float deslocamento = tempoAtual * SCROLL_SPEED;

                // 3️⃣ offset para notas simultâneas (chords)
                float chordOffset = (grupo.indexOf(nota) - (grupo.size()-1)/2f) * espacamentoEntreNotas;

                // 4️⃣ posição final
                float x = xBase - deslocamento + chordOffset;

                float yCentro = getYParaNota(nota.nome);

                // topo da haste estendido
                float yStemTop = yCentro - (alturaNota + alturaFeixe);
                xs.add(x + raioNota - 2);
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

            float halfWidth = getWidth() / 2f;
            float xBase = halfWidth
                    + nota.tempoInicio * TIME_TO_PX;

            float deslocamento = tempoAtual * SCROLL_SPEED;

            float chordOffset = (indiceVisual
                    - (totalNoMesmoTempo - 1) / 2f
            ) * espacamentoEntreNotas;

            float x = xBase - deslocamento + chordOffset;

            float y = getYParaNota(nota.nome);

            if (x > getWidth() || x < -100) continue;

            if (nota.tocando) {
                xBase = halfWidth + nota.tempoInicio * TIME_TO_PX;
                deslocamento= tempoAtual * SCROLL_SPEED;
                chordOffset = (indiceVisual - (totalNoMesmoTempo-1)/2f) * espacamentoEntreNotas;
                x           = xBase - deslocamento + chordOffset;

                float x0 = x - raioNota + 9f;
                float x1 = x + raioNota + 100f;

                // 3️⃣ padding vertical e radius
                float y0 = y - alturaNota/2 - 20f;
                float y1 = y + alturaNota/2 + 20f;
                float corner = (y1 - y0)/2f;

                 // 4️⃣ desenho do fill crescendo pelo scroll
                xIndicador = halfWidth;
                float fillRight  = Math.min(x1, Math.max(x0, xIndicador));
                if (fillRight > x0) {
                    RectF fillPill = new RectF(x0 , y0 + 32, fillRight, y1-70);
                    canvas.drawRoundRect(fillPill, corner, corner, paintFundoVerde);
                }

                // 5️⃣ contorno completo
                RectF pill = new RectF(x0 , y0 +30 , x1, y1 - 70);
                canvas.drawRoundRect(pill, corner, corner, paintBordaVerde);
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
        Map<String, Float> posicoes = new HashMap<>();

        posicoes.put("E5", -0.8F);
        posicoes.put("D#5", 0.5F);
        posicoes.put("D5", 0.5F);
        posicoes.put("C#5", 2.0F);
        posicoes.put("C5", 2.0F);
        posicoes.put("B4", 3.0F);
        posicoes.put("A#4", 4.6F);
        posicoes.put("A4", 4.6F);
        posicoes.put("G#4", 5.8F);
        posicoes.put("G4", 5.8F);
        posicoes.put("F#4", 7.0F);
        posicoes.put("F4", 7.0F);
        posicoes.put("E4", 8.0F);
        posicoes.put("D#4", 9.0F);
        posicoes.put("D4", 9.0F);
        posicoes.put("C#4", 10.0F);
        posicoes.put("C4", 10.0F);

        // Verifica posição
        Float offset = posicoes.get(nomeNota.toUpperCase());
        if (offset == null) return yBase; // default: B4 no centro

        return yBase + offset * espacamentoMeiaLinha;
    }
    public long getTempoInicial() {
        return tempoInicial != null ? tempoInicial : 0;
    }
}
