package bar.tile;

import bar.logic.Settings;
import bar.tile.action.TileAction;
import bar.tile.custom.*;
import bar.ui.TrayUtil;
import bar.util.Util;
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

    private final static int TILE_FORMAT_VERSION = 1;

    private final PluginTileLoader plugins;
    private final TileBackups tileBackups;

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
        plugins = new PluginTileLoader();
        plugins.loadPlugins();
        findTilesFile();
        if (tileFile == null) {
            tileFile = new File("res/tiles.json");
            generateDefaultTiles();
            createSettingsTiles();
            isFirstLaunch = true;
            tileBackups = new TileBackups(tileFile);
        } else {
            tileBackups = new TileBackups(tileFile);
            readTilesFromFile();
        }
        LOG.info("Is first launch: [{}]", isFirstLaunch);
        addRuntimeTiles();
    }

    public boolean isFirstLaunch() {
        return isFirstLaunch;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    {
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdownNow));
    }

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

            plugins.getPluginRuntimeTiles().stream()
                    .filter(t -> !disabledRuntimeTiles.contains(t.getName()))
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

    private void findTilesFile() {
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

            // only create a backup if the file was loaded successfully,
            // we don't want to create a backup if the file is corrupted
            tileBackups.createBackup();
        } catch (JSONException e) {
            TrayUtil.showError("Unable to load tiles: file is corrupted");
            LOG.error("Unable to load tiles: file is corrupted: {}", e.getMessage());
            if (tileBackups.userAskLoadBackup()) {
                Util.popupMessage("Backup", "Will now attempt to load tiles from backup.\n" +
                                            "If the problem persists, please create an issue on GitHub.");
                readTilesFromFile();
            }
        } catch (FileNotFoundException e) {
            TrayUtil.showError("Unable to load tiles: file does not exist");
            LOG.error("Unable to load tiles: file does not exist: {}", e.getMessage());
        }
        regenerateGeneratedTiles();
        createSettingsTiles();
    }

    public void loadTilesFromJson(JSONObject tilesRoot) {
        int version = tilesRoot.optInt("version", 0);
        if (version != TILE_FORMAT_VERSION) {
            tileBackups.createBackup();
            LOG.warn("Tile file format [{}] is not the current version [{}]", version, TILE_FORMAT_VERSION);
            TrayUtil.showWarning("Tile file format is not the current version.\nCheck popup for details.");
            String answer = Util.popupChooseButton("Load Tiles",
                    "Tile format does not match current version (file = " + version + ", current = " + TILE_FORMAT_VERSION + ").\n" +
                    "Do you want to still load the tiles? A backup has already been created.\n" +
                    "If you have problems migrating, please create an issue on GitHub.",
                    new String[]{"Load", "Open backup directory and exit"});
            if (answer == null || answer.equals("Open backup directory and exit")) {
                tileBackups.openBackupDirAndExit();
            }
        }
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

    public Tile findLocalTileByAction(TileAction action) {
        for (Tile tile : tiles) {
            TileAction firstAction = tile.getFirstAction();
            if (firstAction != null && firstAction.equals(action)) {
                return tile;
            }
        }
        return null;
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
            if (tile.isExportable()) tilesArray.put(tile.toJSON());
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
        tilesRoot.put("version", TILE_FORMAT_VERSION);

        return tilesRoot;
    }

    private final static Tile SETTINGS_TILE_CLOUD_SYNC;
    private final static Tile SETTINGS_TILE_CLOUD_CREATE_TILE;
    private final static Tile SETTINGS_TILE_CHECK_FOR_UPDATE;

    static {
        SETTINGS_TILE_CLOUD_SYNC = createSettingsTile("Synchronize Cloud Tiles", "", "cloud-sync");
        SETTINGS_TILE_CLOUD_CREATE_TILE = createSettingsTile("Create Cloud Tile", "", "cloud-create-tile");
        SETTINGS_TILE_CHECK_FOR_UPDATE = createSettingsTile("Check for Update", "", "check-for-update");
    }

    private void setSettingsCloudTilesActive(boolean active) {
        if (active) {
            tiles.add(SETTINGS_TILE_CLOUD_SYNC);
            tiles.add(SETTINGS_TILE_CLOUD_CREATE_TILE);
        } else {
            tiles.remove(SETTINGS_TILE_CLOUD_SYNC);
            tiles.remove(SETTINGS_TILE_CLOUD_CREATE_TILE);
        }
    }

    public void setSettingsTileCheckForUpdateActive(boolean active) {
        // if the application is not launched from a jar file, then the update cannot take place anyway
        if (active && Util.isApplicationStartedFromJar()) tiles.add(SETTINGS_TILE_CHECK_FOR_UPDATE);
        else tiles.remove(SETTINGS_TILE_CHECK_FOR_UPDATE);
    }

    private void createSettingsTiles() {
        LOG.info("Creating settings tiles");

        tiles.add(createSettingsTile("LaunchAnything Settings", "tile option editor help", "webeditor"));
        tiles.add(createSettingsTile("Create Tile", "add new", "createTile"));

        Tile selfDir = new Tile("LaunchAnything Directory");
        selfDir.setCategory("settings");
        selfDir.setActive(true);
        selfDir.setExportable(false);
        selfDir.setKeywords("");
        selfDir.addAction(TileAction.getInstance("file", System.getProperty("user.dir")));
        tiles.add(selfDir);

        tiles.add(createSettingsTile("Restart LaunchAnything", "relaunch", "restartBar"));
        tiles.add(createSettingsTile("Reload Plugins", "", "reloadPlugins"));
        tiles.add(createSettingsTile("Exit LaunchAnything", "leave quit stop", "exit"));
    }

    private void generateDefaultTiles() {
        tiles.clear();
        tiles.add(createUrlTile("LaunchAnything GitHub", "readme help", "https://github.com/Skyball2000/launch-anything"));
        tiles.add(createUrlTile("WhatsApp", "messages", "https://web.whatsapp.com"));
        tiles.add(createUrlTile("GeoGebra", "graphs", "https://www.geogebra.org/calculator"));
        tiles.add(createUrlTile("YouTube", "video", "https://www.youtube.com"));
        tiles.add(createUrlTile("Timerling", "countdown", "http://yanwittmann.de/projects/timerling"));
        categories.add(new TileCategory("file", new Color(60, 150, 199)));
        categories.add(new TileCategory("url", new Color(239, 93, 62)));
        categories.add(new TileCategory("copy", new Color(252, 186, 3)));
        categories.add(new TileCategory("runtime", new Color(59, 196, 57)));
        categories.add(new TileCategory("settings", new Color(222, 40, 0)));
        LOG.info("Generated default tiles and categories");
    }

    private static Tile createSettingsTile(String label, String keywords, String action) {
        Tile tile = new Tile(label);
        tile.setCategory("settings");
        tile.setActive(true);
        tile.setExportable(false);
        tile.setKeywords(keywords);
        tile.addAction(TileAction.getInstance("settings", action));
        return tile;
    }

    private Tile createUrlTile(String label, String keywords, String url) {
        Tile tile = new Tile(label);
        tile.setCategory("url");
        tile.setActive(true);
        tile.setExportable(true);
        tile.setKeywords(keywords);
        tile.addAction(TileAction.getInstance("url", url));
        return tile;
    }

    private void addRuntimeTiles() {
        runtimeTiles.clear();
        for (RuntimeTiles value : RuntimeTiles.values()) {
            if (!disabledRuntimeTiles.contains(value.getName())) {
                runtimeTiles.add(value.getProvider().getTile());
            }
        }
    }

    public void reloadPlugins() {
        LOG.info("Reloading plugins");
        plugins.loadPlugins();
        LOG.info("Reloaded plugins");
    }

    private enum RuntimeTiles {
        GO_WEBSITE(GoWebsiteTile::new),
        NUMBER_BASE_CONVERTER(NumberBaseConverterTile::new),
        ASPECT_RATIO(AspectRationTile::new),
        UNIT_CONVERTER(UnitConverterTile::new),
        MULTI_TYPE_EVALUATOR(MultiTypeEvaluatorTile::new),
        CHART_GENERATOR(ChartGeneratorTile::new),
        WIKI_SEARCH(WikiSearchTile::new),
        TIMEOUT(TimeoutTile::new),
        SYSTEM_INFO(SystemInfoTile::new),
        DIRECTORY_PATH(URIOpenerTile::new);

        private final String name;
        private final String description;
        private final String author;
        private final String version;
        private final RuntimeTileProvider provider;

        RuntimeTiles(RuntimeTileProvider provider) {
            this.provider = provider;
            RuntimeTile tile = this.provider.getTile();
            this.name = tile.getName();
            this.description = tile.getDescription();
            this.author = tile.getAuthor();
            this.version = tile.getVersion();
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

        public String getAuthor() {
            return author;
        }

        public String getVersion() {
            return version;
        }

        interface RuntimeTileProvider {
            RuntimeTile getTile();
        }
    }

    public static Map<String, JSONObject> getRuntimeTilesNames() {
        Map<String, JSONObject> names = new HashMap<>();
        for (RuntimeTiles value : RuntimeTiles.values()) {
            names.put(value.getName(),
                    new JSONObject().put("description", value.getDescription())
                            .put("author", value.getAuthor())
                            .put("version", value.getVersion()));
        }
        return names;
    }

    public Map<String, JSONObject> getPluginRuntimeTilesNames() {
        Map<String, JSONObject> names = new HashMap<>();
        for (RuntimeTile value : plugins.getPluginRuntimeTiles()) {
            names.put(value.getName(),
                    new JSONObject().put("description", value.getDescription())
                            .put("author", value.getAuthor())
                            .put("version", value.getVersion()));
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

        setSettingsCloudTilesActive(false);
        cloudAccess = new CloudAccess(url, username, password, false);
        setSettingsCloudTilesActive(true);
    }

    public void addCloudAccessCreateAccount(Settings settings) throws IOException {
        String url = settings.getStringOrNull(Settings.Setting.CLOUD_TIMER_URL);
        String username = settings.getStringOrNull(Settings.Setting.CLOUD_TIMER_USERNAME);
        String password = settings.getStringOrNull(Settings.Setting.CLOUD_TIMER_PASSWORD);

        setSettingsCloudTilesActive(false);
        cloudAccess = new CloudAccess(url, username, password, true);
        setSettingsCloudTilesActive(true);
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
                unsynchronizedCloudTiles.clear();
                synchronizedCloudTiles.clear();
                LOG.warn("Received error from cloud: [{}]", tilesForUser.optString("message", ""));
            }

        } catch (IOException e) {
            TrayUtil.showError("Could not connect to the cloud server to synchronize tiles");
        } catch (JSONException e) {
            TrayUtil.showError("Unable to parse the response from the cloud server");
        }
        save();
    }

    public void checkLocalDuplicates() {
        for (Tile tile : synchronizedCloudTiles) checkLocalDuplicate(tile);
        for (Tile tile : unsynchronizedCloudTiles) checkLocalDuplicate(tile);
    }

    public void checkLocalDuplicate(Tile checkTile) {
        Tile localTile = findLocalTileByAction(checkTile.getFirstAction());
        if (localTile != null) {
            String selection = Util.popupChooseButton("Cloud Tile",
                    "A local tile [" + localTile.getLabel() + "] already exists.\n" +
                    "Do you want to delete the local tile and only keep the cloud tile [" + checkTile.getLabel() + "]?",
                    new String[]{"Yes", "No"});
            if (selection != null && selection.equals("Yes")) {
                removeTile(localTile);
            }
        }
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

    public File getTileFile() {
        return tileFile;
    }

    public TileBackups getTileBackups() {
        return tileBackups;
    }

    public void addOnInputEvaluatedListener(InputEvaluatedListener listener) {
        onInputEvaluatedListeners.add(listener);
    }

    public interface InputEvaluatedListener {
        void onInputEvaluated(List<Tile> tiles);
    }
}
