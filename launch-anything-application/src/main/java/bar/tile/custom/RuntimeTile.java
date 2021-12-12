package bar.tile.custom;

import bar.tile.Tile;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public interface RuntimeTile {
    List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated);

    String getName();

    String getDescription();
}
