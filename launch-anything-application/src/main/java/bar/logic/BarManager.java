package bar.logic;

import bar.tile.Tile;
import bar.tile.TileCategory;
import bar.ui.GlassBar;

import java.util.ArrayList;
import java.util.List;

public class BarManager {

    private final Settings settings;
    private final GlassBar inputGlassBar;
    private final List<GlassBar> resultGlassBars = new ArrayList<>();
    private boolean isInputActive = false;

    public BarManager(Settings settings) {
        this.settings = settings;

        // create the main input bar
        inputGlassBar = new GlassBar();
        inputGlassBar.setType(-1, settings);
        inputGlassBar.setAllowInput(true);

        // create the result bars
        setAmountResultBars(settings.getInt(Settings.AMOUNT_RESULTS));
    }

    private void setAmountResultBars(int amount) {
        if (resultGlassBars.size() < amount) {
            for (int i = resultGlassBars.size(); i < amount; i++) {
                GlassBar resultGlassBar = new GlassBar();
                resultGlassBar.setType(i, settings);
                resultGlassBar.setAllowInput(false);
                resultGlassBar.prepareUpdateBackground();
                resultGlassBars.add(resultGlassBar);
            }
        } else if (resultGlassBars.size() > amount) {
            for (int i = resultGlassBars.size() - 1; i >= amount && i > 0; i--) {
                resultGlassBars.remove(i);
            }
        }
    }

    public void setInputActive(boolean active) {
        isInputActive = active;
        resultGlassBars.forEach(glassBar -> glassBar.setVisible(false));
        if (active) {
            inputGlassBar.setVisible(false);
            for (GlassBar resultGlassBar : resultGlassBars) {
                resultGlassBar.prepareUpdateBackground();
                resultGlassBar.setOpacity(0.0f);
                resultGlassBar.setOnlyVisibility(true);
            }
        }
        inputGlassBar.setVisible(active);
    }

    public boolean isInputActive() {
        return isInputActive;
    }

    public void addInputListener(GlassBar.InputListener listener) {
        inputGlassBar.addInputListener(listener);
    }

    public void setTiles(List<Tile> tiles, int index, List<TileCategory> categories) {
        try {
            if (!isInputActive) return;
            setAmountResultBars(settings.getInt(Settings.AMOUNT_RESULTS));
            for (int i = 0; i < resultGlassBars.size(); i++) {
                if (tiles.size() > i + index) {
                    resultGlassBars.get(i).setText(tiles.get(i + index).getLabel());
                    String category = tiles.get(i + index).getCategory();
                    if (category != null) {
                        TileCategory cat = findCategory(category, categories);
                        if (cat != null) {
                            resultGlassBars.get(i).tintBackground(cat.getColor());
                        }
                    }
                    resultGlassBars.get(i).setOpacity(1.0f);
                } else {
                    resultGlassBars.get(i).setOpacity(0.0f);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TileCategory findCategory(String label, List<TileCategory> categories) {
        for (TileCategory category : categories) {
            if (category.getLabel().equals(label)) return category;
        }
        return null;
    }

    public void barReloadRequest() {
        inputGlassBar.reloadWithSettings(settings);
        resultGlassBars.forEach(glassBar -> glassBar.reloadWithSettings(settings));
    }

    public void setInput(String input) {
        inputGlassBar.setText(input);
    }
}
