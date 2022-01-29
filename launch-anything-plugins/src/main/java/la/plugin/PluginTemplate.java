package la.plugin;

import bar.tile.Tile;
import bar.tile.action.TileAction;
import bar.tile.custom.RuntimeTile;
import bar.util.Util;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PluginTemplate implements RuntimeTile {

    public PluginTemplate() {
        info(this, "Plugin Template loaded");
    }

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        if (search.equals("template")) {
            Tile tile = new Tile("Template Plugin Tile", "Template", "", false);
            TileAction action = TileAction.getInstance(() -> {
                Util.popupMessage("Template Plugin Tile", "You executed the Template Plugin Tile!");
                info(this, "Template Plugin Tile executed");
            });
            tile.addAction(action);
            return Collections.singletonList(tile);
        }
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "Template Plugin";
    }

    @Override
    public String getDescription() {
        return "Replace this with your plugin description";
    }

    @Override
    public String getAuthor() {
        return "Yan Wittmann";
    }

    @Override
    public String getVersion() {
        return "1.0-SNAPSHOT";
    }
}
