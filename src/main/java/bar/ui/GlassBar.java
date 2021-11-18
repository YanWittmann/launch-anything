package bar.ui;

import bar.blur.GaussianFilter;
import bar.logic.Settings;
import bar.util.ImageUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GlassBar extends JFrame {

    private final static int BORDER_RADIUS = 12;
    private final static int BORDER_THICKNESS = 2;
    private static final float BACKGROUND_TINT_FACTOR_FOR_DARK_MODE = 0.05f;
    private static final float BACKGROUND_TINT_FACTOR_FOR_BRIGHT_MODE = 0.1f;
    private static final float BACKGROUND_BLEND_FACTOR_FOR_DARK_MODE = 0.6f;
    private static final float BACKGROUND_BLEND_FACTOR_FOR_BRIGHT_MODE = 0.6f;
    private final static Color BLUR_COLOR_FOR_DARK_MODE = new Color(45, 48, 52);
    private final static Color BLUR_COLOR_FOR_BRIGHT_MODE = new Color(255, 255, 255);
    private final static Color TEXT_COLOR_FOR_DARK_MODE = new Color(220, 220, 220);
    private final static Color TEXT_COLOR_FOR_BRIGHT_MODE = new Color(31, 31, 31);
    private final static TextBubbleBorder ROUNDED_LINE_BORDER_FOR_DARK_MODE = new TextBubbleBorder(new Color(177, 182, 183), BORDER_THICKNESS, BORDER_RADIUS, 0, false);
    private final static TextBubbleBorder ROUNDED_LINE_BORDER_FOR_BRIGHT_MODE = new TextBubbleBorder(new Color(100, 100, 100), BORDER_THICKNESS, BORDER_RADIUS, 0, false);
    public final static Rectangle SCREEN_RECTANGLE = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    public final static GaussianFilter BACKGROUND_BLUR_FILTER = new GaussianFilter(20);

    private final JPanel contentPane;
    private Rectangle barRectangle;
    private final JTextField inputField;
    private final JLabel backgroundImageLabel;
    private final JLabel frameBorderLabel;
    private boolean allowInput = true;

    private final List<InputListener> inputListeners = new ArrayList<>();

    public GlassBar() {
        // set the general properties of the frame
        this.setSize(10, 10);
        this.setTitle("Launch Anything");
        this.setAlwaysOnTop(true);
        this.setUndecorated(true);
        contentPane = new JPanel(null);

        // configure the background to be transparent
        contentPane.setBackground(new Color(0, 0, 0, 0));
        contentPane.setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));

        // add the content pane to the frame
        // start with the input field the user uses to enter the command
        inputField = new JTextField();
        inputField.setBounds(25, 0, 10, 10);
        inputField.setBackground(new Color(0, 0, 0, 0));
        inputField.setForeground(new Color(255, 255, 255));
        inputField.setCaretColor(new Color(0, 0, 0, 0));
        inputField.setSelectionColor(new Color(71, 76, 82));
        inputField.setSelectedTextColor(new Color(255, 255, 255));
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
        frameBorderLabel.setBounds(0, 0, 10, 10);
        frameBorderLabel.setBackground(new Color(0, 0, 0, 0));
        frameBorderLabel.setBorder(ROUNDED_LINE_BORDER_FOR_DARK_MODE);
        frameBorderLabel.setOpaque(false);
        contentPane.add(frameBorderLabel);

        // add the background image label, the image will be set later when actually opening the bar
        backgroundImageLabel = new JLabel();
        backgroundImageLabel.setBounds(0, 0, 10, 10);
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
            updateBackground();
            inputField.setText("");
        }
        super.setVisible(visible);
        if (visible && allowInput) grabFocus();
    }

    public void setOnlyVisibility(boolean visible) {
        if (super.isVisible() != visible) super.setVisible(visible);
    }

    public void grabFocus() {
        Point mousePosition = MouseInfo.getPointerInfo().getLocation();
        robot.mouseMove(getX() + getWidth() - 3, getY() + getHeight() - 3);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseMove(mousePosition.x, mousePosition.y);
        inputField.requestFocus();
    }

    public void prepareUpdateBackground() {
        new Thread(this::updateBackground).start();
    }

    private BufferedImage lastBackgroundImage;
    private boolean isLightMode = true;

    public void updateBackground() {
        BufferedImage screenshot = robot.createScreenCapture(barRectangle);
        screenshot = ImageUtil.saturateImage(screenshot, 1.6f);
        BACKGROUND_BLUR_FILTER.filter(screenshot, screenshot);
        Color averageColor = ImageUtil.averageColor(screenshot);
        isLightMode = averageColor.getRed() + averageColor.getGreen() + averageColor.getBlue() > 170;
        if (isLightMode) {
            frameBorderLabel.setBorder(ROUNDED_LINE_BORDER_FOR_BRIGHT_MODE);
            screenshot = ImageUtil.overlayColor(screenshot, BLUR_COLOR_FOR_BRIGHT_MODE, BACKGROUND_BLEND_FACTOR_FOR_BRIGHT_MODE);
            inputField.setForeground(TEXT_COLOR_FOR_BRIGHT_MODE);
        } else {
            frameBorderLabel.setBorder(ROUNDED_LINE_BORDER_FOR_DARK_MODE);
            screenshot = ImageUtil.overlayColor(screenshot, BLUR_COLOR_FOR_DARK_MODE, BACKGROUND_BLEND_FACTOR_FOR_DARK_MODE);
            inputField.setForeground(TEXT_COLOR_FOR_DARK_MODE);
        }
        screenshot = ImageUtil.makeRoundedCorner(screenshot, BORDER_RADIUS + BORDER_THICKNESS + 5);
        lastBackgroundImage = screenshot;
        backgroundImageLabel.setIcon(new ImageIcon(screenshot));
    }

    public void tintBackground(Color color) {
        if (lastBackgroundImage != null) {
            BufferedImage tintedImage;
            if (isLightMode) {
                tintedImage = ImageUtil.overlayColor(lastBackgroundImage, color, BACKGROUND_TINT_FACTOR_FOR_BRIGHT_MODE);
            } else {
                tintedImage = ImageUtil.overlayColor(lastBackgroundImage, color, BACKGROUND_TINT_FACTOR_FOR_DARK_MODE);
            }
            backgroundImageLabel.setIcon(new ImageIcon(tintedImage));
        }
    }

    public void setAllowInput(boolean allowInput) {
        this.allowInput = allowInput;
        inputField.setEditable(allowInput);
    }

    public void setText(String text) {
        inputField.setText(text);
    }

    public void addInputListener(InputListener listener) {
        inputListeners.add(listener);
    }

    /**
     * Repositions the bar on the screen.<br>
     * The index determines the position of the bar on the screen:<ul>
     * <li>-2: notification</li>
     * <li>-1: input bar</li>
     * <li>0+: results bar</li>
     * </ul>
     *
     * @param index    Where to position the bar on the screen.
     * @param settings The settings to use for the bar.
     */
    public void setType(int index, Settings settings) {
        // first, using the index, the positioning of the bar is determined
        int fontSize;
        if (index == -2) {
            int width = settings.getInt(Settings.INPUT_WIDTH), height = settings.getInt(Settings.INPUT_HEIGHT);
            this.setSize(width, height);
            contentPane.setPreferredSize(new Dimension(width, height));
            this.setLocation((int) SCREEN_RECTANGLE.getWidth() - width - 10, (int) SCREEN_RECTANGLE.getHeight() - height - 10);
            fontSize = 15;
        } else if (index == -1) {
            int width = settings.getInt(Settings.INPUT_WIDTH), height = settings.getInt(Settings.INPUT_HEIGHT);
            this.setSize(width, height);
            contentPane.setPreferredSize(new Dimension(width, height));
            this.setLocationRelativeTo(null);
            this.setLocation(this.getX(), SCREEN_RECTANGLE.height / 6);
            fontSize = 36;

            // hide the cursor if it is on the frame
            this.setCursor(this.getToolkit().createCustomCursor(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "null"));
            backgroundImageLabel.setCursor(this.getToolkit().createCustomCursor(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "null"));
            frameBorderLabel.setCursor(this.getToolkit().createCustomCursor(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "null"));
            inputField.setCursor(this.getToolkit().createCustomCursor(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "null"));
        } else {
            int width = settings.getInt(Settings.RESULT_WIDTH), height = settings.getInt(Settings.RESULT_HEIGHT);
            this.setSize(width, height);
            contentPane.setPreferredSize(new Dimension(width, height));
            this.setLocationRelativeTo(null);
            this.setLocation(this.getX(), (SCREEN_RECTANGLE.height / 6) + ((index + 1) * (height + settings.getInt(Settings.RESULT_MARGIN))) + settings.getInt(Settings.INPUT_RESULT_DISTANCE));
            inputField.setSelectionColor(new Color(0, 0, 0, 0));
            inputField.setSelectedTextColor(TEXT_COLOR_FOR_DARK_MODE);
            fontSize = 30;
        }

        // set the size of the components on the bar
        barRectangle = new Rectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
        inputField.setBounds(fontSize - 5, 4, (int) barRectangle.getWidth() - fontSize - 5, (int) barRectangle.getHeight());
        inputField.setFont(new Font(settings.getString(Settings.BAR_FONT), Font.BOLD, fontSize));
        frameBorderLabel.setBounds(0, 0, (int) barRectangle.getWidth(), (int) barRectangle.getHeight());
        backgroundImageLabel.setBounds(0, 0, (int) barRectangle.getWidth(), (int) barRectangle.getHeight());
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
