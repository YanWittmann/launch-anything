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
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ChartGeneratorTile implements RuntimeTile {

    private String lastExpression = null;

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        search = search.replaceAll("^[^^=]*=", "").trim();
        String range = search.replaceAll(".*for ?", "");
        search = search.replaceAll("for.*", "");
        if (range.equals(search)) range = "-10,10,1";
        else range = range.replace(" ", "");
        if (solveForValue(search, 1) != ERROR_VALUE || solveForValue(search, 2) != ERROR_VALUE || solveForValue(search, 100) != ERROR_VALUE) {
            String finalSearch = search;
            String finalRange = range;

            List<Tile> tiles = new ArrayList<>();
            Tile tile = new Tile("Graph for " + search);
            tile.setCategory("runtime");
            TileAction action = new TileAction(() -> generateGraph(finalRange, finalSearch));
            tile.addAction(action);
            tiles.add(tile);
            if (lastExpression != null && !lastExpression.equals(finalSearch)) {
                Tile multiExpressionTile = new Tile("Graph for " + search + " and " + lastExpression);
                multiExpressionTile.setCategory("runtime");
                TileAction multiExpressionTileAction = new TileAction(() -> generateGraph(finalRange, lastExpression, finalSearch));
                multiExpressionTile.addAction(multiExpressionTileAction);
                tiles.add(multiExpressionTile);
            }
            return tiles;
        }
        return Collections.emptyList();
    }

    private void generateGraph(String range, String... expressions) {
        System.out.println("Generating graph for " + Arrays.toString(expressions) + " in range " + range);
        lastExpression = expressions[0];

        ScatterChartData data = new ScatterChartData();
        for (int i = 0; i < expressions.length; i++) {
            String expression = expressions[i];
            if (expression == null) continue;
            String[] rangeElements = range.split(",");
            double xMin, xMax, xStep;
            if (rangeElements.length > 0) xMin = Double.parseDouble(rangeElements[0]);
            else xMin = -10;
            if (rangeElements.length > 1) xMax = Double.parseDouble(rangeElements[1]);
            else xMax = 10;
            if (rangeElements.length > 2) xStep = Double.parseDouble(rangeElements[2]);
            else xStep = 1;

            ScatterChartDataset dataset = new ScatterChartDataset();
            for (double x = xMin; x <= xMax; x += xStep) {
                double y = solveForValue(expression, x);
                if (y != ERROR_VALUE) {
                    dataset.addData(new ScatterChartDatapoint(x, y));
                }
            }
            dataset.setShowLine(true);
            dataset.setLabel(new String[]{"f", "g", "h", "i", "j"}[i] + "(x) = " + expression);
            data.addDataset(dataset);
        }
        data.applyDefaultStylePerDataset();

        ChartOptions options = new ChartOptions();

        ScatterChart chart = new ScatterChart();
        chart.setChartData(data);
        chart.setChartOptions(options);

        List<String> htmlLines = new ArrayList<>();
        htmlLines.add("<html>");
        htmlLines.add("<head>");
        htmlLines.add("<script src=\"https://cdn.jsdelivr.net/npm/chart.js@3.5.1/dist/chart.min.js\"></script>");
        htmlLines.add("</canvas>");
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
            e.printStackTrace();
            TrayUtil.showError("Something went wrong while generating the graph: " + e.getMessage());
        }
    }

    private double solveForValue(String expression, double x) {
        try {
            MathExpressionTile.setVariable("x", x);
            return MathExpressionTile.evaluate(expression);
        } catch (Exception e) {
            return ERROR_VALUE;
        }
    }

    private final double ERROR_VALUE = -999999;

    public static String getTitle() {
        return "Chart Generator";
    }

    public static String getDescription() {
        return "Enter an expression and a graph will be generated";
    }
}
