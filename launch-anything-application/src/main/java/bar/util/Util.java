package bar.util;

import bar.Main;
import bar.logic.Settings;
import bar.ui.TrayUtil;
import jnafilechooser.api.JnaFileChooser;
import mslinks.ShellLink;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;
import static javax.swing.JOptionPane.*;

public abstract class Util {

    private static Settings settings = null;
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    public static void setSettings(Settings settings) {
        Util.settings = settings;
    }

    public static void registerFont(String path) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Main.class.getClassLoader().getResourceAsStream(path));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (FontFormatException | IOException | NullPointerException e) {
            LOG.error("error ", e);
        }
    }

    public static void copyResource(String res, String dest) throws IOException {
        InputStream src = Main.class.getClassLoader().getResourceAsStream(res);
        Files.copy(src, Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
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
            LOG.error("error ", e);
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

    public static File pickFile(File dir, String filterName, String... filters) {
        if (isOsWindows()) {
            JnaFileChooser fc = new JnaFileChooser();
            fc.setCurrentDirectory(String.valueOf(dir));
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
        if (!currentJar.getName().endsWith(".jar")) {
            TrayUtil.showError("The application has not been launched from a jar file. Canceling restart.");
            return;
        }

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

    public static void setPreviousFile(File file) {
        previousFile = file;
    }

    public static String popupDropDown(String title, String message, String[] options, String preselected) {
        LOG.info("popupDropDown: [{}] [{}] [{}]", title, String.valueOf(message).replace("\n", ""), preselected);
        if (options == null || options.length == 0) return null;
        Object o = showInputDialog(null, message, title, PLAIN_MESSAGE, null, options, preselected != null ? preselected : options[0]);
        if (o == null) return null;
        return o.toString();
    }

    public static String popupTextInput(String title, String message, String pretext) {
        LOG.info("popupTextInput: [{}] [{}] [{}]", title, String.valueOf(message).replace("\n", ""), pretext);
        Object o = showInputDialog(null, message, title, PLAIN_MESSAGE, null, null, pretext);
        return o == null ? null : o.toString();
    }

    public static String popupChooseButton(String title, String message, String[] options) {
        LOG.info("popupChooseButton: [{}] [{}] [{}]", title, String.valueOf(message).replace("\n", ""), options);
        if (options == null || options.length == 0) return null;
        int o = showOptionDialog(null, message, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, 0);
        if (o == JOptionPane.CLOSED_OPTION) return null;
        return options[o];
    }

    public static void popupMessage(String title, String message) {
        LOG.info("popupMessage: [{}] [{}]", title, String.valueOf(message).replace("\n", ""));
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

    public static String getClipboardText() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable transferable = clipboard.getContents(null);
        if (transferable == null) return null;
        try {
            return (String) transferable.getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException e) {
            return null;
        }
    }

    private final static List<Function> directFunctions = new ArrayList<>();

    static {
        directFunctions.add(new Function("root", 2) {
            @Override
            public double apply(double... args) {
                double xPre = Math.random() % 10;
                double error = 0.0000001;
                double delX = 2147483647;
                double current = 0.0;

                while (delX > error) {
                    current = ((args[1] - 1.0) * xPre + args[0] / Math.pow(xPre, args[1] - 1)) / args[1];
                    delX = Math.abs(current - xPre);
                    xPre = current;
                }
                return current;
            }
        });
        directFunctions.add(new Function("fac", 1) {
            @Override
            public double apply(double... args) {
                if (args[0] > 170) return Double.POSITIVE_INFINITY;
                double result = 1.0;
                for (int i = 1; i <= args[0]; i++) result *= i;
                return result;
            }
        });
        directFunctions.add(new Function("log", 2) {
            @Override
            public double apply(double... args) {
                return Math.log(args[1]) / Math.log(args[0]);
            }
        });
        directFunctions.add(new Function("ln", 1) {
            @Override
            public double apply(double... args) {
                return Math.log(args[0]);
            }
        });
        directFunctions.add(new Function("max", 2) {
            @Override
            public double apply(double... args) {
                return Math.max(args[0], args[1]);
            }
        });
        directFunctions.add(new Function("min", 2) {
            @Override
            public double apply(double... args) {
                return Math.min(args[0], args[1]);
            }
        });
        directFunctions.add(new Function("random", 2) {
            @Override
            public double apply(double... args) {
                return Math.random() * (args[1] - args[0]) + args[0];
            }
        });
        directFunctions.add(new Function("round", 1) {
            @Override
            public double apply(double... args) {
                return Math.round(args[0]);
            }
        });
        directFunctions.add(new Function("fac", 1) {
            @Override
            public double apply(double... args) {
                if (args[0] > 170) return Double.POSITIVE_INFINITY;
                double result = 1.0;
                for (int i = 1; i <= args[0]; i++) result *= i;
                return result;
            }
        });
        directFunctions.add(new Function("checksum", 1) {
            @Override
            public double apply(double... args) {
                return createQuersummeFunction(args);
            }
        });
        directFunctions.add(new Function("sod", 1) {
            @Override
            public double apply(double... args) {
                return createQuersummeFunction(args);
            }
        });
        directFunctions.add(new Function("quersumme", 1) {
            @Override
            public double apply(double... args) {
                return createQuersummeFunction(args);
            }
        });
        directFunctions.add(new Function("ggt", 2) {
            @Override
            public double apply(double... args) {
                return createGgtFunction(args);
            }
        });
        directFunctions.add(new Function("gcd", 2) {
            @Override
            public double apply(double... args) {
                return createGgtFunction(args);
            }
        });
    }

    private static int createQuersummeFunction(double[] args) {
        int total = 0;
        double arg = args[0];
        while (arg > 0) {
            total += (int) (arg % 10);
            arg /= 10;
        }
        return total;
    }

    private static double createGgtFunction(double[] args) {
        int a = (int) args[0];
        int b = (int) args[1];
        while (b != 0) {
            int c = a % b;
            a = b;
            b = c;
        }
        return a;
    }

    public static double evaluateMathematicalExpression(String expression) {
        Expression expr = new ExpressionBuilder(expression).build();
        return expr.evaluate();
    }

    public static double evaluateMathematicalExpression(String expression, Map<String, Double> variables, Map<String, String> functions) {
        ExpressionBuilder expressionBuilder = new ExpressionBuilder(expression);
        directFunctions.forEach(expressionBuilder::function);
        if (functions != null) {
            for (String function : functions.keySet()) {
                expressionBuilder.function(new Function(function, 1) {
                    @Override
                    public double apply(double... args) {
                        final Map<String, Double> localVariables = new HashMap<>(variables);
                        if (args.length > 0) localVariables.put("x", args[0]);
                        return Util.evaluateMathematicalExpression(functions.get(function), localVariables, null);
                    }
                });
            }
        }
        if (variables != null) expressionBuilder.variables(variables.keySet());
        Expression expr = expressionBuilder.build();
        if (variables != null) variables.forEach(expr::setVariable);
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
                    if (amount.incrementAndGet() > (settings != null ? settings.getInt(Settings.Setting.TILE_GENERATOR_FILE_LIMIT) : 1000)) {
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
                    if (amount.incrementAndGet() > (settings != null ? settings.getInt(Settings.Setting.TILE_GENERATOR_FILE_LIMIT) : 1000)) {
                        return files;
                    }
                }
            }
            return files;
        }
        return Collections.emptyList();
    }

    public static String capitalizeWords(String text) {
        StringBuilder sb = new StringBuilder();
        if (text.length() > 0) {
            sb.append(Character.toUpperCase(text.charAt(0)));
        }
        for (int i = 1; i < text.length(); i++) {
            String chPrev = String.valueOf(text.charAt(i - 1));
            String ch = String.valueOf(text.charAt(i));
            if (Objects.equals(chPrev, " ")) {
                sb.append(ch.toUpperCase());
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();

    }

    public static boolean isAutostartEnabled() {
        final String startupPath = "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs/Startup";
        File file = new File(System.getProperty("user.home") + startupPath + "/launch-anything.lnk");
        return file.exists();
    }

    public static boolean isApplicationStartedFromJar() {
        try {
            final File currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return currentJar.getName().endsWith(".jar");
        } catch (Exception e) {
            return false;
        }
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
                LOG.error("error ", e);
            }
        } else {
            if (file.delete()) {
                TrayUtil.showMessage("Application will not run on system startup");
            } else {
                TrayUtil.showError("Unable to remove autostart feature");
            }
        }
    }

    private static final Pattern IPV4_PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private static final String[] IPV4_SERVICES = {
            "http://checkip.amazonaws.com/",
            "https://ipv4.icanhazip.com/",
            "http://bot.whatismyipaddress.com/"
    };

    public static String getExternalIpAddress() throws ExecutionException, InterruptedException {
        List<Callable<String>> callables = new ArrayList<>();
        for (String ipService : IPV4_SERVICES) {
            callables.add(() -> getIpAddressFromUrl(ipService));
        }

        ExecutorService executorService = Executors.newCachedThreadPool();
        try {
            return executorService.invokeAny(callables);
        } finally {
            executorService.shutdown();
        }
    }

    private static String getIpAddressFromUrl(String url) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            String ip = in.readLine();
            if (IPV4_PATTERN.matcher(ip).matches()) {
                return ip;
            } else {
                throw new IOException("invalid IPv4 address: " + ip);
            }
        }
    }

    public static String getLocalIp() throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("google.com", 80));
        InetAddress localAddress = socket.getLocalAddress();
        socket.close();
        return localAddress.getHostAddress();
    }

    public static String[] getAvailableFonts() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    }

    public static File getTempFile(String ending) {
        try {
            return File.createTempFile("launch-anything-file-" + (Math.random() + "").replace(".", ""), ending);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void cleanupTempFiles() {
        File[] tempFiles = new File(System.getProperty("java.io.tmpdir")).listFiles((dir, name) -> name.startsWith("launch-anything-file-"));
        if (tempFiles != null) {
            LOG.info("Cleaning up [{}] temp files", tempFiles.length);
            for (File tempFile : tempFiles) {
                tempFile.delete();
            }
        }
    }

    public static Rectangle detectMonitor(Point point) {
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();

        GraphicsDevice[] devices = e.getScreenDevices();

        Rectangle displayBounds = null;

        for (GraphicsDevice device : devices) {

            GraphicsConfiguration[] configurations = device.getConfigurations();
            for (GraphicsConfiguration config : configurations) {
                Rectangle gcBounds = config.getBounds();

                if (gcBounds.contains(point)) {
                    displayBounds = gcBounds;
                }
            }
        }

        if (displayBounds == null) {
            // not found, get the bounds for the default display
            GraphicsDevice device = e.getDefaultScreenDevice();
            displayBounds = device.getDefaultConfiguration().getBounds();
        }

        return displayBounds;
    }

    public static Point getAbsoluteMousePosition() {
        return MouseInfo.getPointerInfo().getLocation();
    }

    public static String getWebsiteTitle(String url) {
        InputStream response = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            connection.connect();
            response = connection.getInputStream();
            String responseBody = IOUtils.toString(response, StandardCharsets.UTF_8);
            return responseBody.substring(responseBody.indexOf("<title>") + 7, responseBody.indexOf("</title>"));
        } catch (Exception ex) {
            LOG.error("Error getting website title", ex);
        } finally {
            try {
                response.close();
            } catch (Exception ex) {
                LOG.error("Error closing response", ex);
            }
        }
        return null;
    }
}
