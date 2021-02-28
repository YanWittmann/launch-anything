import org.json.JSONObject;

import java.io.File;

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
                if(action.has("path")) {
                    String path = action.getString("path");
                    FileUtils.openFile(path, path.replaceAll("(^.+)(?:/|\\\\)[^\\\\/]+", "$1"));
                }
        }
    }

    public JSONObject generateJSON() {
        JSONObject object = new JSONObject();
        object.put("type", actionType);
        object.put("action", action);
        return object;
    }
}
