package com.dwicke.tsat.view.table;

/**
 * Created by David Fleming on 1/23/17.
 */
public enum RPMTableColumns {

    RPM_CLASSES("Classes"),
    RPM_CLASS_STATS("Statistics (Wrong Label/Total Labeled)");

    private final String columnName;

    RPMTableColumns(String columnName) {this.columnName = columnName; }

    public String getColumnName() {
        return this.columnName;
    }

}
