import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;

public class GuiActionEditor {
    private JTable actionTable;
    private JPanel mainPanel;
    private JButton saveButton;
    private JButton addNewActionButton;
    private JButton removeActionButton;
    private Tile tile;
    private JFrame frame;

    public GuiActionEditor(Tile tile) {
        this.tile = tile;
        saveButton.addActionListener(e -> {
            saveClicked();
            frame.dispose();
        });
        removeActionButton.addActionListener(e -> {
            removeClicked();
        });
        addNewActionButton.addActionListener(e -> {
            addNewClicked();
        });
        actionTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

    private final static String[] COLUMN_NAMES = new String[]{"ID", "Type", "Parameters"};

    private ArrayList<TileAction> actions;

    public void updateTiles(ArrayList<TileAction> actions) {
        this.actions = actions;
        actionTable.setModel(new DefaultTableModel(new String[actions.size()][COLUMN_NAMES.length], COLUMN_NAMES));
        for (int i = 0; i < actions.size(); i++) {
            actionTable.setValueAt(i, i, 0);
            actionTable.setValueAt(actions.get(i).getActionType(), i, 1);
            actionTable.setValueAt(actions.get(i).getParametersAsString(), i, 2);
        }
        GuiSettings.resizeColumnWidth(actionTable);
    }

    private String getValueAt(int x, int y) {
        return actionTable.getModel().getValueAt(x, y).toString();
    }

    public void saveClicked() {
        for (int i = 0; i < actions.size(); i++) {
            TileAction action = actions.get(i);
            action.setActionType(getValueAt(i, 1));
            action.setParametersFromString(getValueAt(i, 2));
        }
        tile.setActions(actions);
        GuiSettings.update("actions");
    }

    private void removeClicked() {
        saveClicked();
        String input = Popup.input("Enter the ID of the action you want to remove:", "");
        if (input == null || input.length() == 0) return;
        int id;
        try {
            id = Integer.parseInt(input);
        } catch (Exception e) {
            return;
        }
        if (id > actions.size() || id < 0) return;
        actions.remove(id);
        updateTiles(actions);
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    private final static String[] CREATE_NEW_PRESET = new String[]{"None", "Open file", "Copy to clipboard", "Settings"};

    public void addNewClicked() {
        saveClicked();
        String preset = Popup.dropDown("LaunchBar", "Select a preset", CREATE_NEW_PRESET);
        if (preset == null) return;
        switch (preset) {
            case "None" -> actions.add(new TileAction());
            case "Open file" -> actions.add(new TileAction("openFile", "path=none.png"));
            case "Settings" -> actions.add(new TileAction("settings", "setting=settings"));
            case "Copy to clipboard" -> actions.add(new TileAction("copyToClipboard", "text=Hello World!"));
        }
        updateTiles(actions);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
