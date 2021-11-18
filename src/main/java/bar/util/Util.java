package bar.util;

import bar.Main;
import jnafilechooser.api.JnaFileChooser;

import java.awt.*;
import java.io.*;
import java.util.StringJoiner;

public abstract class Util {

    public static void registerFont(String path) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Main.class.getClassLoader().getResourceAsStream(path));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (FontFormatException | IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static String readClassResource(String path) {
        StringJoiner out = new StringJoiner("\n");
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(path);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toString();
    }

    public static File pickFile(String filterName, String... filters) {
        JnaFileChooser fc = new JnaFileChooser();
        if (filterName != null && filterName.length() > 0)
            fc.addFilter(filterName, filters);
        fc.showOpenDialog(null);
        return fc.getSelectedFile();
    }
}
