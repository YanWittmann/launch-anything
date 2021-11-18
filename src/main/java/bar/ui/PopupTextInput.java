package bar.ui;

import bar.util.Sleep;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PopupTextInput extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField textField;
    private JLabel messageLabel;

    public PopupTextInput(String title, String message, String pretext) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        this.setTitle(title);
        messageLabel.setText(message);
        textField.setText(pretext);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);

        this.setAlwaysOnTop(true);
    }

    private boolean cancelled = false;

    private void onOK() {
        cancelled = false;
        dispose();
    }

    private void onCancel() {
        cancelled = true;
        dispose();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public String getText() {
        return textField.getText();
    }
}
