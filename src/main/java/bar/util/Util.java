package bar.util;

import bar.Main;
import jnafilechooser.api.JnaFileChooser;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.StringJoiner;

import static java.net.URLDecoder.*;

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

    public static String popupDropDown(String title, String message, String[] options, String preselected) {
        if (options == null || options.length == 0) return null;
        return (String) JOptionPane.showInputDialog(null, message, title, JOptionPane.PLAIN_MESSAGE, null, options, preselected != null ? preselected : options[0]);
    }

    public static String popupTextInput(String title, String message, String pretext) {
        return String.valueOf(JOptionPane.showInputDialog(null, message, title, JOptionPane.PLAIN_MESSAGE, null, null, pretext));
    }

    public static String urlDecode(String url) {
        try {
            return decode(url, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return url;
    }
}
