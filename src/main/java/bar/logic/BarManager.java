package bar.logic;

import bar.ui.GlassBar;
import bar.util.GlobalKeyListener;
import lc.kra.system.keyboard.event.GlobalKeyEvent;

import java.util.ArrayList;
import java.util.List;

public class BarManager {

    private final GlassBar inputGlassBar;
    private final List<GlassBar> resultGlassBars = new ArrayList<>();
    private final GlobalKeyListener globalKeyListener;
    private Settings settings;

    public BarManager(Settings settings) {
        this.settings = settings;

        // create the main input bar
        inputGlassBar = new GlassBar();
        inputGlassBar.setType(-1, settings);
        inputGlassBar.setVisible(true);
        inputGlassBar.setAllowInput(true);
        inputGlassBar.addInputListener(System.out::println);

        // create the result bars
        for (int i = 0; i < settings.getInt(Settings.AMOUNT_RESULTS); i++) {
            GlassBar resultGlassBar = new GlassBar();
            resultGlassBar.setType(i, settings);
            resultGlassBar.setAllowInput(false);
            resultGlassBar.setText("Result bar " + i);
            resultGlassBar.setVisible(true);
            resultGlassBars.add(resultGlassBar);
        }

        // initialize the global key listener
        globalKeyListener = new GlobalKeyListener();
        globalKeyListener.addListener(new GlobalKeyListener.KeyListener() {
            @Override
            public void keyPressed(GlobalKeyEvent e) {
                System.out.println(e.getVirtualKeyCode());
            }

            @Override
            public void keyReleased(GlobalKeyEvent e) {

            }
        });
        globalKeyListener.activate();
    }
}
