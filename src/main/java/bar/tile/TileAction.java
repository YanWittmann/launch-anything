package bar.tile;

import bar.Main;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class TileAction {

    private JSONObject json;

    public TileAction(JSONObject json) {
        this.json = json;
    }

    public TileAction(String type, String param1, String param2) {
        this.json = new JSONObject();
        this.json.put("type", type);
        this.json.put("param1", param1);
        this.json.put("param2", param2);
    }

    public String getType() {
        return json.optString("type", null);
    }

    public String getParam1() {
        return json.optString("param1", null);
    }

    public String getParam2() {
        return json.optString("param2", null);
    }

    public void setType(String type) {
        json.put("type", type);
    }

    public void setParam1(String param1) {
        json.put("param1", param1);
    }

    public void setParam2(String param2) {
        json.put("param2", param2);
    }

    public void execute(Main main) {
        try {
            String type = json.optString("type");
            String param1 = json.optString("param1");
            String param2 = json.optString("param2");

            if (type != null) {
                switch (type) {
                    case "file":
                        if (param1 != null) {
                            try {
                                Desktop desktop = Desktop.getDesktop();
                                File myFile = new File(param1);
                                desktop.open(myFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case "url":
                        if (param1 != null) {
                            try {
                                Desktop desktop = Desktop.getDesktop();
                                desktop.browse(new URI(param1));
                            } catch (IOException | URISyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case "settingsWeb":
                        main.openSettingsWebServer();
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject toJSON() {
        return json;
    }

    public final static String[] ACTION_TYPES = {
        "file",
        "url",
        "settingsWeb"
    };
}
