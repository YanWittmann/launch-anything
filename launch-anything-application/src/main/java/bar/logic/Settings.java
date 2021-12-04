package bar.logic;

import bar.ui.TrayUtil;
import bar.util.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
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

    public JSONObject toSettingsJSON() {
        Map<String, Map<String, String>> categorySettings = new LinkedHashMap<>();
        for (Setting setting : Setting.values()) {
            categorySettings.computeIfAbsent(setting.category, k -> new LinkedHashMap<>()).put(setting.key, getString(setting));
        }
        return new JSONObject(categorySettings);
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
        for (Setting value : Setting.values()) {
            settings.putIfAbsent(value.key, value.defaultValue);
        }
    }

    public void loadTemplate(String template) {
        switch (template) {
            case "sizeNormal":
                setSettingSilent(Setting.INPUT_WIDTH, 800);
                setSettingSilent(Setting.INPUT_HEIGHT, 80);
                setSettingSilent(Setting.RESULT_WIDTH, 700);
                setSettingSilent(Setting.RESULT_HEIGHT, 70);
                setSettingSilent(Setting.AMOUNT_RESULTS, 6);
                setSettingSilent(Setting.INPUT_RESULT_DISTANCE, 20);
                setSettingSilent(Setting.INPUT_BAR_FONT_SIZE, 36);
                setSettingSilent(Setting.RESULT_BAR_FONT_SIZE, 30);
                TrayUtil.showMessage("Settings loaded: Size normal");
                break;
            case "sizeMedium":
                setSettingSilent(Setting.INPUT_WIDTH, 800);
                setSettingSilent(Setting.INPUT_HEIGHT, 64);
                setSettingSilent(Setting.RESULT_WIDTH, 700);
                setSettingSilent(Setting.RESULT_HEIGHT, 62);
                setSettingSilent(Setting.AMOUNT_RESULTS, 7);
                setSettingSilent(Setting.INPUT_RESULT_DISTANCE, 20);
                setSettingSilent(Setting.INPUT_BAR_FONT_SIZE, 36);
                setSettingSilent(Setting.RESULT_BAR_FONT_SIZE, 28);
                TrayUtil.showMessage("Settings loaded: Size medium");
                break;
            case "sizeSmall":
                setSettingSilent(Setting.INPUT_WIDTH, 700);
                setSettingSilent(Setting.INPUT_HEIGHT, 55);
                setSettingSilent(Setting.RESULT_WIDTH, 600);
                setSettingSilent(Setting.RESULT_HEIGHT, 50);
                setSettingSilent(Setting.AMOUNT_RESULTS, 8);
                setSettingSilent(Setting.INPUT_RESULT_DISTANCE, 6);
                setSettingSilent(Setting.INPUT_BAR_FONT_SIZE, 26);
                setSettingSilent(Setting.RESULT_BAR_FONT_SIZE, 24);
                TrayUtil.showMessage("Settings loaded: Size small");
                break;
        }
        save();
    }

    public void setSetting(Setting setting, Object value) {
        String key = setting.key;
        settings.put(key, value);
        TrayUtil.showMessage(Util.capitalizeWords(key.replaceAll("([A-Z])", " $1")) + " set to: " + value);
        save();
    }

    public void setSetting(String key, Object value) {
        settings.put(key, value);
        TrayUtil.showMessage(Util.capitalizeWords(key.replaceAll("([A-Z])", " $1")) + " set to: " + value);
        save();
    }

    public void setSettingSilent(Setting setting, Object value) {
        String key = setting.key;
        settings.put(key, value);
        save();
    }

    public void setSettingSilent(String key, Object value) {
        settings.put(key, value);
        save();
    }

    public int getInt(Setting setting) {
        String key = setting.key;
        if (settings.containsKey(key)) {
            if (settings.get(key) instanceof Integer) return (int) settings.get(key);
            if (settings.get(key) instanceof Long) return (int) settings.get(key);
            if (settings.get(key) instanceof Double) return (int) settings.get(key);
            if (settings.get(key) instanceof Float) return (int) settings.get(key);
            if (settings.get(key) instanceof String) return Integer.parseInt(settings.get(key) + "");
        }
        return -1;
    }

    public String getString(Setting setting) {
        return getString(setting.key);
    }

    public String getString(String key) {
        if (settings.containsKey(key)) {
            if (settings.get(key) instanceof String) return (String) settings.get(key);
            else return String.valueOf(settings.get(key));
        }
        return key + " does not exist";
    }

    public boolean getBoolean(Setting setting) {
        String key = setting.key;
        if (settings.containsKey(key)) {
            if (settings.get(key) instanceof Boolean) return (boolean) settings.get(key);
            if (settings.get(key) instanceof String) return settings.get(key).equals("true");
        }
        return false;
    }

    public Font getFont(Setting setting) {
        String key = setting.key;
        if (settings.containsKey(key)) {
            String fontValue = getString(setting);
            if (fontValue.contains("\\") || fontValue.contains("/")) {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, new File(fontValue));
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                    return font;
                } catch (FontFormatException | IOException e) {
                    e.printStackTrace();
                }
            } else {
                return new Font(fontValue, Font.PLAIN, 12);
            }
        }
        return new Font("Arial", Font.PLAIN, 12);
    }

    public enum Setting {
        INPUT_WIDTH("inputWidth", 800, "input"),
        INPUT_HEIGHT("inputHeight", 80, "input"),
        RESULT_WIDTH("resultWidth", 700, "result"),
        RESULT_HEIGHT("resultHeight", 70, "result"),
        AMOUNT_RESULTS("amountResults", 6, "result"),
        RESULT_MARGIN("resultMargin", 10, "result"),
        INPUT_RESULT_DISTANCE("inputResultDistance", 20, "bar"),
        BAR_FONT("barFont", "Comfortaa Regular", "bar"),
        ACTIVATION_DELAY("activationDelay", 250, "time"),
        ACTIVATION_KEY("activationKey", 162, "key"),
        CANCEL_KEY("cancelKey", 27, "key"),
        CONFIRM_KEY("confirmKey", 13, "key"),
        MODIFY_KEY("modifyKeyMetaChar", 164, "key"),
        NEXT_RESULT_KEY("nextResultKey", 40, "key"),
        PREVIOUS_RESULT_KEY("previousResultKey", 38, "key"),
        TILE_GENERATOR_FILE_LIMIT("tileGeneratorFileLimit", 1000, "tile"),
        INPUT_BAR_FONT_SIZE("inputBarFontSize", 36, "input"),
        RESULT_BAR_FONT_SIZE("resultBarFontSize", 30, "result"),
        BAR_FONT_BOLD_BOOL("barFontBoldBool", true, "bar"),
        INPUT_TEXT_PADDING("inputTextPadding", 4, "input"),
        RESULT_TEXT_PADDING("resultTextPadding", 4, "result"),
        LEFT_ARROW_KEY("leftArrowKey", 37, "key"),
        RIGHT_ARROW_KEY("rightArrowKey", 39, "key");

        public final String key;
        public final Object defaultValue;
        public final String category;

        Setting(String key, Object defaultValue, String category) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.category = category;
        }
    }
}
