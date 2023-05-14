package bar.tile.custom.menter;

import bar.ui.TrayUtil;
import bar.util.Util;
import de.yanwittmann.j2chartjs.chart.ScatterChart;
import de.yanwittmann.j2chartjs.data.ScatterChartData;
import de.yanwittmann.j2chartjs.datapoint.ScatterChartDatapoint;
import de.yanwittmann.j2chartjs.dataset.ScatterChartDataset;
import de.yanwittmann.j2chartjs.options.ChartOptions;
import de.yanwittmann.j2chartjs.options.interaction.InteractionOption;
import de.yanwittmann.menter.interpreter.structure.value.CustomType;
import de.yanwittmann.menter.interpreter.structure.value.TypeFunction;
import de.yanwittmann.menter.interpreter.structure.value.TypeMetaData;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@TypeMetaData(typeName = "chartjsplot", moduleName = "chartjsplot")
public class MenterPlot extends CustomType {

    private static final Logger LOG = LoggerFactory.getLogger(MenterPlot.class);

    private final Value x;
    private final Value y;
    private final Value expressions;

    public MenterPlot(List<Value> parameters) {
        super(parameters);
        x = parameters.get(0);
        y = parameters.get(1);
        expressions = parameters.get(2);

        if (!x.isMapAnArray()) {
            throw new IllegalArgumentException("x must be an array!");
        } else if (!y.isMapAnArray()) {
            throw new IllegalArgumentException("y must be an array!");
        } else if (y.getMap().size() != expressions.getMap().size()) {
            throw new IllegalArgumentException(y.getMap().size() + " != " + expressions.getMap().size() + ": y and expressions must have the same size!");
        }
    }

    @TypeFunction
    public Value show(List<Value> parameters) {
        try {
            final ScatterChartData data = new ScatterChartData();

            final BigDecimal[] xValues = x.getMap().values().stream()
                    .map(v -> (BigDecimal) v.getValue())
                    .toArray(BigDecimal[]::new);

            final BigDecimal[][] yValues = new BigDecimal[y.size()][];
            final LinkedHashMap<Object, Value> yMap = y.getMap();
            for (int i = 0; i < yMap.size(); i++) {
                final Value yValue = yMap.get(new BigDecimal(i));
                yValues[i] = yValue.getMap().values().stream()
                        .map(v -> (BigDecimal) v.getValue())
                        .toArray(BigDecimal[]::new);
            }

            final String[] labels = expressions.getMap().values().stream()
                    .map(x -> x.toDisplayString())
                    .toArray(String[]::new);

            for (int i = 0; i < yValues.length; i++) {
                final ScatterChartDataset dataset = new ScatterChartDataset();
                dataset.setShowLine(true);
                data.addDataset(dataset);
                dataset.setLabel(labels[i]);

                for (int j = 0; j < xValues.length; j++) {
                    final BigDecimal currentY = yValues[i][j];
                    if (currentY == null) continue;

                    final BigDecimal currentX = xValues[j];
                    dataset.addData(new ScatterChartDatapoint(currentX, currentY));
                }
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
        } catch (Exception e) {
            LOG.error("error ", e);
            TrayUtil.showError("Something went wrong while generating the graph: " + e.getMessage());
        }

        return Value.empty();
    }

    @Override
    public String toString() {
        return "<confirm to show plot>";
    }
}
