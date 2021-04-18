import lc.kra.system.keyboard.event.GlobalKeyEvent;
import mslinks.ShellLink;
import org.json.JSONObject;
import yanwittmann.notification.BlurNotification;
import yanwittmann.types.File;
import yanwittmann.utils.FileUtils;
import yanwittmann.utils.GeneralUtils;
import yanwittmann.utils.Log;
import yanwittmann.utils.MathEval;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

public class Main {

    private static Main self;

    public static void main(String[] args) throws IOException {
        Main main = new Main();
        self = main;
        main.initializeConfig();
        main.beforeTasks();
        main.initializeTileManager();
        String openMode = main.getConfigOrSetDefault("openMode", "bar");
        if (openMode.equals("bar")) {
            main.initializeBar();
            main.initializeKeyDetector();
            new BlurNotification("LaunchAnything is active");
        } else if (openMode.equals("settings")) {
            main.openSettings();
        }


    }

    private int activationKey = GlobalKeyEvent.VK_CONTROL;
    private int cancelKey = GlobalKeyEvent.VK_ESCAPE;

    private JSONObject config = new JSONObject();

    private void initializeConfig() throws IOException {
        File configFile = new File("res/config.json");
        if (configFile.exists()) {
            String[] input = configFile.readToArray();
            if (input != null) {
                StringBuilder inputJSONbuilder = new StringBuilder();
                for (String s : input) inputJSONbuilder.append(s.trim());
                config = new JSONObject(inputJSONbuilder.toString());
                return;
            }
        }
        //file does not exist, write new
        setConfig("openMode", "settings");
        setConfig("firstTimeOpen", "true");
    }

    private void beforeTasks() throws IOException {
        if (getConfig("firstTimeOpen").equals("true")) {
            int result = Popup.selectButton("LaunchBar", "Do you want to have the application launch on system startup?\n" +
                    "You can still change this later in the general settings.", new String[]{"Yes", "No"});
            if (result == 0) setAutostart(true);
            setConfig("firstTimeOpen", "false");
        }
        launcherBarXsize = getConfigIntegerOrSetDefault("launcherBarXsize", 800);
        launcherBarYsize = getConfigIntegerOrSetDefault("launcherBarYsize", 80);
        distanceToMainBar = getConfigIntegerOrSetDefault("distanceToMainBar", 10);
        distanceBetweenResults = getConfigIntegerOrSetDefault("distanceBetweenResults", 6);
        maxAmountResults = getConfigIntegerOrSetDefault("maxAmountResults", 8);
        activationKey = getConfigIntegerOrSetDefault("activationKey", 17);
        maxPeriod = getConfigIntegerOrSetDefault("maxDoubleClickDuration", 300);
    }

    private void initializeKeyDetector() {
        //Log.info(event);
        new GlobalKeyDetector() {
            @Override
            public void keyDetected(GlobalKeyEvent event) {
                //Log.info(event);
                int virtualKeyCode = event.getVirtualKeyCode();
                if (virtualKeyCode == activationKey) {
                    activationKeyPress();
                } else if (virtualKeyCode == cancelKey) {
                    cancelKeyPress();
                } else {
                    launchBar.characterTyped(event.getVirtualKeyCode());
                }
            }
        };
    }

    private void initializeBar() {
        launchBar = new LaunchBar(this);
        createResultsAmount(maxAmountResults);
    }

    private TileManager tileManager;

    private void initializeTileManager() throws IOException {
        tileManager = new TileManager("res\\");
        readSettingsData();

        tileManager.generateSettingTile("openSettings", "LaunchAnything Settings", "settings", "settings,launch,anything,open,options", "settings");
        tileManager.generateSettingTile("closeAction", "Exit LaunchAnything", "settings", "exit,quit,close,dispose", "exit");
        tileManager.generateSettingTile("openLaunchAnythingDir", "Open LaunchAnything directory", "settings", "open,directory,folder,lafolder,ladir", "lafolder");
        tileManager.generateCategory("settings", "#8a0a14");
    }

    private void openSettings() throws IOException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        GuiSettings settings = new GuiSettings(this);
        JFrame frame = new JFrame("LaunchAnything Settings");

        frame.setContentPane(settings.getMainPanel());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setIconImage(new ImageIcon("res/icon.png").getImage());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.dispose();
                System.exit(0);
            }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        settings.updateTiles(tileManager.getNonGeneratedTiles());
        settings.updateTileGenerators(tileManager.getTileGenerators());
        settings.updateCategories(tileManager.getCategories());
        settings.initializeGeneralSettings();
    }

    private void readSettingsData() throws IOException {
        tileManager.read(getConfig("openMode").equals("bar"));
    }

    private long mostRecentSearchTime = 0;
    private int currentResultScrollIndex = 0;
    private boolean searchRunning = false;
    private ArrayList<Tile> currentResults;
    private String currentSearch = "";

    public void search(String search) {
        if (search.matches("go.+")) {
            resetSearch();
            return;
        }
        currentResultScrollIndex = 0;
        if (search.trim().length() <= 1) return;
        long mySearchTime = getTime();
        mostRecentSearchTime = mySearchTime;
        while (searchRunning) {
            Sleep.milliseconds(100);
            if (mySearchTime != mostRecentSearchTime) {
                Log.info("Gave up search '" + search + "'");
                return;
            }
        }
        searchRunning = true;
        Log.info("Searching for '" + search + "'");
        this.currentSearch = search;
        search = search.toLowerCase();
        currentResults = sortResults(tileManager.search(search));
        if(checkForMathEval()) currentResults.add(mathResultsTile);
        createResultsAmount(currentResults.size());
        for (int i = 0, resultsSize = currentResults.size(); i < launchBarResults.size() && i < maxAmountResults; i++) {
            if (i < resultsSize) {
                Tile tile = currentResults.get(i);
                launchBarResults.get(i).setResult(tile);
                Log.info("Displaying result tile (" + i + ") " + tile);
            } else {
                launchBarResults.get(i).deactivate();
            }
        }
        searchRunning = false;
    }

    public void resetSearch() {
        launchBarResults.forEach(LaunchBarResult::deactivate);
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
        if (launchBar.getSearch().matches("go.+")) {
            resetSearch();
            return;
        }
        if (currentResults == null) return;
        int newScrollIndex = Math.min(currentResults.size() - 1, Math.max(0, currentResultScrollIndex + amount));
        if (newScrollIndex == currentResultScrollIndex) return;
        currentResultScrollIndex = newScrollIndex;
        long myScrollTime = getTime();
        latestScrollTime = myScrollTime;
        while (scrollRunning) {
            Sleep.milliseconds(100);
            if (myScrollTime != latestScrollTime) {
                Log.info("Gave up displaying scroll '" + amount + "'");
                return;
            }
        }
        scrollRunning = true;
        Log.info("Displaying scroll index: " + currentResultScrollIndex);
        int counter = 0;
        for (int i = currentResultScrollIndex, resultsSize = currentResults.size(); counter < launchBarResults.size() && counter < maxAmountResults; i++) {
            if (i < 0) continue;
            if (i < resultsSize) {
                Tile tile = currentResults.get(i);
                launchBarResults.get(counter).setResult(tile);
                Log.info("Displaying result tile (" + counter + ") " + tile);
            } else {
                launchBarResults.get(counter).deactivate();
            }
            counter++;
        }
        scrollRunning = false;
    }

    private final ArrayList<LaunchBarResult> launchBarResults = new ArrayList<>();

    private void createResultsAmount(int amount) {
        while (launchBarResults.size() < amount && maxAmountResults > launchBarResults.size()) {
            LaunchBarResult launchBarResult = new LaunchBarResult(this, launchBarResults.size());
            launchBarResult.prepareNow();
            launchBarResults.add(launchBarResult);
        }
    }

    private static Tile lastExecutedTile = null;

    public static void setLastExecutedTile(Tile lastExecutedTile) {
        Main.lastExecutedTile = lastExecutedTile;
    }

    public void executeResultsTile(int index) throws IOException {
        launchBar.deactivate();
        launchBarResults.forEach(LaunchBarResult::deactivate);
        if (launchBar.getSearch().matches("go.+")) {
            try {
                Desktop.getDesktop().browse(URI.create("http://www.google.com/search?q=" + launchBar.getSearch().replaceAll("go(.+)", "$1").trim().replace(" ", "+") + "&btnI"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        } else if (checkForMathEval()) {
            return;
        } else if (launchBar.getSearch().equals("again") || launchBar.getSearch().length() == 0) {
            if (lastExecutedTile != null) {
                Log.info("Executing tile (" + index + "): " + lastExecutedTile);
                lastExecutedTile.execute();
                tileManager.save();
            }
            return;
        }
        if (launchBarResults.size() < index) return;
        if (launchBarResults.get(index).getTile() == null) return;
        Log.info("Executing tile (" + index + "): " + launchBarResults.get(index).getTile());
        launchBarResults.get(index).getTile().execute();
        tileManager.save();
    }

    private Tile mathResultsTile = new Tile("");

    private boolean checkForMathEval() {
        try {
            String result = "" + MathEval.evaluate(currentSearch.replace("d", ""));
            if (!currentSearch.contains("d")) result = result.replaceAll("(.+)\\.0+", "$1");
            mathResultsTile.setLabel(result);
            GeneralUtils.copyString(result);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private LaunchBar launchBar;
    private long lastActivationKeyPressMillis = getTime();
    private int maxPeriod = 300, minPeriod = 60;

    private void activationKeyPress() {
        long millis = getTime();
        if (millis < lastActivationKeyPressMillis + maxPeriod && millis > lastActivationKeyPressMillis + minPeriod) {
            launchBar.activate();
            launchBarResults.forEach(launchBarResult -> new Thread(launchBarResult::prepareNow).start());
        }
        lastActivationKeyPressMillis = millis;
    }

    private void cancelKeyPress() {
        launchBar.deactivate();
        launchBarResults.forEach(LaunchBarResult::deactivate);
    }

    public void save() {
        try {
            tileManager.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static long getTime() {
        return System.currentTimeMillis();
    }

    public Color getColorForCategory(String category) {
        return tileManager.getColorForCategory(category);
    }

    public void setCategories(ArrayList<Pair<String, String>> categories) {
        tileManager.setCategories(categories);
    }

    private final static File CONFIG_FILE = new File("res/config.json");

    public void setConfig(String key, String value) throws IOException {
        config.put(key, value);
        CONFIG_FILE.write(config.toString());
    }

    public String getConfig(String key) {
        if (config.has(key))
            return config.getString(key);
        return "";
    }

    public String getConfigOrSetDefault(String key, String def) throws IOException {
        if (config.has(key))
            return config.getString(key);
        setConfig(key, def);
        CONFIG_FILE.write(config.toString());
        return def;
    }

    public int getConfigIntegerOrSetDefault(String key, int def) throws IOException {
        if (config.has(key)) {
            try {
                return Integer.parseInt(config.getString(key));
            } catch (Exception ignored) {
            }
        }
        setConfig(key, "" + def);
        CONFIG_FILE.write(config.toString());
        return def;
    }

    public static void setOpenMode(boolean barMode) throws IOException {
        self.setConfig("openMode", barMode ? "bar" : "settings");
        File launchAnything = new File("launch-anything.jar");
        if (launchAnything.exists()) FileUtils.openJar("launch-anything.jar", ".", new String[]{});
        System.exit(0);
    }

    public final static File AUTOSTART_SHORTCUT = new File("C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\launch-anything.lnk");

    public void setAutostart(boolean active) {
        try {
            if (active) {
                if (!getAutostartState()) {
                    ShellLink.createLink("launch-anything.jar", "launch-anything.lnk");
                    File lnk = new File("launch-anything.lnk");
                    lnk.copyFile(AUTOSTART_SHORTCUT);
                    lnk.delete();
                    new BlurNotification("Created shortcut");
                }
            } else {
                if (getAutostartState()) {
                    AUTOSTART_SHORTCUT.delete();
                    new BlurNotification("Removed shortcut");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            new BlurNotification("Unable to set shortcut state to (\" + active + \")");
            new BlurNotification(e.toString());
        }
    }

    public boolean getAutostartState() {
        return AUTOSTART_SHORTCUT.exists();
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
