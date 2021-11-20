package bar.logic;

import bar.util.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;

public class Settings {

    private final Map<String, Object> settings = new LinkedHashMap<>();
    private File settingsFile;

    public Settings() {
        findSettingsFile();
        if (settingsFile == null) reset();
        else readSettingsFromFile();

        Util.setSettings(this);
    }

    private final static String[] possibleSettingsFiles = {
            "settings.json",
            "res/settings.json",
            "../settings.json",
            "../res/settings.json"
    };

    private void findSettingsFile() {
        for (String possibleSettingsFile : possibleSettingsFiles) {
            File candidate = new File(possibleSettingsFile).getAbsoluteFile();
            if (candidate.exists()) {
                settingsFile = candidate;
                return;
            }
        }
        settingsFile = null;
    }

    private void readSettingsFromFile() {
        try {
            StringJoiner fileContent = new StringJoiner("\n");
            Scanner reader = new Scanner(settingsFile);
            while (reader.hasNextLine()) {
                fileContent.add(reader.nextLine());
            }
            reader.close();
            settings.putAll(new JSONObject(fileContent.toString()).toMap());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public JSONObject toJSON() {
        return new JSONObject(settings);
    }

    public void save() {
        try {
            settingsFile.getParentFile().mkdirs();
            FileWriter myWriter = new FileWriter(settingsFile);
            myWriter.write(new JSONObject(settings).toString());
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        settings.put(ACTIVATION_DELAY, 250);
        settings.put(ACTIVATION_KEY, 162);
        settings.put(CANCEL_KEY, 27);
        settings.put(CONFIRM_KEY, 13);
        settings.put(NEXT_RESULT_KEY, 40);
        settings.put(PREVIOUS_RESULT_KEY, 38);
        settings.put(RECURSION_LIMIT, 100);
        settingsFile = new File("res/settings.json");
    }

    public boolean hasSetting(String key) {
        return settings.containsKey(key);
    }

    public void setSetting(String key, Object value) {
        settings.put(key, value);
        save();
    }

    public int getInt(String key) {
        if (settings.containsKey(key)) {
            if (settings.get(key) instanceof Integer) return (int) settings.get(key);
            if (settings.get(key) instanceof Long) return (int) settings.get(key);
            if (settings.get(key) instanceof Double) return (int) settings.get(key);
            if (settings.get(key) instanceof Float) return (int) settings.get(key);
            if (settings.get(key) instanceof String) return Integer.parseInt(settings.get(key) + "");
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
    public final static String ACTIVATION_DELAY = "activationDelay";
    public final static String ACTIVATION_KEY = "activationKey";
    public final static String CANCEL_KEY = "cancelKey";
    public final static String CONFIRM_KEY = "confirmKey";
    public final static String NEXT_RESULT_KEY = "nextResultKey";
    public final static String PREVIOUS_RESULT_KEY = "previousResultKey";
    public final static String RECURSION_LIMIT = "recursionLimit";
}
