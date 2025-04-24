package com.example.virtual_piano;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class PartituraView extends View {

    private List<Nota> notas;
    private Paint paintNota;
    private Paint paintLinha;
    private Paint paintDestaque;
    private long tempoInicial;

    private static final float VELOCIDADE_PIXELS_POR_MS = 0.2f;
    private static final float raioNota = 15f;
    private static final float alturaNota = 30f;

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

        tempoInicial = System.currentTimeMillis();
    }

    public void setNotas(List<Nota> notas) {
        this.notas = notas;
        tempoInicial = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (notas == null) return;

        long tempoAtual = System.currentTimeMillis() - tempoInicial;

        // Desenha as 5 linhas do pentagrama
        int alturaLinha = 40;
        int espacamento = 30;
        for (int i = 0; i < 5; i++) {
            int y = alturaLinha + i * espacamento;
            canvas.drawLine(0, y, getWidth(), y, paintLinha);
        }

        for (Nota nota : notas) {
            if (!nota.visivel) continue;

            long tempoRelativo = nota.tempoInicio - tempoAtual;
            float x = getWidth() / 2 + tempoRelativo * VELOCIDADE_PIXELS_POR_MS;
            float y = getYParaNota(nota.nome);

            if (x + nota.duracao * VELOCIDADE_PIXELS_POR_MS < 0 || x > getWidth()) continue;

            // 🎯 Desenha o retângulo de destaque se tiver duração
            if (nota.duracao > 0) {
                float largura = nota.duracao * VELOCIDADE_PIXELS_POR_MS;
                RectF retangulo = new RectF(x, y - alturaNota / 2, x + largura, y + alturaNota / 2);
                canvas.drawRoundRect(retangulo, 10, 10, paintDestaque);
            }

            // 🎶 Desenha a nota por cima
            canvas.drawCircle(x, y, raioNota, paintNota);
        }

        postInvalidateDelayed(16); // Redesenha a cada ~60 FPS
    }

    private float getYParaNota(String nota) {
        String[] notasVisiveis = {
                "E5", "D#5", "D5", "C#5", "C5", "B4", "A#4", "A4",
                "G#4", "G4", "F#4", "F4", "E4", "D#4", "D4", "C#4", "C4"
        };

        for (int i = 0; i < notasVisiveis.length; i++) {
            if (notasVisiveis[i].equals(nota)) {
                return 40 + i * 8;
            }
        }

        return 0;
    }
}
