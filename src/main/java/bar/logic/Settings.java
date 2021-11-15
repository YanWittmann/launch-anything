package bar.logic;

import java.util.LinkedHashMap;
import java.util.Map;

public class Settings {

    private final Map<String, Object> settings = new LinkedHashMap<>();

    public Settings() {
        reset();
    }

    public void reset() {
        settings.clear();
        settings.put(INPUT_WIDTH, 800);
        settings.put(INPUT_HEIGHT, 80);
        settings.put(RESULT_WIDTH, 700);
        settings.put(RESULT_HEIGHT, 70);
        settings.put(AMOUNT_RESULTS, 6);
        settings.put(RESULT_MARGIN, 10);
        settings.put(INPUT_RESULT_DISTANCE, 20);
        settings.put(BAR_FONT, "Comfortaa Regular");
    }

    public boolean hasSetting(String key) {
        return settings.containsKey(key);
    }

    public void setSetting(String key, Object value) {
        settings.put(key, value);
    }

    public int getInt(String key) {
        if (settings.containsKey(key)) {
            if (settings.get(key) instanceof Integer) return (int) settings.get(key);
            if (settings.get(key) instanceof Long) return (int) settings.get(key);
            if (settings.get(key) instanceof Double) return (int) settings.get(key);
            if (settings.get(key) instanceof Float) return (int) settings.get(key);
        }
        return -1;
    }

    public double getDouble(String key) {
        if (settings.containsKey(key)) {
            if (settings.get(key) instanceof Integer) return (double) settings.get(key);
            if (settings.get(key) instanceof Long) return (double) settings.get(key);
            if (settings.get(key) instanceof Double) return (double) settings.get(key);
            if (settings.get(key) instanceof Float) return (double) settings.get(key);
        }
        return -1;
    }

    public String getString(String key) {
        if (settings.containsKey(key)) {
            if (settings.get(key) instanceof String) return (String) settings.get(key);
            else return String.valueOf(settings.get(key));
        }
        return key + " does not exist";
    }

    public final static String INPUT_WIDTH = "inputWidth";
    public final static String INPUT_HEIGHT = "inputHeight";
    public final static String RESULT_WIDTH = "resultWidth";
    public final static String RESULT_HEIGHT = "resultHeight";
    public final static String AMOUNT_RESULTS = "amountResults";
    public final static String RESULT_MARGIN = "resultMargin";
    public final static String INPUT_RESULT_DISTANCE = "inputResultDistance";
    public final static String BAR_FONT = "barFont";
}
