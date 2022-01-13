package bar.tile;

import bar.logic.Settings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class CloudAccess {

    private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

    private final String cloudTimerUrl;
    private final String username, password;

    public CloudAccess(String cloudTimerUrl, String username, String password, boolean createAccount) throws IOException {
        this.cloudTimerUrl = cloudTimerUrl;
        this.username = username;
        this.password = password;
        if (this.cloudTimerUrl == null || this.cloudTimerUrl.isEmpty() || this.cloudTimerUrl.equals("null")) {
            throw new IOException("Invalid cloud timer url may not be empty or null");
        }
        if (!ping()) throw new IOException("Could not connect to cloud timer");
        if (this.username.equals("") || this.password.equals("") || this.username.equals("null") || this.password.equals("null")) {
            throw new IOException("Connection to API was established, but username or password may not be empty");
        }
        if (createAccount) {
            JSONObject response = createUser();
            if (!isSuccess(response)) {
                throw new IOException("Could not create user: " + response.optString("message", ""));
            }
        }
        if (!isSuccess(validateLoginData())) {
            throw new IOException("Connection to API was established, but login data is incorrect");
        }
    }

    private URL makeUrl(String url) throws MalformedURLException {
        if (url == null || cloudTimerUrl == null) {
            return null;
        } else if (url.startsWith("/")) {
            url = cloudTimerUrl + url;
        } else {
            url = cloudTimerUrl + "/" + url;
        }
        return new URL(url);
    }

    private JSONObject makeRequestJSONObject(String url, JSONObject post) throws IOException {
        String response = makeRequest(url, post);
        try {
            return new JSONObject(response);
        } catch (Exception e) {
            LOG.error("Error parsing response [{}]", response);
            return null;
        }
    }

    private String makeRequest(String url, JSONObject post) throws IOException {
        if (url == null || cloudTimerUrl == null) return null;
        URL requestUrl = makeUrl(url);
        if (requestUrl == null) return null;

        HttpURLConnection http = (HttpURLConnection) requestUrl.openConnection();
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // convert the JSON object to a & = encoded string
        StringJoiner postData = new StringJoiner("&");
        boolean isSensitiveData = false;
        for (String key : post.keySet()) {
            postData.add(key + "=" + post.get(key));
            if (key.contains("pass")) isSensitiveData = true;
        }

        if (isSensitiveData || postData.length() == 0) LOG.info("Sending POST request to [{}]", requestUrl);
        else LOG.info("Sending POST request to [{}] with data [{}]", requestUrl, postData);

        byte[] out = postData.toString().getBytes(StandardCharsets.UTF_8);
        OutputStream stream = http.getOutputStream();
        stream.write(out);

        String response;
        if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
            LOG.info("Response code [{}] [{}]", http.getResponseCode(), http.getResponseMessage());
            response = new BufferedReader(new InputStreamReader(http.getInputStream())).lines().collect(Collectors.joining());
        } else {
            LOG.warn("Error response code [{}] [{}]", http.getResponseCode(), http.getResponseMessage());
            response = new BufferedReader(new InputStreamReader(http.getErrorStream())).lines().collect(Collectors.joining());
        }
        http.disconnect();

        return response;
    }

    public boolean ping() throws IOException {
        return makeRequest("ping.php", new JSONObject()) != null;
    }

    public static boolean isSuccess(JSONObject response) {
        return response != null && response.optString("code", "").equals("success");
    }

    public JSONObject createUser() throws IOException {
        JSONObject post = new JSONObject();
        post.put("username", username);
        post.put("password", password);
        return makeRequestJSONObject("create_user.php", post);
    }

    public JSONObject removeUser() throws IOException {
        JSONObject post = new JSONObject();
        post.put("username", username);
        post.put("password", password);
        return makeRequestJSONObject("remove_user.php", post);
    }

    public JSONObject modifyUserName(String newUsername) throws IOException {
        JSONObject post = new JSONObject();
        post.put("username", username);
        post.put("password", password);
        post.put("new_username", newUsername);
        return makeRequestJSONObject("modify_user_name.php", post);
    }

    public JSONObject modifyUserPassword(String newPassword) throws IOException {
        JSONObject post = new JSONObject();
        post.put("username", username);
        post.put("password", password);
        post.put("new_password", newPassword);
        return makeRequestJSONObject("modify_user_password.php", post);
    }

    public JSONObject validateLoginData() throws IOException {
        JSONObject post = new JSONObject();
        post.put("username", username);
        post.put("password", password);
        post.put("success_response", true);
        return makeRequestJSONObject("validate_login_data.php", post);
    }

    public JSONObject creteOrModifyTile(Tile tile) throws IOException {
        return creteOrModifyTile(tile.getId(), tile.getLabel(), tile.getCategory(), tile.getTileActionsAsJSON(), tile.getKeywords());
    }

    public JSONObject creteOrModifyTile(String tile_id, String tile_label, String tile_category, JSONArray tile_action, String tile_keywords) throws IOException {
        JSONObject post = new JSONObject();
        post.put("username", username);
        post.put("password", password);
        post.put("tile_id", tile_id);
        post.put("tile_label", tile_label);
        post.put("tile_category", tile_category);
        post.put("tile_action", tile_action);
        post.put("tile_keywords", tile_keywords);
        return makeRequestJSONObject("create_or_modify_tile.php", post);
    }

    public JSONObject getTilesForUser() throws IOException {
        JSONObject post = new JSONObject();
        post.put("username", username);
        post.put("password", password);
        return makeRequestJSONObject("get_tiles_for_user.php", post);
    }

    public JSONObject removeTile(String tile_id) throws IOException {
        JSONObject post = new JSONObject();
        post.put("username", username);
        post.put("password", password);
        post.put("tile_id", tile_id);
        return makeRequestJSONObject("remove_tile.php", post);
    }

    public String getUsername() {
        return username;
    }
}
