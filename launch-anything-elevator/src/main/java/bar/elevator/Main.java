package bar.elevator;

import bar.common.Sleep;
import bar.common.VersionUtil;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static javax.swing.JOptionPane.showMessageDialog;

public class Main {

    public static void main(String[] args) {
        try {
            Sleep.seconds(4);
            JSONObject latestVersion = new JSONObject(VersionUtil.getLatestVersionJson());
            String version = VersionUtil.extractVersion(latestVersion);
            String versionName = VersionUtil.extractVersionTitle(latestVersion);
            String versionUrl = VersionUtil.findAsset(latestVersion, "launch-anything.jar");
            String releaseDate = VersionUtil.extractReleaseDate(latestVersion);
            System.out.println("Latest version: " + version + " (" + versionName + ")");
            System.out.println("Release date: " + releaseDate);
            System.out.println("Download URL: " + versionUrl);

            InputStream in = new URL(versionUrl).openStream();
            Files.copy(in, Paths.get("launch-anything.jar"), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
            showMessageDialog(null, "Unable to download latest version:\n" + e.getMessage(), "LaunchAnything Elevator Error", JOptionPane.ERROR_MESSAGE);
        }

        try {
            Desktop.getDesktop().open(new File("launch-anything.jar"));
        } catch (IOException e) {
            e.printStackTrace();
            showMessageDialog(null, "Unable to restart launch bar, please reopen it yourself:\n" + e.getMessage(), "Elevator Error", JOptionPane.ERROR_MESSAGE);
            try {
                Desktop.getDesktop().open(new File("./"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
