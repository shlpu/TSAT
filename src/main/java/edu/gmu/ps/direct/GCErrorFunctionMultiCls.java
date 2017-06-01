package edu.gmu.ps.direct;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.gmu.connectGI.ConnectGI;
import edu.gmu.connectGI.GrammarInductionMethod;
import edu.gmu.grammar.classification.GCProcessMultiClass;
import edu.gmu.grammar.classification.util.*;
import edu.gmu.grammar.classification.util.TimeSeriesTrain;
import edu.gmu.grammar.patterns.PatternsSimilarity;
import edu.gmu.grammar.patterns.TSPattern;
import edu.gmu.grammar.patterns.TSPatterns;
import edu.gmu.grammar.patterns.PatternsAndTransformedData;
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

public class GCErrorFunctionMultiCls {

	public static final Character DELIMITER = '~';
	final static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	public HashMap<String, TSPatterns> thisRPatterns;

	public TSPattern[] selectedRepresentativePatterns;

	// the default numerosity strategy
	private NumerosityReductionStrategy numerosityReductionStrategy;

	private ArrayList<TimeSeriesTrain> trainData;
	private Map<String, List<TimeSeriesTrain>> trainDataPerClass;

	private GrammarInductionMethod giMethod;
	private int folderNum;
	private double rpFrequencyTPer;
	private int maxRPNum;
	private double overlapTPer;
	private Boolean isCoverageFre;
	PatternsSimilarity pSimilarity;

	private static final Logger consoleLogger;
	private static final Level LOGGING_LEVEL = Level.INFO;
	static {
		consoleLogger = (Logger) LoggerFactory
				.getLogger(GCErrorFunctionMultiCls.class);
		consoleLogger.setLevel(LOGGING_LEVEL);
	}

	/**
	 * Constructor.
	 *
	 */
	public GCErrorFunctionMultiCls(Map<String, List<double[]>> inputTrainData,
								   NumerosityReductionStrategy strategy,
								   GrammarInductionMethod giMethod, int folderNum,
								   double rpFrequencyTPer, int maxRPNum, double overlapTPer,
								   Boolean isCoverageFre, PatternsSimilarity pSimilarity) {
		this.trainData = new ArrayList<TimeSeriesTrain>();
		this.trainDataPerClass = new HashMap<String, List<TimeSeriesTrain>>();
		for (Entry<String, List<double[]>> e : inputTrainData.entrySet()) {
			String label = e.getKey();
			List<double[]> tsesInClass = e.getValue();

			List<TimeSeriesTrain> tses = new ArrayList<TimeSeriesTrain>();

			int idx = 1;
			for (double[] ts : tsesInClass) {
				TimeSeriesTrain tsTrain = new TimeSeriesTrain(label, ts, idx);
				trainData.add(tsTrain);
				tses.add(tsTrain);
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

	public static int[] convertIntegers(List<Integer> integers) {
		int[] ret = new int[integers.size()];
		Iterator<Integer> iterator = integers.iterator();
		for (int i = 0; i < ret.length; i++) {
			ret[i] = iterator.next().intValue();
		}
		return ret;
	}

	// Originally from the DataProcess Class - Copied over for code cleanup
	public static HashMap<String, double[]> concatenateTrainInTrain(
			Map<String, List<TimeSeriesTrain>> trainDataPerClass, HashMap<String, int[]> allStartPositions) {

		HashMap<String, double[]> concatenatedData = new HashMap<String, double[]>();

		List<Entry<String, List<TimeSeriesTrain>>> list = new ArrayList<Entry<String, List<TimeSeriesTrain>>>(
				trainDataPerClass.entrySet());
		Collections.shuffle(list);

		for (Entry<String, List<TimeSeriesTrain>> e : list) {
			String classLabel = e.getKey();
			// if(classLabel.equals("41"))
			// System.out.println();

			// Record the start point of time series in concatenated one.
			ArrayList<Integer> temp = new ArrayList<Integer>();
			// temp.add(0);
			int startPoint = 0;

			int tsNum = e.getValue().size();
			int tsIdx = 1;

			for (TimeSeriesTrain series : e.getValue()) {

				double[] existSeries = concatenatedData.get(classLabel);
				if (null == existSeries) {
					concatenatedData.put(classLabel, series.getValues());
				} else {
					double[] newExistSeries = ArrayUtils.addAll(existSeries, series.getValues());
					concatenatedData.put(classLabel, newExistSeries);

				}
				if (tsIdx < tsNum) {
					startPoint += series.getValues().length;
					temp.add(startPoint);
				}
				tsIdx++;
			}

			// if (temp.size() < 1)
			// temp.add(0);

			int[] tempInt = convertIntegers(temp);
			allStartPositions.put(classLabel, tempInt);

		}

		return concatenatedData;
	}

	/**
	 * Computes the value at point.
	 * 
	 * @param point
	 * @return
	 */
	public ClassificationErrorEachSample valueAtTransformMultiClass(Point point) {

		// point is in fact a aset of parameters - window, paa, and the alphabet
		//
		double[] coords = point.toArray();
		int windowSize = Long.valueOf(Math.round(coords[0])).intValue();
		int paaSize = Long.valueOf(Math.round(coords[1])).intValue();
		int alphabetSize = Long.valueOf(Math.round(coords[2])).intValue();

		// if we stepped above window length with PAA size - for some reason -
		// return the max possible
		// error value
		if (paaSize > windowSize) {
			pSimilarity.clear();
			return null;
		}

		// the whole thing begins here
		//
		try {
			// make a parameters vector
			int[][] params = new int[1][4];
			params[0][0] = windowSize;
			params[0][1] = paaSize;
			params[0][2] = alphabetSize;
			params[0][3] = this.numerosityReductionStrategy.index();
			// consoleLogger.debug("parameters: " + windowSize + ", " + paaSize
			// + ", " + alphabetSize + ", "
			// + this.numerosityReductionStrategy.toString());

			HashMap<String, int[]> allStartPositions = new HashMap<String, int[]>();

			// Concatenate training time series
			HashMap<String, double[]> concatenateData = concatenateTrainInTrain(trainDataPerClass,
							allStartPositions);

			// TODO: write concatenated data.
			// DataProcessor.writeConcatenatedData(concatenateData);

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

//			HashMap<String, TSPatterns> topFrequentPatterns = GCProcessMultiClass
//					.selectTopFrequentPatterns(allPatterns, allStartPositions);
			 HashMap<String, TSPatterns> topFrequentPatterns = allPatterns;
			// allPatterns.clear();
			int clsNum = topFrequentPatterns.size();
			// // Number of incorrectly classified instances per class.
			// int[] missclassifiedSamplesPerClass = new int[clsNum];
			// // Number of correctly classified instances per class.
			// int[] correctNumPerClass = new int[clsNum];
			// // Initialized as zero.
			// Arrays.fill(missclassifiedSamplesPerClass, 0);
			// Arrays.fill(correctNumPerClass, 0);

			GCProcessMultiClass gcp = new GCProcessMultiClass(folderNum);

			// For timing
			// long startTimeRepresentative = System.currentTimeMillis();

			// HashMap<String, TSPatterns> representativePatterns = gcp
			// .selectBestFromRNNTrain(topFrequentPatterns, 3,
			// trainDataPerClass);

			// Transform the original time series into the features space of
			// distance to selected patterns.
			PatternsAndTransformedData pTransformTS = gcp.transformTS(
					topFrequentPatterns, trainDataPerClass, pSimilarity);
			// The transformed time series. The last column is the label of
			// class.
			double[][] transformedTS = pTransformTS.getTransformedTS();
			// Put patterns from each class together into an array.
			TSPattern[] allPatternsTogether = pTransformTS.getAllPatterns();

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

			selectedRepresentativePatterns = selectedPatterns.clone();

			// Classify train with the transformed data to get error rate.
			Evaluation evaluation = gcp.cvEvaluationAllCls(newTransformedTS);
			double allError = evaluation.errorRate();
			double[] error = new double[clsNum];
			for (int i = 0; i < clsNum; i++) {
				error[i] = 1 - evaluation.fMeasure(i);
			}

			if (pSimilarity.getIsTSimilarSetted()) {
			} else {
				if ((allError < 1)) {
					// set pSimilarity

					double sumCandi = 0;
					ArrayList<Double> distCandidates = pSimilarity
							.gettCandidates();
					double numCandi = distCandidates.size();
					if (numCandi > 0) {
						// for (double candi : distCandidates) {
						// sumCandi += candi;
						// }
						// double tSimilar = sumCandi / numCandi;

						Collections.sort(distCandidates);
						// int idx25 = (int) (numCandi * 0.05);
						// double tSimilar = distCandidates.get(idx25);
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

			ClassificationErrorEachSample clssifyError = new ClassificationErrorEachSample(
					allError, error);
			return clssifyError;

		} catch (Exception e) {
			System.err.println("Exception caught: " + StackTrace.toString(e));
			return null;
		}

	}

}
