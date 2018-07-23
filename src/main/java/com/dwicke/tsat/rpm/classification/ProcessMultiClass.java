package com.dwicke.tsat.rpm.classification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.dwicke.tsat.model.GrammarVizConfiguration;
import com.dwicke.tsat.rpm.connectGI.GrammarInductionMethod;
import com.dwicke.tsat.rpm.patterns.*;
import com.dwicke.tsat.rpm.util.ClassificationResults;
import com.dwicke.tsat.rpm.util.DistMethods;
import org.slf4j.LoggerFactory;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.output.prediction.CSV;
import weka.classifiers.misc.InputMappedClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.*;

import java.util.*;
import java.util.Map.Entry;



public class ProcessMultiClass {

	private int folderNum;
	private boolean testUnlabeled = false;

	private static final Logger consoleLogger;
	private static final Level LOGGING_LEVEL = Level.INFO;

	static {
		consoleLogger = (Logger) LoggerFactory.getLogger(ProcessMultiClass.class);
		consoleLogger.setLevel(LOGGING_LEVEL);
	}


	public ProcessMultiClass(int folderNum) {
		this.folderNum = folderNum;
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
            error = classifyTransformedData(transformedTrainTS, transformedTestTS, null);
        }

		//consoleLogger.info("Classification Accuracy: " + String.valueOf(error));

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
												  Map<String, List<double[]>> trainDataPerClass, PatternsSimilarity pSimilarity) {

		// int tsNum = 0;
		int patternNum = 0;
		for (Entry<String, List<double[]>> eTrain : trainDataPerClass.entrySet()) {
			String label = eTrain.getKey();
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

	public double[][] transformTSWithPatterns(TSPattern[] allPatterns, Map<String, List<double[]>> dataset) {
		int tsNum = 0;
		int patternNum = allPatterns.length;
		for (Entry<String, List<double[]>> eTrain : dataset.entrySet()) {
			tsNum += eTrain.getValue().size();
		}

		double[][] transformedTS = new double[tsNum][patternNum + 1];

		int idxTs = 0;
		for (Entry<String, List<double[]>> eTrain : dataset.entrySet()) {
			String clsLabel = eTrain.getKey();
			for (double[] tsInstance : eTrain.getValue()) {

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
				if (clsLabel.contains("?")) {
					clsLabel = "-1";
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







	/**
	 * 
	 * @param data,
	 *            J48; 2, NaiveBayes; 3, BayesNet; 4, LibSVM;
	 * @return
	 */
	public Classifier chooseClassifier(Instances data) {
		//int classfier = 4;

        Classifier cls = new RandomForest();

		return cls;
	}




	public Evaluation cvEvaluationAllCls(double[][] transformedTS) {


		Instances data = buildArff(transformedTS);

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




	public double classifyTransformedData(double[][] trainData, double[][] testData, StringBuffer output) {

		Instances train = buildArff(trainData);
		Instances test = buildArff(testData, trainData, train);


        Classifier chosenclassifier = chooseClassifier(train);

		InputMappedClassifier classifier = new InputMappedClassifier();
		classifier.setClassifier(chosenclassifier);
		classifier.setSuppressMappingReport(true);

		try {
			classifier.buildClassifier(train);
			// evaluate classifier and print some statistics
			Evaluation eval = new Evaluation(train);

			if (!testUnlabeled) {
				eval.evaluateModel(classifier, test);



				if (output != null) {
					CSV results = new CSV();
					results.setBuffer(output);
					results.setHeader(test);
					results.print(classifier, test);
				}

				String rltString = eval.toSummaryString("\n\n======\nResults: ", false);
				System.out.println(rltString);
				System.out.println("F1 score:");

				for (int i = 0; i < train.numClasses(); i++) {
					System.out.println("class " + (i + 1) + "F1 Score: " + eval.fMeasure(i) + " AUC: " + eval.areaUnderROC(i) + " MCC = " + eval.matthewsCorrelationCoefficient(i));
				}
				System.out.println("Weighted F1 Score = " + eval.weightedFMeasure());
				System.out.println("Weighted AUC = " + eval.weightedAreaUnderROC());
				System.out.println("Weighted MCC = " + eval.weightedMatthewsCorrelation());

				System.out.println(eval.toMatrixString());



				return eval.errorRate();
			}
			output.append("#\n");
			int instNum = 1;
			for (Instance i : test) {
				double[] val = classifier.distributionForInstance(i);
				System.err.print(instNum + ", ");
				output.append(instNum + ", ");
				instNum++;
				for (int j = 0; j < val.length; j++) {
					System.err.print(val[j] + ", ");
					output.append(val[j] + ", ");
				}

				output.append(test.classAttribute().value((int) classifier.classifyInstance(i)) + "\n");
				System.err.println(test.classAttribute().value((int) classifier.classifyInstance(i)));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return 1;

	}



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

	public Instances buildArff(double[][] array, double[][] trainData, Instances train) {
		FastVector atts = new FastVector();
		int attrNum = trainData[0].length;
		System.err.println("trainData length = " + attrNum + " test = " + array[0].length);
		for (int i = 0; i < attrNum - 1; i++) {
			atts.addElement(new Attribute("C" + String.valueOf(i + 1)));
		}

		FastVector classVal = new FastVector();
		List<Integer> clsLabel = new ArrayList<Integer>();
		for (int i = 0; i < trainData.length; i++) {
			clsLabel.add((int) trainData[i][attrNum - 1]);
		}

		int clsNum = Collections.max(clsLabel);
		boolean wasNeg = false;

		for (int i = 1; i <= clsNum; i++) {
			classVal.addElement(String.valueOf(i));
		}


		atts.addElement(new Attribute("@@class@@", classVal));
		// atts.add(new Attribute("@@class@@", classVal));

		// 2. create Instances object
		Instances test = new Instances("DistanceToPatterns", atts, 0);

		// 3. fill with data

		for (int i = 0; i < array.length; i++) {
			DenseInstance denseInstance = new DenseInstance(attrNum);
			for (int j = 0; j < array[i].length - 1; j++) {
				denseInstance.setValue(j, array[i][j]);
			}
			if (array[i][array[i].length - 1] == -1) {
				testUnlabeled = true;
				denseInstance.setMissing(array[i].length - 1);
			} else {
				denseInstance.setValue(array[i].length - 1, array[i][array[i].length - 1] - 1);
			}
			denseInstance.setDataset(train);
			test.add(denseInstance);
		}

		test.setClassIndex(test.numAttributes() - 1);



		return (test);
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
		boolean wasNeg = false;
		if (clsNum != -1) {
			for (int i = 1; i <= clsNum; i++) {
				classVal.addElement(String.valueOf(i));
			}
		}else {
			System.err.println("adding ?");
			wasNeg = true;
			classVal.addElement("?");
		}

		atts.addElement(new Attribute("@@class@@", classVal));
		// atts.add(new Attribute("@@class@@", classVal));

		// 2. create Instances object
		Instances test = new Instances("DistanceToPatterns", atts, 0);

		// 3. fill with data

		for (int i = 0; i < array.length; i++) {
			DenseInstance denseInstance = new DenseInstance(attrNum);
			for (int j = 0; j < array[i].length - 1; j++) {
				denseInstance.setValue(j,array[i][j]);
			}
			if (array[i][array[i].length - 1] == -1){
				denseInstance.setValue(array[i].length - 1,0);
			} else {
				denseInstance.setValue(array[i].length - 1, array[i][array[i].length - 1] - 1);
			}
			test.add(denseInstance);
		}

		test.setClassIndex(test.numAttributes() - 1);
		return (test);
	}
}
