package com.dwicke.tsat.view.table;

/**
 * Created by David Fleming on 1/23/17.
 */
public class RPMTableModel extends GrammarvizRulesTableDataModel {

    /** Fancy serial. */
    private static final long serialVersionUID = 170675488414361571L;

    public RPMTableModel() {
        RPMTableColumns[] columns = RPMTableColumns.values();
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
                item[nColumn++] = results[rowIndex][0]; // Get Class Label
                item[nColumn++] = results[rowIndex][1]; // Get Class failure to success rate
                rows.add(item);
            }
        }
        fireTableDataChanged();
    }

    public Class<?> getColumnClass(int columnIndex) {
        if(columnIndex == RPMTableColumns.RPM_CLASSES.ordinal())
            return String.class;
        if(columnIndex == RPMTableColumns.RPM_CLASS_STATS.ordinal())
            return Integer.class;

        return String.class;
    }
}
