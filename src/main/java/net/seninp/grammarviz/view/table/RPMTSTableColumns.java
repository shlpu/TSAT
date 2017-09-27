package net.seninp.grammarviz.view.table;

/**
 *
 */
public enum RPMTSTableColumns {

    RPM_TS_ID("ID"),
    RPM_ACTUAL("Actual Class"),
    RPM_PREDICTED("Predicted"),
    RPM_TIMESERIES("Time Series");

    private final String columnName;

    RPMTSTableColumns(String columnName) {this.columnName = columnName; }

    public String getColumnName() {
        return this.columnName;
    }
}
