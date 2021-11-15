package bar.tile;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TileGenerator {

    private final List<Tile> generatedTiles = new ArrayList<>();

    public TileGenerator(JSONObject json, boolean generate) {

    }

    public boolean isValid() {
        return true;
    }

    public JSONObject toJSON() {
        return new JSONObject();
    }
}
