package net.seninp.grammarviz.view.table;

import edu.gmu.grammar.classification.util.ClassificationResults;

/**
 * Created by David Fleming on 1/23/17.
 */
public class RPMTSTableModel extends GrammarvizRulesTableDataModel {

    /** Fancy serial. */
    private static final long serialVersionUID = 170675488414361571L;

    public RPMTSTableModel() {
        RPMTSTableColumns[] columns = RPMTSTableColumns.values();
        String[] schemaColumns = new String[columns.length];
        for(int i = 0; i < columns.length; i++) {
            schemaColumns[i] = columns[i].getColumnName();
        }
        setSchema(schemaColumns);
    }

    public void update(String[][] results) {
        int rowIndex = 0;
        rows.clear();
        if (!(null == results)) {
            for (rowIndex = 0; rowIndex < results.length; rowIndex++) {
                Object[] item = new Object[getColumnCount()];
                int nColumn = 0;
                item[nColumn++] = results[rowIndex][0]; // Get TS inst#
                item[nColumn++] = results[rowIndex][1]; // Get actual class
                item[nColumn++] = results[rowIndex][2]; // Get predicted class
                item[nColumn++] = results[rowIndex][3]; // time series

                rows.add(item);
            }
        }
        fireTableDataChanged();
    }

    public Class<?> getColumnClass(int columnIndex) {
        if(columnIndex == RPMTSTableColumns.RPM_TS_ID.ordinal())
            return Integer.class;
        if(columnIndex == RPMTSTableColumns.RPM_ACTUAL.ordinal())
            return Integer.class;
        if(columnIndex == RPMTSTableColumns.RPM_PREDICTED.ordinal())
            return Integer.class;
        return String.class;
    }
}
