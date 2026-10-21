package tv.dumbtv.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A row that goes on to the right. With nothing focused, three cards share the width equally; the
 * focused card takes half and its neighbours a quarter each, and the row slides so that the previous card,
 * the focused one and the next stay on screen. Left and right move by one card.
 */
final class Carousel extends ViewGroup {
    /** Shares of the row: the focused card and each neighbour. Wider than this and a backdrop is cropped past recognition. */
    private static final float FOCUS = 0.5f, SIDE = 0.25f;

    private final int gap, padX, padY;
    private float[] w = new float[0];
    private float offset, usable;
    private int focus = -1, last;
    private ValueAnimator anim;

    Carousel(Context c, int gap, int padX, int padY) {
        super(c);
        this.gap = gap;
        this.padX = padX;
        this.padY = padY;
    }

    /** Empties the row and puts it back to its first state. */
    void clear() {
        if (anim != null) anim.cancel();
        removeAllViews();
        w = new float[0];
        focus = -1;
        last = 0;
        offset = 0;
    }

    int count() {
        return getChildCount();
    }

    /** The card to land on when focus comes from outside: the one focused last. */
    View entry() {
        return getChildAt(Math.min(last, getChildCount() - 1));
    }

    /** Moves the 60% to card {@code i}; -1 puts all cards back to equal widths. */
    void setFocus(int i) {
        if (i == focus) return;
        focus = i;
        if (i >= 0) last = i;
        slide();
    }

    private float width(int i, int f) {
        float u = usable - 2 * gap;
        return f < 0 ? u / 3 : i == f ? u * FOCUS : u * SIDE;
    }

    private float offsetFor(int f) {
        float u = usable - 2 * gap;
        if (f < 0) return Math.max(0, Math.min(last - 1, getChildCount() - 3)) * (u / 3 + gap);
        return Math.max(0, f - 1) * (u * SIDE + gap);
    }

    private void slide() {
        if (usable <= 0) return;
        int n = getChildCount();
        float[] from = Arrays.copyOf(w, n), to = new float[n];
        for (int i = 0; i < n; i++) to[i] = width(i, focus);
        float fromOffset = offset, toOffset = offsetFor(focus);
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(280);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            float f = a.getAnimatedFraction();
            for (int i = 0; i < Math.min(w.length, from.length); i++) w[i] = from[i] + (to[i] - from[i]) * f;
            offset = fromOffset + (toOffset - fromOffset) * f;
            requestLayout();
        });
        anim.start();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int total = MeasureSpec.getSize(widthSpec);
        int n = getChildCount();
        if (usable != total - 2 * padX) {
            usable = total - 2 * padX;
            w = new float[0];
            offset = offsetFor(focus);
        }
        if (w.length != n) {
            int old = w.length;
            w = Arrays.copyOf(w, n);
            for (int i = old; i < n; i++) w[i] = width(i, focus);
        }
        int h = 0;
        for (int i = 0; i < n; i++) {
            View c = getChildAt(i);
            c.measure(MeasureSpec.makeMeasureSpec(Math.max(1, Math.round(w[i])), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            h = Math.max(h, c.getMeasuredHeight());
        }
        setMeasuredDimension(total, h + 2 * padY);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        float x = padX - offset;
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            int left = Math.round(x);
            c.layout(left, padY, left + c.getMeasuredWidth(), padY + c.getMeasuredHeight());
            x += w[i] + gap;
        }
    }

    /** Left and right step to the neighbouring card; up and down leave the row as usual. */
    @Override
    public View focusSearch(View focused, int direction) {
        if (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) {
            View card = focused;
            while (card != null && card.getParent() != this) card = card.getParent() instanceof View ? (View) card.getParent() : null;
            int i = card == null ? -1 : indexOfChild(card);
            int to = i + (direction == View.FOCUS_RIGHT ? 1 : -1);
            return i >= 0 && to >= 0 && to < getChildCount() ? getChildAt(to) : focused;
        }
        return super.focusSearch(focused, direction);
    }

    /** From outside, only one card is a candidate: the focus finder would otherwise pick one off screen. */
    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (getChildCount() == 0 || hasFocus()) return;
        entry().addFocusables(views, direction, focusableMode);
    }
}
