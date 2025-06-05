package com.example.virtual_piano;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class PathCanvasView extends View {

    private Paint paintLine;
    private Path path = new Path();

    // Para armazenar referências ao RecyclerView e ao adapter
    private RecyclerView recyclerView;
    private LevelAdapter adapter;

    public PathCanvasView(Context ctx) {
        super(ctx);
        init();
    }

    public PathCanvasView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    private void init() {
        paintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(dpToPx(getContext(), 4));
        paintLine.setColor(Color.parseColor("#FFC107")); // amarelo/dourado, por exemplo
    }

    /**
     * Liga o PathCanvasView ao RecyclerView para consultar posições dos items.
     */
    public void bindRecyclerView(RecyclerView rv, LevelAdapter adapter) {
        this.recyclerView = rv;
        this.adapter = adapter;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (recyclerView == null || adapter == null) return;

        path.reset();
        boolean firstPoint = true;

        // Para cada posição de item no adapter, tenta pegar o ViewHolder atual
        for (int i = 0; i < adapter.getItemCount(); i++) {
            RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(i);
            if (vh != null) {
                // Obtém o View (LevelItem) e calcula seu ponto central
                View itemView = vh.itemView;
                float cx = itemView.getX() + itemView.getWidth() / 2f;
                float cy = itemView.getY() + itemView.getHeight() / 2f;
                float lastX = cx ;
                float lastY = cy;

                if (firstPoint) {
                    path.moveTo(cx, cy);
                    lastX = cx;
                    lastY = cy;
                    firstPoint = false;
                } else {
                    float controlX = (lastX + cx) / 2f;
                    float controlY = (lastY + cy) / 2f; // curva levemente pra cima
                    path.quadTo(controlX, controlY, cx, cy);
                    lastX = cx;
                    lastY = cy;
                }

            }
        }

        // Desenha o traçado
        canvas.drawPath(path, paintLine);
    }

    private int dpToPx(Context ctx, int dp){
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics()
        );
    }
}
