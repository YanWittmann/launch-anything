import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class TileGenerator {

    private String type;
    private String category;
    private String param1 = null;
    private String param2 = null;
    private String param3 = null;
    private String param4 = null;

    public TileGenerator(JSONObject json) {
        type = json.getString("type");
        category = json.getString("category");
        if (json.has("param1"))
            param1 = json.getString("param1");
        if (json.has("param2"))
            param2 = json.getString("param2");
        if (json.has("param3"))
            param3 = json.getString("param3");
        if (json.has("param4"))
            param4 = json.getString("param4");
    }

    public TileGenerator(String type, String category, String param1, String param2, String param3, String param4) {
        this.type = type;
        this.category = category;
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
        this.param4 = param4;
    }

    public TileGenerator() {
        this.type = "";
        this.category = "";
        this.param1 = null;
        this.param2 = null;
        this.param3 = null;
        this.param4 = null;
    }

    public JSONObject generateJSON() {
        JSONObject object = new JSONObject();
        object.put("type", type);
        object.put("category", category);
        if (param1 != null)
            object.put("param1", param1);
        if (param2 != null)
            object.put("param2", param2);
        if (param3 != null)
            object.put("param3", param3);
        if (param4 != null)
            object.put("param4", param4);
        return object;
    }

    public ArrayList<Tile> generateTiles() {
        ArrayList<Tile> generated = new ArrayList<>();

        if (type.equals("music")) {
            if (param1 != null) {
                for (File file : FileUtils.listf(param1)) {
                    for (String musicExtension : MUSIC_EXTENSIONS) {
                        if (file.getName().contains("." + musicExtension)) {
                            generated.add(generateMusicTile(file));
                            break;
                        }
                    }
                }

            }
        }

        return generated;
    }

    private Tile generateMusicTile(File file) {
        return generateOpenFileTile(file.getAbsolutePath(), file.getName(), file.getName(), file.getParentFile().getName(), category);

    }

    private Tile generateOpenFileTile(String file, String id, String label, String keywords, String category) {
        String json = Tile.TEMPLATE_OPEN_FILE.replace("ID", id).replace("LABEL", label).replace("PATH", file).replace("KEYWORDS", keywords).replace("CATEGORY", category).replace("LASTEXECUTED", "0");
        return new Tile(new JSONObject(json.replace("\\", "\\\\")));
    }

    public String getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public String getParam1() {
        return param1;
    }

    public String getParam2() {
        return param2;
    }

    public String getParam3() {
        return param3;
    }

    public String getParam4() {
        return param4;
    }

    public void setAllData(String type, String category, String param1, String param2, String param3, String param4) {
        this.type = type;
        this.category = category;
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
        this.param4 = param4;
    }

    private final static String[] MUSIC_EXTENSIONS = new String[]{"mp3", "wav", "m4a", "flac", "wma", "aac"};
}
