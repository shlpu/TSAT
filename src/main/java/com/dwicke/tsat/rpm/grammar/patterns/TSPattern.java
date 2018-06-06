package com.dwicke.tsat.rpm.grammar.patterns;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A class for storing both a unique time series pattern and its relevant information and metadata.
 */
public class TSPattern implements Cloneable, Comparable<TSPattern>, Serializable {

	private static final long serialVersionUID = 4268364054811283654L;

	private int frequency;
	private double[] patternTS;
	private double error = 0;
	private String label;
	private int fromTS;
	private int startP;

	/**
	 * Constructor
	 *
	 * @param frequency - the frequency that the pattern occurs in the time series.
	 * @param patternTS - the pattern.
	 * @param classLabel - the label for the pattern.
	 * @param startP - the patterns start position in the concatenated data.
	 */
	public TSPattern(int frequency, double[] patternTS, String classLabel, int startP) {
		super();
		this.frequency = frequency;
		this.patternTS = patternTS;
		this.label = classLabel;
		this.startP = startP;
	}

	/**
	 * Constructor for cloning.
	 *
	 * @param another - another TSPattern to be cloned.
	 */
	public TSPattern(TSPattern another) {
		super();
		this.frequency = another.frequency;
		this.patternTS = another.patternTS;
		this.error = another.error;
		this.label = another.label;
		this.fromTS = another.fromTS;
	}

	/**
	 * Get the pattern's frequency.
	 *
	 * @return - the pattern's frequency.
	 */
	public int getFrequency() {
		return frequency;
	}

	/**
	 * Set the pattern's frequency.
	 *
	 * @param frequency - the pattern's frequency.
	 */
	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	/**
	 * Get the time series pattern.
	 *
	 * @return - the time series pattern.
	 */
	public double[] getPatternTS() {
		return patternTS;
	}

	/**
	 * Set the time series pattern.
	 *
	 * @param patternTS - the time series pattern.
	 */
	public void setPatternTS(double[] patternTS) {
		this.patternTS = patternTS;
	}

	/**
	 * Get the pattern's label.
	 *
	 * @return - the pattern's label.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Get the pattern's error rate.
	 *
	 * @return - the pattern's error rate.
	 */
	public double getError() {
		return error;
	}

	/**
	 * Set the pattern's error rate.
	 *
	 * @param error - the pattern's error rate.
	 */
	public void setError(double error) {
		this.error = error;
	}

	/**
	 * Get the index for the time series.
	 *
	 * @return - the index for the time series.
	 */
	public int getFromTS() {
		return fromTS;
	}

	/**
	 * Set the index for the time series.
	 *
	 * @param fromTS - the index for the time series.
	 */
	public void setFromTS(int fromTS) {
		this.fromTS = fromTS;
	}

	/**
	 * Get the patterns start position in the concatenated data.
	 *
	 * @return - the patterns start position in the concatenated data.
	 */
	public int getStartP() {
		return startP;
	}

	/**
	 * Set the patterns start position in the concatenated data.
	 *
	 * @param startP - the patterns start position in the concatenated data.
	 */
	public void setStartP(int startP) {
		this.startP = startP;
	}


	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Compares to TSPattern objects based on their frequency. If the comparing object's frequency is greater then
	 * the object being compared to it will return a 1. If the frequency of the comparing object is less then
	 * the object being compared to it will return a -1. If the frequency of the two objects are equal, a 0 is returned.
	 *
	 * @param arg0 - the other TSPattern to be compared to.
	 * @return - the results of the frequency comparison.
	 */
	@Override
	public int compareTo(TSPattern arg0) {
		Integer p1 = arg0.getFrequency();

		if (this.getFrequency() > p1) {
			return 1;
		} else if (this.getFrequency() < p1) {
			return -1;
		} else
			return 0;
	}

	/**
	 * Creates a formatted string useful for printing.
	 *
	 * @return - a formatted string.
	 */
	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();
		output.append("Label: " + this.label + "\n");
		output.append("Length PatternsTS (" + this.patternTS.length + "):\n");
		output.append(Arrays.toString(this.patternTS));

		return output.toString();
	}
}