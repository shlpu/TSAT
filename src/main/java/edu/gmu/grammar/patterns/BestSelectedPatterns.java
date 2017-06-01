package edu.gmu.grammar.patterns;


import java.io.Serializable;
import java.util.Arrays;

/**
 * This class stores the best patterns as found by RPM, as well as the best parameters and the minimal error.
 */
public class BestSelectedPatterns implements Serializable {

	private static final long serialVersionUID = -5065401322294738867L;

	// The minimum error
	private double minimalError;
	//The best patterns
	private TSPattern[] bestSelectedPatterns;
	// The best parameters, Window Size, PAA, Alphabet
	private int[] bestParams;

	/**
	 * Creates the storage class for the best selected patterns from RPM
	 * @param minimalError - The minimum error.
	 * @param bestParams - The best parameters (Window Size, PAA, Alphabet).
	 * @param bestSelectedPatterns - The best selected patterns from RPM.
	 */
	public BestSelectedPatterns(double minimalError, int[] bestParams,
			TSPattern[] bestSelectedPatterns) {
		this.minimalError = minimalError;
		this.bestParams = bestParams;
		this.bestSelectedPatterns = bestSelectedPatterns;
	}

	/**
	 * Get the minimal error.
	 * @return - The minimal error.
	 */
	public double getMinimalError() {
		return minimalError;
	}

	/**
	 * Set the minimal error.
	 * @param minimalError - The new minimal error.
	 */
	public void setMinimalError(double minimalError) {
		this.minimalError = minimalError;
	}

	/**
	 * Get the best selected patterns.
	 * @return - The best selected patterns.
	 */
	public TSPattern[] getBestSelectedPatterns() {
		return bestSelectedPatterns;
	}

	/**
	 * Set the best slected patterns.
	 * @param bestSelectedPatterns - The new best selected patterns.
	 */
	public void setBestSelectedPatterns(TSPattern[] bestSelectedPatterns) {
		this.bestSelectedPatterns = bestSelectedPatterns;
	}

	/**
	 * Get the best parameters.
	 * @return - The best parameters (Window Size, PAA, Alphabet).
	 */
	public int[] getBestParams() {
		return bestParams;
	}

	/**
	 * Set the best parameters.
	 * @param bestParams - The new best parameters (Window Size, PAA, Alphabet).
	 */
	public void setBestParams(int[] bestParams) {
		this.bestParams = bestParams;
	}

	/**
	 * Creates printable formatted string.
	 * @return - Formatted string.
	 */
	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();
		output.append("Minimal Error: " + this.minimalError + "\n");
		output.append("Best Parameters: " + Arrays.toString(this.bestParams) + "\n");
		output.append("Best Patterns:\n");
		for(int i = 0; i < this.bestSelectedPatterns.length; i++) {
			output.append(this.bestSelectedPatterns[i].toString() + "\n");
		}

		return output.toString();
	}

}
