package com.dwicke.tsat.rpm.connectGI;

import com.dwicke.tsat.logic.GrammarVizChartData;
import com.dwicke.tsat.rpm.grammar.classification.util.DistMethods;
import com.dwicke.tsat.rpm.grammar.patterns.PatternsSimilarity;
import net.seninp.gi.logic.GrammarRules;
import net.seninp.gi.logic.RuleInterval;
import net.seninp.gi.sequitur.SAXRule;
import net.seninp.gi.sequitur.SequiturFactory;
import net.seninp.gi.sequitur.SequiturFactoryWithEscape;
import net.seninp.jmotif.sax.NumerosityReductionStrategy;
import net.seninp.jmotif.sax.SAXProcessor;
import net.seninp.jmotif.sax.alphabet.NormalAlphabet;
import net.seninp.jmotif.sax.datastructure.SAXRecords;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class GetRulesFromGI {
	private ArrayList<int[]> patternsLocation;

	private String SPACE = " ";
	private GrammarVizChartData chartData;
	int[] startingPositions;
	// int orignalLen;
	GrammarInductionMethod giMethod;
	int windowSize;
	double[] concatenatedTS;

	/**
	 * Constructor, sets defaults (null).
	 */
	public GetRulesFromGI() {
		chartData = null;
		startingPositions = null;
		giMethod = null;
		concatenatedTS = null;
	}

	/**
	 * Returns a refined set of patterns that represent the grammar of the time series data.
	 *
	 * @param windowSize the window size.
	 * @param paaSize the paa size.
	 * @param alphabetSize the alphabet size.
	 * @param numerosityReductionStrategy the numerosity reduction strategy.
	 * @param concatenatedTS the concatenated time series data.
	 * @param giMethod the grammar induction method.
	 * @param startingPositions the parting positions of time series in concatenatedTS
	 * @param rpFrequencyTPer the repeated pattern frequency threshold.
	 * @param maxRPNum
	 * @param overlapTPer
	 * @param isCoverageFre
	 * @param pSimilarity
	 * @return the grammars of the time series data.
	 */
	public ArrayList<int[]> getGrammars(int windowSize, int paaSize, int alphabetSize,
										NumerosityReductionStrategy numerosityReductionStrategy, double[] concatenatedTS,
										GrammarInductionMethod giMethod, int[] startingPositions, double rpFrequencyTPer, int maxRPNum,
										double overlapTPer, Boolean isCoverageFre, PatternsSimilarity pSimilarity) {
		this.startingPositions = startingPositions;
		// this.orignalLen = orignalLen;
		this.windowSize = windowSize;
		this.giMethod = giMethod;
		this.concatenatedTS = concatenatedTS;

		ArrayList<int[]> patterns = null;

		try {

			NumerosityReductionStrategy strategy = numerosityReductionStrategy;
			//System.err.println("ConcatenatedTS = " + concatenatedTS.length + " starting positions = " + startingPositions);
			// Get all rules from Sequitur
			processData(windowSize, paaSize, alphabetSize, strategy, concatenatedTS, giMethod, startingPositions);

			// if (isParameterGood(startingPositions)) {
			int originalTSNum = startingPositions.length + 1;
			int realRepeatedThreshold = (int) (originalTSNum * rpFrequencyTPer);
			if (realRepeatedThreshold < 1)
				realRepeatedThreshold = 1;
			// Put all patterns together, get repeated patterns by their
			// start
			// position. Prune the repeated pattern less than a threshold.
			//System.err.println("OriginalTSNum = " + originalTSNum + " realRepeatedThreshold = " + realRepeatedThreshold);
			ArrayList<RepeatedPattern> allRepeatedPatterns = getRepeatedPatterns(realRepeatedThreshold, maxRPNum,
					overlapTPer, startingPositions, isCoverageFre, pSimilarity);

			if (allRepeatedPatterns.size() > 0) {
				patterns = findRepeatedPatterns(allRepeatedPatterns, startingPositions, isCoverageFre);
			} else {
				patterns = null;
				//System.err.println("Patterns was set to null so i'm going to return null this is really bad...!!!");
			}

			allRepeatedPatterns.clear();
			// }

		} catch (IOException e) {
			e.printStackTrace();
		}
		return patterns;
	}

	/**
	 * Put all patterns together. Based on their position, we could know which
	 * are the repeated patterns (In this concatenated time series, all original
	 * time series belong to one class, repeated pattern should be at the same
	 * position).
	 * 
	 * @param realRepeatedThreshold
	 * @return
	 */
	public ArrayList<RepeatedPattern> getRepeatedPatterns(int realRepeatedThreshold, int maxRPNum, double overlapTPer,
			int[] startingPositions, Boolean isCoverageFre, PatternsSimilarity pSimilarity) {

		// int removeDiffT = 5;
		// Threshold of start position difference. Check overlap.
		int positionDiffT = (int) (windowSize * overlapTPer);
		int pDiffThreshold = positionDiffT;
		// int pDiffThreshold = 5 > positionDiffT ? 5 : positionDiffT;
		// Threshold of length difference. Motif with different length.
		double lDiffThrePer = 0.5;

		ArrayList<RepeatedPattern> allRepeatedPatterns = new ArrayList<RepeatedPattern>();

		int totalRule = 0;
		if (giMethod.index() == GrammarInductionMethod.REPAIR.index()) {
			// totalRule = this.getRulesNumberRepair();
		} else {
			totalRule = chartData.getRulesNumber();
		}
		//System.err.println("ChartData totalRule = " + totalRule);
		for (int idx = 1; idx < totalRule; idx++) {
			ArrayList<RepeatedPattern> tempRepeatedPatterns = new ArrayList<RepeatedPattern>();

			ArrayList<RuleInterval> arrPos = null;
			if (giMethod.index() == GrammarInductionMethod.REPAIR.index()) {
				// arrPos = this.getRulePositionsByRuleNumRepair(idx);
			} else {

				arrPos = chartData.getRulePositionsByRuleNum(idx);
			}
			if (arrPos.size() < realRepeatedThreshold) {
				continue;
			}
			//System.err.println("Number of rule subsequences = " + arrPos.size() + " threshold is = " + realRepeatedThreshold);

			for (RuleInterval p : arrPos) {
				int len = p.getLength();

				Boolean found = false;

				for (RepeatedPattern rp : tempRepeatedPatterns) {
					// if (Math.abs(len - rp.getLength()) <= (lDiffThrePer * rp
					// .getLength())) {
					//
					// rp.getSequences().add(p);
					// found = true;
					// break;
					// }
					int rpLen = rp.getLength();
					double proportion = len / rpLen;
					if (proportion < lDiffThrePer || proportion > 1 / lDiffThrePer) {
					} else {
						rp.getSequences().add(p);
						found = true;
						break;
					}
				}

				if (!found) {
					RepeatedPattern newRP = new RepeatedPattern(len, p, startingPositions);
					tempRepeatedPatterns.add(newRP);
				}

			}

			if (tempRepeatedPatterns.size() > 0)
				allRepeatedPatterns.addAll(tempRepeatedPatterns);
		}

		// removeOverlapBetweenCluster(allRepeatedPatterns);
		removeOverlapWithinCluster(allRepeatedPatterns, pDiffThreshold);

		// If the threshold for similarity hasn't been calculated, this method
		// will calculate it.
		// It will be calculated only once.
		if (!pSimilarity.getIsTSimilarSetted()) {
			if (allRepeatedPatterns.size() > 1) {
				double tSimilar = calcDistThreshold2(allRepeatedPatterns);
				pSimilarity.addCandidate(tSimilar);
				//System.err.println("TSimilar Candidate ======================================= " + tSimilar);
			}
		}


		removeNotFrequentGroups(allRepeatedPatterns, maxRPNum, realRepeatedThreshold, isCoverageFre);

		return allRepeatedPatterns;
	}

	private double calcDistThreshold2(ArrayList<RepeatedPattern> allRepeatedPatterns) {

		ArrayList<Double> withinDistances = new ArrayList<Double>();

		for (RepeatedPattern rp : allRepeatedPatterns) {
			ArrayList<RuleInterval> riWithin = rp.getSequences();
			int numPatterns = riWithin.size();
			if (numPatterns > 1) {
				// compute pairwise distances
				for (int i = 0; i < numPatterns; i++) {
					RuleInterval riI = riWithin.get(i);
					int startPositionI = riI.getStart();
					int patternLengthI = riI.getLength();
					double[] patternTSI = Arrays.copyOfRange(concatenatedTS, startPositionI,
							startPositionI + patternLengthI);
					for (int j = i + 1; j < numPatterns; j++) {
						RuleInterval riJ = riWithin.get(j);
						int startPositionJ = riJ.getStart();
						int patternLengthJ = riJ.getLength();
						double[] patternTSJ = Arrays.copyOfRange(concatenatedTS, startPositionJ,
								startPositionJ + patternLengthJ);

						double d;
						if (patternTSI.length > patternTSJ.length)
							d = DistMethods.calcDistEuclidean(patternTSI, patternTSJ);
						else
							d = DistMethods.calcDistEuclidean(patternTSJ, patternTSI);

						withinDistances.add(d);
					}
				}
			}
		}

		if (withinDistances.size() > 0) {
			// double sum = 0;
			// for (double d : withinDistances)
			// sum += d;
			//
			// return sum / withinDistances.size();
			Collections.sort(withinDistances);
			int idx25 = (int) (withinDistances.size() * 0.50);
			return withinDistances.get(idx25);
		} else {
			return 0.02;
		}
	}





	public void removeNotFrequentGroups(ArrayList<RepeatedPattern> allRepeatedPatterns, int maxRPNum,
			int realRepeatedThreshold, Boolean isCoverageFre) {

		int count = 0;
		while ((allRepeatedPatterns.size() > maxRPNum) || count == 0) {
			ArrayList<Integer> notQualifiedRP = new ArrayList<Integer>();
			// Remove entry whose frequency not enough.
			for (int i = 0; i < allRepeatedPatterns.size(); i++) {
				RepeatedPattern rp = allRepeatedPatterns.get(i);
				int patternFrequency = rp.getSequences().size();
				if (isCoverageFre)
					patternFrequency = rp.getFrequency();
				if (patternFrequency < realRepeatedThreshold) {
					notQualifiedRP.add(i);
				}
			}

			Collections.sort(notQualifiedRP, Collections.reverseOrder());
			for (int i : notQualifiedRP) {
				allRepeatedPatterns.remove(i);
			}

			realRepeatedThreshold += realRepeatedThreshold;
			// System.out.println(count++);
			count++;
		}
	}

	/**
	 * Remove the overlapped sub-sequences.
	 * 
	 * @param allRepeatedPatterns
	 * @param pDiffThreshold
	 */
	public void removeOverlapWithinCluster(ArrayList<RepeatedPattern> allRepeatedPatterns, int pDiffThreshold) {

		//System.err.println("Number of repeated patterns before removing them: " + allRepeatedPatterns.size());
		for (int i = 0; i < allRepeatedPatterns.size(); i++) {

			ArrayList<RuleInterval> arrPos = allRepeatedPatterns.get(i).getSequences();
			int frequency = arrPos.size();

			ArrayList<Integer> removeIdx = new ArrayList<Integer>();
			for (int j = 0; j < frequency; j++) {

				if (removeIdx.contains(j)) {
					continue;
				}

				RuleInterval rj = arrPos.get(j);

				int lenj = rj.getLength();
				int startj = rj.getStart();
				int endj = rj.getEnd();

				for (int k = j + 1; k < frequency; k++) {

					if (removeIdx.contains(k)) {
						// TODO: Algorithm for removing redundant subsequences
						continue;
					}

					RuleInterval rk = arrPos.get(k);
					int lenk = rk.getLength();
					int startk = rk.getStart();
					int endk = rk.getEnd();

					if (Math.abs(startk - startj) <= pDiffThreshold) {
						removeIdx.add(k);
					}
				}
			}

			Collections.sort(removeIdx, Collections.reverseOrder());

			for (int ridx : removeIdx) {
				arrPos.remove(ridx);
			}

			// if (removeIdx.size() > 0)
			// System.out.println(removeIdx.size()
			// + " overlaped sequences were removed!");

		}

	}


	private ArrayList<int[]> findRepeatedPatterns(ArrayList<RepeatedPattern> allRepeatedPatterns,
			int[] startingPositions, Boolean isCoverageFre) {

		ArrayList<int[]> allPatterns = new ArrayList<int[]>();

		for (RepeatedPattern rp : allRepeatedPatterns) {
			if (rp.getSequences().size() > 1) {
				PatternsProcess pp = new PatternsProcess();
				allPatterns.addAll(pp.refinePatternsByClustering(concatenatedTS, rp, startingPositions, isCoverageFre));
			}
		}
		if (allPatterns.size() < 1)
			return null;
		return allPatterns;
	}



	/**
	 * Process data with Sequitur. Populate and broadcast ChartData object.
	 *
	 * @param windowSize The SAX sliding window size.
	 * @param paaSize The SAX PAA size.
	 * @param alphabetSize The SAX alphabet size.
	 * @param numerosityReductionStrategy The numerosity reduction strategy.
	 * @param concatenatedTS the concatenated time series data.
	 * @param giMethod the grammar induction method to be used.
	 * @param startingPositions the list of starting position for the time series in the concatenatedTS.
	 * @throws IOException
	 */
	public void processData(int windowSize, int paaSize, int alphabetSize,
							NumerosityReductionStrategy numerosityReductionStrategy, double[] concatenatedTS,
							GrammarInductionMethod giMethod, int[] startingPositions) throws IOException {

		double normalizationThreshold = 0.005;
		boolean useSlidingWindow = true;

		// the logging block
		//
		StringBuffer sb = new StringBuffer("setting up GI with params: ");
		if (giMethod.index() == GrammarInductionMethod.SEQUITUR.index()) {
			sb.append("algorithm: Sequitur, ");
		} else {
			sb.append("algorithm: RePair, ");
		}
		// sb.append("sliding window ").append(useSlidingWindow);
		sb.append(", numerosity reduction ").append(numerosityReductionStrategy.toString());
		sb.append(", SAX window ").append(windowSize);
		sb.append(", PAA ").append(paaSize);
		sb.append(", Alphabet ").append(alphabetSize);


		chartData = new GrammarVizChartData(concatenatedTS, useSlidingWindow, numerosityReductionStrategy,
				windowSize, alphabetSize, paaSize);

		try {

			if (giMethod.index() == GrammarInductionMethod.SEQUITUR.index()) {

				SAXProcessor sp = new SAXProcessor();
				NormalAlphabet normalA = new NormalAlphabet();

				SAXRecords saxFrequencyData = null;
				if (useSlidingWindow) {
					// consoleLogger.debug("discretizing string ...");
					saxFrequencyData = sp.ts2saxViaWindow(concatenatedTS, windowSize, paaSize,
							normalA.getCuts(alphabetSize), numerosityReductionStrategy, normalizationThreshold);


				}

				ArrayList<Integer> saxWordsIndexes = new ArrayList<>(saxFrequencyData.getAllIndices());

				SAXRule sequiturGrammar = SequiturFactoryWithEscape.runSequitur(saxFrequencyData.getSAXString(SPACE),
						startingPositions, saxWordsIndexes, windowSize);
				saxWordsIndexes.clear();
				GrammarRules rules = sequiturGrammar.toGrammarRulesData();

				SequiturFactory.updateRuleIntervals(rules, saxFrequencyData, useSlidingWindow, concatenatedTS,
						windowSize, paaSize);

				// consoleLogger.debug("done ...");
				chartData.setGrammarRules(rules);
			}

		} catch (Exception e) {
			// this.log("error while processing data " +
			// StackTrace.toString(e));
			e.printStackTrace();
		}

	}




}
