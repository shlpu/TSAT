package edu.gmu.grammar.classification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.gmu.connectGI.GrammarInductionMethod;
import edu.gmu.grammar.classification.util.*;
import edu.gmu.grammar.classification.util.TimeSeriesTrain;
import edu.gmu.grammar.patterns.BestSelectedPatterns;
import edu.gmu.grammar.patterns.PatternsSimilarity;
import edu.gmu.grammar.patterns.TSPattern;
import edu.gmu.grammar.patterns.TSPatterns;
import edu.gmu.grammar.patterns.PatternsAndTransformedData;
import net.seninp.grammarviz.GrammarVizConfiguration;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.LoggerFactory;
import weka.attributeSelection.*;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
//import weka.classifiers.bayes.*;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.output.prediction.*;
import weka.classifiers.functions.*;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.classifiers.functions.supportVector.RBFKernel;
//import weka.classifiers.lazy.IBk;
//import weka.classifiers.trees.*;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
//import weka.classifiers.meta.OneClassClassifier;
import weka.core.*;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

//import libsvm.*;

public class GCProcessMultiClass {

	public static final int FILTER_NORMALIZE = 0;
	/** The filter to apply to the training data: Standardize */
	public static final int FILTER_STANDARDIZE = 1;
	/** The filter to apply to the training data: None */
	public static final int FILTER_NONE = 2;
	/** The filter to apply to the training data */
	public static final Tag[] TAGS_FILTER = { new Tag(FILTER_NORMALIZE, "Normalize training data"),
			new Tag(FILTER_STANDARDIZE, "Standardize training data"),
			new Tag(FILTER_NONE, "No normalization/standardization"), };

	private int folderNum;

	private static final Logger consoleLogger;
	private static final Level LOGGING_LEVEL = Level.INFO;

	static {
		consoleLogger = (Logger) LoggerFactory.getLogger(GCProcessMultiClass.class);
		consoleLogger.setLevel(LOGGING_LEVEL);
	}

	private boolean isNormalize = true;

	public GCProcessMultiClass(int folderNum) {
		// this.isNormalize = isNormalize;
		this.folderNum = folderNum;
	}

	public double[][] transformTSWithPatternsTestTTT(Map<String, List<double[]>> dataset) {
		int tsNum = 0;
		for (Entry<String, List<double[]>> edata : dataset.entrySet()) {
			tsNum += edata.getValue().size();
		}

		double[][] transformedTS = new double[tsNum][];

		int idxTs = 0;
		for (Entry<String, List<double[]>> eData : dataset.entrySet()) {
			String clsLabel = eData.getKey();
			for (double[] tsInstance : eData.getValue()) {

				double[] tsWithLabel = new double[tsInstance.length + 1];
				System.arraycopy(tsInstance, 0, tsWithLabel, 0, tsInstance.length);
				tsWithLabel[tsInstance.length] = Integer.parseInt(clsLabel);
				transformedTS[idxTs] = tsWithLabel;
				idxTs++;
			}
		}
		return transformedTS;
	}

	public void doClassifyTransformedMultiCls(BestSelectedPatterns[] bestSelectedPatternsAllCls,
			Map<String, List<double[]>> trainData, Map<String, List<double[]>> testData,
			GrammarInductionMethod giMethod,
			ClassificationResults results) {

		consoleLogger.debug("Classifing...");

		TSPattern[] finalPatterns = combinePatterns(bestSelectedPatternsAllCls);

		double[][] transformedTrainTS = transformTSWithPatternsTest(finalPatterns, trainData, null);

		double[][] transformedTestTS = transformTSWithPatternsTest(finalPatterns, testData, results);

		double error;

		if(results != null) {
			StringBuffer output = new StringBuffer();
			error = classifyTransformedData(transformedTrainTS, transformedTestTS, output);
			//results.
			results.results = output.toString();
		} else {
			error = classifyTransformedData(transformedTrainTS, transformedTestTS);
		}

		consoleLogger.info("Classification Accuracy: " + String.valueOf(error));

	}

	/**
	 * Combine the selected best patterns for each class into one array.
	 * 
	 * @param bestSelectedPatternsAllCls
	 * @return
	 */
	public TSPattern[] combinePatterns(BestSelectedPatterns[] bestSelectedPatternsAllCls) {

		ArrayList<TSPattern> finalPatterns = new ArrayList<TSPattern>();
		ArrayList<int[]> existParams = new ArrayList<int[]>();

		for (int i = 0; i < bestSelectedPatternsAllCls.length; i++) {
			BestSelectedPatterns bsp = bestSelectedPatternsAllCls[i];
			int[] params = bsp.getBestParams();
			TSPattern[] patterns = bsp.getBestSelectedPatterns();

			if (existParams.size() < 1) {
				//System.err.println("patterns = " + patterns);
				if (patterns == null) {
					throw new NullPointerException("Error no patterns found.  Please considering scaling timeseries data.");
				}
				Collections.addAll(finalPatterns, patterns);
				existParams.add(params);
			} else {
				boolean exist = false;
				for (int[] eparams : existParams) {
					if (Arrays.equals(params, eparams)) {
						exist = true;
					}
				}
				if (!exist) {
					Collections.addAll(finalPatterns, patterns);
					existParams.add(params);
				}

			}
		}
		TSPattern[] rlt = new TSPattern[finalPatterns.size()];
		for (int i = 0; i < finalPatterns.size(); i++) {
			rlt[i] = finalPatterns.get(i);
		}
		return rlt;
	}

	/**
	 * Transform time series in to new space, which has features as the distance
	 * to patterns.
	 * 
	 * @param selectedPatterns
	 * @param trainDataPerClass
	 */
	public PatternsAndTransformedData transformTS(HashMap<String, TSPatterns> selectedPatterns,
												  Map<String, List<TimeSeriesTrain>> trainDataPerClass, PatternsSimilarity pSimilarity) {

		// int tsNum = 0;
		int patternNum = 0;
		for (Entry<String, List<TimeSeriesTrain>> eTrain : trainDataPerClass.entrySet()) {
			String label = eTrain.getKey();
			// tsNum += eTrain.getValue().size();

			TSPatterns tsps = selectedPatterns.get(label);
			patternNum += tsps.getPatterns().size();
		}

		TSPattern[] allPatterns = new TSPattern[patternNum];
		int idxPattern = 0;
		// Put all patterns together
		for (Entry<String, TSPatterns> ePattern : selectedPatterns.entrySet()) {
			TSPatterns tsps = ePattern.getValue();
			for (TSPattern tsp : tsps.getPatterns()) {
				allPatterns[idxPattern] = tsp;
				idxPattern++;
			}
		}

		TSPattern[] goodPatterns = refinePatterns(allPatterns, pSimilarity);

		// allPatterns = featureRemoveRedundence(allPatterns, redundencyPer);
		double[][] transformedTS = transformTSWithPatterns(goodPatterns, trainDataPerClass);

		PatternsAndTransformedData patternsAndTransformedTS = new PatternsAndTransformedData();

		patternsAndTransformedTS.setAllPatterns(goodPatterns);
		patternsAndTransformedTS.setTransformedTS(transformedTS);
		return patternsAndTransformedTS;
	}

	private TSPattern[] refinePatterns(TSPattern[] allPatterns, PatternsSimilarity pSimilarity) {
		double t;
		if (pSimilarity.getIsTSimilarSetted()) {
			t = pSimilarity.gettSimilar();
		} else {
			t = pSimilarity.getInitialTSimilar();
		}
		ArrayList<TSPattern> refinedPatterns = new ArrayList<TSPattern>();
		outter: for (TSPattern tsp : allPatterns) {
			double[] tspValues = tsp.getPatternTS();

			for (int i = 0; i < refinedPatterns.size(); i++) {
				TSPattern goodP = refinedPatterns.get(i);
				double[] goodPValues = goodP.getPatternTS();

				double d;
				if (tspValues.length > goodPValues.length)
					d = DistMethods.calcDistEuclidean(tspValues, goodPValues);
				else
					d = DistMethods.calcDistEuclidean(goodPValues, tspValues);

				if (d < t) {
					if (goodP.getFrequency() < tsp.getFrequency()) {
						// refinedPatterns.set(i, tsp);
						refinedPatterns.remove(i);
						refinedPatterns.add(tsp);
					}
					continue outter;
				}

			}

			refinedPatterns.add(tsp);
		}

		return refinedPatterns.toArray(new TSPattern[refinedPatterns.size()]);
	}

	public double[][] transformTSWithPatterns(TSPattern[] allPatterns, Map<String, List<TimeSeriesTrain>> dataset) {
		int tsNum = 0;
		int patternNum = allPatterns.length;
		for (Entry<String, List<TimeSeriesTrain>> eTrain : dataset.entrySet()) {
			// String label = eTrain.getKey();
			tsNum += eTrain.getValue().size();
		}

		double[][] transformedTS = new double[tsNum][patternNum + 1];

		int idxTs = 0;
		for (Entry<String, List<TimeSeriesTrain>> eTrain : dataset.entrySet()) {
			String clsLabel = eTrain.getKey();
			for (TimeSeriesTrain tsTrain : eTrain.getValue()) {
				double[] tsInstance = tsTrain.getValues();

				int idxPattern = 0;
				for (int i = 0; i < patternNum; i++) {

					TSPattern tsp = allPatterns[i];
					transformedTS[idxTs][idxPattern] = DistMethods.calcDistEuclidean(tsInstance, tsp.getPatternTS());
					idxPattern++;

				}
				transformedTS[idxTs][idxPattern] = Integer.parseInt(clsLabel);
				idxTs++;
			}
		}
		return transformedTS;
	}

	public double[][] transformTSWithPatternsTest(TSPattern[] allPatterns, Map<String, List<double[]>> dataset, ClassificationResults results) {
		GrammarVizConfiguration config = GrammarVizConfiguration.getConfiguration();

		int tsNum = 0;
		int patternNum = allPatterns.length;
		for (Entry<String, List<double[]>> edata : dataset.entrySet()) {
			tsNum += edata.getValue().size();
		}

		double[][] transformedTS = new double[tsNum][patternNum + 1];


		ArrayList<double[]> timeseries = new ArrayList<>();

		int idxTs = 0;
		for (Entry<String, List<double[]>> eData : dataset.entrySet()) {
			String clsLabel = eData.getKey();
			for (double[] tsInstance : eData.getValue()) {

				timeseries.add(tsInstance);
				int idxPattern = 0;
				for (int i = 0; i < patternNum; i++) {

					TSPattern tsp = allPatterns[i];

					if (config.getDistanceMeasure() == GrammarVizConfiguration.EUCLIDEAN_DISTANCE) {
						transformedTS[idxTs][idxPattern] = DistMethods.calcDistEuclidean(tsInstance, tsp.getPatternTS());
					} else {
						transformedTS[idxTs][idxPattern] = DistMethods.calcDistDTW(tsInstance, tsp.getPatternTS(),
																				   config.getDTWWindow());
					}

					idxPattern++;

				}
				transformedTS[idxTs][idxPattern] = Integer.parseInt(clsLabel);
				idxTs++;
			}
		}
		if (results != null) {
			results.testDataTS = timeseries.toArray(new double[timeseries.size()][]);
		}
		return transformedTS;
	}

	public double[][] transformTSWithPatternsTestShift(TSPattern[] allPatterns, Map<String, List<double[]>> dataset) {
		int tsNum = 0;
		int patternNum = allPatterns.length;
		for (Entry<String, List<double[]>> edata : dataset.entrySet()) {
			tsNum += edata.getValue().size();
		}

		double[][] transformedTS = new double[tsNum][patternNum + 1];

		int idxTs = 0;
		for (Entry<String, List<double[]>> eData : dataset.entrySet()) {
			String clsLabel = eData.getKey();
			for (double[] tsInstance : eData.getValue()) {

				// Rebuild time series for shifting
				int tsLen = tsInstance.length;
				int midPoint = tsLen / 2;
				double[] firstPart = Arrays.copyOfRange(tsInstance, midPoint, tsLen);
				double[] secondPart = Arrays.copyOfRange(tsInstance, 0, midPoint);
				double[] rebuildedValues = ArrayUtils.addAll(firstPart, secondPart);

				int idxPattern = 0;
				for (int i = 0; i < patternNum; i++) {

					TSPattern tsp = allPatterns[i];
					double d1 = DistMethods.calcDistEuclidean(tsInstance, tsp.getPatternTS());
					double d2 = DistMethods.calcDistEuclidean(rebuildedValues, tsp.getPatternTS());
					double d = d1;
					if (d1 > d2)
						d = d2;
					transformedTS[idxTs][idxPattern] = d;
					idxPattern++;

				}
				transformedTS[idxTs][idxPattern] = Integer.parseInt(clsLabel);
				idxTs++;
			}
		}
		return transformedTS;
	}

	public SMO getPolySvmClassifier(double svmComplexity, double polyKernelExponent) {
		SMO classifier = new SMO();

		//classifier.setBuildLogisticModels(false);
		classifier.setC(svmComplexity);
		classifier.setChecksTurnedOff(false);
		classifier.setDebug(false);
		classifier.setEpsilon(1.0E-12);
		classifier.setFilterType(new SelectedTag(FILTER_NONE, TAGS_FILTER));
		PolyKernel pk = new PolyKernel();
		pk.setCacheSize(250007);
		pk.setChecksTurnedOff(false);
		pk.setDebug(false);
		pk.setExponent(polyKernelExponent);
		pk.setUseLowerOrder(false);
		classifier.setKernel(pk);

		classifier.setNumFolds(-1);
		classifier.setRandomSeed(1);
		classifier.setToleranceParameter(0.001);

		return classifier;
	}

	public static SMO getRbfSvmClassifier(double svmComplexity, double gamma) {
		SMO classifier = new SMO();

		//classifier.setBuildLogisticModels(false);
		classifier.setC(svmComplexity);
		classifier.setChecksTurnedOff(false);
		classifier.setDebug(false);
		classifier.setEpsilon(1.0E-12);
		classifier.setFilterType(new SelectedTag(FILTER_NONE, TAGS_FILTER));

		RBFKernel rk = new RBFKernel();
		rk.setGamma(gamma);

		rk.setChecksTurnedOff(false);
		rk.setDebug(false);
		rk.setCacheSize(250007);

		classifier.setKernel(rk);

		classifier.setNumFolds(-1);
		classifier.setRandomSeed(1);
		classifier.setToleranceParameter(0.001);

		return classifier;
	}

	/**
	 * 
	 * @param data,
	 *            J48; 2, NaiveBayes; 3, BayesNet; 4, LibSVM;
	 * @return
	 */
	public Classifier chooseClassifier(Instances data) {
		//int classfier = 4;

		Classifier cls = new RandomForest();//getPolySvmClassifier(1, 3);

		/*switch (classfier) {
		case 1:
			cls = new J48();
			break;
		case 2:
			cls = new NaiveBayes();
			break;
		case 3:
			cls = new BayesNet();
			break;
		case 4:
			cls = getPolySvmClassifier(1, 3);
			// cls = getRbfSvmClassifier(1, 0.5);
			// cls = new LibSVM();
			// try {
			// String[] options = { "-K", "2", "-S", "0", "-D", "3", "-R",
			// "0.0", "-N", "0.5", "-M", "100.0", "-G", "0.5", "-C",
			// "1000.0", "-E", "0.001", "-P", "0.1", "-Q" };
			// cls.setOptions(options);
			//
			// // CVParameterSelection cvClassifier = new
			// // CVParameterSelection();
			// // cvClassifier.setClassifier(cls);
			// // cvClassifier.addCVParameter("G 0.001 2 10");
			// // cvClassifier.addCVParameter("C 1 1100 10");
			// // cvClassifier.setNumFolds(5);
			// // cvClassifier.buildClassifier(data);
			// // cls.setOptions(cvClassifier.getBestClassifierOptions());
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			break;
		case 5:
			cls = new Logistic();
			break;
		case 6:
			cls = new RandomForest();
			break;
		case 7:
			cls = new LinearRegression();
			break;
		case 8:
			cls = new IBk();
			break;
		default:
			cls = new NaiveBayes();
			break;
		}*/

		return cls;
	}



//	public Evaluation buildOCC(double [][] transformedTS) {
//
//		Instances data = buildArff(transformedTS);
//
//		try {
//			OneClassClassifier cls = new OneClassClassifier();
//			cls.setTargetClassLabel();
//
//			cls.buildClassifier(data);
//			//eval.crossValidateModel(cls, data, folderNum, new Random(1));
//			cls.buildClassifier(data);
//
//
//		}catch(Exception e) {
//
//		}
//
//	}


	public Evaluation cvEvaluationAllCls(double[][] transformedTS) {


		Instances data = buildArff(transformedTS);

		// AttributeSelectedClassifier classifier = new
		// AttributeSelectedClassifier();
		// CfsSubsetEval eval = new CfsSubsetEval();
		// GreedyStepwise search = new GreedyStepwise();
		// search.setSearchBackwards(true);
		//
		// classifier.setClassifier(chooseClassifier());
		// classifier.setEvaluator(eval);
		// classifier.setSearch(search);
		// 10-fold cross-validation
		Evaluation evaluation;
		Classifier cls = chooseClassifier(data);

		try {
			evaluation = new Evaluation(data);
			evaluation.crossValidateModel(cls, data, folderNum, new Random(1));
			// System.out.println(evaluation.toSummaryString());

			// double allError = evaluation.errorRate();
			return evaluation;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}


	}

	public double cvEvaluation(double[][] transformedTS) {
		Instances data = buildArff(transformedTS);

		// AttributeSelectedClassifier classifier = new
		// AttributeSelectedClassifier();
		// CfsSubsetEval eval = new CfsSubsetEval();
		// GreedyStepwise search = new GreedyStepwise();
		// search.setSearchBackwards(true);
		//
		// classifier.setClassifier(chooseClassifier());
		// classifier.setEvaluator(eval);
		// classifier.setSearch(search);
		// 10-fold cross-validation
		Evaluation evaluation;
		Classifier cls = chooseClassifier(data);

		try {
			evaluation = new Evaluation(data);
			evaluation.crossValidateModel(cls, data, folderNum, new Random(1));
			// System.out.println(evaluation.toSummaryString());

			double allError = evaluation.errorRate();
			return allError;
		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
	}

	public double classifyTransformedData(double[][] trainData, double[][] testData) {
		return this.classifyTransformedData(trainData, testData, null);
	}

	public double classifyTransformedData(double[][] trainData, double[][] testData, StringBuffer output) {
		// return svmClassify(trainData, testData);

		Instances train = buildArff(trainData);
		Instances test = buildArff(testData);

		Classifier classifier = chooseClassifier(train);

		// Select attibutes again
		// AttributeSelectedClassifier classifier = new
		// AttributeSelectedClassifier();
		// CfsSubsetEval eval1 = new CfsSubsetEval();
		// GreedyStepwise search = new GreedyStepwise();
		// search.setSearchBackwards(true);
		// classifier.setClassifier(cls);
		// classifier.setEvaluator(eval1);
		// classifier.setSearch(search);

		try {
			classifier.buildClassifier(train);
			// evaluate classifier and print some statistics
			Evaluation eval = new Evaluation(train);
			eval.evaluateModel(classifier, test);

			if(output != null) {
				CSV results = new CSV();
				results.setBuffer(output);
				results.setHeader(test);
				results.print(classifier,test);
			}

			String rltString = eval.toSummaryString("\n\n======\nResults: ", false);
			System.out.println(rltString);

			return eval.errorRate();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return 1;

	}

	public double[][] arffToArray(Instances data) {
		double[][] reversed_rlt = new double[data.numAttributes()][];

		data.setClassIndex(data.numAttributes() - 1);

		for (int i = 0; i < data.numAttributes(); i++) {

			double[] values = data.attributeToDoubleArray(i);
			reversed_rlt[i] = values;
		}

		double[][] rlt = new double[data.numInstances()][data.numAttributes()];

		for (int i = 0; i < data.numInstances(); i++) {
			for (int j = 0; j < data.numAttributes(); j++) {
				rlt[i][j] = reversed_rlt[j][i];
				if (j == data.numAttributes() - 1)
					rlt[i][j]++;
			}
		}
		return rlt;
	}
	//
	// public double svmClassify(double[][] trainData, double[][] testData) {
	//
	// int numFeature = trainData[0].length;
	//
	// svm_parameter param = new svm_parameter();
	// param.svm_type = svm_parameter.C_SVC;
	// param.kernel_type = svm_parameter.RBF;
	// param.degree = 3;
	// param.probability = 0;
	// param.gamma = 0.5;
	// param.coef0 = 0;
	// param.nu = 0.5;
	// param.C = 1;
	// param.cache_size = 100;
	// param.eps = 0.001;
	// // param.shrinking = 1;
	//
	// svm_problem prob = new svm_problem();
	// int numTrain = trainData.length;
	// // Number of training data.
	// prob.l = numTrain;
	// // Initial label of traning data.
	// prob.y = new double[numTrain];
	// // Training data
	// prob.x = new svm_node[numTrain][];
	//
	// for (int i = 0; i < numTrain; i++) {
	// double[] features = trainData[i];
	// prob.x[i] = new svm_node[features.length - 1];
	// for (int j = 0; j < features.length - 1; j++) {
	// svm_node node = new svm_node();
	// node.index = j;
	// node.value = features[j];
	// prob.x[i][j] = node;
	// }
	// // Assign value to label, training data.
	// prob.y[i] = features[numFeature - 1];
	// }
	//
	// svm_model model = svm.svm_train(prob, param);
	// int[] x = model.label;
	// int numTest = testData.length;
	// int missClsNum = 0;
	// for (int i = 0; i < numTest; i++) {
	// double[] featuresTestData = testData[i];
	// missClsNum += evaluateLibSVM(featuresTestData, model, x.length);
	// }
	// double errorRate = (double) missClsNum / (double) numTest;
	// // System.out.println("LibSVM Error Rate: " + fmt.format(errorRate *
	// // 100)
	// // + "%, Incorrect Classified Number: " + missClsNum);
	//
	// return errorRate;
	// }
	//
	// public int evaluateLibSVM(double[] features, svm_model model,
	// int totalClasses) {
	// svm_node[] nodes = new svm_node[features.length - 1];
	// for (int i = 0; i < features.length - 1; i++) {
	// svm_node node = new svm_node();
	// node.index = i;
	// node.value = features[i];
	//
	// nodes[i] = node;
	// }
	//
	// int[] labels = new int[totalClasses];
	// svm.svm_get_labels(model, labels);
	//
	// double[] prob_estimates = new double[totalClasses];
	// double v = svm.svm_predict_probability(model, nodes, prob_estimates);
	//
	// // System.out.println("(Actual:" + features[features.length - 1]
	// // + " Prediction:" + v + ")");
	//
	// if (features[features.length - 1] == v)
	// return 0;
	// else
	// return 1;
	// }

	/**
	 * Selected features from transformed time series.
	 * 
	 * @param transformedTS
	 * @return The indices of selected features.
	 */
	public int[] featureSelection(double[][] transformedTS) {
		int attrNum = transformedTS[0].length;

		Instances data = buildArff(transformedTS);
		AttributeSelection attsel = new AttributeSelection();

		CfsSubsetEval eval = new CfsSubsetEval();
		GreedyStepwise search = new GreedyStepwise();
		search.setSearchBackwards(true);

		// SVMAttributeEval eval = new SVMAttributeEval();
		// Ranker search = new Ranker();

		attsel.setEvaluator(eval);
		attsel.setSearch(search);
		try {
			attsel.SelectAttributes(data);
			//
			// obtain the attribute indices that were selected
			int[] indices = attsel.selectedAttributes();
			// System.out.println(Utils.arrayToString(indices));

			// The label index must be in the selected indices.
			if (indices[indices.length - 1] != attrNum - 1) {
				int[] result = Arrays.copyOf(indices, indices.length + 1);
				result[indices.length] = attrNum - 1;
				return result;
			}

			return indices;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Instances buildArff(double[][] array) {
		FastVector atts = new FastVector();
		int attrNum = array[0].length;
		for (int i = 0; i < attrNum - 1; i++) {
			atts.addElement(new Attribute("C" + String.valueOf(i + 1)));
		}

		FastVector classVal = new FastVector();
		List<Integer> clsLabel = new ArrayList<Integer>();
		for (int i = 0; i < array.length; i++) {
			clsLabel.add((int) array[i][attrNum - 1]);
		}
		int clsNum = Collections.max(clsLabel);
		for (int i = 1; i <= clsNum; i++) {
			classVal.addElement(String.valueOf(i));
		}

		atts.addElement(new Attribute("@@class@@", classVal));
		// atts.add(new Attribute("@@class@@", classVal));

		// 2. create Instances object
		Instances test = new Instances("DistanceToPatterns", atts, 0);

		// 3. fill with data
		for (int tsI = 0; tsI < array.length; tsI = tsI + 1) {
			double vals[] = new double[test.numAttributes()];

			for (int attrI = 0; attrI < attrNum - 1; attrI++) {
				vals[attrI] = array[tsI][attrI];
			}
			vals[attrNum - 1] = array[tsI][attrNum - 1] - 1;
			test.add(new SparseInstance(1.0, vals));
		}
		test.setClassIndex(test.numAttributes() - 1);

		return (test);
	}

	/**
	 * Select the most frequent patterns from patterns for each class.
	 * 
	 * @param allPatterns
	 * @param allStartPositions
	 *            , the percentage of representative pattern number.
	 * @return
	 */
	public static HashMap<String, TSPatterns> selectTopFrequentPatterns(HashMap<String, TSPatterns> allPatterns,
			HashMap<String, int[]> allStartPositions) {
		HashMap<String, TSPatterns> topFrequentPatterns = new HashMap<String, TSPatterns>();

		for (Entry<String, TSPatterns> e : allPatterns.entrySet()) {
			int k = 3;

			String classLabel = e.getKey();
			TSPatterns patternsInClass = e.getValue();

			// Patterns in each class.
			@SuppressWarnings("unchecked")
			ArrayList<TSPattern> tempTSPatterns = (ArrayList<TSPattern>) patternsInClass.getPatterns().clone();

			int pNum = tempTSPatterns.size();
			// int[] startPs = allStartPositions.get(classLabel);
			// int pNum = 5;
			// if (startPs.length > 0) {
			// int originalTSLen = startPs[0];
			// int pLen = tempTSPatterns.get(0).getPatternTS().length;
			// pNum = 5 * originalTSLen / pLen;
			// } else {
			// }
			// int pNum = (int) (patternRate * tempTSPatterns.size());
			if (pNum > k) {
				k = pNum;
				if (k > 20) {
					k = 20;
				}
			}

			if (tempTSPatterns.size() < k) {
				k = tempTSPatterns.size();
				if (k < 1) {
					continue;
				}
				// return null;
			}

			Collections.sort(tempTSPatterns, Collections.reverseOrder());

			TSPatterns mostFrequentPatterns = new TSPatterns(classLabel);
			for (int i = 0; i < k; i++) {
				TSPattern pattern = tempTSPatterns.get(i);
				mostFrequentPatterns.addPattern(pattern);
			}
			topFrequentPatterns.put(classLabel, mostFrequentPatterns);
		}

		return topFrequentPatterns;
	}

	public static TSPattern[] featureRemoveRedundence(TSPattern[] allPatterns, double overlapPerT) {
		ArrayList<Integer> allSPPatterns = new ArrayList<Integer>();
		ArrayList<Integer> tobeKeepedIdx = new ArrayList<Integer>();

		outter: for (int i = 0; i < allPatterns.length; i++) {
			TSPattern pattern = allPatterns[i];
			int startP = pattern.getStartP();
			int pLen = pattern.getPatternTS().length;
			int pDiffThreshold = (int) (pLen * overlapPerT);
			for (int existP : allSPPatterns) {
				// There is overlap, remove this pattern.
				if (Math.abs(startP - existP) <= pDiffThreshold) {
					continue outter;
				}
			}
			tobeKeepedIdx.add(i);
			allSPPatterns.add(startP);

		}
		int tobeKeepedNum = tobeKeepedIdx.size();
		TSPattern[] noOverlapPatterns = new TSPattern[tobeKeepedNum];

		for (int i = 0; i < tobeKeepedNum; i++) {
			noOverlapPatterns[i] = allPatterns[tobeKeepedIdx.get(i)];
		}
		return noOverlapPatterns;
	}
}
