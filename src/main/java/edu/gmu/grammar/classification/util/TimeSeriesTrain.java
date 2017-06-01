package edu.gmu.grammar.classification.util;

/**
 * An object to store a set of time series data, its idx value, its true label, and its transformed version of the data.
 */
public class TimeSeriesTrain {
	private String trueLable;
	private double[] values;
	private int idx;
	private double[] transformedTS;

	/**
	 * Sets values.
	 * @param trueLable - the true label of the data.
	 * @param values - the values of the time series data.
	 * @param idx - the index of the pattern.
	 */
	public TimeSeriesTrain(String trueLable, double[] values, int idx) {
		this.trueLable = trueLable;
		this.values = values;
		this.idx = idx;
	}

	/**
	 * Get the true label.
	 *
	 * @return - the true label.
	 */
	public String getTrueLable() {
		return trueLable;
	}

	/**
	 * Set the true label.
	 *
	 * @param trueLable - the true label.
	 */
	public void setTrueLable(String trueLable) {
		this.trueLable = trueLable;
	}

	/**
	 * Get the time series values.
	 *
	 * @return - the time series values.
	 */
	public double[] getValues() {
		return values;
	}

	/**
	 * Set the time series values.
	 *
	 * @param values - the time series values.
	 */
	public void setValues(double[] values) {
		this.values = values;
	}

	/**
	 * Get the time series index.
	 *
	 * @return - the time series index.
	 */
	public int getIdx() {
		return idx;
	}

	/**
	 * Set the time series index.
	 *
	 * @param idx - the time series index.
	 */
	public void setIdx(int idx) {
		this.idx = idx;
	}

	/**
	 * Get the transformed time series.
	 *
	 * @return - the transformed time series.
	 */
	public double[] getTransformedTS() {
		return transformedTS;
	}

	/**
	 * Set the transformed time series.
	 *
	 * @param transformedTS - the transformed time series.
	 */
	public void setTransformedTS(double[] transformedTS) {
		this.transformedTS = transformedTS;
	}

}
