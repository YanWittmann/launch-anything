package bar.ui;

import bar.blur.GaussianFilter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GlassBar extends JFrame {

    private final static int BORDER_RADIUS = 12;
    private final static int BORDER_THICKNESS = 2;
    private static final float BACKGROUND_BLEND_FACTOR_FOR_DARK_MODE = 0.3f;
    private static final float BACKGROUND_BLEND_FACTOR_FOR_BRIGHT_MODE = 0.7f;
    private final static Color BLUR_COLOR_FOR_DARK_MODE = new Color(45, 48, 52);
    private final static Color BLUR_COLOR_FOR_BRIGHT_MODE = new Color(197, 197, 197);
    private final static Color TEXT_COLOR_FOR_DARK_MODE = new Color(220, 220, 220);
    private final static Color TEXT_COLOR_FOR_BRIGHT_MODE = new Color(31, 31, 31);
    private final static TextBubbleBorder ROUNDED_LINE_BORDER_FOR_DARK_MODE = new TextBubbleBorder(new Color(177, 182, 183), BORDER_THICKNESS, BORDER_RADIUS, 0, false);
    private final static TextBubbleBorder ROUNDED_LINE_BORDER_FOR_BRIGHT_MODE = new TextBubbleBorder(new Color(100, 100, 100), BORDER_THICKNESS, BORDER_RADIUS, 0, false);
    public final static Rectangle SCREEN_RECTANGLE = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    public final static GaussianFilter BACKGROUND_BLUR_FILTER = new GaussianFilter(30);

    public static Rectangle barRectangle;
    private final JTextField inputField;
    private final JLabel backgroundImageLabel;
    private final JLabel frameBorderLabel;
    private boolean allowInput = true;

    private final List<InputListener> inputListeners = new ArrayList<>();

    public GlassBar(int width, int height) {
        // set the general properties of the frame
        this.setSize(width, height);
        this.setTitle("Launch Anything");
        this.setAlwaysOnTop(true);
        this.setUndecorated(true);
        this.setLocationRelativeTo(null);
        this.setLocation(this.getX(), SCREEN_RECTANGLE.height / 6);
        barRectangle = new Rectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
        JPanel contentPane = new JPanel(null);
        contentPane.setPreferredSize(new Dimension(width, height));

        // configure the background to be transparent
        contentPane.setBackground(new Color(0, 0, 0, 0));
        contentPane.setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));

        // add the content pane to the frame
        // start with the input field the user uses to enter the command
        inputField = new JTextField();
        inputField.setBounds(25, 20, (int) barRectangle.getWidth() - 60, (int) barRectangle.getHeight() - 40);
        inputField.setBackground(new Color(0, 0, 0, 0));
        inputField.setForeground(new Color(255, 255, 255));
        inputField.setCaretColor(new Color(0, 0, 0, 0));
        inputField.setSelectionColor(new Color(71, 76, 82));
        inputField.setSelectedTextColor(new Color(255, 255, 255));
        inputField.setFont(new Font("Monospaced", Font.BOLD, 36));
        inputField.setBorder(null);
        inputField.setVisible(true);
        inputField.setOpaque(false);
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                inputListeners.forEach(listener -> listener.onInput(inputField.getText()));
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                inputListeners.forEach(listener -> listener.onInput(inputField.getText()));
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                inputListeners.forEach(listener -> listener.onInput(inputField.getText()));
            }
        });
        contentPane.add(inputField);

        // add the frame border
        frameBorderLabel = new JLabel();
        frameBorderLabel.setBounds(0, 0, (int) barRectangle.getWidth(), (int) barRectangle.getHeight());
        frameBorderLabel.setBackground(new Color(0, 0, 0, 0));
        frameBorderLabel.setBorder(ROUNDED_LINE_BORDER_FOR_DARK_MODE);
        frameBorderLabel.setOpaque(false);
        contentPane.add(frameBorderLabel);

        // add the background image label, the image will be set later when actually opening the bar
        backgroundImageLabel = new JLabel();
        backgroundImageLabel.setBounds(0, 0, (int) barRectangle.getWidth(), (int) barRectangle.getHeight());
        backgroundImageLabel.setBackground(new Color(0, 0, 0, 0));
        backgroundImageLabel.setVisible(true);
        backgroundImageLabel.setOpaque(false);
        contentPane.add(backgroundImageLabel);

        // add the content pane to the frame and set other properties
        this.setType(javax.swing.JFrame.Type.UTILITY);
        this.setContentPane(contentPane);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.pack();
        this.setVisible(false);
    }

    public void setVisible(boolean visible) {
        if (visible) {
            BufferedImage screenshot = robot.createScreenCapture(barRectangle);
            screenshot = saturateImage(screenshot, 0.3f);
            BACKGROUND_BLUR_FILTER.filter(screenshot, screenshot);
            Color averageColor = averageColor(screenshot);
            if (averageColor.getRed() + averageColor.getGreen() + averageColor.getBlue() > 170) {
                frameBorderLabel.setBorder(ROUNDED_LINE_BORDER_FOR_BRIGHT_MODE);
                screenshot = overlayColor(screenshot, BLUR_COLOR_FOR_BRIGHT_MODE, BACKGROUND_BLEND_FACTOR_FOR_BRIGHT_MODE);
                inputField.setForeground(TEXT_COLOR_FOR_BRIGHT_MODE);
            } else {
                frameBorderLabel.setBorder(ROUNDED_LINE_BORDER_FOR_DARK_MODE);
                screenshot = overlayColor(screenshot, BLUR_COLOR_FOR_DARK_MODE, BACKGROUND_BLEND_FACTOR_FOR_DARK_MODE);
                inputField.setForeground(TEXT_COLOR_FOR_DARK_MODE);
            }
            screenshot = makeRoundedCorner(screenshot, BORDER_RADIUS + BORDER_THICKNESS + 4);
            backgroundImageLabel.setIcon(new ImageIcon(screenshot));
        }
        super.setVisible(visible);
        if (visible && allowInput) inputField.requestFocus();
    }

    private static BufferedImage overlayColor(BufferedImage image, Color color, float amount) {
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, amount));
        g.setColor(color);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
        return output;
    }

    private BufferedImage saturateImage(BufferedImage image, float saturation) {
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                float[] hsb = Color.RGBtoHSB(getRed(rgb), getGreen(rgb), getBlue(rgb), null);
                float hue = hsb[0];
                float brightness = hsb[2];
                int rgb2 = Color.HSBtoRGB(hue, saturation, brightness);
                output.setRGB(x, y, rgb2);
            }
        }
        return output;
    }

    public int getAlpha(int rgb) {
        return (rgb >> 24) & 0xFF;
    }

    public int getRed(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    public int getGreen(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    public int getBlue(int rgb) {
        return rgb & 0xFF;
    }

    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return output;
    }

    private static Color averageColor(BufferedImage bi) {
        int x1 = bi.getWidth();
        int y1 = bi.getHeight();
        long sumr = 0, sumg = 0, sumb = 0;
        int skipper = 0;
        for (int x = 0; x < x1; x++) {
            for (int y = 0; y < y1; y++) {
                if (++skipper > 1) {
                    skipper = 0;
                    continue;
                }
                Color pixel = new Color(bi.getRGB(x, y));
                sumr += pixel.getRed();
                sumg += pixel.getGreen();
                sumb += pixel.getBlue();
            }
        }
        int num = bi.getWidth() * bi.getHeight();
        return new Color((int) (sumr / toF(num)), (int) (sumg / toF(num)), (int) (sumb / toF(num)));
    }

    private static float toF(int i) {
        return Float.parseFloat("" + i);
    }

    public void setAllowInput(boolean allowInput) {
        this.allowInput = allowInput;
        inputField.setEditable(allowInput);
    }

    public void addInputListener(InputListener listener) {
        inputListeners.add(listener);
    }

    private Robot robot;

    {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public interface InputListener {
        void onInput(String input);
    }
}
