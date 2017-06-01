package net.seninp.grammarviz.view;

import net.seninp.grammarviz.session.UserSession;
import net.seninp.grammarviz.view.table.RPMRepTableModel;
import net.seninp.grammarviz.view.table.RPMRepTableColumns;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by David Fleming on 1/24/17.
 *
 * This class handles the display of the representative patterns found by RPM in the GrammarViz GUI.
 */
public class GrammarVizRPMRepPanel  extends JPanel implements ListSelectionListener {

    /** Fancy Serial */
    private static final long serialVersionUID = -2040422995980516094L;

    public static final String FIRING_PROPERTY_RPM_REP = "selectedRow_rpm_rep";

    private UserSession session;

    private net.seninp.grammarviz.view.table.RPMRepTableModel RPMRepTableModel;

    private JXTable RPMRepTable;

    private JScrollPane RPMRepPane;

    private ArrayList<String> selectedResults;

    private boolean acceptListEvents;

    // static block - we instantiate the logger
    //
    private static final Logger LOGGER = LoggerFactory.getLogger(GrammarRulesPanel.class);

    /**
     * Creates the panel and table within the panel for displaying the representative patterns found by RPM.
     */
    public GrammarVizRPMRepPanel() {
        super();
        this.RPMRepTableModel = new RPMRepTableModel();
        this.RPMRepTable = new JXTable() {
            private static final long serialVersionUID = 5L;
            private DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
            private DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();

            {
                leftRenderer.setHorizontalAlignment(SwingConstants.LEFT);
            }

            @Override
            protected JTableHeader createDefaultTableHeader() {

                return new JXTableHeader(columnModel) {
                    private static final long serialVersionUID = 3L;

                    @Override
                    public void updateUI() {
                        super.updateUI();
                        // need to do in updateUI to survive toggling of LAF
                        if (getDefaultRenderer() instanceof JLabel) {
                            ((JLabel) getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);

                        }
                    }
                };
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int col) {
                if(col == 2) {
                    return leftRenderer;
                }

                return defaultRenderer;
            };
        };

        this.RPMRepTable.setModel(this.RPMRepTableModel);
        this.RPMRepTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.RPMRepTable.setShowGrid(false);

        this.RPMRepTable.getSelectionModel().addListSelectionListener(this);

        @SuppressWarnings("unused")
        org.jdesktop.swingx.renderer.DefaultTableRenderer renderer =
                (org.jdesktop.swingx.renderer.DefaultTableRenderer) RPMRepTable.getDefaultRenderer(String.class);

        TableRowSorter<RPMRepTableModel> sorter = new TableRowSorter<RPMRepTableModel>(this.RPMRepTableModel);
        this.RPMRepTable.setRowSorter(sorter);
        this.RPMRepTable.setHorizontalScrollEnabled(true);



        this.RPMRepPane = new JScrollPane(this.RPMRepTable);
        this.RPMRepPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * Resets the panel.
     */
    public void resetPanel() {
        // cleanup all the content
        this.removeAll();
        this.add(this.RPMRepPane);
        this.validate();
        this.repaint();
    }

    /**
     * Get the table model.
     *
     * @return the table model.
     */
    public RPMRepTableModel getRPMRepTableModel() {return this.RPMRepTableModel; }

    /**
     * Get the RPM table.
     *
     * @return the RPM table.
     */
    public JTable getRPMRepTable() {return this.RPMRepTable; }

    /**
     * Updates the table values when values change.
     *
     * @param arg
     */
    @Override
    public void valueChanged(ListSelectionEvent arg) {
        if (!arg.getValueIsAdjusting() && this.acceptListEvents) {
            int[] rows = this.RPMRepTable.getSelectedRows();
            LOGGER.debug("Selected ROWS: " + Arrays.toString(rows));
            ArrayList<String> rules = new ArrayList<String>(rows.length);
            for (int i = 0; i < rows.length; i++) {
                int ridx = rows[i];
                String rule = String.valueOf(
                        this.RPMRepTable.getValueAt(ridx, RPMRepTableColumns.RPM_CLASS_PATTERN_NUMBER.ordinal()));
                rules.add(rule);
            }
            this.firePropertyChange(FIRING_PROPERTY_RPM_REP, this.selectedResults, rules);
        }

    }

    /**
     * Updates the table by populating it with the representative patterns.
     */
    public void updateRPMRepPatterns() {
        this.acceptListEvents = false;
        this.RPMRepTableModel.update(this.session.rpmHandler.getRepresentativePatterns());
        this.RPMRepTable.packAll();
        this.acceptListEvents = true;
    }

    /**
     * Clears the table.
     */
    public void clear() {
        this.acceptListEvents = false;
        this.removeAll();
        this.session = null;
        RPMRepTableModel.update(null);
        this.validate();
        this.repaint();
        this.acceptListEvents = true;
    }

    /**
     * Set the user session.
     *
     * @param session the user session.
     */
    public void setResults(UserSession session) {this.session = session; }
}
