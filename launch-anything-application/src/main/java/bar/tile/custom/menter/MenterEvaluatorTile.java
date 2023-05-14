package bar.tile.custom.menter;

import bar.tile.Tile;
import bar.tile.action.TileAction;
import bar.tile.action.TileActionRuntimeInteraction;
import bar.tile.custom.RuntimeTile;
import de.yanwittmann.menter.exceptions.LexerException;
import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.exceptions.ParsingException;
import de.yanwittmann.menter.interpreter.MenterInterpreter;
import de.yanwittmann.menter.interpreter.structure.EvaluationContext;
import de.yanwittmann.menter.interpreter.structure.value.Value;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MenterEvaluatorTile implements RuntimeTile {

    private final static String DEFAULT_CONTEXT_NAME = "launch-anything";
    private final static Pattern ASSIGNMENT_PATTERN = Pattern.compile("( *[A-z.,-]+ *)=([^;]+)");

    private final MenterInterpreter interpreter = new MenterInterpreter();

    public MenterEvaluatorTile() {
        interpreter.finishLoadingContexts();

        EvaluationContext.registerCustomValueType(MenterPlot.class);
        try {
            Class.forName("bar.tile.custom.menter.MenterPlotGenerator");
        } catch (ClassNotFoundException e) {
            throw new MenterExecutionException("Failed to load MenterPlotGenerator class!", e);
        }
        interpreter.evaluateInContextOf("chartjsplotnative", "native plot(); export [plot] as chartjsplotnative;");
        interpreter.finishLoadingContexts();

        interpreter.getModuleOptions()
                .addAutoImport("math inline")
                .addAutoImport("system inline")
                .addAutoImport("chartjsplot inline")
                .addAutoImport("chartjsplotnative inline");
    }

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        try {
            final Matcher assignmentMatcher = ASSIGNMENT_PATTERN.matcher(search);
            if (assignmentMatcher.matches()) {
                final String variableName = assignmentMatcher.group(1).trim();
                final String variableValue = assignmentMatcher.group(2).trim();

                info(this, "Assigning variable [" + variableName + "] to [" + variableValue + "]");

                final Value result = interpreter.evaluateInContextOf(DEFAULT_CONTEXT_NAME, variableValue);

                final Tile tile = new Tile(variableName + " = " + result.toString(), "runtime", "", false);
                tile.addAction(new TileActionRuntimeInteraction(() -> {
                    interpreter.evaluateInContextOf(DEFAULT_CONTEXT_NAME, search);
                    lastInputEvaluated.set(System.currentTimeMillis());
                }));
                tile.addAction(TileAction.getInstance("copy", result.toDisplayString()));
                return Collections.singletonList(tile);
            }

            final Value result = interpreter.evaluateInContextOf(DEFAULT_CONTEXT_NAME, search);
            final Tile tile = new Tile(result.toString(), "runtime", "", false);

            if (result.getValue() instanceof MenterPlot) {
                tile.addAction(new TileActionRuntimeInteraction(() -> {
                    ((MenterPlot) result.getValue()).show(Collections.emptyList());
                }));
            } else {
                tile.addAction(TileAction.getInstance("copy", result.toDisplayString()));
            }

            return Collections.singletonList(tile);

        } catch (LexerException | ParsingException | MenterExecutionException e) {
            if (isMostLikelyExpressionInput(search)) {
                return Collections.singletonList(createCopyTextTile(formatErrorMessage(e.getMessage()), e.getMessage()));
            } else {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private boolean isMostLikelyExpressionInput(String input) {
        if (input.startsWith("http") || input.startsWith("C:") || input.startsWith("D:") || input.startsWith("E:") || input.startsWith("F:")) {
            return false;
        }

        if (input.contains("+") || input.contains("-") || input.contains("*") || input.contains("/") || input.contains("%") ||
            input.contains("==") || input.contains("!=") || input.contains("<") || input.contains(">") || input.contains("<=") ||
            input.contains(">=") || input.contains("&&") || input.contains("||") || input.contains("!")) {
            return true;
        }

        return false;
    }

    private String formatErrorMessage(String message) {
        return message.replace("Syntax error starting from: ", "Error: ")
                .replaceAll("\t", " ")
                .replaceAll(" +", " ");
    }

    private Tile createCopyTextTile(String label, String copyText) {
        Tile tile = new Tile(label);
        tile.setCategory("runtime");
        tile.addAction(TileAction.getInstance("copy", copyText));
        return tile;
    }

    @Override
    public String getName() {
        return "Menter Evaluator";
    }

    @Override
    public String getDescription() {
        return "https://github.com/YanWittmann/menter-lang";
    }

    @Override
    public String getAuthor() {
        return "Yan Wittmann";
    }

    @Override
    public String getVersion() {
        return MenterInterpreter.VERSION;
    }
}
