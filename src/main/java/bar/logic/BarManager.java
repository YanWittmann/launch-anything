package bar.logic;

import bar.ui.GlassBar;

import java.util.ArrayList;
import java.util.List;

public class BarManager {

    private final GlassBar inputGlassBar;
    private final List<GlassBar> resultGlassBars = new ArrayList<>();
    private final Settings settings;

    public BarManager(Settings settings) {
        this.settings = settings;

        // create the main input bar
        inputGlassBar = new GlassBar();
        inputGlassBar.setType(-1, settings);
        inputGlassBar.setAllowInput(true);

        // create the result bars
        for (int i = 0; i < settings.getInt(Settings.AMOUNT_RESULTS); i++) {
            GlassBar resultGlassBar = new GlassBar();
            resultGlassBar.setType(i, settings);
            resultGlassBar.setAllowInput(false);
            resultGlassBars.add(resultGlassBar);
        }
    }

    public void setInputActive(boolean active) {
        resultGlassBars.forEach(glassBar -> glassBar.setVisible(false));
        if (active) {
            inputGlassBar.setVisible(false);
            resultGlassBars.forEach(GlassBar::prepare);
        }
        inputGlassBar.setVisible(active);
    }

    public void prepareResultBars() {
        resultGlassBars.forEach(glassBar -> glassBar.setVisible(false));
        resultGlassBars.forEach(GlassBar::prepare);
    }

    public void addInputListener(GlassBar.InputListener listener) {
        inputGlassBar.addInputListener(listener);
    }
}
