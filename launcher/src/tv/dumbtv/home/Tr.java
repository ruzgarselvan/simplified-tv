package tv.dumbtv.home;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** The interface text in Turkish when the TV's language is Turkish; the English key otherwise. */
final class Tr {
    private static final Map<String, String> TR = new HashMap<>();

    static {
        String[] p = {
            "Options", "Seçenekler",
            "Settings", "Ayarlar",
            "Apps", "Uygulamalar",
            "Popular movies", "Popüler filmler",
            "Inputs", "Girişler",
            "Accent color", "Vurgu rengi",
            "White", "Beyaz", "Blue", "Mavi", "Cyan", "Turkuaz", "Green", "Yeşil",
            "Amber", "Kehribar", "Red", "Kırmızı", "Pink", "Pembe", "Violet", "Mor",
            "Tile size", "Kutu boyutu", "Small", "Küçük", "Medium", "Orta", "Large", "Büyük",
            "App names", "Uygulama adları", "On focus", "Seçiliyken", "Always", "Her zaman", "Never", "Asla",
            "Clock", "Saat", "System", "Sistem", "24-hour", "24 saat", "12-hour", "12 saat",
            "Inputs button", "Girişler düğmesi", "Shown", "Görünür", "Hidden", "Gizli",
            "Background", "Arka plan", "Graphite", "Grafit", "Midnight", "Gece mavisi",
            "Image file", "Resim dosyası", "Raised", "Yükseltilmiş", "Steel", "Çelik mavisi", "Atmosphere", "Atmosfer", "Language", "Dil", "YouTube app", "YouTube uygulaması",
            "SmartTube is not installed, opening YouTube", "SmartTube kurulu değil, YouTube açılıyor",
            "On", "Açık",
            "Image dim", "Resim karartma",
            "Wallpaper rotation", "Duvar kağıdı değişimi", "Off", "Kapalı",
            "Every return home", "Her ana ekrana dönüşte", "Every hour", "Her saat",
            "Every day", "Her gün", "By time of day", "Günün saatine göre",
            "Move", "Taşı", "Rename", "Yeniden adlandır", "Hide", "Gizle", "App info", "Uygulama bilgisi",
            "Could not open that", "Açılamadı",
            "Hidden. Bring it back from Options.", "Gizlendi. Seçenekler'den geri getirebilirsiniz.",
            "Save", "Kaydet", "Original", "Orijinal", "Cancel", "İptal", "Done", "Tamam", "Reset", "Sıfırla",
            "Next wallpaper", "Sonraki duvar kağıdı",
            "Apps on home screen", "Ana ekrandaki uygulamalar",
            "Inputs on home screen", "Ana ekrandaki girişler",
            "Reset order, names and hidden tiles", "Sırayı, adları ve gizlenenleri sıfırla",
            "Reset the home screen layout?", "Ana ekran düzeni sıfırlansın mı?",
            "Order, names and hidden tiles go back to default. Colors and sizes stay.",
            "Sıra, adlar ve gizlenen kutular varsayılana döner. Renkler kalır.",
            "Settings file: ", "Ayar dosyası: ",
            "Wallpaper: put wallpaper.jpg in that folder, then pick Background › Image file",
            "Duvar kağıdı: wallpaper.jpg dosyasını o klasöre koyun, sonra Arka plan › Resim dosyası'nı seçin",
            "Several wallpapers: put them in %s/ and pick a rotation",
            "Birden fazla duvar kağıdı: hepsini %s/ klasörüne koyun ve bir değişim seçin",
            "Internet is used only for the popular movies row. No tracking, no ads, no accounts.",
            "İnternet yalnızca popüler filmler satırı için kullanılır. İzleme, reklam ve hesap yok.",
        };
        for (int i = 0; i < p.length; i += 2) TR.put(p[i], p[i + 1]);
    }

    private Tr() {}

    /** "tr", "en", or "auto" to follow the TV's language. */
    private static String forced = "auto";

    static void use(String language) {
        forced = language;
    }

    /** The locale the screen's dates are written in. */
    static Locale locale() {
        switch (forced) {
            case "tr": return new Locale("tr", "TR");
            case "en": return Locale.US;
            default: return Locale.getDefault();
        }
    }

    /** The language TMDB is asked for: Turkish on a Turkish screen, English otherwise. */
    static String tmdbLanguage() {
        return "tr".equals(locale().getLanguage()) ? "tr-TR" : "en-US";
    }

    static String t(String en) {
        if (!"tr".equals(locale().getLanguage())) return en;
        String s = TR.get(en);
        return s == null ? en : s;
    }
}
