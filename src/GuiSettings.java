import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

public class GuiSettings {
    private JPanel mainPanel;
    private JTabbedPane tabPane;
    private JTable tilesTable;
    private JTable categoriesTable;
    private JTable tileGeneratorsTable;
    private JButton createTileButton;
    private JButton editTileButton;
    private JButton removeTileButton;
    private JButton saveTilesButton;
    private JButton returnToBarButton;
    private JButton createTileGeneratorButton;
    private JButton removeTileGeneratorButton;
    private JButton saveTileGeneratorsButton;
    private JButton createCategoryButton;
    private JButton removeCategoryButton;
    private JButton saveCategoriesButton;
    private JButton saveGeneralButton;
    private JTextField generalWidth;
    private JRadioButton onRadioButton;
    private JRadioButton offRadioButton;
    private JButton clickMeButton;
    private JTextField generalHeight;
    private JTextField generalDistToResults;
    private JTextField generalDistBetweenResults;
    private JTextField generalMaxResults;
    private JTextField generalActivationKey;
    private JTextField generalDoubleClickMax;
    private Main main;
    private static GuiSettings self;

    public GuiSettings(Main main) {
        self = this;
        this.main = main;

        createTileButton.addActionListener(e -> createTileClicked());
        editTileButton.addActionListener(e -> editTileActionClicked());
        removeTileButton.addActionListener(e -> removeTileClicked());
        saveTilesButton.addActionListener(e -> saveTilesClicked(false));

        createTileGeneratorButton.addActionListener(e -> createTileGeneratorClicked());
        removeTileGeneratorButton.addActionListener(e -> removeTileGeneratorClicked());
        saveTileGeneratorsButton.addActionListener(e -> saveTileGeneratorsClicked(false));

        createCategoryButton.addActionListener(e -> createCategoryClicked());
        removeCategoryButton.addActionListener(e -> removeCategoryClicked());
        saveCategoriesButton.addActionListener(e -> saveCategoriesClicked(false));

        saveGeneralButton.addActionListener(e -> saveGeneralSettings(false));
        clickMeButton.addActionListener(e -> buyMeACoffee());
        onRadioButton.addActionListener(e -> offRadioButton.setSelected(false));
        offRadioButton.addActionListener(e -> onRadioButton.setSelected(false));
        generalActivationKey.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                generalActivationKey.setText("" + e.getKeyCode());
            }
        });

        returnToBarButton.addActionListener(e -> finishAndLeave());

        tilesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tileGeneratorsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        categoriesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

    public void initializeGeneralSettings() {
        generalWidth.setText(main.getConfigOrSetDefault("launcherBarXsize", "800"));
        generalHeight.setText(main.getConfigOrSetDefault("launcherBarYsize", "80"));
        generalDistToResults.setText(main.getConfigOrSetDefault("distanceToMainBar", "10"));
        generalDistBetweenResults.setText(main.getConfigOrSetDefault("distanceBetweenResults", "6"));
        generalMaxResults.setText(main.getConfigOrSetDefault("maxAmountResults", "8"));
        generalActivationKey.setText(main.getConfigOrSetDefault("activationKey", "17"));
        generalDoubleClickMax.setText(main.getConfigOrSetDefault("maxDoubleClickDuration", "300"));
        if (main.getAutostartState()) onRadioButton.setSelected(true);
        else offRadioButton.setSelected(true);
    }

    public void saveGeneralSettings(boolean silent) {
        main.setConfig("launcherBarXsize", generalWidth.getText());
        main.setConfig("launcherBarYsize", generalHeight.getText());
        main.setConfig("distanceToMainBar", generalDistToResults.getText());
        main.setConfig("distanceBetweenResults", generalDistBetweenResults.getText());
        main.setConfig("maxAmountResults", generalMaxResults.getText());
        main.setConfig("activationKey", generalActivationKey.getText());
        main.setConfig("maxDoubleClickDuration", generalDoubleClickMax.getText());
        main.setAutostart(onRadioButton.isSelected());
        if (!silent) Popup.message("LaunchBar", "Saved general settings!");
    }

    public static void buyMeACoffee() {
        try {
            Desktop.getDesktop().browse(URI.create("https://paypal.me/yanwittmann"));
        } catch (IOException e) {
            Popup.error("LaunchBar", "Unable to open https://paypal.me/yanwittmann\n" + e.toString());
            e.printStackTrace();
        }
    }

    private final static String[] TILES_COLUMN_NAMES = new String[]{"ID", "String ID", "Label", "Category", "Keywords", "Actions (Use button below to edit)", "Hidden", "Last executed"};
    private final static String[] TILE_GENERATORS_COLUMN_NAMES = new String[]{"ID", "Type", "Category", "Parameter 1", "Parameter 2", "Parameter 3", "Parameter 4"};
    private final static String[] CATEGORIES_COLUMN_NAMES = new String[]{"ID", "Category name", "Color"};

    private final static String[] CREATE_NEW_TILE_PRESET = new String[]{"None", "Open file", "Open URL", "Copy to clipboard", "Settings"};
    private final static String[] CREATE_NEW_TILE_GENERATOR_PRESET = new String[]{"None", "Music"};

    private ArrayList<Pair<String, String>> displayCategories;

    public void updateCategories(ArrayList<Pair<String, String>> categories) {
        displayCategories = categories;
        categoriesTable.setModel(new DefaultTableModel(new String[categories.size()][CATEGORIES_COLUMN_NAMES.length], CATEGORIES_COLUMN_NAMES));
        for (int i = 0; i < categories.size(); i++) {
            categoriesTable.setValueAt(i, i, 0);
            categoriesTable.setValueAt(categories.get(i).getLeft(), i, 1);
            categoriesTable.setValueAt(categories.get(i).getRight(), i, 2);
        }
        resizeColumnWidth(categoriesTable);
    }

    private void saveCategoriesClicked(boolean silent) {
        for (int i = 0; i < displayCategories.size(); i++) {
            displayCategories.get(i).setLeft(getValueAt(categoriesTable, i, 1));
            displayCategories.get(i).setRight(getValueAt(categoriesTable, i, 2));
        }
        main.setCategories(displayCategories);
        main.save();
        displayCategories.forEach(System.out::println);
        if (!silent) Popup.message("LaunchAnything", "Saved all categories!");
    }

    private void createCategoryClicked() {
        saveCategoriesClicked(true);
        displayCategories.add(new Pair<>("", ""));
        updateCategories(displayCategories);
    }

    private void removeCategoryClicked() {
        saveCategoriesClicked(true);
        String input = Popup.input("Enter the ID of the category you want to remove:", "");
        if (input == null || input.length() == 0) return;
        int id;
        try {
            id = Integer.parseInt(input);
        } catch (Exception e) {
            return;
        }
        if (id > displayCategories.size() || id < 0) return;
        displayCategories.remove(id);
        updateCategories(displayCategories);
    }

    private ArrayList<TileGenerator> displayTileGenerators;

    public void updateTileGenerators(ArrayList<TileGenerator> tilegenerators) {
        displayTileGenerators = tilegenerators;
        tileGeneratorsTable.setModel(new DefaultTableModel(new String[tilegenerators.size()][TILE_GENERATORS_COLUMN_NAMES.length], TILE_GENERATORS_COLUMN_NAMES));
        for (int i = 0; i < tilegenerators.size(); i++) {
            tileGeneratorsTable.setValueAt(i, i, 0);
            tileGeneratorsTable.setValueAt(tilegenerators.get(i).getType(), i, 1);
            tileGeneratorsTable.setValueAt(tilegenerators.get(i).getCategory(), i, 2);
            tileGeneratorsTable.setValueAt(tilegenerators.get(i).getParam1(), i, 3);
            tileGeneratorsTable.setValueAt(tilegenerators.get(i).getParam2(), i, 4);
            tileGeneratorsTable.setValueAt(tilegenerators.get(i).getParam3(), i, 5);
            tileGeneratorsTable.setValueAt(tilegenerators.get(i).getParam4(), i, 6);
        }
        resizeColumnWidth(tileGeneratorsTable);
    }

    private void saveTileGeneratorsClicked(boolean silent) {
        for (int i = 0; i < displayTileGenerators.size(); i++) {
            TileGenerator tileGenerator = displayTileGenerators.get(i);
            tileGenerator.setAllData(getValueAt(tileGeneratorsTable, i, 1), getValueAt(tileGeneratorsTable, i, 2), makeNullIfEmpty(getValueAt(tileGeneratorsTable, i, 3)), makeNullIfEmpty(getValueAt(tileGeneratorsTable, i, 4)), makeNullIfEmpty(getValueAt(tileGeneratorsTable, i, 5)), makeNullIfEmpty(getValueAt(tileGeneratorsTable, i, 6)));
        }
        main.save();
        displayTileGenerators.forEach(System.out::println);
        if (!silent) Popup.message("LaunchAnything", "Saved all tile generators!");
    }

    private void createTileGeneratorClicked() {
        saveTileGeneratorsClicked(true);

        String preset = Popup.dropDown("LaunchBar", "Select a preset", CREATE_NEW_TILE_GENERATOR_PRESET);
        if (preset == null) return;

        TileGenerator tileGeneratorToAdd = null;
        switch (preset) {
            case "None" -> tileGeneratorToAdd = new TileGenerator();
            case "Music" -> {
                String input = Popup.input("Enter the path to the folder with the music files:", "");
                if (input == null || input.length() == 0) return;
                tileGeneratorToAdd = new TileGenerator("music", "music", new File(input).getAbsolutePath(), null, null, null);
            }
        }

        if (tileGeneratorToAdd == null) return;
        displayTileGenerators.add(tileGeneratorToAdd);
        updateTileGenerators(displayTileGenerators);
    }

    private void removeTileGeneratorClicked() {
        saveTileGeneratorsClicked(true);
        String input = Popup.input("Enter the ID of the tile generator you want to remove:", "");
        if (input == null || input.length() == 0) return;
        int id;
        try {
            id = Integer.parseInt(input);
        } catch (Exception e) {
            return;
        }
        if (id > displayTileGenerators.size() || id < 0) return;
        displayTileGenerators.remove(id);
        updateTileGenerators(displayTileGenerators);
    }

    private String makeNullIfEmpty(String s) {
        if (s == null || s.length() == 0) return null;
        return s;
    }

    private ArrayList<Tile> displayedTiles;

    public void updateTiles(ArrayList<Tile> tiles) {
        displayedTiles = tiles;
        tilesTable.setModel(new DefaultTableModel(new String[tiles.size()][TILES_COLUMN_NAMES.length], TILES_COLUMN_NAMES));
        for (int i = 0; i < tiles.size(); i++) {
            tilesTable.setValueAt(i, i, 0);
            tilesTable.setValueAt(tiles.get(i).getId(), i, 1);
            tilesTable.setValueAt(tiles.get(i).getLabel(), i, 2);
            tilesTable.setValueAt(tiles.get(i).getCategory(), i, 3);
            tilesTable.setValueAt(tiles.get(i).getKeywordsString(), i, 4);
            tilesTable.setValueAt(Arrays.toString(tiles.get(i).getActions().toArray(new TileAction[0])), i, 5);
            tilesTable.setValueAt(tiles.get(i).isHidden(), i, 6);
            tilesTable.setValueAt(tiles.get(i).getLastExecuted(), i, 7);
        }
        resizeColumnWidth(tilesTable);
    }

    public void updateTilesActions(ArrayList<Tile> tiles) {
        displayedTiles = tiles;
        IntStream.range(0, tiles.size()).forEach(i -> tilesTable.setValueAt(Arrays.toString(tiles.get(i).getActions().toArray(new TileAction[0])), i, 5));
        resizeColumnWidth(tilesTable);
    }

    private void finishAndLeave() {
        int result = Popup.selectButton("LaunchBar", "Do you want to save before you return?", new String[]{"Yes", "No"});
        if (result == 0) {
            saveTilesClicked(true);
            saveTileGeneratorsClicked(true);
            saveCategoriesClicked(true);
            saveGeneralSettings(true);
            Popup.message("LaunchBar", "Saved all settings and tiles!");
        }
        Main.setOpenMode(true);
    }

    public static void updateTilesTable(String what) {
        if (what.equals("all"))
            self.updateTiles(self.displayedTiles);
        else if (what.equals("actions"))
            self.updateTilesActions(self.displayedTiles);
    }

    private void createTileClicked() {
        saveTilesClicked(true);

        String preset = Popup.dropDown("LaunchBar", "Select a preset", CREATE_NEW_TILE_PRESET);
        if (preset == null) return;

        Tile tileToAdd = null;
        switch (preset) {
            case "None" -> tileToAdd = new Tile();
            case "Open file" -> {
                tileToAdd = new Tile();
                String input = Popup.input("Enter the path to the file:", "");
                if (input == null || input.length() == 0) return;
                File file = new File(input);
                tileToAdd.setTileDataFromSettings(makeStringID("open " + file.getName()), "Open " + file.getName(), "openFile", file.getAbsolutePath().split("[/\\\\]"), false, "0");
                TileAction action = new TileAction("openFile", "path=" + file.getAbsolutePath());
                tileToAdd.addAction(action);
            }
            case "Open URL" -> {
                tileToAdd = new Tile();
                String input = Popup.input("Enter the URL:", "");
                if (input == null || input.length() == 0) return;
                tileToAdd.setTileDataFromSettings(makeStringID("open " + input), "Open " + input, "openURL", input, false, "0");
                TileAction action = new TileAction("openURL", "url=" + input);
                tileToAdd.addAction(action);
            }
            case "Copy to clipboard" -> {
                tileToAdd = new Tile();
                String input = Popup.input("Enter the text to copy:", "");
                if (input == null || input.length() == 0) return;
                tileToAdd.setTileDataFromSettings(makeStringID("copy " + input), "Copy '" + input + "'", "copy", input, false, "0");
                TileAction action = new TileAction("copyToClipboard", "text=" + input);
                tileToAdd.addAction(action);
            }
        }

        if (tileToAdd == null) return;
        displayedTiles.add(tileToAdd);
        updateTiles(displayedTiles);
    }

    private final static String LOWER_CASE_LETTERS = "abcdefghijklmnopqrstuvwxyzöäü";

    private String makeStringID(String text) {
        for (int i = 0; i < LOWER_CASE_LETTERS.length(); i++)
            text = replaceCharacterWithSpace(text, LOWER_CASE_LETTERS.charAt(i));
        return text;
    }

    private String replaceCharacterWithSpace(String text, char c) {
        return text.replace(" " + c, ("" + c).toUpperCase()).replace(" " + ("" + c).toUpperCase(), ("" + c).toUpperCase());
    }

    private void removeTileClicked() {
        saveTilesClicked(true);
        String input = Popup.input("Enter the ID of the tile you want to remove:", "");
        if (input == null || input.length() == 0) return;
        int id;
        try {
            id = Integer.parseInt(input);
        } catch (Exception e) {
            return;
        }
        if (id > displayedTiles.size() || id < 0) return;
        displayedTiles.remove(id);
        updateTiles(displayedTiles);
    }

    private void saveTilesClicked(boolean silent) {
        for (int i = 0; i < displayedTiles.size(); i++) {
            Tile tile = displayedTiles.get(i);
            tile.setTileDataFromSettings(getValueAt(tilesTable, i, 1), getValueAt(tilesTable, i, 2), getValueAt(tilesTable, i, 3), getValueAt(tilesTable, i, 4), getValueAt(tilesTable, i, 5).equals("true"), getValueAt(tilesTable, i, 6));
        }
        main.save();
        displayedTiles.forEach(System.out::println);
        if (!silent) Popup.message("LaunchAnything", "Saved all tiles!");
    }

    private String getValueAt(JTable table, int x, int y) {
        Object value = table.getModel().getValueAt(x, y);
        if (value == null) return null;
        return value.toString();
    }

    private void editTileActionClicked() {
        String input = Popup.input("Enter the ID of the tile you want to edit:", "");
        if (input == null || input.length() == 0) return;
        int id;
        try {
            id = Integer.parseInt(input);
        } catch (Exception e) {
            return;
        }
        if (id > displayedTiles.size() || id < 0) return;
        displayedTiles.get(id).openActionEditor();
    }

    public static void resizeColumnWidth(JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 15; //min width
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width + 1, width);
            }
            if (width > 300)
                width = 300;
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
