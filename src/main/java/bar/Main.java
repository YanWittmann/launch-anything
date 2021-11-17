package bar;

import bar.logic.BarManager;
import bar.logic.Settings;
import bar.tile.Tile;
import bar.tile.TileManager;
import bar.util.GlobalKeyListener;
import bar.util.Sleep;
import bar.util.Util;
import bar.webserver.HTTPServer;
import com.sun.tracing.dtrace.StabilityLevel;
import lc.kra.system.keyboard.event.GlobalKeyEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    private HTTPServer webserver;

    public void openSettingsWebServer() {
        if (webserver == null) {
            new Thread(() -> {
                try {
                    webserver = new HTTPServer(36345);
                    webserver.addListener(this::handleSettingsWebServer);
                    webserver.open();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        Sleep.seconds(2);
        webserver.openInBrowser();
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

        out.write("HTTP/1.0 200 OK\r\n");
        out.write("Date: " + new Date() + "\r\n");
        out.write("Server: Java/1.0\r\n");
        out.write("Content-Type: text/html\r\n");
        //out.write("Content-Type: application/json\r\n");
        out.write("\r\n");
        out.write(Util.readClassResource("web/settings.html"));

        /*
        System.out.println(tileManager.toJSON().toString());
        out.write(tileManager.toJSON().toString());
        }*/
    }
}