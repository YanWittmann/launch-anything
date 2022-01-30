package la.plugin;

import bar.Main;
import bar.tile.action.TileAction;
import bar.util.Util;
import org.json.JSONObject;

public class TileActionPlugin extends TileAction {

    private String text;

    public TileActionPlugin() {
    }

    public TileActionPlugin(JSONObject json) {
        this.text = json.optString("text", null);
        if (this.text == null) this.text = json.optString("param1");
    }

    @Override
    public String getType() {
        return "popup";
    }

    @Override
    public void execute(Main main) {
        Util.popupMessage("Custom Tile Action", text);
    }

    @Override
    public String getExampleTileLabel() {
        return "Popup " + text;
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

    @Override
    public boolean equals(Object o) {
        if (o instanceof TileActionPlugin) {
            TileActionPlugin other = (TileActionPlugin) o;
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

        String newText = Util.popupTextInput("Tile Action", "Enter the text to show in a popup", suggested);

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
}
