package bar.tile.action;

import bar.Main;
import bar.ui.TrayUtil;
import bar.util.Util;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class TileActionRuntimeInteraction extends TileAction {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private RuntimeTileInteraction interaction;

    public TileActionRuntimeInteraction(RuntimeTileInteraction interaction) {
        this.interaction = interaction;
    }

    @Override
    public void execute(Main main) {
        if (interaction == null) {
            LOG.warn("interaction is [null]");
            return;
        }

        interaction.run();
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public boolean equalsByParams(String... params) {
        return false;
    }

    @Override
    protected boolean userModifyActionParameters() {
        return false;
    }

    @Override
    protected String getClipboardSuggestedParameters() {
        return null;
    }

    @Override
    public String getExampleTileLabel() {
        return "Tile Interaction";
    }

    @Override
    public String getType() {
        return "interaction";
    }

    @Override
    public JSONObject toJSON() {
        return new JSONObject();
    }

    @Override
    public String[] getParameters() {
        return new String[]{};
    }
}
