package bar.util.evaluator;

import bar.util.RandomString;
import com.fathzer.soft.javaluator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultiTypeEvaluator extends AbstractEvaluator<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTypeEvaluator.class);

    private final static int DOUBLE_SCALE = 20;
    private final Map<String, MultiTypeEvaluatorManager.Expression> customExpressionFunctions = new HashMap<>();

    public MultiTypeEvaluator() {
        this(getDefaultParameters());
    }

    public MultiTypeEvaluator(Parameters parameters) {
        super(parameters);
    }

    public void addCustomExpressionFunction(String name, MultiTypeEvaluatorManager.Expression expression) {
        customExpressionFunctions.put(name, expression);
        addEscapeCharacters(name);
    }

    public static Parameters getDefaultParameters() {
        if (DEFAULT_PARAMETERS == null) {
            Parameters result = new Parameters();
            result.addOperators(Arrays.asList(OPERATORS));
            result.addFunctions(Arrays.asList(FUNCTIONS));
            result.addConstants(Arrays.asList(CONSTANTS));
            result.addFunctionBracket(BracketPair.PARENTHESES);
            result.addExpressionBracket(BracketPair.PARENTHESES);
            DEFAULT_PARAMETERS = result;
            return result;
        }
        return DEFAULT_PARAMETERS;
    }

    public final static Pattern SET_PATTERN = Pattern.compile("\\{(.*)}");

    @Override
    protected Object toValue(String literal, Object evaluationContext) {
        String normalizedLiteral = literal.trim().toLowerCase();
        if (evaluationContext instanceof VariableSet && ((VariableSet<?>) evaluationContext).getVariables().containsKey(normalizedLiteral)) {
            return toValue(((VariableSet<?>) evaluationContext).getVariables().get(normalizedLiteral));
        } else if (normalizedLiteral.equals("true") || normalizedLiteral.equals("t")) {
            return true;
        } else if (normalizedLiteral.equals("false") || normalizedLiteral.equals("f")) {
            return false;
        } else if (normalizedLiteral.startsWith("0x")) {
            return new BigInteger(normalizedLiteral.substring(2), 16);
        } else if (normalizedLiteral.startsWith("0b")) {
            return new BigInteger(normalizedLiteral.substring(2), 2);
        } else if (normalizedLiteral.startsWith("0o")) {
            return new BigInteger(normalizedLiteral.substring(2), 8);
        } else if (normalizedLiteral.startsWith("{")) {
            Matcher setMatcher = SET_PATTERN.matcher(normalizedLiteral);
            if (setMatcher.matches()) {
                String set = unescapeExpression(setMatcher.group(1).trim());
                set = escapeSets(set);
                String[] elements = set.split("; *");
                if (elements.length == 0) {
                    return Collections.emptyList();
                } else {
                    return Arrays.stream(elements).map(element -> evaluate(element, evaluationContext)).collect(Collectors.toList());
                }
            } else {
                return Collections.emptyList();
            }
        } else {
            String unescaped = unescapeExpression(normalizedLiteral);
            for (Function function : FUNCTIONS) {
                if (unescaped.equals(function.getName())) {
                    return function;
                }
            }
            for (Map.Entry<String, MultiTypeEvaluatorManager.Expression> customFunction : customExpressionFunctions.entrySet()) {
                if (unescaped.equals(customFunction.getKey())) {
                    return customFunction.getValue();
                }
            }
        }
        try {
            return new BigDecimal(normalizedLiteral);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Cannot parse literal '" + normalizedLiteral + "'");
        }
    }

    protected Object toValue(Object literal) {
        if (literal instanceof BigDecimal) {
            return literal;
        } else if (literal instanceof BigInteger) {
            return literal;
        } else if (literal instanceof Boolean) {
            return literal;
        } else if (literal instanceof String) {
            return toValue((String) literal, null);
        }
        return null;
    }

    public static final Constant PI = new Constant("pi");
    public static final Constant E = new Constant("e");

    private static final Constant[] CONSTANTS = new Constant[]{PI, E};

    @Override
    protected Object evaluate(Constant constant, Object evaluationContext) {
        if (PI.equals(constant)) {
            return new BigDecimal(Math.PI);
        } else if (E.equals(constant)) {
            return new BigDecimal(Math.E);
        } else {
            return super.evaluate(constant, evaluationContext);
        }
    }

    public static final Function CEIL = new Function("ceil", 1);
    public static final Function FLOOR = new Function("floor", 1);
    public static final Function ROUND = new Function("round", 1);
    public static final Function ABS = new Function("abs", 1);
    public static final Function SINE = new Function("sin", 1);
    public static final Function COSINE = new Function("cos", 1);
    public static final Function TANGENT = new Function("tan", 1);
    public static final Function ACOSINE = new Function("acos", 1);
    public static final Function ASINE = new Function("asin", 1);
    public static final Function ATAN = new Function("atan", 1);
    public static final Function SINEH = new Function("sinh", 1);
    public static final Function COSINEH = new Function("cosh", 1);
    public static final Function TANGENTH = new Function("tanh", 1);
    public static final Function MIN = new Function("min", 1, Integer.MAX_VALUE);
    public static final Function MAX = new Function("max", 1, Integer.MAX_VALUE);
    public static final Function SUM = new Function("sum", 1, Integer.MAX_VALUE);
    public static final Function AVERAGE = new Function("avg", 1, Integer.MAX_VALUE);
    public static final Function PRODUCT = new Function("product", 1, Integer.MAX_VALUE);
    public static final Function COUNT_DEEP = new Function("countDeep", 1, Integer.MAX_VALUE);
    public static final Function COUNT_SHALLOW = new Function("count", 1, Integer.MAX_VALUE);
    public static final Function LN = new Function("ln", 1);
    public static final Function LOG = new Function("log", 1);
    public static final Function RANDOM = new Function("random", 2);
    public static final Function GGT = new Function("ggt", 2);
    public static final Function GCD = new Function("gcd", 2);
    public static final Function PHI = new Function("phi", 1);
    public static final Function IS_PRIME = new Function("isPrime", 1);
    public static final Function NEXT_PRIME = new Function("nextPrime", 1);
    public static final Function IF_ELSE = new Function("if", 3);
    public static final Function TO_BINARY_STRING = new Function("toBin", 1);
    public static final Function TO_HEX_STRING = new Function("toHex", 1);
    public static final Function POW = new Function("pow", 2);
    public static final Function SQRT = new Function("sqrt", 1);
    public static final Function ROOT = new Function("root", 2);
    public static final Function SUM_OF_DIGITS = new Function("sod", 1);
    public static final Function FACULTY = new Function("fac", 1);
    public static final Function FACTORIZE = new Function("factorize", 1);
    public static final Function DIVISORS = new Function("divisors", 1);
    public static final Function GROUP_DUPLICATES = new Function("groupDuplicates", 1, Integer.MAX_VALUE);
    public static final Function SORT = new Function("sort", 1, Integer.MAX_VALUE);
    public static final Function MERGE = new Function("merge", 1, Integer.MAX_VALUE);
    public static final Function DISTINCT = new Function("distinct", 1, Integer.MAX_VALUE);
    public static final Function GET_ELEMENT = new Function("elementAt", 1, Integer.MAX_VALUE);
    public static final Function FILTER = new Function("filter", 2, Integer.MAX_VALUE);
    public static final Function FLAT_FILTER = new Function("flatFilter", 2, Integer.MAX_VALUE);
    public static final Function MAP = new Function("map", 2, Integer.MAX_VALUE);
    public static final Function FLAT_MAP = new Function("flatMap", 2, Integer.MAX_VALUE);
    public static final Function LIMIT = new Function("limit", 1, Integer.MAX_VALUE);
    public static final Function FLAT_LIMIT = new Function("limit", 1, Integer.MAX_VALUE);
    public static final Function FLATTEN = new Function("flat", 1, Integer.MAX_VALUE);
    public static final Function RANGE = new Function("range", 2);

    private static final Function[] FUNCTIONS = new Function[]{SINE, COSINE, TANGENT, ASINE, ACOSINE, ATAN, SINEH,
            COSINEH, TANGENTH, MIN, MAX, SUM, AVERAGE, PRODUCT, COUNT_DEEP, COUNT_SHALLOW, LN, LOG, ROUND, CEIL, FLOOR,
            ABS, RANDOM, GGT, GCD, PHI, IS_PRIME, NEXT_PRIME, IF_ELSE, TO_BINARY_STRING, TO_HEX_STRING, POW, SQRT, ROOT,
            SUM_OF_DIGITS, FACULTY, FACTORIZE, DIVISORS, GROUP_DUPLICATES, SORT, MERGE, DISTINCT, GET_ELEMENT, FILTER,
            FLAT_FILTER, MAP, FLAT_MAP, LIMIT, FLAT_LIMIT, FLATTEN, RANGE};

    private static final Function[] FUNCTION_FUNCTIONS = new Function[]{FILTER, FLAT_FILTER, MAP, FLAT_MAP};

    @Override
    protected Object evaluate(Function function, Iterator<Object> arguments, Object evaluationContext) {
        if (CEIL.equals(function)) {
            return getBigDecimal(arguments).setScale(0, RoundingMode.CEILING);
        } else if (FLOOR.equals(function)) {
            return getBigDecimal(arguments).setScale(0, RoundingMode.FLOOR);
        } else if (ROUND.equals(function)) {
            return getBigDecimal(arguments).setScale(0, RoundingMode.HALF_EVEN);
        } else if (ABS.equals(function)) {
            return getBigDecimal(arguments).abs();
        } else if (SINE.equals(function)) {
            return BigDecimal.valueOf(Math.sin(getBigDecimal(arguments).doubleValue()));
        } else if (COSINE.equals(function)) {
            return BigDecimal.valueOf(Math.cos(getBigDecimal(arguments).doubleValue()));
        } else if (TANGENT.equals(function)) {
            return BigDecimal.valueOf(Math.tan(getBigDecimal(arguments).doubleValue()));
        } else if (ACOSINE.equals(function)) {
            return BigDecimal.valueOf(Math.acos(getBigDecimal(arguments).doubleValue()));
        } else if (ASINE.equals(function)) {
            return BigDecimal.valueOf(Math.asin(getBigDecimal(arguments).doubleValue()));
        } else if (ATAN.equals(function)) {
            return BigDecimal.valueOf(Math.atan(getBigDecimal(arguments).doubleValue()));
        } else if (SINEH.equals(function)) {
            return BigDecimal.valueOf(Math.sinh(getBigDecimal(arguments).doubleValue()));
        } else if (COSINEH.equals(function)) {
            return BigDecimal.valueOf(Math.cosh(getBigDecimal(arguments).doubleValue()));
        } else if (TANGENTH.equals(function)) {
            return BigDecimal.valueOf(Math.tanh(getBigDecimal(arguments).doubleValue()));
        } else if (MIN.equals(function)) {
            BigDecimal min = null;
            List<Object> allArgumentsAsList = getAllArgumentsAsListFlat(arguments);
            for (Object next : allArgumentsAsList) {
                BigDecimal value = getBigDecimal(next);
                if (value != null) {
                    if (min == null || min.compareTo(value) > 0) {
                        min = value;
                    }
                }
            }
            return min;
        } else if (MAX.equals(function)) {
            BigDecimal max = null;
            List<Object> allArgumentsAsList = getAllArgumentsAsListFlat(arguments);
            for (Object next : allArgumentsAsList) {
                BigDecimal value = getBigDecimal(next);
                if (value != null) {
                    if (max == null || value.compareTo(max) > 0) {
                        max = value;
                    }
                }
            }
            return max;
        } else if (SUM.equals(function)) {
            BigDecimal sum = BigDecimal.ZERO;
            List<Object> allArgumentsAsList = getAllArgumentsAsListFlat(arguments);
            for (Object next : allArgumentsAsList) {
                BigDecimal value = getBigDecimal(next);
                if (value != null) {
                    sum = sum.add(value);
                }
            }
            return sum;
        } else if (AVERAGE.equals(function)) {
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            List<Object> allArgumentsAsList = getAllArgumentsAsListFlat(arguments);
            for (Object next : allArgumentsAsList) {
                BigDecimal value = getBigDecimal(next);
                if (value != null) {
                    sum = sum.add(value);
                    count++;
                }
            }
            return sum.divide(BigDecimal.valueOf(count), DOUBLE_SCALE, RoundingMode.HALF_EVEN);
        } else if (PRODUCT.equals(function)) {
            BigDecimal product = BigDecimal.ONE;
            List<Object> allArgumentsAsList = getAllArgumentsAsListFlat(arguments);
            for (Object next : allArgumentsAsList) {
                BigDecimal value = getBigDecimal(next);
                if (value != null) {
                    product = product.multiply(value);
                }
            }
            return product;
        } else if (COUNT_DEEP.equals(function)) {
            return getAllArgumentsAsListFlat(arguments).size();
        } else if (COUNT_SHALLOW.equals(function)) {
            int count = 0;
            while (arguments.hasNext()) {
                arguments.next();
                count++;
            }
            return count;
        } else if (GROUP_DUPLICATES.equals(function)) {
            Map<Object, BigDecimal> counts = new LinkedHashMap<>();
            List<Object> allArgumentsAsList = getAllArgumentsAsListFlat(arguments);
            for (Object next : allArgumentsAsList) {
                counts.putIfAbsent(next, BigDecimal.ZERO);
                counts.put(next, counts.get(next).add(BigDecimal.ONE));
            }
            List<Object> results = new ArrayList<>();
            for (Map.Entry<Object, BigDecimal> entry : counts.entrySet()) {
                if (entry.getValue().compareTo(BigDecimal.ONE) > 0) {
                    results.add(Arrays.asList(entry.getKey(), entry.getValue()));
                } else {
                    results.add(entry.getKey());
                }
            }
            return results;
        } else if (SORT.equals(function)) {
            List<Object> argumentsList = getAllArgumentsAsList(arguments);
            if (argumentsList.size() == 1 && argumentsList.get(0) instanceof List) {
                argumentsList = (List<Object>) argumentsList.get(0);
            }
            return argumentsList.stream().sorted(OBJECT_COMPARATOR).collect(Collectors.toList());
        } else if (MERGE.equals(function)) {
            List<Object> argumentsList = getAllArgumentsAsList(arguments);
            List<Object> merged = new ArrayList<>();
            for (Object next : argumentsList) {
                if (next instanceof List) {
                    merged.addAll((List<Object>) next);
                } else {
                    merged.add(next);
                }
            }
            return merged;
        } else if (DISTINCT.equals(function)) {
            List<Object> argumentsList = getAllArgumentsAsList(arguments);
            if (argumentsList.size() == 1 && argumentsList.get(0) instanceof List) {
                argumentsList = (List<Object>) argumentsList.get(0);
            }
            return argumentsList.stream().distinct().collect(Collectors.toList());
        } else if (LN.equals(function)) {
            return BigDecimal.valueOf(Math.log(getBigDecimal(arguments).doubleValue()));
        } else if (LOG.equals(function)) {
            return BigDecimal.valueOf(Math.log10(getBigDecimal(arguments).doubleValue()));
        } else if (RANDOM.equals(function)) {
            BigDecimal b1 = getBigDecimal(arguments);
            BigDecimal b2 = getBigDecimal(arguments);
            if (b1.compareTo(b2) > 0) {
                BigDecimal tmp = b1;
                b1 = b2;
                b2 = tmp;
            }
            return b1.add(b2.subtract(b1).multiply(BigDecimal.valueOf(Math.random())));
        } else if (GGT.equals(function) || GCD.equals(function)) {
            BigDecimal b1 = getBigDecimal(arguments);
            BigDecimal b2 = getBigDecimal(arguments);
            if (b1.compareTo(BigDecimal.ZERO) == 0 || b2.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ONE;
            }
            while (b1.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal tmp = b1;
                b1 = b2.remainder(b1);
                b2 = tmp;
            }
            return b2;
        } else if (PHI.equals(function)) {
            // https://stackoverflow.com/questions/13216853/calculating-eulers-totient-function-for-very-large-numbers-java
            // implementation translated to big integer

            BigInteger n = getBigDecimal(arguments).toBigInteger();
            BigInteger tot = n;
            BigInteger p = BigInteger.valueOf(2);
            while (p.multiply(p).compareTo(n) <= 0) {
                if (n.remainder(p).equals(BigInteger.ZERO)) {
                    tot = tot.divide(p);
                    tot = tot.multiply(p.subtract(BigInteger.ONE));
                    while (n.remainder(p).equals(BigInteger.ZERO)) {
                        n = n.divide(p);
                    }
                }
                p = p.add(BigInteger.ONE);
            }
            if (n.compareTo(BigInteger.ONE) > 0) {
                tot = tot.divide(n);
                tot = tot.multiply(n.subtract(BigInteger.ONE));
            }

            return tot;
        } else if (IS_PRIME.equals(function)) {
            BigInteger n = getBigDecimal(arguments).toBigInteger();
            return isPrime(n);
        } else if (NEXT_PRIME.equals(function)) {
            BigInteger n = getBigDecimal(arguments).toBigInteger();
            return nextPrime(n);
        } else if (IF_ELSE.equals(function)) {
            Boolean condition = getBoolean(arguments);
            Object ifTrue = arguments.next();
            Object ifFalse = arguments.next();
            return condition ? ifTrue : ifFalse;
        } else if (TO_BINARY_STRING.equals(function)) {
            BigInteger n = getBigDecimal(arguments).toBigInteger();
            return "0b" + n.toString(2);
        } else if (TO_HEX_STRING.equals(function)) {
            BigInteger n = getBigDecimal(arguments).toBigInteger();
            return "0x" + n.toString(16);
        } else if (POW.equals(function)) {
            BigDecimal base = getBigDecimal(arguments);
            BigDecimal exponent = getBigDecimal(arguments);
            return base.pow(exponent.intValue());
        } else if (SQRT.equals(function)) {
            BigDecimal n = getBigDecimal(arguments);
            return BigDecimal.valueOf(Math.sqrt(n.doubleValue()));
        } else if (ROOT.equals(function)) {
            BigDecimal a = getBigDecimal(arguments);
            BigDecimal n = getBigDecimal(arguments);
            return BigDecimal.valueOf(Math.exp(Math.log(a.doubleValue()) / n.doubleValue()));
        } else if (SUM_OF_DIGITS.equals(function)) {
            BigDecimal n = getBigDecimal(arguments);
            BigDecimal sum = BigDecimal.ZERO;
            while (n.compareTo(BigDecimal.ZERO) > 0) {
                sum = sum.add(n.remainder(BigDecimal.TEN));
                n = n.divide(BigDecimal.TEN, 0, RoundingMode.DOWN);
            }
            return sum;
        } else if (FACULTY.equals(function)) {
            BigDecimal operand = getBigDecimal(arguments);
            BigDecimal result = BigDecimal.ONE;
            for (int i = 1; i <= operand.intValue(); i++) {
                result = result.multiply(BigDecimal.valueOf(i));
            }
            return result;
        } else if (FACTORIZE.equals(function)) {
            List<BigDecimal> factors = new ArrayList<>();
            BigDecimal n = getBigDecimal(arguments);
            BigDecimal i = BigDecimal.valueOf(2);
            if (n.compareTo(BigDecimal.ONE) == 0) {
                factors.add(BigDecimal.ONE);
            }
            while (i.compareTo(n) <= 0) {
                if (n.remainder(i).equals(BigDecimal.ZERO)) {
                    factors.add(i);
                    n = n.divide(i, 0, RoundingMode.DOWN);
                    i = BigDecimal.ONE;
                }
                i = i.add(BigDecimal.ONE);
            }
            return factors;
        } else if (DIVISORS.equals(function)) {
            List<BigDecimal> divisors = new ArrayList<>();
            BigDecimal n = getBigDecimal(arguments);
            BigDecimal i = BigDecimal.ONE;
            while (i.compareTo(n) <= 0) {
                if (n.remainder(i).equals(BigDecimal.ZERO)) {
                    divisors.add(i);
                }
                i = i.add(BigDecimal.ONE);
            }
            return divisors;
        } else if (GET_ELEMENT.equals(function)) {
            BigDecimal index = getBigDecimal(arguments);
            List<Object> allElements = getAllArgumentsAsListFlat(arguments);
            return allElements.get(index.intValue());
        } else if (FILTER.equals(function)) {
            Object f = arguments.next();
            if (f instanceof Function) {
                Function filterFunction = (Function) f;
                List<Object> allElements = getAllArgumentsAsList(arguments);
                return filterObjectsUsingFunction(filterFunction, allElements);
            }
            throw new IllegalArgumentException("Filter function must be a function");
        } else if (FLAT_FILTER.equals(function)) {
            Object f = arguments.next();
            if (f instanceof Function) {
                Function filterFunction = (Function) f;
                List<Object> allElements = getAllArgumentsFirstLayer(arguments);
                return filterObjectsUsingFunction(filterFunction, allElements);
            }
            throw new IllegalArgumentException("Filter function must be a function");
        } else if (MAP.equals(function)) {
            Object f = arguments.next();
            if (f instanceof Function) {
                Function mapFunction = (Function) f;
                List<Object> allElements = getAllArgumentsAsList(arguments);
                return mapObjectsUsingFunction(mapFunction, allElements);
            }
            throw new IllegalArgumentException("Map function must be a function");
        } else if (FLAT_MAP.equals(function)) {
            Object f = arguments.next();
            if (f instanceof Function) {
                Function mapFunction = (Function) f;
                List<Object> allElements = getAllArgumentsFirstLayer(arguments);
                return mapObjectsUsingFunction(mapFunction, allElements);
            }
            throw new IllegalArgumentException("Map function must be a function");
        } else if (LIMIT.equals(function)) {
            BigDecimal limit = getBigDecimal(arguments);
            List<Object> allElements = getAllArgumentsAsList(arguments);
            return allElements.subList(0, limit.intValue());
        } else if (FLAT_LIMIT.equals(function)) {
            BigDecimal limit = getBigDecimal(arguments);
            List<Object> allElements = getAllArgumentsAsListFlat(arguments);
            return allElements.subList(0, limit.intValue());
        } else if (FLATTEN.equals(function)) {
            return getAllArgumentsAsListFlat(arguments);
        } else if (RANGE.equals(function)) {
            BigDecimal from = getBigDecimal(arguments);
            BigDecimal to = getBigDecimal(arguments);
            return IntStream.rangeClosed(from.intValue(), to.intValue()).boxed().collect(Collectors.toList());
        } else {
            for (Map.Entry<String, MultiTypeEvaluatorManager.Expression> entry : customExpressionFunctions.entrySet()) {
                if (function.getName().equals(entry.getKey())) {
                    VariableSet<Object> variables;
                    if (evaluationContext instanceof VariableSet) {
                        variables = new VariableSet<>((VariableSet<?>) evaluationContext);
                    } else {
                        variables = new VariableSet<>();
                    }
                    for (String parameter : entry.getValue().getParameters()) {
                        variables.set(parameter, toValue(arguments.next()));
                    }
                    return evaluate(entry.getValue().getExpression(), variables);
                }
            }
            return super.evaluate(function, arguments, evaluationContext);
        }
    }

    private List<Object> getAllArgumentsAsList(Iterator<Object> arguments) {
        List<Object> allElements = new ArrayList<>();
        while (arguments.hasNext()) {
            allElements.add(arguments.next());
        }
        return allElements;
    }

    private List<Object> getAllSetElements(List<Object> list) {
        List<Object> result = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof List) {
                result.addAll((List<?>) element);
            } else {
                result.add(element);
            }
        }
        return result;
    }

    private Object mapObjectsUsingFunction(Function mapFunction, List<Object> allElements) {
        List<Object> mappedElements = new ArrayList<>();
        List<Object> parameters = getParametersForFunction(mapFunction, allElements);
        for (Object element : allElements) {
            List<Object> newParameters = new ArrayList<>(parameters);
            newParameters.add(element);
            Object result = evaluateExpressionWithParameter(mapFunction, newParameters);
            mappedElements.add(result);
        }
        return mappedElements;
    }

    private Object filterObjectsUsingFunction(Function filterFunction, List<Object> allElements) {
        List<Object> filteredElements = new ArrayList<>();
        List<Object> parameters = getParametersForFunction(filterFunction, allElements);
        for (Object element : allElements) {
            List<Object> newParameters = new ArrayList<>(parameters);
            newParameters.add(element);
            Object result = evaluateExpressionWithParameter(filterFunction, newParameters);
            if (result instanceof Boolean) {
                if ((Boolean) result) {
                    filteredElements.add(element);
                }
            } else {
                throw new IllegalArgumentException("Filter function must return boolean");
            }
        }
        return filteredElements;
    }

    private List<Object> getParametersForFunction(Function mapFunction, List<Object> allElements) {
        List<Object> parameters = new ArrayList<>();
        if (mapFunction.getMinimumArgumentCount() > 1) {
            for (int i = 0; i < mapFunction.getMinimumArgumentCount() - 1; i++) {
                parameters.add(allElements.get(i));
                allElements.remove(i);
            }
        }
        return parameters;
    }

    private Object evaluateExpressionWithParameter(Function filterFunction, List<Object> parameters) {
        VariableSet<Object> variables = new VariableSet<>();

        String expression;

        if (filterFunction instanceof MultiTypeEvaluatorManager.Expression) {
            // if the function is a custom function, the corresponding parameter names have to be set
            MultiTypeEvaluatorManager.Expression customFunction = (MultiTypeEvaluatorManager.Expression) filterFunction;
            for (int i = 0; i < customFunction.getParameters().length; i++) {
                variables.set(customFunction.getParameters()[i], parameters.get(i));
            }
            expression = customFunction.getExpression();
        } else {
            // otherwise, the parameters are set in the order they are defined in the function
            for (int i = 0; i < parameters.size(); i++) {
                Object element = parameters.get(i);
                variables.set("elem" + i, element);
            }
            expression = filterFunction.getName() + "(" + String.join(", ", variables.getVariables().keySet()) + ")";
        }

        return evaluate(expression, variables);
    }

    private List<Object> getAllArgumentsAsListFlat(Iterator<Object> arguments) {
        List<Object> result = new ArrayList<>();
        while (arguments.hasNext()) {
            Object next = arguments.next();
            if (next instanceof List) {
                result.addAll(getAllSetElements((List<Object>) next));
            } else {
                result.add(next);
            }
        }
        return result;
    }

    private List<Object> getAllArgumentsFirstLayer(Iterator<Object> arguments) {
        List<Object> result = new ArrayList<>();
        while (arguments.hasNext()) {
            Object next = arguments.next();
            if (next instanceof List) {
                result.addAll((List<Object>) next);
            } else {
                result.add(next);
            }
        }
        return result;
    }

    private final static Comparator<Object> OBJECT_COMPARATOR = new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            if (o1 instanceof Comparable && o2 instanceof Comparable) {
                return ((Comparable) o1).compareTo(o2);
            } else {
                if (o1 instanceof List && o2 instanceof Comparable) {
                    List<Object> list = (List<Object>) o1;
                    if (list.size() > 0) {
                        Object first = list.get(0);
                        return OBJECT_COMPARATOR.compare(first, o2);
                    }
                } else if (o2 instanceof List && o1 instanceof Comparable) {
                    List<Object> list = (List<Object>) o2;
                    if (list.size() > 0) {
                        Object first = list.get(0);
                        return OBJECT_COMPARATOR.compare(o1, first);
                    }
                }
            }
            return 0;
        }
    };

    private boolean isPrime(BigInteger n) {
        return n.isProbablePrime(20);
    }

    private BigInteger nextPrime(BigInteger n) {
        for (int i = 0; i < 99999; i++) {
            n = n.add(BigInteger.valueOf(1));
            if (isPrime(n)) {
                return n;
            }
        }
        return n;
    }

    public static final Operator EXPONENT_DOUBLE = new Operator("^^", 2, Operator.Associativity.LEFT, 15);
    public static final Operator EXPONENT = new Operator("^", 2, Operator.Associativity.LEFT, 15);
    public static final Operator NEGATE = new Operator("-", 1, Operator.Associativity.RIGHT, 14);
    public static final Operator DECREMENT = new Operator("--", 1, Operator.Associativity.RIGHT, 14);
    public static final Operator INCREMENT = new Operator("++", 1, Operator.Associativity.RIGHT, 14);
    public static final Operator LOGICAL_NOT_1 = new Operator("!", 1, Operator.Associativity.LEFT, 14);
    public static final Operator LOGICAL_NOT_2 = new Operator("NOT", 1, Operator.Associativity.RIGHT, 14);
    public static final Operator MULTIPLY = new Operator("*", 2, Operator.Associativity.LEFT, 13);
    public static final Operator DIVIDE = new Operator("/", 2, Operator.Associativity.LEFT, 13);
    public static final Operator MODULO = new Operator("%", 2, Operator.Associativity.LEFT, 13);
    public static final Operator MINUS = new Operator("-", 2, Operator.Associativity.LEFT, 12);
    public static final Operator PLUS = new Operator("+", 2, Operator.Associativity.LEFT, 12);
    public static final Operator RIGHT_SHIFT = new Operator(">>", 2, Operator.Associativity.LEFT, 11);
    public static final Operator LEFT_SHIFT = new Operator("<<", 2, Operator.Associativity.LEFT, 11);
    public static final Operator RELATIONAL_SMALLER = new Operator("<", 2, Operator.Associativity.LEFT, 10);
    public static final Operator RELATIONAL_SMALLER_OR_EQUAL = new Operator("<=", 2, Operator.Associativity.LEFT, 10);
    public static final Operator RELATIONAL_GREATER = new Operator(">", 2, Operator.Associativity.LEFT, 10);
    public static final Operator RELATIONAL_GREATER_OR_EQUAL = new Operator(">=", 2, Operator.Associativity.LEFT, 10);
    public static final Operator EQUALITY = new Operator("==", 2, Operator.Associativity.LEFT, 9);
    public static final Operator INEQUALITY = new Operator("!=", 2, Operator.Associativity.LEFT, 9);
    public static final Operator BITWISE_AND = new Operator("&", 2, Operator.Associativity.LEFT, 8);
    public static final Operator BITWISE_OR = new Operator("|", 2, Operator.Associativity.LEFT, 7);
    public static final Operator LOGICAL_AND_1 = new Operator("&&", 2, Operator.Associativity.LEFT, 6);
    public static final Operator LOGICAL_AND_2 = new Operator("AND", 2, Operator.Associativity.LEFT, 6);
    public static final Operator LOGICAL_OR_1 = new Operator("||", 2, Operator.Associativity.LEFT, 5);
    public static final Operator LOGICAL_OR_2 = new Operator("OR", 2, Operator.Associativity.LEFT, 5);
    public static final Operator LOGICAL_IMPLICATION = new Operator("=>", 2, Operator.Associativity.LEFT, 4);
    public static final Operator LOGICAL_EQUIVALENCE = new Operator("<=>", 2, Operator.Associativity.LEFT, 3);

    private static final Operator[] OPERATORS = new Operator[]{NEGATE, DECREMENT, INCREMENT, MULTIPLY, DIVIDE, MODULO, MINUS, PLUS, RIGHT_SHIFT, LEFT_SHIFT,
            RELATIONAL_SMALLER, RELATIONAL_SMALLER_OR_EQUAL, RELATIONAL_GREATER, RELATIONAL_GREATER_OR_EQUAL, EQUALITY, INEQUALITY, BITWISE_AND, BITWISE_OR,
            LOGICAL_AND_1, LOGICAL_AND_2, LOGICAL_OR_1, LOGICAL_OR_2, LOGICAL_NOT_1, LOGICAL_NOT_2, EXPONENT, EXPONENT_DOUBLE, LOGICAL_IMPLICATION,
            LOGICAL_EQUIVALENCE};

    @Override
    protected Object evaluate(Operator operator, Iterator<Object> operands, Object evaluationContext) {
        if (NEGATE.equals(operator)) {
            return getBigDecimal(operands).negate();
        } else if (MINUS.equals(operator)) {
            BigDecimal left = getBigDecimal(operands);
            BigDecimal right = getBigDecimal(operands);
            return left.subtract(right);
        } else if (PLUS.equals(operator)) {
            BigDecimal left = getBigDecimal(operands);
            BigDecimal right = getBigDecimal(operands);
            return left.add(right);
        } else if (MULTIPLY.equals(operator)) {
            BigDecimal left = getBigDecimal(operands);
            BigDecimal right = getBigDecimal(operands);
            return left.multiply(right);
        } else if (DIVIDE.equals(operator)) {
            BigDecimal left = getBigDecimal(operands);
            BigDecimal right = getBigDecimal(operands);
            return left.divide(right, DOUBLE_SCALE, RoundingMode.HALF_EVEN);
        } else if (EXPONENT.equals(operator) || EXPONENT_DOUBLE.equals(operator)) {
            BigDecimal left = getBigDecimal(operands);
            BigDecimal right = getBigDecimal(operands);
            return left.pow(right.intValue());
        } else if (MODULO.equals(operator)) {
            BigDecimal left = getBigDecimal(operands);
            BigDecimal right = getBigDecimal(operands);
            return left.remainder(right);
        } else if (DECREMENT.equals(operator)) {
            BigDecimal operand = getBigDecimal(operands);
            return operand.subtract(BigDecimal.ONE);
        } else if (INCREMENT.equals(operator)) {
            BigDecimal operand = getBigDecimal(operands);
            return operand.add(BigDecimal.ONE);
        } else if (LOGICAL_AND_1.equals(operator) || LOGICAL_AND_2.equals(operator)) {
            Boolean left = getBoolean(operands);
            Boolean right = getBoolean(operands);
            return left && right;
        } else if (LOGICAL_OR_1.equals(operator) || LOGICAL_OR_2.equals(operator)) {
            Boolean left = getBoolean(operands);
            Boolean right = getBoolean(operands);
            return left || right;
        } else if (LOGICAL_NOT_1.equals(operator) || LOGICAL_NOT_2.equals(operator)) {
            Boolean operand = getBoolean(operands);
            return !operand;
        } else if (RIGHT_SHIFT.equals(operator)) {
            BigInteger left = getBigDecimal(operands).toBigInteger();
            BigInteger right = getBigDecimal(operands).toBigInteger();
            return left.shiftRight(right.intValue());
        } else if (LEFT_SHIFT.equals(operator)) {
            BigInteger left = getBigDecimal(operands).toBigInteger();
            BigInteger right = getBigDecimal(operands).toBigInteger();
            return left.shiftLeft(right.intValue());
        } else if (RELATIONAL_SMALLER.equals(operator)) {
            BigDecimal left = getBigDecimal(operands);
            BigDecimal right = getBigDecimal(operands);
            return left.compareTo(right) < 0;
        } else if (RELATIONAL_SMALLER_OR_EQUAL.equals(operator)) {
            BigDecimal left = getBigDecimal(operands);
            BigDecimal right = getBigDecimal(operands);
            return left.compareTo(right) <= 0;
        } else if (RELATIONAL_GREATER.equals(operator)) {
            BigDecimal left = getBigDecimal(operands);
            BigDecimal right = getBigDecimal(operands);
            return left.compareTo(right) > 0;
        } else if (RELATIONAL_GREATER_OR_EQUAL.equals(operator)) {
            BigDecimal left = getBigDecimal(operands);
            BigDecimal right = getBigDecimal(operands);
            return left.compareTo(right) >= 0;
        } else if (EQUALITY.equals(operator)) {
            BigDecimal left = getBigDecimal(operands);
            BigDecimal right = getBigDecimal(operands);
            return left.compareTo(right) == 0;
        } else if (INEQUALITY.equals(operator)) {
            BigDecimal left = getBigDecimal(operands);
            BigDecimal right = getBigDecimal(operands);
            return left.compareTo(right) != 0;
        } else if (BITWISE_AND.equals(operator)) {
            BigInteger left = getBigDecimal(operands).toBigInteger();
            BigInteger right = getBigDecimal(operands).toBigInteger();
            return left.and(right);
        } else if (BITWISE_OR.equals(operator)) {
            BigInteger left = getBigDecimal(operands).toBigInteger();
            BigInteger right = getBigDecimal(operands).toBigInteger();
            return left.or(right);
        } else if (LOGICAL_IMPLICATION.equals(operator)) {
            Boolean left = getBoolean(operands);
            Boolean right = getBoolean(operands);
            return !left || right;
        } else if (LOGICAL_EQUIVALENCE.equals(operator)) {
            Boolean left = getBoolean(operands);
            Boolean right = getBoolean(operands);
            return left == right;
        } else {
            return super.evaluate(operator, operands, evaluationContext);
        }
    }

    private BigDecimal getBigDecimal(Iterator<Object> arguments) {
        BigDecimal value = getBigDecimal(arguments.next());
        if (value == null) {
            return BigDecimal.valueOf(-1);
        } else {
            return value;
        }
    }

    private BigDecimal getBigDecimal(Object argument) {
        if (argument instanceof BigDecimal) {
            return (BigDecimal) argument;
        } else if (argument instanceof BigInteger) {
            return new BigDecimal((BigInteger) argument);
        } else if (argument instanceof Double) {
            return BigDecimal.valueOf((Double) argument);
        } else if (argument instanceof Integer) {
            return BigDecimal.valueOf((Integer) argument);
        } else if (argument instanceof String) {
            String str = (String) argument;
            if (str.startsWith("0b")) {
                return new BigDecimal(new BigInteger(str.substring(2), 2));
            } else if (str.startsWith("0x")) {
                return new BigDecimal(new BigInteger(str.substring(2), 16));
            }
        }
        return null;
    }

    private Boolean getBoolean(Iterator<Object> arguments) {
        Object argument = arguments.next();
        if (argument instanceof Boolean) {
            return (Boolean) argument;
        }
        return false;
    }

    private final Map<String, String> ESCAPE_CHARACTERS = new TreeMap<>(
            Comparator.comparingInt(String::length).reversed().thenComparing(String::compareTo));
    private final RandomString RANDOM_STRING_GENERATOR_ESCAPE_CHARACTERS = new RandomString(8, 596865);

    {
        for (Function function : FUNCTIONS) {
            addEscapeCharacters(function.getName());
        }
        for (Operator operator : OPERATORS) {
            addEscapeCharacters(operator.getSymbol());
        }
        addEscapeCharacters("(");
        addEscapeCharacters(")");
        addEscapeCharacters(",");
    }

    private void addEscapeCharacters(String s) {
        ESCAPE_CHARACTERS.put(s, RANDOM_STRING_GENERATOR_ESCAPE_CHARACTERS.nextString());
    }

    public String escapeExpression(String unescaped) {
        for (Map.Entry<String, String> escape : ESCAPE_CHARACTERS.entrySet()) {
            unescaped = unescaped.replace(escape.getKey(), escape.getValue());
        }
        return unescaped;
    }

    public String escapeFunctionFunctions(String unescaped) {
        for (Function functionFunction : FUNCTION_FUNCTIONS) {
            Matcher m = Pattern.compile(functionFunction.getName() + "\\(([^,]+) *,").matcher(unescaped);
            while (m.find()) {
                String functionName = m.group(1);
                String escapedFunctionName = escapeExpression(functionName);
                unescaped = unescaped.replace(functionFunction.getName() + "(" + functionName, functionFunction.getName() + "(" + escapedFunctionName);
            }
        }
        return unescaped;
    }

    public String escapeSets(String unescaped) {
        if (unescaped.contains("{")) {
            Matcher sets = MultiTypeEvaluator.SET_PATTERN.matcher(unescaped);
            while (sets.find()) {
                unescaped = unescaped.replace(
                        "{" + sets.group(1) + "}",
                        "{" + escapeExpression(sets.group(1)) + "}");
            }
        }
        return unescaped;
    }

    public String unescapeExpression(String escaped) {
        for (Map.Entry<String, String> escape : ESCAPE_CHARACTERS.entrySet()) {
            escaped = escaped.replace(escape.getValue(), escape.getKey());
        }
        return escaped;
    }

    private static Parameters DEFAULT_PARAMETERS = null;
}
