package bar.ui;

import bar.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;

public abstract class TrayUtil {

    private static final Logger LOG = LoggerFactory.getLogger(TrayUtil.class);

    private static Main main;

    private static TrayIcon trayIcon;

    private static PopupMenu trayMenu;
    private static MenuItem resetTimeoutIcon;

    private static long lastErrorOrWarningTimestamp = 0;

    public static void showMessage(String message) {
        if (System.currentTimeMillis() - lastErrorOrWarningTimestamp > 5000) {
            LOG.info("Showing message tray: {}", message.replace("\n", "\n   "));
            trayIcon.displayMessage("LaunchAnything", message, TrayIcon.MessageType.INFO);
        } else {
            LOG.info("Not showing message tray: {}", message.replace("\n", "\n   "));
        }
    }

    public static void showWarning(String message) {
        LOG.warn("Showing warning message tray: {}", message.replace("\n", "\n   "));
        trayIcon.displayMessage("LaunchAnything Warning", message, TrayIcon.MessageType.WARNING);
        lastErrorOrWarningTimestamp = System.currentTimeMillis();
    }

    public static void showError(String message) {
        LOG.info("Showing error message tray: {}", message.replace("\n", "\n   "));
        trayIcon.displayMessage("LaunchAnything Error", message, TrayIcon.MessageType.ERROR);
        lastErrorOrWarningTimestamp = System.currentTimeMillis();
    }

    public interface MessageCallback {
        void onMessage(String message);
    }

    public static void showMessage(String message, int maxDuration, MessageCallback callback) {
        LOG.info("Showing message tray: {}", message.replace("\n", "\n   "));
        try {
            SystemTray sysTray = SystemTray.getSystemTray();
            TrayIcon icon = createTrayIconFromResource();
            setDefaultIconVisible(false);
            sysTray.add(icon);
            icon.addActionListener(e -> callback.onMessage(message));
            icon.displayMessage("LaunchAnything", message, TrayIcon.MessageType.INFO);
            new Thread(() -> {
                try {
                    Thread.sleep(maxDuration);
                    sysTray.remove(icon);
                    setDefaultIconVisible(true);
                } catch (InterruptedException ignored) {
                }
            }).start();
        } catch (Exception e) {
            LOG.error("Error showing custom message tray", e);
        }
    }

    private static void setDefaultIconVisible(boolean visible) {
        try {
            SystemTray sysTray = SystemTray.getSystemTray();
            if (visible) {
                sysTray.add(trayIcon);
            } else {
                sysTray.remove(trayIcon);
            }
        } catch (Exception e) {
            LOG.error("Unable to update default tray icon visibility", e);
        }
    }

    private static PopupMenu createTrayMenu() {
        trayMenu = new PopupMenu();
        resetTimeoutIcon = new MenuItem("Reset timeout");
        resetTimeoutIcon.addActionListener(e -> main.timeout(0));

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        trayMenu.add(exitItem);

        MenuItem restartBar = new MenuItem("Restart bar");
        restartBar.addActionListener(e -> {
            try {
                main.restartBar();
            } catch (URISyntaxException | IOException ex) {
                ex.printStackTrace();
                TrayUtil.showError("Unable to restart bar: " + ex.getMessage());
            }
        });
        trayMenu.add(restartBar);

        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.addActionListener(e -> main.openSettingsWebServer(true));
        trayMenu.add(settingsItem);

        MenuItem resetSettingsItem = new MenuItem("Reset Settings");
        resetSettingsItem.addActionListener(e -> main.resetSettings());
        trayMenu.add(resetSettingsItem);

        return trayMenu;
    }

    public static void setMenuItemActive(int key, boolean active) {
        if (key == 0) {
            if (active) trayMenu.add(resetTimeoutIcon);
            else trayMenu.remove(resetTimeoutIcon);
        }
    }

    private static TrayIcon createTrayIconFromResource() {
        ClassLoader cldr = TrayUtil.class.getClassLoader();
        java.net.URL imageURL = cldr.getResource("img/tray.png");
        Image image = Toolkit.getDefaultToolkit().getImage(imageURL);
        PopupMenu popup = createTrayMenu();
        TrayIcon ti = new TrayIcon(image, "LaunchAnything", popup);
        ti.setImageAutoSize(true);
        return ti;
    }

    public static void init(Main main) {
        TrayUtil.main = main;

        if (!SystemTray.isSupported()) {
            LOG.info("System tray not supported on this platform");
        }

        try {
            SystemTray sysTray = SystemTray.getSystemTray();
            trayIcon = createTrayIconFromResource();
            sysTray.add(trayIcon);
        } catch (AWTException e) {
            LOG.info("Unable to add icon to the system tray: {}", e.getMessage());
            LOG.error("error ", e);
        } catch (Exception e) {
            LOG.info("Something went wrong while adding the icon to the system tray: {}", e.getMessage());
            LOG.error("error ", e);
        }
    }
}
