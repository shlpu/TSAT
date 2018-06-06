package com.dwicke.tsat.dataprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GVToRPM {

    /**
     * Converts a string to a double if it is possible, returning Not a Number (NaN) if it fails.
     *
     * @param string - A string that should hold a decimal number only
     * @return - The value from the string or NaN
     */
    private static Double parseValue(String string) {
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
     * Converts the time series data from the data structures used by
     * GrammarViz (2D double array plus an Array of strings) to that used by RPM (A HashMap with labels as keys and
     * a List of doubles arrays of time series).
     *
     * @param data - 2D double array of time series data
     * @param labels - Array of labels, there must be a label for every array of times series data
     * @return - A Map from the labels to a List of double arrays
     */
    public static Map<String, List<double[]>> convertGrammarVizData(double[][] data, String[] labels) {
        if(data.length != labels.length) {
            throw new NullPointerException("The number of classes (" +
                    data.length + ") and the number of labels (" +
                    labels.length + ") did not match");
        }

        Map<String, List<double[]>> res = new HashMap<>();

        for(int i = 0; i < labels.length; i++) {

            Double num = parseValue(labels[i]);
            String seriesType = labels[i];
            if (!(Double.isNaN(num))) {
                seriesType = String.valueOf(num.intValue());
            }

            double[] series = data[i];

            if (!res.containsKey(seriesType)) {
                res.put(seriesType, new ArrayList<>());
            }

            res.get(seriesType).add(series);
        }

        return res;
    }
}
