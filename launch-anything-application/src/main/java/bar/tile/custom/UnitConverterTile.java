package bar.tile.custom;

import bar.tile.Tile;
import bar.tile.action.TileActionCopy;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class UnitConverterTile implements RuntimeTile {

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        if (search.length() >= 2) {
            search = search.trim().toLowerCase();
            try {
                if (search.length() >= 6 && search.contains(" to ") || search.contains(" in ")) {
                    String[] parts = search.split(" (to|in) ");
                    if (parts.length == 2) {
                        String input = parts[0];
                        String outputUnit = parts[1];
                        if (input.length() > 0 && outputUnit.length() > 0) {
                            String inputUnit = input.replaceAll("^[\\d.]+", "");
                            double inputValue = Double.parseDouble(input.replaceAll("([^\\d.]+).*$", ""));
                            List<Tile> tiles = new ArrayList<>();
                            for (Converter inputConverter : getConverters(inputUnit)) {
                                for (Converter outputConverter : getConverters(outputUnit)) {
                                    if (inputConverter.getClass() == outputConverter.getClass()) {
                                        Tile tile = createTile(inputConverter, outputConverter, inputValue);
                                        if (tile != null) tiles.add(tile);
                                    }
                                }
                            }
                            return tiles;
                        }
                    }
                } else {
                    if (search.matches("^[\\d.]+([^\\d.]+).*$")) {
                        String inputUnit = search.replaceAll("^[\\d.]+", "");
                        double inputValue = Double.parseDouble(search.replaceAll("([^\\d.]+).*$", ""));
                        List<Tile> tiles = new ArrayList<>();
                        for (Converter inputConverter : getConverters(inputUnit)) {
                            for (Converter outputConverter : getConverters(inputConverter)) {
                                Tile tile = createTile(inputConverter, outputConverter, inputValue);
                                if (tile != null) tiles.add(tile);
                            }
                        }
                        return tiles;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return Collections.emptyList();
    }

    private Tile createTile(Converter input, Converter output, double inputValue) {
        if (input != null && output != null) {
            String outputValue = formatOutputNumber(input.convert(output, inputValue));
            Tile tile = new Tile();
            tile.setCategory("runtime");
            String outputString = outputValue + " " + output.toString(outputValue.equals("1")) + " (from " + input.toString(inputValue == 1) + ")";
            tile.setLabel("= " + outputString);
            tile.addAction(new TileActionCopy(outputValue));
            return tile;
        }
        return null;
    }

    private String formatOutputNumber(double number) {
        return BigDecimal.valueOf(number).toPlainString().replaceAll("\\.0+$", "").replaceAll("(\\d+\\..+?)0+$", "$1").replaceAll("(\\d+\\..+?[1-9]+)0{6,}\\d$", "$1");
    }

    private interface Converter {
        double getFactor();

        String[] getNames();

        default double convert(Converter output, double input) {
            return input * output.getFactor() / getFactor();
        }

        default String toString(boolean singular) {
            if (!singular && getNames().length >= 2) return getNames()[1];
            if (getNames().length >= 1) return getNames()[0];
            return "unnamed";
        }
    }

    private enum LengthConverters implements Converter {
        METER(1, "meter", "meters", "m"),
        KILOMETER(0.001, "kilometer", "kilometers", "km"),
        CENTIMETER(100, "centimeter", "centimeters", "cm"),
        MILLIMETER(1000, "millimeter", "millimeters", "mm"),
        MICROMETER(1000000, "micrometer", "micrometers", "micron"),
        NANOMETER(1000000000, "nanometer", "nanometers", "nm"),
        DECIMETER(10, "decimeter", "decimeters", "dm"),
        DECAMETER(0.1, "decameter", "decameters", "dam"),
        HECTOMETER(0.01, "hectometer", "hectometers", "hm"),
        INCH(39.37007874, "inch", "inches", "in"),
        FOOT(3.280839895, "foot", "feet", "ft"),
        YARD(1.0936132983, "yard", "yards", "yd"),
        MILE(0.000621371, "mile", "miles", "mi"),
        LIGHT_YEAR(1.057008707E-16, "light", "light year", "light years", "ly");

        private final String[] names;
        private final double factor;

        LengthConverters(double factor, String... names) {
            this.names = names;
            this.factor = factor;
        }

        @Override
        public double getFactor() {
            return factor;
        }

        @Override
        public String[] getNames() {
            return names;
        }
    }

    private enum AreaConverters implements Converter {
        SQUARE_METER(1, "square meter", "square meters", "m2", "sm"),
        HECTARE(0.0001, "hectare", "hectares", "ha"),
        ACRE(0.000247105, "acre", "acres", "ac"),
        SQUARE_FOOT(10.7639104, "square foot", "square feet", "ft2", "sft"),
        SQUARE_INCH(1550.0031, "square inch", "square inches", "in2", "sin"),
        SQUARE_YARD(1.195990046, "square yard", "square yards", "yd2", "syd"),
        SQUARE_MILE(3.86102158E-7, "square mile", "square miles", "mi2", "smi"),
        SQUARE_KILOMETER(0.000001, "square kilometer", "square kilometers", "km2", "skm");

        private final String[] names;
        private final double factor;

        AreaConverters(double factor, String... names) {
            this.names = names;
            this.factor = factor;
        }

        @Override
        public double getFactor() {
            return factor;
        }

        @Override
        public String[] getNames() {
            return names;
        }
    }

    private enum VolumeConverters implements Converter {
        CUBIC_KILOMETER(1E-9, "cubic kilometer", "cubic kilometers", "km3", "ckm"),
        CUBIC_METER(1, "cubic meter", "cubic meters", "m3"),
        CUBIC_CENTIMETER(1000000, "cubic centimeter", "cubic centimeters", "cm3"),
        CUBIC_MILLIMETER(1000000000, "cubic millimeter", "cubic millimeters", "mm3"),
        CUBIC_INCH(61023.7441, "cubic inch", "cubic inches", "in3", "ci"),
        CUBIC_FOOT(35.314666721, "cubic foot", "cubic feet", "ft3", "cf"),
        CUBIC_YARD(1.3079506193, "cubic yard", "cubic yards", "yd3", "cyd"),
        CUBIC_MILE(2.399128636E-10, "cubic mile", "cubic miles", "mi3", "cmi"),
        LITER(1000, "liter", "liters", "l"),
        MILLILITER(1000000, "milliliter", "milliliters", "ml", "mil"),
        MICROLITER(1000000000, "microliter", "microliters", "µl", "micl"),
        US_GALLON(264.17217686, "us gallon", "us gallons", "us gal"),
        US_QUART(1056.6887074, "quart", "us quarts", "us qt"),
        US_PINT(2113.3774149, "us pint", "us pints", "us pt"),
        US_CUP(4226.7548297, "us cup", "us cups", "us cp"),
        US_FLUID_OUNCE(33814.038638, "us fluid ounce", "us fluid ounces", "us fl oz"),
        US_TABLESPOON(67628.077276, "us tablespoon", "us tablespoons", "us tbsp", "us tbs"),
        US_TEASPOON(202884.23183, "us teaspoon", "us teaspoons", "us tsp"),
        IMP_GALLON(219.9692483, "imperial gallon", "imperial gallons", "imp gal"),
        IMP_QUART(879.8769932, "imperial quart", "imperial quarts", "imp qt"),
        IMP_PINT(1759.7539864, "imperial pint", "imperial pints", "imp pt"),
        IMP_CUP(3519.51, "imperial cup", "imperial cups", "imp cp"),
        IMP_FLUID_OUNCE(35195.079728, "imperial fluid ounce", "imperial fluid ounces", "imp fl oz"),
        IMP_TABLESPOON(56312.127565, "imperial tablespoon", "imperial tablespoons", "imp tbsp", "imp tbs"),
        IMP_TEASPOON(168936.38269, "imperial teaspoon", "imperial teaspoons", "imp tsp");

        private final String[] names;
        private final double factor;

        VolumeConverters(double factor, String... names) {
            this.names = names;
            this.factor = factor;
        }

        @Override
        public double getFactor() {
            return factor;
        }

        @Override
        public String[] getNames() {
            return names;
        }
    }

    private enum MassConverters implements Converter {
        KILOGRAM(1, "kilogram", "kilograms", "kg"),
        GRAM(1000, "gram", "grams", "g"),
        MILLIGRAM(1000000, "milligram", "milligrams", "mg"),
        MICROGRAM(1000000000, "microgram", "micrograms", "µg"),
        POUND(2.2046244202, "pound", "pounds", "lb"),
        OUNCE(35.273990723, "ounce", "ounces", "oz"),
        CARAT(5000, "carat", "carats", "ct"),
        TONNE(0.001, "tonne", "tonnes", "t"),
        TON(0.001, "metric ton", "metric tons", "ton", "tons", "tn"),
        LONG_TON(0.0009842073, "long ton", "long tons", "ltn"),
        SHORT_TON(0.0011023122, "short ton", "short tons", "stn"),
        STONE(0.157473, "stone", "stones", "st"),
        CUP(0.2365882365, "cup", "cups", "cp");

        private final String[] names;
        private final double factor;

        MassConverters(double factor, String... names) {
            this.names = names;
            this.factor = factor;
        }

        @Override
        public double getFactor() {
            return factor;
        }

        @Override
        public String[] getNames() {
            return names;
        }
    }

    private enum TimeConverters implements Converter {
        MINUTE(1, "minute", "minutes", "min"),
        SECOND(60, "second", "seconds", "s"),
        HOUR(0.0166666667, "hour", "hours", "h"),
        DAY(0.0006944444, "day", "days", "d"),
        WEEK(0.0000992063, "week", "weeks", "wk"),
        MONTH(0.0000228154, "month", "months", "mo"),
        YEAR(0.0000019013, "year", "years", "yr"),
        DECADE(1.9026e-7, "decade", "decades", "dec"),
        CENTURY(1.9026e-8, "century", "centuries", "cen"),
        MILLISECOND(60000, "millisecond", "milliseconds", "ms"),
        MICROSECOND(60000000, "microsecond", "microseconds", "µs"),
        NANOSECOND(60000000000.0, "nanosecond", "nanoseconds", "ns"),
        PICOSECOND(60000000000000.0, "picosecond", "picoseconds", "ps");

        private final String[] names;
        private final double factor;

        TimeConverters(double factor, String... names) {
            this.names = names;
            this.factor = factor;
        }

        @Override
        public double getFactor() {
            return factor;
        }

        @Override
        public String[] getNames() {
            return names;
        }
    }

    private enum EnergyConverters implements Converter {
        JOULE(1, "joule", "joules", "j"),
        KILOJOULE(0.001, "kilojoule", "kilojoules", "kJ"),
        CALORIE(0.239006, "calorie", "calories", "cal"),
        KILOCALORIE(0.000239006, "kilocalorie", "kilocalories", "kcal"),
        BTU(0.0009478181667034029376, "british thermal unit", "british thermal units", "btu", "btus"),
        WATT_HOUR(0.00027777808444444, "watt hour", "watt hours", "wh"),
        KILOWATT_HOUR(2.777780844444400151e-7, "kilowatt hour", "kilowatt hours", "kwh"),
        ELECTRON_VOLT(6241516233886395392.0, "electron volt", "electron volts", "ev"),
        US_THERM(9.480444746132779938e-9, "us thermal unit", "us thermal units", "therm"),
        FOOT_POUND(0.73756296354586647901, "foot pound", "foot pounds", "ft lb");

        private final String[] names;
        private final double factor;

        EnergyConverters(double factor, String... names) {
            this.names = names;
            this.factor = factor;
        }

        @Override
        public double getFactor() {
            return factor;
        }

        @Override
        public String[] getNames() {
            return names;
        }
    }

    private enum SpeedConverters implements Converter {
        METER_PER_SECOND(1, "meter per second", "meters per second", "m/s", "mps", "m/sec", "mpsec"),
        METER_PER_MINUTE(60, "meter per minute", "meters per minute", "m/min", "m/min", "mpm"),
        METER_PER_HOUR(3600, "meter per hour", "meters per hour", "m/h", "mph", "m/hr", "mphr"),
        KILOMETER_PER_SECOND(0.001, "kilometer per second", "kilometers per second", "km/s", "kms", "km/sec", "kmpsec"),
        KILOMETER_PER_MINUTE(0.06, "kilometer per minute", "kilometers per minute", "km/min", "kmpm"),
        KILOMETER_PER_HOUR(3.6, "kilometer per hour", "kilometers per hour", "km/h", "kmph", "km/hr", "kmphr"),
        MILE_PER_HOUR(2.2369362921, "mile per hour", "miles per hour", "mph", "mpm", "m/hr", "mphr"),
        MILE_PER_MINUTE(0.0372822715, "mile per minute", "miles per minute", "mpm", "m/min", "mphr"),
        MILE_PER_SECOND(0.0006213712, "mile per second", "miles per second", "mps", "mpsec", "m/sec", "mphr"),
        FOOT_PER_SECOND(3.280839895, "foot per second", "feet per second", "ft/s", "fps", "ft/sec", "ftsec"),
        FOOT_PER_MINUTE(196.8503937, "foot per minute", "feet per minute", "ft/min", "ft/min", "ftpm"),
        FOOT_PER_HOUR(11811.023622, "foot per hour", "feet per hour", "ft/h", "ftph", "ft/hr", "ftphr"),
        KNOT(1.9438444924, "knot", "knots", "kn"),
        MACH(0.0033892974, "mach", "machs"),
        LIGHT_SPEED(3.335640951E-9, "speed of light", "speed of light", "c", "light speed", "light speed");

        private final String[] names;
        private final double factor;

        SpeedConverters(double factor, String... names) {
            this.names = names;
            this.factor = factor;
        }

        @Override
        public double getFactor() {
            return factor;
        }

        @Override
        public String[] getNames() {
            return names;
        }
    }

    private enum TemperatureConverters implements Converter {
        KELVIN("kelvin", "kelvins", "K"),
        CELSIUS("celsius", "celsius", "C"),
        FAHRENHEIT("fahrenheit", "fahrenheit", "F");

        private final String[] names;

        TemperatureConverters(String... names) {
            this.names = names;
        }

        public double convert(Converter output, double input) {
            double inCelsius;
            if (this == KELVIN) inCelsius = input - 273.15;
            else if (this == CELSIUS) inCelsius = input;
            else inCelsius = (input - 32) / 1.8;
            if (output == CELSIUS) return inCelsius;
            else if (output == KELVIN) return inCelsius + 273.15;
            else return inCelsius * 9 / 5 + 32;
        }

        @Override
        public double getFactor() {
            return 1.0;
        }

        @Override
        public String[] getNames() {
            return names;
        }
    }

    private static List<Converter> getConverters(String name) {
        name = name.trim().toLowerCase();
        List<Converter> converters = new ArrayList<>();
        converters.add(getConverter(LengthConverters.values(), name));
        converters.add(getConverter(AreaConverters.values(), name));
        converters.add(getConverter(VolumeConverters.values(), name));
        converters.add(getConverter(MassConverters.values(), name));
        converters.add(getConverter(TimeConverters.values(), name));
        converters.add(getConverter(EnergyConverters.values(), name));
        converters.add(getConverter(SpeedConverters.values(), name));
        converters.add(getConverter(TemperatureConverters.values(), name));
        return converters.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static List<Converter> getConverters(Converter converter) {
        List<Converter> converters = new ArrayList<>();
        if (converter.getClass() == LengthConverters.class) converters.addAll(Arrays.asList(LengthConverters.values()));
        else if (converter.getClass() == AreaConverters.class)
            converters.addAll(Arrays.asList(AreaConverters.values()));
        else if (converter.getClass() == VolumeConverters.class)
            converters.addAll(Arrays.asList(VolumeConverters.values()));
        else if (converter.getClass() == MassConverters.class)
            converters.addAll(Arrays.asList(MassConverters.values()));
        else if (converter.getClass() == TimeConverters.class)
            converters.addAll(Arrays.asList(TimeConverters.values()));
        else if (converter.getClass() == EnergyConverters.class)
            converters.addAll(Arrays.asList(EnergyConverters.values()));
        else if (converter.getClass() == SpeedConverters.class)
            converters.addAll(Arrays.asList(SpeedConverters.values()));
        else if (converter.getClass() == TemperatureConverters.class)
            converters.addAll(Arrays.asList(TemperatureConverters.values()));
        converters.remove(converter);
        return converters.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static Converter getConverter(Converter[] converters, String name) {
        for (Converter converter : converters) {
            for (String n : converter.getNames()) {
                if (n.equalsIgnoreCase(name)) {
                    return converter;
                }
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "Unit Converter";
    }

    @Override
    public String getDescription() {
        return "Enter '[number] [unit] to [unit]' or leave away the second unit";
    }

    @Override
    public String getAuthor() {
        return "Yan Wittmann";
    }

    @Override
    public String getVersion() {
        return null;
    }
}
