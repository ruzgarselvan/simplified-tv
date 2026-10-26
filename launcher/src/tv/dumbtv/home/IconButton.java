package tv.dumbtv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** A round glass button with a line icon drawn by hand: no image assets. */
final class IconButton extends View {
    static final int SLIDERS = 0, SUN = 1, INPUT = 2, AIRPLAY = 3;

    private final int kind;
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);

    IconButton(Context c, int kind, CharSequence label, OnClickListener click) {
        super(c);
        this.kind = kind;
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeCap(Paint.Cap.ROUND);
        ink.setStrokeWidth(Ui.dp(c, 1.8f));
        setContentDescription(label);
        setFocusable(true);
        setClickable(true);
        setOnClickListener(click);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[] {android.R.attr.state_focused}, new Ui.Glass(Ui.Glass.ACTIVE, 0));
        bg.addState(new int[0], new Ui.Glass(Ui.Glass.QUIET, 0));
        setBackground(bg);
        setOnFocusChangeListener((v, has) -> v.animate().scaleX(has ? 1.08f : 1f).scaleY(has ? 1.08f : 1f)
                .translationZ(has ? Ui.dp(c, 8) : 0).setDuration(180)
                .setInterpolator(new DecelerateInterpolator()).start());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float s = Math.min(getWidth(), getHeight()), cx = getWidth() / 2f, cy = getHeight() / 2f;
        ink.setColor(isFocused() ? Ui.BASE : Ui.SILVER);
        ink.setStyle(Paint.Style.STROKE);
        if (kind == SUN) {
            // a ring with eight short rays
            canvas.drawCircle(cx, cy, s * 0.15f, ink);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI / 4 * i;
                float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
                canvas.drawLine(cx + cos * s * 0.22f, cy + sin * s * 0.22f, cx + cos * s * 0.28f, cy + sin * s * 0.28f, ink);
            }
        } else if (kind == AIRPLAY) {
            // a screen with a triangle rising from its base
            float w = s * 0.34f, h = s * 0.24f, top = cy - h / 2 - s * 0.03f;
            canvas.drawRoundRect(cx - w / 2, top, cx + w / 2, top + h, s * 0.03f, s * 0.03f, ink);
            ink.setStyle(Paint.Style.FILL);
            Path tri = new Path();
            tri.moveTo(cx, top + h * 0.55f);
            tri.lineTo(cx + s * 0.09f, top + h + s * 0.09f);
            tri.lineTo(cx - s * 0.09f, top + h + s * 0.09f);
            tri.close();
            canvas.drawPath(tri, ink);
        } else if (kind == INPUT) {
            // a plug: body, two prongs, a cable
            float w = s * 0.30f, h = s * 0.19f, top = cy - h / 2 - s * 0.02f;
            canvas.drawRoundRect(cx - w / 2, top, cx + w / 2, top + h, s * 0.04f, s * 0.04f, ink);
            canvas.drawLine(cx - s * 0.06f, top, cx - s * 0.06f, top - s * 0.075f, ink);
            canvas.drawLine(cx + s * 0.06f, top, cx + s * 0.06f, top - s * 0.075f, ink);
            canvas.drawLine(cx, top + h, cx, top + h + s * 0.08f, ink);
        } else {
            // two sliders
            float half = s * 0.2f;
            float[] y = {-0.075f, 0.075f}, knob = {-0.07f, 0.08f};
            for (int i = 0; i < 2; i++) {
                canvas.drawLine(cx - half, cy + y[i] * s, cx + half, cy + y[i] * s, ink);
                canvas.drawCircle(cx + knob[i] * s * 1.5f, cy + y[i] * s, s * 0.05f, ink);
            }
        }
    }
}
