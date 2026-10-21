package tv.dumbtv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** A round glass button with a line icon drawn by hand: no image assets. */
final class IconButton extends View {
    static final int SLIDERS = 0, SUN = 1;

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
