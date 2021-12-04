package bar.tile.custom;

import bar.Main;
import bar.tile.Tile;
import bar.tile.TileAction;
import bar.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class SystemInfoTile implements RuntimeTile {

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        if (search.startsWith("sys") && search.length() > 3) {
            String searchValue = search.substring(3).trim().toLowerCase().replace(" ", "");
            String result = null;
            if (searchValue.length() > 0) {
                switch (searchValue) {
                    case "externalip":
                    case "eip":
                    case "ipexternal":
                    case "ipe":
                        try {
                            result = Util.getExternalIpAddress();
                        } catch (ExecutionException | InterruptedException ignored) {
                        }
                        break;
                    case "localip":
                    case "lip":
                    case "iplocal":
                    case "ipl":
                        try {
                            result = Util.getLocalIp();
                        } catch (IOException ignored) {
                        }
                        break;
                    case "version":
                    case "ver":
                        result = Main.getVersionString();
                        break;
                    case "os":
                    case "op":
                        result = Util.getOS();
                        break;
                    case "isjar":
                    case "jar":
                        result = Util.isApplicationStartedFromJar() + "";
                        break;
                    case "isautostart":
                    case "autostart":
                        result = Util.isAutostartEnabled() + "";
                        break;
                    case "font":
                    case "fonts":
                        List<Tile> tiles = new ArrayList<>();
                        for (String availableFont : Util.getAvailableFonts()) {
                            tiles.add(createCopyTextTile(availableFont, availableFont));
                        }
                        return tiles;
                }
            }
            if (result != null) {
                return Collections.singletonList(createCopyTextTile(result, result));
            }
        }
        return Collections.emptyList();
    }

    private Tile createCopyTextTile(String label, String copyText) {
        Tile tile = new Tile(label);
        tile.setCategory("runtime");
        tile.addAction(new TileAction("copy", copyText));
        return tile;
    }

    public static String getTitle() {
        return "System Info";
    }

    public static String getDescription() {
        return "Enter 'sys' and some value you want (see documentation)";
    }
}
