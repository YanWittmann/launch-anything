package bar.ui;

import bar.Main;
import bar.logic.Settings;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TrayUtil {

    private static Main main;

    private static TrayIcon trayIcon;

    private static PopupMenu trayMenu;
    private static MenuItem resetTimeoutIcon;
    private static final Logger logger = LoggerFactory.getLogger(TrayUtil.class);

    public static void showMessage(String message) {
    	logger.info("Showing message tray: {}", message.replace("\n", "\n   "));
        trayIcon.displayMessage("LaunchAnything", message, TrayIcon.MessageType.INFO);
    }

    public static void showError(String message) {
    	logger.info("Showing error message tray: {}", message.replace("\n", "\n   "));
        trayIcon.displayMessage("LaunchAnything Error", message, TrayIcon.MessageType.ERROR);
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
        	logger.info("System tray not supported on this platform");
        }

        try {
            SystemTray sysTray = SystemTray.getSystemTray();
            trayIcon = createTrayIconFromResource();
            sysTray.add(trayIcon);
        } catch (AWTException e) {
        	logger.info("Unable to add icon to the system tray: {}", e.getMessage());
            logger.error("error ", e);
        } catch (Exception e) {
        	logger.info("Something went wrong while adding the icon to the system tray: {}", e.getMessage());
        	logger.error("error ", e);
        }
    }
}
