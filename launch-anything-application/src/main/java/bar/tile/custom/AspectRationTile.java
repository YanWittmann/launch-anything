package bar.tile.custom;

import bar.tile.Tile;
import bar.tile.action.TileActionCopy;
import de.yanwittmann.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class AspectRationTile implements RuntimeTile {

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        if (search.length() >= 6 && search.contains(":") && search.contains(" ")) {
            String[] split = search.split(" ");
            if (split.length == 2) {
                String[] ratio = split[0].split(":");
                if (ratio.length == 2) {
                    try {
                        int width = Integer.parseInt(ratio[0]);
                        int height = Integer.parseInt(ratio[1]);
                        if (width > 0 && height > 0) {
                            String other = split[1];
                            if (other.contains(":")) {
                                Tile roundedResult = new Tile();
                                Tile exactResult = new Tile();
                                roundedResult.setCategory("runtime");
                                exactResult.setCategory("runtime");
                                if (other.startsWith(":")) {
                                    int otherHeight = Integer.parseInt(other.substring(1));
                                    int otherWidth = (int) Math.round(otherHeight * width / (double) height);
                                    String exactOtherWidth = String.valueOf(Util.roundToDecimals(otherHeight * width / (double) height, 2)).replaceAll("\\.0$", "");
                                    roundedResult.setLabel(otherWidth + "  =  " + otherHeight + " * " + width + " / " + height + " (rounded)");
                                    roundedResult.addAction(new TileActionCopy(otherWidth + ""));
                                    exactResult.setLabel(exactOtherWidth + "  =  " + otherHeight + " * " + width + " / " + height + " (exact)");
                                    exactResult.addAction(new TileActionCopy(exactOtherWidth + ""));
                                } else {
                                    int otherWidth = Integer.parseInt(other.substring(0, other.indexOf(":")));
                                    int otherHeight = (int) Math.round(otherWidth * height / (double) width);
                                    String exactOtherHeight = String.valueOf(Util.roundToDecimals(otherWidth * height / (double) width, 2)).replaceAll("\\.0$", "");
                                    roundedResult.setLabel(otherHeight + "  =  " + otherWidth + " * " + height + " / " + width + " (rounded)");
                                    roundedResult.addAction(new TileActionCopy(otherHeight + ""));
                                    exactResult.setLabel(exactOtherHeight + "  =  " + otherWidth + " * " + height + " / " + width + " (exact)");
                                    exactResult.addAction(new TileActionCopy(exactOtherHeight + ""));
                                }
                                List<Tile> result = new ArrayList<>();
                                result.add(roundedResult);
                                if (!exactResult.getLabel().replace("(exact)", "").equals(roundedResult.getLabel().replace("(rounded)", ""))) {
                                    result.add(exactResult);
                                } else {
                                    roundedResult.setLabel(roundedResult.getLabel().replace("(rounded)", ""));
                                }
                                return result;
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "Aspect Ratio";
    }

    @Override
    public String getDescription() {
        return "Enter something like '4:3 500:' or '16:9 :450' to calculate the corresponding other value";
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
