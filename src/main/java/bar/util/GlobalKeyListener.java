package bar.util;

import lc.kra.system.keyboard.GlobalKeyboardHook;
import lc.kra.system.keyboard.event.GlobalKeyAdapter;
import lc.kra.system.keyboard.event.GlobalKeyEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * This class uses the {@link GlobalKeyboardHook} from <code>lc.kra.system</code> to globally listen for key events.<br>
 * This is a very important aspect of the application, since it allows for live input of the players, which is required
 * for some games.<br>
 * This is being activated when needed by a minigame and deactivated if no longer required.
 */
public class GlobalKeyListener {

    private GlobalKeyboardHook keyboardHook;
    private final GlobalKeyAdapter listener = new GlobalKeyAdapter() {
        @Override
        public void keyPressed(GlobalKeyEvent event) {
            listeners.forEach(l -> l.keyPressed(event));
        }

        @Override
        public void keyReleased(GlobalKeyEvent event) {
            listeners.forEach(l -> l.keyReleased(event));
        }
    };

    private final List<KeyListener> listeners = new ArrayList<>();

    public void addListener(KeyListener listener) {
        listeners.add(listener);
    }

    public void removeListener(KeyListener listener) {
        listeners.remove(listener);
    }

    public void activate() {
        keyboardHook = new GlobalKeyboardHook(false);
        keyboardHook.addKeyListener(listener);
    }

    public void deactivate() {
        keyboardHook.removeKeyListener(listener);
        keyboardHook.shutdownHook();
    }

    public interface KeyListener {
        void keyPressed(GlobalKeyEvent e);

        void keyReleased(GlobalKeyEvent e);
    }
}
