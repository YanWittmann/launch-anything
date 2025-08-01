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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
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

            if (result.getValue() instanceof MenterPlot) {
                final Tile tile = new Tile(result.toString(), "runtime", "", false);
                tile.addAction(new TileActionRuntimeInteraction(() -> {
                    ((MenterPlot) result.getValue()).show(Collections.emptyList());
                }));
                return Collections.singletonList(tile);

            } else {
                return createResultTiles(search, result);
            }


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

    private List<Tile> createResultTiles(String query, Value result) {
        final List<Tile> tiles = new ArrayList<>();
        if (result.isEmpty()) return tiles;

        if (result.getValue() instanceof BigDecimal || result.getValue() instanceof BigInteger) {
            final String resultString = result.toString();
            final String copyString = result.toDisplayString();

            tiles.add(createCopyTextTile(resultString, copyString));

            if (((Number) result.getValue()).doubleValue() < Double.MAX_VALUE) {
                double doubleValue = ((Number) result.getValue()).doubleValue();
                String f1 = findFractionValue(doubleValue, 0.001);
                String f1Value = f1.replace(" = ", "").replace(" ≈ ", "");
                if (!f1.isEmpty() && !f1Value.equals(query)) {
                    tiles.add(createCopyTextTile(f1, f1Value));
                }
                String f2 = findFractionValue(doubleValue, 0.01);
                String f2Value = f2.replace(" = ", "").replace(" ≈ ", "");
                if (!f2.isEmpty() && !f1.equals(f2) && !f2Value.equals(query)) {
                    tiles.add(createCopyTextTile(f2, f2Value));
                }
            }

            final String rounded = findRoundedValue((Number) result.getValue());
            if (!rounded.isEmpty())
                tiles.add(createCopyTextTile(rounded, rounded.replace(" ≈ ", "")));

            if (query.matches("[0-9 ]+/[0-9 ]+")) {
                int dividend = Integer.parseInt(query.split("/")[0].trim());
                int divisor = Integer.parseInt(query.split("/")[1].trim());
                if (divisor != 0) {
                    int quotient = dividend / divisor;
                    int remainder = dividend % divisor;
                    if (remainder != 0 && quotient != 0) {
                        tiles.add(createCopyTextTile(" = " + quotient + " R " + remainder, quotient + " R " + remainder));
                    }
                }
            }
        } else if (result.getValue() instanceof Collection) {
            final Collection<?> collection = (Collection<?>) result.getValue();
            final StringJoiner displayJoiner = new StringJoiner(", ", "[", "]");
            final StringJoiner copyJoiner = new StringJoiner(", ", "[", "]");

            for (Object o : collection) {
                if (o instanceof Value) {
                    displayJoiner.add(((Value) o).toString());
                    copyJoiner.add(((Value) o).toDisplayString());
                } else if (o instanceof BigDecimal || o instanceof BigInteger) {
                    displayJoiner.add(formatBigNumberResult(o));
                    copyJoiner.add(formatBigNumberResult(o));
                } else {
                    displayJoiner.add(o.toString());
                    copyJoiner.add(o.toString());
                }
            }

            tiles.add(createCopyTextTile(displayJoiner.toString(), copyJoiner.toString()));
        } else {
            tiles.add(createCopyTextTile(result.toString(), result.toDisplayString()));
        }

        return tiles;
    }

    private String formatBigNumberResult(Object result) {
        String resultString;
        if (result instanceof BigDecimal) {
            resultString = ((BigDecimal) result).toPlainString();
        } else {
            resultString = result.toString();
        }

        resultString = !resultString.contains(".") ? resultString : resultString.replaceAll("0*$", "").replaceAll("\\.$", "");
        return resultString;
    }

    private String findRoundedValue(Number value) {
        BigDecimal v;
        if (value instanceof BigDecimal) {
            v = (BigDecimal) value;
        } else {
            v = new BigDecimal(value.toString());
        }
        BigDecimal rounded = v.setScale(0, RoundingMode.HALF_UP);
        BigDecimal distance = v.subtract(rounded).abs();
        if (distance.compareTo(BigDecimal.valueOf(0.1)) < 0 && distance.compareTo(BigDecimal.ZERO) > 0) {
            return " ≈ " + rounded;
        }
        return "";
    }

    private String findFractionValue(double value, double accuracy) {
        Fraction fraction = Fraction.realToFraction(value, accuracy);
        if (fraction == null) return "";
        if (fraction.numerator == fraction.denominator || fraction.denominator == 1) return "";
        float distance = (float) Math.abs(((float) fraction.numerator / (float) fraction.denominator) - value);
        if (distance < 0.001) {
            return " = " + fraction;
        }
        return " ≈ " + fraction;
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
        if (message.contains("Syntax error starting from")) {
            message = message.replace("Syntax error starting from: ", "Error: ");
        }
        if (message.contains("Cannot resolve symbol")) {
            String[] lines = message.split("\n");
            String symbol = "";
            String suggestions = "";
            for (String line : lines) {
                if (line.startsWith("Cannot resolve symbol")) {
                    int start = line.indexOf('\'');
                    int end = line.indexOf('\'', start + 1);
                    if (start >= 0 && end > start) {
                        symbol = line.substring(start + 1, end);
                    }
                } else if (line.startsWith("Did you mean")) {
                    int start = line.indexOf('\'');
                    int end = line.lastIndexOf('\'');
                    if (start >= 0 && end > start) {
                        suggestions = line.substring(start, end + 1).replace("', '", ", ");
                    }
                }
            }
            if (!symbol.isEmpty()) {
                if (!suggestions.isEmpty()) {
                    message = "Unknown '" + symbol + "' (" + suggestions + ")";
                } else {
                    message = "Unknown '" + symbol + "'";
                }
            }
        }
        message = message.replaceAll("\t", " ").replaceAll(" +", " ");
        return message;
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

    private static class Fraction {
        private final int numerator;
        private final int denominator;

        public Fraction(int numerator, int denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }

        /**
         * <a href="https://stackoverflow.com/a/32903747">https://stackoverflow.com/a/32903747</a>
         *
         * @param value    The value to convert into a fraction.
         * @param accuracy The accuracy of the fraction.
         * @return The fraction representation of the value.
         */
        public static Fraction realToFraction(double value, double accuracy) {
            if (accuracy <= 0.0 || accuracy >= 1.0) {
                throw new IllegalArgumentException("Accuracy must be > 0 and < 1.");
            }

            int sign = value == 0 ? 0 : (value < 0 ? -1 : 1);

            if (sign == -1) {
                value = Math.abs(value);
            }

            // Accuracy is the maximum relative error; convert to absolute maxError
            double maxError = sign == 0 ? accuracy : value * accuracy;

            int n = (int) Math.floor(value);
            value -= n;

            if (value < maxError) {
                return new Fraction(sign * n, 1);
            }

            if (1 - maxError < value) {
                return new Fraction(sign * (n + 1), 1);
            }

            // The lower fraction is 0/1
            int lower_n = 0;
            int lower_d = 1;

            // The upper fraction is 1/1
            int upper_n = 1;
            int upper_d = 1;

            for (int i = 0; i < 100000; i++) {
                // The middle fraction is (lower_n + upper_n) / (lower_d + upper_d)
                int middle_n = lower_n + upper_n;
                int middle_d = lower_d + upper_d;

                if (middle_d * (value + maxError) < middle_n) {
                    // real + error < middle : middle is our new upper
                    upper_n = middle_n;
                    upper_d = middle_d;
                } else if (middle_n < (value - maxError) * middle_d) {
                    // middle < real - error : middle is our new lower
                    lower_n = middle_n;
                    lower_d = middle_d;
                } else {
                    // Middle is our best fraction
                    return new Fraction((n * middle_d + middle_n) * sign, middle_d);
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return numerator + "/" + denominator;
        }
    }
}
