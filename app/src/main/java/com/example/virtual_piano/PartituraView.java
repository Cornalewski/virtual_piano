package com.example.virtual_piano;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PartituraView extends View {

    private List<Nota> notasVisiveis;
    private Map<Long, List<Nota>> grupoPorTempo;
    private Long tempoInicial = null;

    private Paint paintFundo;
    private Shader shaderFundo;

    private Paint paintNota, paintContorno, paintFeixe, paintHaste, paintTexto, paintDestaque;

    // Variáveis dimensionais recalculadas em onSizeChanged()
    private float raioNota;
    private float alturaHaste;
    public float espacamentoEntreNotas;
    private float spacingLine;
    private boolean emPausa = false;


    // Constantes de conversão de tempo
    public float TIME_TO_PX;
    //devem ser iguais
    public float SCROLL_SPEED;

    public PartituraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public float getTIME_TO_PX() {
        return TIME_TO_PX;
    }

    public float getSCROLL_SPEED() {
        return SCROLL_SPEED;
    }

    private void initPaints() {
        paintFundo = new Paint();

        paintNota = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNota.setColor(Color.WHITE);
        paintNota.setStyle(Paint.Style.FILL);

        paintContorno = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintContorno.setColor(Color.BLACK);
        paintContorno.setStyle(Paint.Style.STROKE);

        paintFeixe = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintFeixe.setColor(Color.BLACK);

        paintHaste = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHaste.setColor(Color.BLACK);

        paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTexto.setColor(Color.BLACK);

        paintDestaque = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDestaque.setColor(Color.parseColor("#34C759"));
        paintDestaque.setStyle(Paint.Style.FILL);
    }
    public void pausarAnimacao() {
        emPausa = true;
    }

    public void retomarAnimacao() {
        emPausa = false;
        invalidate(); // força redesenho imediato
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 1) Espaçamento entre linhas do pentagrama: 10% da altura
        spacingLine = h * 0.10f;

        // 2) Raio da cabeça da nota: quase um espaçamento
        raioNota = spacingLine * 0.6f;

        // 3) Altura da haste: 3× espaçamento
        alturaHaste = spacingLine * 3f;

        // 4) Espaçamento lateral entre notas simultâneas: 30% da largura
        espacamentoEntreNotas = spacingLine * 3f;
        //tamanho da fonte do texto
        float textSize = spacingLine * 0.8f;
        // Se quiser maior, use 1.0f; menor, 0.6f; etc.
        paintTexto.setTextSize(textSize);

        float janelaMs = 5_000f;
        TIME_TO_PX  = w / janelaMs;          // px por ms
        SCROLL_SPEED = TIME_TO_PX;


        // 5) Espessuras de traço proporcionais
        paintContorno.setStrokeWidth(spacingLine * 0.07f);
        paintFeixe.setStrokeWidth(spacingLine * 0.2f);
        paintHaste.setStrokeWidth(spacingLine * 0.07f);

        // 6) Tamanho do texto proporcional
        textSize = spacingLine * 0.6f;
        paintTexto.setTextSize(textSize);

        // 7) Gradiente de fundo do tamanho atual
        shaderFundo = new LinearGradient(
                0, 0, 0, h,
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#F0F0F0"),
                Shader.TileMode.CLAMP
        );
        paintFundo.setShader(shaderFundo);
    }

    public void setNotas(List<Nota> todasNotas) {
        notasVisiveis = new ArrayList<>();
        for (Nota n : todasNotas) if (n.visivel) notasVisiveis.add(n);
        Collections.sort(notasVisiveis, (a, b) -> Long.compare(a.tempoInicio, b.tempoInicio));
        grupoPorTempo = new HashMap<>();
        for (Nota n : notasVisiveis) {
            grupoPorTempo.computeIfAbsent(n.tempoInicio, k -> new ArrayList<>()).add(n);
        }
        invalidate();
    }

    public void iniciarPartitura() {
        tempoInicial = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (notasVisiveis == null || notasVisiveis.isEmpty()) return;

        long tempoAtual = (tempoInicial != null)
                ? (System.currentTimeMillis() - tempoInicial)
                : 0;

        float largura = getWidth();
        float altura = getHeight();
        float meioW = largura / 2f;
        float buffer = raioNota * 2;

        // 1) Fundo
        canvas.drawRect(0, 0, largura, altura, paintFundo);

        // 2) Pentagrama: 5 linhas igualmente espaçadas
        Paint pentagrama = new Paint(Paint.ANTI_ALIAS_FLAG);
        pentagrama.setColor(Color.LTGRAY);
        pentagrama.setStrokeWidth(spacingLine * 0.05f);
        float linhaBaseY = altura / 2f - 2 * spacingLine;
        for (int i = 0; i < 5; i++) {
            float y = linhaBaseY + i * spacingLine;
            canvas.drawLine(0, y, largura, y, pentagrama);
        }

        // 3) Indicador de tempo (linha central verde)
        Paint linhaTempo = new Paint(Paint.ANTI_ALIAS_FLAG);
        linhaTempo.setColor(Color.GREEN);
        linhaTempo.setStrokeWidth(spacingLine * 0.12f);
        float xIndicador = meioW;
        canvas.drawLine(xIndicador, 0, xIndicador, altura, linhaTempo);

        // 4) Deslocamento e janela visível em tempo
        float desloc = tempoAtual * SCROLL_SPEED;
        float tMin = ((-buffer) + desloc - meioW) / TIME_TO_PX;
        float tMax = ((largura + buffer) + desloc - meioW) / TIME_TO_PX;


        // 6) Desenha cada nota
        for (Nota nota : notasVisiveis) {
            if (nota.tempoInicio < tMin || nota.tempoInicio > tMax) continue;
            float xBase = meioW + nota.tempoInicio * TIME_TO_PX;
            List<Nota> grupo = grupoPorTempo.get(nota.tempoInicio);
            int idx = grupo.indexOf(nota), tot = grupo.size();
            float chordOff = (idx - (tot - 1) / 2f) * espacamentoEntreNotas;
            float x = xBase - desloc + chordOff;
            float y = getYParaNota(nota.nome);

            // destaque se tocando
            if (nota.tocando) {
                RectF r = new RectF(
                        x - raioNota * 1.4f, y - raioNota *1.1f,
                        x + raioNota * 4f, y + raioNota *1.1f
                );
                canvas.drawRoundRect(r, raioNota, raioNota, paintDestaque);
            }

            // corpo oval
            RectF corpo = new RectF(
                    x - raioNota * 1.3f, y - raioNota,
                    x + raioNota, y + raioNota
            );
            canvas.drawOval(corpo, paintNota);
            canvas.drawOval(corpo, paintContorno);

            // haste vertical
            canvas.drawRect(
                    x + raioNota * 0.88f,
                    y - alturaHaste,
                    x + raioNota * 1.08f,
                    y,
                    paintHaste
            );

            // texto (ex.: dó, ré, mi…)
            String txt = getTextoNota(nota.nome);
            canvas.drawText(
                    txt,
                    x + raioNota + spacingLine * 0.3f,
                    y + spacingLine * 1.3f,
                    paintTexto
            );
        }
        // 5) Feixes para colcheias SIMULTÂNEAS (mesmo tempo)
        for (Map.Entry<Long, List<Nota>> entry : grupoPorTempo.entrySet()) {
            long t = entry.getKey();
            if (t < tMin || t > tMax) continue;

            // Filtra apenas as colcheias desse instante
            List<Nota> colcheiasNoAcorde = new ArrayList<>();
            for (Nota n : entry.getValue()) {
                if (n.colcheia) colcheiasNoAcorde.add(n);
            }
            if (colcheiasNoAcorde.size() < 2) continue;

            // Coleta Xs e determina o topo das hastes
            List<Float> xs = new ArrayList<>();
            float topoHaste = Float.MAX_VALUE;
            for (Nota c : colcheiasNoAcorde) {
                // posição X exata da cabeça
                float xBase = meioW + c.tempoInicio * TIME_TO_PX;
                int idx = entry.getValue().indexOf(c);
                int tot = entry.getValue().size();
                float chordOff = (idx - (tot - 1) / 2f) * espacamentoEntreNotas;
                float x = xBase - desloc + chordOff;
                xs.add(x + raioNota * 0.9f);

                // topo da haste (sem offset extra)
                float yStemTop = getYParaNota(c.nome) - alturaHaste;
                if (yStemTop < topoHaste) topoHaste = yStemTop;
            }
            Collections.sort(xs);
            float x0 = xs.get(0);
            float x1 = xs.get(xs.size() - 1);

            // define espessura do feixe (ajuste fino: 15% do espaçamento entre linhas)
            float beamThickness = spacingLine * 0.15f;

            // desenha o feixe logo abaixo de topoHaste (para não encostar nas cabeças)
            canvas.drawRect(
                    x0,
                    topoHaste + spacingLine * 0.06f,
                    x1,
                    topoHaste + spacingLine * 0.06f + beamThickness,
                    paintFeixe
            );
        }

        // 7) Animação
        if (tempoInicial != null && !emPausa) {
            postInvalidateDelayed(16);
        }

    }

    private float getYParaNota(String nota) {
        // mesmo mapeamento que antes, agora com spacingLine
        Map<String, Float> pos = new HashMap<>();
        pos.put("C4",  3f);  pos.put("D4",  2.5f); pos.put("E4",  2f);
        pos.put("F4",  1.5f);  pos.put("G4",  1f); pos.put("A4", 0.5f);
        pos.put("B4", 0f);  pos.put("C5", -0.5f); pos.put("D5", -1f);
        pos.put("E5", -1.5f);
        Float off = pos.get(nota.toUpperCase());
        return (getHeight() / 2f) + (off != null ? off * spacingLine : 0f);
    }

    private String getTextoNota(String nome) {
        switch (nome) {
            case "C4":  return "Dó";
            case "C#4": return "Dó#";
            case "D4":  return "Ré";
            case "D#4": return "Ré#";
            case "E4":  return "Mi";
            case "F4":  return "Fá";
            case "F#4": return "Fá#";
            case "G4":  return "Sol";
            case "G#4": return "Sol#";
            case "A4":  return "Lá";
            case "A#4": return "Lá#";
            case "B4":  return "Si";
            case "C5":  return "Dó ↑";
            case "C#5": return "Dó# ↑";
            case "D5":  return "Ré ↑";
            case "D#5": return "Ré# ↑";
            case "E5":  return "Mi ↑";
            default:    return "";
        }
    }

    public long getTempoInicial() {
        return (tempoInicial != null) ? tempoInicial : 0L;
    }
}
