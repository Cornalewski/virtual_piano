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

    // Lista ordenada das notas que serão desenhadas (apenas visíveis)
    private List<Nota> notasVisiveis;
    // Agrupamento por tempoInicio → lista de notas naquele instante (para feixes e acordes)
    private Map<Long, List<Nota>> grupoPorTempo;
    // Marca o início da animação (quando o usuário toca a primeira nota)
    private Long tempoInicial = null;

    // Paints únicos, criados em init()
    private Paint paintNota, paintContorno, paintFeixe, paintHaste,paintTexto,
            paintSustenido, paintDestaque;
    // Raio (meio eixo) para desenhar o oval da cabeça da nota
    private static final float raioNota = 42f;
    // Altura para posicionar a haste vertical
    private static final float alturaNota = 120f;
    // Conversão de tempo (ms) para pixels na horizontal
    public static final float TIME_TO_PX = 0.35f;
    // Velocidade de rolagem da partitura (pixels por ms)
    public static final float SCROLL_SPEED = 0.35f;
    // Espaçamento lateral entre notas simultâneas (acordes)
    public float espacamentoEntreNotas = 140f;

    public PartituraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Paint para o corpo (oval) da nota
        paintNota = new Paint();
        paintNota.setColor(Color.WHITE);
        paintNota.setStyle(Paint.Style.FILL);

        // Contorno do corpo da nota
        paintContorno = new Paint();
        paintContorno.setColor(Color.BLACK);
        paintContorno.setStyle(Paint.Style.STROKE);
        paintContorno.setStrokeWidth(3);

        // Feixes (beams) que ligam colcheias
        paintFeixe = new Paint();
        paintFeixe.setColor(Color.BLACK);

        // Haste vertical das notas (todo tipo: colcheia ou não)
        paintHaste = new Paint();
        paintHaste.setColor(Color.BLACK);

        // Texto de sustenido (“♯”)
        paintSustenido = new Paint();
        paintSustenido.setColor(Color.BLACK);
        paintSustenido.setTextSize(32f);
        paintSustenido.setFakeBoldText(true);

        paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTexto.setColor(Color.BLACK);
        paintTexto.setTextSize(32f);
        paintTexto.setFakeBoldText(true);

        // Destaque (rosa transparente) para nota acertada
        paintDestaque = new Paint();
        paintDestaque.setColor(Color.parseColor("#34C759"));
        paintDestaque.setStyle(Paint.Style.FILL);
    }

    /**
     * Recebe a lista completa de notas e monta:
     * 1) notasVisiveis: apenas as notas que devem aparecer (visivel == true)
     * 2) grupoPorTempo: mapeia cada tempoInicio → lista de notas (para feixes e acordes)
     */
    public void setNotas(List<Nota> todasNotas) {
        // 1) Filtra só notas visíveis
        notasVisiveis = new ArrayList<>();
        for (Nota n : todasNotas) {
            if (n.visivel) {
                notasVisiveis.add(n);
            }
        }
        // 2) Ordena por tempoInicio para permitir culling via busca binária
        Collections.sort(notasVisiveis, (a, b) -> Long.compare(a.tempoInicio, b.tempoInicio));

        // 3) Agrupa por tempoInicio
        grupoPorTempo = new HashMap<>();
        for (Nota n : notasVisiveis) {
            long t = n.tempoInicio;
            if (!grupoPorTempo.containsKey(t)) {
                grupoPorTempo.put(t, new ArrayList<>());
            }
            grupoPorTempo.get(t).add(n);
        }

        // 4) Força desenho estático (tempoAtual = 0) assim que as notas forem definidas
        invalidate();
    }

    /**
     * Marca o timestamp de início da partitura e dispara o invalidate
     * para começar a animação.
     */
    public void iniciarPartitura() {
        tempoInicial = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Se não há notas visíveis, nada a desenhar
        if (notasVisiveis == null || notasVisiveis.isEmpty()) {
            return;
        }

        // 1) Calcula quanto tempo já passou desde o início da partitura
        long tempoAtual = (tempoInicial != null)
                ? (System.currentTimeMillis() - tempoInicial)
                : 0;

        float larguraTela = getWidth();
        float alturaTela = getHeight();
        float meioW = larguraTela / 2f;
        // Buffer horizontal extra para desenhar notas “próximas à borda”
        float buffer = raioNota * 2;

        // 2) Desenha fundo e pentagrama
        canvas.drawColor(Color.parseColor("#FDFDFD"));
        Paint pentagramaPaint = new Paint();
        pentagramaPaint.setColor(Color.parseColor("#CCCCCC"));
        pentagramaPaint.setStrokeWidth(2);
        float linhaBaseY = alturaTela / 2 - 60;
        for (int i = 0; i < 5; i++) {
            float y = linhaBaseY + i * 50;
            canvas.drawLine(0, y, larguraTela, y, pentagramaPaint);
        }

        // 3) Desenha a linha indicadora de tempo (verde no centro)
        Paint linhaTempoPaint = new Paint();
        linhaTempoPaint.setColor(Color.GREEN);
        linhaTempoPaint.setStrokeWidth(6);
        float xIndicador = meioW;
        canvas.drawLine(xIndicador, 0, xIndicador, alturaTela, linhaTempoPaint);

        // 4) Calcula deslocamento e janela de tempo visível [minVisibleTime .. maxVisibleTime]
        //    desloc = quantos pixels já rolou (se início não aconteceu, desloc = 0)
        float desloc = tempoAtual * SCROLL_SPEED;

        // Para cada nota de tempo t: xBase = meioW + t * TIME_TO_PX
        // Ela está visível se (xBase - desloc) ∈ [-buffer .. larguraTela + buffer]

        // 4.1) tMin  = ((-buffer) + desloc - meioW) / TIME_TO_PX
        float minVisibleTime = ((-buffer) + desloc - meioW) / TIME_TO_PX;
        // 4.2) tMax = ((larguraTela + buffer) + desloc - meioW) / TIME_TO_PX
        float maxVisibleTime = ((larguraTela + buffer) + desloc - meioW) / TIME_TO_PX;

        // 5) Usa busca binária em notasVisiveis (ordenadas por tempoInicio) para achar índices
        int idxStart = indexOfFirstNotaMaiorOuIgualA(minVisibleTime);
        int idxEnd   = indexOfLastNotaMenorOuIgualA(maxVisibleTime);
        if (idxStart < 0) idxStart = 0;
        if (idxEnd < 0) idxEnd = 0;
        if (idxEnd >= notasVisiveis.size()) idxEnd = notasVisiveis.size() - 1;

        // 6) Desenha os feixes (beams) para colcheias dentro da janela visível
        //    Percorre cada grupo de tempo t, mas só se t ∈ [minVisibleTime .. maxVisibleTime]
        for (Map.Entry<Long, List<Nota>> entry : grupoPorTempo.entrySet()) {
            long t = entry.getKey();
            if (t < minVisibleTime || t > maxVisibleTime) continue;

            List<Nota> grupo = entry.getValue();
            // Coleta apenas as colcheias do grupo
            List<Float> xs = new ArrayList<>();
            List<Float> ysTop = new ArrayList<>();

            for (Nota n : grupo) {
                if (!n.colcheia) continue;

                // xBase = meioW + n.tempoInicio * TIME_TO_PX
                float xBase = meioW + n.tempoInicio * TIME_TO_PX;
                // efetivo: x = xBase - desloc + chordOffset (para acordes)
                int idxNota = grupo.indexOf(n);
                int totalNoGrupo = grupo.size();
                float chordOffset = (idxNota - (totalNoGrupo - 1) / 2f) * espacamentoEntreNotas;
                float x = xBase - desloc + chordOffset;

                // yCentro base para nota
                float yCentro = getYParaNota(n.nome);
                // A haste “sobe” altitudeNota unidades acima do centro
                float yStemTop = yCentro - (alturaNota + 8f);

                xs.add(x + raioNota - 2);
                ysTop.add(yStemTop);
            }
            // Se houver pelo menos 2 colcheias nesse mesmo tempo, desenha um feixe
            if (xs.size() >= 2) {
                Collections.sort(xs);
                float xStart = xs.get(0);
                float xEnd = xs.get(xs.size() - 1);
                float minTop = Collections.min(ysTop);
                float bottom = minTop + 8f;
                canvas.drawRect(xStart, minTop, xEnd, bottom, paintFeixe);
            }
        }

        // 7) Desenha cada nota visível dentro do intervalo [idxStart .. idxEnd]
        for (int i = idxStart; i <= idxEnd; i++) {
            Nota nota = notasVisiveis.get(i);

            // xBase = meioW + nota.tempoInicio * TIME_TO_PX
            float xBase = meioW + nota.tempoInicio * TIME_TO_PX;
            // chordOffset para notas simultâneas (acorde) no mesmo tempo
            List<Nota> grupo = grupoPorTempo.get(nota.tempoInicio);
            int idxNota = (grupo != null) ? grupo.indexOf(nota) : 0;
            int totalNoGrupo = (grupo != null) ? grupo.size() : 1;
            float chordOffset = (idxNota - (totalNoGrupo - 1) / 2f) * espacamentoEntreNotas;

            // posição final X, descontando desloc para animar
            float x = xBase - desloc + chordOffset;

            // Se mesmo assim estiver fora da tela (apenas safety check), continua
            if (x + raioNota < 0 || x - raioNota > larguraTela) continue;

            float y = getYParaNota(nota.nome);

            // 7.1) Se a nota está “tocando” (nota.tocando == true), desenha destaque parcial + contorno
            if (nota.tocando) {
                float x0 = x - raioNota + 9f;
                float x1 = x + raioNota + 100f;
                float y0 = y - alturaNota / 2 - 20f;
                float y1 = y + alturaNota / 2 + 20f;
                float corner = (y1 - y0) / 2f;
                // Calcula quanto preencher: do x0 até o indicador central
                float fillRight = Math.min(x1, Math.max(x0, xIndicador));
                if (fillRight > x0) {
                    RectF fillRect = new RectF(x0, y0 + 32, fillRight, y1 - 70);
                    canvas.drawRoundRect(fillRect, corner, corner, paintDestaque);
                }
                // Desenha borda oval de destaque (verde)
                RectF pill = new RectF(x0, y0 + 30, x1, y1 - 70);
                Paint borda = new Paint();
                borda.setColor(Color.parseColor("#34C759"));
                borda.setStyle(Paint.Style.STROKE);
                borda.setStrokeWidth(4);
                canvas.drawRoundRect(pill, corner, corner, borda);
            }

            // 7.2) Desenha sustenido “♯” se o nome conter “#”
            if (nota.nome.contains("#")) {
                canvas.drawText("♯", x - 35, y + 10, paintSustenido);
            }

            // 7.3) Desenha o corpo oval da nota
            RectF corpo = new RectF(
                    x - raioNota + 10,
                    y - raioNota - 5,
                    x + raioNota,
                    y + raioNota - 34
            );
            canvas.drawOval(corpo, paintNota);
            canvas.drawOval(corpo, paintContorno);

            // 7.x) Desenha o nome da nota ao lado
            String Texto = "";
            switch (nota.nome) {
                case "C4":
                    Texto = "dó";
                    break;
                case "C4sharp":
                    Texto = "dó#";
                    break;
                case "D4":
                    Texto = "Ré";
                    break;
                case "D4sharp":
                    Texto = "Ré#";
                    break;
                case "E4":
                    Texto = "Mi";
                    break;
                case "F4":
                    Texto = "Fá";
                    break;
                case "F4sharp":
                    Texto = "Fá#";
                    break;
                case "G4":
                    Texto = "Sol";
                    break;
                case "G4sharp":
                    Texto = "Sol#";
                    break;
                case "A4":
                    Texto = "La";
                    break;
                case "A4sharp":
                    Texto = "La#";
                    break;
                case "B4":
                    Texto = "Si";
                    break;
                case "C5":
                    Texto = "Dó ↑";
                    break;

                case "C5sharp":
                    Texto = "dó# ↑";
                    break;
                case "D5":
                    Texto = "Ré ↑";
                    break;
                case "D5sharp":
                    Texto = "Ré# ↑";
                    break;
                case "E5":
                    Texto = "Mi ↑";
                    break;
            }
            float xTexto = x + raioNota + 16;       // posicione um pouco à direita da cabeça
            float yTexto = y + (paintTexto.getTextSize() / 2) + 23; // centraliza verticalmente
            canvas.drawText(Texto, xTexto, yTexto, paintTexto);


            // 7.4) Desenha a haste vertical para **todas** as notas
            canvas.drawRect(
                    x + raioNota - 2,
                    y - alturaNota,
                    x + raioNota + 2,
                    y,
                    paintHaste
            );
        }

        // 8) Se a animação já começou (tempoInicial != null), agenda nova frame (≈60 FPS)
        if (tempoInicial != null) {
            postInvalidateDelayed(16);
        }
    }

    /**
     * Busca binária na lista notasVisiveis (ordenada por tempoInicio) para
     * retornar o índice da primeira nota que tenha tempoInicio >= tempoLimite.
     * Se não encontrar nenhuma, retorna -1.
     */
    private int indexOfFirstNotaMaiorOuIgualA(float tempoLimite) {
        int lo = 0, hi = notasVisiveis.size() - 1;
        int ans = -1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (notasVisiveis.get(mid).tempoInicio >= tempoLimite) {
                ans = mid;
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return ans;
    }

    /**
     * Busca binária na lista notasVisiveis (ordenada) para encontrar o índice
     * da última nota cujo tempoInicio <= tempoLimite.
     * Se não encontrar nenhuma, retorna -1.
     */
    private int indexOfLastNotaMenorOuIgualA(float tempoLimite) {
        int lo = 0, hi = notasVisiveis.size() - 1;
        int ans = -1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (notasVisiveis.get(mid).tempoInicio <= tempoLimite) {
                ans = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return ans;
    }

    /**
     * Retorna a coordenada Y (vertical) para desenhar uma nota com nome dado.
     * O mapeamento é fixo, com base no centro da tela e um espaçamento vertical constante.
     */
    private float getYParaNota(String nota) {
        float espac = 20f;
        float yBase = getHeight() / 2f;
        Map<String, Float> pos = new HashMap<>();
        pos.put("E5", -0.8f);   pos.put("D#5", 0.5f);  pos.put("D5", 0.5f);
        pos.put("C#5", 2f);    pos.put("C5", 2f);     pos.put("B4", 3f);
        pos.put("A#4", 4.6f);  pos.put("A4", 4.6f);   pos.put("G#4", 5.8f);
        pos.put("G4", 5.8f);   pos.put("F#4", 7f);    pos.put("F4", 7f);
        pos.put("E4", 8f);     pos.put("D#4", 9f);    pos.put("D4", 9f);
        pos.put("C#4", 10f);   pos.put("C4", 10f);
        Float offset = pos.get(nota.toUpperCase());
        return yBase + ((offset != null) ? offset : 0f) * espac;
    }

    /**
     * Retorna o timestamp de início da partitura, ou 0 se ainda não iniciou.
     */
    public long getTempoInicial() {
        return (tempoInicial != null) ? tempoInicial : 0L;
    }
}
