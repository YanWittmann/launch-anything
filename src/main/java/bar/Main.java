package bar;

import bar.logic.BarManager;
import bar.logic.Settings;
import bar.logic.UndoHistory;
import bar.tile.*;
import bar.ui.TrayUtil;
import bar.util.GlobalKeyListener;
import bar.util.Sleep;
import bar.util.Util;
import bar.webserver.HTTPServer;
import lc.kra.system.keyboard.event.GlobalKeyEvent;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.awt.Desktop.getDesktop;

public class Main {

    public static final String VERSION = "2.1";

    public static void main(String[] args) {
        Util.registerFont("font/Comfortaa-Regular.ttf");
        new Main(args);
    }

    private final Settings settings;
    private final BarManager barManager;
    private final TileManager tileManager;
    private final UndoHistory undoHistory = new UndoHistory();
    private int lastPressedKey = -1;

    private Main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ignored) {
        }

        System.out.println("Launching application version [" + VERSION + "] on OS [" + Util.getOS() + "]");
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
                if (e.getVirtualKeyCode() == settings.getInt(Settings.ACTIVATION_KEY)) {
                    long currentTime = System.currentTimeMillis();
                    if (timeoutUntil < currentTime && currentTime - lastCommandInput[0] < settings.getInt(Settings.ACTIVATION_DELAY) && currentTime - lastCommandInput[0] > 50) {
                        TrayUtil.setMenuItemActive(0, false);
                        barManager.setInputActive(true);
                    }
                    lastCommandInput[0] = currentTime;
                } else if (e.getVirtualKeyCode() == settings.getInt(Settings.CANCEL_KEY)) {
                    barManager.setInputActive(false);
                } else if (e.getVirtualKeyCode() == settings.getInt(Settings.CONFIRM_KEY) && lastPressedKey != settings.getInt(Settings.MODIFY_KEY)) {
                    boolean isInputActive = barManager.isInputActive();
                    if (isInputActive) {
                        barManager.setInputActive(false);
                        executeTopmostTile();
                    }
                } else if (e.getVirtualKeyCode() == settings.getInt(Settings.CONFIRM_KEY) && lastPressedKey == settings.getInt(Settings.MODIFY_KEY)) {
                    modifyTopmostTile();
                } else if (e.getVirtualKeyCode() == settings.getInt(Settings.PREVIOUS_RESULT_KEY)) {
                    currentResultIndex = Math.max(0, currentResultIndex - 1);
                    barManager.setTiles(lastTiles, currentResultIndex, tileManager.getCategories());
                } else if (e.getVirtualKeyCode() == settings.getInt(Settings.NEXT_RESULT_KEY)) {
                    currentResultIndex = Math.min(currentResultIndex + 1, lastTiles.size() - 1);
                    barManager.setTiles(lastTiles, currentResultIndex, tileManager.getCategories());
                }
                lastPressedKey = e.getVirtualKeyCode();
            }

            @Override
            public void keyReleased(GlobalKeyEvent e) {
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
                    TrayUtil.showMessage("LaunchAnything V" + VERSION + " is now active.\nSettings page will be available in a few seconds.");
                    Sleep.milliseconds(10000);
                    openSettingsWebServer(false);
                } catch (Exception e) {
                    TrayUtil.showError("Failed to start web server: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        } else {
            TrayUtil.showMessage("LaunchAnything is now active.");
        }
    }

    private void userInput(String input) {
        tileManager.evaluateUserInput(input);
    }

    private List<Tile> lastTiles = new ArrayList<>();
    private Tile lastExecutedTile;
    private int currentResultIndex = 0;

    private void executeTopmostTile() {
        if (currentResultIndex < lastTiles.size()) {
            lastExecutedTile = lastTiles.get(currentResultIndex);
        }
        if (lastExecutedTile != null) {
            lastExecutedTile.execute(this);
            tileManager.save();
        }
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
                        case "Both":
                            TileAction firstAction = lastExecutedTile.getFirstAction();
                            String param1 = null, param2 = null;
                            if (firstAction != null) {
                                String editType = Util.popupChooseButton("LaunchAnything", "Do you want to modify the action type or only the parameters?", new String[]{"Parameters", "Type and Parameters", "Cancel"});
                                if (editType != null) {
                                    if (editType.equals("Parameters")) {
                                        param1 = firstAction.getParam1();
                                        param2 = firstAction.getParam2();
                                    } else if (editType.equals("Type and Parameters")) {
                                        lastExecutedTile.removeAction(firstAction);
                                    } else if (editType.equals("Cancel")) {
                                        return;
                                    }
                                }
                            }
                            createOrEditNewTileAction(lastExecutedTile, param1, param2);
                            if (!selectedOption.equals("Both")) {
                                break;
                            }
                        case "Name":
                            String templateName = lastExecutedTile.getLabel();
                            if (selectedOption.equals("Both") && lastExecutedTile.getFirstAction() != null) {
                                templateName = lastExecutedTile.getFirstAction().getExampleTileLabel();
                            }
                            String newName = Util.popupTextInput("LaunchAnything", "Enter new name:", templateName);
                            if (newName != null && !newName.isEmpty() && !newName.equals("null")) {
                                lastExecutedTile.setLabel(newName);
                            }
                            break;
                        case "Cancel":
                            return;
                    }
                }
                tileManager.save();
            }
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
                    e.printStackTrace();
                }
            }).start();
            System.out.println("Settings webserver started on port " + port);
        }
        if (openWebpage) {
            Sleep.milliseconds(300);
            try {
                getDesktop().browse(new URI(webserver.getUrl() + "/?p=" + port));
            } catch (Exception e) {
                TrayUtil.showError("Unable to open url " + webserver.getUrl() + "/?p=" + port);
                e.printStackTrace();
            }
        }
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
            System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - -");
            getParams.forEach((k, v) -> System.out.println(k + ": " + v));

            if (getParams.containsKey("action")) {
                try {
                    JSONObject response = new JSONObject();
                    String action = getParams.getOrDefault("action", null);

                    if (getParams.get("action").equals("getAllTiles")) {
                        response.put(
                                "tiles",
                                tileManager.toJSON()
                                        .put("runtime-tiles", TileManager.RUNTIME_TILES)
                                        .put("settings", settings.toJSON()));

                    } else if (action.equals("metaInteraction")) {

                        String editType = getParams.getOrDefault("editType", null);

                        if (editType != null) {
                            switch (editType) {
                                case "restartBar":
                                    Util.restartApplication(isWebserverOpen());
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
                                    break;
                                case "settingsTemplateSizeMedium":
                                    settings.loadTemplate("sizeMedium");
                                    break;
                                case "settingsTemplateSizeNormal":
                                    settings.loadTemplate("sizeNormal");
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
                                    }
                                } else {
                                    if (whatToEdit.equals("createTile")) {
                                        createTile();
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
                                    } else {
                                        newValue = Util.popupTextInput("Edit Setting", "Enter the new value for the setting:", settings.getString(whatToEdit));
                                    }
                                    if (newValue != null && (newValue + "").length() > 0 && !newValue.equals("null")) {
                                        settings.setSetting(whatToEdit, newValue);
                                    }
                                }
                            } else {
                                undoHistory.undo(null);
                            }
                            tileManager.cleanUpTileActions();
                            tileManager.save();
                        }
                    }

                    out.write("HTTP/1.0 200 OK\r\n");
                    out.write("Date: " + new Date() + "\r\n");
                    out.write("Server: Java/1.0\r\n");
                    out.write("Content-Type: application/json\r\n");
                    out.write("\r\n");
                    out.write(response.toString());
                } catch (Exception e) {
                    TrayUtil.showError("Something went wrong: " + e.getMessage());
                    setResponseError(500, "Something went wrong: " + e.getMessage() + ", " + Arrays.toString(e.getStackTrace()), out);
                    e.printStackTrace();
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
            System.out.println("Something went wrong while answering to the client: " + e.getMessage());
            e.printStackTrace();
            openSettingsWebServer(false);
        }
    }

    public void resetSettings() {
        String confirmation = Util.popupTextInput("Reset Settings", "Are you sure you want to reset all settings?\nEnter 'confirm' to delete the settings:", "");
        if (confirmation != null && confirmation.equals("confirm")) {
            settings.reset(true);
            settings.save();
        }
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
            switch (generatorType) {
                case "file":
                    File file = Util.pickDirectory();
                    if (file != null) {
                        String filter = Util.popupTextInput("Generator", "Leave empty or enter file extensions", tileGeneratorGenerator != null ? tileGeneratorGenerator.getParam2() : null);
                        if (filter != null) {
                            tileGeneratorGenerator = new TileGeneratorGenerator(generatorType, file.getAbsolutePath(), filter.length() > 0 ? filter : null);
                        }
                    }
                    break;
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
            tile.removeAction(tileAction);
            param1 = null;
            param2 = null;
        }

        TileAction newTileAction;
        String actionType = tileAction != null ? tileAction.getType() : Util.popupDropDown("Tile Action", "What type of action do you want to create?", TileAction.ACTION_TYPES, null);

        if (actionType != null) {
            String previousValue = tileAction != null && tileAction.getParam1() != null ? tileAction.getParam1() : "";
            switch (actionType) {
                case "file":
                    File file = Util.pickFile(null);
                    if (file != null) {
                        param1 = file.getAbsolutePath();
                    }
                    break;
                case "directory":
                    actionType = "file";
                    file = Util.pickDirectory();
                    if (file != null) {
                        param1 = file.getAbsolutePath();
                    }
                    break;
                case "url":
                    param1 = Util.popupTextInput("Tile Action", "Enter the URL", previousValue);
                    break;
                case "copy":
                    param1 = Util.popupTextInput("Tile Action", "Enter the text to copy", previousValue);
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

    public void createTile() {
        Tile newTile = new Tile();
        TileAction newTileAction = createOrEditNewTileAction(newTile, null, null);
        if (newTileAction != null) {
            String tileName = Util.popupTextInput("Create new Tile", "Enter the name of the new Tile:", newTileAction.getExampleTileLabel());
            if (tileName != null && tileName.length() > 0 && !tileName.equals("null")) {
                newTile.setLabel(tileName);
                newTile.setCategory(newTileAction.getType());
                tileManager.addTile(newTile);
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
            System.out.println("Disabling input for " + duration + " minute(s)");
            timeoutUntil = System.currentTimeMillis() + ((long) duration * 60 * 1000);
            TrayUtil.showMessage("LaunchBar is now disabled for " + duration + " minute" + (duration == 1 ? "" : "s"));
            TrayUtil.setMenuItemActive(0, true);
        }
    }

    private void setResponseError(int errorCode, String message, BufferedWriter out) throws IOException {
        switch (errorCode) {
            case 400:
            default:
                out.write("HTTP/1.0 400 Bad Request\r\n");
                break;
            case 500:
                out.write("HTTP/1.0 500 Internal Server Error\r\n");
                break;
        }

        out.write("Date: " + new Date() + "\r\n");
        out.write("Server: Java/1.0\r\n");
        out.write("Content-Type: application/json\r\n");
        out.write("\r\n{\"error\":\"" + message + "\"}");
    }

    private final static Pattern GET_PATTERN = Pattern.compile("GET /\\?(.+) HTTP/\\d.+");
    private final static Pattern POST_PATTERN = Pattern.compile("POST /\\?(.+) HTTP/\\d.+");
}