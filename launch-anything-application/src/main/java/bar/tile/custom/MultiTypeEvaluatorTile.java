package bar.tile.custom;

import bar.tile.Tile;
import bar.tile.action.TileAction;
import bar.util.Util;
import bar.util.evaluator.MultiTypeEvaluatorManager;
import com.fathzer.soft.javaluator.Function;
import com.fathzer.soft.javaluator.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class MultiTypeEvaluatorTile implements RuntimeTile {

    private static final Logger LOG = LoggerFactory.getLogger(MultiTypeEvaluatorTile.class);

    private final MultiTypeEvaluatorManager evaluator = new MultiTypeEvaluatorManager();

    public MultiTypeEvaluatorTile() {
        STANDARD_INSTANCE = this;
    }

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        if (search.contains("for")) return Collections.emptyList();
        if (search.equals("function")) {
            List<Tile> tiles = new ArrayList<>();
            for (MultiTypeEvaluatorManager.ExpressionFunction function : evaluator.getCustomFunctions()) {
                tiles.add(createCopyTextTile(function.toString(), function.getName()));
            }
            for (Function function : evaluator.getEvaluator().getFunctions()) {
                tiles.add(createCopyTextTile(function.getName(), function.getName()));
            }
            for (Operator operator : evaluator.getEvaluator().getOperators().stream().sorted(Comparator.comparingInt(Operator::getPrecedence).reversed()).toArray(Operator[]::new)) {
                tiles.add(createCopyTextTile(operator.getSymbol(), operator.getSymbol()));
            }
            return tiles;
        }

        if (search.contains(";")) {
            String[] split = search.split(";");
            if (Arrays.stream(split).allMatch(e -> e.contains("="))) {
                List<MultiTypeEvaluatorManager.EvaluationResult> results = new ArrayList<>();
                for (String expression : split) {
                    MultiTypeEvaluatorManager.EvaluationResult result = attemptEvaluate(expression);
                    if (result instanceof MultiTypeEvaluatorManager.EvaluationResultAssignment || result instanceof MultiTypeEvaluatorManager.EvaluationResultFunction) {
                        results.add(result);
                    } else {
                        return Collections.emptyList();
                    }
                }
                Tile assignmentTile = new Tile(results.size() + " assignment" + (results.size() == 1 ? "" : "s") + " " + results.stream().map(Objects::toString).collect(Collectors.joining(", ")), "runtime", "", false);
                assignmentTile.addAction(TileAction.getInstance(() -> {
                    for (MultiTypeEvaluatorManager.EvaluationResult result : results) {
                        if (result instanceof MultiTypeEvaluatorManager.EvaluationResultAssignment) {
                            ((MultiTypeEvaluatorManager.EvaluationResultAssignment) result).applyVariable();
                        } else if (result instanceof MultiTypeEvaluatorManager.EvaluationResultFunction) {
                            ((MultiTypeEvaluatorManager.EvaluationResultFunction) result).applyFunction();
                        }
                    }
                }));
                return Collections.singletonList(assignmentTile);
            }
        }

        MultiTypeEvaluatorManager.EvaluationResult result = attemptEvaluate(search);
        switch (result.getClass().getSimpleName()) {
            case "EvaluationResultResult":
                Object resultValue = ((MultiTypeEvaluatorManager.EvaluationResultResult) result).getResult();
                return createResultTiles(search, resultValue);
            case "EvaluationResultFunction":
                MultiTypeEvaluatorManager.EvaluationResultFunction functionResult = (MultiTypeEvaluatorManager.EvaluationResultFunction) result;
                Tile functionTile = new Tile(functionResult.toString(), "runtime", "", false);
                functionTile.addAction(TileAction.getInstance(functionResult::applyFunction));
                return Collections.singletonList(functionTile);
            case "EvaluationResultAssignment":
                MultiTypeEvaluatorManager.EvaluationResultAssignment assignmentResult = (MultiTypeEvaluatorManager.EvaluationResultAssignment) result;
                Tile assignmentTile = new Tile(assignmentResult.toString(), "runtime", "", false);
                assignmentTile.addAction(TileAction.getInstance(assignmentResult::applyVariable));
                return Collections.singletonList(assignmentTile);
            case "EvaluationResultFailure":
                if (isLikelyExpression(search)) {
                    Exception failure = ((MultiTypeEvaluatorManager.EvaluationResultFailure) result).getReason();
                    if (failure.getMessage() == null || failure.getMessage().isEmpty() || failure.getMessage().contains(";")) {
                        return Collections.emptyList();
                    }
                    return Collections.singletonList(new Tile(failure.getMessage(), "runtime", "", false));
                }
                break;
        }
        return Collections.emptyList();
    }

    private MultiTypeEvaluatorManager.EvaluationResult attemptEvaluate(String expression) {
        MultiTypeEvaluatorManager.EvaluationResult result = evaluator.evaluate(expression);
        if (result instanceof MultiTypeEvaluatorManager.EvaluationResultFailure) {
            int openBraces = Util.countSubstring(expression, "(");
            int closedBraces = Util.countSubstring(expression, ")");
            int diff = openBraces - closedBraces;
            if (diff > 0) {
                // it might be worth an attempt to fix the closing brackets
                result = evaluator.evaluate(expression + Util.repeat(")", diff));
            }
        }

        return result;
    }

    private boolean isLikelyExpression(String text) {
        return text.contains("+") || text.contains("-") || text.contains("*") || text.contains("/") || text.contains("(");
    }

    private List<Tile> createResultTiles(String query, Object result) {
        List<Tile> tiles = new ArrayList<>();

        if (result instanceof BigDecimal || result instanceof BigInteger) {
            String resultString = result.toString();
            resultString = !resultString.contains(".") ? resultString : resultString.replaceAll("0*$", "").replaceAll("\\.$", "");
            tiles.add(createCopyTextTile(resultString, resultString));

            if (((Number) result).doubleValue() < Double.MAX_VALUE) {
                double doubleValue = ((Number) result).doubleValue();
                String f1 = findFractionValue(doubleValue, 0.001);
                String f1Value = f1.replace(" = ", "").replace(" ≈ ", "");
                if (f1.length() > 0 && !f1Value.equals(query)) {
                    tiles.add(createCopyTextTile(f1, f1Value));
                }
                String f2 = findFractionValue(doubleValue, 0.01);
                String f2Value = f2.replace(" = ", "").replace(" ≈ ", "");
                if (f2.length() > 0 && !f1.equals(f2) && !f2Value.equals(query)) {
                    tiles.add(createCopyTextTile(f2, f2Value));
                }
            }

            String r = findRoundedValue((Number) result);
            if (r.length() > 0)
                tiles.add(createCopyTextTile(r, r.replace(" ≈ ", "")));

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
        } else {
            tiles.add(createCopyTextTile(result.toString(), result.toString()));
        }

        return tiles;
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

    private Tile createCopyTextTile(String label, String copyText) {
        Tile tile = new Tile(label);
        tile.setCategory("runtime");
        tile.addAction(TileAction.getInstance("copy", copyText));
        return tile;
    }

    @Override
    public String getName() {
        return "Muti-Type Evaluator";
    }

    @Override
    public String getDescription() {
        return "Enter any type expression. Assignments and functions are supported. 'function' for functions/operators.";
    }

    @Override
    public String getAuthor() {
        return "Yan Wittmann";
    }

    @Override
    public String getVersion() {
        return null;
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

    public MultiTypeEvaluatorManager.EvaluationResult solveForX(String expression, BigDecimal x) {
        evaluator.setVariable("x", x);
        return evaluator.evaluate(expression);
    }

    public static boolean checkForValidFunction(String expression) {
        if (STANDARD_INSTANCE.solveForX(expression, BigDecimal.ONE).getClass() != MultiTypeEvaluatorManager.EvaluationResultResult.class) {
            return false;
        } else if (STANDARD_INSTANCE.solveForX(expression, new BigDecimal(2)).getClass() != MultiTypeEvaluatorManager.EvaluationResultResult.class) {
            return false;
        } else if (STANDARD_INSTANCE.solveForX(expression, BigDecimal.TEN).getClass() != MultiTypeEvaluatorManager.EvaluationResultResult.class) {
            return false;
        }
        return true;
    }

    public String getFunctionSignatureForFunctionName(String name) {
        for (MultiTypeEvaluatorManager.ExpressionFunction function : evaluator.getCustomFunctions()) {
            if (name.equals(function.getName())) {
                return function.toString();
            }
        }
        return null;
    }

    private static MultiTypeEvaluatorTile STANDARD_INSTANCE;

    public static MultiTypeEvaluatorTile getInstance() {
        if (STANDARD_INSTANCE == null) {
            STANDARD_INSTANCE = new MultiTypeEvaluatorTile();
        }
        return STANDARD_INSTANCE;
    }
}
