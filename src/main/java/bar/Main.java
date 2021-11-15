package bar;

import bar.logic.BarManager;
import bar.logic.Settings;
import bar.tile.Tile;
import bar.tile.TileManager;
import bar.util.GlobalKeyListener;
import bar.util.Util;
import bar.webserver.Webserver;
import lc.kra.system.keyboard.event.GlobalKeyEvent;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        Util.registerFont("font/Comfortaa-Regular.ttf");
        new Main();
    }

    private Settings settings;
    private BarManager barManager;
    private TileManager tileManager;

    private Main() {
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
                    if (currentTime - lastCommandInput[0] < settings.getInt(Settings.ACTIVATION_DELAY)) {
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

    private Webserver webserver;

    public void openSettingsWebServer() {
        if (webserver == null) {
            try {
                webserver = new Webserver(36345, "settings");
                webserver.setHandler(this::handleSettingsWebServer);
                webserver.open();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        webserver.openInBrowser();
    }

    private String handleSettingsWebServer(Map<String, String> params) {
        if (!params.getOrDefault("p", "null").equals("null")) {
            return Util.readClassResource("web/settings.html");
        } else if (!params.getOrDefault("t", "null").equals("null")) {
            String type = params.get("t");
            switch (type) {
                case "get_data":
                    System.out.println(tileManager.toJSON().toString());
                    return tileManager.toJSON().toString();
            }
        }
        return new JSONObject().put("error", "no expected value defined").toString();
    }
}
