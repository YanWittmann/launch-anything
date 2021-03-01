import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

public class GuiSettings {
    private JPanel mainPanel;
    private JTabbedPane tabbedPane1;
    private JTable tilesTable;
    private JButton createTileButton;
    private JButton editTileButton;
    private JButton removeTileButton;
    private JButton saveTilesButton;
    private JButton returnToBarButton;
    private JTable categoriesTable;
    private JTable tileGeneratorsTable;
    private Main main;
    private static GuiSettings self;

    public GuiSettings(Main main) {
        self = this;
        this.main = main;
        createTileButton.addActionListener(e -> createTileClicked());
        editTileButton.addActionListener(e -> editTileActionClicked());
        removeTileButton.addActionListener(e -> deleteTileClicked());
        saveTilesButton.addActionListener(e -> saveTilesClicked(false));
        returnToBarButton.addActionListener(e -> finishAndLeave());
        tilesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

    private final static String[] COLUMN_NAMES = new String[]{"ID", "String ID", "Label", "Category", "Keywords", "Actions (Use button below to edit)", "Hidden", "Last executed"};

    private ArrayList<Tile> displayedTiles;

    public void updateTiles(ArrayList<Tile> tiles) {
        displayedTiles = tiles;
        tilesTable.setModel(new DefaultTableModel(new String[tiles.size()][COLUMN_NAMES.length], COLUMN_NAMES));
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
        Main.setOpenMode(true);
    }

    public static void update(String what) {
        if (what.equals("all"))
            self.updateTiles(self.displayedTiles);
        else if (what.equals("actions"))
            self.updateTilesActions(self.displayedTiles);
    }

    private final static String[] CREATE_NEW_PRESET = new String[]{"None", "Open file", "Copy to clipboard", "Settings"};

    private void createTileClicked() {
        saveTilesClicked(true);

        String preset = Popup.dropDown("LaunchBar", "Select a preset", CREATE_NEW_PRESET);
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

    private void deleteTileClicked() {

    }

    private void saveTilesClicked(boolean silent) {
        for (int i = 0; i < displayedTiles.size(); i++) {
            Tile tile = displayedTiles.get(i);
            tile.setTileDataFromSettings(getValueAt(i, 1), getValueAt(i, 2), getValueAt(i, 3), getValueAt(i, 4), getValueAt(i, 5).equals("true"), getValueAt(i, 6));
        }
        main.save();
        displayedTiles.forEach(System.out::println);
        if (!silent) Popup.message("LaunchAnything", "Saved all tiles!");
    }

    private String getValueAt(int x, int y) {
        return tilesTable.getModel().getValueAt(x, y).toString();
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
