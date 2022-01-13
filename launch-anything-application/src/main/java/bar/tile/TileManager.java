package bar.tile;

import bar.Main;
import bar.logic.Settings;
import bar.tile.custom.*;
import bar.ui.TrayUtil;
import org.json.JSONArray;
import org.json.JSONException;
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

    private static final Logger LOG = LoggerFactory.getLogger(TileManager.class);

    private final List<Tile> tiles = new ArrayList<>();
    private final List<RuntimeTile> runtimeTiles = new ArrayList<>();
    private final List<Tile> generatedTiles = new ArrayList<>();
    private final List<Tile> synchronizedCloudTiles = new ArrayList<>();
    private final List<Tile> unsynchronizedCloudTiles = new ArrayList<>();
    private final List<Tile> deletedCloudTiles = new ArrayList<>();
    private final List<TileGenerator> tileGenerators = new ArrayList<>();
    private final List<InputEvaluatedListener> onInputEvaluatedListeners = new ArrayList<>();
    private final List<TileCategory> categories = new ArrayList<>();
    private final List<String> disabledRuntimeTiles = new ArrayList<>();
    private File tileFile;
    private boolean isFirstLaunch = false;

    private CloudAccess cloudAccess;

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
            List<Tile> matchingTiles = new ArrayList<>();

            searchTiles(tiles, matchingTiles, input);
            searchTiles(unsynchronizedCloudTiles, matchingTiles, input);
            searchTiles(synchronizedCloudTiles, matchingTiles, input);
            searchTiles(generatedTiles, matchingTiles, input);

            runtimeTiles.stream()
                    .map(runtimeTile -> runtimeTile.generateTiles(input, lastInputEvaluated))
                    .forEach(matchingTiles::addAll);

            setEvaluationResults(matchingTiles);
            return null;
        });
    }

    private void searchTiles(List<Tile> tiles, List<Tile> matchingTiles, String input) {
        tiles.stream()
                .filter(tile -> tile.matchesSearch(input))
                .sorted(Comparator.comparing(Tile::getLastActivated).reversed())
                .forEach(matchingTiles::add);
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
        createTilesFromJsonArray(tilesArray, tiles);

        JSONObject cloudTiles = tilesRoot.optJSONObject("cloudTiles");
        if (cloudTiles != null) {
            JSONArray synchronizedTilesArray = cloudTiles.optJSONArray("synchronizedTiles");
            createTilesFromJsonArray(synchronizedTilesArray, synchronizedCloudTiles);
            JSONArray unsynchronizedTilesArray = cloudTiles.optJSONArray("unsynchronizedTiles");
            createTilesFromJsonArray(unsynchronizedTilesArray, unsynchronizedCloudTiles);
            JSONArray deletedCloudTilesArray = cloudTiles.optJSONArray("deletedCloudTiles");
            createTilesFromJsonArray(deletedCloudTilesArray, deletedCloudTiles);
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

    private void createTilesFromJsonArray(JSONArray tilesArray, List<Tile> tiles) {
        if (tilesArray != null) {
            for (int i = 0; i < tilesArray.length(); i++) {
                JSONObject tileJson = tilesArray.optJSONObject(i);
                if (tileJson == null) continue;
                Tile tile = new Tile(tileJson);
                if (tile.isValid()) tiles.add(tile);
            }
        }
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
        Tile localTile = tiles.stream().filter(tile -> tile.getId().equals(tileId)).findFirst().orElse(null);
        if (localTile != null) return localTile;
        Tile synchronizedCloudTile = synchronizedCloudTiles.stream().filter(tile -> tile.getId().equals(tileId)).findFirst().orElse(null);
        if (synchronizedCloudTile != null) return synchronizedCloudTile;
        return unsynchronizedCloudTiles.stream().filter(tile -> tile.getId().equals(tileId)).findFirst().orElse(null);
    }

    public void removeTile(Tile tile) {
        if (synchronizedCloudTiles.contains(tile) || unsynchronizedCloudTiles.contains(tile)) {
            synchronizedCloudTiles.remove(tile);
            unsynchronizedCloudTiles.remove(tile);
            deletedCloudTiles.add(tile);
        }
        tiles.remove(tile);
    }

    public void save() {
        try {
            tileFile.getParentFile().mkdirs();
            FileWriter myWriter = new FileWriter(tileFile);

            myWriter.write(toJSON().toString());
            myWriter.close();
            LOG.info("Saved tiles");
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

        JSONObject cloudTiles = new JSONObject();
        cloudTiles.put("synchronizedCloudTiles", synchronizedCloudTiles.stream().map(Tile::toJSON).collect(Collectors.toList()));
        cloudTiles.put("unsynchronizedCloudTiles", unsynchronizedCloudTiles.stream().map(Tile::toJSON).collect(Collectors.toList()));
        cloudTiles.put("deletedCloudTiles", deletedCloudTiles.stream().map(Tile::toJSON).collect(Collectors.toList()));
        if (cloudAccess != null) {
            cloudTiles.put("username", cloudAccess.getUsername());
        }

        JSONObject tilesRoot = new JSONObject();
        tilesRoot.put("tiles", tilesArray);
        tilesRoot.put("tile-generators", tileGeneratorsArray);
        tilesRoot.put("categories", categoriesArray);
        tilesRoot.put("disabled-runtime-tiles", disabledRuntimeTiles.stream().distinct().collect(Collectors.toList()));
        tilesRoot.put("cloudTiles", cloudTiles);

        return tilesRoot;
    }

    private final static Tile SETTINGS_TILE_CLOUD_SYNC = new Tile();

    static {
        SETTINGS_TILE_CLOUD_SYNC.addAction(new TileAction("settings", "cloud-sync"));
        SETTINGS_TILE_CLOUD_SYNC.setExportable(false);
        SETTINGS_TILE_CLOUD_SYNC.setLabel("Synchronize Cloud Tiles");
        SETTINGS_TILE_CLOUD_SYNC.setCategory("settings");
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
        for (Tile tile : tiles) tile.cleanUpTileActions();
        for (Tile tile : synchronizedCloudTiles) tile.cleanUpTileActions();
        for (Tile tile : unsynchronizedCloudTiles) tile.cleanUpTileActions();
        for (Tile tile : deletedCloudTiles) tile.cleanUpTileActions();
    }

    public void addCloudAccess(Settings settings) throws IOException {
        String url = settings.getStringOrNull(Settings.Setting.CLOUD_TIMER_URL);
        String username = settings.getStringOrNull(Settings.Setting.CLOUD_TIMER_USERNAME);
        String password = settings.getStringOrNull(Settings.Setting.CLOUD_TIMER_PASSWORD);

        tiles.remove(SETTINGS_TILE_CLOUD_SYNC);
        cloudAccess = new CloudAccess(url, username, password, false);
        tiles.add(SETTINGS_TILE_CLOUD_SYNC);
    }

    public void addCloudAccessCreateAccount(Settings settings) throws IOException {
        String url = settings.getStringOrNull(Settings.Setting.CLOUD_TIMER_URL);
        String username = settings.getStringOrNull(Settings.Setting.CLOUD_TIMER_USERNAME);
        String password = settings.getStringOrNull(Settings.Setting.CLOUD_TIMER_PASSWORD);

        tiles.remove(SETTINGS_TILE_CLOUD_SYNC);
        cloudAccess = new CloudAccess(url, username, password, true);
        tiles.add(SETTINGS_TILE_CLOUD_SYNC);
    }

    public void synchronizeCloudTiles() {
        if (cloudAccess == null) return;
        try {
            // upload the unsynchronized tiles into the cloud
            for (int i = unsynchronizedCloudTiles.size() - 1; i >= 0; i--) {
                Tile tile = unsynchronizedCloudTiles.get(i);
                JSONObject response = cloudAccess.creteOrModifyTile(tile);
                if (CloudAccess.isSuccess(response)) {
                    unsynchronizedCloudTiles.removeIf(t -> t.getId().equals(tile.getId()));
                    synchronizedCloudTiles.removeIf(t -> t.getId().equals(tile.getId()));
                    synchronizedCloudTiles.add(tile);
                    LOG.info("Successfully uploaded tile [{}] to cloud", tile.getId());
                } else {
                    LOG.warn("Failed to upload tile [{}] to cloud: [{}]", tile.getId(), response.optString("message", ""));
                }
            }

            // delete the tiles from the cloud that have been deleted locally
            for (int i = deletedCloudTiles.size() - 1; i >= 0; i--) {
                Tile tile = deletedCloudTiles.get(i);
                JSONObject response = cloudAccess.removeTile(tile.getId());
                if (CloudAccess.isSuccess(response)) {
                    deletedCloudTiles.remove(tile);
                    unsynchronizedCloudTiles.removeIf(t -> t.getId().equals(tile.getId()));
                    synchronizedCloudTiles.removeIf(t -> t.getId().equals(tile.getId()));
                    LOG.info("Successfully deleted tile [{}] from cloud", tile.getId());
                } else {
                    LOG.warn("Failed to delete tile [{}] from cloud: [{}]", tile.getId(), response.optString("message", ""));
                    if (response.optString("message", "").contains("You are not the owner of this tile")) {
                        deletedCloudTiles.remove(tile);
                        unsynchronizedCloudTiles.removeIf(t -> t.getId().equals(tile.getId()));
                        synchronizedCloudTiles.removeIf(t -> t.getId().equals(tile.getId()));
                    }
                }
            }

            // get all tiles from the cloud
            JSONObject tilesForUser = cloudAccess.getTilesForUser();
            if (CloudAccess.isSuccess(tilesForUser)) {
                JSONArray tiles = new JSONArray(tilesForUser.optString("message", ""));
                unsynchronizedCloudTiles.clear();
                synchronizedCloudTiles.clear();
                for (int i = 0; i < tiles.length(); i++) {
                    JSONObject tileJson = tiles.getJSONObject(i);
                    if (tileJson != null) {
                        Tile tile = new Tile(tileJson);
                        if (tile.getId() != null) {
                            synchronizedCloudTiles.add(tile);
                        }
                    }
                }
                LOG.info("Received [{}] tiles from cloud", tiles.length());
            } else {
                LOG.warn("Received error from cloud: [{}]", tilesForUser.optString("message", ""));
            }

        } catch (IOException e) {
            TrayUtil.showError("Could not connect to the cloud server to synchronize tiles");
        } catch (JSONException e) {
            TrayUtil.showError("Unable to parse the response from the cloud server");
        }
        save();
    }

    public void cloudResetAll() {
        unsynchronizedCloudTiles.clear();
        synchronizedCloudTiles.clear();
        deletedCloudTiles.clear();
        save();
    }

    public boolean isCloudTile(Tile tile) {
        boolean s = synchronizedCloudTiles.stream().anyMatch(t -> t.getId().equals(tile.getId()));
        boolean u = unsynchronizedCloudTiles.stream().anyMatch(t -> t.getId().equals(tile.getId()));
        return s || u;
    }

    public void cloudTileHasBeenEdited(Tile tile) {
        unsynchronizedCloudTiles.removeIf(t -> t.getId().equals(tile.getId()));
        synchronizedCloudTiles.removeIf(t -> t.getId().equals(tile.getId()));
        unsynchronizedCloudTiles.add(tile);
        LOG.info("Tile [{}] has been edited locally, but not yet synchronized to the cloud", tile.getId());
    }

    public void addCloudTile(Tile tile) {
        unsynchronizedCloudTiles.add(tile);
    }

    public CloudAccess getCloudAccess() {
        return cloudAccess;
    }

    public void addOnInputEvaluatedListener(InputEvaluatedListener listener) {
        onInputEvaluatedListeners.add(listener);
    }

    public interface InputEvaluatedListener {
        void onInputEvaluated(List<Tile> tiles);
    }
}
