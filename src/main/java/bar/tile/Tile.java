package bar.tile;

import org.json.JSONObject;

public class Tile {

    public Tile(JSONObject json) {

    }

    public boolean isValid() {
        return true;
    }

    public JSONObject toJSON() {
        return new JSONObject();
    }
}
