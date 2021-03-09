import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class Tile {

    private long lastExecuted = 0;
    private boolean hidden = false;
    private String category;
    private String id;
    private String label;
    private String keywordsString;
    private String[] keywords;
    private String normalizedCategory;
    private String normalizedId;
    private String normalizedLabel;
    private String[] normalizedKeywords;
    private ArrayList<TileAction> actions;

    public Tile(JSONObject json) {
        category = json.getString("category");
        id = json.getString("id");
        label = json.getString("label");
        keywordsString = json.getString("keywords");
        keywords = keywordsString.split(",");
        lastExecuted = Long.parseLong("" + json.getString("lastExecuted"));
        if (json.has("hidden")) hidden = json.getString("hidden").equals("true");

        normalizedCategory = normalize(category);
        normalizedId = normalize(id);
        normalizedLabel = normalize(label);
        normalizedKeywords = normalize(json.getString("keywords")).split(",");

        JSONArray actionsArray = json.getJSONArray("actions");
        actions = new ArrayList<>();
        int bound = actionsArray.length();
        for (int i = 0; i < bound; i++) {
            actions.add(new TileAction(actionsArray.getJSONObject(i)));
        }
    }

    public Tile() {
        category = "";
        id = "";
        label = "";
        keywordsString = "";
        keywords = "".split(",");
        lastExecuted = 0;
        hidden = false;

        normalizedCategory = normalize(category);
        normalizedId = normalize(id);
        normalizedLabel = normalize(label);
        normalizedKeywords = normalize("").split(",");

        JSONArray actionsArray = new JSONArray();
        actions = new ArrayList<>();
        IntStream.range(0, actionsArray.length()).forEach(i -> actions.set(i, new TileAction(actionsArray.getJSONObject(i))));
    }

    public boolean matchesSearch(String search) {
        if (hidden) return false;
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
                if (charArray[i] == search.charAt(amountFound)) amountFound++;
                else mayMatchNonBeginningCharacter = false;
            }
            if (amountFound >= search.length()) return true;
        }
        return false;
    }

    public void execute() {
        Main.setLastExecutedTile(this);
        lastExecuted = Main.getTime();
        actions.forEach(TileAction::execute);
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
        if (hidden) object.put("hidden", true);
        JSONArray actions = new JSONArray();
        for (TileAction action : this.actions) {
            JSONObject generated = action.generateJSON();
            actions.put(generated);
        }
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

    public String getId() {
        return id;
    }

    public String getKeywordsString() {
        return keywordsString;
    }

    public ArrayList<TileAction> getActions() {
        return actions;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setTileDataFromSettings(String id, String label, String category, String[] keywords, boolean hidden, String lastExecuted) {
        StringBuilder key = new StringBuilder();
        for (String keyword : keywords) {
            if(key.length() > 0) key.append(",");
            key.append(keyword);
        }
        setTileDataFromSettings(id, label, category, key.toString(), hidden, lastExecuted);
    }

    public void setTileDataFromSettings(String id, String label, String category, String keywords, boolean hidden, String lastExecuted) {
        this.id = id;
        this.label = label;
        this.category = category;
        this.keywordsString = keywords;
        this.keywords = keywordsString.split(",");
        this.hidden = hidden;
        try {
            this.lastExecuted = Long.parseLong(lastExecuted);
        } catch (Exception ignored) {
        }

        normalizedCategory = normalize(category);
        normalizedId = normalize(id);
        normalizedLabel = normalize(label);
        normalizedKeywords = normalize(keywordsString).split(",");
    }

    public void openActionEditor() {
        System.out.println("Editing actions of " + toString());

        GuiActionEditor settings = new GuiActionEditor(this);
        JFrame frame = new JFrame("LaunchAnything tile action editor: '" + label + "'");

        frame.setContentPane(settings.getMainPanel());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setIconImage(new ImageIcon("res/icon.png").getImage());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.dispose();
            }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        settings.setFrame(frame);

        settings.updateTiles(actions);
    }

    public void setActions(List<TileAction> actions) {
        this.actions = new ArrayList<>(actions);
    }

    public void addAction(TileAction action) {
        this.actions.add(action);
    }

    @Override
    public String toString() {
        return "Tile{" +
                "lastExecuted=" + lastExecuted +
                ", category='" + category + '\'' +
                ", id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", keywords=" + Arrays.toString(keywords) +
                ", actions=" + Arrays.toString(actions.toArray(new TileAction[0])) +
                '}';
    }

    public final static String TEMPLATE_OPEN_FILE = "{\"keywords\":\"KEYWORDS\",\"id\":\"ID\",\"label\":\"LABEL\",\"category\":\"CATEGORY\",\"lastExecuted\":\"LASTEXECUTED\",\"actions\":[{\"action\":{\"path\":\"PATH\"},\"type\":\"openFile\"}]}";

}
