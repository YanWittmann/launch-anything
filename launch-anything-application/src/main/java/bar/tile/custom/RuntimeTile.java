package bar.tile.custom;

import bar.tile.Tile;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public interface RuntimeTile {

    /**
     * Generate a list of tiles that the user should get displayed.<br>
     * It is recommended to check the user input for some kind of keyword that activates theis plugin.<br>
     * If the input does not match your criteria, return an empty list.<br>
     * You must catch all exceptions yourself.
     *
     * @param search             The search string the user entered.
     * @param lastInputEvaluated The last time in milliseconds the user entered a search string.
     * @return A list of tiles that should be displayed.
     */
    List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated);

    /**
     * @return The name of the plugin. This will be displayed in the settings menu.
     */
    String getName();

    /**
     * @return The description of the plugin. This will be displayed in the settings menu.
     */
    String getDescription();

    /**
     * @return The author of the plugin.
     */
    String getAuthor();

    /**
     * @return The plugin version.
     */
    String getVersion();

    /**
     * Allows you to log info messages to the console.
     *
     * @param instance Pass <code>this</code> as parameter.
     * @param message  The message to log.
     * @param args     The arguments to use for formatting the message.
     */
    default void info(RuntimeTile instance, String message, Object... args) {
        LoggerFactory.getLogger(ChartGeneratorTile.class).info("[" + getImplementationIdentifier(instance) + "] " + message, args);
    }

    /**
     * Allows you to log warn messages to the console.
     *
     * @param instance Pass <code>this</code> as parameter.
     * @param message  The message to log.
     * @param args     The arguments to use for formatting the message.
     */
    default void warn(RuntimeTile instance, String message, Object... args) {
        LoggerFactory.getLogger(ChartGeneratorTile.class).warn("[" + getImplementationIdentifier(instance) + "] " + message, args);
    }

    /**
     * Allows you to log error messages to the console.
     *
     * @param instance Pass <code>this</code> as parameter.
     * @param message  The message to log.
     * @param args     The arguments to use for formatting the message.
     */
    default void error(RuntimeTile instance, String message, Object... args) {
        LoggerFactory.getLogger(ChartGeneratorTile.class).error("[" + getImplementationIdentifier(instance) + "] " + message, args);
    }

    default String getImplementationIdentifier(RuntimeTile instance) {
        return instance.getClass().getSimpleName() + " " + instance.getVersion();
    }
}
