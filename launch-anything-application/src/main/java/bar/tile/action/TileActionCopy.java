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

public class TileActionCopy extends TileAction {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private String text;

    public TileActionCopy() {
    }

    public TileActionCopy(JSONObject json) {
        this.text = json.optString("text", null);
        if (this.text == null) this.text = json.optString("param1");
    }

    public TileActionCopy(String text) {
        this.text = text;
    }

    @Override
    public void execute(Main main) {
        if (text == null) {
            LOG.warn("text is [null]");
            return;
        }
        Util.copyToClipboard(text);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TileActionCopy) {
            TileActionCopy other = (TileActionCopy) o;
            return other.text.equals(text) || fuzzyCompare(other.text, text);
        }
        return false;
    }

    @Override
    public boolean equalsByParams(String... params) {
        return params.length == 1 && fuzzyCompare(params[0], text);
    }

    @Override
    protected boolean userModifyActionParameters() {
        String suggested = getClipboardSuggestedParameters();
        if (suggested == null) suggested = text;

        String newText = Util.popupTextInput("Tile Action", "Enter the text to copy", suggested);

        if (newText != null && !newText.isEmpty()) {
            text = newText;
            return true;
        }

        return false;
    }

    @Override
    protected String getClipboardSuggestedParameters() {
        return Util.getClipboardText();
    }

    @Override
    public String getExampleTileLabel() {
        return "Copy " + text;
    }

    @Override
    public String getType() {
        return "copy";
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getType());
        json.put("text", text);
        return json;
    }

    @Override
    public String[] getParameters() {
        return new String[]{text};
    }
}
