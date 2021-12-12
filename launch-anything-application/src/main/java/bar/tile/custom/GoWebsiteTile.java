package bar.tile.custom;

import bar.tile.Tile;
import bar.tile.TileAction;
import bar.util.Util;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class GoWebsiteTile implements RuntimeTile {

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        if (search.startsWith("go") && search.length() > 2) {
            Tile tile = new Tile("I'm Feeling Lucky!");
            tile.setCategory("runtime");
            String searchTerm = search.replaceAll("^go *", "");
            TileAction action = new TileAction("url", "https://duckduckgo.com/?q=!ducky+" + Util.urlEncode(searchTerm));
            tile.addAction(action);
            return Collections.singletonList(tile);
        }
        return Collections.emptyList();
    }

    public String getName() {
        return "Go Website";
    }

    public String getDescription() {
        return "Enter 'go' and any search term";
    }
}
