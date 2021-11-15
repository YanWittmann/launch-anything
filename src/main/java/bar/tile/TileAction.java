package bar.tile;

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

    public void execute() {
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
            }
        }
    }

    public JSONObject toJSON() {
        return json;
    }
}
