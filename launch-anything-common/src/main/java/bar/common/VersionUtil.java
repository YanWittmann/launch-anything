package bar.common;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

public class VersionUtil {

    public static String getLatestVersionJson() throws IOException {
        String url = "https://api.github.com/repos/Skyball2000/launch-anything/releases/latest";
        return new Scanner(new URL(url).openStream(), "UTF-8").useDelimiter("\\A").next();
    }

    public static String extractVersion(JSONObject json) {
        return json.optString("tag_name");
    }

    public static String extractVersionTitle(JSONObject json) {
        return json.optString("name");
    }

    public static String extractReleaseDate(JSONObject json) {
        return json.optString("published_at");
    }

    public static String extractBody(JSONObject json) {
        return json.optString("body");
    }

    public static String findAsset(JSONObject json, String assetName) {
        JSONArray assets = json.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.optJSONObject(i);
                if (asset != null && asset.optString("name").equals(assetName)) {
                    return asset.optString("browser_download_url");
                }
            }
        }
        return null;
    }
}
