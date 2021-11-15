package bar.tile;

import bar.logic.Settings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TileManager {

    private Settings settings;
    private final List<Tile> tiles = new ArrayList<>();
    private final List<TileGenerator> tileGenerators = new ArrayList<>();
    private final List<InputEvaluatedListener> onInputEvaluatedListeners = new ArrayList<>();
    private File tileFile;

    public TileManager(Settings settings) {
        this.settings = settings;
        findSettingsFile();
        if (tileFile == null) tileFile = new File("res/tiles.json");
        else readTilesFromFile();
    }

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentFuture = null;

    public void evaluateUserInput(String input) {
        if (currentFuture != null) currentFuture.cancel(true);
        currentFuture = evaluate(input);
    }

    private void setEvaluationResults(List<Tile> tiles) {
        onInputEvaluatedListeners.forEach(listener -> listener.onInputEvaluated(tiles));
    }

    private Future<?> evaluate(String input) {
        return executor.submit(() -> {
            List<Tile> newTiles = new ArrayList<>();
            setEvaluationResults(newTiles);
            return null;
        });
    }

    private final static String[] possibleTilesFiles = {
            "tiles.json",
            "res/tiles.json",
            "../tiles.json",
            "../res/tiles.json"
    };

    private void findSettingsFile() {
        for (String possibleSettingsFile : possibleTilesFiles) {
            File candidate = new File(possibleSettingsFile).getAbsoluteFile();
            if (candidate.exists()) {
                tileFile = candidate;
                return;
            }
        }
        tileFile = null;
    }

    private void readTilesFromFile() {
        try {
            StringJoiner fileContent = new StringJoiner("\n");
            Scanner reader = new Scanner(tileFile);
            while (reader.hasNextLine()) {
                fileContent.add(reader.nextLine());
            }
            reader.close();

            JSONObject tilesRoot = new JSONObject(fileContent);
            JSONArray tilesArray = tilesRoot.optJSONArray("tiles");
            if (tilesArray != null) {
                for (int i = 0; i < tilesArray.length(); i++) {
                    JSONObject tileJson = tilesArray.optJSONObject(i);
                    if (tileJson == null) continue;
                    Tile tile = new Tile(tileJson);
                    if (tile.isValid()) tiles.add(tile);
                }
            }

            JSONArray tileGeneratorsArray = tilesRoot.optJSONArray("tile-generators");
            if (tileGeneratorsArray != null) {
                for (int i = 0; i < tileGeneratorsArray.length(); i++) {
                    JSONObject tileGeneratorJson = tileGeneratorsArray.optJSONObject(i);
                    if (tileGeneratorJson == null) continue;
                    TileGenerator tileGenerator = new TileGenerator(tileGeneratorJson);
                    if (tileGenerator.isValid()) tileGenerators.add(tileGenerator);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            tileFile.getParentFile().mkdirs();
            FileWriter myWriter = new FileWriter(tileFile);

            JSONArray tilesArray = new JSONArray();
            for (Tile tile : tiles) {
                tilesArray.put(tile.toJSON());
            }

            JSONArray tileGeneratorsArray = new JSONArray();
            for (TileGenerator tileGenerator : tileGenerators) {
                tileGeneratorsArray.put(tileGenerator.toJSON());
            }

            JSONObject tilesRoot = new JSONObject();
            tilesRoot.put("tiles", tilesArray);
            tilesRoot.put("tile-generators", tileGeneratorsArray);

            myWriter.write(tilesRoot.toString());
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addOnInputEvaluatedListener(InputEvaluatedListener listener) {
        onInputEvaluatedListeners.add(listener);
    }

    public interface InputEvaluatedListener {
        void onInputEvaluated(List<Tile> tiles);
    }
}
