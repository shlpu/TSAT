package edu.gmu.grammar.patterns;

/**
 * Stores both the pattern and the transformed data
 */
public class PatternsAndTransformedData {
	private TSPattern[] allPatterns;
	private double[][] transformedTS;

	/**
	 * Get all patterns.
	 *
	 * @return - all patterns.
	 */
	public TSPattern[] getAllPatterns() {
		return allPatterns;
	}

	/**
	 * Set all patterns.
	 *
	 * @param allPatterns - new patterns.
	 */
	public void setAllPatterns(TSPattern[] allPatterns) {
		this.allPatterns = allPatterns;
	}

	/**
	 * Get transformed time series.
	 *
	 * @return - transformed time series.
	 */
	public double[][] getTransformedTS() {
		return transformedTS;
	}

	/**
	 * Set transformed time series.
	 *
	 * @param transformedTS - new transformed time series.
	 */
	public void setTransformedTS(double[][] transformedTS) {
		this.transformedTS = transformedTS;
	}
}
