package bar.tile;

import org.json.JSONObject;

public class TileGenerator {

    public TileGenerator(JSONObject json) {

    }

    public boolean isValid() {
        return true;
    }

    public JSONObject toJSON() {
        return new JSONObject();
    }
}
