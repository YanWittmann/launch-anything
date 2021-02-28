import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class LaunchBarResult extends JFrame {

    private final Main main;
    private final int index;

    public LaunchBarResult(Main main, int index) {
        this.main = main;
        this.index = index;
        createBar();
    }

    public void deactivate() {
        if(!this.isVisible()) return;
        System.out.println("Deactivate results bar " + index);
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

    private void updateBar() {
        this.setVisible(false);
        this.setVisible(true);
        inputField.setText(" ");
        inputField.setForeground(average);
        inputField.setVisible(false);
        inputField.invalidate();
        inputField.validate();
        inputField.revalidate();
        inputField.setVisible(true);
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

        contentPane = new JPanel(null) {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                BufferedImage background;
                if (recalculateBackground) {
                    background = LaunchBar.getScreenshotImage();
                    background = LaunchBar.cropImage(background, barRectangle);
                    background = LaunchBar.darken(background, .9f);
                    average = LaunchBar.averageColor(background);
                    background = LaunchBar.blurImageSmall(background);
                    background = LaunchBar.makeRoundedCorner(background, 20);
                    oldBackground = background;
                } else background = oldBackground;
                g2.drawImage(background, 0, 0, barRectangle.width, barRectangle.height, null);
                if (tile != null) {
                    Color overlayGradientColor = main.getColorForCategory(tile.getCategory());
                    int width = 18 + Math.min(barRectangle.width - 100, Math.min(tile.getLabel().length(), 40) * 18);
                    GradientPaint gradient = new GradientPaint(3, 0, overlayGradientColor, width, 0, TRANSPARENT, false);
                    g2.setPaint(gradient);
                    g2.fillRect(3, 3, width, barRectangle.height - 6);
                }
                if (recalculateBackground)
                    if (average.getRed() + average.getGreen() + average.getBlue() > 300)
                        average = BLACK;
                    else average = WHITE;
                g2.setColor(average);
                g2.setStroke(new BasicStroke(4));
                g2.drawRoundRect(1, 1, (int) barRectangle.getWidth() - 2, (int) barRectangle.getHeight() - 2, 20, 20);
                g2.drawRoundRect(1, 1, (int) barRectangle.getWidth() - 2, (int) barRectangle.getHeight() - 2, 20, 20);
                g2.dispose();
            }
        };
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

        this.setType(Type.UTILITY);
        this.setContentPane(contentPane);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.pack();
        this.setVisible(false);
    }

    public final static Color TRANSPARENT = new Color(0, 0, 0, 0);
    public final static Color WHITE = new Color(255, 255, 255);
    public final static Color BLACK = new Color(0, 0, 0);
}
