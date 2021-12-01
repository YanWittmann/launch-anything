package bar.tile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;

public class TileCategory {

    private String label;
    private Color color;

    public TileCategory(JSONObject json) {
        this.label = json.optString("label", null);
        JSONArray color = json.optJSONArray("color");
        if (color != null && color.length() == 3) {
            this.color = new Color(color.optInt(0, 0), color.optInt(1, 0), color.optInt(2, 0));
        }
    }

    public TileCategory(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public Color getColor() {
        return color;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isValid() {
        return label != null && color != null && label.length() > 0 && !label.equals("null");
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("label", label);
        json.put("color", new JSONArray().put(color.getRed()).put(color.getGreen()).put(color.getBlue()));
        return json;
    }
}
