package tv.dumbtv.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TMDB's films trending this week, a page at a time and as wide backdrops. Lists and images are cached on
 * disk, so what was seen once still shows when the network is down.
 */
final class Movies {
    private static final String IMAGES = "https://image.tmdb.org/t/p/w780";
    private static final long TTL_MS = 6L * 60 * 60 * 1000;

    static final class Movie {
        final String title, path, note, overview;

        Movie(String title, String path, String note, String overview) {
            this.title = title;
            this.path = path;
            this.note = note;
            this.overview = overview;
        }
    }

    /** One page of a release window; {@code failed} means nothing could be had, not that the window is empty. */
    static final class Result {
        final List<Movie> movies = new ArrayList<>();
        int pages = 1;
        boolean failed;
    }

    /** TMDB's genre ids, which do not change; the titles are English everywhere, so the genres are too. */
    private static final java.util.Map<Integer, String> GENRES = new java.util.HashMap<>();

    static {
        Object[] g = {28, "Action", 12, "Adventure", 16, "Animation", 35, "Comedy", 80, "Crime",
            99, "Documentary", 18, "Drama", 10751, "Family", 14, "Fantasy", 36, "History", 27, "Horror",
            10402, "Music", 9648, "Mystery", 10749, "Romance", 878, "Sci-Fi", 10770, "TV Movie",
            53, "Thriller", 10752, "War", 37, "Western"};
        for (int i = 0; i < g.length; i += 2) GENRES.put((Integer) g[i], (String) g[i + 1]);
    }

    private Movies() {}

    /**
     * One page of TMDB's "trending this week" films that have a backdrop, in English. Blocking:
     * call it off the UI thread.
     */
    static Result page(Context c, String token, int page) {
        Result out = new Result();
        File dir = dir(c);
        if (dir == null) {
            out.failed = true;
            return out;
        }
        File list = new File(dir, "trending-" + page + ".json");
        try {
            if (!list.isFile() || System.currentTimeMillis() - list.lastModified() > TTL_MS) {
                write(list, get("https://api.themoviedb.org/3/trending/movie/week?language=en-US&page=" + page, token));
            }
        } catch (IOException e) {
            Log.w("dumbtv", "trending movies: " + e.getMessage());
        }
        if (!list.isFile()) {
            out.failed = true;
            return out;
        }
        try {
            JSONObject root = new JSONObject(new String(read(list), StandardCharsets.UTF_8));
            out.pages = Math.min(root.optInt("total_pages", 1), 500);
            JSONArray results = root.getJSONArray("results");
            for (int i = 0; i < results.length(); i++) {
                JSONObject m = results.getJSONObject(i);
                String path = m.optString("backdrop_path", "");
                if (path.isEmpty() || "null".equals(path)) continue;
                String year = m.optString("release_date", "");
                double vote = m.optDouble("vote_average", 0);
                StringBuilder note = new StringBuilder(year.length() >= 4 ? year.substring(0, 4) : "");
                JSONArray ids = m.optJSONArray("genre_ids");
                int named = 0;
                for (int g = 0; ids != null && g < ids.length() && named < 2; g++) {
                    String name = GENRES.get(ids.optInt(g));
                    if (name == null) continue;
                    note.append(note.length() > 0 ? " \u00b7 " : "").append(name);
                    named++;
                }
                if (vote > 0) note.append(note.length() > 0 ? " \u00b7 " : "").append("\u2605 ").append(String.format(Locale.US, "%.1f", vote));
                out.movies.add(new Movie(m.optString("title", ""), path, note.toString(), m.optString("overview", "")));
            }
        } catch (JSONException | IOException e) {
            Log.w("dumbtv", "trending movies: " + e.getMessage());
            out.failed = true;
        }
        return out;
    }

    /** The backdrop of {@code m}, or null when it cannot be had; blocking. Half-size pixels: it adds up. */
    static Bitmap art(Context c, String token, Movie m) {
        File dir = dir(c);
        if (dir == null) return null;
        File f = new File(dir, "w780" + m.path.replace('/', '_'));
        if (!f.isFile()) {
            try {
                write(f, get(IMAGES + m.path, token));
            } catch (IOException e) {
                return null;
            }
        }
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(f.getPath(), o);
    }

    private static File dir(Context c) {
        File dir = new File(c.getCacheDir(), "tmdb");
        return dir.isDirectory() || dir.mkdirs() ? dir : null;
    }

    private static byte[] get(String url, String token) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(12000);
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setRequestProperty("Accept", "application/json");
        try {
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream()) {
                return slurp(in);
            }
        } finally {
            c.disconnect();
        }
    }

    private static byte[] read(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            return slurp(in);
        }
    }

    private static byte[] slurp(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16 * 1024];
        for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private static void write(File f, byte[] data) throws IOException {
        File tmp = new File(f.getPath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(data);
        }
        if (!tmp.renameTo(f)) throw new IOException("could not replace " + f);
    }
}
