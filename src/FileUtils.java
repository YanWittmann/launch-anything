
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.Timer;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public abstract class FileUtils {

    public static void makeDirectories(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public static boolean writeFile(File file, String[] data) {
        BufferedWriter outputWriter = null;
        try {
            outputWriter = new BufferedWriter(new FileWriter(file));
            for (String datum : data) {
                outputWriter.write(datum);
                outputWriter.newLine();
            }
            outputWriter.flush();
            outputWriter.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean writeFile(File file, String data) {
        BufferedWriter outputWriter = null;
        try {
            outputWriter = new BufferedWriter(new FileWriter(file));
            outputWriter.write(data);
            outputWriter.flush();
            outputWriter.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String[] readFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            return sb.toString().split(System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] readFileToByteArray(String filename) {
        try {
            return Files.readAllBytes(new File(filename).toPath());
        } catch (IOException ignored) {
        }
        return null;
    }

    public static void writeFileFromByteArray(String filename, byte[] array) {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(array);
            fos.close();
        } catch (Exception ignored) {
        }
    }

    public static boolean copyFile(String sourceFile, String destinationFile) {
        try {
            Files.copy(new File(sourceFile).toPath(), (new File(destinationFile)).toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation) {
        try {
            Files.walk(Paths.get(sourceDirectoryLocation))
                    .forEach(source -> {
                        Path destination = Paths.get(destinationDirectoryLocation, source.toString()
                                .substring(sourceDirectoryLocation.length()));
                        try {
                            Files.copy(source, destination);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteDirectory(String dir) {
        if (!directoryExists(dir)) return false;
        deleteDir(new File(dir));
        return true;
    }

    private static boolean deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (final File file : files) {
                deleteDir(file);
            }
        }
        return dir.delete();
    }

    public static boolean deleteFile(String file) {
        return new File(file).delete();
    }

    public static void deleteFilesInDirectory(String directory) {
        try {
            File dir = new File(directory);
            File[] listFiles = dir.listFiles();
            for (File file : listFiles) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean fileExists(String file) {
        return new File(file).exists();
    }

    public static boolean directoryExists(String directory) {
        return new File(directory).exists();
    }

    public static boolean openFile(String file, String workingDirectory) {
        try {
            ProcessBuilder pb = new ProcessBuilder(new File(file).getAbsoluteFile().toString());
            pb.directory(new File(workingDirectory).getAbsoluteFile());
            pb.start();
            return true;
        } catch (Exception e) {
            if (!openFileViaDesktop(new File(file)))
                e.printStackTrace();
            return false;
        }
    }

    public static boolean openFile(String file) {
        try {
            ProcessBuilder pb = new ProcessBuilder(file);
            pb.start();
            return true;
        } catch (Exception e) {
            if (!openFileViaDesktop(new File(file)))
                e.printStackTrace();
            return false;
        }
    }

    public static boolean openFileViaDesktop(File file) {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (file.exists())
                desktop.open(file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /* thank you to,        Maurício Linhares   https://stackoverflow.com/questions/6811522/changing-the-working-directory-of-command-from-java/6811578
     * and                  Aniket Thakur       https://stackoverflow.com/questions/17985036/run-a-jar-file-from-java-program
     */
    public static void openJar(String jar, String path, String[] args) {
        try {
            File pathToExecutable = new File(jar);
            String[] args2 = new String[args.length + 3];
            args2[0] = "java";
            args2[1] = "-jar";
            args2[2] = pathToExecutable.getAbsolutePath();
            System.arraycopy(args, 0, args2, 3, args2.length - 3);
            /*for (int i = 3; i < args2.length; i++)
                args2[i] = args[i - 3];*/
            ProcessBuilder builder = new ProcessBuilder(args2);
            builder.directory(new File(path).getAbsoluteFile()); // this is where you set the root folder for the executable to run with
            builder.redirectErrorStream(true);
            builder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean pack(String sourceDirPath, String zipFilePath) {
        try {
            deleteFile(zipFilePath);
            Path p = Files.createFile(Paths.get(zipFilePath));
            try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
                Path pp = Paths.get(sourceDirPath);
                Files.walk(pp)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                            try {
                                zs.putNextEntry(zipEntry);
                                Files.copy(path, zs);
                                zs.closeEntry();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void unpack(String zipFile, String destination) throws IOException {
        File destDir = new File(destination);
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newUnZipFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    private static File newUnZipFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public static boolean isArchive(String file) {
        int fileSignature = 0;
        try (RandomAccessFile raf = new RandomAccessFile(new File(file), "r")) {
            fileSignature = raf.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708;
    }

    public static long fileSize(String filename) {
        return new File(filename).length();
    }

    public static int onlineFileSize(String url) {
        try {
            URL url1 = new URL(url);
            URLConnection conn = null;
            try {
                conn = url1.openConnection();
                if (conn != null) {
                    ((HttpURLConnection) conn).setRequestMethod("HEAD");
                }
                assert conn != null;
                conn.getInputStream();
                return conn.getContentLength();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (conn instanceof HttpURLConnection) {
                    ((HttpURLConnection) conn).disconnect();
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static String[] getFiles(String path) {
        File directoryPath = new File(path);
        int counter = 0;
        for (File file : Objects.requireNonNull(directoryPath.listFiles()))
            counter++;
        String[] allFiles = new String[counter];
        counter = 0;
        for (File file : Objects.requireNonNull(directoryPath.listFiles())) {
            allFiles[counter] = file.getName();
            counter++;
        }
        return allFiles;
    }

    public static String[] getFilesWithEnding(String path, String ending) {
        final String ending2 = ending.replace(".", "");
        File directoryPath = new File(path);

        File[] files = directoryPath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("." + ending2);
            }
        });

        int counter = 0;
        assert files != null;
        for (File file : files)
            counter++;
        String[] allFiles = new String[counter];
        counter = 0;
        for (File file : files) {
            allFiles[counter] = file.getName();
            counter++;
        }
        return allFiles;
    }

    public static ArrayList<File> listf(String directoryName) {
        ArrayList<File> files = new ArrayList<>();
        File directory = new File(directoryName);

        File[] fList = directory.listFiles();
        if (fList != null)
            for (File file : fList) {
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    files.addAll(listf(file.getAbsolutePath()));
                }
            }
        return files;
    }

    private static String lastPickLocation = "";

    public static String javaFilePicker() {
        JFileChooser chooser = null;
        if (lastPickLocation.equals("")) chooser = new JFileChooser(System.getProperty("user.home") + "/Desktop");
        else chooser = new JFileChooser(lastPickLocation);
        chooser.showOpenDialog(null);
        lastPickLocation = chooser.getSelectedFile().getAbsolutePath();
        try {
            return chooser.getSelectedFile().getAbsolutePath();
        } catch (Exception e) {
            return "";
        }
    }

    public static String[] windowsFilePicker() {
        FileDialog picker = new FileDialog((Frame) null);
        picker.setVisible(true);
        File[] f = picker.getFiles();
        String[] paths = new String[f.length];
        for (int i = 0; i < f.length; i++)
            paths[i] = f[i].getAbsolutePath();
        return paths;
    }

    public static String getFilename(String path) {
        File f = new File(path);
        return f.getName();
    }

    public static String[] getResponseFromURL(String pUrl) {
        pUrl = pUrl.replace(" ", "%20");
        ArrayList<String> lines = new ArrayList<>();
        try {
            URL url = new URL(pUrl);
            BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));
            String i;
            while ((i = read.readLine()) != null)
                lines.add(i);
            read.close();
        } catch (Exception ignored) {
        }
        String[] result = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++)
            result[i] = lines.get(i);
        return result;
    }

    public static boolean saveUrl(String filename, String urlString) {
        BufferedInputStream in;
        FileOutputStream fout;
        try {
            in = new BufferedInputStream(new URL(urlString).openStream());
            fout = new FileOutputStream(filename);

            final byte[] data = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }
            in.close();
            fout.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean connectedToInternet() {
        try {
            URL url = new URL("http://www.google.com");
            URLConnection connection = url.openConnection();
            connection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Font getFont(String filename) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, new File(filename)).deriveFont(30f);
        } catch (Exception e) {
            return new Font("TimesRoman", Font.PLAIN, 30);
        }
    }

    private static HashMap<String, Timer> watchFiles = new HashMap<>();

    public static void addWatchFile(String path) {
        if (watchFiles.containsKey(path)) return;
        watchFiles.put(path, watchFileSaved(path));
    }

    public static void removeWatchFile(String path) {
        if (!watchFiles.containsKey(path)) return;
        Timer t = watchFiles.get(path);
        t.cancel();
        t.purge();
        watchFiles.remove(path);
        deleteFile(path);
    }

    //thanks to Réal Gagnon for this part of the code (https://www.rgagnon.com/javadetails/java-0490.html)
    private static Timer watchFileSaved(String path) {
        TimerTask task = new FileWatcher(new File(path)) {
            protected void onChange(File file) {
                //put your onChange code here
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, new Date(), 1000);
        return timer;
    }

    public abstract static class FileWatcher extends TimerTask {
        private long timeStamp;
        private final File file;

        public FileWatcher(File file) {
            this.file = file;
            this.timeStamp = file.lastModified();
        }

        public final void run() {
            long timeStamp = file.lastModified();
            if (this.timeStamp != timeStamp) {
                this.timeStamp = timeStamp;
                onChange(file);
            }
        }

        protected abstract void onChange(File file);

    }

    public static long lastModified(String file) {
        return new File(file).lastModified();
    }

    public static void setLastModified(String file, long time) {
        try {
            Files.setLastModifiedTime(Path.of(file), FileTime.fromMillis(time));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setHidden(String file, boolean hidden) {
        try {
            Files.setAttribute(Paths.get(file), "dos:hidden", hidden, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isHidden(String file) {
        return new File(file).isHidden();
    }
}
