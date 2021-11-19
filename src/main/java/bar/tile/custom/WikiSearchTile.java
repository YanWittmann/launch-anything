package bar.tile.custom;

import bar.tile.Tile;
import bar.tile.TileAction;
import bar.util.Sleep;
import bar.util.Util;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class WikiSearchTile implements RuntimeTile {

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        if (search.startsWith("wiki") && search.length() > 4) {
            long before = lastInputEvaluated.get();
            Sleep.milliseconds(800);
            if (lastInputEvaluated.get() != null && lastInputEvaluated.get() != before) {
                return Collections.emptyList();
            }
            try {
                JSONObject response = new JSONObject(Util.getHttpRequestResult("https://en.wikipedia.org/api/rest_v1/page/summary/" + search.replaceAll("wiki *", "")));
                Tile tile = new Tile(response.optString("description"));
                tile.setCategory("custom");
                JSONObject contentUrls = response.optJSONObject("content_urls");
                if (contentUrls != null) {
                    JSONObject desktop = contentUrls.optJSONObject("desktop");
                    if (desktop != null) {
                        TileAction action = new TileAction("url", desktop.optString("page"));
                        tile.addAction(action);
                    }
                }
                return Collections.singletonList(tile);
            } catch (Exception ignored) {
            }
        }
        return Collections.emptyList();
    }
}
