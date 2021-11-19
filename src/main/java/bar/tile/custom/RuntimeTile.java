package bar.tile.custom;

import bar.tile.Tile;

import java.util.List;

public interface RuntimeTile {
    List<Tile> generateTiles(String search);
}
