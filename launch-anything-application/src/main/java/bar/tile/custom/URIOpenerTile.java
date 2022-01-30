package bar.tile.custom;

import bar.tile.Tile;
import bar.tile.action.TileAction;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class URIOpenerTile implements RuntimeTile {

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        if (search.contains("/") || search.contains("\\") || search.contains(".")) {
            if (search.startsWith("file://")) search = search.substring(7);
            File file = new File(search);
            if (file.exists()) {
                if (file.isDirectory()) {
                    Tile tile = new Tile("Open directory " + file.getName());
                    tile.setCategory("file");
                    tile.addAction(TileAction.getInstance("directory", file.getAbsolutePath()));
                    return Collections.singletonList(tile);
                } else {
                    Tile tile = new Tile("Open file " + file.getName());
                    tile.setCategory("file");
                    tile.addAction(TileAction.getInstance("file", file.getAbsolutePath()));
                    return Collections.singletonList(tile);
                }
            }
            try {
                URL url = new URL(search);
                if (url.getProtocol().equals("http") || url.getProtocol().equals("https")) {
                    Tile tile = new Tile("Open URL " + url);
                    tile.setCategory("url");
                    tile.addAction(TileAction.getInstance("url", url.toString()));
                    return Collections.singletonList(tile);
                }
            } catch (MalformedURLException ignored) {
            }
        }
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "URI opener";
    }

    @Override
    public String getDescription() {
        return "Enter a local path to a directory or a url";
    }

    @Override
    public String getAuthor() {
        return "Yan Wittmann";
    }

    @Override
    public String getVersion() {
        return null;
    }
}
