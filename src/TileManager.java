import org.json.JSONArray;
import org.json.JSONObject;
import yanwittmann.types.File;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class TileManager {
    private final static String TILES_JSON = "tiles.json";
    private final String directory;
    private final ArrayList<Tile> tiles = new ArrayList<>();
    private final ArrayList<Tile> generatedTiles = new ArrayList<>();
    private final ArrayList<TileGenerator> tileGenerators = new ArrayList<>();
    private ArrayList<Pair<String, String>> categories = new ArrayList<>();
    private final ArrayList<Pair<String, String>> generatedCategories = new ArrayList<>();

    public TileManager(String directory) {
        this.directory = directory;
    }

    public void read(boolean generateTiles) throws IOException {
        String[] tiles = new File(directory + TILES_JSON).readToArray();
        if (tiles == null || tiles.length == 0) {
            System.out.println("Unable to read tile data!");
            return;
        }
        StringBuilder inputJSONbuilder = new StringBuilder();
        for (String tile : tiles) inputJSONbuilder.append(tile.trim());
        try {
            JSONObject tileData = new JSONObject(inputJSONbuilder.toString());
            JSONArray tilesArray = tileData.getJSONArray("tiles");
            JSONArray categoriesArray = tileData.getJSONArray("categories");
            JSONArray tileGeneratorsArray = tileData.getJSONArray("tileGenerators");

            for (int i = 0; i < tilesArray.length(); i++)
                this.tiles.add(new Tile(tilesArray.getJSONObject(i)));
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject cat = categoriesArray.getJSONObject(i);
                this.categories.add(new Pair<>(cat.getString("name"), cat.getString("color")));
            }
            for (int i = 0; i < tileGeneratorsArray.length(); i++) {
                TileGenerator generator = new TileGenerator(tileGeneratorsArray.getJSONObject(i));
                if (generateTiles)
                    generatedTiles.addAll(generator.generateTiles());
                this.tileGenerators.add(generator);
            }

        } catch (Exception e) {
            System.out.println("Invalid JSON tile data!");
            e.printStackTrace();
            Popup.error("LaunchAnything", "Unable to import data:\nInvalid JSON tile data!\n" + e.toString());
        }
        System.out.println("Imported:\n" +
                this.tiles.size() + " custom tile" + (this.tiles.size() != 1 ? "s" : "") + "\n" +
                this.tileGenerators.size() + " tile generator" + (this.tileGenerators.size() != 1 ? "s" : "") + "\n" +
                this.generatedTiles.size() + " generated tile" + (this.generatedTiles.size() != 1 ? "s" : "") + "\n" +
                this.categories.size() + (this.categories.size() != 1 ? " categories" : " category"));
    }

    public ArrayList<Tile> search(String search) {
        ArrayList<Tile> result = new ArrayList<>();
        for (Tile tile : tiles)
            if (tile.matchesSearch(search))
                result.add(tile);
        for (Tile tile : generatedTiles)
            if (tile.matchesSearch(search))
                result.add(tile);
        return result;
    }

    private HashMap<String, Color> catCol = new HashMap<>();

    public Color getColorForCategory(String cat) {
        if (catCol.containsKey(cat)) {
            return catCol.get(cat);
        } else {
            for (Pair<String, String> category : categories)
                catCol.put(category.getLeft(), LaunchBar.hex2Rgb(category.getRight()));
            for (Pair<String, String> category : generatedCategories)
                catCol.put(category.getLeft(), LaunchBar.hex2Rgb(category.getRight()));
            if (catCol.containsKey(cat))
                return catCol.get(cat);
            else return new Color(0, 0, 0);
        }
    }

    public JSONArray generateTilesJSON() {
        JSONArray tiles = new JSONArray();
        this.tiles.stream().map(Tile::generateJSON).forEach(tiles::put);
        return tiles;
    }

    public JSONArray generateCategoriesJSON() {
        JSONArray cat = new JSONArray();
        for (Pair<String, String> category : categories) {
            JSONObject obj = new JSONObject();
            obj.put("name", category.getLeft());
            obj.put("color", category.getRight());
            cat.put(obj);
        }
        return cat;
    }

    public JSONArray generateGeneratorsJSON() {
        JSONArray tiles = new JSONArray();
        for (TileGenerator tileGenerator : tileGenerators) {
            tiles.put(tileGenerator.generateJSON());
        }
        return tiles;
    }

    public void generateCategory(String name, String color) {
        generatedCategories.add(new Pair<>(name, color));
    }

    public void generateTile(String id, String label, String category, String keywords, JSONArray actions) {
        JSONObject tile = new JSONObject();
        tile.put("id", id);
        tile.put("label", label);
        tile.put("category", category);
        tile.put("keywords", keywords);
        tile.put("lastExecuted", "0");
        tile.put("actions", actions);
        generatedTiles.add(new Tile(tile));
    }

    public void generateSettingTile(String id, String label, String category, String keywords, String action) {
        JSONArray settingsAction = new JSONArray();
        JSONObject settingsActionObject = new JSONObject();
        JSONObject settingsActionObjectParameters = new JSONObject();
        settingsActionObjectParameters.put("setting", action);
        settingsActionObject.put("action", settingsActionObjectParameters);
        settingsActionObject.put("type", "settings");
        settingsAction.put(settingsActionObject);
        generateTile(id, label, category, keywords, settingsAction);
    }

    public void save() throws IOException {
        JSONObject object = new JSONObject();
        object.put("tiles", generateTilesJSON());
        object.put("categories", generateCategoriesJSON());
        object.put("tileGenerators", generateGeneratorsJSON());
        new File(directory + TILES_JSON).write(object.toString());
    }

    public void setCategories(ArrayList<Pair<String, String>> categories) {
        this.categories = categories;
    }

    public ArrayList<Tile> getNonGeneratedTiles() {
        return tiles;
    }

    public ArrayList<TileGenerator> getTileGenerators() {
        return tileGenerators;
    }

    public ArrayList<Pair<String, String>> getCategories() {
        return categories;
    }
}
