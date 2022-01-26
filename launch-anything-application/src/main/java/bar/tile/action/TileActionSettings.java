package bar.tile.action;

import bar.Main;
import bar.ui.TrayUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

public class TileActionSettings extends TileAction {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private String setting;
    private int intParam;

    public TileActionSettings() {
    }

    public TileActionSettings(JSONObject json) {
        this.setting = json.optString("setting", null);
        if (this.setting == null) this.setting = json.optString("param1");
        this.intParam = json.optInt("intParam", -1);
        if (this.intParam == -1) this.intParam = json.optInt("param2", 0);
    }

    public TileActionSettings(String setting) {
        this.setting = setting;
    }

    public TileActionSettings(String setting, int intParam) {
        this.setting = setting;
        this.intParam = intParam;
    }

    @Override
    public void execute(Main main) {
        if (setting == null) {
            LOG.warn("setting is [null]");
            return;
        }
        switch (setting) {
            case "webeditor":
                main.openSettingsWebServer(true);
                break;
            case "createTile":
                main.createTile(false);
                break;
            case "timeout":
                main.timeout(intParam);
                break;
            case "restartBar":
                try {
                    main.restartBar();
                } catch (IOException | URISyntaxException e) {
                    TrayUtil.showError("Tile action failure: unable to restart application: " + e.getMessage());
                    LOG.error("Unable to restart application: {}" + e.getMessage());
                }
                break;
            case "cloud-sync":
                main.cloudSync();
                break;
            case "cloud-create-tile":
                main.createTile(true);
                break;
            case "check-for-update":
                main.checkForNewVersion();
                break;
            case "reloadPlugins":
                main.reloadPlugins();
                break;
            case "exit":
                System.exit(0);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TileActionSettings) {
            TileActionSettings other = (TileActionSettings) o;
            if (o == this) return true;
            return String.valueOf(other.setting).equals(String.valueOf(setting)) || fuzzyCompare(String.valueOf(other.setting), String.valueOf(setting));
        }
        return false;
    }

    @Override
    public boolean equalsByParams(String... params) {
        if (params.length > 0 && (fuzzyCompare(params[0], setting) || fuzzyCompare(params[0], intParam + "")))
            return true;
        if (params.length > 1 && (fuzzyCompare(params[1], setting) || fuzzyCompare(params[1], intParam + "")))
            return true;
        return false;
    }

    @Override
    protected boolean userModifyActionParameters() {
        return setting != null;
    }

    @Override
    protected String getClipboardSuggestedParameters() {
        return null;
    }

    @Override
    public String getExampleTileLabel() {
        return setting;
    }

    @Override
    public String getType() {
        return "settings";
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getType());
        json.put("setting", setting);
        json.put("intParam", intParam);
        return json;
    }

    @Override
    public String[] getParameters() {
        return new String[]{setting, String.valueOf(intParam)};
    }
}
