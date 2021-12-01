package bar.tile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TileGenerator {

    private String id, category, keywords;
    private final List<TileGeneratorGenerator> generators = new ArrayList<>();

    public TileGenerator(JSONObject json) {
        id = json.optString("id", null);
        category = json.optString("category", null);
        keywords = json.optString("keywords", null);
        JSONArray gen = json.optJSONArray("generators");
        if (gen != null) {
            for (int i = 0; i < gen.length(); i++) {
                this.generators.add(new TileGeneratorGenerator(gen.getJSONObject(i)));
            }
        }
    }

    public TileGenerator() {
        id = UUID.randomUUID().toString();
        category = "none";
    }

    public boolean isValid() {
        return id != null;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getKeywords() {
        return keywords;
    }

    public List<TileGeneratorGenerator> getGenerators() {
        return generators;
    }

    public void addGenerator(TileGeneratorGenerator generator) {
        this.generators.add(generator);
        cleanUpGenerators();
    }

    public void removeGenerator(TileGeneratorGenerator generator) {
        this.generators.remove(generator);
        cleanUpGenerators();
    }

    public void cleanUpGenerators() {
        for (int i = generators.size() - 1; i >= 0; i--) {
            boolean p1IsNull = generators.get(i).getParam1() == null || generators.get(i).getParam1().length() == 0;
            boolean p2IsNull = generators.get(i).getParam2() == null || generators.get(i).getParam2().length() == 0;
            if (p1IsNull && p2IsNull) {
                generators.remove(i);
            }
        }
    }

    public TileGeneratorGenerator findGenerator(String id) {
        for (TileGeneratorGenerator action : generators) {
            if (action.getId().equals(id)) {
                return action;
            }
        }
        return null;
    }

    public void addKeyword(String keyword) {
        keywords = keywords + " " + keyword;
        normalizeKeywords();
    }

    public void removeKeyword(String keyword) {
        keywords = (" " + keywords).replace(" " + keyword, "");
        normalizeKeywords();
    }

    public void editKeyword(String keyword, String newKeyword) {
        keywords = (" " + keywords).replace(" " + keyword, " " + newKeyword);
        normalizeKeywords();
    }

    private void normalizeKeywords() {
        keywords = keywords.replaceAll(" +", " ").trim();
    }

    public List<Tile> generateTiles() {
        List<Tile> generatedTiles = new ArrayList<>();
        for (TileGeneratorGenerator generator : generators) {
            generatedTiles.addAll(generator.generateTiles());
        }
        for (Tile tile : generatedTiles) {
            tile.setCategory(category);
            tile.addKeyword(keywords);
        }
        return generatedTiles;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("category", category);
        json.put("keywords", keywords);
        JSONArray gen = new JSONArray();
        for (TileGeneratorGenerator generator : generators) {
            gen.put(generator.toJSON());
        }
        json.put("generators", gen);
        return json;
    }
}
