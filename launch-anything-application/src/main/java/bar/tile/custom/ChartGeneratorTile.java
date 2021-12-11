package bar.tile.custom;

import bar.tile.Tile;
import bar.tile.TileAction;
import bar.ui.TrayUtil;
import bar.util.Util;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ChartGeneratorTile implements RuntimeTile {

    private static final Logger logger = LoggerFactory.getLogger(ChartGeneratorTile.class);

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        try {
            List<Tile> tiles = new ArrayList<>();
            search = search.replace(" ", "");
            if (search.isEmpty()) {
                return tiles;
            } else {
                if (search.contains("=")) {
                    search = search.split("=", 2)[1];
                    if (search.isEmpty()) {
                        return tiles;
                    }
                }

                String expression = search;
                double start = -10;
                double end = 10;
                double step;

                if (search.contains("for")) {
                    String[] forSplit = search.split("for", 2);
                    expression = forSplit[0];
                    String[] rangeSplit = forSplit[1].split(",");
                    if (rangeSplit[0].length() > 0) start = Double.parseDouble(rangeSplit[0]);
                    if (rangeSplit.length > 1 && rangeSplit[1].length() > 0) end = Double.parseDouble(rangeSplit[1]);
                    step = rangeSplit.length > 2 && rangeSplit[2].length() > 0 ? Double.parseDouble(rangeSplit[2]) : getStepSize(start, end);
                } else {
                    step = getStepSize(start, end);
                }

                List<String> expressions = new ArrayList<>();
                Arrays.stream(expression.split(";")).forEach(e -> expressions.add(e.trim()));

                if (expressions.size() > 0 && expressions.stream().allMatch(MathExpressionTile::checkForValidFunction)) {
                    Tile tile = new Tile("Graph for " + String.join(", ", expressions));
                    tile.setCategory("runtime");
                    double finalStart = start;
                    double finalEnd = end;
                    TileAction action = new TileAction(() -> generateGraph(finalStart, finalEnd, step, expressions));
                    tile.addAction(action);
                    tiles.add(tile);
                    return tiles;
                }
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }

    private final static double[] VALID_STEP_SIZES = {0.1, 0.2, 0.25, 0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 500.0, 1000.0};

    private double getStepSize(double start, double end) {
        double step = (end - start) / 200;
        if (step < 0.1) step = 0.1;
        for (double validStepSize : VALID_STEP_SIZES) {
            if (step <= validStepSize) {
                return validStepSize;
            }
        }
        return step;
    }

    private void generateGraph(double start, double end, double step, List<String> expressions) {
        logger.info("Generating graph for [{}] in range [{} {} {}]", String.join(", ", expressions), start, end, step);

        ScatterChartData data = new ScatterChartData();
        for (String expression : expressions) {
            if (expression == null) continue;

            ScatterChartDataset dataset = new ScatterChartDataset();
            for (double x = start; x <= end; x += step) {
                double displayX = roundToDisplay(x, step);
                double y = MathExpressionTile.solveForValue(expression, displayX);
                if (y != MathExpressionTile.ERROR_VALUE) {
                    dataset.addData(new ScatterChartDatapoint(displayX, y));
                }
            }
            dataset.setShowLine(true);
            String functionExpression = MathExpressionTile.getFunctionExpression(expression);
            if (functionExpression == null) {
                dataset.setLabel(expression);
            } else {
                dataset.setLabel(expression + " = " + functionExpression);
            }
            data.addDataset(dataset);
        }
        data.applyDefaultStylePerDataset();

        ChartOptions options = new ChartOptions();
        options.setInteraction(new InteractionOption().setMode("index").setIntersect(false));

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
            logger.error("error ", e);
            TrayUtil.showError("Something went wrong while generating the graph: " + e.getMessage());
        }
    }

    private double roundToDisplay(double value, double stepSize) {
        // round to the nearest stepSize
        double rounded = Math.round(value / stepSize) * stepSize;
        // if the distance to the next whole number is smaller than 0.01, round to the next whole number
        if (Math.abs(rounded - Math.round(value)) < 0.01) {
            rounded = Math.round(value);
        }
        return rounded;
    }

    public static String getTitle() {
        return "Chart Generator";
    }

    public static String getDescription() {
        return "Enter an expression and a graph will be generated";
    }
}
