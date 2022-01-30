package bar.tile.action;

import bar.Main;
import bar.tile.TileGeneratorGenerator;
import bar.ui.TrayUtil;
import bar.util.Util;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class TileActionURL extends TileAction {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private String url;

    public TileActionURL() {
    }

    public TileActionURL(JSONObject json) {
        this.url = json.optString("url", null);
        if (this.url == null) this.url = json.optString("param1");
    }

    public TileActionURL(String url) {
        this.url = url;
    }

    @Override
    public void execute(Main main) {
        if (url == null) {
            LOG.warn("url is [null]");
            return;
        }

        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(new URI(url.replace(" ", "%20")));
        } catch (IOException | URISyntaxException e) {
            TrayUtil.showError("Tile action failure: unable to open url: " + e.getMessage());
            LOG.error("error ", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TileActionURL) {
            TileActionURL other = (TileActionURL) o;
            if (o == this) return true;
            return other.url.equals(url) || fuzzyCompare(other.url, url);
        }
        return false;
    }

    @Override
    public boolean equalsByParams(String... params) {
        return params.length == 1 && fuzzyCompare(params[0], url);
    }

    @Override
    protected boolean userModifyActionParameters() {
        String suggested = getClipboardSuggestedParameters();
        if (suggested == null) suggested = url;

        String newUrl = Util.popupTextInput("Tile Action", "Enter the URL", suggested);

        if (newUrl != null && !newUrl.isEmpty()) {
            try {
                new URL(newUrl);
            } catch (Exception e) {
                TrayUtil.showWarning("The URL might be invalid: " + newUrl);
            }
            url = newUrl;
            return true;
        }

        return false;
    }

    @Override
    protected String getClipboardSuggestedParameters() {
        try {
            return new URL(Util.getClipboardText()).toString();
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public String getExampleTileLabel() {
        // check if the website title is available
        String websiteTitle = Util.getWebsiteTitle(url);
        if (websiteTitle != null) return websiteTitle;

        // otherwise, use the url parts to generate a label
        try {
            URL url = new URL(this.url);
            StringBuilder sb = new StringBuilder();
            if (url.getHost() != null) sb.append(url.getHost());
            if (url.getPath() != null) {
                sb.append(" ->");
                sb.append(url.getPath().replace("/", " ").replaceAll("\\?.*", "").replaceAll(" +", " "));
            }
            return sb.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // if all else fails, just use the url
        return "URL " + this.url;
    }

    @Override
    public String getType() {
        return "url";
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getType());
        json.put("url", url);
        return json;
    }

    @Override
    public String[] getParameters() {
        return new String[]{url};
    }
}
