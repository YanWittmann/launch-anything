import Blur.GaussianFilter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;

public class LaunchBar extends JFrame {

    private final Main main;

    public LaunchBar(Main main) {
        this.main = main;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
            robot = null;
        }
        createBar();
    }

    public void activate() {
        System.out.println("Activate bar");
        updateBar(true);
    }

    public void deactivate() {
        System.out.println("Deactivate bar");
        this.setVisible(false);
    }

    private int lastCharacter = 0;

    public void characterTyped(int typed) {
        if (!this.isVisible()) return;
        char c = (char) typed;
        System.out.println("Character typed: " + typed + " " + c);
        boolean append = false, updateLastCharacter = true, updateFrame = false;
        if (lastCharacter == 16) {
            append = true;
            updateLastCharacter = false;
            switch (typed) {
                case 49 -> c = '!';
                case 50 -> c = '\"';
                case 51 -> c = '§';
                case 52 -> c = '$';
                case 53 -> c = '%';
                case 54 -> c = '&';
                case 55 -> c = '/';
                case 56 -> c = '(';
                case 57 -> c = ')';
                case 48 -> c = '=';
                case 219 -> c = '?';
                case 187 -> c = '*';
                case 191 -> c = '\'';
                case 189 -> c = '_';
                case 190 -> c = ':';
                case 188 -> c = ';';
                default -> {
                    if (typed == 16) return;
                    lastCharacter = typed;
                    characterTyped(typed);
                    return;
                }
            }
        } else if (typed == 13) { //enter
            main.executeResultsTile(0);
            return;
        } else if (typed == 40) { //up
            new Thread(() -> main.scrollResults(1)).start();
            return;
        } else if (typed == 38) { //down
            new Thread(() -> main.scrollResults(-1)).start();
            return;
        } else if (typed == 219) {
            append = true;
            c = '?';
        } else if (typed == 186) {
            append = true;
            c = 'Ü';
        } else if (typed == 222) {
            append = true;
            c = 'Ä';
        } else if (typed == 192) {
            append = true;
            c = 'Ö';
        } else if (typed == 189) {
            append = true;
            c = '-';
        } else if (typed == 190) {
            append = true;
            c = '.';
        } else if (typed == 188) {
            append = true;
            c = ',';
        } else if (typed == 226) {
            append = true;
            c = '<';
        } else if (typed == 191) {
            append = true;
            c = '#';
        } else if (typed == 187) {
            append = true;
            c = '+';
        } else if (Character.isAlphabetic(c) || Character.isDigit(c)) append = true;
        else if (c == ' ') append = true;
        else if (typed == 8 && inputField.getText().length() > 1) {
            inputField.setText(inputField.getText().substring(0, inputField.getText().length() - 1));
            updateFrame = true;
        }
        if (append) {
            inputField.setText(inputField.getText() + c);
        }
        if (inputField.getText().length() % 5 == 0) updateFrame = true;
        if (updateFrame && updateLastCharacter) {
            inputField.setVisible(false);
            inputField.invalidate();
            inputField.validate();
            inputField.revalidate();
            inputField.setVisible(true);
            this.recalculateBackground = false;
            this.setVisible(false);
            this.setVisible(true);
        }
        if (updateLastCharacter) lastCharacter = typed;
        if (inputField.getText().length() > 1) new Thread(() -> main.search(inputField.getText().trim())).start();
        else main.resetSearch();

    }

    public final static GaussianFilter filter = new GaussianFilter(4);
    public final static GaussianFilter filter_small = new GaussianFilter(2);
    //public final static UnsharpFilter filter = new UnsharpFilter();
    private static Robot robot;
    public final static Rectangle screenRectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    public static Rectangle barRectangle;

    public static BufferedImage getScreenshotImage() {
        return robot.createScreenCapture(screenRectangle);
    }

    public static BufferedImage blurImage(BufferedImage image) {
        return filter.filter(image, image);
    }

    public static BufferedImage blurImageSmall(BufferedImage image) {
        return filter_small.filter(image, image);
    }

    private static int xSize, ySize;
    private BufferedImage oldBackground;
    private boolean recalculateBackground = true;
    private JPanel contentPane;
    private Color average = new Color(255, 255, 255);
    private JTextField inputField;

    private void updateBar(boolean recalculateBackground) {
        this.recalculateBackground = recalculateBackground;
        this.setVisible(false);
        this.setVisible(true);
        inputField.setText(" ");
        inputField.setForeground(average);
        inputField.setVisible(false);
        inputField.invalidate();
        inputField.validate();
        inputField.revalidate();
        inputField.setVisible(true);
        try {
            Point mouse = MouseInfo.getPointerInfo().getLocation();
            click((int) barRectangle.getX() + 10, (int) barRectangle.getY() + 10);
            goTo((int) mouse.getX(), (int) mouse.getY());
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private void createBar() {
        xSize = 800;
        ySize = 80;

        this.setSize(xSize, ySize);
        this.setTitle("LaunchAnything");
        this.setAlwaysOnTop(true);
        this.setUndecorated(true);
        this.setLocationRelativeTo(null);
        this.setLocation(this.getX(), screenRectangle.height / 6);
        barRectangle = new Rectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());

        contentPane = new JPanel(null) {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                BufferedImage background;
                if (recalculateBackground) {
                    background = getScreenshotImage();
                    background = cropImage(background, barRectangle);
                    background = darken(background, .9f);
                    average = averageColor(background);
                    background = blurImage(background);
                    background = makeRoundedCorner(background, 20);
                    oldBackground = background;
                } else background = oldBackground;
                g2.drawImage(background, 0, 0, barRectangle.width, barRectangle.height, null);
                if (recalculateBackground)
                    if (average.getRed() + average.getGreen() + average.getBlue() > 300)
                        average = LaunchBarResult.BLACK;
                    else average = LaunchBarResult.WHITE;
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
        inputField.setBounds(10, 20, (int) barRectangle.getWidth() - 60, (int) barRectangle.getHeight() - 40);
        inputField.setBackground(new Color(0, 0, 0, 0));
        inputField.setBorder(null);
        inputField.setText(" ");
        inputField.setFont(new Font("Monospaced", Font.BOLD, 36));
        inputField.setCaretColor(new Color(0, 0, 0, 0));
        inputField.setVisible(true);
        contentPane.add(inputField);

        this.setType(javax.swing.JFrame.Type.UTILITY);
        this.setContentPane(contentPane);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.pack();
        this.setVisible(false);
    }

    public static int getxSize() {
        return xSize;
    }

    public static int getySize() {
        return ySize;
    }

    public static void click(int x, int y) throws AWTException {
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    public static void goTo(int x, int y) throws AWTException {
        robot.mouseMove(x, y);
    }

    public static void saveBufferedImage(BufferedImage bufferedImage) {
        File outputfile = new File("image.jpg");
        try {
            ImageIO.write(bufferedImage, "jpg", outputfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BufferedImage cropImage(BufferedImage src, Rectangle rect) {
        return src.getSubimage((int) rect.getX(), (int) rect.getY(), rect.width, rect.height);
    }

    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = output.createGraphics();

        // This is what we want, but it only does hard-clipping, i.e. aliasing
        // g2.setClip(new RoundRectangle2D ...)

        // so instead fake soft-clipping by first drawing the desired clip shape
        // in fully opaque white with antialiasing enabled...
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

        // ... then compositing the image on top,
        // using the white shape from above as alpha source
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, null);

        g2.dispose();

        return output;
    }

    public static Color averageColor(BufferedImage bi, Rectangle rectangle) {
        int x1 = (int) (rectangle.getX() + rectangle.getWidth());
        int y1 = (int) (rectangle.getY() + rectangle.getHeight());
        long sumr = 0, sumg = 0, sumb = 0;
        for (int x = (int) rectangle.getX(); x < x1; x++) {
            for (int y = (int) rectangle.getY(); y < y1; y++) {
                Color pixel = new Color(bi.getRGB(x, y));
                sumr += pixel.getRed();
                sumg += pixel.getGreen();
                sumb += pixel.getBlue();
            }
        }
        int num = (int) (rectangle.getWidth() * rectangle.getHeight());
        return new Color((int) (sumr / toF(num)), (int) (sumg / toF(num)), (int) (sumb / toF(num)));
    }

    public static Color averageColor(BufferedImage bi) {
        int x1 = bi.getWidth();
        int y1 = bi.getHeight();
        long sumr = 0, sumg = 0, sumb = 0;
        for (int x = 0; x < x1; x++) {
            for (int y = 0; y < y1; y++) {
                Color pixel = new Color(bi.getRGB(x, y));
                sumr += pixel.getRed();
                sumg += pixel.getGreen();
                sumb += pixel.getBlue();
            }
        }
        int num = (int) (bi.getWidth() * bi.getHeight());
        return new Color((int) (sumr / toF(num)), (int) (sumg / toF(num)), (int) (sumb / toF(num)));
    }

    public static BufferedImage darken(BufferedImage image, float amount) {
        RescaleOp op = new RescaleOp(amount, 0, null);
        return op.filter(image, null);
    }

    public static Color hex2Rgb(String colorStr) {
        try {
            return new Color(
                    Integer.valueOf(colorStr.substring(1, 3), 16),
                    Integer.valueOf(colorStr.substring(3, 5), 16),
                    Integer.valueOf(colorStr.substring(5, 7), 16));
        } catch (Exception e) {
            return new Color(0, 0, 0);
        }
    }

    public static float toF(int i) {
        return Float.parseFloat("" + i);
    }
}
