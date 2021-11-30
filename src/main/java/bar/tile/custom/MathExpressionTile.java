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
                double result = evaluate(value);

                Tile tile = new Tile(varName + " = " + makeResultForTileLabel(result));
                tile.setCategory("runtime");
                TileAction action = new TileAction(() -> variables.put(varName, result));
                tile.addAction(action);
                return Collections.singletonList(tile);
            } else {
                double result = evaluate(search);
                Tile tile = new Tile(makeResultForTileLabel(result));
                tile.setCategory("runtime");
                TileAction action = new TileAction("copy", result + "");
                tile.addAction(action);
                return Collections.singletonList(tile);
            }
        } catch (UnknownFunctionOrVariableException e) {
            return Collections.emptyList();
        }
    }

    private String makeResultForTileLabel(double result) {
        return removeTrailingZeros(result) +
               removeDoubles(findFractionValue(result, 0.001),
                       findFractionValue(result, 0.01)) +
               findRoundedValue(result);
    }

    private String removeDoubles(String... strings) {
        String concentrated = "";
        for (String s : strings) {
            if (!concentrated.contains(s)) {
                concentrated += s;
            }
        }
        return concentrated;
    }

    private String findRoundedValue(double value) {
        double distance = Math.abs(value - Math.round(value));
        if (distance < 0.1 && distance > 0) {
            return " ≈ " + Math.round(value);
        }
        return "";
    }

    private String findFractionValue(double value, double accuracy) {
        Fraction fraction = realToFraction(value, accuracy);
        if (fraction.numerator == fraction.denominator || fraction.denominator == 1) return "";
        float distance = (float) Math.abs(((float) fraction.numerator / (float) fraction.denominator) - value);
        if (distance < 0.001) {
            return " = " + fraction;
        }
        return " ≈ " + fraction;
    }

    private double evaluate(String expression) {
        return Util.evaluateMathematicalExpression(expression.trim(), variables);
    }

    private String removeTrailingZeros(double value) {
        String s = value + "";
        if (s.endsWith(".0")) return s.replace(".0", "");
        return s;
    }

    /**
     * https://stackoverflow.com/a/32903747
     *
     * @param value    The value to convert into a fraction.
     * @param accuracy The accuracy of the fraction.
     * @return The fraction representation of the value.
     */
    public Fraction realToFraction(double value, double accuracy) {
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

        while (true) {
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
    }

    private static class Fraction {
        private final int numerator;
        private final int denominator;

        public Fraction(int numerator, int denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }

        @Override
        public String toString() {
            return numerator + "/" + denominator;
        }
    }

    public static String getTitle() {
        return "Math Expression";
    }

    public static String getDescription() {
        return "Enter any mathematical expression";
    }
}
