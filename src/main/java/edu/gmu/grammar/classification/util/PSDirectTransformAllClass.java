package edu.gmu.grammar.classification.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.gmu.connectGI.GrammarInductionMethod;
import edu.gmu.dataprocess.UCRUtils;
import edu.gmu.grammar.classification.GCProcessMultiClass;
import edu.gmu.grammar.patterns.BestSelectedPatterns;
import edu.gmu.grammar.patterns.PatternsSimilarity;
import edu.gmu.ps.direct.GCErrorFunctionMultiCls;
import net.seninp.jmotif.direct.Point;
import net.seninp.jmotif.direct.ValuePointColored;
import net.seninp.jmotif.sax.NumerosityReductionStrategy;
import net.seninp.jmotif.sax.TSProcessor;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class PSDirectTransformAllClass {

	// Default number of iterations for sampling
	public static final int DEFAULT_NUMBER_OF_ITERATIONS = 5;

	// Default strategy for sampling
	public static final String DEFAULT_STRATEGY = "EXACT";

	// the number formatter
	private final DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(
			Locale.US);
	private DecimalFormat fmt = new DecimalFormat("0.00###",
			otherSymbols);

	private PatternsSimilarity pSimilarity;

	// array with all rectangle centerpoints
	private ArrayList<Double[]> centerPoints;

	// array with all rectangle side lengths in each dimension
	private ArrayList<Double[]> lengthsSide;

	// array with distances from center points to the vertices
	private ArrayList<Double> diagonalLength;

	// array vector of all different distances, sorted
	private ArrayList<Double> differentDiagonalLength;

	// array vector of minimum function value for each distance
	private double[] diagonalsMinFunc;

	// array with function values
	private ArrayList<Double> functionValues;

	// array used to track sampled points and function values
	private ArrayList<ValuePointColored> coordinates;

	// array with function values
	// private ArrayList<Double> functionValues;

	private final double precision = 1E-16;
	private int b = 0;
	private double[] resultMinimum;

	private int sampledPoints;
	private int rectangleCounter;
	private int indexPotentialBestRec;
	private double minFunctionValue;
	private double[] minFunctionValuesDouble;

	// init bounds
	//
	private int dimensions = 3;

	private GCErrorFunctionMultiCls function;

	// block - we instantiate the logger
	//
	private final Logger consoleLogger;
	private final Level LOGGING_LEVEL = Level.INFO;

	private int directTime = 0;
	
	private final String COMMA = ", ";
	 {
		consoleLogger = (Logger) LoggerFactory
				.getLogger(PSDirectTransformAllClass.class);
		consoleLogger.setLevel(LOGGING_LEVEL);
	}

	// the global minimum point
	private ValuePointColored minimum = ValuePointColored.at(
			Point.at(0), Double.POSITIVE_INFINITY, false);

	private BestSelectedPatterns[] bestSelectedPatternsAllClass;

	private int[] upperBounds;
	private int[] lowerBounds;

	private String TRAINING_DATA_PATH;
	private String TEST_DATA_PATH;
	private int iterations_num; // 5
	private int FOLDERNUM;
	private double rpFrequencyTPer;
	private int maxRPNum;
	private double overlapTPer;
	private Boolean isCoverageFre;

	private Map<String, List<double[]>> trainData;
	private Map<String, List<double[]>> testData;

	private NumerosityReductionStrategy allStrategy = NumerosityReductionStrategy.EXACT;

	private GrammarInductionMethod giMethod = GrammarInductionMethod.SEQUITUR;


	/**
	 * Loads training data from a path, setting the appropriate fields.
	 *
	 * @param trainingDataFilePath - A file path to RPM formatted (UCR) data.
	 * @throws IOException
	 */
	private void loadTrainingData(String trainingDataFilePath) throws IOException {
		TRAINING_DATA_PATH = trainingDataFilePath;
		trainData = UCRUtils.readUCRData(TRAINING_DATA_PATH);
	}

	/**
	 * Converts a string to a double if it is possible, returning Not a Number (NaN) if it fails.
	 *
	 * @param string - A string that should hold a decimal number only
	 * @return - The value from the string or NaN
	 */
	private Double parseValue(String string) {
		Double res = Double.NaN;
		try {
			Double r = Double.valueOf(string);
			res = r;
		} catch (NumberFormatException e) {
			assert true;
		}
		return res;
	}

	/**
	 * Goes through the given labeled time series data and relabels the data with a increasing numeric value.
	 *
	 * @param res - The data to be relabeled.
	 * @return - The relabeled data.
	 */
	private Map<String, List<double[]>> refineClassLabel(Map<String, List<double[]>> res) {
		Set<String> keys = res.keySet();
		Map<String, List<double[]>> newRes = new HashMap<String, List<double[]>>();

		HashMap<String, String> replaceMap = new HashMap<String, String>();

		int count = 1;
		for (String k : keys) {
			String newLabel = String.valueOf(count);
			replaceMap.put(k, newLabel);
			count++;
		}
		for (Map.Entry<String, List<double[]>> e : res.entrySet()) {
			String label = (String) e.getKey();

			String newLabel = (String) replaceMap.get(label);
			newRes.put(newLabel, (List<double[]>) e.getValue());
		}
		return newRes;
	}

	/**
	 * Converts the time series data from the data structures used by
	 * GrammarViz (2D double array plus an Array of strings) to that used by RPM (A HashMap with labels as keys and
	 * a List of doubles arrays of time series).
	 *
	 * @param data - 2D double array of time series data
	 * @param labels - Array of labels, there must be a label for every array of times series data
	 * @return - A Map from the labels to a List of double arrays
	 */
	public Map<String, List<double[]>> convertGrammarVizData(double[][] data, String[] labels) {
		if(data.length != labels.length) {
			this.consoleLogger.error("The number of classes (" +
					data.length + ") and the number of labels (" +
					labels.length + ") did not match");
			return null;
		}

		Map<String, List<double[]>> res = new HashMap<String, List<double[]>>();

		for(int i = 0; i < labels.length; i++) {
			Double num = parseValue(labels[i]);
			String seriesType = labels[i];
			if (!(Double.isNaN(num))) {
				seriesType = String.valueOf(num.intValue());
			}

			TSProcessor tsp = new TSProcessor();
			double[] series = data[i];//tsp.znorm(data[i], 0.05);

			if (!res.containsKey(seriesType)) {
				res.put(seriesType, new ArrayList<double[]>());
			}

			res.get(seriesType).add(series);
		}

		//res = refineClassLabel(res);

		return res;
	}

	/**
	 * This is the internal function for training RPM using GrammarViz, it is used by public facing functions only.
	 *
	 * @param strategy - The strategy to be used for sampling.
	 * @param lowerWindow - The lower bounds for the Window Size.
	 * @param upperWindow - The upper bounds for the Window Size.
	 * @param lowerPaa - The lower bounds for the PAA Size.
	 * @param upperPaa - The upper bounds for the PAA Size.
	 * @param lowerAlphabet - The lower bounds for the Alphabet.
	 * @param upperAlphabet - The upper bounds for the Alphabet.
	 * @param iterations - The maximum number of times to train on the data before returning
	 *                      (Will end before if threshold is met).
	 * @return An object that represent the trained model and associating metadata.
	 */
	private RPMTrainedData RPMTrainPrivate(String strategy,
												  int lowerWindow, int upperWindow,
												  int lowerPaa, int upperPaa,
												  int lowerAlphabet, int upperAlphabet,
												  int iterations) {
		// Create RPM Trained Model representative object
		RPMTrainedData rpmTrainedOutput = new RPMTrainedData();
		// Set model's training data path record
		rpmTrainedOutput.training_data_path = TRAINING_DATA_PATH;
		// Set model's training data record
		rpmTrainedOutput.trainData = trainData;
		// Set model's sampling strategy record
		rpmTrainedOutput.allStrategy = strategy;
		// Set training phase's Numerosity Reduction Strategy
		allStrategy = NumerosityReductionStrategy.valueOf(strategy);

		// Set the window size, PAA size, alphabet size lower and upper bounds
		rpmTrainedOutput.lowerBounds = lowerBounds = new int[] {lowerWindow, lowerPaa, lowerAlphabet};
		rpmTrainedOutput.upperBounds = upperBounds = new int[] {upperWindow, upperPaa, upperAlphabet};

		// Setup configuration defaults
		rpmTrainedOutput.folderNum = FOLDERNUM = 5;
		rpmTrainedOutput.rpFrequencyTPer = rpFrequencyTPer = 0.2;
		rpmTrainedOutput.maxRPNum = maxRPNum = 50;
		rpmTrainedOutput.overlapTPer = overlapTPer = 0.5;
		rpmTrainedOutput.isCoverageFre = isCoverageFre = true;
		rpmTrainedOutput.pSimilarity = 0.02;

		// Configure training phase Pattern Similarity function
		pSimilarity = new PatternsSimilarity(0.02);

		// Set the maximum number of iterations
		rpmTrainedOutput.iterations_num = iterations_num = iterations; // Default 5

		consoleLogger.info("running sampling for " + giMethod
			+ " with " + allStrategy.toString() + " strategy...");

		// Start training, storing the results
		rpmTrainedOutput.bestSelectedPatternsAllClass = sample(allStrategy);

		// Pull and store the best trained Window Size, PAA Size, and Alphabet
		int[] bestParams = rpmTrainedOutput.bestSelectedPatternsAllClass[0].getBestParams();
		rpmTrainedOutput.windowSize = bestParams[0];
		rpmTrainedOutput.paa = bestParams[1];
		rpmTrainedOutput.alphabet = bestParams[2];

		return rpmTrainedOutput;
	}

	/**
	 * Trains the RPM model with all options set to default.
	 *
	 * @param trainingDataFilePath - Path to the file containing the training data.
	 * @param data - Times series data in a 2D double array with each entry being an array of time stamps.
	 * @param labels - The labels that are associated with the time series in data (data[0] is label[0]).
	 * @return - An object that represent the trained model and associating metadata.
	 * @throws IOException -
	 */
	public RPMTrainedData RPMTrain(String trainingDataFilePath,
								   double[][] data, String[] labels) throws IOException {
		// Call main RPM train function
		return this.RPMTrain(trainingDataFilePath,
				data, labels,
				DEFAULT_STRATEGY);
	}

	/**
	 * Trains the RPM model with the selected strategy and all other options set to default.
	 *
	 * @param trainingDataFilePath - Path to the file containing the training data.
	 * @param data - Times series data in a 2D double array with each entry being an array of time stamps.
	 * @param labels - The labels that are associated with the time series in data (data[0] is label[0]).
	 * @param strategy - The strategy to be used for sampling.
	 * @return - An object that represent the trained model and associating metadata.
	 * @throws IOException
	 */
	public RPMTrainedData RPMTrain(String trainingDataFilePath,
								   double[][] data, String[] labels,
								   String strategy) throws IOException {
		// Call main RPM train function
		return this.RPMTrain(trainingDataFilePath, data, labels, strategy,
				DEFAULT_NUMBER_OF_ITERATIONS);
	}

	/**
	 * Trains the RPM model with the selected strategy, number of iterations and all other options set to default.
	 *
	 * @param trainingDataFilePath - Path to the file containing the training data.
	 * @param data - Times series data in a 2D double array with each entry being an array of time stamps.
	 * @param labels - The labels that are associated with the time series in data (data[0] is label[0]).
	 * @param strategy - The strategy to be used for sampling.
	 * @param iterations - The maximum number of times to train on the data before returning
	 *                      (Will end before if threshold is met).
	 * @return - An object that represent the trained model and associating metadata.
	 * @throws IOException
	 */
	public RPMTrainedData RPMTrain(String trainingDataFilePath,
								   double[][] data, String[] labels,
								   String strategy, int iterations) throws IOException {

		// Load data path and convert data to GrammarViz Supported format
		this.TRAINING_DATA_PATH = trainingDataFilePath;
		this.trainData = this.convertGrammarVizData(data, labels);

		// lets check the trainData (this looks right)
//		for (Map.Entry<String, List<double[]>> e : trainData.entrySet()) {
//			System.err.println("label = " + e.getKey() + " number of time series = " + e.getValue().size() + " len of first time series = " + e.getValue().get(0).length);
//		}



		// lower bound.
		double lper = 0.1;
		// upper bound.
		double uper = 0.9;

		// Upper and lower bound of sliding window size.
		int tsLen = trainData.entrySet().iterator().next().getValue().get(0).length;
		int lWLen = (int) (tsLen * lper);
		int lb = lWLen > 1 ? lWLen : 1;
		int uWLen = (int) (tsLen * uper);
		int ub = uWLen > 1 ? uWLen : 1;

		// Call main RPM train function
		return this.RPMTrainPrivate(strategy,
				// Lower & Upper Window Default
				lb, ub,
				// Lower & Upper PAA Default
				2, 20,
				// Lower & Upper Alphabet Default
				2, 20,
				iterations);
	}

	/**
	 * * This is the internal function for training RPM using GrammarViz, it is used by public facing functions only.
	 *
	 * @param trainingDataFilePath - Path to the file containing the training data.
	 * @param data - Times series data in a 2D double array with each entry being an array of time stamps.
	 * @param labels - The labels that are associated with the time series in data (data[0] is label[0]).
	 * @param strategy - The strategy to be used for sampling.
	 * @param lowerWindow - The lower bounds for the Window Size.
	 * @param upperWindow - The upper bounds for the Window Size.
	 * @param lowerPaa - The lower bounds for the PAA Size.
	 * @param upperPaa - The upper bounds for the PAA Size.
	 * @param lowerAlphabet - The lower bounds for the Alphabet.
	 * @param upperAlphabet - The upper bounds for the Alphabet.
	 * @param iterations - The maximum number of times to train on the data before returning
	 *                      (Will end before if threshold is met).
	 * @return An object that represent the trained model and associating metadata.
	 * @throws IOException
	 */
	public RPMTrainedData RPMTrain(String trainingDataFilePath,
								   double[][] data, String[] labels,
								   String strategy,
								   int lowerWindow, int upperWindow,
								   int lowerPaa, int upperPaa,
								   int lowerAlphabet, int upperAlphabet,
								   int iterations) throws IOException {

		// Load data path and convert data to GrammarViz Supported format
		this.TRAINING_DATA_PATH = trainingDataFilePath;
		this.trainData = this.convertGrammarVizData(data, labels);

		// Call main RPM train function
		return this.RPMTrainPrivate(strategy,
				lowerWindow, upperWindow,
				lowerPaa, upperPaa,
				lowerAlphabet, upperAlphabet,
				iterations);

	}

	/**
	 * Loads a trained RPM model for use with testing, setting all parameters to match that in the model.
	 * @param trainedData - The object that represents the trained RPM model.
	 */
	public void loadRPMTrain(RPMTrainedData trainedData) {
		// Load data and data path
		TRAINING_DATA_PATH = trainedData.training_data_path;
		trainData = trainedData.trainData;

		// Load Numerosity Reduction Strategy
		allStrategy = NumerosityReductionStrategy.valueOf(trainedData.allStrategy);

		// Set window size, PAA size, alphabet size lower and upper bounds.
		lowerBounds = trainedData.lowerBounds;
		upperBounds = trainedData.upperBounds;

		// Set all RPM parameters
		FOLDERNUM = trainedData.folderNum;
		rpFrequencyTPer = trainedData.rpFrequencyTPer;
		maxRPNum = trainedData.maxRPNum;
		overlapTPer = trainedData.overlapTPer;
		isCoverageFre = trainedData.isCoverageFre;
		pSimilarity = new PatternsSimilarity(trainedData.pSimilarity);

		// Set the maximum number of iterations
		iterations_num = trainedData.iterations_num; // Default 5

		consoleLogger.info("loading sample for " + giMethod
				+ " with " + allStrategy.toString() + " strategy...");

		// The core of the model gets loaded here
		bestSelectedPatternsAllClass = trainedData.bestSelectedPatternsAllClass;
	}

	/**
	 * Tests a sample against a trained model to verify the models accuracy.
	 *
	 * @param testingDataFilePath - Path to the file containing the test data.
	 * @param data - Times series test data in a 2D double array with each entry being an array of time stamps.
	 * @param labels - The labels that are associated with the time series in data (data[0] is label[0]).
	 * @return The object that stores results from the testing phase, including all statistics.
	 * @throws IOException
	 */
	public ClassificationResults RPMTestData(String testingDataFilePath,
											 double[][] data, String[] labels) throws IOException {
		// Create results object to store the statistics
		ClassificationResults results = new ClassificationResults();
		// Load test data path
		results.testDataPath = TEST_DATA_PATH = testingDataFilePath;
		// Convert test data to GrammarViz compatible format (Used for information display)
		results.testData = this.testData = convertGrammarVizData(data, labels);

		// Test the model
		classifyWithTransformedDataAllCls(bestSelectedPatternsAllClass, results);

		return results;
	}

	/**
	 *
	 * @param bestSelectedPatterns - An array of time searies patterns that are an output of RPM.
	 * @param results - An object that stores the results of the evaluation phase during the testing phase
	 * @throws IndexOutOfBoundsException
	 */
	private void classifyWithTransformedDataAllCls(BestSelectedPatterns[] bestSelectedPatterns,
												   ClassificationResults results) throws IndexOutOfBoundsException {
		// Object that handles the processing
		GCProcessMultiClass gcp = new GCProcessMultiClass(FOLDERNUM);
		gcp.doClassifyTransformedMultiCls(bestSelectedPatterns,
				trainData, testData, giMethod, results);
	}

	/**
	 * This function makes the best selection of the candidate patterns using the selected indices.
	 */
	private void update() {
		resultMinimum = minimum(functionValues);
		// getting minimum and giving it at last points
		minFunctionValue = resultMinimum[0];
		minimum.setBest(false);
		minimum = ValuePointColored.at(Point.at(0), Double.POSITIVE_INFINITY,
				false);
		int i = 0;
		for (ValuePointColored valuePoint : coordinates) {
			if (valuePoint.getValue() < minimum.getValue()) {
				b = i;
				minimum = valuePoint;
			}
			i++;
		}
		minimum.setBest(true);
		//
		// coordinates.remove(b);
		// coordinates.add(minimum);
		coordinates.set(b, minimum);
		double epsilon = 1E-4;
		double e = Math.max(epsilon * Math.abs(minFunctionValue), 1E-8);
		double[] temporaryArray = new double[functionValues.size()];
		for (int i2 = 0; i2 < functionValues.size(); i2++) {
			temporaryArray[i2] = (functionValues.get(i2) - minFunctionValue + e)
					/ diagonalLength.get(i2);
		}
		indexPotentialBestRec = (int) minimum(temporaryArray)[1];

		differentDiagonalLength = diagonalLength;
		int i1 = 0;
		while (true) {
			double diagonalTmp = differentDiagonalLength.get(i1);
			Integer[] indx = findNonMatches(differentDiagonalLength,
					diagonalTmp);
			ArrayList<Double> diagonalCopy = differentDiagonalLength;
			differentDiagonalLength = new ArrayList<Double>();
			differentDiagonalLength.add(diagonalTmp);

			for (int i2 = 1; i2 < indx.length + 1; i2++) {
				differentDiagonalLength.add(diagonalCopy.get(indx[i2 - 1]));
			}
			if (i1 + 1 == differentDiagonalLength.size()) {
				break;
			} else {
				i1++;
			}
		}
		Collections.sort(differentDiagonalLength);
		diagonalsMinFunc = new double[differentDiagonalLength.size()];
		for (i1 = 0; i1 < differentDiagonalLength.size(); i1++) {
			Integer[] indx1 = findMatches(diagonalLength,
					differentDiagonalLength.get(i1));
			ArrayList<Double> fTmp = new ArrayList<Double>();
			for (int i2 = 0; i2 < indx1.length; i2++) {
				fTmp.add(functionValues.get(indx1[i2]));
			}
			diagonalsMinFunc[i1] = minimum(fTmp)[0];
		}
	}

	/**
	 * This is the primary function that runs when RPM is being trained. It contains most of the RPM algorithm.
	 * @param strategy the numerosity reduction strategy.
	 * @return the best representative patterns.
	 */
	private BestSelectedPatterns[] sample(
            NumerosityReductionStrategy strategy) {

		// Initializing global varibales.
		function = new GCErrorFunctionMultiCls(trainData, strategy, giMethod,
				FOLDERNUM, rpFrequencyTPer, maxRPNum, overlapTPer,
				isCoverageFre, pSimilarity);
		centerPoints = new ArrayList<Double[]>();
		lengthsSide = new ArrayList<Double[]>();
		diagonalLength = new ArrayList<Double>();
		differentDiagonalLength = new ArrayList<Double>();
		diagonalsMinFunc = new double[1];
		functionValues = new ArrayList<Double>();
		coordinates = new ArrayList<ValuePointColored>();
		minFunctionValuesDouble = null;

		sampledPoints = 0;
		rectangleCounter = 1;
		indexPotentialBestRec = 0;
		// The error of all class
		minFunctionValue = 0;

		Double[] scaledCenter = new Double[dimensions];
		double[] realCenter = new double[dimensions];
		Double[] lTmp = new Double[dimensions];
		Double dTmp = 0.0;
		Double[] cooTmp = new Double[dimensions];

		for (int i = 0; i < dimensions; i++) {
			scaledCenter[i] = 0.5;
			lTmp[i] = 0.5;
			dTmp = dTmp + scaledCenter[i] * scaledCenter[i];
			realCenter[i] = lowerBounds[i] + scaledCenter[i]
					* (upperBounds[i] - lowerBounds[i]);
		}
		centerPoints.add(scaledCenter);
		lengthsSide.add(lTmp);
		dTmp = Math.sqrt(dTmp);
		diagonalLength.add(dTmp);
		Point startingPoint = Point.at(realCenter);

		int clsNum = trainData.keySet().size();

		ClassificationErrorEachSample classifyError = function
				.valueAtTransformMultiClass(startingPoint);
		directTime++;

		if (minFunctionValuesDouble == null) {
			minFunctionValuesDouble = new double[clsNum];
			Arrays.fill(minFunctionValuesDouble, 10);
			function.thisRPatterns = null;
			minFunctionValue = 1.0d;
		} else {
			minFunctionValue = classifyError.getAllError();
			minFunctionValuesDouble = classifyError.getErrorPerClass();
		}

		sampledPoints = sampledPoints + 1;
		for (int i1 = 0; i1 < dimensions; i1++) {
			cooTmp[i1] = realCenter[i1];
		}

		// bestKResults = new TopKBestPatterns[clsNum];
		int count = 0;
		count = count + 1;

		double[] startingCoords = startingPoint.toArray();
		int windowSize = Long.valueOf(Math.round(startingCoords[0])).intValue();
		int paaSize = Long.valueOf(Math.round(startingCoords[1])).intValue();
		int alphabetSize = Long.valueOf(Math.round(startingCoords[2]))
				.intValue();
		int[] startParams = { windowSize, paaSize, alphabetSize };

		bestSelectedPatternsAllClass = new BestSelectedPatterns[clsNum];

		for (int j = 0; j < clsNum; j++) {
			BestSelectedPatterns bsp = new BestSelectedPatterns(
					minFunctionValuesDouble[j], startParams,
					function.selectedRepresentativePatterns);
			bestSelectedPatternsAllClass[j] = bsp;

			consoleLogger.debug("iteration: " + count + ", minimal value "
					+ minFunctionValuesDouble[j] + " for class " + (j + 1)
					+ " at " + lowerBounds[0] + ", " + lowerBounds[1] + ", "
					+ lowerBounds[2]);
//			System.err.println("iteration: " + count + ", minimal value "
//					+ minFunctionValuesDouble[j] + " for class " + (j + 1)
//					+ " at " + lowerBounds[0] + ", " + lowerBounds[1] + ", "
//					+ lowerBounds[2]);
		}

		minimum = ValuePointColored.at(startingPoint, minFunctionValue, true);
		coordinates.add(minimum);
		diagonalsMinFunc[0] = minFunctionValue;
		functionValues.add(minFunctionValue);
		differentDiagonalLength = diagonalLength;

		ArrayList<Integer> potentiallyOptimalRectangles = null;

		// optimization loop
		// SAX Parameter Selection
		for (int ctr = 0; ctr < iterations_num; ctr++) {
			// The minimal error and its index.
			resultMinimum = minimum(functionValues);
			double[] params = coordinates.get((int) resultMinimum[1])
					.getPoint().toArray();
			consoleLogger.info("iteration: " + ctr + ", minimal value "
					+ resultMinimum[0] + " at " + params[0] + ", " + params[1]
					+ ", " + params[2]);
			// System.out.println(resultMinimum[0] + ","+params[0] + "," +
			// params[1] + ", " + params[2]);
			potentiallyOptimalRectangles = identifyPotentiallyRec();
			// For each potentially optimal rectangle
			for (int jj = 0; jj < potentiallyOptimalRectangles.size(); jj++) {
				int j = potentiallyOptimalRectangles.get(jj);
				samplingPotentialRec(j);
			}
			update();
		}

		return bestSelectedPatternsAllClass;

	}

	/**
	 * Updates the best representative patterns during the training phase of RPM.
	 * @param errorValue the current error values.
	 * @param point the point in the data.
	 */
	private void updateBest(double[] errorValue, Point point) {

		double[] coords = point.toArray();
		int windowSize = Long.valueOf(Math.round(coords[0])).intValue();
		int paaSize = Long.valueOf(Math.round(coords[1])).intValue();
		int alphabetSize = Long.valueOf(Math.round(coords[2])).intValue();

		for (int i = 0; i < errorValue.length; i++) {
			// String label = String.valueOf(i + 1);

			if ((errorValue[i] - minFunctionValuesDouble[i]) < 0) {
				minFunctionValuesDouble[i] = errorValue[i];
				int[] params = { windowSize, paaSize, alphabetSize };

				BestSelectedPatterns bsp = new BestSelectedPatterns(
						minFunctionValuesDouble[i], params,
						function.selectedRepresentativePatterns);
				bestSelectedPatternsAllClass[i] = bsp;

			}
		}
	}

	/**
	 * Determine where to sample within rectangle j and how to divide the
	 * rectangle into subrectangles. Update minFunctionValue and set
	 * m=m+delta_m, where delta_m is the number of new points sampled.
	 * 
	 * @param j
	 */
	private void samplingPotentialRec(int j) {

		double max_L = lengthsSide.get(j)[0], delta;
		Integer[] maxSideLengths;

		// get the longest side
		//
		for (int i1 = 0; i1 < lengthsSide.get(j).length; i1++) {
			max_L = Math.max(max_L, lengthsSide.get(j)[i1]);
		}

		// Identify the array maxSideLengths of dimensions with the maximum side
		// length.
		//
		maxSideLengths = findMatches(lengthsSide.get(j), max_L);
		delta = 2 * max_L / 3;
		double[] w = new double[0];
		double i1;
		double[] e_i;

		// Sample the function at the points c +- delta*e_i for all ii in
		// maxSideLengths.
		for (int ii = 0; ii < maxSideLengths.length; ii++) {
			Double[] c_m1 = new Double[dimensions];
			double[] x_m1 = new double[dimensions];
			Double[] c_m2 = new Double[dimensions];
			double[] x_m2 = new double[dimensions];
			i1 = maxSideLengths[ii];
			e_i = new double[dimensions];
			e_i[(int) i1] = 1;

			// Center point for a new rectangle
			//
			for (int i2 = 0; i2 < centerPoints.get(j).length; i2++) {
				c_m1[i2] = centerPoints.get(j)[i2] + delta * e_i[i2];
			}
			// Transform c_m1 to original search space
			for (int i2 = 0; i2 < c_m1.length; i2++) {
				x_m1[i2] = lowerBounds[i2] + c_m1[i2]
						* (upperBounds[i2] - lowerBounds[i2]);
			}
			// Function value at x_m1
			Point pointToSample1 = Point.at(x_m1);

			Double f_m1;
			ClassificationErrorEachSample classifyError1 = function
					.valueAtTransformMultiClass(pointToSample1);
			directTime++;
			if (classifyError1 == null) {
				f_m1 = 1.0d;
			} else {
				f_m1 = classifyError1.getAllError();
				double[] f_m1_each_class = classifyError1.getErrorPerClass();
				updateBest(f_m1_each_class, pointToSample1);
			}

			//
			// Double f_m1 = function.valueAt(pointToSample1);
			consoleLogger.debug("@" + f_m1 + "\t"
					+ pointToSample1.toLogString());

			// add to all points
			coordinates.add(ValuePointColored.at(pointToSample1, f_m1, false));
			sampledPoints = sampledPoints + 1;

			// Center point for a new rectangle
			//
			for (int i2 = 0; i2 < centerPoints.get(j).length; i2++) {
				c_m2[i2] = centerPoints.get(j)[i2] - delta * e_i[i2];
			}
			// Transform c_m2 to original search space
			for (int i2 = 0; i2 < c_m2.length; i2++) {
				x_m2[i2] = lowerBounds[i2] + c_m2[i2]
						* (upperBounds[i2] - lowerBounds[i2]);
			}
			// Function value at x_m2
			Point pointToSample2 = Point.at(x_m2);

			Double f_m2;
			ClassificationErrorEachSample classifyError2 = function
					.valueAtTransformMultiClass(pointToSample2);
			directTime++;
			if (classifyError2 == null) {
				f_m2 = 1.0d;
			} else {
				f_m2 = classifyError2.getAllError();
				double[] f_m2_each_class = classifyError2.getErrorPerClass();
				updateBest(f_m2_each_class, pointToSample2);
			}
			//
			consoleLogger.debug("@" + f_m2 + "\t"
					+ pointToSample2.toLogString());

			// add to all points
			coordinates.add(ValuePointColored.at(pointToSample2, f_m2, false));
			sampledPoints = sampledPoints + 1;

			double[] w_pom;
			w_pom = w;
			w = new double[ii + 1];
			System.arraycopy(w_pom, 0, w, 0, w_pom.length);
			w[ii] = Math.min(f_m2, f_m1);

			centerPoints.add(c_m1);
			centerPoints.add(c_m2);
			functionValues.add(f_m1);
			functionValues.add(f_m2);

			// System.out.println(Arrays.toString(x_m1) + ", " + f_m1);
			// System.out.println(Arrays.toString(x_m2) + ", " + f_m2);
		}

		devideRec(w, maxSideLengths, delta, j);

	}

	/**
	 * Divide the rectangle containing centerPoints.get(j) into thirds along the
	 * dimension in maxSideLengths, starting with the dimension with the lowest
	 * value of w[ii]
	 * 
	 * @param w
	 * @param maxSideLengths
	 * @param delta
	 * @param j
	 */
	private void devideRec(double[] w, Integer[] maxSideLengths,
			double delta, int j) {

		double[][] ab = sort(w);

		for (int ii = 0; ii < maxSideLengths.length; ii++) {
			int i1 = maxSideLengths[(int) ab[1][ii]];
			int index1 = rectangleCounter + 2 * (int) ab[1][ii]; // Index for
																	// new
																	// rectangle
			int index2 = rectangleCounter + 2 * (int) ab[1][ii] + 1; // Index
																		// for
																		// new
																		// rectangle
			lengthsSide.get(j)[i1] = delta / 2;
			int index = 0;
			if (index2 + 1 > index1 + 1) {
				index = index2 + 1;
			} else {
				index = index1 + 1;
			}

			Double[] lTmp = new Double[dimensions];
			Double[] lTmp2 = new Double[dimensions];
			for (int i2 = 0; i2 < lengthsSide.get(0).length; i2++) {
				lTmp[i2] = lengthsSide.get(j)[i2];
				lTmp2[i2] = lengthsSide.get(j)[i2];
			}
			if (index == lengthsSide.size() + 2) {
				lengthsSide.add(lTmp);
				lengthsSide.add(lTmp2);
			} else {
				Double[] lTmp3;
				int lengthsSize = lengthsSide.size();
				for (int i2 = 0; i2 < index - lengthsSize; i2++) {
					lTmp3 = new Double[dimensions];
					lengthsSide.add(lTmp3);
				}
				lengthsSide.set(index1, lTmp);
				lengthsSide.set(index2, lTmp2);
			}

			diagonalLength.set(j, 0.0);
			Double dTmp;
			for (int i2 = 0; i2 < lengthsSide.get(j).length; i2++) {
				dTmp = diagonalLength.get(j) + lengthsSide.get(j)[i2]
						* lengthsSide.get(j)[i2];
				diagonalLength.set(j, dTmp);
			}
			diagonalLength.set(j, Math.sqrt(diagonalLength.get(j)));
			dTmp = diagonalLength.get(j);
			Double d_kop2 = diagonalLength.get(j);
			if (index == diagonalLength.size() + 2) {
				diagonalLength.add(dTmp);
				diagonalLength.add(d_kop2);
			} else {
				Double dTmp3;
				int size = diagonalLength.size();
				for (int i2 = 0; i2 < index - size; i2++) {
					dTmp3 = 0.0;
					diagonalLength.add(dTmp3);
				}
				diagonalLength.set(index1, diagonalLength.get(j));
				diagonalLength.set(index2, diagonalLength.get(j));
			}
		}
		rectangleCounter = rectangleCounter + 2 * maxSideLengths.length;
	}

	/**
	 * Identify the set of all potentially optimal rectangles.
	 *
	 * @return
	 */
	private ArrayList<Integer> identifyPotentiallyRec() {

		double localPrecision = 1E-12;

		// find rectangles with the same diagonal
		//
		Integer[] sameDiagonalIdxs = findMatches(differentDiagonalLength,
				diagonalLength.get(indexPotentialBestRec));

		ArrayList<Integer> s_1 = new ArrayList<Integer>();
		for (int i = sameDiagonalIdxs[0]; i < differentDiagonalLength.size(); i++) {
			Integer[] indx3 = findMatches(functionValues, diagonalsMinFunc[i]);
			Integer[] indx4 = findMatches(diagonalLength,
					differentDiagonalLength.get(i));
			Integer[] idx2 = findArrayIntersection(indx3, indx4);
			s_1.addAll(Arrays.asList(idx2));
		}

		// s_1 now includes all rectangles i, with diagonals[i] >=
		// diagonals(indexPotentialBestRec)
		//
		ArrayList<Integer> s_2 = new ArrayList<Integer>();
		ArrayList<Integer> s_3 = new ArrayList<Integer>();
		if (differentDiagonalLength.size() - sameDiagonalIdxs[0] > 2) {

			double a1 = diagonalLength.get(indexPotentialBestRec), a2 = differentDiagonalLength
					.get(differentDiagonalLength.size() - 1), b1 = functionValues
					.get(indexPotentialBestRec), b2 = diagonalsMinFunc[differentDiagonalLength
					.size() - 1];

			// The line is defined by: y = slope*x + const
			double slope = (b2 - b1) / (a2 - a1);
			double consta = b1 - slope * a1;

			for (int i1 = 0; i1 < s_1.size(); i1++) {
				int j = s_1.get(i1).intValue();
				if (functionValues.get(j) <= slope * diagonalLength.get(j)
						+ consta + localPrecision) {
					s_2.add(j);
				}
			}

			if (0 == s_2.size()) {
				return s_1;
			}

			// s_2 now contains all points in S_1 which lie on or below the line
			// Find the points on the convex hull defined by the points in s_2
			double[] xx = new double[s_2.size()];
			double[] yy = new double[s_2.size()];
			for (int i1 = 0; i1 < xx.length; i1++) {
				xx[i1] = diagonalLength.get(s_2.get(i1).intValue());
				yy[i1] = functionValues.get(s_2.get(i1).intValue());
			}
			double[] h = conhull(xx, yy);
			for (int i1 = 0; i1 < h.length; i1++) {
				s_3.add(s_2.get((int) h[i1]));
			}
		} else {
			s_3 = s_1;
		}
		return s_3;
	}

	/**
	 * Finds all points on the convex hull, even redundant ones.
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	private double[] conhull(double[] x, double[] y) {
		// System.out.println(Arrays.toString(x) + " : " + Arrays.toString(y));
		int m = x.length;
		double[] h;
		int start = 0, flag = 0, v, w, a, b, c, leftturn, j, k;
		double determinant;
		if (x.length != y.length) {
			System.out.println("Input dimension must agree");
			return null;
		}
		if (m == 2) {
			h = new double[2];
			h[0] = 0;
			h[1] = 1;
			return h;
		}
		if (m == 1) {
			h = new double[1];
			h[0] = 0;
			return h;
		}
		v = start;
		w = x.length - 1;
		h = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			h[i] = i + 1;
		}
		while ((next(v, m) != 0) || (flag == 0)) {
			if (next(v, m) == w) {
				flag = 1;
			}
			// getting three points
			a = v;
			b = next(v, m);
			c = next(next(v, m), m);
			determinant = (x[a] * y[b] * 1) + (x[b] * y[c] * 1)
					+ (x[c] * y[a] * 1) - (1 * y[b] * x[c]) - (1 * y[c] * x[a])
					- (1 * y[a] * x[b]);

			if (determinant >= 0) {
				leftturn = 1;
			} else {
				leftturn = 0;
			}
			if (leftturn == 1) {
				v = next(v, m);
			} else {
				j = next(v, m);
				k = 0;
				double[] x1 = new double[x.length - 1];
				for (int i = 0; i < x1.length; i++) {
					if (j == i) {

						k++;
					}
					x1[i] = x[k];
					k++;
				}
				x = x1;
				k = 0;
				x1 = new double[y.length - 1];
				for (int i = 0; i < x1.length; i++) {
					if (j == i) {

						k++;
					}
					x1[i] = y[k];
					k++;
				}
				y = x1;
				k = 0;
				x1 = new double[h.length - 1];
				for (int i = 0; i < x1.length; i++) {
					if (j == i) {

						k++;
					}
					x1[i] = h[k];
					k++;
				}
				h = x1;
				m = m - 1;
				w = w - 1;
				v = pred(v, m);
			}
		}
		for (int i = 0; i < h.length; i++) {
			h[i] = h[i] - 1;
		}
		return h;
	}

	/**
	 * returns next point if the last then the first
	 * 
	 * @param v
	 * @param m
	 * @return
	 */
	private int next(int v, int m) {
		if ((v + 1) == m) {
			return 0;
		} else {
			if ((v + 1) < m) {
				return (v + 1);
			} else {
				return -1;
			}
		}
	}

	/**
	 * M is the size, v is the index, returns the previous index value.
	 *
	 * @param idx
	 * @param size
	 * @return
	 */
	private int pred(int idx, int size) {
		if ((idx + 1) == 1) {
			return size - 1;
		} else {
			if ((idx + 1) > 1) {
				return (idx - 1);
			} else {
				return -1;
			}
		}
	}

	/**
	 * returns sorted array and the original indicies
	 * 
	 * @param array
	 * @return
	 */
	private double[][] sort(double[] array) {
		double[][] arr1 = new double[3][array.length];
		double[][] arr2 = new double[2][array.length];
		System.arraycopy(array, 0, arr1[0], 0, array.length);
		Arrays.sort(array);
		for (int i = 0; i < array.length; i++) {
			for (int i1 = 0; i1 < array.length; i1++) {
				if (array[i] == arr1[0][i1] && arr1[2][i1] != 1) {
					arr1[2][i1] = 1;
					arr1[1][i] = i1;
					break;
				}
			}
		}
		arr2[0] = array;
		arr2[1] = arr1[1];
		return arr2;
	}

	/**
	 * Finds an index and a minimal value of an array.
	 *
	 * @param array
	 * @return
	 */
	private double[] minimum(double[] array) {
		Double min = array[0];
		double[] res = { min, 0.0 };
		for (int i = 0; i < array.length; i++) {
			if (min > array[i]) {
				min = array[i];
				res[0] = min;
				res[1] = i;
			}
		}
		return res;
	}

	/**
	 * Finds an index and a minimal value of an array.
	 *
	 * @param array
	 * @return
	 */
	private double[] minimum(ArrayList<Double> array) {
		Double min = array.get(0);
		double[] res = { min, 0.0 };
		for (int i = 0; i < array.size(); i++) {
			if (min > array.get(i)) {
				min = array.get(i);
				res[0] = min;
				res[1] = i;
			}
		}
		return res;
	}

	/**
	 * Finds matches.
	 *
	 * @param array
	 * @param value
	 * @return
	 */
	private Integer[] findMatches(Double[] array, double value) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		for (int i = 0; i < array.length; i++) {
			if (Math.abs(array[i] - value) <= precision) {
				res.add(i);
			}
		}
		return res.toArray(new Integer[res.size()]);
	}

	/**
	 * Finds matches.
	 *
	 * @param array
	 * @param value
	 * @return
	 */
	private Integer[] findMatches(ArrayList<Double> array, double value) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		for (int i = 0; i < array.size(); i++) {
			if (Math.abs(array.get(i) - value) <= precision) {
				res.add(i);
			}
		}
		return res.toArray(new Integer[res.size()]);
	}

	/**
	 * Finds array elements that are not equal to the value up to threshold.
	 *
	 * @param array
	 * @param value
	 * @return
	 */
	private Integer[] findNonMatches(ArrayList<Double> array,
			double value) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		for (int i = 0; i < array.size(); i++) {
			if (Math.abs(array.get(i) - value) > precision) {
				res.add(i);
			}
		}
		return res.toArray(new Integer[res.size()]);
	}

	/**
	 * Returns arrays intersection.
	 *
	 * @param arr1
	 * @param arr2
	 * @return
	 */
	private Integer[] findArrayIntersection(Integer[] arr1,
			Integer[] arr2) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		for (int i1 = 0; i1 < arr1.length; i1++) {
			for (int i2 = 0; i2 < arr2.length; i2++) {
				if (arr1[i1] == arr2[i2]) {
					res.add(arr2[i2]);
				}
			}
		}
		return res.toArray(new Integer[res.size()]);
	}

}
