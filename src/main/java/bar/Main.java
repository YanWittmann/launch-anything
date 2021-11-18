package bar;

import bar.logic.BarManager;
import bar.logic.Settings;
import bar.tile.Tile;
import bar.tile.TileAction;
import bar.tile.TileCategory;
import bar.tile.TileManager;
import bar.util.GlobalKeyListener;
import bar.util.Sleep;
import bar.util.Util;
import bar.webserver.HTTPServer;
import lc.kra.system.keyboard.event.GlobalKeyEvent;
import org.json.JSONObject;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.awt.Desktop.getDesktop;

public class Main {

    public static void main(String[] args) {
        Util.registerFont("font/Comfortaa-Regular.ttf");
        new Main();
    }

    private Settings settings;
    private BarManager barManager;
    private TileManager tileManager;

    private Main() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ignored) {
        }

        settings = new Settings();
        barManager = new BarManager(settings);
        tileManager = new TileManager(settings);
        barManager.addInputListener(this::userInput);
        tileManager.addOnInputEvaluatedListener(this::onInputEvaluated);

        final long[] lastCommandInput = {System.currentTimeMillis()};
        GlobalKeyListener keyListener = new GlobalKeyListener();
        keyListener.addListener(new GlobalKeyListener.KeyListener() {
            @Override
            public void keyPressed(GlobalKeyEvent e) {
                if (e.getVirtualKeyCode() == settings.getInt(Settings.ACTIVATION_KEY)) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastCommandInput[0] < settings.getInt(Settings.ACTIVATION_DELAY) && currentTime - lastCommandInput[0] > 50) {
                        barManager.setInputActive(true);
                    }
                    lastCommandInput[0] = currentTime;
                } else if (e.getVirtualKeyCode() == settings.getInt(Settings.CANCEL_KEY)) {
                    barManager.setInputActive(false);
                } else if (e.getVirtualKeyCode() == settings.getInt(Settings.CONFIRM_KEY)) {
                    boolean isInputActive = barManager.isInputActive();
                    if (isInputActive) {
                        barManager.setInputActive(false);
                        executeTopmostTile();
                    }
                } else if (e.getVirtualKeyCode() == settings.getInt(Settings.PREVIOUS_RESULT_KEY)) {
                    currentResultIndex = Math.max(0, currentResultIndex - 1);
                    barManager.setTiles(lastTiles, currentResultIndex, tileManager.getCategories());
                } else if (e.getVirtualKeyCode() == settings.getInt(Settings.NEXT_RESULT_KEY)) {
                    currentResultIndex = Math.min(currentResultIndex + 1, lastTiles.size() - 1);
                    barManager.setTiles(lastTiles, currentResultIndex, tileManager.getCategories());
                }
            }

            @Override
            public void keyReleased(GlobalKeyEvent e) {
            }
        });
        keyListener.activate();

        openSettingsWebServer();
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

    private void onInputEvaluated(List<Tile> tiles) {
        lastTiles = tiles;
        currentResultIndex = 0;
        barManager.setTiles(lastTiles, currentResultIndex, tileManager.getCategories());
    }

    private HTTPServer webserver;

    public void openSettingsWebServer() {
        int port = 36345;
        if (webserver == null) {
            new Thread(() -> {
                try {
                    webserver = new HTTPServer(port);
                    webserver.addListener(this::handleSettingsWebServer);
                    webserver.open();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        Sleep.milliseconds(300);
        // FIXME: Reactivate this when the testing is over
        if (true) return;
        try {
            getDesktop().browse(new URI(webserver.getUrl() + "/?p=" + port));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSettingsWebServer(BufferedReader in, BufferedWriter out) throws IOException {
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
                    response.put("tiles", tileManager.toJSON());

                } else if (action.equals("tileInteraction")) {
                    Sleep.milliseconds(200);

                    String editTypeContext = getParams.getOrDefault("editTypeContext", null);
                    String attribute = getParams.getOrDefault("attribute", null);
                    String tileId = getParams.getOrDefault("tileId", null);
                    String tileName = getParams.getOrDefault("tileName", null);
                    String additionalValue = getParams.getOrDefault("additionalValue", null);

                    if (attribute != null && editTypeContext != null) {
                        if (editTypeContext.equals("tile")) {
                            Tile tile = tileManager.findTile(tileId);
                            if (tile != null) {
                                switch (attribute) {
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
                                if (attribute.equals("createTile")) {
                                    tileName = Util.popupTextInput("Create new Tile", "Enter the name of the new Tile", null);
                                    Tile newTile = new Tile(tileName);
                                    if (tileName != null && tileName.length() > 0 && !tileName.equals("null")) {
                                        TileAction newTileAction = createOrEditNewTileAction(newTile, null, null);
                                        if (newTileAction != null) {
                                            newTile.setCategory(newTileAction.getType());
                                        }
                                    }
                                    tileManager.addTile(newTile);
                                }
                            }
                        }
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
                setResponseError(500, "Something went wrong: " + e.getMessage(), out);
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

        TileAction newTileAction = null;

        if (tileAction != null) {
            if (tileAction.getType().equals("file")) {
                File file = Util.pickFile(null);
                if (file != null) {
                    param1 = file.getAbsolutePath();
                }
            } else if (tileAction.getType().equals("url")) {
                param1 = Util.popupTextInput("Edit Tile Action", "Enter the URL", tileAction.getParam1() != null ? tileAction.getParam1() : "");
            }
            if (param1 != null) {
                newTileAction = new TileAction(tileAction.getType(), param1, param2);
                tile.addAction(newTileAction);
                tile.removeAction(tileAction);
            }
        } else {
            String actionType = Util.popupDropDown("Create new Tile Action", "What type of action do you want to create?", TileAction.ACTION_TYPES, null);
            if (actionType != null && actionType.length() > 0 && !actionType.equals("null")) {
                if (actionType.equals("file")) {
                    File file = Util.pickFile(null);
                    if (file != null) {
                        param1 = file.getAbsolutePath();
                    }
                } else if (actionType.equals("url")) {
                    param1 = Util.popupTextInput("Create new Tile Action", "Enter the URL", null);
                }
                if (param1 != null) {
                    newTileAction = new TileAction(actionType, param1, param2);
                    tile.addAction(newTileAction);
                }
            }
        }

        return newTileAction;
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