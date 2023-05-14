package bar.tile.custom.menter;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.structure.EvaluationContext;
import de.yanwittmann.menter.interpreter.structure.EvaluationContextLocalInformation;
import de.yanwittmann.menter.interpreter.structure.GlobalContext;
import de.yanwittmann.menter.interpreter.structure.value.PrimitiveValueType;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenterPlotGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(MenterPlotGenerator.class);

    static {
        EvaluationContext.registerNativeFunction("chartjsplotnative", "plot", MenterPlotGenerator::plot);
    }

    public static Value plot(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> arguments) {
        if (arguments.size() < 2) {
            throw new MenterExecutionException("The plot function requires at least two arguments: [width] [height] [xRange] [yRange...] [function...]");
        }

        final boolean hasSpecifiedPlotSize = PrimitiveValueType.isType(arguments.get(0), PrimitiveValueType.NUMBER.getType()) &&
                                             PrimitiveValueType.isType(arguments.get(1), PrimitiveValueType.NUMBER.getType());
        final boolean hasInputXRange = PrimitiveValueType.isType(arguments.get(0), PrimitiveValueType.OBJECT.getType()) ||
                                       (hasSpecifiedPlotSize && PrimitiveValueType.isType(arguments.get(2), PrimitiveValueType.OBJECT.getType()));
        final boolean hasFunction = arguments.stream().anyMatch(value -> PrimitiveValueType.isType(value, PrimitiveValueType.FUNCTION.getType()));
        final boolean hasInputYRange = PrimitiveValueType.isType(arguments.get(arguments.size() - 1), PrimitiveValueType.OBJECT.getType());

        if (!hasInputXRange) {
            throw new MenterExecutionException("The first argument of the plot function must be an array of numbers if a function is specified.");
        }
        if (!hasFunction && !hasInputYRange) {
            throw new MenterExecutionException("The last argument of the plot function must be either an array of numbers or a function.");
        }

        final Value x = hasSpecifiedPlotSize ? arguments.get(2) : arguments.get(0);
        final List<Value> y = generateYValues(context, localInformation, x, arguments);
        final List<Value> expressions = generateExpressionNameStrings(arguments);

        return new Value(new MenterPlot(Arrays.asList(x, new Value(y), new Value(expressions))));
    }

    private static List<Value> generateYValues(GlobalContext context, EvaluationContextLocalInformation localInformation, Value xValues, List<Value> arguments) {
        final List<Value> yValues = new ArrayList<>();

        boolean isFirst = true;
        for (int i = 0; i < arguments.size(); i++) {
            final Value currentArgument = arguments.get(i);

            if (PrimitiveValueType.isType(currentArgument, PrimitiveValueType.FUNCTION)) {
                if (isFirst) {
                    isFirst = false;
                }
                final List<Value> y = new ArrayList<>();
                for (Value x : xValues.getMap().values()) {
                    final Value result;
                    try {
                        result = context.evaluateFunction("chartjsplotnative.plot.eval", currentArgument, context, localInformation, x);
                    } catch (Exception e) {
                        y.add(Value.empty());
                        continue;
                    }

                    if (!PrimitiveValueType.isType(result, PrimitiveValueType.NUMBER)) {
                        throw new MenterExecutionException("The plot function can only plot functions that return numbers.");
                    }
                    y.add(result);
                }
                yValues.add(new Value(y));
            } else if (PrimitiveValueType.isType(currentArgument, PrimitiveValueType.OBJECT)) {
                if (isFirst) {
                    isFirst = false;
                    continue;
                }
                yValues.add(currentArgument);
            }
        }

        return yValues;
    }

    private static List<Value> generateExpressionNameStrings(List<Value> arguments) {
        final List<Value> expressions = new ArrayList<>();

        boolean isFirst = true;
        for (int i = 0; i < arguments.size(); i++) {
            final Value currentArgument = arguments.get(i);

            if (PrimitiveValueType.isType(currentArgument, PrimitiveValueType.FUNCTION)) {
                if (isFirst) {
                    isFirst = false;
                }
                expressions.add(new Value(currentArgument.toDisplayString()));
            } else if (PrimitiveValueType.isType(currentArgument, PrimitiveValueType.OBJECT)) {
                if (isFirst) {
                    isFirst = false;
                    continue;
                }
                expressions.add(new Value("dataset [" + (i - 1) + "]"));
            }
        }

        return expressions;
    }
}
