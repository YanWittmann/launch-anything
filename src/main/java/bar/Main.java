package bar;

import bar.logic.BarManager;
import bar.logic.Settings;
import bar.util.Util;

public class Main {

    public static void main(String[] args) {
        Util.registerFont("font/Comfortaa-Regular.ttf");
        Settings settings = new Settings();
        BarManager barManager = new BarManager(settings);
    }
}
