package com.example.virtual_piano;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PartituraView extends View {

    private List<Nota> notas;
    private Paint paintNota;
    private Paint paintLinha;
    private Paint paintDestaque;
    private Long tempoInicial = null;

    private static final float raioNota = 42f;
    private static final float alturaNota = 120f;

    // mapeamento de tempo → espaçamento “fixo” entre notas
    public static final float TIME_TO_PX = 0.45f;
    // velocidade real de deslocamento da partitura (rolagem)
    public static final float SCROLL_SPEED = 0.35f;
    public float espacamentoEntreNotas = 160f;

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



    }

    public void setNotas(List<Nota> notas) {
        this.notas = notas;
        invalidate();
    }

    public void iniciarPartitura() {
        this.tempoInicial = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paintFundoVerde = new Paint();
        paintFundoVerde.setColor(Color.parseColor("#5534C759"));

        Paint paintBordaVerde = new Paint();
        paintBordaVerde.setColor(Color.parseColor("#34C759"));
        paintBordaVerde.setStyle(Paint.Style.STROKE);
        paintBordaVerde.setStrokeWidth(4);

        super.onDraw(canvas);
        if (notas == null) {
            return;
        }

        long tempoAtual = (tempoInicial != null)
                ? System.currentTimeMillis() - tempoInicial
                : 0;

        // Fundo e pentagrama
        canvas.drawColor(Color.parseColor("#FDFDFD"));
        Paint pentagramaPaint = new Paint();
        pentagramaPaint.setColor(Color.parseColor("#CCCCCC"));
        pentagramaPaint.setStrokeWidth(2);
        float linhaBaseY = getHeight() / 2 - 60;
        for (int i = 0; i < 5; i++) {
            float y = linhaBaseY + i * 50;
            canvas.drawLine(0, y, getWidth(), y, pentagramaPaint);
        }

        // Indicador de tempo
        Paint linhaTempoPaint = new Paint();
        linhaTempoPaint.setColor(Color.GREEN);
        linhaTempoPaint.setStrokeWidth(6);
        float xIndicador = getWidth() / 2f;
        canvas.drawLine(xIndicador, 0, xIndicador, getHeight(), linhaTempoPaint);

        // Agrupa notas visíveis por tempo
        Map<Long, List<Nota>> notasPorTempo = new HashMap<>();
        for (Nota nota : notas) {
            if (!nota.visivel) continue;
            notasPorTempo.computeIfAbsent(nota.tempoInicio, k -> new ArrayList<>()).add(nota);
        }

        // Desenha beams (feixes) para colcheias
        Paint paintFeixe = new Paint();
        paintFeixe.setColor(Color.BLACK);
        for (Map.Entry<Long, List<Nota>> entry : notasPorTempo.entrySet()) {
            List<Nota> grupo = entry.getValue();
            List<Float> xs = new ArrayList<>();
            List<Float> ysTop = new ArrayList<>();
            for (Nota nota : grupo) {
                if (!nota.colcheia) continue;
                float xBase = getWidth() / 2f + nota.tempoInicio * TIME_TO_PX;
                float desloc = tempoAtual * SCROLL_SPEED;
                float chordOffset = (grupo.indexOf(nota) - (grupo.size() - 1) / 2f) * espacamentoEntreNotas;
                float x = xBase - desloc + chordOffset;
                float yCentro = getYParaNota(nota.nome);
                float yStemTop = yCentro - (alturaNota + 8f);
                xs.add(x + raioNota - 2);
                ysTop.add(yStemTop);
            }
            if (xs.size() < 2) continue;
            Collections.sort(xs);
            float xStart = xs.get(0);
            float xEnd = xs.get(xs.size() - 1);
            float minTop = Collections.min(ysTop);
            float bottom = minTop + 8f;
            canvas.drawRect(xStart, minTop, xEnd, bottom, paintFeixe);
        }

        // Desenha cada nota visível
        for (Nota nota : notas) {
            if (!nota.visivel) continue;
            float halfWidth = getWidth() / 2f;
            float xBase = halfWidth + nota.tempoInicio * TIME_TO_PX;
            float desloc = tempoAtual * SCROLL_SPEED;
            List<Nota> grupo = notasPorTempo.get(nota.tempoInicio);
            int idx = (grupo != null) ? grupo.indexOf(nota) : 0;
            int total = (grupo != null) ? grupo.size() : 1;
            float chordOffset = (idx - (total - 1) / 2f) * espacamentoEntreNotas;
            float x = xBase - desloc + chordOffset;
            if (x < -100 || x > getWidth()) continue;
            float y = getYParaNota(nota.nome);

            // Destaque se tocando
            if (nota.tocando) {
                float x0 = x - raioNota + 9f;
                float x1 = x + raioNota + 100f;
                float y0 = y - alturaNota / 2 - 20f;
                float y1 = y + alturaNota / 2 + 20f;
                float corner = (y1 - y0) / 2f;
                float fillRight = Math.min(x1, Math.max(x0, xIndicador));
                if (fillRight > x0) {
                    RectF fillRect = new RectF(x0, y0 + 32, fillRight, y1 - 70);
                    canvas.drawRoundRect(fillRect, corner, corner, paintFundoVerde);
                }
                RectF pill = new RectF(x0, y0 + 30, x1, y1 - 70);
                Paint borda = new Paint();
                borda.setColor(Color.parseColor("#34C759"));
                borda.setStyle(Paint.Style.STROKE);
                borda.setStrokeWidth(4);
                canvas.drawRoundRect(pill, corner, corner, paintBordaVerde);
            }

            // Desenha sustenido
            if (nota.nome.contains("#")) {
                Paint p = new Paint();
                p.setColor(Color.BLACK);
                p.setTextSize(32f);
                p.setFakeBoldText(true);
                canvas.drawText("♯", x - 35, y + 10, p);
            }

            // Desenha o corpo da nota
            RectF corpo = new RectF(x - raioNota + 10, y - raioNota - 5,
                    x + raioNota, y + raioNota - 34);
            canvas.drawOval(corpo, paintNota);
            Paint pContorno = new Paint();
            pContorno.setColor(Color.BLACK);
            pContorno.setStyle(Paint.Style.STROKE);
            pContorno.setStrokeWidth(3);
            canvas.drawOval(corpo, pContorno);

            // Desenha haste para todas as notas (incluindo colcheias)
            Paint haste = new Paint();
            haste.setColor(Color.BLACK);
            canvas.drawRect(x + raioNota - 2, y - alturaNota,
                    x + raioNota + 2, y, haste);
        }

        // Continua animação só se já iniciado
        if (tempoInicial != null) {
            postInvalidateDelayed(16);
        }
    }

    private float getYParaNota(String nota) {
        float espac = 20f;
        float yBase = getHeight() / 2;
        Map<String, Float> pos = new HashMap<>();
        pos.put("E5", -0.8f); pos.put("D#5", 0.5f); pos.put("D5", 0.5f);
        pos.put("C#5", 2f); pos.put("C5", 2f); pos.put("B4", 3f);
        pos.put("A#4", 4.6f); pos.put("A4", 4.6f); pos.put("G#4", 5.8f);
        pos.put("G4", 5.8f); pos.put("F#4", 7f); pos.put("F4", 7f);
        pos.put("E4", 8f); pos.put("D#4", 9f); pos.put("D4", 9f);
        pos.put("C#4", 10f);
        pos.put("C4", 10f);
        Float offset = pos.get(nota.toUpperCase());
        return yBase + ((offset != null) ? offset : 0f) * espac;
    }

    public long getTempoInicial() {
        return (tempoInicial != null) ? tempoInicial : 0;
    }
}
