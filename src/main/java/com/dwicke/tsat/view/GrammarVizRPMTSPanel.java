package com.dwicke.tsat.view;

import com.dwicke.tsat.model.UserSession;
import com.dwicke.tsat.view.table.RPMTSTableColumns;
import com.dwicke.tsat.view.table.RPMTSTableModel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by David Fleming on 1/23/17.
 *
 * This class handles the display of the out put of the classification phase of RPM in the GrammarViz GUI.
 */
public class GrammarVizRPMTSPanel extends JPanel implements ListSelectionListener {

    /** Fancy Serial */
    private static final long serialVersionUID = -6017992967964000474L;

    public static final String FIRING_PROPERTY_RPM_TS = "selectedRow_rpmTS";

    private UserSession session;

    private com.dwicke.tsat.view.table.RPMTSTableModel RPMTSTableModel;

    private JXTable RPMTable;

    private JScrollPane RPMPane;

    private ArrayList<String> selectedResults;

    private boolean acceptListEvents;

    // static block - we instantiate the logger
    //
    private static final Logger LOGGER = LoggerFactory.getLogger(GrammarRulesPanel.class);

    /**
     * Creates the panel and table within the panel for displaying the classification results of RPM.
     */
    public GrammarVizRPMTSPanel() {
        super();
        this.RPMTSTableModel = new RPMTSTableModel();
        this.RPMTable = new JXTable() {
            private static final long serialVersionUID = 4L;

            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new JXTableHeader(columnModel) {
                    private static final long serialVersionUID = 2L;

                    @Override
                    public void updateUI() {
                        super.updateUI();
                        // need to do in updateUI to survive toggling of LAF
                        if (getDefaultRenderer() instanceof JLabel) {
                            ((JLabel) getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

                        }
                    }
                };
            }
        };

        this.RPMTable.setModel(this.RPMTSTableModel);
        this.RPMTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.RPMTable.setShowGrid(false);

        this.RPMTable.getSelectionModel().addListSelectionListener(this);


        @SuppressWarnings("unused")
        org.jdesktop.swingx.renderer.DefaultTableRenderer renderer =
                (org.jdesktop.swingx.renderer.DefaultTableRenderer) RPMTable.getDefaultRenderer(String.class);

        TableRowSorter<RPMTSTableModel> sorter = new TableRowSorter<RPMTSTableModel>(this.RPMTSTableModel);
        this.RPMTable.setRowSorter(sorter);

        this.RPMPane = new JScrollPane(this.RPMTable);
        this.RPMPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    }

    /**
     * Resets the panel.
     */
    public void resetPanel() {
        // cleanup all the content
        this.removeAll();
        this.add(this.RPMPane);
        this.validate();
        this.repaint();
    }

    /**
     * Get the table model.
     *
     * @return the table model.
     */
    public RPMTSTableModel getRPMTSTableModel() {return this.RPMTSTableModel; }

    /**
     * Get the RPM table.
     *
     * @return the RPM table.
     */
    public JTable getRPMTable() {return this.RPMTable; }

    /**
     * Updates the table values when values change.
     *
     * @param arg
     */
    @Override
    public void valueChanged(ListSelectionEvent arg) {
        if (!arg.getValueIsAdjusting() && this.acceptListEvents) {
            int[] rows = this.RPMTable.getSelectedRows();
            LOGGER.debug("Selected ROWS: " + Arrays.toString(rows));
            ArrayList<String> rules = new ArrayList<String>(rows.length);
            for (int i = 0; i < rows.length; i++) {
                int ridx = rows[i];
                String rule = String.valueOf(
                        this.RPMTable.getValueAt(ridx, RPMTSTableColumns.RPM_TS_ID.ordinal()));

                rules.add(rule);
            }
            this.firePropertyChange(FIRING_PROPERTY_RPM_TS, this.selectedResults, rules);
        }

    }

    /**
     * Updates the table by populating it with results.
     */
    public void updateRPMStatistics() {
        this.acceptListEvents = false;
        this.RPMTSTableModel.update(this.session.rpmHandler.getMisclassifiedResults());
        this.acceptListEvents = true;
    }

    /**
     * Clears the table.
     */
    public void clear() {
        this.acceptListEvents = false;
        this.removeAll();
        this.session = null;
        RPMTSTableModel.update(null);
        this.validate();
        this.repaint();
        this.acceptListEvents = true;
    }

    /**
     * Set the user session.
     *
     * @param session the user session.
     */
    public void setClassificationResults(UserSession session) {this.session = session; }

}
