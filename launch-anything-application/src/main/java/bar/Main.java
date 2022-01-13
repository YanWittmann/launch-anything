package bar;

import bar.common.Sleep;
import bar.common.VersionUtil;
import bar.logic.BarManager;
import bar.logic.Settings;
import bar.logic.UndoHistory;
import bar.tile.*;
import bar.ui.TrayUtil;
import bar.util.GlobalKeyListener;
import bar.util.Util;
import bar.webserver.HTTPServer;
import lc.kra.system.keyboard.event.GlobalKeyEvent;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.awt.Desktop.getDesktop;

public class Main {

    private static final Pattern GET_PATTERN = Pattern.compile("GET /\\?(.+) HTTP/\\d.+");
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static String versionString;
    public final String version;

    private List<Tile> lastTiles = new ArrayList<>();
    private int currentResultIndex = 0;
    private Tile lastExecutedTile;

    private final List<String> inputHistory = new ArrayList<>();
    private int currentInputHistoryIndex = 0;
    private String currentInput;
    private String storedUserInput;

    private int lastPressedKey = -1;
    private boolean isModifyKeyPressed = false;

    private final Settings settings;
    private final BarManager barManager;
    private final TileManager tileManager;
    private final UndoHistory undoHistory = new UndoHistory();


    private Main(String[] args) {
        String ver;
        try {
            Properties props = new Properties();
            props.load(Main.class.getClassLoader().getResourceAsStream("project.properties"));
            ver = props.getProperty("application.version");
        } catch (IOException e) {
            ver = "unknown";
            LOG.error("error ", e);
        }
        version = ver;
        versionString = "V" + version;

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ignored) {
            // do nothing
        }

        LOG.info("Launching application version [{}] on OS [{}]", version, Util.getOS());
        TrayUtil.init(this);

        settings = new Settings();
        barManager = new BarManager(settings);
        tileManager = new TileManager();
        barManager.addInputListener(this::userInput);
        tileManager.addOnInputEvaluatedListener(this::onInputEvaluated);

        final long[] lastCommandInput = {System.currentTimeMillis()};
        GlobalKeyListener keyListener = new GlobalKeyListener();
        keyListener.addListener(new GlobalKeyListener.KeyListener() {
            @Override
            public void keyPressed(GlobalKeyEvent e) {
                int code = e.getVirtualKeyCode();
                if (code == settings.getInt(Settings.Setting.MODIFY_KEY)) {
                    isModifyKeyPressed = true;
                } else if (code == settings.getInt(Settings.Setting.ACTIVATION_KEY)) {
                    long currentTime = System.currentTimeMillis();
                    if (timeoutUntil < currentTime && currentTime - lastCommandInput[0] < settings.getInt(Settings.Setting.ACTIVATION_DELAY) && currentTime - lastCommandInput[0] > 50) {
                        TrayUtil.setMenuItemActive(0, false);
                        barManager.setInputActive(true);
                        storedUserInput = null;
                        currentInputHistoryIndex = inputHistory.size();
                    }
                    lastCommandInput[0] = currentTime;
                } else if (code == settings.getInt(Settings.Setting.CANCEL_KEY)) {
                    barManager.setInputActive(false);
                } else if (code == settings.getInt(Settings.Setting.CONFIRM_KEY) && !isModifyKeyPressed) {
                    boolean isInputActive = barManager.isInputActive();
                    if (isInputActive) {
                        barManager.setInputActive(false);
                        executeTopmostTile();
                    }
                } else if (code == settings.getInt(Settings.Setting.CONFIRM_KEY) && isModifyKeyPressed) {
                    boolean isInputActive = barManager.isInputActive();
                    if (isInputActive) {
                        modifyTopmostTile();
                    }
                } else if (code == settings.getInt(Settings.Setting.PREVIOUS_RESULT_KEY) || code == settings.getInt(Settings.Setting.NEXT_RESULT_KEY)) {
                    if (isModifyKeyPressed) {
                        scrollThroughInputHistory(code == settings.getInt(Settings.Setting.NEXT_RESULT_KEY));
                    } else {
                        scrollThroughResultBars(code == settings.getInt(Settings.Setting.NEXT_RESULT_KEY));
                    }
                } else if ((code == settings.getInt(Settings.Setting.PREVIOUS_RESULT_KEY) || code == settings.getInt(Settings.Setting.NEXT_RESULT_KEY)) && !inputHistory.isEmpty()) {
                    if (code == settings.getInt(Settings.Setting.PREVIOUS_RESULT_KEY))
                        currentInputHistoryIndex = Math.max(0, currentInputHistoryIndex - 1);
                    else currentInputHistoryIndex = Math.min(inputHistory.size() - 1, currentInputHistoryIndex + 1);
                    barManager.setInput(inputHistory.get(Math.max(0, currentInputHistoryIndex)));
                } else if (code == settings.getInt(Settings.Setting.LEFT_ARROW_KEY) || code == settings.getInt(Settings.Setting.RIGHT_ARROW_KEY)) {
                    barManager.setInputCaretVisible(true);
                }
                lastPressedKey = code;
            }

            @Override
            public void keyReleased(GlobalKeyEvent e) {
                int code = e.getVirtualKeyCode();
                if (code == settings.getInt(Settings.Setting.MODIFY_KEY)) {
                    isModifyKeyPressed = false;
                }
            }
        });
        keyListener.activate();

        boolean willRestartWebServer = false;
        for (String arg : args) {
            if (arg.equals("-ws")) {
                willRestartWebServer = true;
                break;
            }
        }

        if (willRestartWebServer) {
            new Thread(() -> {
                try {
                    TrayUtil.showMessage("LaunchAnything " + versionString + " is now active.\nSettings page will be available in a few seconds.");
                    Sleep.milliseconds(10000);
                    openSettingsWebServer(false);
                } catch (Exception e) {
                    TrayUtil.showError("Failed to start web server: " + e.getMessage());
                    LOG.error("error ", e);
                }
            }).start();
        } else {
            TrayUtil.showMessage("LaunchAnything " + versionString + " is now active.");
        }

        new Thread(() -> {
            Util.cleanupTempFiles();
            if (!isVersionSnapshot()) checkForNewVersion();
            final File elevatorFile = new File("elevator.jar");
            if (elevatorFile.exists()) {
                Sleep.seconds(4);
                if (elevatorFile.exists() && !elevatorFile.delete()) {
                    TrayUtil.showError("Failed to delete elevator.jar");
                }
            }

            if (tileManager.isFirstLaunch() && isStartedFromJarAndIsAutostartDisabled()) {
                new Thread(() -> {
                    String activateAutostart = Util.popupChooseButton(
                            "LaunchAnything",
                            "Do you want LaunchAnything to start on system startup?\n" +
                            "This can be activated / deactivated in the settings later on.",
                            new String[]{"Yes", "No"});
                    if ("Yes".equals(activateAutostart)) {
                        Util.setAutostartActive(true);
                    }
                }).start();
            }
        }).start();

        try {
            String url = settings.getStringOrNull(Settings.Setting.CLOUD_TIMER_URL);
            String username = settings.getStringOrNull(Settings.Setting.CLOUD_TIMER_USERNAME);
            String password = settings.getStringOrNull(Settings.Setting.CLOUD_TIMER_PASSWORD);
            if (url != null && username != null && password != null
                && !url.isEmpty() && !username.isEmpty() && !password.isEmpty()
                && !url.equals("null") && !password.equals("null")) {
                tileManager.addCloudAccess(settings);
                tileManager.synchronizeCloudTiles();
            }
        } catch (IOException e) {
            TrayUtil.showError("Failed to get cloud tile access: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Util.registerFont("font/Comfortaa-Regular.ttf");
        new Main(args);
    }

    public static boolean isVersionSnapshot() {
        return versionString.contains("SNAPSHOT");
    }

    private void userInput(String input) {
        tileManager.evaluateUserInput(input);
        currentInput = input;
    }

    private void executeTopmostTile() {
        if (currentResultIndex < lastTiles.size()) {
            lastExecutedTile = lastTiles.get(currentResultIndex);
        }
        if (lastExecutedTile != null) {
            lastExecutedTile.execute(this);
            tileManager.save();
            addInputToHistory();
        }
    }

    private void addInputToHistory() {
        if (inputHistory.isEmpty() || !currentInput.equals(inputHistory.get(inputHistory.size() - 1))) {
            inputHistory.add(currentInput);
        }
        if (inputHistory.size() > 30) {
            inputHistory.remove(0);
        }
    }

    private long lastHistoryScroll = 0;

    private void scrollThroughInputHistory(boolean direction) {
        if (!inputHistory.isEmpty() && System.currentTimeMillis() - lastHistoryScroll > 100) {
            lastHistoryScroll = System.currentTimeMillis();
            int oldIndex = currentInputHistoryIndex;
            if (direction) {
                currentInputHistoryIndex = Math.min(inputHistory.size(), currentInputHistoryIndex + 1); // up
            } else {
                currentInputHistoryIndex = Math.max(0, currentInputHistoryIndex - 1); // down
            }
            if (oldIndex != currentInputHistoryIndex) {
                if (currentInputHistoryIndex == inputHistory.size()) {
                    barManager.setInput(storedUserInput);
                    storedUserInput = null;
                } else {
                    if (storedUserInput == null) {
                        storedUserInput = currentInput;
                    }
                    barManager.setInput(inputHistory.get(currentInputHistoryIndex));
                }
            }
        }
    }

    private void scrollThroughResultBars(boolean direction) {
        if (!lastTiles.isEmpty()) {
            if (direction) {
                currentResultIndex = Math.min(lastTiles.size() - 1, currentResultIndex + 1); // down
            } else {
                currentResultIndex = Math.max(0, currentResultIndex - 1); // up
            }
            updateResultBars();
        }
    }

    private void updateResultBars() {
        barManager.setTiles(lastTiles, currentResultIndex, tileManager.getCategories());
    }

    private void modifyTopmostTile() {
        if (currentResultIndex < lastTiles.size()) {
            lastExecutedTile = lastTiles.get(currentResultIndex);
            barManager.setInputActive(false);
            if (lastExecutedTile != null) {
                String selectedOption = Util.popupChooseButton("LaunchAnything", "What do you want to edit?", new String[]{"Name", "Action", "Both", "Cancel"});
                if (selectedOption != null) {
                    switch (selectedOption) {
                        case "Action":
                            if (!isToBeContinuedAfterEvaluatingEditType())
                                return;
                            break;
                        case "Both":
                            if (!isToBeContinuedAfterEvaluatingEditType())
                                return;
                            if (lastExecutedTile.getFirstAction() != null) {
                                setLabelToLastExecutedTile(lastExecutedTile.getFirstAction().getExampleTileLabel());
                            } else {
                                setLabelToLastExecutedTile(lastExecutedTile.getLabel());
                            }
                            break;
                        case "Name":
                            setLabelToLastExecutedTile(lastExecutedTile.getLabel());
                            break;
                        case "Cancel":
                            return;
                        default:
                            break;
                    }
                }
                tileManager.save();
            }
        }
    }

    private boolean isToBeContinuedAfterEvaluatingEditType() {
        TileAction firstAction = lastExecutedTile.getFirstAction();
        String param1 = null;
        String param2 = null;
        boolean removeActionAfterwards = false;
        if (firstAction != null) {
            String editType = Util.popupChooseButton("LaunchAnything", "Do you want to modify the action type or only the parameters?", new String[]{"Parameters", "Type and Parameters", "Cancel"});
            if ("Parameters".equals(editType)) {
                param1 = firstAction.getParam1();
                param2 = firstAction.getParam2();
            } else if ("Type and Parameters".equals(editType)) {
                removeActionAfterwards = true;
            } else if ("Cancel".equals(editType)) {
                return false;
            }
        }
        if (createOrEditNewTileAction(lastExecutedTile, param1, param2) != null && removeActionAfterwards) {
            lastExecutedTile.removeAction(firstAction);
        }
        return true;
    }

    private void setLabelToLastExecutedTile(String templateName) {
        String newName = Util.popupTextInput("LaunchAnything", "Enter new name:", templateName);
        if (newName != null && !newName.isEmpty() && !newName.equals("null")) {
            lastExecutedTile.setLabel(newName);
        }
    }

    private void onInputEvaluated(List<Tile> tiles) {
        lastTiles = tiles;
        currentResultIndex = 0;
        barManager.setTiles(lastTiles, currentResultIndex, tileManager.getCategories());
    }

    private HTTPServer webserver;

    public void openSettingsWebServer(boolean openWebpage) {
        int port = 36345;
        if (!isWebserverOpen()) {
            new Thread(() -> {
                try {
                    webserver = new HTTPServer(port);
                    webserver.addListener(this::handleSettingsWebServer);
                    webserver.open();
                } catch (IOException e) {
                    TrayUtil.showError("Unable to start settings webserver on port " + port);
                    LOG.error("error ", e);
                }
            }).start();
            LOG.info("Settings webserver started on port {}", port);
        }
        if (openWebpage) {
            Sleep.milliseconds(300);
            try {
                getDesktop().browse(new URI(webserver.getUrl() + "/?p=" + port));
            } catch (Exception e) {
                TrayUtil.showError("Unable to open url " + webserver.getUrl() + "/?p=" + port);
                LOG.error("error ", e);
            }
        }
    }

    public void restartBar() throws URISyntaxException, IOException {
        Util.restartApplication(isWebserverOpen());
    }

    public boolean isWebserverOpen() {
        return webserver != null;
    }

    private void handleSettingsWebServer(BufferedReader in, BufferedWriter out) throws IOException {
        try {
            List<String> request = new ArrayList<>();
            String s;
            while ((s = in.readLine()) != null) {
                request.add(s);
                if (s.isEmpty()) {
                    break;
                }
            }

            Map<String, String> getParams = new HashMap<>();
            for (String line : request) {
                if (line.startsWith("GET")) {
                    Matcher getMatcher = GET_PATTERN.matcher(line);
                    if (getMatcher.find() && getMatcher.groupCount() > 0) {
                        for (String get : getMatcher.group(1).split("&")) {
                            String[] split = get.split("=");
                            if (split.length == 2) getParams.put(split[0], Util.urlDecode(split[1]));
                        }
                    }
                }
            }
            LOG.info("- - - - - - - - - - - - - - - - - - - - - - - - - -");
            getParams.forEach((k, v) -> LOG.info("{}: {}", k, v));

            if (getParams.containsKey("action")) {
                try {
                    JSONObject response = new JSONObject();
                    String action = getParams.getOrDefault("action", null);

                    if (getParams.get("action").equals("getAllTiles")) {
                        response.put(
                                "tiles",
                                tileManager.toJSON()
                                        .put("runtime-tiles", TileManager.getRuntimeTilesNames())
                                        .put("settings", settings.toSettingsJSON()));

                    } else if (action.equals("metaInteraction")) {

                        String editType = getParams.getOrDefault("editType", null);

                        if (editType != null) {
                            switch (editType) {
                                case "restartBar":
                                    restartBar();
                                    break;
                                case "activateAutostart":
                                    Util.setAutostartActive(true);
                                    break;
                                case "deactivateAutostart":
                                    Util.setAutostartActive(false);
                                    break;
                                case "undo":
                                    JSONObject undo = undoHistory.undo(tileManager.toJSON());
                                    if (undo != null) {
                                        tileManager.loadTilesFromJson(undo);
                                        tileManager.save();
                                        response.put("message", "Undo successful");
                                    } else {
                                        response.put("message", "Nothing to undo");
                                    }
                                    break;
                                case "redo":
                                    JSONObject redo = undoHistory.redo(tileManager.toJSON());
                                    if (redo != null) {
                                        tileManager.loadTilesFromJson(redo);
                                        tileManager.save();
                                        response.put("message", "Redo successful");
                                    } else {
                                        response.put("message", "Nothing to redo");
                                    }
                                    break;
                                case "settingsTemplateSizeSmall":
                                    settings.loadTemplate("sizeSmall");
                                    barManager.barReloadRequest();
                                    break;
                                case "settingsTemplateSizeMedium":
                                    settings.loadTemplate("sizeMedium");
                                    barManager.barReloadRequest();
                                    break;
                                case "settingsTemplateSizeNormal":
                                    settings.loadTemplate("sizeNormal");
                                    barManager.barReloadRequest();
                                    break;
                                default:
                                    break;
                            }
                        }

                    } else if (action.equals("tileInteraction")) {

                        String editType = getParams.getOrDefault("editType", null);
                        String whatToEdit = getParams.getOrDefault("whatToEdit", null);
                        String additionalValue = getParams.getOrDefault("additionalValue", null);

                        if (whatToEdit != null && editType != null) {
                            undoHistory.add(tileManager.toJSON());
                            if (editType.equals("tile")) {
                                String tileId = getParams.getOrDefault("tileId", null);
                                Tile tile = tileManager.findTile(tileId);
                                if (tile != null) {
                                    switch (whatToEdit) {
                                        case "createAction":
                                        case "editAction":
                                            createOrEditNewTileAction(tile, additionalValue, null);
                                            break;
                                        case "deleteAction":
                                            TileAction tileAction = tile.findTileAction(additionalValue, null);
                                            if (tileAction != null) {
                                                tile.removeAction(tileAction);
                                            }
                                            break;
                                        case "deleteTile":
                                            tileManager.removeTile(tile);
                                            break;
                                        case "editName":
                                            String name = Util.popupTextInput("Edit Tile Name", "Enter the new name", tile.getLabel());
                                            if (name != null && name.length() > 0 && !name.equals("null")) {
                                                tile.setLabel(name);
                                            }
                                            break;
                                        case "editCategory":
                                            String category = Util.popupDropDown(
                                                    "Edit Tile Category",
                                                    "Select the new category",
                                                    tileManager.getCategories().stream().map(TileCategory::getLabel).toArray(String[]::new),
                                                    tile.getCategory());
                                            if (category != null && !category.equals("null")) {
                                                tile.setCategory(category);
                                            }
                                            break;
                                        case "addKeyword":
                                            String keyword = Util.popupTextInput("Add Keyword", "Enter the new keyword", null);
                                            if (keyword != null && keyword.length() > 0 && !keyword.equals("null")) {
                                                tile.addKeyword(keyword);
                                            }
                                            break;
                                        case "editKeyword":
                                            keyword = Util.popupTextInput("Edit Keyword", "Enter the new keyword", null);
                                            if (keyword != null && keyword.length() > 0 && !keyword.equals("null")) {
                                                tile.editKeyword(additionalValue, keyword);
                                            }
                                            break;
                                        case "deleteKeyword":
                                            tile.removeKeyword(additionalValue);
                                            break;
                                        default:
                                            break;
                                    }
                                    if (tileManager.isCloudTile(tile)) {
                                        tileManager.cloudTileHasBeenEdited(tile);
                                        tileManager.synchronizeCloudTiles();
                                    }
                                } else {
                                    if (whatToEdit.equals("createTile")) {
                                        createTile(false);
                                    }
                                }
                            } else if (editType.equals("category")) {
                                String categoryId = getParams.getOrDefault("categoryName", null);
                                TileCategory category = tileManager.findCategory(categoryId);
                                if (category != null) {
                                    switch (whatToEdit) {
                                        case "editColor":
                                            Color color = JColorChooser.showDialog(null, "Choose a color", Color.RED);
                                            if (color != null) {
                                                category.setColor(color);
                                            }
                                            break;
                                        case "deleteCategory":
                                            tileManager.removeCategory(category);
                                            break;
                                        default:
                                            break;
                                    }
                                } else {
                                    if (whatToEdit.equals("createCategory")) {
                                        String categoryName = Util.popupTextInput("Create Category", "Enter the new category name", null);
                                        if (categoryName != null && categoryName.length() > 0 && !categoryName.equals("null")) {
                                            Color color = JColorChooser.showDialog(null, "Choose a color", Color.RED);
                                            if (color != null) {
                                                tileManager.addCategory(new TileCategory(categoryName, color));
                                            }
                                        }
                                    }
                                }
                            } else if (editType.equals("generator")) {
                                String generatorId = getParams.getOrDefault("tileId", null);
                                TileGenerator generator = tileManager.findTileGenerator(generatorId);
                                if (generator != null) {
                                    switch (whatToEdit) {
                                        case "editCategory":
                                            String category = Util.popupDropDown(
                                                    "Edit Generator Category",
                                                    "Select the new category",
                                                    tileManager.getCategories().stream().map(TileCategory::getLabel).toArray(String[]::new),
                                                    generator.getCategory());
                                            if (category != null && !category.equals("null")) {
                                                generator.setCategory(category);
                                            }
                                            break;
                                        case "deleteGenerator":
                                            tileManager.removeTileGenerator(generator);
                                            break;
                                        case "addKeyword":
                                            String keyword = Util.popupTextInput("Add Keyword", "Enter the new keyword", null);
                                            if (keyword != null && keyword.length() > 0 && !keyword.equals("null")) {
                                                generator.addKeyword(keyword);
                                            }
                                            break;
                                        case "editKeyword":
                                            keyword = Util.popupTextInput("Edit Keyword", "Enter the new keyword", null);
                                            if (keyword != null && keyword.length() > 0 && !keyword.equals("null")) {
                                                generator.editKeyword(additionalValue, keyword);
                                            }
                                            break;
                                        case "deleteKeyword":
                                            generator.removeKeyword(additionalValue);
                                            break;
                                        case "createAction":
                                            createOrEditTileGeneratorGenerator(generator, null);
                                            break;
                                        case "editAction":
                                            String generatorGeneratorId = getParams.getOrDefault("additionalValue", null);
                                            createOrEditTileGeneratorGenerator(generator, generatorGeneratorId);
                                            break;
                                        case "deleteAction":
                                            generatorGeneratorId = getParams.getOrDefault("additionalValue", null);
                                            TileGeneratorGenerator tileGeneratorGenerator = generator.findGenerator(generatorGeneratorId);
                                            TileGenerator tileGenerator = tileManager.findTileGenerator(generatorId);
                                            if (tileGenerator != null && tileGeneratorGenerator != null) {
                                                tileGenerator.removeGenerator(tileGeneratorGenerator);
                                                tileManager.regenerateGeneratedTiles();
                                            }
                                            break;
                                        default:
                                            break;
                                    }
                                } else {
                                    if (whatToEdit.equals("createGenerator")) {
                                        generator = new TileGenerator();
                                        TileGeneratorGenerator gen = createOrEditTileGeneratorGenerator(generator, generatorId);
                                        if (gen != null) {
                                            generator.setCategory(gen.getType());
                                            generator.setKeywords(gen.getType());
                                            tileManager.addTileGenerator(generator);
                                        }
                                    }
                                }
                            } else if (editType.equals("runtime")) {
                                String runtimeId = getParams.getOrDefault("tileId", null);
                                if (runtimeId != null) {
                                    tileManager.toggleRuntimeTile(runtimeId);
                                }
                            } else if (editType.equals("setting")) {
                                if (whatToEdit.equals("resetSettings")) {
                                    resetSettings();
                                } else {
                                    Object newValue;
                                    if (whatToEdit.toLowerCase().contains("key")) {
                                        Util.popupMessage("Edit Key", "After closing this popup, press any key you want to assign this action to.");
                                        lastPressedKey = -1;
                                        while (lastPressedKey == -1) {
                                            Sleep.milliseconds(100);
                                        }
                                        newValue = lastPressedKey;
                                    } else if (whatToEdit.toLowerCase().contains("bool")) {
                                        newValue = Util.popupChooseButton("Edit Setting", "True or False?", new String[]{"True", "False"}).toLowerCase();
                                    } else if (whatToEdit.toLowerCase().endsWith("font")) {
                                        String whereToPickFontFrom = Util.popupChooseButton("Choose Font", "From where do you want to load the font?", new String[]{"System Font", "From TTF File"});
                                        if (whereToPickFontFrom != null) {
                                            if (whereToPickFontFrom.equals("System Font")) {
                                                newValue = Util.popupDropDown("Choose Font", "Choose the font you want to use.", GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(), settings.getString(whatToEdit));
                                            } else {
                                                newValue = Util.pickFile("TTF File", "ttf");
                                            }
                                        } else {
                                            newValue = null;
                                        }
                                    } else {
                                        newValue = Util.popupTextInput("Edit Setting", "Enter the new value for the setting:", settings.getString(whatToEdit));
                                    }
                                    if (newValue != null && (newValue + "").length() > 0 && !newValue.equals("null")) {
                                        settings.setSetting(whatToEdit, newValue);
                                        barManager.barReloadRequest();
                                    }
                                }
                            } else {
                                undoHistory.undo(null);
                            }
                            tileManager.cleanUpTileActions();
                            tileManager.save();
                        }
                    } else if (action.equals("cloudTileInteraction")) {

                        String whatToEdit = getParams.getOrDefault("whatToEdit", null);

                        if (whatToEdit != null) {
                            switch (whatToEdit) {
                                case "cloud-configure-server":
                                    String before = settings.getString(Settings.Setting.CLOUD_TIMER_URL);
                                    String after = Util.popupTextInput("Cloud Configure Server", "Enter the server address, leave empty to clear url:", before == null || before.equals("null") ? "" : before);
                                    if (after != null && !after.equals("null")) {
                                        settings.setSettingSilent(Settings.Setting.CLOUD_TIMER_URL, after);
                                        tileManager.addCloudAccess(settings);
                                    }
                                    break;
                                case "cloud-synchronize-tiles":
                                    break;
                                case "cloud-login":
                                    before = settings.getString(Settings.Setting.CLOUD_TIMER_USERNAME);
                                    String username = Util.popupTextInput("Cloud Configure Server", "Enter your username:", before == null || before.equals("null") ? "" : before);
                                    if (username != null) {
                                        String password = Util.popupTextInput("Cloud Configure Server", "Enter your password:", "");
                                        if (password != null) {
                                            settings.setSettingSilent(Settings.Setting.CLOUD_TIMER_USERNAME, username);
                                            settings.setSettingSilent(Settings.Setting.CLOUD_TIMER_PASSWORD, password);
                                            tileManager.cloudResetAll();
                                            tileManager.addCloudAccess(settings);
                                        }
                                    }
                                    TrayUtil.showMessage("You have successfully logged in to the cloud server.");
                                    break;
                                case "cloud-logout":
                                    String confirmLogout = Util.popupChooseButton("Cloud Configure Server", "Are you sure you want to logout?", new String[]{"Yes", "No"});
                                    if (confirmLogout.equals("Yes")) {
                                        settings.setSettingSilent(Settings.Setting.CLOUD_TIMER_USERNAME, null);
                                        settings.setSettingSilent(Settings.Setting.CLOUD_TIMER_PASSWORD, null);
                                        tileManager.cloudResetAll();
                                        tileManager.addCloudAccess(settings);
                                    }
                                    TrayUtil.showMessage("You have successfully logged out of the cloud server.");
                                    break;
                                case "cloud-create-user":
                                    username = Util.popupTextInput("Cloud Configure Server", "Enter your username:", "");
                                    String password = Util.popupTextInput("Cloud Configure Server", "Enter your password:", "");
                                    String confirmPassword = Util.popupTextInput("Cloud Configure Server", "Confirm your password:", "");
                                    if (username != null && password != null && confirmPassword != null && username.length() > 0 && password.length() > 0 && confirmPassword.length() > 0 && password.equals(confirmPassword)) {
                                        settings.setSettingSilent(Settings.Setting.CLOUD_TIMER_USERNAME, username);
                                        settings.setSettingSilent(Settings.Setting.CLOUD_TIMER_PASSWORD, password);
                                        tileManager.cloudResetAll();
                                        tileManager.addCloudAccessCreateAccount(settings);
                                        TrayUtil.showMessage("You have successfully created an account on the cloud server.");
                                    }
                                    break;
                                case "cloud-modify-username":
                                    if (tileManager.getCloudAccess() == null) {
                                        TrayUtil.showError("You must login to modify your username.");
                                    } else {
                                        String newUsername = Util.popupTextInput("Cloud Configure Server", "Enter your new username:", "");
                                        if (newUsername != null && newUsername.length() > 0) {
                                            tileManager.getCloudAccess().modifyUserName(newUsername);
                                            settings.setSettingSilent(Settings.Setting.CLOUD_TIMER_USERNAME, newUsername);
                                            tileManager.addCloudAccess(settings);
                                            TrayUtil.showMessage("You have successfully modified your username.");
                                        }
                                    }
                                    break;
                                case "cloud-modify-password":
                                    if (tileManager.getCloudAccess() == null) {
                                        TrayUtil.showError("You must login to modify your password.");
                                    } else {
                                        String newPassword = Util.popupTextInput("Cloud Configure Server", "Enter your new password:", "");
                                        if (newPassword != null && newPassword.length() > 0) {
                                            tileManager.getCloudAccess().modifyUserPassword(newPassword);
                                            settings.setSettingSilent(Settings.Setting.CLOUD_TIMER_PASSWORD, newPassword);
                                            tileManager.addCloudAccess(settings);
                                            TrayUtil.showMessage("You have successfully modified your password.");
                                        }
                                    }
                                    break;
                                case "cloud-remove-user":
                                    if (tileManager.getCloudAccess() == null) {
                                        TrayUtil.showError("You must login to delete your account.");
                                    } else {
                                        String confirmRemove = Util.popupChooseButton("Cloud Configure Server", "Are you sure you want to remove your account?\nThis is a destructive action. All your cloud tiles will be deleted.", new String[]{"Yes", "No"});
                                        if (confirmRemove.equals("Yes")) {
                                            tileManager.getCloudAccess().removeUser();
                                            settings.setSettingSilent(Settings.Setting.CLOUD_TIMER_USERNAME, null);
                                            settings.setSettingSilent(Settings.Setting.CLOUD_TIMER_PASSWORD, null);
                                            tileManager.cloudResetAll();
                                            tileManager.addCloudAccess(settings);
                                            TrayUtil.showMessage("You have successfully removed your account.");
                                        } else {
                                            TrayUtil.showMessage("You have not removed your account.");
                                        }
                                    }
                                    break;
                                case "add-new-cloud-tile":
                                    if (tileManager.getCloudAccess() == null) {
                                        TrayUtil.showError("You must login to create a new cloud tile.");
                                    } else {
                                        createTile(true);
                                    }
                                    break;
                            }
                        }

                        tileManager.synchronizeCloudTiles();
                    }

                    out.write("HTTP/1.0 200 OK\r\n");
                    out.write("Date: " + new Date() + "\r\n");
                    out.write("Server: Java/1.0\r\n");
                    out.write("Content-Type: application/json\r\n");
                    out.write("\r\n");
                    out.write(response.toString());

                    if (barManager.isInputActive()) {
                        userInput(currentInput);
                    }
                } catch (Exception e) {
                    TrayUtil.showError("Something went wrong: " + e.getMessage());
                    setResponseError(500, "Something went wrong: " + e.getMessage() + ", " + Arrays.toString(e.getStackTrace()), out);
                    LOG.error("error ", e);
                }
            } else {
                out.write("HTTP/1.0 200 OK\r\n");
                out.write("Date: " + new Date() + "\r\n");
                out.write("Server: Java/1.0\r\n");
                out.write("Content-Type: text/html\r\n");
                out.write("\r\n");
                out.write(Util.readClassResource("web/settings.html"));
            }
        } catch (Exception e) {
            LOG.info("Something went wrong while answering to the client: {}", e.getMessage());
            LOG.error("error ", e);
            openSettingsWebServer(false);
        }
    }

    public void resetSettings() {
        String confirmation = Util.popupTextInput("Reset Settings", "Are you sure you want to reset all settings?\nEnter 'confirm' to delete the settings:", "");
        if (confirmation != null && confirmation.equals("confirm")) {
            settings.reset(true);
            settings.save();
            barManager.barReloadRequest();
        }
    }

    public void cloudSync() {
        tileManager.synchronizeCloudTiles();
    }

    private TileGeneratorGenerator createOrEditTileGeneratorGenerator(TileGenerator generator, String id) {
        TileGeneratorGenerator tileGeneratorGenerator;
        if (id == null) {
            tileGeneratorGenerator = null;
        } else {
            tileGeneratorGenerator = generator.findGenerator(id);
            generator.removeGenerator(tileGeneratorGenerator);
        }

        String generatorType = Util.popupDropDown("Generator", "Select the generator type", TileGeneratorGenerator.GENERATOR_TYPES, tileGeneratorGenerator != null ? tileGeneratorGenerator.getType() : null);

        if (generatorType != null) {
            if (generatorType.equals("file")) {
                File file = Util.pickDirectory();
                if (file != null) {
                    String filter = Util.popupTextInput("Generator", "Leave empty or enter file extensions", tileGeneratorGenerator != null ? tileGeneratorGenerator.getParam2() : null);
                    if (filter != null) {
                        tileGeneratorGenerator = new TileGeneratorGenerator(generatorType, file.getAbsolutePath(), filter.length() > 0 ? filter : null);
                    }
                }
            }
            if (tileGeneratorGenerator != null) {
                generator.addGenerator(tileGeneratorGenerator);
                tileManager.regenerateGeneratedTiles();
                return tileGeneratorGenerator;
            }
        }

        tileManager.regenerateGeneratedTiles();
        return null;
    }

    private TileAction createOrEditNewTileAction(Tile tile, String param1, String param2) {
        TileAction tileAction;
        if (param1 == null && param2 == null) {
            tileAction = null;
        } else {
            tileAction = tile.findTileAction(param1, param2);
            param1 = null;
            param2 = null;
        }

        TileAction newTileAction;
        String actionType;
        if (tileAction != null) {
            actionType = tileAction.getType();
        } else {
            String actionFromSnippet = TileAction.getActionFromSnippet(Util.getClipboardText());
            actionType = Util.popupDropDown("Tile Action", "What type of action do you want to create?", TileAction.ACTION_TYPES, actionFromSnippet);
        }

        if (actionType != null) {
            String previousValue = tileAction != null && tileAction.getParam1() != null ? tileAction.getParam1() : "";
            switch (actionType) {
                case "file":
                    File file;
                    try {
                        File clipboardFile = new File(Util.getClipboardText());
                        file = Util.pickFile(clipboardFile, null);
                    } catch (Exception e) {
                        file = Util.pickFile(null);
                    }
                    if (file != null) {
                        param1 = file.getAbsolutePath();
                    }
                    break;
                case "directory":
                    actionType = "file";
                    try {
                        File clipboardFile = new File(Util.getClipboardText());
                        if (clipboardFile.exists() && clipboardFile.isDirectory()) Util.setPreviousFile(clipboardFile);
                    } catch (Exception e) {
                    }
                    file = Util.pickDirectory();
                    if (file != null) {
                        param1 = file.getAbsolutePath();
                    }
                    break;
                case "url":
                    String proposedUrl = previousValue;
                    try {
                        proposedUrl = new URL(Util.getClipboardText()).toString();
                    } catch (Exception e) {
                    }
                    param1 = Util.popupTextInput("Tile Action", "Enter the URL", proposedUrl);
                    break;
                case "copy":
                    param1 = Util.popupTextInput("Tile Action", "Enter the text to copy", Util.getClipboardText());
                    break;
                default:
                    break;
            }

            if (param1 != null) {
                newTileAction = new TileAction(actionType, param1, param2);
                tile.addAction(newTileAction);
                return newTileAction;
            }
        }

        return null;
    }

    public void createTile(boolean isCloudTile) {
        Tile newTile = new Tile();
        TileAction newTileAction = createOrEditNewTileAction(newTile, null, null);
        if (newTileAction != null) {
            String tileName = Util.popupTextInput("Create new Tile", "Enter the name of the new Tile:", newTileAction.getExampleTileLabel());
            if (tileName != null && tileName.length() > 0 && !tileName.equals("null")) {
                newTile.setLabel(tileName);
                newTile.setCategory(newTileAction.getType());
                if (isCloudTile) {
                    tileManager.addCloudTile(newTile);
                    tileManager.synchronizeCloudTiles();
                } else {
                    tileManager.addTile(newTile);
                }
                tileManager.save();
            }
        }
    }

    private long timeoutUntil = 0;

    public void timeout(int duration) {
        if (duration == 0) {
            timeoutUntil = 0;
            TrayUtil.setMenuItemActive(0, false);
        } else {
            LOG.info("Disabling input for {} minute(s)", duration);
            timeoutUntil = System.currentTimeMillis() + ((long) duration * 60 * 1000);
            TrayUtil.showMessage("LaunchBar is now disabled for " + duration + " minute" + (duration == 1 ? "" : "s"));
            TrayUtil.setMenuItemActive(0, true);
        }
    }

    private void setResponseError(int errorCode, String message, BufferedWriter out) throws IOException {
        switch (errorCode) {
            case 400:
                out.write("HTTP/1.0 400 Bad Request\r\n");
                break;
            case 500:
                out.write("HTTP/1.0 500 Internal Server Error\r\n");
                break;
            default:
                out.write("HTTP/1.0 400 Bad Request\r\n");
                break;
        }

        out.write("Date: " + new Date() + "\r\n");
        out.write("Server: Java/1.0\r\n");
        out.write("Content-Type: application/json\r\n");
        out.write("\r\n{\"error\":\"" + message + "\"}");
    }

    public void checkForNewVersion() {
        if (Util.isApplicationStartedFromJar()) {
            try {
                JSONObject latestVersion = new JSONObject(VersionUtil.getLatestVersionJson());
                String version = VersionUtil.extractVersion(latestVersion);
                String versionName = VersionUtil.extractVersionTitle(latestVersion);
                String versionUrl = VersionUtil.findAsset(latestVersion, "launch-anything.jar");
                String releaseDate = VersionUtil.extractReleaseDate(latestVersion);
                String versionBody = VersionUtil.extractBody(latestVersion);

                if (version != null && version.length() > 0 && !version.equals("null")) {
                    String compareVersion = version.replace("v", "");
                    if (!compareVersion.equals(this.version)) {

                        LOG.info("There is an update available:");
                        LOG.info("Latest version: {} ({})", version, versionName);
                        LOG.info("Release date: {}", releaseDate);
                        LOG.info("Download URL: {}", versionUrl);
                        String updateNow = Util.popupChooseButton("Update Available",
                                "There is an update available for the LaunchBar!\n\nLatest version: " + version + " (" + versionName + ")\nRelease date: " + releaseDate + "\nDownload URL: " + versionUrl + "\n\n" + versionBody + "\n\nDo you want to download it now?",
                                new String[]{"Download", "Ignore"});
                        if (updateNow != null && updateNow.equals("Download")) {
                            LOG.info("Copying elevator to outside the jar");
                            Util.copyResource("executables/elevator-jar-with-dependencies.jar", "elevator.jar");
                            LOG.info("Launching elevator.jar");

                            try {
                                Desktop.getDesktop().open(new File("elevator.jar"));
                                System.exit(0);
                            } catch (IOException e) {
                                LOG.error("error ", e);
                                TrayUtil.showError("Failed to open the elevator.jar file: " + e.getMessage());
                            }
                        }

                    }
                }

            } catch (Exception e) {
                LOG.info("Unable to check for new version: {}", e.getMessage());
            }
        }
    }

    public static String getVersionString() {
        return versionString;
    }

    private static boolean isStartedFromJarAndIsAutostartDisabled() {
        return Util.isApplicationStartedFromJar() && !Util.isAutostartEnabled();
    }
}