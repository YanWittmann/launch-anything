package bar.tile.custom;

import bar.tile.Tile;
import bar.tile.TileAction;
import bar.util.Util;
import net.objecthunter.exp4j.tokenizer.UnknownFunctionOrVariableException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MathExpressionTile implements RuntimeTile {

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        try {
            String result = Util.evaluateMathematicalExpression(search) + "";
            if (result.endsWith(".0")) result = result.replace(".0", "");
            Tile tile = new Tile(result);
            tile.setCategory("runtime");
            TileAction action = new TileAction("copy", result);
            tile.addAction(action);
            return Collections.singletonList(tile);
        } catch (UnknownFunctionOrVariableException e) {
            return Collections.emptyList();
        }
    }
}
