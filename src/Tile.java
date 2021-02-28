import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.stream.IntStream;

public class Tile {

    private long lastExecuted = 0;
    private String category;
    private String id;
    private String label;
    private String keywordsString;
    private String[] keywords;
    private String normalizedCategory;
    private String normalizedId;
    private String normalizedLabel;
    private String[] normalizedKeywords;
    private TileAction[] actions;

    public Tile(JSONObject json) {
        category = json.getString("category");
        id = json.getString("id");
        label = json.getString("label");
        keywordsString = json.getString("keywords");
        keywords = keywordsString.split(",");
        lastExecuted = Long.parseLong("" + json.getString("lastExecuted"));

        normalizedCategory = normalize(category);
        normalizedId = normalize(id);
        normalizedLabel = normalize(label);
        normalizedKeywords = normalize(json.getString("keywords")).split(",");

        JSONArray actionsArray = json.getJSONArray("actions");
        actions = new TileAction[actionsArray.length()];
        IntStream.range(0, actionsArray.length()).forEach(i -> actions[i] = new TileAction(actionsArray.getJSONObject(i)));
    }

    public boolean matchesSearch(String search) {
        //look for entire words
        String[] splitted = search.split(" ");
        int amountSearchFound = 0;
        for (String s : splitted) {
            boolean matches = false;
            if (normalizedCategory.contains(s)) matches = true;
            if (normalizedId.contains(s)) matches = true;
            if (normalizedLabel.contains(s)) matches = true;
            for (String keyword : normalizedKeywords)
                if (keyword.contains(s)) {
                    matches = true;
                    break;
                }
            if (matches) amountSearchFound++;
        }
        if (amountSearchFound >= splitted.length)
            return true;

        //look for smart search
        if (smartSearch(normalizedCategory, search)) return true;
        if (smartSearch(normalizedId, search)) return true;
        if (smartSearch(normalizedLabel, search)) return true;
        for (String keyword : normalizedKeywords)
            if (smartSearch(keyword, search)) return true;

        return false;
    }

    public boolean smartSearch(String attribute, String search) {
        int amountFound = 0;
        boolean mayMatchNonBeginningCharacter = false;
        char[] charArray = attribute.toCharArray();
        for (int i = 0, charArrayLength = charArray.length; i < charArrayLength; i++) {
            if (charArray[i] == search.charAt(amountFound))
                if (i == 0 || charArray[i - 1] == ' ') {
                    amountFound++;
                    mayMatchNonBeginningCharacter = true;
                    if (amountFound >= search.length()) return true;
                    continue;
                }

            if (mayMatchNonBeginningCharacter) {
                if(charArray[i] == search.charAt(amountFound)) amountFound++;
                else mayMatchNonBeginningCharacter = false;
            }
            if (amountFound >= search.length()) return true;
        }
        return false;
    }

    public void execute() {
        lastExecuted = Main.getTime();
        Arrays.stream(actions).forEach(TileAction::execute);
    }

    private String normalize(String s) {
        return s.replaceAll("([A-Z])", " $1").toLowerCase();
    }

    public JSONObject generateJSON() {
        JSONObject object = new JSONObject();
        object.put("category", category);
        object.put("id", id);
        object.put("label", label);
        object.put("keywords", keywordsString);
        object.put("lastExecuted", "" + lastExecuted);
        JSONArray actions = new JSONArray();
        Arrays.stream(this.actions).map(TileAction::generateJSON).forEach(actions::put);
        object.put("actions", actions);
        return object;
    }

    public long getLastExecuted() {
        return lastExecuted;
    }

    public String getLabel() {
        return label;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "Tile{" +
                "lastExecuted=" + lastExecuted +
                ", category='" + category + '\'' +
                ", id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", keywords=" + Arrays.toString(keywords) +
                ", actions=" + Arrays.toString(actions) +
                '}';
    }

    public final static String TEMPLATE_OPEN_FILE = "{\"keywords\":\"KEYWORDS\",\"id\":\"ID\",\"label\":\"LABEL\",\"category\":\"CATEGORY\",\"lastExecuted\":\"LASTEXECUTED\",\"actions\":[{\"action\":{\"path\":\"PATH\"},\"type\":\"openFile\"}]}";
}
