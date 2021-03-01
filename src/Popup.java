import javax.swing.*;

public class Popup {
    public static String dropDown(String title, String message, String[] choice) {
        return (String) JOptionPane.showInputDialog(null, message, title, JOptionPane.PLAIN_MESSAGE, null, choice, choice[0]);
    }

    public static String dropDown(String title, String message, String[] choice, String preseleted) {
        return (String) JOptionPane.showInputDialog(null, message, title, JOptionPane.PLAIN_MESSAGE, null, choice, preseleted);
    }

    public static String input(String message, String pretext) {
        return (String) JOptionPane.showInputDialog(null, message, pretext);
    }

    public static void message(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.PLAIN_MESSAGE);
    }

    public static void warning(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
    }

    public static void error(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static int selectButton(String title, String message, String[] choice) {
        return JOptionPane.showOptionDialog(null, message, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, choice, choice[0]);
    }
}
