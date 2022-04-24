package bar.util.evaluator;

import org.junit.jupiter.api.Test;

class MultiTypeEvaluatorTest {

    @Test
    public void setExpressionTest() {
        MultiTypeEvaluatorManager manager = new MultiTypeEvaluatorManager();
        System.out.println(manager.evaluate("{-3;sin(5)+3;45*3}"));
    }

}