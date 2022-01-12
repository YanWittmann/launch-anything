package bar.tile;

import bar.Main;
import bar.ui.TrayUtil;
import bar.util.Util;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class TileAction {

    private JSONObject json;
    private static final Logger LOG = LoggerFactory.getLogger(TileAction.class);

    public TileAction(JSONObject json) {
        this.json = json;
    }

    public TileAction(String type, String param1, String param2) {
        this.json = new JSONObject();
        this.json.put("type", type);
        this.json.put("param1", param1);
        this.json.put("param2", param2);
    }

    public TileAction(String type, String param1) {
        this.json = new JSONObject();
        this.json.put("type", type);
        this.json.put("param1", param1);
        this.json.put("param2", (Object) null);
    }

    public TileAction(String type) {
        this.json = new JSONObject();
        this.json.put("type", type);
        this.json.put("param1", (Object) null);
        this.json.put("param2", (Object) null);
    }

    private RuntimeTileInteraction runtimeTileInteraction = null;

    public TileAction(RuntimeTileInteraction interaction) {
        this.json = new JSONObject();
        this.json.put("type", "interaction");
        runtimeTileInteraction = interaction;
    }

    public interface RuntimeTileInteraction {
        void run();
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
                    case "directory":
                        if (param1 != null) {
                            try {
                                Desktop desktop = Desktop.getDesktop();
                                File myFile = new File(param1);
                                desktop.open(myFile);
                            } catch (IOException e) {
                                TrayUtil.showError("Tile action failure: unable to open file: " + e.getMessage());
                                LOG.error("error ", e);
                            }
                        }
                        break;
                    case "url":
                        if (param1 != null) {
                            try {
                                Desktop desktop = Desktop.getDesktop();
                                desktop.browse(new URI(param1));
                            } catch (IOException | URISyntaxException e) {
                                TrayUtil.showError("Tile action failure: unable to open url: " + e.getMessage());
                                LOG.error("error ", e);
                            }
                        }
                        break;
                    case "copy":
                        if (param1 != null) {
                            Util.copyToClipboard(param1);
                        }
                        break;
                    case "interaction":
                        runtimeTileInteraction.run();
                        break;
                    case "settings":
                        if (param1 != null) {
                            switch (param1) {
                                case "webeditor":
                                    main.openSettingsWebServer(true);
                                    break;
                                case "createTile":
                                    main.createTile();
                                    break;
                                case "timeout":
                                    main.timeout(Integer.parseInt(param2));
                                    break;
                                case "update":
                                    main.checkForNewVersion();
                                    break;
                                case "restartBar":
                                    try {
                                        main.restartBar();
                                    } catch (IOException | URISyntaxException e) {
                                        TrayUtil.showError("Tile action failure: unable to restart application: " + e.getMessage());
                                        LOG.error("Unable to restart application: {}" + e.getMessage());
                                    }
                                    break;
                                case "exit":
                                    System.exit(0);
                            }
                        }
                        break;
                }
            }
        } catch (Exception e) {
            TrayUtil.showError("Tile action failure: " + e.getMessage());
            LOG.error("error ", e);
        }
    }

    public String getExampleTileLabel() {
        try {
            String type = json.optString("type");
            String param1 = json.optString("param1");

            if (type != null) {
                switch (type) {
                    case "file":
                    case "directory":
                        if (param1 != null) {
                            return TileGeneratorGenerator.fileTypeNameGenerator(new File(param1));
                        }
                        break;
                    case "url":
                        if (param1 != null) {
                            URL url = new URL(param1);
                            StringBuilder sb = new StringBuilder();
                            if (url.getHost() != null) sb.append(url.getHost());
                            if (url.getPath() != null) {
                                sb.append(" ->");
                                sb.append(url.getPath().replace("/", " ").replaceAll("\\?.*", "").replaceAll(" +", " "));
                            }
                            return sb.toString();
                        }
                        break;
                    case "copy":
                        if (param1 != null) {
                            return "COPY: " + param1;
                        }
                        break;
                }
            }
        } catch (Exception e) {
            TrayUtil.showError("Tile action failure: " + e.getMessage() + "\nAre you sure that the tile action value is valid?");
        }
        return "";
    }

    public JSONObject toJSON() {
        return json;
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

        // check if text is a url
        try {
            new URL(text);
            return "url";
        } catch (MalformedURLException ignored) {
        }

        return null;
    }

    @Override
    public String toString() {
        return "TileAction{" +
               json +
               '}';
    }

    public final static String[] ACTION_TYPES = {
            "file",
            "directory",
            "url",
            "copy",
            "settings"
    };
}
