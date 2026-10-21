package tv.dumbtv.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.widget.TextView;

/** Design tokens and the few shared surfaces: every color on screen comes from here. */
final class Ui {
    // Dark graphite base, titanium and silver neutrals, one measured cool accent.
    static final int BASE = 0xFF090B0E, RAISED = 0xFF14171C, GRAPHITE = 0xFF232830;
    static final int TITANIUM = 0xFF868E99, SILVER = 0xFFC7CDD5, COOL = 0xFFB7CCDF;
    static final int TEXT = 0xFFF4F6F8, MUTED = 0xFFA9B1BC, FAINT = TITANIUM;
    static final int PAGE = BASE;
    static final int DIALOG = android.R.style.Theme_DeviceDefault_Dialog_Alert;
    /** Corner radius shared by tiles, chips and rows, in dp. */
    static final float RADIUS = 22;

    static Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    static Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    static Typeface CLOCK = LIGHT;
    private static boolean fonts;
    private static float density = 2f;

    private Ui() {}

    /** Switches to the bundled Satoshi; the system sans-serif stays if it cannot be loaded. */
    static void init(Context c) {
        density = c.getResources().getDisplayMetrics().density;
        if (fonts) return;
        fonts = true;
        Typeface light = satoshi(c, 300), medium = satoshi(c, 500), clock = satoshi(c, 400);
        if (light != null && medium != null && clock != null) {
            LIGHT = light;
            MEDIUM = medium;
            CLOCK = clock;
        }
    }

    private static Typeface satoshi(Context c, int weight) {
        try {
            return new Typeface.Builder(c.getAssets(), "Satoshi-Variable.ttf")
                    .setFontVariationSettings("'wght' " + weight).setWeight(weight).build();
        } catch (RuntimeException e) {
            return null;
        }
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context c, float sp, int color, Typeface face) {
        TextView v = new TextView(c);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        v.setTypeface(face);
        v.setIncludeFontPadding(false);
        return v;
    }

    static GradientDrawable round(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    /**
     * Frosted glass without a live blur (a TV's GPU would pay for it every frame): translucent
     * white over whatever the backdrop shows, a faint top-edge highlight, and no outline. QUIET is
     * low and calm, LIFT brightens it, ACTIVE is the light capsule that takes dark ink.
     * A radius of 0 makes a capsule.
     */
    static final class Glass extends Drawable {
        static final int QUIET = 0, LIFT = 1, ACTIVE = 2, DOCK = 3;
        private final int mode;
        private final float radius;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private float r;

        Glass(int mode, float radius) {
            this.mode = mode;
            this.radius = radius;
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(mode == ACTIVE ? 0f : mode == DOCK ? 2.2f * density : 1.5f);
            halo.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onBoundsChange(Rect b) {
            float in = mode == DOCK ? 10 * density : 1;
            box.set(b.left + in, b.top + in, b.right - in, b.bottom - in);
            r = radius <= 0 ? box.height() / 2 : Math.min(radius, box.height() / 2);
            int top, bottom;
            switch (mode) {
                case ACTIVE: top = 0xFFF4F6F8; bottom = 0xFFCDD3DA; break;
                case DOCK: top = 0x3DFFFFFF; bottom = 0x1CFFFFFF; break;
                case LIFT: top = 0x38FFFFFF; bottom = 0x24FFFFFF; break;
                default: top = 0x24FFFFFF; bottom = 0x14FFFFFF;
            }
            fill.setShader(new LinearGradient(0, box.top, 0, box.bottom, top, bottom, Shader.TileMode.CLAMP));
            if (mode == DOCK) {
                // Apple's rim light: bright along the top edge, a quiet side, a second glint low down
                edge.setShader(new LinearGradient(0, box.top, 0, box.bottom,
                        new int[] {0xF2FFFFFF, 0x59FFFFFF, 0x14FFFFFF, 0x66FFFFFF},
                        new float[] {0f, 0.22f, 0.65f, 1f}, Shader.TileMode.CLAMP));
                // light pooled under the top edge, inside the glass
                sheen.setShader(new LinearGradient(0, box.top, 0, box.top + box.height() * 0.5f,
                        0x33FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
                halo.setShader(new LinearGradient(0, box.top - 10 * density, 0, box.top + box.height() * 0.4f,
                        0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            } else {
                edge.setShader(new LinearGradient(0, box.top, 0, box.top + box.height() * 0.6f,
                        mode == QUIET ? 0x33FFFFFF : 0x4DFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            }
        }

        @Override
        public void draw(Canvas canvas) {
            if (mode == DOCK) {
                // a soft glow around the top edge, three strokes thinning outward
                for (int i = 3; i >= 1; i--) {
                    halo.setStrokeWidth(i * 3f * density);
                    halo.setAlpha(26 + (3 - i) * 10);
                    canvas.drawRoundRect(box, r, r, halo);
                }
            }
            canvas.drawRoundRect(box, r, r, fill);
            if (mode == DOCK) canvas.drawRoundRect(box, r, r, sheen);
            if (mode != ACTIVE) canvas.drawRoundRect(box, r, r, edge);
        }

        @Override
        public void getOutline(android.graphics.Outline outline) {
            Rect b = getBounds();
            outline.setRoundRect(b, radius <= 0 ? b.height() / 2f : Math.min(radius, b.height() / 2f));
            outline.setAlpha(mode == ACTIVE ? 1f : 0f);
        }

        @Override
        public void setAlpha(int alpha) {}

        @Override
        public void setColorFilter(ColorFilter filter) {}

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
