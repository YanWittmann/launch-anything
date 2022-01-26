package bar.tile.custom;

import bar.tile.Tile;
import bar.tile.action.TileAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberBaseConverterTile implements RuntimeTile {

    private final static Pattern SYSTEM_PATTERN = Pattern.compile("(dec|decimal|bin|binary|oct|octal|hex|hexadecimal)");

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        search = search.replace(" ", "");
        if (search.length() > 2) {
            if (search.matches("[01]+")) search += "bin";
            Matcher matcher = SYSTEM_PATTERN.matcher(search);
            if (matcher.find()) {
                String originSystem = matcher.group(1).substring(0, 3).toLowerCase();
                String convertNumber = search.replaceAll("[^0-9A-F-]", "");

                if (matcher.find()) {
                    // if a destination system is specified, convert to that system
                    String targetSystem = matcher.group(1).substring(0, 3).toLowerCase();
                    if (convertNumber.length() > 0) {
                        String converted = convert(originSystem, targetSystem, convertNumber).toUpperCase();
                        return Collections.singletonList(createCopyTextTile(targetSystem.toUpperCase() + ": " + converted, converted));
                    }
                } else {
                    // if no destination system is specified, convert to all systems
                    List<Tile> tiles = new ArrayList<>();
                    if (!originSystem.equals("dec")) {
                        String convertedDec = convert(originSystem, "dec", convertNumber);
                        tiles.add(createCopyTextTile("DEC: " + convertedDec, convertedDec));
                    }
                    String convertedBin = convert(originSystem, "bin", convertNumber);
                    if (!originSystem.equals("bin")) {
                        tiles.add(createCopyTextTile("BIN: " + convertedBin, convertedBin));
                    }
                    if (!originSystem.equals("oct")) {
                        String convertedOct = convert(originSystem, "oct", convertNumber);
                        tiles.add(createCopyTextTile("OCT: " + convertedOct, convertedOct));
                    }
                    if (!originSystem.equals("hex")) {
                        String convertedHex = convert(originSystem, "hex", convertNumber.toUpperCase());
                        tiles.add(createCopyTextTile("HEX: " + convertedHex, convertedHex));
                    }
                    String convertedBCD = binToHexBCD(convertedBin);
                    tiles.add(createCopyTextTile("BCD: " + convertedBCD, convertedBCD));
                    return tiles;
                }
            }
        }
        return Collections.emptyList();
    }

    private Tile createCopyTextTile(String label, String copyText) {
        Tile tile = new Tile(label);
        tile.setCategory("runtime");
        tile.addAction(TileAction.getInstance("copy", copyText));
        return tile;
    }

    private String binToHexBCD(String bin) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < bin.length(); i += 4) {
            String bcd = bin.substring(i, Math.min(i + 4, bin.length()));
            hex.append(Integer.toHexString(Integer.parseInt(bcd, 2)));
        }
        return hex.toString().toUpperCase();
    }

    private String convert(String originSystem, String targetSystem, String convertNumber) {
        switch (originSystem) {
            case "dec":
                switch (targetSystem) {
                    case "bin":
                        return Integer.toBinaryString(Integer.parseInt(convertNumber));
                    case "oct":
                        return Integer.toOctalString(Integer.parseInt(convertNumber));
                    case "hex":
                        return Integer.toHexString(Integer.parseInt(convertNumber)).toUpperCase();
                }
                break;
            case "bin":
                switch (targetSystem) {
                    case "dec":
                        return Integer.toString(Integer.parseInt(convertNumber, 2));
                    case "oct":
                        return Integer.toOctalString(Integer.parseInt(convertNumber, 2));
                    case "hex":
                        return Integer.toHexString(Integer.parseInt(convertNumber, 2)).toUpperCase();
                }
                break;
            case "oct":
                switch (targetSystem) {
                    case "dec":
                        return Integer.toString(Integer.parseInt(convertNumber, 8));
                    case "bin":
                        return Integer.toBinaryString(Integer.parseInt(convertNumber, 8));
                    case "hex":
                        return Integer.toHexString(Integer.parseInt(convertNumber, 8)).toUpperCase();
                }
                break;
            case "hex":
                switch (targetSystem) {
                    case "dec":
                        return Integer.toString(Integer.parseInt(convertNumber, 16));
                    case "bin":
                        return Integer.toBinaryString(Integer.parseInt(convertNumber, 16));
                    case "oct":
                        return Integer.toOctalString(Integer.parseInt(convertNumber, 16));
                }
                break;
        }
        return convertNumber;
    }

    @Override
    public String getName() {
        return "Number Base Converter";
    }

    @Override
    public String getDescription() {
        return "Enter something like 'dec 13 bin' to convert 13 to binary (available: 'dec/bin/oct/hex')";
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
