package bar;

import bar.logic.BarManager;
import bar.logic.Settings;
import bar.tile.Tile;
import bar.tile.TileManager;
import bar.util.GlobalKeyListener;
import bar.util.Util;
import lc.kra.system.keyboard.event.GlobalKeyEvent;

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
                System.out.println(e.getVirtualKeyCode());
                if (e.getVirtualKeyCode() == settings.getInt(Settings.ACTIVATION_KEY)) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastCommandInput[0] < settings.getInt(Settings.ACTIVATION_DELAY)) {
                        barManager.setInputActive(true);
                    }
                    lastCommandInput[0] = currentTime;
                } else if (e.getVirtualKeyCode() == settings.getInt(Settings.CANCEL_KEY)) {
                    barManager.setInputActive(false);
                } else if (e.getVirtualKeyCode() == settings.getInt(Settings.CONFIRM_KEY)) {
                    barManager.setInputActive(false);
                } else if (e.getVirtualKeyCode() == 53) {
                    barManager.prepareResultBars();
                }
            }

            @Override
            public void keyReleased(GlobalKeyEvent e) {
            }
        });
        keyListener.activate();
    }

    private void userInput(String input) {
        tileManager.evaluateUserInput(input);
    }

    private void onInputEvaluated(List<Tile> tiles) {
        System.out.println(tiles);
    }
}
