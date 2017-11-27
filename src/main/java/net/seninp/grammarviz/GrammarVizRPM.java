package net.seninp.grammarviz;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import net.seninp.grammarviz.logic.RPMHandler;
import net.seninp.grammarviz.model.GrammarVizMessage;
import net.seninp.grammarviz.model.GrammarVizModel;
import net.seninp.util.StackTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dwicke on 4/14/17.
 *
 * A command line interface for using RPM with GrammarViz.
 */
public class GrammarVizRPM {




    private static final Logger LOGGER = LoggerFactory.getLogger(GrammarVizRPM.class);

    private boolean enableRPM = false;
    private String[] RPMLabels;
    final static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


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

    /**
     * The main function used to run GrammarViz/RPM from the command line, it takes in an argument list
     * and runs RPM using those parameters.
     *
     * @param args the command line arguments.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // Setup GrammarViz/RPM commandline system
        GrammarVizRPM rpmCLI = new GrammarVizRPM();
        // Handle command line arguments
        JCommander jct = new JCommander(rpmCLI, args);

        if (0 == args.length) { // If there are no arguments
            jct.usage(); // tell the user how to use this tool
        }
        else { // Else run RPM using the command line arguments
            RPMHandler rpmHandler = new RPMHandler();
            double[][] data = rpmCLI.loadDataPrivate("0", rpmCLI.trainDataFilename);
            rpmHandler.setNumberOfIterations(rpmCLI.numIterations);
            LOGGER.debug("loaded training data");
            rpmHandler.RPMTrain(rpmCLI.trainDataFilename, data, rpmCLI.RPMLabels);
            LOGGER.debug("finished training using RPM");
            LOGGER.debug("Window size = " + rpmHandler.getWindowSize());
            LOGGER.debug("Alphabet = " + rpmHandler.getAlphabet());
            LOGGER.debug("PAA = " + rpmHandler.getPaa());

            rpmHandler.RPMSaveModel(rpmCLI.saveTrainedModelFilename);
            rpmHandler.trainingToJSON(rpmCLI.saveTrainedModelFilename);


            if (rpmCLI.testdataFilename != null) {
                GrammarVizConfiguration gconf = GrammarVizConfiguration.getConfiguration();
                gconf.setDistanceMeasure(GrammarVizConfiguration.EUCLIDEAN_DISTANCE);
                LOGGER.debug("Testing using Euclidean distance");
                double[][] testdata = rpmCLI.loadDataPrivate("0", rpmCLI.testdataFilename);
                rpmHandler.RPMTestData(rpmCLI.testdataFilename, testdata, rpmCLI.RPMLabels);
                LOGGER.debug("Results:");
                LOGGER.debug(rpmHandler.toString());
                rpmHandler.testingToJSON(rpmCLI.saveTrainedModelFilename);

//                LOGGER.debug("Testing using DTW");
//
//                gconf.setDistanceMeasure(GrammarVizConfiguration.DTW_DISTANCE);
//                gconf.setDTWWindow(rpmCLI.windowSize);
//                rpmHandler.RPMTestData(rpmCLI.testdataFilename, testdata, rpmCLI.RPMLabels);
//                LOGGER.debug("Results:");
//                LOGGER.debug(rpmHandler.toString());
            }

        }
    }

    /**
     * Logging function.
     *
     * @param message the message to be logged.
     */
    private void log(String message) {
        LOGGER.debug(message);
    }


    /**
     * Loads time series data from file for use with GrammarViz/RPM.
     *
     * @param limitStr limits how many lines can be read in from the file.
     * @param fileName the path to the time series data.
     * @return the time series data.
     */
    double[][] loadDataPrivate(String limitStr, String fileName) {
        // check if everything is ready
        if ((null == fileName) || fileName.isEmpty()) {
            this.log("unable to load data - no data source selected yet");
            return null;
        }

        // make sure the path exists
        Path path = Paths.get(fileName);
        if (!(Files.exists(path))) {
            this.log("file " + fileName + " doesn't exist.");
            return null;
        }

        // read the input
        //
        // init the data araay
        ArrayList<ArrayList<Double>> data = new ArrayList<ArrayList<Double> >();

        // lets go
        try {

            // set the lines limit
            long loadLimit = 0l;
            if (!(null == limitStr) && !(limitStr.isEmpty())) {
                loadLimit = Long.parseLong(limitStr);
            }

            // open the reader
            BufferedReader reader = Files.newBufferedReader(path, DEFAULT_CHARSET);

            // read by the line in the loop from reader
            String line = null;
            long lineCounter = 0;
            this.enableRPM = false;

            while ((line = reader.readLine()) != null) {

                String[] lineSplit = line.trim().split("\\s+");

                if(0 == lineCounter)
                {
                    if(lineSplit[0].compareTo("#") == 0) {
                        this.log("Found RPM Data");
                        this.enableRPM = true;
                        ArrayList<String> labels = new ArrayList<String>();
                        for(int i = 1; i < lineSplit.length; i++) {
                            labels.add(lineSplit[i]);
                        }
                        this.RPMLabels = labels.toArray(new String[labels.size()]);
                        continue;
                    }
                    data = new ArrayList<ArrayList<Double> >();
                    for (int i = 0; i < lineSplit.length; i++) {
                        data.add(new ArrayList<Double>());
                    }
                }

                if (lineSplit.length < data.size()) {
                    this.log("line " + (lineCounter+1) + " of file " + fileName + " contains too few data points.");
                }

                // we read only first column
                for (int i = 0; i < lineSplit.length; i++) {
                    double value = new BigDecimal(lineSplit[i]).doubleValue();
                    data.get(i).add(value);
                }

                lineCounter++;

                // break the load if needed
                if ((loadLimit > 0) && (lineCounter > loadLimit)) {
                    break;
                }
            }
            reader.close();
        }
        catch (Exception e) {

            return null;
        }


        double[][] output = null;
        // convert to simple doubles array and clean the variable
        if (!(data.isEmpty())) {
            output = new double[data.size()][data.get(0).size()];
            // this.ts[0] = new double[data.get(0).size()];

            for (int i = 0; i < data.size(); i++) {
                for (int j = 0; j < data.get(0).size(); j++) {
                    output[i][j] = data.get(i).get(j);
                }
            }
        }
        return output;
    }

}
