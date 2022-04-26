package bar.tile.custom;

import bar.tile.Tile;
import bar.tile.TileManager;
import bar.tile.action.TileAction;
import bar.ui.TrayUtil;
import bar.util.Util;
import bar.util.evaluator.MultiTypeEvaluatorManager;
import de.yanwittmann.j2chartjs.chart.ScatterChart;
import de.yanwittmann.j2chartjs.data.ScatterChartData;
import de.yanwittmann.j2chartjs.datapoint.ScatterChartDatapoint;
import de.yanwittmann.j2chartjs.dataset.ScatterChartDataset;
import de.yanwittmann.j2chartjs.options.ChartOptions;
import de.yanwittmann.j2chartjs.options.interaction.InteractionOption;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ChartGeneratorTile implements RuntimeTile {

    private static final Logger LOG = LoggerFactory.getLogger(ChartGeneratorTile.class);

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        try {
            List<Tile> tiles = new ArrayList<>();
            if (search.isEmpty()) {
                return tiles;
            } else {
                String expression = search;
                BigDecimal start = new BigDecimal("-10");
                BigDecimal end = new BigDecimal("10");
                BigDecimal step;

                if (search.contains("for")) {
                    String[] forSplit = search.split("for", 2);
                    expression = forSplit[0];
                    String[] rangeSplit = forSplit[1].split(",");
                    if (rangeSplit[0].length() > 0) start = new BigDecimal(rangeSplit[0]);
                    if (rangeSplit.length > 1 && rangeSplit[1].length() > 0) end = new BigDecimal(rangeSplit[1]);
                    step = rangeSplit.length > 2 && rangeSplit[2].length() > 0 ? new BigDecimal(rangeSplit[2]) : getStepSize(start, end);
                } else {
                    step = getStepSize(start, end);
                }

                List<String> expressions = new ArrayList<>();
                Arrays.stream(expression.split(";")).forEach(e -> expressions.add(e.trim()));

                if (expressions.size() > 0 && expressions.stream().allMatch(MultiTypeEvaluatorTile::checkForValidFunction)) {
                    Tile tile = new Tile("Graph for " + String.join(", ", expressions));
                    tile.setCategory("runtime");
                    BigDecimal finalStart = start;
                    BigDecimal finalEnd = end;
                    TileAction action = TileAction.getInstance(() -> generateGraph(finalStart, finalEnd, step, expressions));
                    tile.addAction(action);
                    tiles.add(tile);
                    return tiles;
                }
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }

    private final static BigDecimal[] VALID_STEP_SIZES = {BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.2),
            BigDecimal.valueOf(0.25), BigDecimal.valueOf(0.5), BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0),
            BigDecimal.valueOf(5.0), BigDecimal.valueOf(10.0), BigDecimal.valueOf(20.0), BigDecimal.valueOf(50.0),
            BigDecimal.valueOf(100.0), BigDecimal.valueOf(200.0), BigDecimal.valueOf(500.0), BigDecimal.valueOf(1000.0)};

    private BigDecimal getStepSize(BigDecimal start, BigDecimal end) {
        BigDecimal step = (end.subtract(start)).divide(new BigDecimal(200), RoundingMode.HALF_UP);
        if (step.compareTo(new BigDecimal("0.1")) < 0) step = new BigDecimal("0.1");
        for (BigDecimal validStepSize : VALID_STEP_SIZES) {
            if (step.compareTo(validStepSize) <= 0) {
                return validStepSize;
            }
        }
        return step;
    }

    private void generateGraph(BigDecimal start, BigDecimal end, BigDecimal step, List<String> expressions) {
        LOG.info("Generating graph for [{}] in range [{} {} {}]", String.join(", ", expressions), start, end, step);

        ScatterChartData data = new ScatterChartData();
        for (String expression : expressions) {
            if (expression == null) continue;

            ScatterChartDataset dataset = new ScatterChartDataset();
            boolean hadEnd = false;
            if (!roundToDisplay(start, start, step).equals(start))
                calculateDatapoint(expression, dataset, start);
            for (BigDecimal x = new BigDecimal(start.toString()); x.compareTo(end.add(step)) <= 0; x = x.add(step)) {
                BigDecimal displayX = roundToDisplay(x, start, step);
                if (isBeyondEnd(displayX, end)) {
                    break;
                }
                if (!hadEnd && isEnd(displayX, end)) {
                    hadEnd = true;
                }
                calculateDatapoint(expression, dataset, displayX);
            }
            if (!hadEnd) calculateDatapoint(expression, dataset, end);
            dataset.setShowLine(true);
            String functionSignature = TileManager.getMultiTypeEvaluator().getFunctionSignatureForFunctionName(expression.replaceAll("\\(.*\\)", ""));
            if (functionSignature == null) {
                dataset.setLabel(expression);
            } else {
                dataset.setLabel(expression + " in " + functionSignature);
            }
            data.addDataset(dataset);
        }
        data.applyDefaultStylePerDataset();

        ChartOptions options = new ChartOptions();
        options.setInteraction(new InteractionOption().setMode("x").setIntersect(false));

        ScatterChart chart = new ScatterChart();
        chart.setChartData(data);
        chart.setChartOptions(options);

        List<String> htmlLines = new ArrayList<>();
        htmlLines.add("<html>");
        htmlLines.add("<head>");
        htmlLines.add("<title>LaunchAnything Graph</title>");
        htmlLines.add("<script src=\"https://cdn.jsdelivr.net/npm/chart.js@3.5.1/dist/chart.min.js\"></script>");
        htmlLines.add("</head>");
        htmlLines.add("<body>");
        htmlLines.add("<canvas id=\"canvasId\" style=\"border: gray 2px solid;\">");
        htmlLines.add("</canvas>");
        htmlLines.add("<script>");
        htmlLines.add("var ctx = document.getElementById(\"canvasId\");");
        htmlLines.add("new Chart(ctx, " + chart.build() + ");");
        htmlLines.add("</script>");
        htmlLines.add("</body>");
        htmlLines.add("</html>");

        try {
            File tempFile = Util.getTempFile(".html");
            FileUtils.write(tempFile, String.join("\n", htmlLines), StandardCharsets.UTF_8);
            Desktop.getDesktop().browse(tempFile.toURI());
        } catch (Exception e) {
            LOG.error("error ", e);
            TrayUtil.showError("Something went wrong while generating the graph: " + e.getMessage());
        }
    }

    private void calculateDatapoint(String expression, ScatterChartDataset dataset, BigDecimal x) {
        MultiTypeEvaluatorManager.EvaluationResult y = TileManager.getMultiTypeEvaluator().solveForX(expression, x);
        if (y.getClass() == MultiTypeEvaluatorManager.EvaluationResultResult.class) {
            dataset.addData(new ScatterChartDatapoint(x, toValue(((MultiTypeEvaluatorManager.EvaluationResultResult) y).getResult())));
        }
    }

    private BigDecimal toValue(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof BigInteger) {
            return new BigDecimal((BigInteger) value);
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    private boolean isStart(BigDecimal value, BigDecimal start) {
        return value.subtract(start).abs().compareTo(new BigDecimal("0.01")) < 0;
    }

    private boolean isEnd(BigDecimal value, BigDecimal end) {
        return value.subtract(end).abs().compareTo(new BigDecimal("0.01")) < 0;
    }

    private boolean isBeyondEnd(BigDecimal value, BigDecimal end) {
        return value.compareTo(end) > 0;
    }

    private BigDecimal roundToDisplay(BigDecimal value, BigDecimal startValue, BigDecimal stepSize) {
        return value.divide(stepSize, 0, RoundingMode.HALF_UP).multiply(stepSize).add(startValue.remainder(stepSize));
    }

    @Override
    public String getName() {
        return "Chart Generator";
    }

    @Override
    public String getDescription() {
        return "Enter an expression and a graph will be generated";
    }

    @Override
    public String getAuthor() {
        return "Yan Wittmann";
    }

    @Override
    public String getVersion() {
        return null;
    }
}
