package tv.dumbtv.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * The focused app's art as a wide, blurred, drained color field behind everything, darkened toward
 * the edges and the bottom. A new field crossfades over the old one slowly; nothing else moves.
 */
final class Backdrop extends FrameLayout {
    private static final long FADE_MS = 1000;
    private static final int W = 96, H = 54;

    private final ImageView back = field(), front = field();
    private final View shade = new View(getContext());
    private Bitmap shown;

    Backdrop(Context c) {
        super(c);
        setClickable(false);
        setFocusable(false);
        addView(back, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(front, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(shade, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        front.setAlpha(0f);
        back.setAlpha(0.85f);
        // a lit graphite base under the fields, so the screen never falls to black
        setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0xFF2E3540, 0xFF12161C}));
        GradientDrawable dark = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x33090B0E, 0x00090B0E, 0x99090B0E});
        shade.setBackground(dark);
    }

    private ImageView field() {
        ImageView v = new ImageView(getContext());
        v.setScaleType(ImageView.ScaleType.FIT_XY);
        return v;
    }

    /** Crossfades to a color field made by {@link #field(Drawable)}; null fades back to plain base. */
    void show(Bitmap next) {
        if (next == shown) return;
        shown = next;
        front.animate().cancel();
        // what the front shows now becomes the back, so an interrupted fade never jumps
        back.setImageDrawable(front.getDrawable() != null && front.getAlpha() > 0.4f
                ? front.getDrawable() : back.getDrawable());
        front.setAlpha(0f);
        front.setImageBitmap(next);
        front.animate().alpha(0.85f).setDuration(FADE_MS).setInterpolator(new DecelerateInterpolator()).start();
    }

    /**
     * A 96x54 light blur of {@code art}, drained of most of its saturation and pulled toward the base
     * color, so a white banner gives a soft grey light and not a glare.
     */
    static Bitmap field(Drawable art) {
        Bitmap b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(Ui.GRAPHITE);
        android.graphics.Rect was = art.copyBounds();
        art.setBounds(0, 0, W, H);
        art.draw(c);
        art.setBounds(was);
        return process(b);
    }

    /** The same field for several images side by side, left to right: the movies spread across the screen. */
    static Bitmap spread(java.util.List<Bitmap> art) {
        Bitmap b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(Ui.GRAPHITE);
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
        int n = art.size();
        for (int i = 0; i < n; i++) {
            c.drawBitmap(art.get(i), null, new android.graphics.Rect(i * W / n, 0, (i + 1) * W / n, H), p);
        }
        return process(b);
    }

    private static Bitmap process(Bitmap b) {
        int[] px = new int[W * H];
        b.getPixels(px, 0, W, 0, 0, W, H);
        for (int i = 0; i < px.length; i++) {
            float r = (px[i] >> 16 & 0xFF), g = (px[i] >> 8 & 0xFF), bl = (px[i] & 0xFF);
            float gray = 0.3f * r + 0.59f * g + 0.11f * bl;
            r = gray + (r - gray) * 0.9f;
            g = gray + (g - gray) * 0.9f;
            bl = gray + (bl - gray) * 0.9f;
            px[i] = 0xFF000000 | mix(r, 0x09) << 16 | mix(g, 0x0B) << 8 | mix(bl, 0x0E);
        }
        for (int pass = 0; pass < 2; pass++) px = blur(px);
        b.setPixels(px, 0, W, 0, 0, W, H);
        return b;
    }

    private static int mix(float c, int base) {
        return Math.round(c * 0.58f + base * 0.42f);
    }

    /** One horizontal and one vertical box blur of radius 3. */
    private static int[] blur(int[] in) {
        int[] tmp = new int[in.length], out = new int[in.length];
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) tmp[y * W + x] = avg(in, x, y, 1, 0);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) out[y * W + x] = avg(tmp, x, y, 0, 1);
        return out;
    }

    private static int avg(int[] px, int x, int y, int dx, int dy) {
        int r = 0, g = 0, b = 0;
        for (int k = -3; k <= 3; k++) {
            int xx = Math.max(0, Math.min(W - 1, x + k * dx)), yy = Math.max(0, Math.min(H - 1, y + k * dy));
            int p = px[yy * W + xx];
            r += p >> 16 & 0xFF;
            g += p >> 8 & 0xFF;
            b += p & 0xFF;
        }
        return 0xFF000000 | r / 7 << 16 | g / 7 << 8 | b / 7;
    }
}
