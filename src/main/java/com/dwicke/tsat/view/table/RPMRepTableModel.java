package com.dwicke.tsat.view.table;

import com.dwicke.tsat.rpm.patterns.TSPattern;

import java.util.Arrays;

/**
 * Created by David Fleming on 1/24/17.
 */
public class RPMRepTableModel extends GrammarvizRulesTableDataModel {

    /** Fancy Serial */
    private static final long serialVersionUID = -8484756628234729960L;

    public RPMRepTableModel() {
        RPMRepTableColumns[] columns = RPMRepTableColumns.values();
        String[] schemaColumns = new String[columns.length];
        for(int i = 0; i < columns.length; i++) {
            schemaColumns[i] = columns[i].getColumnName();
        }
        setSchema(schemaColumns);
    }

    public void update(TSPattern[] finalPatterns) {
        int rowIndex = 0;
        rows.clear();
        if (!(null == finalPatterns)) {
            for (rowIndex = 0; rowIndex < finalPatterns.length; rowIndex++) {
                Object[] item = new Object[getColumnCount()];
                int nColumn = 0;
                item[nColumn++] = rowIndex;
                item[nColumn++] = finalPatterns[rowIndex].getLabel(); // Get Class Label
                String dataCol = Arrays.toString(finalPatterns[rowIndex].getPatternTS());
                if(dataCol.length() > 1500)
                    item[nColumn++] = dataCol.substring(0, 1500) + "..."; // Get Class failure to success rate
                else
                    item[nColumn++] = dataCol;
                rows.add(item);
            }
        }
        fireTableDataChanged();
    }

    public Class<?> getColumnClass(int columnIndex) {
        if(columnIndex == RPMRepTableColumns.RPM_CLASS_PATTERN_NUMBER.ordinal())
            return String.class;
        if(columnIndex == RPMRepTableColumns.RPM_CLASSES.ordinal())
            return Integer.class;
        if(columnIndex == RPMRepTableColumns.RPM_CLASS_REP_PATTERN.ordinal())
            return Integer.class;

        return String.class;
    }
}
