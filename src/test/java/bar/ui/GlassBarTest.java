package bar.ui;

import bar.util.GlobalKeyListener;
import lc.kra.system.keyboard.event.GlobalKeyEvent;

public class GlassBarTest {

    public static void main(String[] args) {
        GlobalKeyListener keyListener = new GlobalKeyListener();
        keyListener.addListener(new GlobalKeyListener.KeyListener() {
            @Override
            public void keyPressed(GlobalKeyEvent e) {
                System.out.println(e.getVirtualKeyCode());
            }

            @Override
            public void keyReleased(GlobalKeyEvent e) {

            }
        });
        keyListener.activate();
        GlassBar glassBar = new GlassBar(800, 80);
        glassBar.setVisible(true);
        glassBar.setAllowInput(true);
        glassBar.addInputListener(System.out::println);
    }
}
