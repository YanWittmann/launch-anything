package bar.tile;

import bar.Main;
import bar.tile.action.TileAction;
import bar.util.Util;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Tile {

    private static final Logger LOG = LoggerFactory.getLogger(Tile.class);

    private boolean isActive = true, exportable;
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
                    tileActions.add(TileAction.getInstance(actions.optJSONObject(i)));
                }
            } else {
                String strActions = json.optString("action", null);
                if (strActions == null) strActions = json.optString("actions", null);
                if (strActions != null) {
                    strActions = strActions.replace("\\", "\\\\");
                    JSONArray arrActions = new JSONArray(strActions);
                    for (int i = 0; i < arrActions.length(); i++) {
                        tileActions.add(TileAction.getInstance(arrActions.optJSONObject(i)));
                    }
                }
            }
            lastActivated = json.optLong("lastActivated", 0);
        } catch (Exception e) {
            LOG.error("Error loading tile from JSON", e);
        }
    }

    public Tile(String label, String category, String keywords, boolean exportable) {
        this.id = UUID.randomUUID().toString();
        this.label = label;
        this.category = category;
        this.keywords = keywords;
        this.exportable = exportable;
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

    public Tile() {
        this.id = UUID.randomUUID().toString();
        this.label = "unnamed";
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

    public String getKeywords() {
        return keywords;
    }

    public String getCategory() {
        return category;
    }

    public TileAction getFirstAction() {
        if (tileActions.size() > 0) {
            return tileActions.get(0);
        }
        return null;
    }

    public JSONArray getTileActionsAsJSON() {
        JSONArray actions = new JSONArray();
        for (TileAction action : tileActions) {
            actions.put(action.toJSON());
        }
        return actions;
    }

    public void execute(Main main) {
        LOG.info("Executing tile [{}]", label);
        lastActivated = System.currentTimeMillis();
        for (TileAction action : tileActions) {
            if (action == null) {
                LOG.warn("Action is null in tile [{}]", label);
                continue;
            }
            String[] parameters = action.getParameters();
            if (parameters.length == 0) LOG.info("Executing action [{}]", action.getType());
            else LOG.info("Executing action [{}] with parameters [{}]", action.getType(), parameters);
            action.execute(main);
        }
    }

    public boolean matchesSearch(String search) {
        if (!isActive) return false;

        // check if the search is directly contained in one of the fields
        String[] lowercasedSplitted = normalizeLowercase(search).split(" ");
        int amountSearchFound = 0;
        for (String s : lowercasedSplitted) {
            if (normalizeLowercase(label).contains(s) || normalizeLowercase(category).contains(s) || normalizeLowercase(keywords).contains(s)) {
                amountSearchFound++;
            }
        }
        if (amountSearchFound >= lowercasedSplitted.length)
            return true;

        // use smart search to check if the search is contained in one of the fields
        if (smartSearch(normalize(category), search)) return true;
        if (smartSearch(normalize(label), search)) return true;
        if (keywords != null) {
            for (String keyword : keywords.split(" "))
                if (smartSearch(normalize(keyword), search)) return true;
        }

        return false;
    }

    private boolean smartSearch(String attribute, String search) {
        int amountFound = 0;
        boolean mayMatchNonBeginningCharacter = false;
        char[] charArray = attribute.toCharArray();
        for (int i = 0, charArrayLength = charArray.length; i < charArrayLength; i++) {
            if (charArray[i] == search.charAt(amountFound)) {
                if (i == 0 || charArray[i - 1] == ' ') {
                    amountFound++;
                    mayMatchNonBeginningCharacter = true;
                    if (amountFound >= search.length()) return true;
                    continue;
                }
            } else if (amountFound > 0 && charArray[i] == search.charAt(amountFound - 1)) {
                mayMatchNonBeginningCharacter = true;
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
        return s.replaceAll("([A-Z./-])", " $1").toLowerCase();
    }

    private String normalizeLowercase(String s) {
        if (s == null) return "";
        return s.toLowerCase();
    }

    public void addAction(TileAction action) {
        if (action == null) return;
        tileActions.add(action);
    }

    public void removeAction(TileAction action) {
        if (action == null) return;
        tileActions.remove(action);
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
            if (tileActions.get(i) == null || tileActions.get(i).getParameters().length == 0) {
                tileActions.remove(i);
            }
        }
    }

    public TileAction findTileAction(String... params) {
        for (TileAction action : tileActions) {
            if (action.equalsByParams(params)) {
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
        return s.replace("\\", "/")
                .replaceAll(" +", " ")
                .toLowerCase();
    }

    public void userModifyTile() {
        String selectedOption = Util.popupChooseButton("LaunchAnything", "What do you want to edit?", new String[]{"Name", "Action", "Both", "Cancel"});
        if (selectedOption != null) {
            switch (selectedOption) {
                case "Action":
                    userModifyAction(getFirstAction());
                    break;
                case "Name":
                    userModifyLabel();
                    break;
                case "Both":
                    if (userModifyAction(getFirstAction())) {
                        userModifyLabel();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public boolean userModifyAction(TileAction tileAction) {
        TileAction modifiedAction = tileAction.userModifyAction();
        if (modifiedAction != null) {
            removeAction(tileAction);
            addAction(modifiedAction);
        }
        return modifiedAction != null;
    }

    public boolean userModifyLabel() {
        String exampleName;
        if (getFirstAction() != null) exampleName = getFirstAction().getExampleTileLabel();
        else exampleName = getLabel();
        String newName = Util.popupTextInput("LaunchAnything", "Enter new name:", exampleName);
        if (newName != null && !newName.isEmpty() && !newName.equals("null")) {
            setLabel(newName);
            return true;
        }
        return false;
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
            if (action == null) continue;
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
