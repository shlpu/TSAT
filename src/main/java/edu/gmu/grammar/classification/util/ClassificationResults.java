package edu.gmu.grammar.classification.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by David Fleming on 12/2/16.
 *
 * This class stores the results from testing.
 */
public class ClassificationResults {
    // Path for the testing data
    public String testDataPath;

    // The testing data
    public Map<String, List<double[]>> testData;

    // The results as an output from Weka Evaluation stage
    public String results;

    /**
     * Function to generate formatted string suitable for printing.
     *
     * @return - String formatted for printing
     */
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("Test Data Path: " + this.testDataPath + "\n");
        output.append(results);
        return output.toString();
    }
}