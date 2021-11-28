package bar.logic;

import bar.ui.TrayUtil;
import bar.util.Util;
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
        if (settingsFile == null) {
            reset(false);
        } else {
            readSettingsFromFile();
            fillRequiredValues();
        }


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
                System.out.println("Loaded settings in " + settingsFile.getAbsolutePath());
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
            TrayUtil.showError("Unable to read settings file: " + e.getMessage());
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
            TrayUtil.showError("Unable to save settings file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reset(boolean notify) {
        settings.clear();
        fillRequiredValues();
        settingsFile = new File("res/settings.json");
        if (notify) {
            TrayUtil.showMessage("Settings reset!");
        }
    }

    private void fillRequiredValues() {
        settings.putIfAbsent(INPUT_WIDTH, 800);
        settings.putIfAbsent(INPUT_HEIGHT, 80);
        settings.putIfAbsent(RESULT_WIDTH, 700);
        settings.putIfAbsent(RESULT_HEIGHT, 70);
        settings.putIfAbsent(AMOUNT_RESULTS, 6);
        settings.putIfAbsent(INPUT_RESULT_DISTANCE, 20);
        settings.putIfAbsent(BAR_FONT, "Comfortaa Regular");
        settings.putIfAbsent(ACTIVATION_DELAY, 250);
        settings.putIfAbsent(ACTIVATION_KEY, 162);
        settings.putIfAbsent(CANCEL_KEY, 27);
        settings.putIfAbsent(CONFIRM_KEY, 13);
        settings.putIfAbsent(MODIFY_KEY, 164);
        settings.putIfAbsent(NEXT_RESULT_KEY, 40);
        settings.putIfAbsent(PREVIOUS_RESULT_KEY, 38);
        settings.putIfAbsent(TILE_GENERATOR_FILE_LIMIT, 1000);
        settings.putIfAbsent(INPUT_BAR_FONT_SIZE, 36);
        settings.putIfAbsent(RESULT_BAR_FONT_SIZE, 30);
    }

    public void loadTemplate(String template) {
        switch (template) {
            case "sizeNormal":
                settings.put(INPUT_WIDTH, 800);
                settings.put(INPUT_HEIGHT, 80);
                settings.put(RESULT_WIDTH, 700);
                settings.put(RESULT_HEIGHT, 70);
                settings.put(AMOUNT_RESULTS, 6);
                settings.put(INPUT_RESULT_DISTANCE, 20);
                settings.put(INPUT_BAR_FONT_SIZE, 36);
                settings.put(RESULT_BAR_FONT_SIZE, 30);
                TrayUtil.showMessage("Settings loaded: Size normal");
                break;
            case "sizeMedium":
                settings.put(INPUT_WIDTH, 800);
                settings.put(INPUT_HEIGHT, 64);
                settings.put(RESULT_WIDTH, 700);
                settings.put(RESULT_HEIGHT, 62);
                settings.put(AMOUNT_RESULTS, 7);
                settings.put(INPUT_RESULT_DISTANCE, 20);
                settings.put(INPUT_BAR_FONT_SIZE, 36);
                settings.put(RESULT_BAR_FONT_SIZE, 28);
                TrayUtil.showMessage("Settings loaded: Size medium");
                break;
            case "sizeSmall":
                settings.put(INPUT_WIDTH, 700);
                settings.put(INPUT_HEIGHT, 55);
                settings.put(RESULT_WIDTH, 600);
                settings.put(RESULT_HEIGHT, 50);
                settings.put(AMOUNT_RESULTS, 8);
                settings.put(INPUT_RESULT_DISTANCE, 6);
                settings.put(INPUT_BAR_FONT_SIZE, 26);
                settings.put(RESULT_BAR_FONT_SIZE, 24);
                TrayUtil.showMessage("Settings loaded: Size small");
                break;
        }
        save();
    }

    public void setSetting(String key, Object value) {
        settings.put(key, value);
        TrayUtil.showMessage(Util.capitalizeWords(key.replaceAll("([A-Z])", " $1")) + " set to: " + value);
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
    public final static String MODIFY_KEY = "modifyKeyMetaChar";
    public final static String NEXT_RESULT_KEY = "nextResultKey";
    public final static String PREVIOUS_RESULT_KEY = "previousResultKey";
    public final static String TILE_GENERATOR_FILE_LIMIT = "tileGeneratorFileLimit";
    public final static String INPUT_BAR_FONT_SIZE = "inputBarFontSize";
    public final static String RESULT_BAR_FONT_SIZE = "resultBarFontSize";
}
