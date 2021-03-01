import Blur.GaussianFilter;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

public class LaunchBar extends JFrame {

    private final Main main;

    public LaunchBar(Main main) {
        this.main = main;
        createBar();
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");
        this.getContentPane().setCursor(blankCursor);
    }

    public void activate() {
        System.out.println("Activate bar");
        updateBackgroundImage();
        updateBar();
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
        boolean append = false, updateLastCharacter = true;
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
        else if (typed == 8 && inputField.getText().length() > 1) { //backspace
            inputField.setText(inputField.getText().substring(0, inputField.getText().length() - 1));
            getFocus();
        }
        if (append) {
            inputField.setText(inputField.getText() + c);
            getFocus();
        }
        if (updateLastCharacter) lastCharacter = typed;
        if (inputField.getText().length() > 1) new Thread(() -> main.search(inputField.getText().trim())).start();
        else main.resetSearch();

        inputField.setVisible(false);
        inputField.setVisible(true);

    }

    public final static GaussianFilter filter = new GaussianFilter(4);
    public final static GaussianFilter filter_small = new GaussianFilter(3);
    //public final static UnsharpFilter filter = new UnsharpFilter();
    private static Robot robot;
    public final static Rectangle screenRectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    public static Rectangle barRectangle;

    public static BufferedImage getScreenshotImage() {
        return getRobot().createScreenCapture(screenRectangle);
    }

    public static BufferedImage blurImage(BufferedImage image) {
        return filter.filter(image, image);
    }

    public static BufferedImage blurImageSmall(BufferedImage image) {
        return filter_small.filter(image, image);
    }

    private static int xSize, ySize;
    private BufferedImage oldBackground;
    private JPanel contentPane;
    private Color average = new Color(255, 255, 255);
    private JTextField inputField;
    private JLabel backgroundImageLabel, frameBorderLabel;
    private LaunchBar.TextBubbleBorder roundedLineBorderWhite = new LaunchBar.TextBubbleBorder(Color.WHITE, 4, 18, 0);
    private LaunchBar.TextBubbleBorder roundedLineBorderBlack = new LaunchBar.TextBubbleBorder(Color.BLACK, 4, 18, 0);

    private void updateBar() {
        inputField.setText(" ");
        inputField.setForeground(average);
        if (average.getBlue() == 255)
            frameBorderLabel.setBorder(roundedLineBorderWhite);
        else frameBorderLabel.setBorder(roundedLineBorderBlack);
        inputField.setVisible(false);
        inputField.invalidate();
        inputField.validate();
        inputField.revalidate();
        inputField.setVisible(true);
        this.setVisible(false);
        this.setVisible(true);
        getFocus();
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

        contentPane = new JPanel(null);
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


        this.setType(javax.swing.JFrame.Type.UTILITY);
        this.setContentPane(contentPane);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.pack();
        this.setVisible(false);
    }

    private void updateBackgroundImage() {
        BufferedImage background = getScreenshotImage();
        background = cropImage(background, barRectangle);
        background = darken(background, .9f);
        average = averageColor(background);
        background = blurImage(background);
        background = makeRoundedCorner(background, 20);
        backgroundImageLabel.setIcon(new ImageIcon(background));
        if (average.getRed() + average.getGreen() + average.getBlue() > 300)
            average = LaunchBarResult.BLACK;
        else average = LaunchBarResult.WHITE;
        oldBackground = background;
    }

    public static void getFocus() {
        try {
            Point mouse = MouseInfo.getPointerInfo().getLocation();
            click((int) barRectangle.getX() + 10, (int) barRectangle.getY());
            goTo((int) mouse.getX(), (int) mouse.getY());
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static void click(int x, int y) throws AWTException {
        getRobot().mouseMove(x, y);
        getRobot().mousePress(InputEvent.BUTTON1_DOWN_MASK);
        getRobot().mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    public static void goTo(int x, int y) throws AWTException {
        getRobot().mouseMove(x, y);
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

    static class TextBubbleBorder extends AbstractBorder {

        private Color color;
        private int thickness = 4;
        private int radii = 8;
        private int pointerSize = 7;
        private Insets insets = null;
        private BasicStroke stroke = null;
        private int strokePad;
        private int pointerPad = 4;
        private boolean left = true;
        RenderingHints hints;

        TextBubbleBorder(Color color) {
            this(color, 4, 8, 7);
        }

        TextBubbleBorder(Color color, int thickness, int radii, int pointerSize) {
            this.thickness = thickness;
            this.radii = radii;
            this.pointerSize = pointerSize;
            this.color = color;

            stroke = new BasicStroke(thickness);
            strokePad = thickness / 2;

            hints = new RenderingHints(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int pad = radii + strokePad;
            int bottomPad = pad + pointerSize + strokePad;
            insets = new Insets(pad, pad, bottomPad, pad);
        }

        TextBubbleBorder(Color color, int thickness, int radii, int pointerSize, boolean left) {
            this(color, thickness, radii, pointerSize);
            this.left = left;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return insets;
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            return getBorderInsets(c);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {

            Graphics2D g2 = (Graphics2D) g;

            int bottomLineY = height - thickness - pointerSize;

            RoundRectangle2D.Double bubble = new RoundRectangle2D.Double(
                    0 + strokePad,
                    0 + strokePad,
                    width - thickness,
                    bottomLineY,
                    radii,
                    radii);

            Polygon pointer = new Polygon();

            if (left) {
                // left point
                pointer.addPoint(
                        strokePad + radii + pointerPad,
                        bottomLineY);
                // right point
                pointer.addPoint(
                        strokePad + radii + pointerPad + pointerSize,
                        bottomLineY);
                // bottom point
                pointer.addPoint(
                        strokePad + radii + pointerPad + (pointerSize / 2),
                        height - strokePad);
            } else {
                // left point
                pointer.addPoint(
                        width - (strokePad + radii + pointerPad),
                        bottomLineY);
                // right point
                pointer.addPoint(
                        width - (strokePad + radii + pointerPad + pointerSize),
                        bottomLineY);
                // bottom point
                pointer.addPoint(
                        width - (strokePad + radii + pointerPad + (pointerSize / 2)),
                        height - strokePad);
            }

            Area area = new Area(bubble);
            area.add(new Area(pointer));

            g2.setRenderingHints(hints);

            // Paint the BG color of the parent, everywhere outside the clip
            // of the text bubble.
            Component parent = c.getParent();
            if (parent != null) {
                Color bg = parent.getBackground();
                Rectangle rect = new Rectangle(0, 0, width, height);
                Area borderRegion = new Area(rect);
                borderRegion.subtract(area);
                g2.setClip(borderRegion);
                g2.setColor(bg);
                g2.fillRect(0, 0, width, height);
                g2.setClip(null);
            }

            g2.setColor(color);
            g2.setStroke(stroke);
            g2.draw(area);
        }
    }

    private static Robot getRobot() {
        if (robot == null)
            try {
                robot = new Robot();
            } catch (AWTException e) {
                e.printStackTrace();
                robot = null;
            }
        return robot;
    }
}