package bar.tile;

import bar.Main;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Tile {

    private boolean isActive, exportable;
    private String id, category, label, keywords;
    private long lastActivated = -1;
    private final List<TileAction> tileActions = new ArrayList<>();

    public Tile(JSONObject json) {
        try {
            id = json.optString("id", UUID.randomUUID().toString());
            label = json.optString("label", "Unlabeled tile");
            category = json.optString("category", "unassigned");
            keywords = json.optString("keywords", "");
            isActive = json.optBoolean("isActive", true);
            exportable = json.optBoolean("exportable", true);
            JSONArray actions = json.optJSONArray("actions");
            if (actions != null) {
                for (int i = 0; i < actions.length(); i++) {
                    tileActions.add(new TileAction(actions.optJSONObject(i)));
                }
            }
            lastActivated = json.optLong("lastActivated", 0);
        } catch (Exception ignored) {
        }
    }

    public Tile(String label) {
        this.id = UUID.randomUUID().toString();
        this.label = label;
        this.category = "";
        this.keywords = "";
        this.isActive = true;
        this.exportable = true;
        this.lastActivated = 0;
    }

    public boolean isValid() {
        return lastActivated != -1;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setExportable(boolean exportable) {
        this.exportable = exportable;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public void setLastActivated(long lastActivated) {
        this.lastActivated = lastActivated;
    }

    public boolean isExportable() {
        return exportable;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public long getLastActivated() {
        return lastActivated;
    }

    public String getCategory() {
        return category;
    }

    public void execute(Main main) {
        lastActivated = System.currentTimeMillis();
        for (TileAction action : tileActions) {
            action.execute(main);
        }
    }

    public boolean matchesSearch(String search) {
        if (!isActive) return false;

        // check if the search is directly contained in one of the fields
        String[] splitted = normalizeLowercase(search).split(" ");
        int amountSearchFound = 0;
        for (String s : splitted) {
            if (normalizeLowercase(id).contains(s)) amountSearchFound++;
            else if (normalizeLowercase(category).contains(s)) amountSearchFound++;
            else if (normalizeLowercase(label).contains(s)) amountSearchFound++;
            else if (normalizeLowercase(keywords).contains(s)) amountSearchFound++;
        }
        if (amountSearchFound >= splitted.length)
            return true;

        // check if the normalized search is directly contained in one of the fields
        String[] normalizedSplitted = normalize(search).split(" ");
        amountSearchFound = 0;
        for (String s : splitted) {
            if (normalize(id).contains(s)) amountSearchFound++;
            else if (normalize(category).contains(s)) amountSearchFound++;
            else if (normalize(label).contains(s)) amountSearchFound++;
            else if (normalize(keywords).contains(s)) amountSearchFound++;
        }
        if (amountSearchFound >= splitted.length)
            return true;

        // use smart search to check if the search is contained in one of the fields
        if (smartSearch(normalize(id), search)) return true;
        if (smartSearch(normalize(category), search)) return true;
        if (smartSearch(normalize(label), search)) return true;
        for (String keyword : keywords.split(" ")) if (smartSearch(normalize(keyword), search)) return true;

        return false;
    }

    private boolean smartSearch(String attribute, String search) {
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
                if (charArray[i] == search.charAt(amountFound)) amountFound++;
                else mayMatchNonBeginningCharacter = false;
            }
            if (amountFound >= search.length()) return true;
        }
        return false;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("([A-Z])", " $1").toLowerCase();
    }

    private String normalizeLowercase(String s) {
        if (s == null) return "";
        return s.toLowerCase();
    }

    public void addAction(TileAction action) {
        tileActions.add(action);
    }

    public void removeAction(TileAction tileAction) {
        tileActions.remove(tileAction);
    }

    public void addKeyword(String keyword) {
        keywords = keywords + " " + keyword;
        normalizeKeywords();
    }

    public void removeKeyword(String keyword) {
        keywords = (" " + keywords).replace(" " + keyword, "");
        normalizeKeywords();
    }

    public void editKeyword(String keyword, String newKeyword) {
        keywords = (" " + keywords).replace(" " + keyword, " " + newKeyword);
        normalizeKeywords();
    }

    private void normalizeKeywords() {
        keywords = keywords.replaceAll(" +", " ").trim();
    }

    public void cleanUpTileActions() {
        for (int i = tileActions.size() - 1; i >= 0; i--) {
            boolean p1IsNull = tileActions.get(i).getParam1() == null || tileActions.get(i).getParam1().length() == 0;
            boolean p2IsNull = tileActions.get(i).getParam2() == null || tileActions.get(i).getParam2().length() == 0;
            if (p1IsNull && p2IsNull) {
                tileActions.remove(i);
            }
        }
    }

    public TileAction findTileAction(String param1, String param2) {
        for (TileAction action : tileActions) {
            if (isNormalizedTileActionEquals(action.getParam1(), param1) && isNormalizedTileActionEquals(action.getParam2(), param2)) {
                return action;
            }
        }
        return null;
    }

    private boolean isNormalizedTileActionEquals(String p1, String p2) {
        if (p1 == null && p2 == null) return true;
        if (p1 == null || p2 == null) return false;
        return normalizeTileActionValue(p1).equals(normalizeTileActionValue(p2));
    }

    private String normalizeTileActionValue(String s) {
        return s.replaceAll("\\+", "\\").replace("\\", "/").replaceAll(" +", " ").toLowerCase();
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("category", category);
        json.put("label", label);
        json.put("keywords", keywords);
        json.put("isActive", isActive);
        json.put("lastActivated", lastActivated);
        JSONArray actions = new JSONArray();
        for (TileAction action : tileActions) {
            actions.put(action.toJSON());
        }
        json.put("actions", actions);
        return json;
    }

    @Override
    public String toString() {
        return "Tile{" +
               "isActive=" + isActive +
               ", id='" + id + '\'' +
               ", category='" + category + '\'' +
               ", label='" + label + '\'' +
               ", keywords='" + keywords + '\'' +
               ", lastActivated=" + lastActivated +
               ", tileActions=" + tileActions +
               '}';
    }
}
