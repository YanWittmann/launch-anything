import org.json.JSONObject;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;

public class TileAction {
    private String actionType;
    private JSONObject action;

    public TileAction(JSONObject json) {
        actionType = json.getString("type");
        action = json.getJSONObject("action");
    }

    public TileAction(String actionType, String parameters) {
        this.actionType = actionType;
        setParametersFromString(parameters);
    }

    public TileAction() {
        actionType = "none";
        action = new JSONObject();
    }

    public void execute() {
        System.out.println("Executing: " + actionType + " " + action);

        switch (actionType) {
            case "openFile":
                if (action.has("path")) {
                    String path = action.getString("path");
                    FileUtils.openFile(path, path.replaceAll("(^.+)(?:/|\\\\)[^\\\\/]+", "$1"));
                }
                break;
            case "openURL":
                if (action.has("url")) {
                    String path = action.getString("url");
                    try {
                        Desktop.getDesktop().browse(URI.create(path));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "copyToClipboard":
                if (action.has("text")) {
                    copyString(action.getString("text"));
                }
            case "settings":
                if (action.has("setting")) {
                    String setting = action.getString("setting");
                    if (setting.equals("settings")) Main.setOpenMode(false);
                    if (setting.equals("exit")) System.exit(1);
                }
        }
    }

    public String getActionType() {
        return actionType;
    }

    public String getParametersAsString() {
        switch (actionType) {
            case "openFile":
                if (action.has("path")) {
                    return "path=" + action.getString("path");
                }
                break;
            case "openURL":
                if (action.has("url")) {
                    return "url=" + action.getString("url");
                }
                break;
            case "copyToClipboard":
                if (action.has("text")) {
                    return "text=" + action.getString("text");
                }
            case "settings":
                if (action.has("setting")) {
                    return "setting=" + action.getString("setting");
                }
        }
        return "";
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public void setParametersFromString(String parameters) {
        action = new JSONObject();
        for (String s : parameters.split("; ?")) {
            String[] p = s.split("=", 2);
            if (p.length == 2)
                action.put(p[0], p[1]);
            else if(p.length == 1)
                action.put(p[0], "");
            else if(p.length == 0)
                action.put("", "");
        }
    }

    public JSONObject generateJSON() {
        JSONObject object = new JSONObject();
        object.put("type", actionType);
        object.put("action", action);
        return object;
    }

    public static void copyString(String text) {
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    @Override
    public String toString() {
        return actionType + " " + action;
    }
}
