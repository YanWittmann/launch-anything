package bar.util.evaluator;

import bar.util.RandomString;
import com.fathzer.soft.javaluator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
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

    @Override
    protected Object toValue(String literal, Object evaluationContext) {
        literal = literal.trim().toLowerCase();
        if (literal.equals("true") || literal.equals("t")) {
            return true;
        } else if (literal.equals("false") || literal.equals("f")) {
            return false;
        } else if (literal.startsWith("0x")) {
            return new BigInteger(literal.substring(2), 16);
        } else if (literal.startsWith("0b")) {
            return new BigInteger(literal.substring(2), 2);
        } else if (literal.startsWith("0o")) {
            return new BigInteger(literal.substring(2), 8);
        } else if (literal.startsWith("0d")) {
            return new BigDecimal(literal.substring(2));
        }
        try {
            return new BigDecimal(literal);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Cannot parse '" + literal + "' as number");
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
    public static final Function LIST = new Function("list", 1, Integer.MAX_VALUE);
    public static final Function SET = new Function("set", 1, Integer.MAX_VALUE);
    public static final Function DISTINCT = new Function("distinct", 1, Integer.MAX_VALUE);
    public static final Function GET_ELEMENT = new Function("elementAt", 1, Integer.MAX_VALUE);
    public static final Function RANGE = new Function("range", 1, 2);
    public static final Function NORMALIZE = new Function("normalize", 1);

    private static final Function[] FUNCTIONS = new Function[]{SINE, COSINE, TANGENT, ASINE, ACOSINE, ATAN, SINEH,
            COSINEH, TANGENTH, MIN, MAX, SUM, AVERAGE, PRODUCT, COUNT_DEEP, COUNT_SHALLOW, LN, LOG, ROUND, CEIL, FLOOR,
            ABS, RANDOM, GGT, GCD, PHI, IS_PRIME, NEXT_PRIME, IF_ELSE, TO_BINARY_STRING, TO_HEX_STRING, POW, SQRT, ROOT,
            SUM_OF_DIGITS, FACULTY, FACTORIZE, DIVISORS, GROUP_DUPLICATES, SORT, MERGE, LIST, SET, DISTINCT, GET_ELEMENT,
            RANGE, NORMALIZE};

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
            List<Object> allArgumentsAsList = getAllArgumentsAsList(arguments);
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
            List<Object> allArgumentsAsList = getAllArgumentsAsList(arguments);
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
            List<Object> allArgumentsAsList = getAllArgumentsAsList(arguments);
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
            List<Object> allArgumentsAsList = getAllArgumentsAsList(arguments);
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
            List<Object> allArgumentsAsList = getAllArgumentsAsList(arguments);
            for (Object next : allArgumentsAsList) {
                BigDecimal value = getBigDecimal(next);
                if (value != null) {
                    product = product.multiply(value);
                }
            }
            return product;
        } else if (COUNT_DEEP.equals(function)) {
            return getAllArgumentsAsList(arguments).size();
        } else if (COUNT_SHALLOW.equals(function)) {
            int count = 0;
            while (arguments.hasNext()) {
                arguments.next();
                count++;
            }
            return count;
        } else if (GROUP_DUPLICATES.equals(function)) {
            Map<Object, BigDecimal> counts = new LinkedHashMap<>();
            List<Object> allArgumentsAsList = getAllArgumentsAsList(arguments);
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
            List<Object> argumentsList = new ArrayList<>();
            while (arguments.hasNext()) {
                argumentsList.add(arguments.next());
            }
            if (argumentsList.size() == 1 && argumentsList.get(0) instanceof List) {
                argumentsList = (List<Object>) argumentsList.get(0);
            }
            return argumentsList.stream().sorted(OBJECT_COMPARATOR).collect(Collectors.toList());
        } else if (MERGE.equals(function)) {
            List<Object> argumentsList = new ArrayList<>();
            while (arguments.hasNext()) {
                argumentsList.add(arguments.next());
            }
            List<Object> merged = new ArrayList<>();
            for (Object next : argumentsList) {
                if (next instanceof List) {
                    merged.addAll((List<Object>) next);
                } else {
                    merged.add(next);
                }
            }
            return merged;
        } else if (SET.equals(function)) {
            Set<Object> argumentsSet = new LinkedHashSet<>();
            while (arguments.hasNext()) {
                argumentsSet.add(arguments.next());
            }
            return new ArrayList<>(argumentsSet);
        } else if (LIST.equals(function)) {
            List<Object> argumentsList = new ArrayList<>();
            while (arguments.hasNext()) {
                argumentsList.add(arguments.next());
            }
            return argumentsList;
        } else if (DISTINCT.equals(function)) {
            List<Object> argumentsList = new ArrayList<>();
            while (arguments.hasNext()) {
                argumentsList.add(arguments.next());
            }
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
            List<Object> allElements = getAllArgumentsAsList(arguments);
            return allElements.get(index.intValue());
        } else if (RANGE.equals(function)) {
            return IntStream.rangeClosed(getBigDecimal(arguments).intValue(), getBigDecimal(arguments).intValue()).boxed().collect(Collectors.toList());
        } else if (NORMALIZE.equals(function)) {
            List<Object> allArgumentsAsList = getAllArgumentsAsList(arguments);
            BigDecimal sum = BigDecimal.ZERO;
            for (Object element : allArgumentsAsList) {
                sum = sum.add(getBigDecimal(element));
            }
            List<Object> normalizedElements = new ArrayList<>();
            for (Object element : allArgumentsAsList) {
                normalizedElements.add(getBigDecimal(element).divide(sum, DOUBLE_SCALE, RoundingMode.DOWN));
            }
            return normalizedElements;
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

    private List<Object> getAllArgumentsAsList(Iterator<Object> arguments) {
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
            Object operand = operands.next();
            if (operand instanceof Number) {
                return getBigDecimal(operand).negate();
            } else if (operand instanceof Boolean) {
                return !(Boolean) operand;
            } else if (operand instanceof Collection) {
                Collection<Object> collection = (Collection<Object>) operand;
                List<Object> result = new ArrayList<>(collection.size());
                for (Object element : collection) {
                    if (element instanceof Number) {
                        element = getBigDecimal(element).negate();
                    } else if (element instanceof Boolean) {
                        element = !(Boolean) element;
                    }
                    result.add(element);
                }
                return result;
            }
            throw new IllegalArgumentException("Cannot negate " + operand);
        } else if (MINUS.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).subtract(getBigDecimal(rightValue)));
        } else if (PLUS.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).add(getBigDecimal(rightValue)));
        } else if (MULTIPLY.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).multiply(getBigDecimal(rightValue)));
        } else if (DIVIDE.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).divide(getBigDecimal(rightValue), DOUBLE_SCALE, RoundingMode.HALF_EVEN));
        } else if (EXPONENT.equals(operator) || EXPONENT_DOUBLE.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).pow(getBigDecimal(rightValue).intValue()));
        } else if (MODULO.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).remainder(getBigDecimal(rightValue)));
        } else if (DECREMENT.equals(operator)) {
            Object operand = operands.next();
            return performUnaryOperationOnValue(operand, operandValue -> getBigDecimal(operandValue).subtract(BigDecimal.ONE));
        } else if (INCREMENT.equals(operator)) {
            Object operand = operands.next();
            return performUnaryOperationOnValue(operand, operandValue -> getBigDecimal(operandValue).add(BigDecimal.ONE));
        } else if (LOGICAL_AND_1.equals(operator) || LOGICAL_AND_2.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBoolean(leftValue) && getBoolean(rightValue));
        } else if (LOGICAL_OR_1.equals(operator) || LOGICAL_OR_2.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBoolean(leftValue) || getBoolean(rightValue));
        } else if (LOGICAL_NOT_1.equals(operator) || LOGICAL_NOT_2.equals(operator)) {
            Object operand = operands.next();
            return performUnaryOperationOnValue(operand, operandValue -> !getBoolean(operandValue));
        } else if (RIGHT_SHIFT.equals(operator)) {
            BigInteger left = getBigDecimal(operands).toBigInteger();
            BigInteger right = getBigDecimal(operands).toBigInteger();
            return left.shiftRight(right.intValue());
        } else if (LEFT_SHIFT.equals(operator)) {
            BigInteger left = getBigDecimal(operands).toBigInteger();
            BigInteger right = getBigDecimal(operands).toBigInteger();
            return left.shiftLeft(right.intValue());
        } else if (RELATIONAL_SMALLER.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).compareTo(getBigDecimal(rightValue)) < 0);
        } else if (RELATIONAL_SMALLER_OR_EQUAL.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).compareTo(getBigDecimal(rightValue)) <= 0);
        } else if (RELATIONAL_GREATER.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).compareTo(getBigDecimal(rightValue)) > 0);
        } else if (RELATIONAL_GREATER_OR_EQUAL.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).compareTo(getBigDecimal(rightValue)) >= 0);
        } else if (EQUALITY.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).compareTo(getBigDecimal(rightValue)) == 0);
        } else if (INEQUALITY.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).compareTo(getBigDecimal(rightValue)) != 0);
        } else if (BITWISE_AND.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).toBigInteger().and(getBigDecimal(rightValue).toBigInteger()));
        } else if (BITWISE_OR.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBigDecimal(leftValue).toBigInteger().or(getBigDecimal(rightValue).toBigInteger()));
        } else if (LOGICAL_IMPLICATION.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> !getBoolean(leftValue) || getBoolean(rightValue));
        } else if (LOGICAL_EQUIVALENCE.equals(operator)) {
            Object left = operands.next();
            Object right = operands.next();
            return performBinaryOperationOnValueAndList(left, right, (leftValue, rightValue) -> getBoolean(leftValue) == getBoolean(rightValue));
        } else {
            return super.evaluate(operator, operands, evaluationContext);
        }
    }

    private Object performBinaryOperationOnValueAndList(Object left, Object right, BinaryCalculation binaryCalculation) {
        boolean leftIsValue = isDecimalOrBoolean(left);
        boolean rightIsValue = isDecimalOrBoolean(right);
        if (leftIsValue && rightIsValue) {
            return binaryCalculation.calculate(left, right);
        } else if (left instanceof Collection && rightIsValue) {
            Collection<Object> collection = (Collection<Object>) left;
            List<Object> result = new ArrayList<>(collection.size());
            for (Object element : collection) {
                result.add(binaryCalculation.calculate(element, right));
            }
            return result;
        } else if (leftIsValue && right instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) right;
            List<Object> result = new ArrayList<>(collection.size());
            for (Object element : collection) {
                result.add(binaryCalculation.calculate(left, element));
            }
            return result;
        } else if (left instanceof Collection && right instanceof Collection) {
            Collection<Object> leftCollection = (Collection<Object>) left;
            Collection<Object> rightCollection = (Collection<Object>) right;
            List<Object> result = new ArrayList<>(leftCollection.size());
            Iterator<Object> leftIterator = leftCollection.iterator();
            Iterator<Object> rightIterator = rightCollection.iterator();
            while (leftIterator.hasNext() && rightIterator.hasNext()) {
                Object leftElement = leftIterator.next();
                Object rightElement = rightIterator.next();
                result.add(binaryCalculation.calculate(leftElement, rightElement));
            }
            return result;
        }
        throw new IllegalArgumentException("Cannot perform binary operation on " + left + " and " + right);
    }

    private Object performUnaryOperationOnValue(Object operand, UnaryCalculation unaryCalculation) {
        if (isDecimalOrBoolean(operand)) {
            return unaryCalculation.calculate(operand);
        } else if (operand instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) operand;
            List<Object> result = new ArrayList<>(collection.size());
            for (Object element : collection) {
                result.add(unaryCalculation.calculate(element));
            }
            return result;
        }
        throw new IllegalArgumentException("Cannot perform unary operation on " + operand);
    }

    private boolean isDecimalOrBoolean(Object operand) {
        return operand instanceof Number || operand instanceof Boolean;
    }

    private interface BinaryCalculation {
        Object calculate(Object left, Object right);
    }

    private interface UnaryCalculation {
        Object calculate(Object operand);
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
        return getBoolean(argument);
    }

    private Boolean getBoolean(Object argument) {
        if (argument instanceof Boolean) {
            return (Boolean) argument;
        }
        return false;
    }

    private final static Map<String, String> ESCAPE_CHARACTERS = new LinkedHashMap<>();

    static {
        RandomString randomString = new RandomString(8, 596865);
        // sort by length, so that longer strings come first
        for (Function function : Arrays.stream(FUNCTIONS).sorted((o1, o2) -> Integer.compare(o2.getName().length(), o1.getName().length())).collect(Collectors.toList())) {
            ESCAPE_CHARACTERS.put(function.getName(), randomString.nextString());
        }
        for (Operator operator : Arrays.stream(OPERATORS).sorted((o1, o2) -> Integer.compare(o2.getSymbol().length(), o1.getSymbol().length())).collect(Collectors.toList())) {
            ESCAPE_CHARACTERS.put(operator.getSymbol(), randomString.nextString());
        }
        ESCAPE_CHARACTERS.put("(", randomString.nextString());
        ESCAPE_CHARACTERS.put(")", randomString.nextString());
        ESCAPE_CHARACTERS.put(",", randomString.nextString());
    }

    public String escapeExpression(String unescaped) {
        for (Map.Entry<String, String> escape : ESCAPE_CHARACTERS.entrySet()) {
            unescaped = unescaped.replace(escape.getKey(), escape.getValue());
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
