package bar.tile;

import bar.tile.action.TileAction;
import bar.tile.custom.RuntimeTile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginTileLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PluginTileLoader.class);

    private File pluginDirectory;
    private final List<RuntimeTile> pluginRuntimeTiles = new ArrayList<>();
    private final Map<String, Class<? extends TileAction>> pluginTileActions = new HashMap<>();

    public PluginTileLoader() {
        findPluginDirectory();
        if (pluginDirectory == null) {
            LOG.info("No plugin directory found, creating one in [{}]", possiblePluginDirectories[0]);
            pluginDirectory = new File(possiblePluginDirectories[0]).getAbsoluteFile();
            if (!pluginDirectory.mkdirs()) {
                LOG.error("Failed to create plugin directory in [{}]", possiblePluginDirectories[0]);
            }
        }
    }

    private final static String[] possiblePluginDirectories = {
            "res/plugins/la/plugin",
            "plugins/la/plugin",
            "../plugins/la/plugin",
            "../res/plugins/la/plugin"
    };

    private void findPluginDirectory() {
        for (String possibleSettingsFile : possiblePluginDirectories) {
            File candidate = new File(possibleSettingsFile).getAbsoluteFile();
            if (candidate.exists()) {
                pluginDirectory = candidate;
                LOG.info("Found plugin directory in [{}]", pluginDirectory.getAbsolutePath());
                return;
            }
        }
        pluginDirectory = null;
    }

    private List<File> loadPluginFiles() {
        LOG.info("Detecting plugins in [{}]", pluginDirectory.getAbsolutePath());
        File[] files = pluginDirectory.listFiles();
        if (files == null) {
            LOG.error("Failed to list files in [{}]", pluginDirectory.getAbsolutePath());
            return new ArrayList<>();
        }

        List<File> pluginFiles = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                if ((file.getName().endsWith(".class") && !file.getName().startsWith(".") && !file.getName().contains("$")) || file.getName().endsWith(".jar")) {
                    pluginFiles.add(file);
                }
            }
        }

        if (pluginFiles.isEmpty()) {
            LOG.info("No plugins found in [{}]", pluginDirectory.getAbsolutePath());
        }
        return pluginFiles;
    }

    public void loadPlugins() {
        pluginRuntimeTiles.clear();

        if (pluginDirectory == null) {
            LOG.error("No plugin directory found, skipping loading plugins");
            return;
        }

        try {
            URL url = pluginDirectory.getParentFile().getParentFile().toURI().toURL();
            URL[] urls = new URL[]{url};

            ClassLoader classLoader = new URLClassLoader(urls);

            List<File> files = loadPluginFiles();

            for (File file : files) {
                try {
                    if (file.getName().endsWith(".class")) {
                        Class<?> cls = classLoader.loadClass("la.plugin." + file.getName().replace(".class", ""));
                        addPluginFromClass(cls, file);
                    } else if (file.getName().endsWith(".jar")) {
                        URL[] jarURLs = {new URL("jar:file:" + file.getAbsolutePath() + "!/")};
                        URLClassLoader jarClassLoader = URLClassLoader.newInstance(jarURLs);

                        JarFile jarFile = new JarFile(file);
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry jarEntry = entries.nextElement();
                            if (jarEntry.isDirectory() || !jarEntry.getName().endsWith(".class")) continue;
                            String className = jarEntry.getName().substring(0, jarEntry.getName().length() - 6).replace('/', '.');
                            Class<?> cls = jarClassLoader.loadClass(className);
                            addPluginFromClass(cls, file);
                        }
                    }
                } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | IOException e) {
                    LOG.error("Failed to load plugin [" + file.getName() + "]", e);
                }
            }
        } catch (MalformedURLException e) {
            LOG.error("Error loading plugins", e);
        }

        TileAction.setPlugins(pluginTileActions);
    }

    private void addPluginFromClass(Class<?> cls, File file) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        boolean isRuntimeTile = Arrays.stream(cls.getInterfaces()).anyMatch(i -> i.getName().equals(RuntimeTile.class.getName()));
        boolean isTileAction = cls.getSuperclass().getName().equals(TileAction.class.getName());
        if (!isRuntimeTile && !isTileAction) {
            LOG.warn("Plugin [{}] does not implement [{}] or extend [{}]", cls.getName(), RuntimeTile.class.getName(), TileAction.class.getName());
            return;
        }
        for (Constructor<?> constructor : cls.getConstructors()) {
            if (isRuntimeTile) {
                RuntimeTile o = (RuntimeTile) constructor.newInstance();
                pluginRuntimeTiles.add(o);
                LOG.info("Loaded runtime tile plugin from [{}]: [{}] [{}] [{}]", file.getName(), o.getName(), o.getAuthor(), o.getVersion());
                return;
            } else {
                TileAction o = (TileAction) constructor.newInstance();
                pluginTileActions.put(o.getType(), (Class<? extends TileAction>) cls);
                LOG.info("Loaded tile action plugin from [{}]: [{}]", file.getName(), o.getType());
                return;
            }
        }
        LOG.warn("Plugin [{}] does not have a constructor", cls.getName());
    }

    public List<RuntimeTile> getPluginRuntimeTiles() {
        return pluginRuntimeTiles;
    }
}
