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

    /** TMDB's genre ids, which do not change, in the two languages the screen speaks. */
    private static final java.util.Map<Integer, String> GENRES = new java.util.HashMap<>();
    private static final java.util.Map<Integer, String> GENRES_TR = new java.util.HashMap<>();

    static {
        int[] id = {28, 12, 16, 35, 80, 99, 18, 10751, 14, 36, 27, 10402, 9648, 10749, 878, 10770, 53, 10752, 37};
        String[] en = {"Action", "Adventure", "Animation", "Comedy", "Crime", "Documentary", "Drama", "Family", "Fantasy",
            "History", "Horror", "Music", "Mystery", "Romance", "Sci-Fi", "TV Movie", "Thriller", "War", "Western"};
        String[] tr = {"Aksiyon", "Macera", "Animasyon", "Komedi", "Su\u00e7", "Belgesel", "Dram", "Aile", "Fantastik",
            "Tarih", "Korku", "M\u00fczik", "Gizem", "Romantik", "Bilim Kurgu", "TV Filmi", "Gerilim", "Sava\u015f", "Vah\u015fi Batı"};
        for (int i = 0; i < id.length; i++) {
            GENRES.put(id[i], en[i]);
            GENRES_TR.put(id[i], tr[i]);
        }
    }

    private Movies() {}

    /**
     * One page of TMDB's "trending this week" films that have a backdrop, in {@code lang} ("tr-TR" or
     * "en-US"). A film with no translation comes back with an empty title or summary: those are taken
     * from the English page. Blocking: call it off the UI thread.
     */
    static Result page(Context c, String token, int page, String lang) {
        Result out = new Result();
        File dir = dir(c);
        JSONObject root = dir == null ? null : fetch(dir, token, page, lang);
        if (root == null) {
            out.failed = true;
            return out;
        }
        java.util.Map<Integer, JSONObject> english = new java.util.HashMap<>();
        JSONObject en = lang.equals("en-US") ? null : fetch(dir, token, page, "en-US");
        JSONArray base = en == null ? null : en.optJSONArray("results");
        for (int i = 0; base != null && i < base.length(); i++) {
            JSONObject o = base.optJSONObject(i);
            if (o != null) english.put(o.optInt("id"), o);
        }
        java.util.Map<Integer, String> genres = lang.startsWith("tr") ? GENRES_TR : GENRES;
        try {
            out.pages = Math.min(root.optInt("total_pages", 1), 500);
            JSONArray results = root.getJSONArray("results");
            for (int i = 0; i < results.length(); i++) {
                JSONObject m = results.getJSONObject(i);
                String path = m.optString("backdrop_path", "");
                if (path.isEmpty() || "null".equals(path)) continue;
                String title = m.optString("title", ""), overview = m.optString("overview", "");
                JSONObject fallback = english.get(m.optInt("id"));
                if (fallback != null) {
                    if (title.isEmpty()) title = fallback.optString("title", "");
                    if (overview.isEmpty()) overview = fallback.optString("overview", "");
                }
                String year = m.optString("release_date", "");
                double vote = m.optDouble("vote_average", 0);
                StringBuilder note = new StringBuilder(year.length() >= 4 ? year.substring(0, 4) : "");
                JSONArray ids = m.optJSONArray("genre_ids");
                int named = 0;
                for (int g = 0; ids != null && g < ids.length() && named < 2; g++) {
                    String name = genres.get(ids.optInt(g));
                    if (name == null) continue;
                    note.append(note.length() > 0 ? " \u00b7 " : "").append(name);
                    named++;
                }
                if (vote > 0) note.append(note.length() > 0 ? " \u00b7 " : "").append("\u2605 ").append(String.format(Locale.US, "%.1f", vote));
                out.movies.add(new Movie(title, path, note.toString(), overview));
            }
        } catch (JSONException e) {
            Log.w("dumbtv", "trending movies: " + e.getMessage());
            out.failed = true;
        }
        return out;
    }

    /** The cached page, or a fresh one when it is older than {@link #TTL_MS}; null when there is nothing at all. */
    private static JSONObject fetch(File dir, String token, int page, String lang) {
        File list = new File(dir, "trending-" + lang + "-" + page + ".json");
        try {
            if (!list.isFile() || System.currentTimeMillis() - list.lastModified() > TTL_MS) {
                write(list, get("https://api.themoviedb.org/3/trending/movie/week?language=" + lang + "&page=" + page, token));
            }
        } catch (IOException e) {
            Log.w("dumbtv", "trending movies: " + e.getMessage());
        }
        if (!list.isFile()) return null;
        try {
            return new JSONObject(new String(read(list), StandardCharsets.UTF_8));
        } catch (JSONException | IOException e) {
            return null;
        }
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
