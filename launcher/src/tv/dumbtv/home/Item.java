package tv.dumbtv.home;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;

/** One tile: an installed app (id = package name) or a TV input (id = input id). */
final class Item {
    final String id;
    final CharSequence label;
    final Intent intent;
    ActivityInfo info;
    boolean leanback;
    int state = -1;
    volatile boolean artRequested;
    volatile Drawable banner, icon;
    /** A second line for a tile, such as a movie's year and rating. */
    String note = "";
    /** A longer text for a tile, shown when it is opened. */
    String overview = "";
    /** The art as a small blurred color field for the backdrop, built on first focus. */
    android.graphics.Bitmap glow;

    Item(String id, CharSequence label, Intent intent) {
        this.id = id;
        this.label = label;
        this.intent = intent;
    }
}
