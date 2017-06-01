package edu.gmu.grammar.classification.util;

/**
 * Stores error values.
 */
public class ClassificationErrorEachSample {
	// The overall error
	private double allError;
	// The errors for every class
	private double[] errorPerClass;

	/**
	 * Constructor.
	 *
	 * @param allError - the overall error.
	 * @param errorPerClass - the errors for every class.
	 */
	public ClassificationErrorEachSample(double allError, double[] errorPerClass) {
		this.allError = allError;
		this.errorPerClass = errorPerClass;
	}

	/**
	 * Get the overall error.
	 *
	 * @return - the overall error.
	 */
	public double getAllError() {
		return allError;
	}

	/**
	 * Set the overall error.
	 *
	 * @param allError - the overall error.
	 */
	public void setAllError(double allError) {
		this.allError = allError;
	}

	/**
	 * Get the errors for every class.
	 *
	 * @return - the errors for every class
	 */
	public double[] getErrorPerClass() {
		return errorPerClass;
	}

	/**
	 * Set the errors for every class.
	 *
	 * @param errorPerClass - the errors for every class
	 */
	public void setErrorPerClass(double[] errorPerClass) {
		this.errorPerClass = errorPerClass;
	}
}
