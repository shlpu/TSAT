package edu.gmu.grammar.patterns;

import java.util.ArrayList;

/**
 * Stores a group of time series patterns that all have the same label.
 */
public class TSPatterns implements Cloneable {

	private ArrayList<TSPattern> patterns;
	private String label;
	private boolean changed = true;

	/**
	 * Constructor that adds that label for the set.
	 *
	 * @param bagLabel - the label for the set.
	 */
	public TSPatterns(String bagLabel) {
		super();
		this.label = bagLabel.substring(0);
		this.patterns = new ArrayList<TSPattern>();
	}

	/**
	 * Add a pattern in to the set of patterns.
	 * 
	 * @param pattern - the pattern to be add.
	 */
	public synchronized void addPattern(TSPattern pattern) {
		this.changed = true;
		this.patterns.add(pattern);
	}

	/**
	 * Get the label for the set.
	 *
	 * @return - the label for the set.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Set the label for the set.
	 *
	 * @param label - the new label for the set.
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Get the patterns in the set.
	 *
	 * @return - the patterns in the set.
	 */
	public ArrayList<TSPattern> getPatterns() {
		return patterns;
	}

	/**
	 * Set the patterns in the set.
	 *
	 * @param patterns - the new patterns in the set.
	 */
	public void setPatterns(ArrayList<TSPattern> patterns) {
		this.patterns = patterns;
	}

	/**
	 * Get the total length of all the patterns in the set.
	 *
	 * @return - the total length of all the patterns in the set.
	 */
	public int getAllLen() {
		int allLen = 0;
		for (TSPattern p : patterns) {
			allLen += p.getPatternTS().length;
		}

		return allLen;
	}

}
