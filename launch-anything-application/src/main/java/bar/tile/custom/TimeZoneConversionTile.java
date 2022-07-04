package bar.tile.custom;

import bar.tile.Tile;
import bar.tile.action.TileAction;
import bar.tile.action.TileActionRuntimeInteraction;
import bar.ui.TrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TimeZoneConversionTile implements RuntimeTile {

    private static final Logger LOG = LoggerFactory.getLogger(TimeZoneConversionTile.class);

    private final static Map<String, Timer> countdownTimers = new HashMap<>();

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        if (search.startsWith("timer")) {
            final String trimmedSearch = search.substring(5).trim();

            if (trimmedSearch.isEmpty()) {
                final List<Tile> timerTiles = new ArrayList<>();
                for (Map.Entry<String, Timer> timerEntry : countdownTimers.entrySet()) {
                    Tile tile = new Tile("Cancel Timer: " + timerEntry.getKey());
                    tile.addAction(new TileActionRuntimeInteraction(() -> {
                        timerEntry.getValue().cancel();
                        countdownTimers.remove(timerEntry.getKey());
                    }));
                    timerTiles.add(tile);
                }
                return timerTiles;
            }

            Calendar parsedTime = parseTime(trimmedSearch, TimeZone.getDefault());
            String formattedTime = formatCalendar(parsedTime);

            Tile timerTile = new Tile("Create Timer: " + formattedTime);
            timerTile.addAction(new TileActionRuntimeInteraction(() -> {
                long diff = parsedTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        LOG.info("Timer expired [{}]", formattedTime);
                        TrayUtil.showMessage("Timer " + formattedTime + " expired");
                        timer.cancel();
                        countdownTimers.remove(formattedTime);
                    }
                }, diff);
                countdownTimers.put(formattedTime, timer);
            }));

            return Collections.singletonList(timerTile);
        }

        TimeZoneConverterUserInputResult convertInput = parseUserInputString(search);

        if (convertInput == null || !convertInput.isValid()) {
            return Collections.emptyList();
        }

        List<Tile> tiles = new ArrayList<>();

        for (TimeZone toTimeZone : convertInput.toTimeZones) {
            Tile tile = createTileForConversion(convertInput.convertTime, convertInput.fromTimeZone, toTimeZone);
            tiles.add(tile);
        }

        return tiles;
    }

    public static Tile createTileForConversion(Calendar convertCalendar, TimeZone fromTimeZone, TimeZone toTimeZone) {
        Calendar convertedTimeCalendar = new GregorianCalendar(toTimeZone);
        convertedTimeCalendar.setTimeInMillis(convertCalendar.getTimeInMillis());

        String label = formatCalendar(convertedTimeCalendar);
        String copyText = formatCalendar(convertedTimeCalendar);

        return createCopyTextTile(label, copyText);
    }

    private static String formatCalendar(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        String timeZone = calendar.getTimeZone().getDisplayName() + ", " + calendar.getTimeZone().getID();

        return String.format("%04d-%02d-%02d %02d:%02d:%02d (%s)", year, month, day, hour, minute, second, timeZone);
    }

    private static Tile createCopyTextTile(String label, String copyText) {
        Tile tile = new Tile(label);
        tile.setCategory("runtime");
        tile.addAction(TileAction.getInstance("copy", copyText));
        return tile;
    }

    private final static String[] TIME_ZONE_CONVERT_TO_PREFIXES = {"into", "in", "to"};
    private final static String[] TIME_ZONE_CONVERT_FROM_PREFIXES = {"from"};
    private final static String[] TIME_ZONE_LOCAL_TIMEZONE_SYNONYMS = {"here", "local timezone", "local time", "local"};

    public static TimeZoneConverterUserInputResult parseUserInputString(String input) {
        input = input.trim()
                .toLowerCase()
                .replaceAll(" +", " ");

        if (input.length() == 0) {
            return null;
        }

        StringJoiner splitters = new StringJoiner("| ", "(", ")");
        Arrays.stream(TIME_ZONE_CONVERT_TO_PREFIXES).forEach(splitters::add);
        Arrays.stream(TIME_ZONE_CONVERT_FROM_PREFIXES).forEach(splitters::add);

        String[] arguments = Arrays.stream(input.split(splitters.toString())).map(String::trim).toArray(String[]::new);

        if (arguments.length <= 1) {
            return null;
        }

        int[] argumentTypes = new int[arguments.length];
        Arrays.fill(argumentTypes, -1);
        argumentTypes[0] = 0;
        String inputWithoutArgFindArgType = input.replace(arguments[0], "").trim();
        inputWithoutArgFindArgType = extractFromArgument(arguments, argumentTypes, inputWithoutArgFindArgType);
        inputWithoutArgFindArgType = extractToArgument(arguments, argumentTypes, inputWithoutArgFindArgType);
        inputWithoutArgFindArgType = extractFromArgument(arguments, argumentTypes, inputWithoutArgFindArgType);
        extractToArgument(arguments, argumentTypes, inputWithoutArgFindArgType);

        TimeZone fromTimeZone = null;
        List<TimeZone> toTimeZones = null;
        for (int i = 0; i < arguments.length; i++) {
            switch (argumentTypes[i]) {
                case 1:
                    fromTimeZone = parseTimeZone(arguments[i]).stream().findFirst().orElse(null);
                    break;
                case 2:
                    toTimeZones = parseTimeZone(arguments[i]);
                    break;
            }
        }

        if ((toTimeZones == null || toTimeZones.isEmpty()) && fromTimeZone != null) {
            toTimeZones = Collections.singletonList(TimeZone.getDefault());
        } else if ((toTimeZones != null && !toTimeZones.isEmpty()) && fromTimeZone == null) {
            fromTimeZone = TimeZone.getDefault();
        } else if (toTimeZones == null || toTimeZones.isEmpty()) {
            return null;
        }

        Calendar convertCalendar = parseTime(arguments[0], fromTimeZone);

        return new TimeZoneConverterUserInputResult(convertCalendar, fromTimeZone, toTimeZones);
    }

    private static String extractFromArgument(String[] arguments, int[] argumentTypes, String inputWithoutArgFindArgType) {
        int nextArgumentIndex = findNextArgumentIndex(argumentTypes);
        if (nextArgumentIndex == -1) return inputWithoutArgFindArgType;
        for (String fromPrefix : TIME_ZONE_CONVERT_FROM_PREFIXES) {
            if (inputWithoutArgFindArgType.startsWith(fromPrefix)) {
                argumentTypes[nextArgumentIndex] = 1;
                inputWithoutArgFindArgType = inputWithoutArgFindArgType.replaceFirst(fromPrefix, "").replace(arguments[1], "").trim();
                break;
            }
        }
        return inputWithoutArgFindArgType;
    }

    private static String extractToArgument(String[] arguments, int[] argumentTypes, String inputWithoutArgFindArgType) {
        int nextArgumentIndex = findNextArgumentIndex(argumentTypes);
        if (nextArgumentIndex == -1) return inputWithoutArgFindArgType;
        for (String fromPrefix : TIME_ZONE_CONVERT_TO_PREFIXES) {
            if (inputWithoutArgFindArgType.startsWith(fromPrefix)) {
                argumentTypes[nextArgumentIndex] = 2;
                inputWithoutArgFindArgType = inputWithoutArgFindArgType.replaceFirst(fromPrefix, "").replace(arguments[1], "").trim();
                break;
            }
        }
        return inputWithoutArgFindArgType;
    }

    private static int findNextArgumentIndex(int[] argumentType) {
        for (int i = 0; i < argumentType.length; i++) {
            if (argumentType[i] == -1) {
                return i;
            }
        }
        return -1;
    }

    public static class TimeZoneConverterUserInputResult {
        public final List<TimeZone> toTimeZones;
        public final TimeZone fromTimeZone;
        public final Calendar convertTime;

        public TimeZoneConverterUserInputResult(Calendar convertTime, TimeZone fromTimeZone, List<TimeZone> toTimeZones) {
            this.convertTime = convertTime;
            this.toTimeZones = toTimeZones;
            this.fromTimeZone = fromTimeZone;
        }

        public boolean isValid() {
            return convertTime != null && fromTimeZone != null && toTimeZones != null && !toTimeZones.isEmpty();
        }

        @Override
        public String toString() {
            return "Convert " + new Date(convertTime.getTimeInMillis()) + " from " + fromTimeZone.getID() + " into " + toTimeZones.stream().map(TimeZone::getID).collect(Collectors.joining(", "));
        }
    }

    public static List<TimeZone> parseTimeZone(String timeZone) {

        for (String local : TIME_ZONE_LOCAL_TIMEZONE_SYNONYMS) {
            if (timeZone.equals(local)) {
                return Collections.singletonList(TimeZone.getDefault());
            }
        }

        if (timeZone.startsWith("+") || timeZone.startsWith("-")) {
            boolean isAddTimezone = timeZone.startsWith("+");
            timeZone = timeZone.replace("+", "").replace("-", "");

            String hours;
            String minutes = null;
            if (timeZone.length() == 4) {
                hours = timeZone.substring(0, 2);
                minutes = timeZone.substring(2, 4);
            } else if (timeZone.length() == 3) {
                hours = timeZone.substring(0, 1);
                minutes = timeZone.substring(1, 3);
            } else if (timeZone.length() == 2 || timeZone.length() == 1) {
                hours = timeZone;
            } else {
                return null;
            }

            int timeZoneOffset = 0;
            timeZoneOffset += Integer.parseInt(hours) * 60 * 60 * 1000;
            if (minutes != null) {
                timeZoneOffset += Integer.parseInt(minutes) * 60 * 1000;
            }
            timeZoneOffset = timeZoneOffset * (isAddTimezone ? 1 : -1);
            String[] availableIDs = TimeZone.getAvailableIDs(timeZoneOffset);
            if (availableIDs.length >= 1) {
                return Collections.singletonList(TimeZone.getTimeZone(availableIDs[0]));
            }
        }

        for (String availableID : TimeZone.getAvailableIDs()) {
            if (availableID.toLowerCase().equals(timeZone)) {
                return Collections.singletonList(TimeZone.getTimeZone(availableID));
            }
        }

        List<TimeZone> zones = new ArrayList<>();

        for (String availableID : TimeZone.getAvailableIDs()) {
            if (availableID.toLowerCase().contains(timeZone)) {
                zones.add(TimeZone.getTimeZone(availableID));
            }
        }

        return zones;
    }

    private final static String[] MONTHS = {
            "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"
    };

    public static Calendar parseTime(String timeString, TimeZone timeZone) {
        Calendar time = new GregorianCalendar(timeZone);

        timeString = timeString.trim()
                .toLowerCase()
                .replace("in ", "")
                .replaceAll("\\s+", " ")
                .replace("\\", "-")
                .replace("/", "-")
                .replace(":", "-")
                .replace(".", "-")
                .replaceAll(" ?- ?", "-")
                .replace(" ago", "ago")
                .replace("after ", "after")
                .replace(" and ", "!and!")
                .replace(", ", "!and!")
                .replaceAll(" (seconds?|minutes?|hours?|milliseconds?|days?|months?|years?|ms|d|s|h|y|m)", "$1")
                .replaceAll("(\\d{1,2}) (" + String.join("|", MONTHS) + ") (\\d{4})", "$1$2$3")
                .replaceAll("(\\d{4}) (" + String.join("|", MONTHS) + ") (\\d{1,2})", "$3$2$1")
                .replaceAll("(\\d{4}) (\\d{1,2}) (" + String.join("|", MONTHS) + ")", "$2$3$1")
                .replaceAll("(" + String.join("|", MONTHS) + ") (\\d{1,2}) (\\d{4})", "$2$1$3");

        String[] majorParts = timeString.split(" ");

        List<String> rearrangedMajorParts = new ArrayList<>();
        boolean rearrangeDelayed = false;
        for (int i = majorParts.length - 1; i >= 0; i--) {
            if (rearrangeDelayed) {
                rearrangeOrderOptionalSplit(majorParts[i + 1], rearrangedMajorParts);
                rearrangeDelayed = false;
            } else if (i > 0 && majorParts[i].startsWith("after")) {
                rearrangeOrderOptionalSplit(majorParts[i - 1], rearrangedMajorParts);
                rearrangeDelayed = true;
            } else {
                rearrangeOrderOptionalSplit(majorParts[i], rearrangedMajorParts);
            }
        }
        majorParts = reverse(rearrangedMajorParts.stream().map(
                e -> e.replace("after", "")
        )).toArray(String[]::new);

        for (int i = 0; i < majorParts.length; i++) {
            String majorPart = majorParts[i];
            if (majorPart.length() == 0) {
                continue;
            }

            if (majorPart.contains("-")) {
                String[] minorParts = majorPart.split("-");
                if (minorParts.length == 3) {
                    if (minorParts[0].length() <= 2 && minorParts[1].length() <= 2 && minorParts[2].length() <= 2) {
                        // HH-MM-SS
                        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(minorParts[0]));
                        time.set(Calendar.MINUTE, Integer.parseInt(minorParts[1]));
                        time.set(Calendar.SECOND, Integer.parseInt(minorParts[2]));
                    } else if (minorParts[0].length() <= 2 && minorParts[1].length() <= 2 && minorParts[2].length() == 4) {
                        // DD-MM-YYYY
                        time.set(Calendar.YEAR, Integer.parseInt(minorParts[2]));
                        time.set(Calendar.MONTH, Integer.parseInt(minorParts[1]) - 1);
                        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(minorParts[0]));
                    } else if (minorParts[0].length() == 4 && minorParts[1].length() <= 2 && minorParts[2].length() <= 2) {
                        // YYYY-MM-DD
                        time.set(Calendar.YEAR, Integer.parseInt(minorParts[0]));
                        time.set(Calendar.MONTH, Integer.parseInt(minorParts[1]));
                        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(minorParts[2]));
                    }
                } else if (minorParts.length == 2) {
                    if (minorParts[0].length() <= 2 && minorParts[1].length() <= 2) {
                        // HH-MM
                        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(minorParts[0]));
                        time.set(Calendar.MINUTE, Integer.parseInt(minorParts[1]));
                        time.set(Calendar.SECOND, 0);
                    }
                }
            } else {
                Pattern individualPartPattern = Pattern.compile("(\\d+)([a-z]+)");
                String trimmedMajorPart = majorPart.replace("ago", "").replaceAll("(.+)s$", "$1");
                Matcher individualPartMatcher = individualPartPattern.matcher(trimmedMajorPart);
                if (individualPartMatcher.matches()) {
                    int number = Integer.parseInt(individualPartMatcher.group(1));
                    String unit = individualPartMatcher.group(2);
                    int timeUnit = -1;
                    switch (unit) {
                        case "s":
                        case "second":
                            timeUnit = Calendar.SECOND;
                            break;
                        case "m":
                        case "minute":
                            timeUnit = Calendar.MINUTE;
                            break;
                        case "h":
                        case "hour":
                            timeUnit = Calendar.HOUR_OF_DAY;
                            break;
                        case "ms":
                        case "millisecond":
                            timeUnit = Calendar.MILLISECOND;
                            break;
                        case "d":
                        case "day":
                            timeUnit = Calendar.DAY_OF_MONTH;
                            break;
                        case "month":
                            timeUnit = Calendar.MONTH;
                            break;
                        case "y":
                        case "year":
                            timeUnit = Calendar.YEAR;
                            break;
                    }
                    if (timeUnit != -1) {
                        if (!majorPart.contains("ago")) {
                            time.add(timeUnit, number);
                        } else {
                            time.add(timeUnit, -number);
                        }
                    }
                } else {

                    if (majorPart.matches("\\d{4}")) {
                        // YYYY
                        time.set(Calendar.YEAR, Integer.parseInt(majorPart));
                    } else if (majorPart.matches("\\d{1,2}(" + String.join("|", MONTHS) + ")\\d{4}")) {
                        // DD-mm-YYYY
                        Pattern monthPattern = Pattern.compile("(\\d{1,2})(" + String.join("|", MONTHS) + ")(\\d{4})");
                        Matcher monthMatcher = monthPattern.matcher(majorPart);
                        if (monthMatcher.matches()) {
                            time.set(Calendar.YEAR, Integer.parseInt(monthMatcher.group(3)));
                            time.set(Calendar.MONTH, Arrays.asList(MONTHS).indexOf(monthMatcher.group(2)) + 1);
                            time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(monthMatcher.group(1)));
                        }
                    } else {
                        switch (majorPart) {
                            case "yesterday":
                                time.add(Calendar.DAY_OF_MONTH, -1);
                                break;
                            case "today":
                                time.add(Calendar.DAY_OF_MONTH, 0);
                                break;
                            case "tomorrow":
                            case "tomorow":
                                time.add(Calendar.DAY_OF_MONTH, 1);
                                break;
                            case "noon":
                                time.set(Calendar.HOUR_OF_DAY, 12);
                                time.set(Calendar.MINUTE, 0);
                                time.set(Calendar.SECOND, 0);
                                break;
                            case "midnight":
                                time.set(Calendar.HOUR_OF_DAY, 0);
                                time.set(Calendar.MINUTE, 0);
                                time.set(Calendar.SECOND, 0);
                                break;
                            case "pm":
                                time.add(Calendar.HOUR_OF_DAY, 12);
                                break;
                        }
                    }
                }
            }
        }

        return time;
    }

    private static void rearrangeOrderOptionalSplit(String element, List<String> rearrangedMajorParts) {
        if (element.contains("!and!")) {
            String[] split = element.split("!and!");
            for (int i = split.length - 1; i >= 0; i--) {
                rearrangedMajorParts.add(split[i]);
            }
        } else {
            rearrangedMajorParts.add(element);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> Stream<T> reverse(Stream<T> input) {
        Object[] temp = input.toArray();
        return (Stream<T>) IntStream.range(0, temp.length)
                .mapToObj(i -> temp[temp.length - i - 1]);
    }

    @Override
    public String getName() {
        return "Time Zone Converter";
    }

    @Override
    public String getDescription() {
        return "Enter 'TIME from TIMEZONE to TIMEZONE'<br>(uses local timezone if zone missing)";
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
