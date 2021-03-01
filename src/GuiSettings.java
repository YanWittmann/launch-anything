import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class GuiSettings {
    private JPanel mainPanel;
    private JTabbedPane tabbedPane1;
    private JTable tilesTable;
    private JButton createTileButton;
    private JButton editTileButton;
    private JButton removeTileButton;
    private JButton saveTilesButton;
    private JButton returnToBarButton;
    private Main main;

    public GuiSettings(Main main) {
        this.main = main;
        createTileButton.addActionListener(e -> createTileClicked());
        editTileButton.addActionListener(e -> editTileActionClicked());
        removeTileButton.addActionListener(e -> deleteTileClicked());
        saveTilesButton.addActionListener(e -> saveTilesClicked());
        returnToBarButton.addActionListener(e -> {

        });
        tilesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

    private final static String[] COLUMN_NAMES = new String[]{"ID", "String ID", "Label", "Category", "Keywords", "Actions (Use button to edit)", "Hidden", "Last executed"};

    private ArrayList<Tile> displayedTiles;

    public void updateTiles(ArrayList<Tile> tiles) {
        displayedTiles = tiles;
        tilesTable.setModel(new DefaultTableModel(new String[tiles.size()][8], COLUMN_NAMES));
        for (int i = 0; i < tiles.size(); i++) {
            tilesTable.setValueAt(i, i, 0);
            tilesTable.setValueAt(tiles.get(i).getId(), i, 1);
            tilesTable.setValueAt(tiles.get(i).getLabel(), i, 2);
            tilesTable.setValueAt(tiles.get(i).getCategory(), i, 3);
            tilesTable.setValueAt(tiles.get(i).getKeywordsString(), i, 4);
            tilesTable.setValueAt(Arrays.toString(tiles.get(i).getActions()), i, 5);
            tilesTable.setValueAt(tiles.get(i).isHidden(), i, 6);
            tilesTable.setValueAt(tiles.get(i).getLastExecuted(), i, 7);
        }
        resizeColumnWidth(tilesTable);
    }

    private void createTileClicked() {

    }

    private void deleteTileClicked() {

    }

    private void saveTilesClicked() {
        for (int i = 0; i < displayedTiles.size(); i++) {
            Tile tile = displayedTiles.get(i);
            tile.setTileDataFromSettings(getValueAt(i, 1), getValueAt(i, 2), getValueAt(i, 3), getValueAt(i, 4), getValueAt(i, 5).equals("true"), getValueAt(i, 6));
        }
        main.save();
        displayedTiles.forEach(System.out::println);
        Popup.message("LaunchAnything", "Saved all tiles!");
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
