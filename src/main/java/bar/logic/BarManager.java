package bar.logic;

import bar.tile.Tile;
import bar.ui.GlassBar;

import java.util.ArrayList;
import java.util.List;

public class BarManager {

    private final GlassBar inputGlassBar;
    private final List<GlassBar> resultGlassBars = new ArrayList<>();
    private final Settings settings;
    private boolean isInputActive = false;

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
        isInputActive = active;
        resultGlassBars.forEach(glassBar -> glassBar.setVisible(false));
        if (active) {
            inputGlassBar.setVisible(false);
            resultGlassBars.forEach(GlassBar::prepareUpdateBackground);
        }
        inputGlassBar.setVisible(active);
    }

    public void addInputListener(GlassBar.InputListener listener) {
        inputGlassBar.addInputListener(listener);
    }

    public void setTiles(List<Tile> tiles) {
        if (!isInputActive) return;
        for (int i = 0; i < resultGlassBars.size(); i++) {
            if (tiles.size() > i) {
                resultGlassBars.get(i).setText(tiles.get(i).getLabel());
                resultGlassBars.get(i).setOnlyVisibility(true);
                inputGlassBar.grabFocus();
            } else {
                resultGlassBars.get(i).setOnlyVisibility(false);

            }
        }
    }
}
