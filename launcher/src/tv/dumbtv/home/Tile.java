package tv.dumbtv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.media.tv.TvInputManager;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;

/** A card drawn in one pass: banner, or icon plus name, or an input name; focus ring on top. */
final class Tile extends View {
    private static final float FOCUS_SCALE = 1.08f, LIFT_DP = 26;

    static final String MOVIES = "movies";

    final Item item;
    final String kind;
    private final CharSequence title;
    private int cardW;
    private final int cardH, labelH;
    private final Rect crop = new Rect();
    private final boolean labelAlways, labelOnFocus;
    private final float radius, stroke;
    private final RectF card = new RectF();
    private final Path clip = new Path();
    private final Path arrow = new Path();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint inner = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint caption = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint movieScrim = new Paint();
    private StaticLayout heading, note, body;
    private boolean open;
    private final TextPaint under = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private Shader sheen;
    private final Paint gloss = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean moving;

    Tile(Context c, Item item, String kind, CharSequence title, int cardW, int cardH,
            String labels, int accent) {
        super(c);
        this.item = item;
        this.kind = kind;
        this.title = title;
        this.cardW = cardW;
        this.cardH = cardH;
        boolean app = Catalog.APPS.equals(kind);
        labelH = 0;
        labelAlways = "always".equals(labels);
        labelOnFocus = "focus".equals(labels);
        radius = Ui.dp(c, MOVIES.equals(kind) ? 12 : Ui.RADIUS);
        stroke = Ui.dp(c, 2.5f);

        fill.setColor(Ui.RAISED);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(stroke);
        ring.setColor(accent);
        dot.setColor(Ui.COOL);
        inner.setColor(Ui.TEXT);
        inner.setTypeface(Ui.MEDIUM);
        inner.setTextAlign(Paint.Align.CENTER);
        inner.setTextSize(Ui.dp(c, app ? 14 : 17));
        under.setColor(Ui.TEXT);
        under.setTypeface(Ui.MEDIUM);
        under.setTextAlign(Paint.Align.CENTER);
        under.setTextSize(Ui.dp(c, 13));

        caption.setTypeface(Ui.MEDIUM);
        caption.setTextSize(Ui.dp(c, 13));

        setFocusable(true);
        setClickable(true);
        setLongClickable(true);
        setContentDescription(title);
    }

    void setMoving(boolean on) {
        moving = on;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // a parent that fixes the width (the movie row's weights) wins over the size asked for
        if (MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY) cardW = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(cardW, cardH + labelH);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        card.set(0, 0, cardW, cardH);
        clip.reset();
        clip.addRoundRect(card, radius, radius, Path.Direction.CW);
        // a soft specular sheen from the top-left corner: the card catches light when it lifts
        sheen = new LinearGradient(0, 0, cardW * 0.7f, cardH * 1.1f, 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        gloss.setShader(sheen);
        movieScrim.setShader(new LinearGradient(0, cardH * 0.25f, 0, cardH, 0x00090B0E, 0xEB090B0E, Shader.TileMode.CLAMP));
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, cardW, cardH, radius);
            }
        });
        layoutText();
        setPivotX(cardW / 2f);
        setPivotY(cardH / 2f);
    }

    /**
     * Sets the title, and on the focused card its year, genres and rating, inside the bottom of the
     * card. Each text steps down in size until it fits; an ellipsis is the last resort.
     */
    private void layoutText() {
        if (!MOVIES.equals(kind) || cardW <= 0) return;
        Context c = getContext();
        int pad = Ui.dp(c, 12), room = Math.max(1, cardW - 2 * pad), gap = Ui.dp(c, 3);
        boolean big = isFocused();
        caption.setColor(Ui.TEXT);
        heading = fit(title, caption, room, big ? new int[] {19, 17, 15, 13} : new int[] {13, 12, 11, 10}, Ui.dp(c, big ? 56 : 58));
        int left = cardH - 2 * pad - heading.getHeight();
        note = null;
        body = null;
        if (big && !item.note.isEmpty()) {
            under.setTextAlign(Paint.Align.LEFT);
            under.setColor(Ui.SILVER);
            note = fit(item.note, under, room, new int[] {14, 13, 12, 11}, Ui.dp(c, 22));
            left -= note.getHeight() + gap;
        }
        if (open && !item.overview.isEmpty()) {
            TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            p.setTypeface(Ui.MEDIUM);
            p.setColor(0xFFE4E8EC);
            body = fit(item.overview, p, room, new int[] {14, 13, 12, 11}, left - 2 * gap);
        }
    }

    /** The first of {@code sizesDp} at which {@code text} fits {@code maxHeight}; the last, cut with an ellipsis, if none does. */
    private StaticLayout fit(CharSequence text, TextPaint paint, int room, int[] sizesDp, int maxHeight) {
        StaticLayout l = null;
        for (int sz : sizesDp) {
            paint.setTextSize(Ui.dp(getContext(), sz));
            l = StaticLayout.Builder.obtain(text, 0, text.length(), paint, room).setLineSpacing(0, 1.08f).build();
            if (l.getHeight() <= maxHeight) return l;
        }
        int lines = Math.max(1, maxHeight * l.getLineCount() / Math.max(1, l.getHeight()));
        return StaticLayout.Builder.obtain(text, 0, text.length(), paint, room).setLineSpacing(0, 1.08f)
                .setMaxLines(lines).setEllipsize(TextUtils.TruncateAt.END).build();
    }

    /** A click on a movie opens its summary inside the card; another closes it. */
    void toggleSummary() {
        if (!MOVIES.equals(kind) || item.overview.isEmpty()) return;
        open = !open;
        layoutText();
        invalidate();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previous) {
        super.onFocusChanged(focused, direction, previous);
        if (!focused) open = false;
        layoutText();
        float s = focused ? FOCUS_SCALE : 1f;
        animate().scaleX(s).scaleY(s).translationZ(focused ? Ui.dp(getContext(), LIFT_DP) : 0)
                .setDuration(180).setInterpolator(new DecelerateInterpolator()).start();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable banner = item.banner, icon = item.icon;
        canvas.save();
        canvas.clipPath(clip);
        canvas.drawRect(card, fill);
        if (banner instanceof android.graphics.drawable.BitmapDrawable && MOVIES.equals(kind)) {
            // fill the card and crop the overflow: the width changes while the row opens up
            android.graphics.Bitmap bmp = ((android.graphics.drawable.BitmapDrawable) banner).getBitmap();
            float s = Math.max(cardW / (float) bmp.getWidth(), cardH / (float) bmp.getHeight());
            int sw = Math.round(cardW / s), sh = Math.round(cardH / s);
            // crop from a little above the middle: faces sit in the upper half of a backdrop
            int top = Math.round((bmp.getHeight() - sh) * 0.35f);
            crop.set((bmp.getWidth() - sw) / 2, top, (bmp.getWidth() + sw) / 2, top + sh);
            canvas.drawBitmap(bmp, crop, card, fill);
        } else if (banner != null) {
            banner.setBounds(0, 0, cardW, cardH);
            banner.draw(canvas);
        } else if (icon != null) {
            int size = cardH * 46 / 100;
            int left = (cardW - size) / 2, top = cardH * 12 / 100;
            icon.setBounds(left, top, left + size, top + size);
            icon.draw(canvas);
            drawLine(canvas, inner, cardW / 2f, cardH - cardH * 13 / 100f);
        } else if (!Catalog.APPS.equals(kind)) {
            drawLine(canvas, inner, cardW / 2f, cardH / 2f - (inner.ascent() + inner.descent()) / 2);
        }
        if (MOVIES.equals(kind) && heading != null) {
            canvas.drawRect(0, 0, cardW, cardH, movieScrim);
            float pad = Ui.dp(getContext(), 12), gap = Ui.dp(getContext(), 3);
            if (body != null) canvas.drawColor(0x99090B0E);
            float y = cardH - pad - heading.getHeight() - (note != null ? note.getHeight() + gap : 0)
                    - (body != null ? body.getHeight() + 2 * gap : 0);
            canvas.save();
            canvas.translate(pad, y);
            heading.draw(canvas);
            if (note != null) {
                canvas.translate(0, heading.getHeight() + gap);
                note.draw(canvas);
            }
            if (body != null) {
                canvas.translate(0, (note != null ? note.getHeight() : 0) + 2 * gap);
                body.draw(canvas);
            }
            canvas.restore();
        }
        if (isFocused() && !moving) canvas.drawRect(card, gloss);
        if (moving) canvas.drawColor(0x99090B0E);
        canvas.restore();

        if (item.state == TvInputManager.INPUT_STATE_CONNECTED) {
            float r = Ui.dp(getContext(), 3.5f);
            canvas.drawCircle(cardW - r * 3, r * 3, r, dot);
        }
        if (moving) {
            float half = stroke / 2;
            ring.setPathEffect(new DashPathEffect(new float[] {stroke * 3, stroke * 2}, 0));
            canvas.drawRoundRect(half, half, cardW - half, cardH - half, radius - half, radius - half, ring);
        }
        if (moving) drawArrows(canvas);
    }

    private void drawLine(Canvas canvas, TextPaint paint, float x, float baseline) {
        float room = cardW - Ui.dp(getContext(), 16);
        CharSequence s = TextUtils.ellipsize(title, paint, room, TextUtils.TruncateAt.END);
        canvas.drawText(s, 0, s.length(), x, baseline, paint);
    }

    private void drawArrows(Canvas canvas) {
        float h = cardH / 6f, w = h * 0.7f, mid = cardH / 2f, pad = h * 0.8f;
        inner.setStyle(Paint.Style.FILL);
        arrow.reset();
        arrow.moveTo(pad, mid);
        arrow.lineTo(pad + w, mid - h / 2);
        arrow.lineTo(pad + w, mid + h / 2);
        arrow.close();
        arrow.moveTo(cardW - pad, mid);
        arrow.lineTo(cardW - pad - w, mid - h / 2);
        arrow.lineTo(cardW - pad - w, mid + h / 2);
        arrow.close();
        canvas.drawPath(arrow, inner);
    }
}
