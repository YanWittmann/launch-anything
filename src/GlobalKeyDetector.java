import lc.kra.system.keyboard.GlobalKeyboardHook;
import lc.kra.system.keyboard.event.GlobalKeyAdapter;
import lc.kra.system.keyboard.event.GlobalKeyEvent;

public class GlobalKeyDetector {
    GlobalKeyboardHook keyboardHook = new GlobalKeyboardHook(true);

    public GlobalKeyDetector() {
        /*for (Map.Entry<Long, String> keyboard : GlobalKeyboardHook.listKeyboards().entrySet()) {
            System.out.format("%d: %s\n", keyboard.getKey(), keyboard.getValue());
        }*/
        keyboardHook.addKeyListener(new GlobalKeyAdapter() {

            @Override
            public void keyPressed(GlobalKeyEvent event) {
                keyDetected(event);
            }

            @Override
            public void keyReleased(GlobalKeyEvent event) {
                //System.out.println(event);
            }
        });
    }

    public void keyDetected(GlobalKeyEvent event) {
    }
}