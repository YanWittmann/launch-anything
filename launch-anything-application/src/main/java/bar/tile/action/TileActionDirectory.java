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

public class TileActionDirectory extends TileAction {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private String path;

    public TileActionDirectory() {
    }

    public TileActionDirectory(JSONObject json) {
        this.path = json.optString("path", null);
        if (this.path == null) this.path = json.optString("param1");
    }

    public TileActionDirectory(String path) {
        this.path = path;
    }

    @Override
    public void execute(Main main) {
        if (path == null) {
            LOG.warn("path is [null]");
            return;
        }

        try {
            openDir(new File(path));
        } catch (IOException e) {
            TrayUtil.showError("Tile action failure: unable to open directory: " + e.getMessage());
            LOG.error("unable to open directory: " + path, e);
        }
    }

    public static void openDir(File dir) throws IOException {
        Desktop desktop = Desktop.getDesktop();
        desktop.open(dir);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TileActionDirectory) {
            TileActionDirectory other = (TileActionDirectory) o;
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
        if (suggested != null) Util.setPreviousFile(new File(suggested));

        File file = Util.pickDirectory();

        if (file != null) {
            path = file.getAbsolutePath();
            return true;
        }

        return false;
    }

    @Override
    protected String getClipboardSuggestedParameters() {
        try {
            File file = new File(Util.getClipboardText());
            if (file.exists() && file.isDirectory()) return file.getAbsolutePath();
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
        return "directory";
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
