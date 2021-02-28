import org.json.JSONObject;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class TileAction {
    private String actionType;
    private final JSONObject action;

    public TileAction(JSONObject json) {
        actionType = json.getString("type");
        action = json.getJSONObject("action");
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
            case "copyToClipboard":
                if (action.has("text")) {
                    copyString(action.getString("text"));
                }
            case "settings":
                if (action.has("setting")) {
                    String setting = action.getString("setting");
                    if(setting.equals("settings")) Main.setOpenMode(false);
                    if(setting.equals("exit")) System.exit(1);
                }
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
}
