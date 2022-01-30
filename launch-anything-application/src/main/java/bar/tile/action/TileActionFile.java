package bar.tile.action;

import bar.Main;
import bar.tile.TileGeneratorGenerator;
import bar.ui.TrayUtil;
import bar.util.Util;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class TileActionFile extends TileAction {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private String path;

    public TileActionFile() {
    }

    public TileActionFile(JSONObject json) {
        this.path = json.optString("path", null);
        if (this.path == null) this.path = json.optString("param1");
    }

    public TileActionFile(String path) {
        this.path = path;
    }

    @Override
    public void execute(Main main) {
        try {
            Desktop desktop = Desktop.getDesktop();
            File myFile = new File(path);
            desktop.open(myFile);
        } catch (IOException e) {
            TrayUtil.showError("Tile action failure: unable to open file: " + e.getMessage());
            LOG.error("error ", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TileActionFile) {
            TileActionFile other = (TileActionFile) o;
            if (o == this) return true;
            return other.path.equals(path) || fuzzyCompare(other.path, path);
        }
        return false;
    }

    @Override
    public boolean equalsByParams(String... params) {
        return params.length == 1 && fuzzyCompare(params[0], path);
    }

    @Override
    protected boolean userModifyActionParameters() {
        String suggested = getClipboardSuggestedParameters();
        File file;

        if (suggested != null) file = Util.pickFile(new File(suggested), null);
        else file = Util.pickFile(null);

        if (file != null) {
            if (!file.exists()) TrayUtil.showWarning("File does not exist: " + file.getAbsolutePath());
            path = file.getAbsolutePath();
            return true;
        }

        return false;
    }

    @Override
    protected String getClipboardSuggestedParameters() {
        try {
            File file = new File(Util.getClipboardText());
            if (file.exists() && file.isFile()) return file.getAbsolutePath();
            return null;
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public String getExampleTileLabel() {
        return TileGeneratorGenerator.fileTypeNameGenerator(new File(path));
    }

    @Override
    public String getType() {
        return "file";
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getType());
        json.put("path", path);
        return json;
    }

    @Override
    public String[] getParameters() {
        return new String[]{path};
    }
}
