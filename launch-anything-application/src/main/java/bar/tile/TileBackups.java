package bar.tile;

import bar.tile.action.TileActionDirectory;
import bar.ui.TrayUtil;
import bar.util.Util;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class TileBackups {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private final File tileFile;
    private final File backupDir;

    public TileBackups(File tileFile) {
        this.tileFile = tileFile;
        this.backupDir = new File(tileFile.getParentFile(), "backup");
    }

    public void createBackup() {
        createDirectoryIfRequired();
        File backupFile = new File(backupDir, "backup_" + System.currentTimeMillis() + ".json");
        try {
            FileUtils.copyFile(this.tileFile, backupFile);
            LOG.info("Created backup of tiles in [{}]", backupFile.getAbsolutePath());
        } catch (IOException e) {
            LOG.error("Unable to create backup file: {}", e.getMessage());
        }
    }

    public boolean userAskLoadBackup() {
        String answer = Util.popupChooseButton("Load backup",
                "Something with your tiles file went wrong.\n" +
                "Do you want to load the most recent backup?",
                new String[]{"Yes", "No", "Open backup directory and exit"});
        if (answer != null) {
            if (answer.equals("Open backup directory and exit")) {
                openBackupDirAndExit();
            } else if (answer.equals("No")) {
                return false;
            }
        } else {
            return false;
        }
        try {
            File mostRecentBackup = getMostRecentBackup();
            tileFile.delete();
            Files.copy(mostRecentBackup.toPath(), tileFile.toPath());
            LOG.info("Loaded backup of tiles in [{}]", mostRecentBackup.getAbsolutePath());
            return true;
        } catch (IOException e) {
            LOG.error("Could not load backup of tiles file", e);
            TrayUtil.showError("Unable to load backup (try copying yourself): " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Could not load backup of tiles file", e);
            TrayUtil.showError("Unexpected error while loading backup (try copying yourself): " + e.getMessage());
        }
        return false;
    }

    public void openBackupDirAndExit() {
        try {
            createDirectoryIfRequired();
            TileActionDirectory.openDir(backupDir);
        } catch (IOException e) {
            Util.popupMessage("Backup", "You can find the backup file in the root directory of the application:\n" + backupDir.getAbsolutePath());
        }
        System.exit(0);
    }

    public void removeOldBackups(int maxBackupCount) {
        List<File> backupFiles = getBackupFiles();
        if (backupFiles.size() > maxBackupCount) {
            for (int i = 0; i < backupFiles.size() - maxBackupCount; i++) {
                LOG.info("Removed old backup file: [{}]", backupFiles.get(i).getAbsolutePath());
                backupFiles.get(i).delete();
            }
        }
    }

    private File getMostRecentBackup() {
        List<File> backupFiles = getBackupFiles();
        if (backupFiles.isEmpty()) return null;
        return backupFiles.get(backupFiles.size() - 1);
    }

    private List<File> getBackupFiles() {
        createDirectoryIfRequired();
        File[] files = backupDir.listFiles(f -> f.getName().startsWith("backup_"));
        if (files == null) {
            return new ArrayList<>();
        }
        List<File> backupFiles = Arrays.asList(files);
        backupFiles.sort(Comparator.comparingLong(f -> Long.parseLong(f.getName().substring(7, f.getName().indexOf(".")))));
        return backupFiles;
    }

    private void createDirectoryIfRequired() {
        if (!backupDir.exists()) backupDir.mkdirs();
    }
}
