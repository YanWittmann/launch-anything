package bar.logic;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UndoHistory {

    private List<JSONObject> undoHistory = new ArrayList<>();
    private List<JSONObject> redoHistory = new ArrayList<>();

    public void add(JSONObject json) {
        undoHistory.add(json);
        redoHistory.clear();
    }

    public JSONObject undo(JSONObject currentState) {
        if (undoHistory.size() > 0) {
            if (currentState != null) redoHistory.add(currentState);
            JSONObject json = undoHistory.get(undoHistory.size() - 1);
            undoHistory.remove(undoHistory.size() - 1);
            return json;
        }
        return null;
    }

    public JSONObject redo(JSONObject currentState) {
        if (redoHistory.size() > 0) {
            undoHistory.add(currentState);
            JSONObject json = redoHistory.get(redoHistory.size() - 1);
            redoHistory.remove(redoHistory.size() - 1);
            return json;
        }
        return null;
    }
}
