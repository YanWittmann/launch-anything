import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

public class LaunchBarResult extends JFrame {

    private final Main main;
    private final int index;

    public LaunchBarResult(Main main, int index) {
        this.main = main;
        this.index = index;
        createBar();
    }

    public void deactivate() {
        if (!this.isVisible()) return;
        System.out.println("Deactivate results bar " + index);
        firstTimeShowResult = true;
        this.setVisible(false);
    }

    public void prepareNow() {
        recalculateBackground = true;
        System.out.println("Preparing results bar " + index);
        this.setVisible(false);
        this.setVisible(true);
        this.setVisible(false);
        recalculateBackground = false;
    }

    private Tile tile;
    private boolean firstTimeShowResult = true;

    public void setResult(Tile tile) {
        this.tile = tile;
        System.out.println("Display results bar " + index);
        updateBar();
        inputField.setText(tile.getLabel());
    }

    public Tile getTile() {
        return tile;
    }

    private final Rectangle mainBarRectangle = LaunchBar.barRectangle;
    private Rectangle barRectangle;

    private BufferedImage oldBackground;
    private boolean recalculateBackground = true;
    private JPanel contentPane;
    private Color average = new Color(255, 255, 255);
    private JTextField inputField;
    private JLabel backgroundImageLabel, frameBorderLabel;

    private void updateBar() {
        if (firstTimeShowResult)
            updateBackgroundImage();
        updateBackgroundImageColor();
        firstTimeShowResult = false;
        inputField.setText(" ");
        inputField.setForeground(average);
        inputField.setVisible(false);
        inputField.invalidate();
        inputField.validate();
        inputField.revalidate();
        inputField.setVisible(true);
        this.setVisible(true);
    }

    private void createBar() {
        int xSize = main.getLauncherBarXsize() - 20, ySize = main.getLauncherBarYsize() - 20;
        this.setSize(xSize, ySize);
        this.setTitle("LaunchAnythingResult");
        this.setAlwaysOnTop(true);
        this.setUndecorated(true);
        this.setLocationRelativeTo(null);
        barRectangle = new Rectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
        this.setLocation(this.getX(), (int) ((int) mainBarRectangle.getY() + main.getDistanceToMainBar() + 20 + ((barRectangle.getHeight() + main.getDistanceBetweenResults()) * (index + 1))));
        barRectangle = new Rectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());

        contentPane = new JPanel(null);
        contentPane.setPreferredSize(new Dimension(xSize, ySize));
        contentPane.setBackground(new Color(0, 0, 0, 0));
        setBackground(new Color(0, 0, 0, 0));

        inputField = new JTextField();
        inputField.setBounds(20, 0, (int) barRectangle.getWidth() - 60, (int) barRectangle.getHeight());
        inputField.setBackground(new Color(0, 0, 0, 0));
        inputField.setBorder(null);
        inputField.setText(" ");
        inputField.setFont(new Font("Monospaced", Font.BOLD, 30));
        inputField.setCaretColor(new Color(0, 0, 0, 0));
        inputField.setVisible(true);
        inputField.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                main.executeResultsTile(index);
            }
        });
        contentPane.add(inputField);

        LaunchBar.TextBubbleBorder roundedLineBorder = new LaunchBar.TextBubbleBorder(Color.WHITE, 4, 18, 0);
        frameBorderLabel = new JLabel();
        frameBorderLabel.setBounds(0, 0, (int) barRectangle.getWidth(), (int) barRectangle.getHeight());
        frameBorderLabel.setBackground(new Color(0, 0, 0, 0));
        frameBorderLabel.setBorder(roundedLineBorder);
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

    private void updateBackgroundImageColor() {
        if (tile != null) {
            BufferedImage updated = deepCopy(oldBackground);
            Color overlayGradientColor = main.getColorForCategory(tile.getCategory());
            int width = 18 + Math.min(barRectangle.width - 100, Math.min(tile.getLabel().length(), 40) * 18);
            GradientPaint gradient = new GradientPaint(3, 0, overlayGradientColor, width, 0, TRANSPARENT, false);
            Graphics2D g2 = (Graphics2D) updated.getGraphics();
            g2.setPaint(gradient);
            g2.fillRect(3, 3, width, barRectangle.height - 6);
            backgroundImageLabel.setIcon(new ImageIcon(updated));
        }
    }
    private void updateBackgroundImage() {
        BufferedImage background = LaunchBar.getScreenshotImage();
        background = LaunchBar.cropImage(background, barRectangle);
        background = LaunchBar.darken(background, .9f);
        average = LaunchBar.averageColor(background);
        background = LaunchBar.blurImageSmall(background);
        background = LaunchBar.makeRoundedCorner(background, 20);
        backgroundImageLabel.setIcon(new ImageIcon(background));
        if (average.getRed() + average.getGreen() + average.getBlue() > 300)
            average = LaunchBarResult.BLACK;
        else average = LaunchBarResult.WHITE;
        oldBackground = background;
    }

    public static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    public final static Color TRANSPARENT = new Color(0, 0, 0, 0);
    public final static Color WHITE = new Color(255, 255, 255);
    public final static Color BLACK = new Color(0, 0, 0);
}
