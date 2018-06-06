package com.dwicke.tsat.view.table;

/**
 * Created by David Fleming on 1/24/17.
 */
public enum RPMRepTableColumns {

    RPM_CLASS_PATTERN_NUMBER("Pattern"),
    RPM_CLASSES("Classes"),
    RPM_CLASS_REP_PATTERN("Representative Patterns");

    private final String columnName;

    RPMRepTableColumns(String columnName) {this.columnName = columnName; }

    public String getColumnName() {
        return this.columnName;
    }
}
