package bar.util;

import bar.Main;
import bar.logic.Settings;
import bar.ui.TrayUtil;
import jnafilechooser.api.JnaFileChooser;
import mslinks.ShellLink;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;
import static javax.swing.JOptionPane.*;

public abstract class Util {

    private static Settings settings = null;

    public static void setSettings(Settings settings) {
        Util.settings = settings;
    }

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
        if (isOsWindows()) {
            JnaFileChooser fc = new JnaFileChooser();
            if (filterName != null && filterName.length() > 0)
                fc.addFilter(filterName, filters);
            fc.showOpenDialog(null);
            return fc.getSelectedFile();
        } else {
            // use the default java file chooser and return the file
            JFileChooser fileChooser = new JFileChooser();
            if (filterName != null && filterName.length() > 0)
                fileChooser.setFileFilter(new FileNameExtensionFilter(filterName, filters));
            if (previousFile != null) fileChooser.setCurrentDirectory(previousFile);
            else fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            boolean result = fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION;
            if (result) {
                previousFile = fileChooser.getSelectedFile();
                return fileChooser.getSelectedFile();
            }
        }
        return null;
    }

    public static String getOS() {
        return System.getProperty("os.name").toLowerCase();
    }

    public static boolean isOsWindows() {
        return getOS().contains("win");
    }

    public static void restartApplication(boolean withWebserver) throws URISyntaxException, IOException {
        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        final File currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        /* is it a jar file? */
        if (!currentJar.getName().endsWith(".jar"))
            return;

        /* Build command: java -jar application.jar */
        final ArrayList<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-jar");
        command.add(currentJar.getPath());
        if (withWebserver) command.add("-ws");

        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.start();
        System.exit(0);
    }

    private static File previousFile = null;

    public static File pickDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (previousFile != null) fileChooser.setCurrentDirectory(previousFile);
        else fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            if (fileChooser.getSelectedFile() != null) {
                previousFile = fileChooser.getSelectedFile();
                return fileChooser.getSelectedFile();
            }
        }

        return null;
    }

    public static String popupDropDown(String title, String message, String[] options, String preselected) {
        if (options == null || options.length == 0) return null;
        Object o = showInputDialog(null, message, title, PLAIN_MESSAGE, null, options, preselected != null ? preselected : options[0]);
        if (o == null) return null;
        return o.toString();
    }

    public static String popupTextInput(String title, String message, String pretext) {
        Object o = showInputDialog(null, message, title, PLAIN_MESSAGE, null, null, pretext);
        return o == null ? null : o.toString();
    }

    public static void popupMessage(String title, String message) {
        showMessageDialog(null, message, title, PLAIN_MESSAGE);
    }

    public static String urlDecode(String url) {
        try {
            return decode(url, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return url;
    }

    public static String urlEncode(String url) {
        try {
            return encode(url, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return url;
    }

    public static void copyToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    public static double evaluateMathematicalExpression(String expression) {
        Expression expr = new ExpressionBuilder(expression).build();
        return expr.evaluate();
    }

    public static String getHttpRequestResult(String url) throws IOException {
        StringBuilder result = new StringBuilder();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line);
            }
        }
        return result.toString();
    }

    public static List<File> recursivelyListFiles(File directory) {
        AtomicInteger amount = new AtomicInteger();
        return recursivelyListFiles(directory, amount);
    }

    private static List<File> recursivelyListFiles(File directory, AtomicInteger amount) {
        if (directory.exists() && directory.isDirectory()) {
            List<File> files = new ArrayList<>();
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    files.addAll(recursivelyListFiles(file, amount));
                } else {
                    files.add(file);
                    if (amount.incrementAndGet() > (settings != null ? settings.getInt("recursionLimit") : 200)) {
                        return files;
                    }
                }
            }
            return files;
        }
        return Collections.emptyList();
    }

    public static List<File> recursivelyListFiles(File directory, String... extension) {
        AtomicInteger amount = new AtomicInteger();
        return recursivelyListFiles(directory, amount, extension);
    }

    private static List<File> recursivelyListFiles(File directory, AtomicInteger amount, String... extension) {
        if (directory.exists() && directory.isDirectory()) {
            List<File> files = new ArrayList<>();
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    files.addAll(recursivelyListFiles(file, extension));
                } else if (extension == null || extension.length == 0 || Arrays.stream(extension).anyMatch(file.getName()::endsWith)) {
                    files.add(file);
                    if (amount.incrementAndGet() > (settings != null ? settings.getInt("recursionLimit") : 200)) {
                        return files;
                    }
                }
            }
            return files;
        }
        return Collections.emptyList();
    }

    public static void setAutostartActive(boolean active) {
        final String startupPath = "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs/Startup";
        File file = new File(System.getProperty("user.home") + startupPath + "/launch-anything.lnk");
        if (active) {
            try {
                final File currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                if (!currentJar.getName().endsWith(".jar")) {
                    TrayUtil.showError("Unable to add autostart feature: Application is not run from a jar.");
                    return;
                }
                ShellLink.createLink(currentJar.getAbsolutePath(), file.getAbsolutePath());
                TrayUtil.showMessage("Application will run on system startup");
            } catch (IOException | URISyntaxException e) {
                TrayUtil.showError("Unable to add autostart feature: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (file.delete()) {
                TrayUtil.showMessage("Application will not run on system startup");
            } else {
                TrayUtil.showError("Unable to remove autostart feature");
            }
        }
    }
}
