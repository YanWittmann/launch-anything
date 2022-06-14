package bar.tile.custom;

import bar.Main;
import bar.common.Sleep;
import bar.tile.Tile;
import bar.tile.action.TileAction;
import bar.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class WolframAlphaTile implements RuntimeTile {

    private static final Logger LOG = LoggerFactory.getLogger(WolframAlphaTile.class);

    public final static String WOLFRAM_APP_ID_SETTINGS_KEY = "Wolfram App Key";
    public final static String WOLFRAM_TIMEOUT_SETTINGS_KEY = "Wolfram Timeout";

    public WolframAlphaTile() {
        Main.registerSetting(WOLFRAM_APP_ID_SETTINGS_KEY, "NONE");
        Main.registerSetting(WOLFRAM_TIMEOUT_SETTINGS_KEY, "10");
    }

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        if (search.startsWith("wolfram")) {
            search = search.substring("wolfram".length());
        } else {
            return Collections.emptyList();
        }

        if (search.length() <= 3) {
            return Collections.emptyList();
        }

        if (search.endsWith("alpha")) {
            search = search.substring(0, search.length() - "alpha".length()).trim();
        } else {
            return Collections.singletonList(new Tile("Enter 'alpha' to confirm, then wait"));
        }

        if (search.length() <= 3) {
            return Collections.emptyList();
        }

        long before = lastInputEvaluated.get();
        Sleep.milliseconds(800);
        if (lastInputEvaluated.get() != null && lastInputEvaluated.get() != before) {
            return Collections.emptyList();
        }

        String appKey = Main.getSettingString(WOLFRAM_APP_ID_SETTINGS_KEY);
        if (appKey.equals("NONE")) {
            return Collections.emptyList();
        }

        int requestTimeout = Main.getSettingInt(WOLFRAM_TIMEOUT_SETTINGS_KEY);
        String urlEncodedQuery = Util.urlEncode(search);

        String requestUrl = String.format("https://api.wolframalpha.com/v1/result?i=%s&timeout=%d&appid=%s", urlEncodedQuery, requestTimeout, appKey);

        final String response;
        try {
            LOG.info("Performing Wolfram API request for [{}]", search);
            response = Util.getHttpRequestResult(requestUrl);
        } catch (IOException e) {
            LOG.error("Error performing Wolfram API request for [{}]", search, e);
            return Collections.singletonList(new Tile(e.getMessage().substring(0, e.getMessage().indexOf("appid="))));
        }

        Tile wolframResponseTile = createCopyTextTile(response, response);

        return Collections.singletonList(wolframResponseTile);
    }

    private static Tile createCopyTextTile(String label, String copyText) {
        Tile tile = new Tile(label);
        tile.setCategory("runtime");
        tile.addAction(TileAction.getInstance("copy", copyText));
        return tile;
    }

    @Override
    public String getName() {
        return "Wolfram Alpha";
    }

    @Override
    public String getDescription() {
        return "Enter 'wolfram QUERY' to search Wolfram Alpha. Requires an API key to be configured.";
    }

    @Override
    public String getAuthor() {
        return "Yan Wittmann";
    }

    @Override
    public String getVersion() {
        return null;
    }
}
