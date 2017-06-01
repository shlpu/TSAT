package edu.gmu.grammar.patterns;

import java.util.ArrayList;

/**
 * This class is used to manage pattern similarity.
 */
public class PatternsSimilarity {

	private Boolean isTSimilarSetted = false;
	private double initialTSimilar;
	private double tSimilar = 0.02;
	private ArrayList<Double> tCandidates;

	/**
	 * Constructs the object.
	 *
	 * @param tSimilar - initial similarity rating.
	 */
	public PatternsSimilarity(double tSimilar) {
		this.initialTSimilar = tSimilar;
		tCandidates = new ArrayList<Double>();
	}

	/**
	 * Get the condition that indicates that the similarity rating has settled.
	 *
	 * @return - weather the similarity has settled.
	 */
	public Boolean getIsTSimilarSetted() {
		return isTSimilarSetted;
	}

	/**
	 * Get the condition that indicates that the similarity rating has settled.
	 *
	 * @param isTSimilarSetted - weather the similarity has settled.
	 */
	public void setIsTSimilarSetted(Boolean isTSimilarSetted) {
		this.isTSimilarSetted = isTSimilarSetted;
	}

	/**
	 * Get the initial similarity rating.
	 *
	 * @return - the initial similarity rating.
	 */
	public double getInitialTSimilar() {
		return initialTSimilar;
	}

	/**
	 * Set the initial similarity rating.
	 *
	 * @param initialTSimilar - the initial similarity rating.
	 */
	public void setInitialTSimilar(double initialTSimilar) {
		this.initialTSimilar = initialTSimilar;
	}

	/**
	 * Get the similarity rating.
	 *
	 * @return - the similarity rating.
	 */
	public double gettSimilar() {
		return tSimilar;
	}

	/**
	 * Set the similarity rating.
	 *
	 * @param tSimilar - the similarity rating.
	 */
	public void settSimilar(double tSimilar) {
		this.tSimilar = tSimilar;
	}

	/**
	 * Add a candidate for the similarity rating.
	 *
	 * @param tcandi - the candidate for the similarity rating.
	 */
	public void addCandidate(double tcandi) {
		tCandidates.add(tcandi);
	}

	/**
	 * Get the candidates for the similarity rating.
	 *
	 * @return - the candidates for the similarity rating.
	 */
	public ArrayList<Double> gettCandidates() {
		return tCandidates;
	}

	/**
	 * Set all the candidates for the similarity rating.
	 *
	 * @param tCandidates - the candidates for the similarity rating.
	 */
	public void settCandidates(ArrayList<Double> tCandidates) {
		this.tCandidates = tCandidates;
	}

	/**
	 * Clear all candidates for the similarity rating.
	 */
	public void clear() {
		tCandidates.clear();
	}
}
