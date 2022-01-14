package bar.logic;

import bar.ui.TrayUtil;
import bar.util.Util;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

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
                LOG.info("Loaded settings in {}", settingsFile.getAbsolutePath());
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
            LOG.error("error: ", e);
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
            LOG.error("error ", e);
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

    public String getStringOrNull(Setting setting) {
        if (settings.containsKey(setting.key)) {
            if (settings.get(setting.key) instanceof String) return (String) settings.get(setting.key);
            else return String.valueOf(settings.get(setting.key));
        }
        return null;
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
                    LOG.error("error ", e);
                }
            } else {
                return new Font(fontValue, Font.PLAIN, 12);
            }
        }
        return new Font("Arial", Font.PLAIN, 12);
    }

    public enum Setting {
        INPUT_WIDTH("inputWidth", 800, "input", TYPE_INT),
        INPUT_HEIGHT("inputHeight", 80, "input", TYPE_INT),
        RESULT_WIDTH("resultWidth", 700, "result", TYPE_INT),
        RESULT_HEIGHT("resultHeight", 70, "result", TYPE_INT),
        AMOUNT_RESULTS("amountResults", 6, "result", TYPE_INT),
        RESULT_MARGIN("resultMargin", 10, "result", TYPE_INT),
        INPUT_RESULT_DISTANCE("inputResultDistance", 20, "bar", TYPE_INT),
        BAR_FONT("barFont", "Comfortaa Regular", "bar", TYPE_FONT),
        ACTIVATION_DELAY("activationDelay", 250, "time", TYPE_INT),
        ACTIVATION_KEY("activationKey", 162, "key", TYPE_KEY),
        CANCEL_KEY("cancelKey", 27, "key", TYPE_KEY),
        CONFIRM_KEY("confirmKey", 13, "key", TYPE_KEY),
        MODIFY_KEY("modifyKeyMetaChar", 164, "key", TYPE_KEY),
        NEXT_RESULT_KEY("nextResultKey", 40, "key", TYPE_KEY),
        PREVIOUS_RESULT_KEY("previousResultKey", 38, "key", TYPE_KEY),
        TILE_GENERATOR_FILE_LIMIT("tileGeneratorFileLimit", 1000, "tile", TYPE_INT),
        INPUT_BAR_FONT_SIZE("inputBarFontSize", 36, "input", TYPE_INT),
        RESULT_BAR_FONT_SIZE("resultBarFontSize", 30, "result", TYPE_INT),
        BAR_FONT_BOLD_BOOL("barFontBoldBool", true, "bar", TYPE_BOOLEAN),
        INPUT_TEXT_PADDING("inputTextPadding", 4, "input", TYPE_INT),
        RESULT_TEXT_PADDING("resultTextPadding", 4, "result", TYPE_INT),
        LEFT_ARROW_KEY("leftArrowKey", 37, "key", TYPE_KEY),
        RIGHT_ARROW_KEY("rightArrowKey", 39, "key", TYPE_KEY),
        CLOUD_TIMER_USERNAME("cloudTimerUsername", null, "cloud", TYPE_STRING),
        CLOUD_TIMER_PASSWORD("cloudTimerPassword", null, "cloud", TYPE_STRING),
        CLOUD_TIMER_URL("cloudTimerUrl", null, "cloud", TYPE_STRING),
        SHOW_STARTUP_MESSAGE("showStartupMessage", true, "general", TYPE_BOOLEAN),
        CHECK_FOR_UPDATES("checkForUpdates", true, "general", TYPE_BOOLEAN),
        NULL("null", null, "null", TYPE_INT);

        public final String key;
        public final Object defaultValue;
        public final String category;
        public final int type;

        Setting(String key, Object defaultValue, String category, int type) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.category = category;
            this.type = type;
        }

        public static Setting getSetting(String key) {
            for (Setting setting : Setting.values()) {
                if (setting.key.equals(key)) return setting;
            }
            return NULL;
        }
    }

    public final static int TYPE_STRING = 1;
    public final static int TYPE_INT = 2;
    public final static int TYPE_BOOLEAN = 3;
    public final static int TYPE_FONT = 4;
    public final static int TYPE_KEY = 5;
}
