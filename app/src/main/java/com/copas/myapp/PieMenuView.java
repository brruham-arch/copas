package com.copas.myapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

// script_name: PieMenuView
// author: brruham
// version: 1.0
// Pie menu manual pakai Canvas.drawArc. Deteksi tombol via atan2 dari
// titik pusat ke posisi touch, di-map ke index item (sama prinsip
// dengan hitbox radial di mimgui).
public class PieMenuView extends View {

    public interface OnItemSelected {
        void onSelected(int index);
    }

    private final String[] labels;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txtPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private OnItemSelected listener;
    private int highlighted = -1;

    public PieMenuView(Context ctx, String[] labels) {
        super(ctx);
        this.labels = labels;

        bgPaint.setColor(Color.parseColor("#CC202020"));
        bgPaint.setStyle(Paint.Style.FILL);

        sepPaint.setColor(Color.parseColor("#55FFFFFF"));
        sepPaint.setStrokeWidth(2f);

        txtPaint.setColor(Color.WHITE);
        txtPaint.setTextSize(30f);
        txtPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setOnItemSelected(OnItemSelected l) {
        this.listener = l;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float radius = Math.min(w, h) / 2f - 4f;
        RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        float sweep = 360f / labels.length;
        for (int i = 0; i < labels.length; i++) {
            float start = i * sweep;
            if (i == highlighted) {
                Paint hi = new Paint(bgPaint);
                hi.setColor(Color.parseColor("#CC3A7BD5"));
                canvas.drawArc(oval, start, sweep, true, hi);
            } else {
                canvas.drawArc(oval, start, sweep, true, bgPaint);
            }
            canvas.drawLine(cx, cy,
                    (float) (cx + radius * Math.cos(Math.toRadians(start))),
                    (float) (cy + radius * Math.sin(Math.toRadians(start))),
                    sepPaint);

            float midAngle = (float) Math.toRadians(start + sweep / 2f);
            float tx = (float) (cx + radius * 0.6f * Math.cos(midAngle));
            float ty = (float) (cy + radius * 0.6f * Math.sin(midAngle));
            canvas.drawText(labels[i], tx, ty, txtPaint);
        }
    }

    private int angleToIndex(float touchX, float touchY) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        double dx = touchX - cx, dy = touchY - cy;
        double distance = Math.sqrt(dx * dx + dy * dy);
        float radius = Math.min(getWidth(), getHeight()) / 2f;
        if (distance > radius) return -1; // di luar lingkaran = batal

        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) angle += 360;
        float sweep = 360f / labels.length;
        return (int) (angle / sweep);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int idx = angleToIndex(event.getX(), event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (idx != highlighted) {
                    highlighted = idx;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (idx >= 0 && listener != null) listener.onSelected(idx);
                return true;
        }
        return true;
    }
}
