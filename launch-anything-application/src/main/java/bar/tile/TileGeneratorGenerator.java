package bar.tile;

import bar.tile.action.TileAction;
import bar.util.Util;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TileGeneratorGenerator {

    private final JSONObject json;
    private static final Logger LOG = LoggerFactory.getLogger(TileGeneratorGenerator.class);


    public TileGeneratorGenerator(JSONObject json) {
        this.json = json;
    }

    public TileGeneratorGenerator(String type, String param1, String param2) {
        this.json = new JSONObject();
        this.json.put("type", type);
        this.json.put("param1", param1);
        this.json.put("param2", param2);
        this.json.put("id", UUID.randomUUID().toString());
    }

    public TileGeneratorGenerator(String type, String param1) {
        this.json = new JSONObject();
        this.json.put("type", type);
        this.json.put("param1", param1);
        this.json.put("param2", (Object) null);
        this.json.put("id", UUID.randomUUID().toString());
    }

    public TileGeneratorGenerator(String type) {
        this.json = new JSONObject();
        this.json.put("type", type);
        this.json.put("param1", (Object) null);
        this.json.put("param2", (Object) null);
        this.json.put("id", UUID.randomUUID().toString());
    }

    public String getType() {
        return json.optString("type", null);
    }

    public String getParam1() {
        return json.optString("param1", null);
    }

    public String getParam2() {
        return json.optString("param2", null);
    }

    public String getId() {
        return json.optString("id", null);
    }

    public void setType(String type) {
        json.put("type", type);
    }

    public void setParam1(String param1) {
        json.put("param1", param1);
    }

    public void setParam2(String param2) {
        json.put("param2", param2);
    }

    public List<Tile> generateTiles() {
        List<Tile> tiles = new ArrayList<>();

        try {
            switch (getType()) {
                case "file":
                    List<File> files;
                    if (getParam2() != null) {
                        files = Util.recursivelyListFiles(new File(getParam1()), getParam2().split(" +"));
                    } else {
                        files = Util.recursivelyListFiles(new File(getParam1()));
                    }
                    for (int i = files.size() - 1; i >= 0; i--) {
                        String name = files.get(i).getName();
                        if (name.contains(".")) {
                            String extension = name.substring(name.lastIndexOf(".") + 1);
                            if (extension.length() > 10) {
                                files.remove(i);
                            }
                        } else {
                            files.remove(i);
                        }
                    }
                    if (files.size() > 0) {
                        for (File file : files) {
                            if (file != null) {
                                Tile tile = new Tile(fileTypeNameGenerator(file));
                                tile.addAction(TileAction.getInstance("file", file.getAbsolutePath()));
                                for (String s : file.getAbsolutePath().replaceAll("[A-Z]:", "").replaceAll("[/\\\\.]+", " ").replaceAll(" +", " ").trim().split(" ")) {
                                    tile.addKeyword(s);
                                }
                                tiles.add(tile);
                            }
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            LOG.error("error ", e);
        }

        return tiles;
    }

    public static String fileTypeNameGenerator(File file) {
        if (file == null) return "";
        if (file.isDirectory() || !file.getName().contains(".")) {
            return file.getName();
        } else {
            String filename = file.getName().replaceAll("(.+)\\.[^.]+", "$1");
            String extension = file.getName().replaceAll(".+\\.([^.]+)", "$1").toUpperCase();
            return filename + " (" + extension + ")";
        }
    }

    public JSONObject toJSON() {
        return json;
    }

    public final static String[] GENERATOR_TYPES = {
            "file"
    };
}
