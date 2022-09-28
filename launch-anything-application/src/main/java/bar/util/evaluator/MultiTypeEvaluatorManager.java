package bar.util.evaluator;

import bar.util.RandomString;
import com.fathzer.soft.javaluator.Function;
import com.fathzer.soft.javaluator.Parameters;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
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

    public void removeCustomFunction(ExpressionFunction function) {
        customFunctions.remove(function);
        regenerateEvaluator();
    }

    public void removeCustomFunctions(Collection<ExpressionFunction> functions) {
        customFunctions.removeAll(functions);
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

        // filter({x,y->type(x) == "string" && y},list("d",24),true)
        final List<ExpressionFunction> inlineFunctions = new ArrayList<>();
        final Matcher inlineFunctionMatcher = INLINE_FUNCTION_PATTERN.matcher(expression);
        while (inlineFunctionMatcher.find()) {
            final String[] inlineFunctionParameters = inlineFunctionMatcher.group(1).trim().split(", *");
            final String inlineFunctionExpression = inlineFunctionMatcher.group(2);
            final String inlineFunctionName = new RandomString(8).nextString();

            final ExpressionFunction inlineFunction = new ExpressionFunction(inlineFunctionName, inlineFunctionParameters, inlineFunctionExpression);
            inlineFunctions.add(inlineFunction);

            addCustomFunction(inlineFunction);
            expression = expression.replace(inlineFunctionMatcher.group(0), inlineFunctionName);
        }

        expression = evaluator.escapeFunctionFunctions(expression);

        if (expression.contains("\"")) {
            expression = evaluator.escapeStringContents(expression);
        }

        Matcher assignmentExpression = ASSIGNMENT_PATTERN.matcher(expression);
        if (assignmentExpression.matches()) {
            String variableName = assignmentExpression.group(1);
            String value = assignmentExpression.group(2);
            final Object result;
            try {
                result = evaluator.evaluate(value, variables);
            } catch (Exception e) {
                removeCustomFunctions(inlineFunctions);
                return new EvaluationResultFailure(e);
            }
            if (result instanceof EvaluationResultFailure) {
                removeCustomFunctions(inlineFunctions);
                return (EvaluationResultFailure) result;
            }
            removeCustomFunctions(inlineFunctions);
            return new EvaluationResultAssignment(variableName, result, this);
        }

        Matcher functionExpression = FUNCTION_PATTERN.matcher(expression);
        if (functionExpression.matches()) {
            if (inlineFunctions.size() > 0) {
                removeCustomFunctions(inlineFunctions);
                return new EvaluationResultFailure(new IllegalArgumentException("Inline functions are not supported in function expressions"));
            }
            final String functionName = functionExpression.group(1);
            final String parameterList = functionExpression.group(2).replace(" ", "");
            final String[] parameters = parameterList.split(",");
            expression = functionExpression.group(3);
            removeCustomFunctions(inlineFunctions);
            return new EvaluationResultFunction(new ExpressionFunction(functionName, parameters, expression), this);
        }

        try {
            Object result = evaluator.evaluate(expression, variables);
            removeCustomFunctions(inlineFunctions);
            return new EvaluationResultResult(result).beautifyResult();
        } catch (Exception e) {
            removeCustomFunctions(inlineFunctions);
            return new EvaluationResultFailure(e);
        }
    }

    private final static Pattern ASSIGNMENT_PATTERN = Pattern.compile("([A-Za-z_]+) *=(?!=) *(.+)");
    private final static Pattern FUNCTION_PATTERN = Pattern.compile("([A-Za-z_]+)\\(((?: *[A-Za-z_]+ *,?)*)\\) *= *(.+)");
    private final static Pattern INLINE_FUNCTION_PATTERN = Pattern.compile("\\{([^{}]+) *-> *([^{}]+)}");

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

        private BigDecimal beautifyBigDecimal(BigDecimal d) {
            return d.stripTrailingZeros();
        }

        private EvaluationResultResult beautifyResult() {
            if (result instanceof BigDecimal) {
                return new EvaluationResultResult(beautifyBigDecimal((BigDecimal) result));
            } else if (result instanceof Collection) {
                final Collection<?> collection = (Collection<?>) result;
                final List<Object> newCollection = new ArrayList<>();
                for (Object o : collection) {
                    if (o instanceof BigDecimal) {
                        newCollection.add(beautifyBigDecimal((BigDecimal) o));
                    } else if (o instanceof BigInteger) {
                        newCollection.add(beautifyBigDecimal(new BigDecimal((BigInteger) o)));
                    } else {
                        newCollection.add(o);
                    }
                }
                return new EvaluationResultResult(newCollection);
            } else {
                return this;
            }
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
