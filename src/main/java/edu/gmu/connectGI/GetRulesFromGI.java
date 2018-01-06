package edu.gmu.connectGI;

import edu.gmu.grammar.classification.util.DistMethods;
import edu.gmu.grammar.patterns.PatternsSimilarity;
import net.seninp.gi.logic.GrammarRules;
import net.seninp.gi.logic.RuleInterval;
import net.seninp.gi.sequitur.SAXRule;
import net.seninp.gi.sequitur.SequiturFactory;
import net.seninp.gi.sequitur.SequiturFactoryWithEscape;
import net.seninp.grammarviz.logic.GrammarVizChartData;
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

	private double calcDistThreshold(ArrayList<RepeatedPattern> allRepeatedPatterns) {
		// Put all repeated pattern together.
		ArrayList<RuleInterval> allPatterns = new ArrayList<RuleInterval>();
		for (RepeatedPattern rp : allRepeatedPatterns) {
			allPatterns.addAll(rp.getSequences());
		}

		int numPatterns = allPatterns.size();
		int min = 0;
		int max = numPatterns - 1;

		ArrayList<Double> pDists = new ArrayList<Double>();
		for (int i = 0; i < numPatterns; i++) {
			int[] idxes = generateRandomTwoIdxes(min, max);
			int idx1 = idxes[0];
			int idx2 = idxes[1];

			RuleInterval ri1 = allPatterns.get(idx1);
			int startPosition1 = ri1.getStart();
			int patternLength1 = ri1.getLength();
			double[] patternTS1 = Arrays.copyOfRange(concatenatedTS, startPosition1, startPosition1 + patternLength1);

			RuleInterval ri2 = allPatterns.get(idx2);
			int startPosition2 = ri2.getStart();
			int patternLength2 = ri2.getLength();
			double[] patternTS2 = Arrays.copyOfRange(concatenatedTS, startPosition2, startPosition2 + patternLength2);

			double d;
			if (patternTS1.length > patternTS2.length)
				d = DistMethods.calcDistEuclidean(patternTS1, patternTS2);
			else
				d = DistMethods.calcDistEuclidean(patternTS2, patternTS1);
			pDists.add(d);
		}

		Collections.sort(pDists);

		int idx25 = (int) (numPatterns * 0.05);

		return pDists.get(idx25);
	}

	private int[] generateRandomTwoIdxes(int min, int max) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		int[] randomTwo = new int[2];
		for (int i = min; i <= max; i++) {
			list.add(new Integer(i));
		}
		Collections.shuffle(list);
		randomTwo[0] = list.get(0);
		randomTwo[1] = list.get(1);
		return randomTwo;
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

	/**
	 * XING
	 * 
	 * @param allRepeatedPatterns
	 * @return
	 */
	private ArrayList<int[]> findRepeatedPatterns(ArrayList<RepeatedPattern> allRepeatedPatterns,
			int[] startingPositions, Boolean isCoverageFre) {

		ArrayList<int[]> allPatterns = new ArrayList<int[]>();

		for (RepeatedPattern rp : allRepeatedPatterns) {

			// int frequency = rp.getSequences().size();
			// RuleInterval bsfInterval = calcRepresentative(rp.getSequences());
			//
			// int[] p = { bsfInterval.getStartPos(), bsfInterval.getLength(),
			// frequency };
			// allPatterns.add(p);

			if (rp.getSequences().size() > 1) {
				PatternsProcess pp = new PatternsProcess();
				allPatterns.addAll(pp.refinePatternsByClustering(concatenatedTS, rp, startingPositions, isCoverageFre));
			} else {
				// int frequency = rp.getSequences().size();
				// RuleInterval bsfInterval =
				// calcRepresentative(rp.getSequences());
				//
				// int[] p = { bsfInterval.getStartPos(),
				// bsfInterval.getLength(),
				// frequency };
				// allPatterns.add(p);
				continue;
			}

		}
		if (allPatterns.size() < 1)
			return null;
		return allPatterns;
	}

	public RuleInterval calcRepresentative(ArrayList<RuleInterval> arrPos) {
		RuleInterval bsfInterval = new RuleInterval();
		double[] origTS = this.concatenatedTS;

		bsfInterval = getCentroid(origTS, arrPos);

		return bsfInterval;
	}

	public RuleInterval getCentroid(double[] origTS, ArrayList<RuleInterval> arrPos) {

		RuleInterval centroid = new RuleInterval();

		int tsNum = arrPos.size();

		double dt[][] = new double[tsNum][tsNum];
		for (int i = 0; i < arrPos.size(); i++) {
			RuleInterval saxPos = arrPos.get(i);

			int start1 = saxPos.getStart();
			int end1 = saxPos.getEnd();
			double[] ts1 = Arrays.copyOfRange(origTS, start1, end1);

			for (int j = 0; j < arrPos.size(); j++) {
				RuleInterval saxPos2 = arrPos.get(j);
				if (dt[i][j] > 0) {
					continue;
				}
				double d = 0;
				dt[i][j] = d;
				if (i == j) {
					continue;
				}

				int start2 = saxPos2.getStart();
				int end2 = saxPos2.getEnd();
				double[] ts2 = Arrays.copyOfRange(origTS, start2, end2);

				if (ts1.length > ts2.length)
					d = DistMethods.calcDistEuclidean(ts1, ts2);
				else
					d = DistMethods.calcDistEuclidean(ts2, ts1);

				// if (ts1.length > ts2.length)
				// ts1 = Arrays.copyOfRange(ts1, 0, ts2.length);
				// else if (ts1.length < ts2.length)
				// ts2 = Arrays.copyOfRange(ts2, 0, ts1.length);
				// d = DistMethods.eculideanDistNorm(ts1, ts2);

				// DTW dtw = new DTW(ts1, ts2);
				// d = dtw.warpingDistance;

				dt[i][j] = d;
			}
		}

		int bestLine = -1;
		double smallestValue = 10000000.00;

		for (int k = 0; k < arrPos.size(); k++) {
			double dk = 0;
			for (int m = 0; m < arrPos.size(); m++) {
				dk += dt[k][m];
			}
			if (smallestValue > dk) {
				smallestValue = dk;
				bestLine = k;
			}
		}

		if (bestLine > -1)
			centroid = arrPos.get(bestLine);

		return centroid;
	}

	/**
	 * The number of repeated patterns has to be at least half of the
	 * concatenated TS
	 * 
	 * @param startingPositions
	 * @return
	 */
	public Boolean isParameterGood(int[] startingPositions) {
		// The number of concatenated time series.
		int originalTSNum = startingPositions.length + 1;
		// Threshold of repeated patterns number.
		int numberThreshold = (int) (originalTSNum * 0.5);

		int bestFrequency = 0;
		int totalRule = 0;
		if (giMethod.index() == GrammarInductionMethod.REPAIR.index()) {
			// totalRule = this.getRulesNumberRepair();
		} else {
			totalRule = chartData.getRulesNumber();
		}
		// Get the frequency of the most frequent pattern
		for (int idx = 1; idx < totalRule; idx++) {
			ArrayList<RuleInterval> arrPos = null;
			if (giMethod.index() == GrammarInductionMethod.REPAIR.index()) {
				// arrPos = chartData.getRulePositionsByRuleNumRepair(idx);
			} else {

				arrPos = chartData.getRulePositionsByRuleNum(idx);
			}

			int frequency = arrPos.size();
			if (bestFrequency < frequency)
				bestFrequency = frequency;
		}
		// If there is not enough repeated patterns return false.
		if (bestFrequency < numberThreshold)
			return false;
		return true;
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
		if (giMethod.index() == giMethod.SEQUITUR.index()) {
			sb.append("algorithm: Sequitur, ");
		} else {
			sb.append("algorithm: RePair, ");
		}
		// sb.append("sliding window ").append(useSlidingWindow);
		sb.append(", numerosity reduction ").append(numerosityReductionStrategy.toString());
		sb.append(", SAX window ").append(windowSize);
		sb.append(", PAA ").append(paaSize);
		sb.append(", Alphabet ").append(alphabetSize);
		// consoleLogger.info(sb.toString());
		// this.log(sb.toString());
		//System.err.println(sb.toString());
		//System.err.println("Timeseries length = " + concatenatedTS.length);
		// consoleLogger.debug("creating ChartDataStructure");
		// TODO: Add the data file name.
		String dataFileName = "";
		chartData = new GrammarVizChartData(dataFileName, concatenatedTS, useSlidingWindow, numerosityReductionStrategy,
				windowSize, alphabetSize, paaSize);

		try {

			if (giMethod.index() == giMethod.SEQUITUR.index()) {

				SAXProcessor sp = new SAXProcessor();
				NormalAlphabet normalA = new NormalAlphabet();

				SAXRecords saxFrequencyData = null;
				if (useSlidingWindow) {
					// consoleLogger.debug("discretizing string ...");
					saxFrequencyData = sp.ts2saxViaWindowGlobalZNorm(concatenatedTS, windowSize, paaSize,
							normalA.getCuts(alphabetSize), numerosityReductionStrategy, normalizationThreshold);
					// saxFrequencyData =
					// SequiturFactory.discretize(concatenatedTS,
					// numerosityReductionStrategy,
					// windowSize, paaSize, alphabetSize,
					// normalizationThreshold);
				} else {
					// SequiturFactory.discretizeNoSlidingWindow(concatenatedTS,
					// paaSize, alphabetSize,
					// normalizationThreshold);
				}


				//System.err.println("Sax freq data = " + saxFrequencyData.getAllIndices().size());
				// consoleLogger.trace("String: "
				// + saxFrequencyData.getSAXString(SPACE));

				ArrayList<Integer> saxWordsIndexes = new ArrayList<Integer>(saxFrequencyData.getAllIndices());
				// consoleLogger.debug("running sequitur ...");
				SAXRule sequiturGrammar = SequiturFactoryWithEscape.runSequitur(saxFrequencyData.getSAXString(SPACE),
						startingPositions, saxWordsIndexes, windowSize);
				saxWordsIndexes.clear();
				// consoleLogger.debug("collecting grammar rules data ...");
				GrammarRules rules = sequiturGrammar.toGrammarRulesData();
				//System.err.println("Number of Grammar rules = " + rules.size());

				// consoleLogger.debug("mapping rule intervals on timeseries
				// ...");
				SequiturFactory.updateRuleIntervals(rules, saxFrequencyData, useSlidingWindow, concatenatedTS,
						windowSize, paaSize);

				// consoleLogger.debug("done ...");
				chartData.setGrammarRules(rules);
				//System.err.println("Number of rules = " + rules.size());


			} else {

				// ParallelSAXImplementation ps = new
				// ParallelSAXImplementation();
				// SAXRecords parallelRes = ps.process(concatenatedTS, 2,
				// windowSize, paaSize, alphabetSize,
				// NumerosityReductionStrategy.EXACT,
				// normalizationThreshold);
				//
				// @SuppressWarnings("unused")
				// RePairRule rePairGrammar = RePairFactory
				// .buildGrammar(parallelRes);
				//
				// RePairRule.expandRules();
				// RePairRule.buildIntervals(parallelRes, concatenatedTS,
				// windowSize);
				//
				// GrammarRules rules = RePairRule.toGrammarRulesData();
				//
				// chartData.setGrammarRules(rules);

			}

		} catch (Exception e) {
			// this.log("error while processing data " +
			// StackTrace.toString(e));
			e.printStackTrace();
		}

	}

	public ArrayList<int[]> getPatternsLocation() {
		return patternsLocation;
	}

	public void setPatternsLocation(ArrayList<int[]> patternsLocation) {
		this.patternsLocation = patternsLocation;
	}

	// public ArrayList<RepeatedPattern> getAllPatterns(int wdSize, int paaSize,
	// int alphabetaSize, String numerosityReductionStrategy,
	// double[] concatenatedTS, int orignalLen,
	// GrammarInductionMethod giMethod, int[] startingPositions) {
	// boolean useSlidingWindow = true;
	//
	// MotifChartData chartData = new MotifChartData(concatenatedTS,
	// useSlidingWindow, numerosityReductionStrategy, wdSize,
	// alphabetaSize, paaSize, orignalLen, giMethod, startingPositions);
	// chartData.buildSAX();
	//
	// patternsLocation = chartData.getPatterns();
	// return chartData.getAllRepeatedPatterns();
	// }

}
