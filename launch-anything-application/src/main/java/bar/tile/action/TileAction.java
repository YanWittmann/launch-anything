package bar.tile.action;

import bar.Main;
import bar.util.Util;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class TileAction {

    public TileAction() {
    }

    public abstract String getType();

    public abstract void execute(Main main);

    public abstract String getExampleTileLabel();

    public abstract JSONObject toJSON();

    public abstract String[] getParameters();

    @Override
    public abstract boolean equals(Object o);

    public abstract boolean equalsByParams(String... params);

    protected boolean fuzzyCompare(String s1, String s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        if (s1.equalsIgnoreCase(s2)) return true;
        if (s1.replaceAll("[^a-zA-Z0-9]", "").equalsIgnoreCase(s2.replaceAll("[^a-zA-Z0-9]", ""))) return true;
        return false;
    }

    public static String getActionFromSnippet(String text) {
        if (text == null) return null;

        // check if text is a file
        try {
            File file = new File(text);
            if (file.exists()) {
                if (file.isDirectory()) return "directory";
                return "file";
            }
        } catch (Exception ignored) {
        }

        // check if text is an url
        try {
            new URL(text);
            return "url";
        } catch (MalformedURLException ignored) {
        }

        return null;
    }

    public TileAction userModifyAction() {
        String editType = Util.popupChooseButton("LaunchAnything", "Do you want to modify the action type or only the parameters?", new String[]{"Parameters", "Type and Parameters", "Cancel"});
        if ("Parameters".equals(editType)) {
            userModifyActionParameters();
        } else if ("Type and Parameters".equals(editType)) {
            return getInstanceUser();
        }
        return null;
    }

    protected abstract boolean userModifyActionParameters();

    protected abstract String getClipboardSuggestedParameters();

    public static TileAction getInstance(JSONObject json) {
        if (json == null) return null;
        String type = json.optString("type", null);
        if (type == null) return null;
        switch (type) {
            case "file":
                return new TileActionFile(json);
            case "directory":
                return new TileActionDirectory(json);
            case "url":
                return new TileActionURL(json);
            case "copy":
                return new TileActionCopy(json);
            case "settings":
                return new TileActionSettings(json);
        }
        return null;
    }

    public static TileAction getInstance(String type, String... parameters) {
        if (type == null) return null;
        TileAction action = null;
        switch (type) {
            case "file":
                if (parameters.length == 1) action = new TileActionFile(parameters[0]);
                else action = new TileActionFile();
                break;
            case "directory":
                if (parameters.length == 1) action = new TileActionDirectory(parameters[0]);
                else action = new TileActionDirectory();
                break;
            case "url":
                if (parameters.length == 1) action = new TileActionURL(parameters[0]);
                else action = new TileActionURL();
                break;
            case "copy":
                if (parameters.length == 1) action = new TileActionCopy(parameters[0]);
                else action = new TileActionCopy();
                break;
            case "settings":
                if (parameters.length == 1) {
                    action = new TileActionSettings(parameters[0]);
                } else if (parameters.length == 2) {
                    if (parameters[1].matches("-?\\d+")) {
                        action = new TileActionSettings(parameters[0], Integer.parseInt(parameters[1]));
                    } else {
                        action = new TileActionSettings(parameters[0]);
                    }
                } else {
                    action = new TileActionSettings();
                }
                break;
        }
        return action;
    }

    public static TileAction getInstanceUser() {
        String actionTypeFromSnippet = TileAction.getActionFromSnippet(Util.getClipboardText());
        String actionType = Util.popupDropDown("Tile Action", "What type of action do you want to create?", TileAction.ACTION_TYPES, actionTypeFromSnippet);
        if (actionType == null) return null;
        return getInstanceUser(actionType);
    }

    public static TileAction getInstanceUser(String type) {
        if (type == null) return null;
        TileAction action = getInstance(type);
        if (action != null) {
            action.userModifyActionParameters();
        }
        return action;
    }

    public interface RuntimeTileInteraction {
        void run();
    }

    public static TileAction getInstance(RuntimeTileInteraction interaction) {
        return new TileActionRuntimeInteraction(interaction);
    }

    public final static String[] ACTION_TYPES = {
            "file",
            "directory",
            "url",
            "copy",
            "settings"
    };
}
