package com.dwicke.tsat.rpm.connectGI;

import com.dwicke.tsat.rpm.grammar.patterns.PatternsSimilarity;
import com.dwicke.tsat.rpm.grammar.patterns.TSPattern;
import com.dwicke.tsat.rpm.grammar.patterns.TSPatterns;
import net.seninp.jmotif.sax.NumerosityReductionStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

public class ConnectGI {

	/**
	 * Get patterns from concatenated data with Sequitur.
	 * 
	 * @param concatenateData - a Map from class label to the concatenated time series data.
	 * @param params - Parameter Vector: Window Size ([0][0]), PAA Size ([0][1]), Alphabet Size ([0][2]),
	 *                  Numerosity Reduction Strategy ([0][3]).
	 * @param giMethod - The Grammar Induction Method to be used.
	 * @param allStartPositions - A map for every class label to an integer array of all the start points in the
	 *                          concatenated data.
	 * @param rpFrequencyTPer @TODO.
	 * @param maxRPNum @TODO.
	 * @param overlapTPer @TODO.
	 * @param isCoverageFre @TODO.
	 * @param pSimilarity - the similarity between patterns.
	 * @return
	 */
	public HashMap<String, TSPatterns> getPatternsFromSequitur(
			HashMap<String, double[]> concatenateData, int[][] params,
			GrammarInductionMethod giMethod,
			HashMap<String, int[]> allStartPositions, double rpFrequencyTPer,
			int maxRPNum, double overlapTPer, Boolean isCoverageFre,
			PatternsSimilarity pSimilarity) {

		HashMap<String, TSPatterns> allPatterns = new HashMap<String, TSPatterns>();

		int windowSize = params[0][0];
		int paaSize = params[0][1];
		int alphabetSize = params[0][2];
		int strategy = params[0][3];
		NumerosityReductionStrategy nRStrategy = NumerosityReductionStrategy
				.fromValue(strategy);

		for (Entry<String, double[]> entry : concatenateData.entrySet()) {
			String classLabel = entry.getKey();
			double[] concatenatedTS = entry.getValue();
			int[] startPositions = allStartPositions.get(classLabel);
			//System.err.println("Class label = " + classLabel + " length of ts = " + concatenatedTS.length);

			GetRulesFromGI gi = new GetRulesFromGI();
			ArrayList<int[]> patternsLocation = gi.getGrammars(windowSize,
					paaSize, alphabetSize, nRStrategy, concatenatedTS,
					giMethod, startPositions, rpFrequencyTPer, maxRPNum,
					overlapTPer, isCoverageFre, pSimilarity);

			if (patternsLocation == null) {
				//System.err.println("Patterns Location is null so returning null!!");
				return null;
			}

			TSPatterns patterns = new TSPatterns(classLabel);

			readPatterns(concatenatedTS, patternsLocation, patterns,
					startPositions);

			allPatterns.put(classLabel, patterns);
		}

		return allPatterns;
	}

	/**
	 * Read subsequences according to the location of patterns in concatenated
	 * time series.
	 * 
	 * @param concatenatedTS - the concatenated time series.
	 * @param patternsLocation - list of locations for each pattern of where it is in the concatenatedTS.
	 * @param patterns - list of time series patterns.
	 * @param startingPositions - list of positions where the time series entries are located in concatenatedTS.
	 */
	public static void readPatterns(double[] concatenatedTS,
			ArrayList<int[]> patternsLocation, TSPatterns patterns,
			int[] startingPositions) {

		// Start place, length, frequency.
		for (int[] location : patternsLocation) {
			int startPosition = location[0];
			if (startPosition < 0) {
				continue;
			}

			int patternLength = location[1];
			int frequency = location[2];

			double[] patternTS = Arrays.copyOfRange(concatenatedTS,
					startPosition, startPosition + patternLength);

			TSPattern tp = new TSPattern(frequency, patternTS,
					patterns.getLabel(), startPosition);
			int sp = findIdx(startingPositions, startPosition);
			tp.setFromTS(sp);
			patterns.addPattern(tp);
		}

	}

	/**
	 * Find the index of a pattern in the concatenated list using a list of starting positions and the patterns
	 * starting position.
	 *
	 * @param startingPositions - All the start positions.
	 * @param startPosition - The start position of the pattern in question.
	 * @return - The index of the pattern.
	 */
	public static int findIdx(int[] startingPositions, int startPosition) {
		int idx = 1;
		for (int sp : startingPositions) {
			if (sp >= startPosition)
				break;
			idx++;
		}

		return idx;
	}

}
