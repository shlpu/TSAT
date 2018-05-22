package net.seninp.grammarviz.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.gmu.grammar.classification.util.ClassificationResults;
import edu.gmu.grammar.classification.util.PSDirectTransformAllClass;
import edu.gmu.grammar.classification.util.RPMTrainedData;
import edu.gmu.grammar.patterns.TSPattern;
import net.seninp.grammarviz.model.GrammarVizMessage;
import net.seninp.util.StackTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by David Fleming on 1/24/17.
 * updated by Drew
 *
 * A class to handle the state of RPM for GrammarViz.
 */
public class RPMHandler extends Observable implements Runnable {

    private PSDirectTransformAllClass RPM;
    private RPMTrainedData trainingResults;
    private String trainingFilename;
    private double[][] trainingData;
    private String[] trainingLabels; // these must be the integer values
    private ClassificationResults testingResults;
    private String[] testingLabels; // these must be the integer values
    private Map<String, Integer> classLabs; // this maps the string label that was loaded to the integer that we use
    private Map<String, String> labsToClass; // this maps the lab that we use to the class that was given
    private TSPattern[] finalPatterns;
    private int numberOfIterations;

    /**
     * Constructor, creates all internal references and sets up defaults.
     */
    public RPMHandler() {
        super();
        this.RPM = new PSDirectTransformAllClass();
        this.numberOfIterations = PSDirectTransformAllClass.DEFAULT_NUMBER_OF_ITERATIONS;
        classLabs = new HashMap<>();
    }

    /**
     * Trains RPM and pulls out final patterns from the results.
     *
     * @param filename the path for the training data, used when saving the model.
     * @param data the time series data.
     * @param labels the labels for the time series data, must be a one-to-one mapping.
     * @throws java.io.IOException
     */
    public synchronized void RPMTrain(String filename, double[][] data, String[] labels) throws java.io.IOException {
        String[] newLabels = createReformattedLabels(labels);
        this.trainingResults = this.RPM.RPMTrain(filename, data, newLabels, PSDirectTransformAllClass.DEFAULT_STRATEGY,
                this.numberOfIterations);
        this.finalPatterns = this.trainingResults.finalPatterns();
    }

    /**
     * Runs training on a background thread, notifying GrammarViz upon completion.
     */
    @Override
    public void run() {
        this.log("Starting RPM Training in Background");
        try {
            this.RPMTrain(this.trainingFilename, this.trainingData, this.trainingLabels);
            this.setChanged();
            notifyObservers(new GrammarVizMessage(GrammarVizMessage.RPM_TRAIN_RESULTS_UPDATE_MESSAGE, this));

            this.log("Finished RPM Training in Background");
        } catch(Exception e) {
            this.log("error while training RPM model " + StackTrace.toString(e));
            e.printStackTrace();
        }
    }

    /**
     * Tests the data against the trained model reporting the statistics.
     * @param filename the filename for the testing data.
     * @param data the time series testing data.
     * @param labels the labels for the time series testing data, must be a one-to-one mapping.
     * @throws java.io.IOException
     */
    public synchronized void RPMTestData(String filename, double[][] data, String[] labels) throws java.io.IOException {
        this.testingLabels = getReformattedLabels(labels);
        this.testingResults = this.RPM.RPMTestData(filename, data, this.testingLabels);
    }

    /**
     * Loads an existing RPM trained model from file.
     *
     * @param filename the path to the saved model.
     * @throws Exception
     */
    public synchronized RPMTrainedData RPMLoadModel(String filename) throws Exception {
        RPMTrainedData rpmTrainedData = null;
        FileInputStream loadFile = new FileInputStream(filename);
        ObjectInputStream loadStream = new ObjectInputStream(loadFile);
        rpmTrainedData = (RPMTrainedData) loadStream.readObject();
        loadStream.close();
        loadFile.close();

        this.trainingResults = rpmTrainedData;
        this.trainingFilename = rpmTrainedData.training_data_path;
        this.finalPatterns = rpmTrainedData.finalPatterns();
        this.numberOfIterations = rpmTrainedData.iterations_num;



        return rpmTrainedData;

    }

    /**
     * Saves an RPM trained model to file.
     *
     * @param filename the path for where to save the model.
     */
    public synchronized void RPMSaveModel(String filename) {
        try {
            FileOutputStream saveFile = new FileOutputStream(filename);
            ObjectOutputStream saveStream = new ObjectOutputStream(saveFile);
            saveStream.writeObject(this.trainingResults);
            saveStream.close();
            saveFile.close();
        } catch(Exception e) {
            this.log("error while saving RPM model " + StackTrace.toString(e));
            e.printStackTrace();
        }

    }

    /**
     * Get the representative patterns from the trained model.
     *
     * @return the representative patterns.
     */
    public synchronized TSPattern[] getRepresentativePatterns() {

        for (TSPattern p : finalPatterns) {
            p.setLabel(labsToClass.get(p.getLabel()));
        }

        return this.finalPatterns;
    }

    /**
     * Parses the results from the testing phase, generating a label and accuracy ratio array.
     * output: [["Label", "Number of Wrongly labeled/Total number labeled"]]
     * @return the results from the testing phase.
     */
    public synchronized String[][] getResults() {
        if(this.testingResults == null)
            return null;

        HashMap<String, int[]> convertedResults = new HashMap<String, int[]>();

        String[] entries = this.testingResults.results.split("\n");
        if (entries[0].equals("#")) {
            String[][] output = new String[entries.length][2];
            output[0][0] = "Classified";
            output[0][1] = "Label probability for each class:";

            for (int i = 1; i < entries.length; i++) {
                String[] columns = entries[i].split(",");
                output[i][0] = columns[columns.length - 1];
                output[i][1] = "";
                for (int j = 1; j < columns.length - 1; j++) {
                    output[i][1] += columns[j] + ",";
                }
            }
            return output;
        }else {
            //System.err.println("Results = " + this.testingResults.results);
            for (Map.Entry<String, String> ent : labsToClass.entrySet()) {
                System.err.println("Key = " + ent.getKey() + " value: " + ent.getValue());
            }
            for (int i = 1; i < entries.length; i++) {
                String[] columns = entries[i].split(",");
                String actualClassLabel = labsToClass.get(columns[1].split(":")[0]);
                System.err.println("actual Class label = " + actualClassLabel + " before conversion = " + columns[1].split(":")[0]);



                if (!convertedResults.containsKey(actualClassLabel)) {
                    convertedResults.put(actualClassLabel, new int[2]);
                }

                int[] ratio = convertedResults.get(actualClassLabel);
                ratio[1]++;
                if (columns[3].equals("+")) {
                    ratio[0]++;
                }
            }

            String[][] output = new String[convertedResults.size()][2];
            int i = 0;
            for (Map.Entry<String, int[]> entry : convertedResults.entrySet()) {
                output[i][0] = entry.getKey();
                int[] ratio = entry.getValue();
                output[i][1] = ratio[0] + "/" + ratio[1];
                i++;
            }

            return output;
        }
    }


    /**
     * Parses the results from the testing phase, generating a label and accuracy ratio array.
     * output: [["inst#", "actual class", "predicted class", "timeSeries"]]
     * @return the results from the testing phase.
     */
    public synchronized String[][] getMisclassifiedResults() {
        if(this.testingResults == null)
            return null;

        ArrayList<String[]> out = new ArrayList<>();

        String[] entries = this.testingResults.results.split("\n");
        if (entries[0].equals("#")) {
            String[] header = new String[4];
            header[0] = "Instance Number";
            header[1] = "Label probability for each class:";
            header[2] = "Predicted class";
            header[3] = "Time Series";
            out.add(header);

            for (int i = 1; i < entries.length; i++) {
                String[] columns = entries[i].split(",");
                String instance = columns[0];
                String predictedClassLabel = columns[columns.length - 1];
                StringBuilder timeSeries = new StringBuilder();
                for (int j = 0; j < getTestingResults().testDataTS[Integer.parseInt(instance) - 1].length; j++) {
                    timeSeries.append(getTestingResults().testDataTS[Integer.parseInt(instance) - 1][j] + ", ");
                }
                String actualClassLabel = "";
                for (int j = 1; j < columns.length - 1; j++) {
                    actualClassLabel += columns[j] + ",";
                }

                String[] result = new String[4];
                result[0] = instance;
                result[1] = actualClassLabel;
                result[2] = predictedClassLabel;
                result[3] = timeSeries.toString();
                out.add(result);
            }

        }else {
            //System.err.println("Results = " + this.testingResults.results);
            for (int i = 1; i < entries.length; i++) {
                String[] columns = entries[i].split(",");
                String instance = columns[0];
                String actualClassLabel = columns[1].split(":")[0];
                String predictedClassLabel = columns[2].split(":")[0];
                StringBuilder timeSeries = new StringBuilder();
                for (int j = 0; j < getTestingResults().testDataTS[Integer.parseInt(instance) - 1].length; j++) {
                    timeSeries.append(getTestingResults().testDataTS[Integer.parseInt(instance) - 1][j] + ", ");
                }

                if (columns[3].equals("+")) {
                    String[] result = new String[4];
                    result[0] = instance;
                    result[1] = labsToClass.get(actualClassLabel);
                    result[2] = labsToClass.get(predictedClassLabel);
                    result[3] = timeSeries.toString();
                    out.add(result);
                }
            }
        }
        return out.toArray(new String[out.size()][]);
    }

    /**
     * Get the windows size as found by RPM.
     *
     * @return the window size.
     */
    public synchronized int getWindowSize() {
        return this.trainingResults.windowSize;
    }

    /**
     * Get the PAA size as found by RPM.
     *
     * @return the PAA size.
     */
    public synchronized int getPaa() {
        return this.trainingResults.paa;
    }

    /**
     * Get the alphabet size as found by RPM.
     *
     * @return the alphabet size.
     */
    public synchronized int getAlphabet() {
        return this.trainingResults.alphabet;
    }

    /**
     * Get the labels from the training data.
     *
     * @return the labels from the training data.
     */
    public synchronized String[] getTrainedLabels() {
        return this.trainingLabels;
    }

    /**
     * Get the labels from the testing data.
     *
     * @return the labels from the testing data.
     */
    public synchronized String[] getTestingLabels() {
        return this.testingLabels;
    }

    /**
     * Set the maximum number of iterations RPM will do during training.
     *
     * @param numberOfIterations the maximum number of iterations.
     */
    public synchronized void setNumberOfIterations(int numberOfIterations) { this.numberOfIterations = numberOfIterations; }

    /**
     * Get the maximum number of iterations RPM will do during training.
     *
     * @return the maximum number of iterations.
     */
    public synchronized int getNumberOfIterations() {return this.numberOfIterations; }

    /**
     * Get the path to the training data.
     *
     * @return the path to the training data.
     */
    public synchronized String getTrainingFilename() {
        return trainingFilename;
    }

    /**
     * Set the path to the training data.
     *
     * @param trainingFilename the path to the training data.
     */
    public synchronized void setTrainingFilename(String trainingFilename) {
        this.trainingFilename = trainingFilename;
    }

    /**
     * Get the training time series data.
     *
     * @return the training time series data.
     */
    public synchronized double[][] getTrainingData() {
        return trainingData;
    }

    /**
     * Set the training time series data.
     *
     * @param trainingData the training time series data.
     */
    public synchronized void setTrainingData(double[][] trainingData) {
        this.trainingData = trainingData;
    }

    /**
     * Get the training time series labels.
     *
     * @return the training time series labels.
     */
    public synchronized String[] getTrainingLabels() {
        return trainingLabels;
    }

    /**
     * Set the training time series labels.
     *
     * @param trainingLabels the training time series labels.
     */
    public synchronized void setTrainingLabels(String[] trainingLabels) {
        this.trainingLabels = trainingLabels;
    }

    public synchronized String[] createReformattedLabels(String[] curLabels) {
        // ok so here I'm going to work the magic...
        // trainingLabels are not in the right format they are going to be just any old string
        // i need them to be integer values from 1 to n (where n is the number of classes)
        // so lets reformat so as to be useful
        classLabs = new HashMap<>(); // need to reset
        labsToClass = new HashMap<>();
        System.err.println("Going to create the labels");
        String[] newLabels = new String[curLabels.length];
        //Map<String, Integer> classLabs = new HashMap<>();
        for (int i = 0; i < curLabels.length; i++) {
            int num = classLabs.size() + 1;
            if (!classLabs.containsKey(curLabels[i])) {
                classLabs.put(curLabels[i], num);
                labsToClass.put(Integer.toString(num), curLabels[i]);
            }
            newLabels[i] = classLabs.get(curLabels[i]).toString();
        }
        labsToClass.put("?", "?");
        return newLabels;
    }

    public synchronized String[] getReformattedLabels(String[] curLabels) {
        // so from the classLabs get the reformated labels
        String[] newLabels = new String[curLabels.length];
        //Map<String, Integer> classLabs = new HashMap<>();
        for (int i = 0; i < curLabels.length; i++) {
            if (curLabels[i].equals("?")) {
                newLabels[i] = "-1";
            } else {
                newLabels[i] = classLabs.get(curLabels[i]).toString();
            }
        }
        return newLabels;
    }


    /**
     * Forces a reload of the training model by reconverting the data and loading the model.
     */
    public synchronized void forceRPMModelReload() {
        this.trainingResults.trainData = this.RPM.convertGrammarVizData(this.trainingData, this.trainingLabels);
        this.RPM.loadRPMTrain(this.trainingResults);
    }


    public ClassificationResults getTestingResults() {
        return testingResults;
    }

    /**
     * Formats a string with the results from testing in the format provided by getResults().
     *
     * @return the results from testing.
     */
    @Override
    public synchronized String toString() {
        StringBuilder output = new StringBuilder();
        String[][] results = this.getResults();
        for(int i = 0; i < results.length; i++) {
            output.append(results[i][0] + ": ");
            output.append(results[i][1] + "\n");
        }

        return output.toString();
    }

    /**
     * Uses GrammarViz's message passing to relay status updates to the GUI.
     * @param message
     */
    private void log(String message) {
        this.setChanged();
        notifyObservers(new GrammarVizMessage(GrammarVizMessage.STATUS_MESSAGE, "RPM Handler: " + message));
    }

    public void trainingToJSON(String outputPrefix) throws Exception{
        try(  PrintWriter out = new PrintWriter( outputPrefix + ".train" )  ){
            Gson g = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            out.write(g.toJson(finalPatterns));

        }
    }

    public void testingToJSON(String outputPrefix) throws Exception{
        try(  PrintWriter out = new PrintWriter( outputPrefix + ".test" )  ){
            Gson g = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            out.write(g.toJson(getMisclassifiedResults()));
        }
    }

    public void featureVectorToFile(String filename, double[][] data, String[] labels) throws Exception{
        try (PrintWriter out = new PrintWriter(filename)) {
            Gson g = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            out.write(g.toJson(trainingResults.getFeatureVector(RPM.convertGrammarVizData(data, labels), testingResults)));
        }


    }
}