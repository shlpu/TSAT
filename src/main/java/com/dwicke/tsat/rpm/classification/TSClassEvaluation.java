package com.dwicke.tsat.rpm.classification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.dwicke.tsat.rpm.connectGI.ConnectGI;
import com.dwicke.tsat.rpm.connectGI.GrammarInductionMethod;
import com.dwicke.tsat.rpm.patterns.PatternsAndTransformedData;
import com.dwicke.tsat.rpm.patterns.PatternsSimilarity;
import com.dwicke.tsat.rpm.patterns.TSPattern;
import com.dwicke.tsat.rpm.patterns.TSPatterns;
import net.seninp.jmotif.direct.Point;
import net.seninp.jmotif.sax.NumerosityReductionStrategy;
import net.seninp.util.StackTrace;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.LoggerFactory;
import weka.classifiers.Evaluation;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

public class TSClassEvaluation {

	public static final Character DELIMITER = '~';
	final static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	public HashMap<String, TSPatterns> thisRPatterns;

	public TSPattern[] selectedRepresentativePatterns;

	// the default numerosity strategy
	private NumerosityReductionStrategy numerosityReductionStrategy;

	private ArrayList<double[]> trainData;
	private Map<String, List<double[]>> trainDataPerClass;

	private GrammarInductionMethod giMethod;
	private int folderNum;
	private double rpFrequencyTPer;
	private int maxRPNum;
	private double overlapTPer;
	private Boolean isCoverageFre;
	PatternsSimilarity pSimilarity;
	int clsNum;

	private static final Logger consoleLogger;
	private static final Level LOGGING_LEVEL = Level.INFO;
	static {
		consoleLogger = (Logger) LoggerFactory
				.getLogger(TSClassEvaluation.class);
		consoleLogger.setLevel(LOGGING_LEVEL);
	}

	/**
	 * Constructor.
	 *
	 */
	public TSClassEvaluation(Map<String, List<double[]>> inputTrainData,
							 NumerosityReductionStrategy strategy,
							 GrammarInductionMethod giMethod, int folderNum,
							 double rpFrequencyTPer, int maxRPNum, double overlapTPer,
							 Boolean isCoverageFre, PatternsSimilarity pSimilarity) {
		this.trainData = new ArrayList<>();
		this.trainDataPerClass = new HashMap<>();
		for (Entry<String, List<double[]>> e : inputTrainData.entrySet()) {
			String label = e.getKey();
			List<double[]> tsesInClass = e.getValue();

			List<double[]> tses = new ArrayList<>();

			int idx = 1;
			for (double[] ts : tsesInClass) {
				trainData.add(ts);
				tses.add(ts);
				idx++;
			}

			this.trainDataPerClass.put(label, tses);
		}

		this.numerosityReductionStrategy = strategy;
		this.giMethod = giMethod;
		this.folderNum = folderNum;
		this.rpFrequencyTPer = rpFrequencyTPer;
		this.maxRPNum = maxRPNum;
		this.overlapTPer = overlapTPer;
		this.isCoverageFre = isCoverageFre;
		this.pSimilarity = pSimilarity;
	}


	public static HashMap<String, double[]> concatenateTrainInTrain(
			Map<String, List<double[]>> trainDataPerClass, HashMap<String, int[]> allStartPositions) {

		HashMap<String, double[]> concatenatedData = new HashMap<>();

		List<Entry<String, List<double[]>>> list = new ArrayList<>(trainDataPerClass.entrySet());
		Collections.shuffle(list);

		for (Entry<String, List<double[]>> e : list) {
			String classLabel = e.getKey();

			// Record the start point of time series in concatenated one.
			ArrayList<Integer> temp = new ArrayList<>();
			int startPoint = 0;

			int tsNum = e.getValue().size();
			int tsIdx = 1;

			for (double[] series : e.getValue()) {

				double[] existSeries = concatenatedData.get(classLabel);
				if (null == existSeries) {
					concatenatedData.put(classLabel, series);
				} else {
					double[] newExistSeries = ArrayUtils.addAll(existSeries, series);
					concatenatedData.put(classLabel, newExistSeries);

				}
				if (tsIdx < tsNum) {
					startPoint += series.length;
					temp.add(startPoint);
				}
				tsIdx++;
			}

			int[] tempInt = temp.stream().mapToInt(i -> i).toArray();
			allStartPositions.put(classLabel, tempInt);

		}

		return concatenatedData;
	}



	public Evaluation evaluateTS(Point point) {
		// point is in fact a aset of parameters - window, paa, and the alphabet
		//
		double[] coords = point.toArray();
		int windowSize = Long.valueOf(Math.round(coords[0])).intValue();
		int paaSize = Long.valueOf(Math.round(coords[1])).intValue();
		int alphabetSize = Long.valueOf(Math.round(coords[2])).intValue();

		//System.err.println("Starting valule at transform multi class!");
		// if we stepped above window length with PAA size - for some reason -
		// return the max possible
		// error value
		if (paaSize > windowSize) {
			pSimilarity.clear();
			return null;
		}

		//System.err.println(" Going to concatenate stuff");
		// the whole thing begins here
		//
		try {
			// make a parameters vector
			int[][] params = new int[1][4];
			params[0][0] = windowSize;
			params[0][1] = paaSize;
			params[0][2] = alphabetSize;
			params[0][3] = this.numerosityReductionStrategy.index();

			HashMap<String, int[]> allStartPositions = new HashMap<>();

			// Concatenate training time series
			HashMap<String, double[]> concatenateData = concatenateTrainInTrain(trainDataPerClass,
					allStartPositions);

			// Get representative patterns
			ConnectGI cgi = new ConnectGI();
			HashMap<String, TSPatterns> allPatterns = cgi
					.getPatternsFromSequitur(concatenateData, params, giMethod,
							allStartPositions, rpFrequencyTPer, maxRPNum,
							overlapTPer, isCoverageFre, pSimilarity);
			concatenateData.clear();

			if (allPatterns == null) {
				pSimilarity.clear();
				return null;
			}

			HashMap<String, TSPatterns> topFrequentPatterns = allPatterns;

			clsNum = topFrequentPatterns.size();


			ProcessMultiClass gcp = new ProcessMultiClass(folderNum);

			// Transform the original time series into the features space of
			// distance to selected patterns.
			PatternsAndTransformedData pTransformTS = gcp.transformTS(
					topFrequentPatterns, trainDataPerClass, pSimilarity);
			// The transformed time series. The last column is the label of
			// class.
			double[][] transformedTS = pTransformTS.getTransformedTS();
			// Put patterns from each class together into an array.
			TSPattern[] allPatternsTogether = pTransformTS.getAllPatterns();

			//System.err.println("Going  to apply feature selection");

			// Apply feature selection method on the transformed data.
			int[] selectedIndices = gcp.featureSelection(transformedTS);
			// Selected patterns from the result of feature selection
			TSPattern[] selectedPatterns = new TSPattern[selectedIndices.length - 1];
			double[][] newTransformedTS = new double[transformedTS.length][selectedIndices.length];
			for (int i = 0; i < selectedIndices.length; i++) {
				if (i != selectedIndices.length - 1)
					selectedPatterns[i] = allPatternsTogether[selectedIndices[i]];
				for (int j = 0; j < transformedTS.length; j++) {
					newTransformedTS[j][i] = transformedTS[j][selectedIndices[i]];
				}
			}

			//System.err.println("did the new transform");

			selectedRepresentativePatterns = selectedPatterns.clone();

			Evaluation evaluation = gcp.cvEvaluationAllCls(newTransformedTS);

			double allError = evaluation.errorRate();
			if (pSimilarity.getIsTSimilarSetted()) {
			} else {
				if ((allError < 1)) {
					// set pSimilarity

					ArrayList<Double> distCandidates = pSimilarity
							.gettCandidates();
					double numCandi = distCandidates.size();
					if (numCandi > 0) {

						Collections.sort(distCandidates);
						double tSimilar = distCandidates.get(0);

						pSimilarity.setIsTSimilarSetted(true);
						pSimilarity.settSimilar(tSimilar);
						pSimilarity.clear();

						System.out.println("Similarity Threshold Setted!"
								+ tSimilar);
					}
				} else {
					pSimilarity.clear();
				}
			}


			// Classify train with the transformed data to get error rate.
			return evaluation;

		} catch (Exception e) {
			System.err.println("Exception caught: " + StackTrace.toString(e));
			return null;
		}

	}

	public double[] getErrorPerClass(Evaluation evaluation) {
		double[] error = new double[clsNum];
		for (int i = 0; i < clsNum; i++) {
			error[i] = 1 - evaluation.fMeasure(i);
		}

		return error;
	}

}
