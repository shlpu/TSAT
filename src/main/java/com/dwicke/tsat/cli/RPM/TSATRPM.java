package com.dwicke.tsat.cli.RPM;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.dwicke.tsat.dataprocess.LoadTSDataset;
import com.dwicke.tsat.logic.RPMHandler;
import com.dwicke.tsat.model.GrammarVizConfiguration;
import com.dwicke.tsat.dataprocess.UCRUtils;
import net.seninp.util.StackTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.DataFormatException;

/**
 * Created by dwicke on 4/14/17.
 *
 * A command line interface for using RPM with GrammarViz.
 */
public class TSATRPM {


    private static final Logger LOGGER = LoggerFactory.getLogger(TSATRPM.class);

    private String[] RPMLabels;


    // Setup command line arguments
    @Parameter
    public List<String> parameters = new ArrayList<String>();


    @Parameter(names = { "--trainD", "-i" }, description = "The training data file name")
    String trainDataFilename;

    @Parameter(names = { "--testD", "-j" }, description = "The testing data file name")
    String testdataFilename;

    @Parameter(names = { "--numIters", "-n" }, description = "The number of iterations for training")
    int numIterations = 5;

    @Parameter(names = { "--model", "-m" }, description = "The trained model file name")
    String saveTrainedModelFilename;

    @Parameter(names = { "--wsize", "-s" }, description = "The DTW window size default is 10")
    int windowSize = 10;

    @Parameter(names = {"--mode"}, description = "0 train only, 1 train and test, 2 test with model")
    int mode = 0;

    /**
     * The main function used to run GrammarViz/RPM from the command line, it takes in an argument list
     * and runs RPM using those parameters.
     *
     * @param args the command line arguments.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // Setup GrammarViz/RPM commandline system
        TSATRPM rpmCLI = new TSATRPM();
        // Handle command line arguments
        JCommander jct = new JCommander(rpmCLI, args);

        if (0 == args.length) { // If there are no arguments
            jct.usage(); // tell the user how to use this tool
        }
        else { // Else run RPM using the command line arguments
            rpmCLI.process();
        }
    }


    public void process() throws Exception {

        if (mode == 0) {
            train();
        }else if (mode == 1) {
            test(train());
        } else if (mode == 2) {
            test(loadModel());
        }
    }



    public RPMHandler train() throws Exception {

        RPMHandler rpmHandler = new RPMHandler();
        Object[] objData = LoadTSDataset.loadData("0", rpmHandler.getTrainingFilename(), false);
        double[][] data = (double[][]) ((Object[]) objData[1])[0];
        this.RPMLabels =  (String []) ((Object[]) objData[1])[1];


        rpmHandler.setNumberOfIterations(this.numIterations);
        LOGGER.debug("loaded training data");
        rpmHandler.RPMTrain(this.trainDataFilename, data, this.RPMLabels);
        LOGGER.debug("finished training using RPM");
        LOGGER.debug("Window size = " + rpmHandler.getWindowSize());
        LOGGER.debug("Alphabet = " + rpmHandler.getAlphabet());
        LOGGER.debug("PAA = " + rpmHandler.getPaa());

        rpmHandler.RPMSaveModel(this.saveTrainedModelFilename);
        rpmHandler.trainingToJSON(this.saveTrainedModelFilename);
        rpmHandler.featureVectorToFile(this.saveTrainedModelFilename + "features.train", data, this.RPMLabels);
        return rpmHandler;
    }

    public void test(RPMHandler rpmHandler) throws  Exception {
        if (this.testdataFilename != null) {
            GrammarVizConfiguration gconf = GrammarVizConfiguration.getConfiguration();
            gconf.setDistanceMeasure(GrammarVizConfiguration.EUCLIDEAN_DISTANCE);
            LOGGER.debug("Testing using Euclidean distance");


            Object[] objData = LoadTSDataset.loadData("0", this.testdataFilename, true);
            double[][] testdata = (double[][]) ((Object[]) objData[1])[0];
            this.RPMLabels =  (String []) ((Object[]) objData[1])[1];

            if (testdata == null) {
                System.err.println("Error: File" + this.testdataFilename +  " not found");
                System.exit(9);
            }
            rpmHandler.RPMTestData(this.testdataFilename, testdata, this.RPMLabels);
            LOGGER.debug("Results:");
            LOGGER.debug(rpmHandler.toString());
            rpmHandler.testingToJSON(this.saveTrainedModelFilename);
            // write out the testing feature vector
            rpmHandler.featureVectorToFile(this.saveTrainedModelFilename + "features.test", testdata, this.RPMLabels);
        }
    }


    public RPMHandler loadModel() throws Exception {
        RPMHandler rpmHandler = new RPMHandler();

        rpmHandler.RPMLoadModel(this.saveTrainedModelFilename);
        String filename = rpmHandler.getTrainingFilename();
        if(!(new File(filename)).exists() || this.trainDataFilename != null) {
            // use the provided training data location whether or not the one that was originally used still exists
            filename = trainDataFilename;
        }
        Object[] data = LoadTSDataset.loadData("0", filename, false);
        double[][] tsData = (double[][]) ((Object[]) data[1])[0];
        this.RPMLabels =  (String []) ((Object[]) data[1])[1];
        rpmHandler.setTrainingData(tsData);
        rpmHandler.setTrainingLabels(this.RPMLabels);
        rpmHandler.createReformattedLabels(this.RPMLabels);
        rpmHandler.forceRPMModelReload();

        return rpmHandler;
    }



}
