import lc.kra.system.keyboard.event.GlobalKeyEvent;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class Main {

    private static Main self;

    public static void main(String[] args) {
        Main main = new Main();
        self = main;
        main.initializeConfig();
        if (main.configOpenMode.equals("bar")) {
            main.initializeSettings();
            main.initializeBar();
            main.initializeKeyDetector();
        } else if (main.configOpenMode.equals("settings")) {

        }
    }

    private final static int ACTIVATION_KEY = GlobalKeyEvent.VK_CONTROL;
    private final static int CANCEL_KEY = GlobalKeyEvent.VK_ESCAPE;

    private String configOpenMode = "bar";
    private JSONObject config = new JSONObject();

    private void initializeConfig() {
        if (FileUtils.fileExists("res/config.json")) {
            String[] input = FileUtils.readFile(new File("res/config.json"));
            if (input != null) {
                StringBuilder inputJSONbuilder = new StringBuilder();
                for (String s : input) inputJSONbuilder.append(s.trim());
                config = new JSONObject(inputJSONbuilder.toString());
                if (config.has("openMode"))
                    configOpenMode = config.getString("openMode");
                return;
            }
        }
        setConfig("openMode", configOpenMode);
    }

    private void initializeKeyDetector() {
        //System.out.println(event);
        new GlobalKeyDetector() {
            @Override
            public void keyDetected(GlobalKeyEvent event) {
                //System.out.println(event);
                switch (event.getVirtualKeyCode()) {
                    case ACTIVATION_KEY -> activationKeyPress();
                    case CANCEL_KEY -> cancelKeyPress();
                    default -> launchBar.characterTyped(event.getVirtualKeyCode());
                }
            }
        };
    }

    private void initializeBar() {
        launchBar = new LaunchBar(this);
    }

    private TileManager tileManager;

    private void initializeSettings() {
        tileManager = new TileManager("D:\\files\\create\\programming\\projects\\launch-anything\\launch-anything-res\\");
        readSettingsData();

        tileManager.generateSettingTile("openSettings", "LaunchAnything Settings", "settings", "settings,launch,anything,open,options", "settings");
        tileManager.generateSettingTile("closeAction", "Exit LaunchAnything", "settings", "exit,quit,close,dispose", "exit");
        tileManager.generateCategory("settings", "#8a0a14");
    }

    private void readSettingsData() {
        tileManager.read();
    }

    private long mostRecentSearchTime = 0;
    private int currentResultScrollIndex = 0;
    private boolean searchRunning = false;
    private ArrayList<Tile> currentResults;

    public void search(String search) {
        currentResultScrollIndex = 0;
        if (search.trim().length() <= 1) return;
        long mySearchTime = getTime();
        mostRecentSearchTime = mySearchTime;
        while (searchRunning) {
            Sleep.milliseconds(100);
            if (mySearchTime != mostRecentSearchTime) {
                System.out.println("Gave up search '" + search + "'");
                return;
            }
        }
        searchRunning = true;
        System.out.println("searching for '" + search + "'");
        search = search.toLowerCase();
        currentResults = sortResults(tileManager.search(search));
        createResultsAmount(currentResults.size());
        for (int i = 0, resultsSize = currentResults.size(); i < launchBarResults.size() && i < maxAmountResults; i++) {
            if (i < resultsSize) {
                Tile tile = currentResults.get(i);
                if (previousAmount <= i) launchBarResults.get(i).prepareNow();
                launchBarResults.get(i).setResult(tile);
                System.out.println("Displaying result tile (" + i + ") " + tile);
            } else {
                launchBarResults.get(i).deactivate();
            }
        }
        previousAmount = currentResults.size();
        searchRunning = false;
    }

    public void resetSearch() {
        launchBarResults.forEach(LaunchBarResult::deactivate);
        previousAmount = 0;
    }

    private ArrayList<Tile> sortResults(ArrayList<Tile> tiles) {
        if (tiles.size() == 0) return tiles;
        int destSize = tiles.size();
        ArrayList<Tile> results = new ArrayList<>();
        do {
            long latestUsed = 0;
            int latestUsedIndex = 0;
            for (int i = 0; i < tiles.size(); i++) {
                Tile tile = tiles.get(i);
                if (tile.getLastExecuted() > latestUsed) {
                    latestUsed = tile.getLastExecuted();
                    latestUsedIndex = i;
                }
            }
            results.add(tiles.get(latestUsedIndex));
            tiles.remove(tiles.get(latestUsedIndex));
        } while (destSize != results.size());
        return results;
    }

    private long latestScrollTime = 0;
    private boolean scrollRunning = false;

    public void scrollResults(int amount) {
        if (currentResults == null) return;
        int newScrollIndex = Math.min(currentResults.size() - 1, Math.max(0, currentResultScrollIndex + amount));
        if (newScrollIndex == currentResultScrollIndex) return;
        currentResultScrollIndex = newScrollIndex;
        long myScrollTime = getTime();
        latestScrollTime = myScrollTime;
        while (scrollRunning) {
            Sleep.milliseconds(100);
            if (myScrollTime != latestScrollTime) {
                System.out.println("Gave up displaying scroll '" + amount + "'");
                return;
            }
        }
        scrollRunning = true;
        System.out.println("Displaying scroll index: " + currentResultScrollIndex);
        int counter = 0;
        for (int i = currentResultScrollIndex, resultsSize = currentResults.size(); counter < launchBarResults.size() && counter < maxAmountResults; i++) {
            if (i < 0) continue;
            if (i < resultsSize) {
                Tile tile = currentResults.get(i);
                if (previousAmount <= i) launchBarResults.get(counter).prepareNow();
                launchBarResults.get(counter).setResult(tile);
                System.out.println("Displaying result tile (" + counter + ") " + tile);
            } else {
                launchBarResults.get(counter).deactivate();
            }
            counter++;
        }
        scrollRunning = false;
    }

    private final ArrayList<LaunchBarResult> launchBarResults = new ArrayList<>();
    private int previousAmount = 0;

    private void createResultsAmount(int amount) {
        while (launchBarResults.size() < amount && maxAmountResults > launchBarResults.size()) {
            LaunchBarResult launchBarResult = new LaunchBarResult(this, launchBarResults.size());
            launchBarResult.prepareNow();
            launchBarResults.add(launchBarResult);
        }
    }

    public void executeResultsTile(int index) {
        launchBar.deactivate();
        launchBarResults.forEach(LaunchBarResult::deactivate);
        System.out.println("Executing tile (" + index + "): " + launchBarResults.get(index).getTile());
        launchBarResults.get(index).getTile().execute();
        tileManager.save();
    }

    private LaunchBar launchBar;
    private long lastActivationKeyPressMillis = getTime();
    private int maxPeriod = 300, minPeriod = 60;

    private void activationKeyPress() {
        long millis = getTime();
        if (millis < lastActivationKeyPressMillis + maxPeriod && millis > lastActivationKeyPressMillis + minPeriod) {
            launchBar.activate();
            launchBarResults.forEach(LaunchBarResult::prepareNow);
        }
        lastActivationKeyPressMillis = millis;
    }

    private void cancelKeyPress() {
        launchBar.deactivate();
        launchBarResults.forEach(LaunchBarResult::deactivate);
    }

    public static long getTime() {
        return System.currentTimeMillis();
    }

    public Color getColorForCategory(String category) {
        return tileManager.getColorForCategory(category);
    }

    public void setConfig(String key, String value) {
        config.put(key, value);
        FileUtils.writeFile(new File("res/config.json"), config.toString());
    }

    public String getConfig(String key) {
        if(config.has(key))
            return config.getString(key);
        return "";
    }

    public static void setOpenMode(boolean barMode) {
        self.setConfig("openMode", barMode ? "bar" : "settings");
        System.exit(0);
    }

    private int launcherBarXsize = 800;
    private int launcherBarYsize = 80;
    private int distanceToMainBar = 10;
    private int distanceBetweenResults = 6;
    private int maxAmountResults = 8;

    public int getLauncherBarXsize() {
        return launcherBarXsize;
    }

    public int getLauncherBarYsize() {
        return launcherBarYsize;
    }

    public int getDistanceToMainBar() {
        return distanceToMainBar;
    }

    public int getDistanceBetweenResults() {
        return distanceBetweenResults;
    }
}
