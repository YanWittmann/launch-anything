package bar.util.evaluator;

import com.fathzer.soft.javaluator.Function;
import com.fathzer.soft.javaluator.Parameters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiTypeEvaluatorManager {

    private MultiTypeEvaluator evaluator;

    private final VariableSet<Object> variables = new VariableSet<>();
    private final List<ExpressionFunction> customFunctions = new ArrayList<>();

    public MultiTypeEvaluatorManager() {
        evaluator = new MultiTypeEvaluator();
        variables.set("x", new BigDecimal(1));
    }

    public void setVariable(String name, Object value) {
        variables.set(name, value);
    }

    public void removeVariable(String name) {
        variables.remove(name);
    }

    public void clearVariables() {
        variables.getVariables().clear();
    }

    public List<ExpressionFunction> getCustomFunctions() {
        return customFunctions;
    }

    public void addCustomFunction(ExpressionFunction function) {
        customFunctions.add(function);
        regenerateEvaluator();
    }

    private void regenerateEvaluator() {
        Parameters parameters = MultiTypeEvaluator.getDefaultParameters();
        customFunctions.forEach(parameters::add);
        evaluator = new MultiTypeEvaluator(parameters);
        customFunctions.forEach(e -> evaluator.addCustomExpressionFunction(e.getName(), e));
    }

    public MultiTypeEvaluator getEvaluator() {
        return evaluator;
    }

    public EvaluationResult evaluate(String expression) {
        if (expression == null) {
            return new EvaluationResultFailure(new NullPointerException("Expression is null"));
        }

        expression = expression.trim();

        if (expression.isEmpty()) {
            return new EvaluationResultFailure(new IllegalArgumentException("Expression is empty"));
        }

        // the set contents are being escaped so that they can be evaluated as a whole and not the individual parts first
        expression = evaluator.escapeSets(expression);

        Matcher assignmentExpression = ASSIGNMENT_PATTERN.matcher(expression);
        if (assignmentExpression.matches()) {
            String variableName = assignmentExpression.group(1);
            String value = assignmentExpression.group(2);
            Object result = evaluator.evaluate(value, variables);
            if (result instanceof EvaluationResultFailure) {
                return (EvaluationResultFailure) result;
            }
            variables.set(variableName, result);
            return new EvaluationResultAssignment(variableName, result, this);
        }

        Matcher functionExpression = FUNCTION_PATTERN.matcher(expression);
        if (functionExpression.matches()) {
            String functionName = functionExpression.group(1);
            String parameterList = functionExpression.group(2).replace(" ", "");
            String[] parameters = parameterList.split(",");
            expression = functionExpression.group(3);
            return new EvaluationResultFunction(new ExpressionFunction(functionName, parameters, expression), this);
        }

        try {
            return new EvaluationResultResult(evaluator.evaluate(expression, variables));
        } catch (Exception e) {
            return new EvaluationResultFailure(e);
        }
    }

    private final static Pattern ASSIGNMENT_PATTERN = Pattern.compile("([A-Za-z_]+) *= *(.+)");
    private final static Pattern FUNCTION_PATTERN = Pattern.compile("([A-Za-z_]+)\\(((?: *[A-Za-z_]+ *,?)*)\\) *= *(.+)");

    public static class ExpressionFunction extends Function implements Expression {
        private final String[] parameters;
        private final String expression;

        public ExpressionFunction(String name, String[] parameters, String expression) {
            super(name, parameters.length);
            this.parameters = parameters;
            this.expression = expression;
        }

        public String getExpression() {
            return expression;
        }

        public String[] getParameters() {
            return parameters;
        }

        @Override
        public String toString() {
            return getName() + "(" + String.join(", ", parameters) + ") = " + expression;
        }
    }

    public interface Expression {
        String getExpression();

        String[] getParameters();
    }

    public interface EvaluationResult {
    }

    public static class EvaluationResultAssignment implements EvaluationResult {
        private final String variableName;
        private final Object result;
        private final MultiTypeEvaluatorManager manager;

        private EvaluationResultAssignment(String variableName, Object result, MultiTypeEvaluatorManager manager) {
            this.variableName = variableName;
            this.result = result;
            this.manager = manager;
        }

        public String getVariableName() {
            return variableName;
        }

        public Object getResult() {
            return result;
        }

        public void applyVariable() {
            manager.setVariable(variableName, result);
        }

        @Override
        public String toString() {
            return variableName + " = " + result;
        }
    }

    public static class EvaluationResultFunction implements EvaluationResult {
        private final ExpressionFunction function;
        private final MultiTypeEvaluatorManager manager;

        private EvaluationResultFunction(ExpressionFunction function, MultiTypeEvaluatorManager manager) {
            this.function = function;
            this.manager = manager;
        }

        public ExpressionFunction getFunction() {
            return function;
        }

        public void applyFunction() {
            manager.addCustomFunction(function);
        }

        @Override
        public String toString() {
            return function.toString();
        }
    }

    public static class EvaluationResultResult implements EvaluationResult {
        private final Object result;

        private EvaluationResultResult(Object result) {
            this.result = result;
        }

        public Object getResult() {
            return result;
        }

        @Override
        public String toString() {
            return "Result: " + result;
        }
    }

    public static class EvaluationResultFailure implements EvaluationResult {
        private final Exception reason;

        private EvaluationResultFailure(Exception reason) {
            this.reason = reason;
        }

        public Exception getReason() {
            return reason;
        }

        public void printStackTrace() {
            reason.printStackTrace();
        }

        @Override
        public String toString() {
            return "Failure: " + reason;
        }
    }
}
