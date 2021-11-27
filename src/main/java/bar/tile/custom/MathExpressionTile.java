package bar.tile.custom;

import bar.tile.Tile;
import bar.tile.TileAction;
import bar.util.Util;
import net.objecthunter.exp4j.tokenizer.UnknownFunctionOrVariableException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MathExpressionTile implements RuntimeTile {

    private final Map<String, Double> variables = new HashMap<>();

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        try {
            if (search.matches("[^=]+=[^=]+")) {
                String[] split = search.split("=", 2);
                String varName = split[0].trim();
                String value = split[1].trim();
                double result = Double.parseDouble(evaluate(value));

                Tile tile = new Tile(varName + " = " + result);
                tile.setCategory("runtime");
                TileAction action = new TileAction(() -> variables.put(varName, result));
                tile.addAction(action);
                return Collections.singletonList(tile);
            } else {
                String result = evaluate(search);
                Tile tile = new Tile(result);
                tile.setCategory("runtime");
                TileAction action = new TileAction("copy", result);
                tile.addAction(action);
                return Collections.singletonList(tile);
            }
        } catch (UnknownFunctionOrVariableException e) {
            return Collections.emptyList();
        }
    }

    private String evaluate(String expression) {
        String result = Util.evaluateMathematicalExpression(expression.trim(), variables) + "";
        if (result.endsWith(".0")) return result.replace(".0", "");
        return result;
    }

    public static String getTitle() {
        return "Math Expression";
    }

    public static String getDescription() {
        return "Enter any mathematical expression";
    }
}
