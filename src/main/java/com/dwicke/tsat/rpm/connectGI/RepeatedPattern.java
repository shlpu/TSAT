package com.dwicke.tsat.rpm.connectGI;

import net.seninp.gi.logic.RuleInterval;

import java.util.ArrayList;

/**
 * Stores a recurring pattern that was found in the rule set, includes frequency and where the pattern is found.
 */
public class RepeatedPattern implements Cloneable, Comparable<RepeatedPattern> {

	// private int frequency;
	private ArrayList<RuleInterval> sequences;
	private int length;
	private int frequency;
	int[] startingPositions;

	/**
	 * Stores a recurring pattern that was found in the rule set.
	 *
	 * @param length the length of the repeating rule.
	 * @param r the rule, and the start and stopping positions of the rule.
	 * @param startingPositions the starting positions where the rule is in the concatenated data.
	 */
	public RepeatedPattern(int length, RuleInterval r, int[] startingPositions) {
		// this.startPositionOriginal = startPosition;
		this.length = length;
		// this.frequency = frequency;
		sequences = new ArrayList<RuleInterval>();
		sequences.add(r);
		this.startingPositions = startingPositions;
	}

	/**
	 * Get the rule sequences.
	 *
	 * @return the sequences of rules.
	 */
	public ArrayList<RuleInterval> getSequences() {
		return sequences;
	}

	/**
	 * Set the rule sequences.
	 *
	 * @param sequences the sequences of rules.
	 */
	public void setSequences(ArrayList<RuleInterval> sequences) {
		this.sequences = sequences;
	}

	/**
	 * Get the length of the rule.
	 *
	 * @return the length of the rule.
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Set the length of the rule.
	 *
	 * @param length the length of the rule.
	 */
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * Get the frequency of the rule.
	 *
	 * @return the frequency of the rule.
	 */
	public int getFrequency() {
		calculationFrequency(startingPositions);
		return frequency;
	}

	/**
	 * Set the frequency of the rule.
	 *
	 * @param frequency the frequency of the rule.
	 */
	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	/**
	 * Calculate the frequency of the rule by taking the rules and finding if they match all the patterns at
	 * the start positions.
	 *
	 * @param startingPositions the start positions of the patterns from the concatenated data.
	 * @return the frequency of the rules.
	 */
	private int calculationFrequency(int[] startingPositions) {
		int f = 0;
		ArrayList<RuleInterval> arrPosThis = new ArrayList<RuleInterval>();
		for (RuleInterval ri : sequences) {
			if (PatternsProcess.isFromDifferenTS(arrPosThis, ri,
					startingPositions)) {
				f++;
			}
			arrPosThis.add(ri);
		}
		frequency = f;
		return f;
	}

	/**
	 * Compares two RepeatedPattern by comparing the frequency of their rules, if comparing object is more frequent
	 * then a 1 is returned if it is less then a -1 else 0 is returned.
	 *
	 * @param arg0 the RepeatedPattern to compare against.
	 * @return if comparing object is more frequent then a 1 is returned if it is less then a -1 else 0 is returned.
	 */
	@Override
	public int compareTo(RepeatedPattern arg0) {
		Integer p1 = arg0.getFrequency();
		Integer fHere = this.getFrequency();

		if (fHere > p1) {
			return 1;
		} else if (fHere < p1) {
			return -1;
		} else
			return 0;
	}

}
