package bar;

import bar.logic.BarManager;
import bar.logic.Settings;
import bar.tile.Tile;
import bar.tile.TileAction;
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
                    barManager.setTiles(lastTiles, currentResultIndex);
                } else if (e.getVirtualKeyCode() == settings.getInt(Settings.NEXT_RESULT_KEY)) {
                    currentResultIndex = Math.min(currentResultIndex + 1, lastTiles.size() - 1);
                    barManager.setTiles(lastTiles, currentResultIndex);
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
    private int currentResultIndex = 0;

    private void executeTopmostTile() {
        lastTiles.get(currentResultIndex).execute(this);
        tileManager.save();
    }

    private void onInputEvaluated(List<Tile> tiles) {
        lastTiles = tiles;
        currentResultIndex = 0;
        barManager.setTiles(lastTiles, currentResultIndex);
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
        System.out.println(request);

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
        System.out.println(getParams);

        if (getParams.containsKey("action")) {
            try {
                JSONObject response = new JSONObject();
                if (getParams.get("action").equals("getAllTiles")) {
                    response.put("tiles", tileManager.toJSON());
                } else if (getParams.get("action").equals("tileInteraction")) {
                    String editTypeContext = getParams.get("editTypeContext");
                    String attribute = getParams.get("attribute");
                    String tileId = getParams.get("tileId");
                    String tileName = getParams.get("tileName");
                    String additionalValue = getParams.get("additionalValue");

                    if (editTypeContext.equals("tile")) {
                        Tile tile = tileManager.findTile(tileId);
                        if (tile != null) {
                            if (attribute.equals("createAction")) {
                                String selectedActionType = Util.popupDropDown("Create new Tile Action", "What type of action do you want to create?", TileAction.ACTION_TYPES, null);
                                if (selectedActionType != null) {
                                    String param1 = null;
                                    if (selectedActionType.equals("file")) {
                                        param1 = Util.pickFile(null).getAbsolutePath();
                                    } else if (selectedActionType.equals("url")) {
                                        param1 = Util.popupTextInput("Create new Tile Action", "Enter the URL", null);
                                    }
                                    if (param1 != null) {
                                        tile.addAction(new TileAction(selectedActionType, param1, null));
                                    }
                                }
                            } else if (attribute.equals("editAction")) {
                                TileAction tileAction = tile.findTileAction(additionalValue, null);
                                if (tileAction != null) {
                                    String param1 = null;
                                    if (tileAction.getType().equals("file")) {
                                        param1 = Util.pickFile(null).getAbsolutePath();
                                    } else if (tileAction.getType().equals("url")) {
                                        param1 = Util.popupTextInput("Edit Tile Action", "Enter the URL", tileAction.getParam1() != null ? tileAction.getParam1() : "");
                                    }
                                    if (param1 != null) {
                                        tileAction.setParam1(param1);
                                    }
                                }
                            }
                        }
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