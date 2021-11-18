package bar.tile;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TileGenerator {

    public TileGenerator(JSONObject json, boolean generate) {

    }

    public List<Tile> generateTiles() {
        return new ArrayList<>();
    }

    public boolean isValid() {
        return true;
    }

    public JSONObject toJSON() {
        return new JSONObject();
    }
}
