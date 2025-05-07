package com.example.virtual_piano;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;
import java.util.List;

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
    
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (tempoInicial == null || notas == null) return;

        long tempoAtual = System.currentTimeMillis() - tempoInicial;

        // 🔳 Fundo branco
        canvas.drawColor(Color.parseColor("#FDFDFD"));

        // 🎼 Pentagrama
        Paint paintLinha = new Paint();
        paintLinha.setColor(Color.parseColor("#CCCCCC"));
        paintLinha.setStrokeWidth(2);

        float linhaBaseY = getHeight() / 2 - 2 * 30; // centralizar
        for (int i = 0; i < 5; i++) {
            float y = linhaBaseY + i * 50;
            canvas.drawLine(0, y, getWidth(), y, paintLinha);
        }

        // 🎶 Paints
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

        // Linha vertical indicadora de tempo (no centro da tela)
        Paint linhaTempoPaint = new Paint();
        linhaTempoPaint.setColor(Color.GREEN);
        linhaTempoPaint.setStrokeWidth(6);

        float xIndicador = getWidth() / 2f; // ou getWidth()/2f se quiser centralizado
        canvas.drawLine(xIndicador, 0, xIndicador, getHeight(), linhaTempoPaint);


        for (Nota nota : notas) {
            if (!nota.visivel) continue;

            long tempoRelativo = nota.tempoInicio - tempoAtual;
            float x = getWidth() / 2 + tempoRelativo * Espaçamento_nota;
            float y = getYParaNota(nota.nome);

            if (x > getWidth() || x < -100) continue;

            float largura = nota.duracao * VELOCIDADE_PIXELS_POR_MS;

            // ✅ Destaque se tocando
            if (nota.tocando) {
                RectF fundo = new RectF(x, y - alturaNota / 2, x + largura, y + alturaNota / 2);
                canvas.drawRoundRect(fundo, 20, 20, paintFundoVerde);
                canvas.drawRoundRect(fundo, 20, 20, paintBordaVerde);
            }

            // 🎶 Nota com haste e contorno
            RectF corpo = new RectF(x - raioNota, y - raioNota -10, x + raioNota, y + raioNota -34);
            canvas.drawOval(corpo, paintNota);
            canvas.drawOval(corpo, paintContorno);
            canvas.drawRect(x + raioNota - 2, y - alturaNota, x + raioNota + 2, y, paintHaste);
        }

        postInvalidateDelayed(16);
    }



    private float getYParaNota(String nomeNota) {
        // Define a ordem das notas
        String[] ordem = {"C", "D", "E", "F", "G", "A", "B"};

        // Extrai nome e oitava
        String letra = nomeNota.substring(0, 1);
        int oitava = Integer.parseInt(nomeNota.substring(1));

        // Calcula posição relativa ao B4 (linha do meio)
        int distanciaSemitons = (oitava - 4) * 7 + (Arrays.asList(ordem).indexOf(letra) - Arrays.asList(ordem).indexOf("B"));
        float espacamentoMeiaLinha = 15f; // altura entre linha e espaço

        // Linha do meio (B4) no centro vertical
        float yCentro = getHeight() / 2;

        return yCentro + distanciaSemitons * (-espacamentoMeiaLinha);
    }


    public long getTempoInicial() {
        return tempoInicial != null ? tempoInicial : 0;
    }
}
