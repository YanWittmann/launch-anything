import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class LaunchBarNotification extends JFrame {

    public LaunchBarNotification(String text) {
        createBar(text);
        updateBar();
        fadeIn();
        fadeOut();
        System.out.println("Showing notification '" + text + "'");
    }

    private void fadeOut() {
        Thread opacity = new Thread(() -> {
            int currentOpacity = 100, startX = getX(), stepsDone = 0;
            double xPerStep = 30d / 50d;
            try {
                Thread.sleep(2500);
            } catch (Exception ignored) {
            }
            for (; currentOpacity > 0; currentOpacity -= 2) {
                try {
                    Thread.sleep(5);
                } catch (Exception ignored) {
                }
                stepsDone++;
                setLocation((int) (startX + (xPerStep * stepsDone)), getY());
                setOpacity(currentOpacity * 0.01f);
            }
            dispose();
        });
        opacity.start();
    }

    private void fadeIn() {
        Thread opacity = new Thread(() -> {
            int currentOpacity = 0;
            setOpacity(0f);
            this.setVisible(true);
            for (; currentOpacity <= 100; currentOpacity += 2) {
                try {
                    Thread.sleep(2);
                } catch (Exception ignored) {
                }
                setOpacity(currentOpacity * 0.01f);
            }
        });
        opacity.start();
    }

    private Rectangle barRectangle;

    private Color average = Color.WHITE;
    private JTextField notificationText;
    private JLabel backgroundImageLabel, frameBorderLabel;
    private final LaunchBar.TextBubbleBorder roundedLineBorderWhite = new LaunchBar.TextBubbleBorder(Color.WHITE, 4, 18, 0);
    private final LaunchBar.TextBubbleBorder roundedLineBorderBlack = new LaunchBar.TextBubbleBorder(Color.BLACK, 4, 18, 0);

    private void updateBar() {
        updateBackgroundImage();
        notificationText.setForeground(average);
        if (average.getBlue() == 255)
            frameBorderLabel.setBorder(roundedLineBorderWhite);
        else frameBorderLabel.setBorder(roundedLineBorderBlack);
        notificationText.setVisible(false);
        notificationText.invalidate();
        notificationText.validate();
        notificationText.revalidate();
        notificationText.setVisible(true);
    }

    private void createBar(String text) {
        int xSize = 40 + text.length() * 14, ySize = 50;
        this.setSize(xSize, ySize);
        this.setTitle("LaunchAnythingResult");
        this.setAlwaysOnTop(true);
        this.setUndecorated(true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        int taskBarSizeBottom = scnMax.bottom;
        int taskBarSizeRight = scnMax.right;
        setLocation(screenSize.width - taskBarSizeRight - getWidth() - 20, screenSize.height - taskBarSizeBottom - getHeight() - 20);
        barRectangle = new Rectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());

        JPanel contentPane = new JPanel(null);
        contentPane.setPreferredSize(new Dimension(xSize, ySize));
        contentPane.setBackground(new Color(0, 0, 0, 0));
        setBackground(new Color(0, 0, 0, 0));

        notificationText = new JTextField(text);
        notificationText.setBounds(20, 0, (int) barRectangle.getWidth(), (int) barRectangle.getHeight());
        notificationText.setBackground(new Color(0, 0, 0, 0));
        notificationText.setBorder(null);
        notificationText.setFont(new Font("Monospaced", Font.BOLD, 24));
        notificationText.setCaretColor(new Color(0, 0, 0, 0));
        notificationText.setVisible(true);
        contentPane.add(notificationText);

        frameBorderLabel = new JLabel();
        frameBorderLabel.setBounds(0, 0, (int) barRectangle.getWidth(), (int) barRectangle.getHeight());
        frameBorderLabel.setBackground(new Color(0, 0, 0, 0));
        frameBorderLabel.setBorder(roundedLineBorderWhite);
        contentPane.add(frameBorderLabel);

        backgroundImageLabel = new JLabel();
        backgroundImageLabel.setBounds(0, 0, (int) barRectangle.getWidth(), (int) barRectangle.getHeight());
        backgroundImageLabel.setBackground(new Color(0, 0, 0, 0));
        backgroundImageLabel.setVisible(true);
        contentPane.add(backgroundImageLabel);

        this.setType(Type.UTILITY);
        this.setContentPane(contentPane);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.pack();
        this.setVisible(false);
    }

    private void updateBackgroundImage() {
        BufferedImage background = LaunchBar.getScreenshotImage();
        background = LaunchBar.cropImage(background, barRectangle);
        background = LaunchBar.darken(background, .9f);
        average = LaunchBar.averageColor(background);
        background = LaunchBar.blurImage(background);
        background = LaunchBar.makeRoundedCorner(background, 20);
        backgroundImageLabel.setIcon(new ImageIcon(background));
        if (average.getRed() + average.getGreen() + average.getBlue() > 300)
            average = LaunchBarNotification.BLACK;
        else average = LaunchBarNotification.WHITE;
    }

    public final static Color WHITE = new Color(255, 255, 255);
    public final static Color BLACK = new Color(0, 0, 0);
}
