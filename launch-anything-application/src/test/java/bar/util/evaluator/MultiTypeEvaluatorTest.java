package bar.util.evaluator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

class MultiTypeEvaluatorTest {

    @Test
    public void mappingTest() {
        assertExpressionEquals("map(sin, list(1, 2, 3))", Arrays.asList(0.8414709848078965, 0.9092974268256817, 0.1411200080598672));
        assertExpressionEquals("map(sin, map(cos, list(1, 2, 3)))", Arrays.asList(0.5143952585235492, -0.4042391538522658, -0.8360218615377305));
        assertExpressionEquals("map(pow, list(1, 2, 3), 2)", Arrays.asList(1.0, 4.0, 9.0));
        assertExpressionEquals("map(pow, list(1, 2, 3), 2) + map(pow, list(1, 2, 3), 2)", Arrays.asList(2.0, 8.0, 18.0));
    }

    @Test
    public void matchingTest() {
        assertExpressionEquals("anyMatch(isPrime, list(10, 12))", false);
        assertExpressionEquals("anyMatch(isPrime, list(2, 3, 5, 7))", true);
        assertExpressionEquals("allMatch(isPrime, list(2, 3, 5, 7))", true);
        assertExpressionEquals("allMatch(isPrime, list(2, 3, 5, 7, 10))", false);
        assertExpressionEquals("noneMatch(isPrime, list(17, 2, 11, 200))", false);
        assertExpressionEquals("noneMatch(isPrime, list(12, 14))", true);
    }

    @Test
    public void findFirstTest() {
        assertExpressionEquals("range(1,10)", Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0));
        assertExpressionEquals("findFirst(isPrime, range(1,10))", 2.0);
        assertExpressionEquals("findLast(isPrime, range(1,10))", 7.0);
    }

    @Test
    public void filteringTest() {
        assertExpressionEquals("filter(isPrime, range(1,10))", Arrays.asList(2.0, 3.0, 5.0, 7.0));
    }

    @Test
    public void digitTest() {
        assertExpressionEquals("1", 1);
        assertExpressionEquals("1.0", 1.0);
        assertExpressionEquals("1.0e1", 10.0);
        assertExpressionEquals("1.0e2", 100.0);
        assertExpressionEquals("1+2", 3);
        assertExpressionEquals("4*6+3", 27);
    }

    private void assertExpressionEquals(String expression, double expected) {
        MultiTypeEvaluatorManager manager = new MultiTypeEvaluatorManager();
        MultiTypeEvaluatorManager.EvaluationResult result = manager.evaluate(expression);
        if (result instanceof MultiTypeEvaluatorManager.EvaluationResultFailure) {
            Assertions.fail(expression + "\n" + result);
        }
        Object value = ((MultiTypeEvaluatorManager.EvaluationResultResult) result).getResult();
        if (value instanceof BigDecimal) {
            Assertions.assertEquals(expected, ((BigDecimal) value).doubleValue(), 0.001, result.toString());
        } else if (value instanceof Double) {
            Assertions.assertEquals(expected, (Double) value, 0.001, result.toString());
        } else if (value instanceof Integer) {
            Assertions.assertEquals(expected, (Integer) value, 0.001, result.toString());
        }
    }

    private void assertExpressionEquals(String expression, List<Double> expected) {
        MultiTypeEvaluatorManager manager = new MultiTypeEvaluatorManager();
        List<Object> result = (List<Object>) ((MultiTypeEvaluatorManager.EvaluationResultResult) manager.evaluate(expression)).getResult();
        Assertions.assertEquals(expected.size(), result.size());
        for (int i = 0; i < expected.size(); i++) {
            if (result.get(i) instanceof BigDecimal) {
                Assertions.assertEquals(expected.get(i), ((BigDecimal) result.get(i)).doubleValue(), 0.001, result.toString());
            } else if (result.get(i) instanceof Double) {
                Assertions.assertEquals(expected.get(i), (Double) result.get(i), 0.001, result.toString());
            } else if (result.get(i) instanceof Integer) {
                Assertions.assertEquals(expected.get(i), (Integer) result.get(i), 0.001, result.toString());
            }
        }
    }

    private void assertExpressionEquals(String expression, boolean expected) {
        MultiTypeEvaluatorManager manager = new MultiTypeEvaluatorManager();
        MultiTypeEvaluatorManager.EvaluationResult result = manager.evaluate(expression);
        boolean value = (boolean) ((MultiTypeEvaluatorManager.EvaluationResultResult) result).getResult();
        Assertions.assertEquals(expected, value, result.toString());
    }

}