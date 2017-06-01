package edu.gmu.grammar.classification.util;

import weka.core.DistanceFunction;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.PerformanceStats;
import weka.core.Option;

import java.io.Serializable;

/**
 * Created by David Fleming on 3/2/17.
 * Implements Dynamic Time Warping for use as a Weka distance Function.
 */
public class DTW implements DistanceFunction, Serializable {

    protected double windowSizePercent = 10;
    protected Instances insts;
    protected boolean invertSelection;
    protected java.lang.String AttributeIndices;

    /**
     * Constructor that uses default values, window size percentage is set to 10.
     */
    public DTW() {
        this.invertSelection = false;
    }

    /**
     * Constructor that uses default values except for window size percentage.
     *
     * @param windowSizePercent - the percentage of the time series to use during DTW.
     */
    public DTW(double windowSizePercent) {
        this.windowSizePercent = windowSizePercent;
        this.invertSelection = false;
    }

    /**
     * Dynamic Time Warping algorithm:
     * DTW := array [0..n, 0..m]
     *
     * w := max(w, abs(n-m)) // adapt window size (*)
     *
     * for i := 0 to n
     *  for j:= 0 to m
     *      DTW[i, j] := infinity
     * DTW[0, 0] := 0
     *
     * for i := 1 to n
     *  for j := max(1, i-w) to min(m, i+w)
     *      cost := d(s[i], t[j])
     *      DTW[i, j] := cost + minimum(DTW[i-1, j  ],    // insertion
     *                                  DTW[i  , j-1],    // deletion
     *                                  DTW[i-1, j-1])    // match
     *
     * return DTW[n, m]
     *
     * However to reduce memory usage only two arrays (2 by length(t) + 1), during operation the arrays are flipped
     * and the i - 1 array becomes the the i array, reset to +infinity, and the original i becomes i - 1. This reduces
     * memory usage from O(n^2) to O(2n).
     *
     * @param s - a time series array.
     * @param t - a time series array.
     * @param windowSize - the window size to limit the search to.
     * @return - the distance between s and t.
     */
    public static double DTW(double[] s, double[] t, int windowSize) {
        int window = Math.max(windowSize, Math.abs(s.length - t.length));
        double[][] D = new double[2][t.length + 1];
        java.util.Arrays.fill(D[0], Double.POSITIVE_INFINITY);
        java.util.Arrays.fill(D[1], Double.POSITIVE_INFINITY);
        D[0][0] = 0.0;

        for(int i = 1; i <= s.length; i++) {
            int jStop = Math.min(t.length, i + window);
            for(int j = Math.max(1, i - window); j <= jStop; j++) {
                double cost = Math.abs(s[i - 1] - t[j - 1]); // Distance

                D[1][j] = cost + Math.min(D[0    ][j    ],   // D[i - 1][j    ]
                                 Math.min(D[1    ][j - 1],   // D[i    ][j - 1]
                                          D[0    ][j - 1])); // D[i - 1][j - 1]
            }

            double[] temp = D[0];
            D[0] = D[1];
            D[1] = temp;
            java.util.Arrays.fill(D[1], Double.POSITIVE_INFINITY);
        }

        return D[0][t.length];
    }

    /**
     * Takes to Weka Instances and measures the distance between them using DTW.
     *
     * @param first - a time series instance.
     * @param second - a time series instance.
     * @return - the distance between first and second.
     */
    public double distance(Instance first, Instance second) {
        return DTW.DTW(first.toDoubleArray(), second.toDoubleArray(),
                (int) (Math.max(first.numValues(), second.numValues()) * windowSizePercent));
    }

    /**
     * Takes to Weka Instances and measures the distance between them using DTW.
     *
     * @param first - a time series instance.
     * @param second - a time series instance.
     * @param cutOffValue - N/A.
     * @return - the distance between first and second.
     */
    public double distance(Instance first, Instance second, double cutOffValue) {
        return distance(first, second);
    }

    /**
     * Takes to Weka Instances and measures the distance between them using DTW.
     *
     * @param first - a time series instance.
     * @param second - a time series instance.
     * @param cutOffValue - N/A.
     * @param stats - N/A.
     * @return - the distance between first and second.
     */
    public double distance(Instance first, Instance second, double cutOffValue, PerformanceStats stats) {
        return distance(first, second);
    }

    /**
     * Takes to Weka Instances and measures the distance between them using DTW.
     *
     * @param first - a time series instance.
     * @param second - a time series instance.
     * @param stats - N/A.
     * @return - the distance between first and second.
     */
    public double distance(Instance first, Instance second, PerformanceStats stats) {
        return distance(first, second);
    }

    /**
     * Part of the DistanceMeasure interface, not used.
     *
     * @param distances - N/A.
     */
    public void postProcessDistances(double[] distances) {

    }

    /**
     * Set the attribute value. Part of the DistanceMeasure interface, not used.
     *
     * @param value - the new attribute value.
     */
    public void setAttributeIndices(java.lang.String value) {
        this.AttributeIndices = value;
    }

    /**
     * Get the attribute value. Part of the DistanceMeasure interface, not used.
     *
     * @return - the attribute value.
     */
    public java.lang.String getAttributeIndices() {
        return this.AttributeIndices;
    }

    /**
     * Set the invert selection condition. Part of the DistanceMeasure interface, not used.
     *
     * @param value - the new invert selection condition.
     */
    public void setInvertSelection(boolean value) {
        this.invertSelection = value;
    }

    /**
     * Get the invert selection condition. Part of the DistanceMeasure interface, not used.
     *
     * @return - the invert slection condition.
     */
    public boolean getInvertSelection() {
        return this.invertSelection;
    }

    /**
     * Set the instances. Part of the DistanceMeasure interface, not used.
     *
     * @param insts - the new instances.
     */
    public void setInstances(Instances insts) {
        this.insts = insts;
    }

    /**
     * Get the instances. Part of the DistanceMeasure interface, not used.
     *
     * @return - the instances.
     */
    public Instances getInstances() {
        return this.insts;
    }

    /**
     * Part of the DistanceMeasure interface, not used.
     *
     * @param ins - N/A.
     */
    public void update(Instance ins) {

    }

    /**
     * Clear the instances. Part of the DistanceMeasure interface, not used.
     */
    public void clean() {
        this.insts = null;
    }

    /**
     * Part of the DistanceMeasure interface, not used.
     *
     * @return - N/A.
     */
    public java.lang.String[] getOptions() {
        return null;
    }

    /**
     * Part of the DistanceMeasure interface, not used.
     *
     * @param options - N/A.
     * @throws java.lang.Exception - N/A.
     */
    public void setOptions(java.lang.String[] options)
            throws java.lang.Exception {
    }

    /**
     * Part of the DistanceMeasure interface, not used.
     *
     * @return - N/A
     */
    public java.util.Enumeration<Option> listOptions() {
        return null;
    }

    /**
     * A test function to check for run time errors.
     *
     * @param argv - N/A.
     */
    public static void main(String[] argv) {
        java.util.Random rand = new java.util.Random(1001001);
        int size = 1000;
        double[] s = new double[size];
        double[] t = new double[size];
        for(int i = 0; i < size; i++) {
            s[i] = rand.nextDouble();
            t[i] = rand.nextDouble();
        }

        System.out.println("s: " + s.length +
                " t: " + t.length);
        System.out.println("dtw:" + DTW(s, t, (int) Math.round(s.length * .01)));

    }

}
