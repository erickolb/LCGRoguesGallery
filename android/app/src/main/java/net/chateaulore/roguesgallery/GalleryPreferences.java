package net.chateaulore.roguesgallery;

import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class GalleryPreferences {
    private final SharedPreferences preferences;
    int maximum;
    boolean randomSelection;
    boolean rememberSelection;
    boolean randomDisplay;
    int rotationSeconds;
    boolean crossFade;

    GalleryPreferences(SharedPreferences preferences) {
        this.preferences = preferences;
        maximum = Math.max(1, preferences.getInt("maximum", CollectionSelection.DEFAULT_MAXIMUM));
        randomSelection = preferences.getBoolean("randomSelection", true);
        rememberSelection = preferences.getBoolean("rememberSelection", false);
        randomDisplay = preferences.getBoolean("randomDisplay", true);
        rotationSeconds = Math.max(3, Math.min(600, preferences.getInt("rotationSeconds", 15)));
        crossFade = preferences.getBoolean("crossFade", true);
    }

    void save() {
        SharedPreferences.Editor editor = preferences.edit()
            .putInt("maximum", maximum).putBoolean("randomSelection", randomSelection)
            .putBoolean("rememberSelection", rememberSelection).putBoolean("randomDisplay", randomDisplay)
            .putInt("rotationSeconds", rotationSeconds).putBoolean("crossFade", crossFade);
        if (!rememberSelection) {
            for (String key : preferences.getAll().keySet()) if (key.startsWith("selection.")) editor.remove(key);
        }
        editor.apply();
    }

    Set<String> exclusions(String account) {
        return new HashSet<>(preferences.getStringSet("excluded." + CoverCache.digest(account), new HashSet<>()));
    }

    void exclusions(String account, Set<String> excluded) {
        preferences.edit().putStringSet("excluded." + CoverCache.digest(account), new HashSet<>(excluded)).apply();
    }

    List<String> selected(String account) {
        List<String> ids = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(preferences.getString("selection." + CoverCache.digest(account), "[]"));
            for (int i = 0; i < array.length(); i++) ids.add(array.getString(i));
        } catch (Exception ignored) { }
        return ids;
    }

    void selected(String account, List<Cover> selected) {
        if (rememberSelection) preferences.edit().putString("selection." + CoverCache.digest(account),
            new JSONArray(CollectionSelection.ids(selected)).toString()).apply();
    }
}
