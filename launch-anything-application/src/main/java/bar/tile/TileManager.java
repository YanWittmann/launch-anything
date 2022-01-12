package bar.tile;

import bar.Main;
import bar.tile.custom.*;
import bar.ui.TrayUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class TileManager {

    private final List<Tile> tiles = new ArrayList<>();
    private final List<RuntimeTile> runtimeTiles = new ArrayList<>();
    private final List<Tile> generatedTiles = new ArrayList<>();
    private final List<TileGenerator> tileGenerators = new ArrayList<>();
    private final List<InputEvaluatedListener> onInputEvaluatedListeners = new ArrayList<>();
    private final List<TileCategory> categories = new ArrayList<>();
    private final List<String> disabledRuntimeTiles = new ArrayList<>();
    private File tileFile;
    private boolean isFirstLaunch = false;
    private static final Logger LOG = LoggerFactory.getLogger(TileManager.class);

    public TileManager() {
        findSettingsFile();
        if (tileFile == null) {
            tileFile = new File("res/tiles.json");
            generateDefaultTiles();
            createSettingsTiles();
            isFirstLaunch = true;
        } else {
            readTilesFromFile();
        }
        LOG.info("Is first launch: [{}]", isFirstLaunch);
        addRuntimeTiles();
    }

    public boolean isFirstLaunch() {
        return isFirstLaunch;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentFuture = null;
    private final AtomicReference<Long> lastInputEvaluated = new AtomicReference<>(System.currentTimeMillis());

    public void evaluateUserInput(String input) {
        try {
            if (currentFuture != null) {
                currentFuture.cancel(true);
            }
            if (input.length() <= 1) {
                setEvaluationResults(new ArrayList<>());
            } else {
                currentFuture = evaluate(input);
            }
        } catch (Exception e) {
            LOG.error("error ", e);
        }
    }

    private void setEvaluationResults(List<Tile> tiles) {
        onInputEvaluatedListeners.forEach(listener -> listener.onInputEvaluated(tiles));
    }

    private Future<?> evaluate(String input) {
        lastInputEvaluated.set(System.currentTimeMillis());
        return executor.submit(() -> {
            List<Tile> matchingTiles = tiles.stream()
                    .filter(tile -> tile.matchesSearch(input))
                    .sorted(Comparator.comparing(Tile::getLastActivated).reversed())
                    .collect(Collectors.toList());

            generatedTiles.stream()
                    .filter(tile -> tile.matchesSearch(input))
                    .sorted(Comparator.comparing(Tile::getLastActivated).reversed())
                    .forEach(matchingTiles::add);

            runtimeTiles.stream()
                    .map(runtimeTile -> runtimeTile.generateTiles(input, lastInputEvaluated))
                    .forEach(matchingTiles::addAll);

            setEvaluationResults(matchingTiles);
            return null;
        });
    }

    private final static String[] possibleTilesFiles = {
            "tiles.json",
            "res/tiles.json",
            "../tiles.json",
            "../res/tiles.json"
    };

    private void findSettingsFile() {
        for (String possibleSettingsFile : possibleTilesFiles) {
            File candidate = new File(possibleSettingsFile).getAbsoluteFile();
            if (candidate.exists()) {
                tileFile = candidate;
                LOG.info("Loaded tiles in {}", tileFile.getAbsolutePath());
                return;
            }
        }
        tileFile = null;
    }

    private void readTilesFromFile() {
        try {
            StringBuilder fileContent = new StringBuilder();
            Scanner reader = new Scanner(tileFile);
            while (reader.hasNextLine()) {
                fileContent.append(reader.nextLine().trim());
            }
            reader.close();

            JSONObject tilesRoot = new JSONObject(fileContent.toString());
            loadTilesFromJson(tilesRoot);
        } catch (FileNotFoundException e) {
            TrayUtil.showError("Something went wrong while reading the tiles: " + e.getMessage());
            LOG.error("error ", e);
        }
    }

    public void loadTilesFromJson(JSONObject tilesRoot) {
        tiles.clear();
        tileGenerators.clear();
        categories.clear();

        JSONArray tilesArray = tilesRoot.optJSONArray("tiles");
        if (tilesArray != null) {
            for (int i = 0; i < tilesArray.length(); i++) {
                JSONObject tileJson = tilesArray.optJSONObject(i);
                if (tileJson == null) continue;
                Tile tile = new Tile(tileJson);
                if (tile.isValid()) tiles.add(tile);
            }
        }

        JSONArray tileGeneratorsArray = tilesRoot.optJSONArray("tile-generators");
        if (tileGeneratorsArray != null) {
            for (int i = 0; i < tileGeneratorsArray.length(); i++) {
                JSONObject tileGeneratorJson = tileGeneratorsArray.optJSONObject(i);
                if (tileGeneratorJson == null) continue;
                TileGenerator tileGenerator = new TileGenerator(tileGeneratorJson);
                if (tileGenerator.isValid()) tileGenerators.add(tileGenerator);
            }
        }

        JSONArray categoriesArray = tilesRoot.optJSONArray("categories");
        if (categoriesArray != null) {
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryJson = categoriesArray.optJSONObject(i);
                if (categoryJson == null) continue;
                TileCategory category = new TileCategory(categoryJson);
                if (category.isValid()) {
                    categories.add(category);
                }
            }
        }

        JSONArray disabledRuntimeTilesArray = tilesRoot.optJSONArray("disabled-runtime-tiles");
        if (disabledRuntimeTilesArray != null) {
            for (int i = 0; i < disabledRuntimeTilesArray.length(); i++) {
                String tile = disabledRuntimeTilesArray.optString(i);
                disabledRuntimeTiles.add(tile);
            }
        }

        regenerateGeneratedTiles();
        createSettingsTiles();

        LOG.info("Loaded [{}] tile(s), [{}] tile generator(s) and [{}] category/ies.", tiles.size(), tileGenerators.size(), categories.size());
    }

    public void regenerateGeneratedTiles() {
        new Thread(() -> {
            generatedTiles.clear();
            for (TileGenerator tileGenerator : tileGenerators) {
                generatedTiles.addAll(tileGenerator.generateTiles());
            }
            LOG.info("Done generating [{}] tile(s).", generatedTiles.size());
        }).start();
    }

    public void addCategory(TileCategory category) {
        categories.add(category);
    }

    public void removeCategory(TileCategory category) {
        for (Tile tile : tiles) {
            if (tile.getCategory() != null && tile.getCategory().equals(category.getLabel())) {
                tile.setCategory(null);
            }
        }
        categories.remove(category);
    }

    public List<TileCategory> getCategories() {
        return categories;
    }

    public TileCategory findCategory(String label) {
        for (TileCategory category : categories) {
            if (category.getLabel().equals(label)) return category;
        }
        return null;
    }

    public TileGenerator findTileGenerator(String id) {
        for (TileGenerator tileGenerator : tileGenerators) {
            if (tileGenerator.getId().equals(id)) return tileGenerator;
        }
        return null;
    }

    public void addTileGenerator(TileGenerator tileGenerator) {
        tileGenerators.add(tileGenerator);
        regenerateGeneratedTiles();
    }

    public void removeTileGenerator(TileGenerator tileGenerator) {
        tileGenerators.remove(tileGenerator);
        regenerateGeneratedTiles();
    }

    public void addTile(Tile tile) {
        tiles.add(tile);
    }

    public Tile findTile(String tileId) {
        return tiles.stream().filter(tile -> tile.getId().equals(tileId)).findFirst().orElse(null);
    }

    public void removeTile(Tile tile) {
        tiles.remove(tile);
    }

    public void save() {
        try {
            tileFile.getParentFile().mkdirs();
            FileWriter myWriter = new FileWriter(tileFile);

            myWriter.write(toJSON().toString());
            myWriter.close();
        } catch (IOException e) {
            TrayUtil.showError("Unable to save tiles: " + e.getMessage());
            LOG.error("error ", e);
        }
    }

    public JSONObject toJSON() {
        JSONArray tilesArray = new JSONArray();
        for (Tile tile : tiles) {
            if (tile.isExportable())
                tilesArray.put(tile.toJSON());
        }

        JSONArray tileGeneratorsArray = new JSONArray();
        for (TileGenerator tileGenerator : tileGenerators) {
            tileGeneratorsArray.put(tileGenerator.toJSON());
        }

        JSONArray categoriesArray = new JSONArray();
        for (TileCategory category : categories) {
            categoriesArray.put(category.toJSON());
        }

        JSONObject tilesRoot = new JSONObject();
        tilesRoot.put("tiles", tilesArray);
        tilesRoot.put("tile-generators", tileGeneratorsArray);
        tilesRoot.put("categories", categoriesArray);
        tilesRoot.put("disabled-runtime-tiles", disabledRuntimeTiles.stream().distinct().collect(Collectors.toList()));

        return tilesRoot;
    }

    private void createSettingsTiles() {
        addSettingsTile("LaunchAnything Settings", "tile option editor help", "webeditor");
        addSettingsTile("Create Tile", "add new", "createTile");

        Tile selfDir = new Tile("LaunchAnything Directory");
        selfDir.setCategory("settings");
        selfDir.setActive(true);
        selfDir.setExportable(false);
        selfDir.setKeywords("");
        selfDir.addAction(new TileAction("file", System.getProperty("user.dir")));
        tiles.add(selfDir);

        if (Main.isVersionSnapshot())
            addSettingsTile("Check for update", "elevate", "update");

        addSettingsTile("Restart LaunchAnything", "relaunch", "restartBar");
        addSettingsTile("Exit LaunchAnything", "leave quit stop", "exit");
    }

    private void addSettingsTile(String label, String keywords, String action) {
        Tile tile = new Tile(label);
        tile.setCategory("settings");
        tile.setActive(true);
        tile.setExportable(false);
        tile.setKeywords(keywords);
        tile.addAction(new TileAction("settings", action));
        tiles.add(tile);
    }

    private void addUrlTile(String label, String keywords, String url) {
        Tile tile = new Tile(label);
        tile.setCategory("url");
        tile.setActive(true);
        tile.setExportable(true);
        tile.setKeywords(keywords);
        tile.addAction(new TileAction("url", url));
        tiles.add(tile);
    }

    private void generateDefaultTiles() {
        tiles.clear();
        addUrlTile("LaunchAnything GitHub", "readme help", "https://github.com/Skyball2000/launch-anything");
        addUrlTile("WhatsApp", "messages", "https://web.whatsapp.com");
        addUrlTile("GeoGebra", "graphs", "https://www.geogebra.org/calculator");
        addUrlTile("YouTube", "video", "https://www.youtube.com");
        addUrlTile("Timerling", "countdown", "http://yanwittmann.de/projects/timerling");
        categories.add(new TileCategory("file", new Color(60, 150, 199)));
        categories.add(new TileCategory("url", new Color(239, 93, 62)));
        categories.add(new TileCategory("copy", new Color(252, 186, 3)));
        categories.add(new TileCategory("runtime", new Color(59, 196, 57)));
        categories.add(new TileCategory("settings", new Color(222, 40, 0)));
        LOG.info("Generated default tiles and categories");
    }

    private void addRuntimeTiles() {
        runtimeTiles.clear();
        for (RuntimeTiles value : RuntimeTiles.values()) {
            if (!disabledRuntimeTiles.contains(value.getName())) {
                runtimeTiles.add(value.getProvider().getTile());
            }
        }
    }

    private enum RuntimeTiles {
        GO_WEBSITE(GoWebsiteTile::new),
        NUMBER_BASE_CONVERTER(NumberBaseConverterTile::new),
        MATH_EXPRESSION(MathExpressionTile::new),
        CHART_GENERATOR(ChartGeneratorTile::new),
        WIKI_SEARCH(WikiSearchTile::new),
        TIMEOUT(TimeoutTile::new),
        SYSTEM_INFO(SystemInfoTile::new),
        DIRECTORY_PATH(URIOpenerTile::new);

        private final String name;
        private final String description;
        private final RuntimeTileProvider provider;

        RuntimeTiles(RuntimeTileProvider provider) {
            this.provider = provider;
            RuntimeTile tile = this.provider.getTile();
            this.name = tile.getName();
            this.description = tile.getDescription();
        }

        public RuntimeTileProvider getProvider() {
            return provider;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        interface RuntimeTileProvider {
            RuntimeTile getTile();
        }
    }

    public static Map<String, String> getRuntimeTilesNames() {
        Map<String, String> names = new HashMap<>();
        for (RuntimeTiles value : RuntimeTiles.values()) {
            names.put(value.getName(), value.getDescription());
        }
        return names;
    }

    public void toggleRuntimeTile(String tileId) {
        if (disabledRuntimeTiles.contains(tileId)) {
            disabledRuntimeTiles.remove(tileId);
        } else {
            disabledRuntimeTiles.add(tileId);
        }
        List<String> distinct = disabledRuntimeTiles.stream().distinct().collect(Collectors.toList());
        disabledRuntimeTiles.clear();
        disabledRuntimeTiles.addAll(distinct);
        addRuntimeTiles();
    }

    public void cleanUpTileActions() {
        for (Tile tile : tiles) {
            tile.cleanUpTileActions();
        }
    }

    public void addOnInputEvaluatedListener(InputEvaluatedListener listener) {
        onInputEvaluatedListeners.add(listener);
    }

    public interface InputEvaluatedListener {
        void onInputEvaluated(List<Tile> tiles);
    }
}
