package com.dwicke.tsat.model;

/**
 * Created by sdcollins on 3/30/17.
 */
public class GrammarVizConfiguration {
    public static final int EUCLIDEAN_DISTANCE = 0;
    public static final int DTW_DISTANCE = 1;

    private static GrammarVizConfiguration config;

    private int dtwWindow;
    private int distanceMeasure;

    private GrammarVizConfiguration() {
        dtwWindow = 10;
        distanceMeasure = EUCLIDEAN_DISTANCE;
    }

    public static synchronized GrammarVizConfiguration getConfiguration() {
        if (config == null) {
            config = new GrammarVizConfiguration();
        }

        return config;
    }

    public synchronized int getDistanceMeasure() {
        return distanceMeasure;
    }

    public synchronized void setDistanceMeasure(int distanceMeasure) {
        this.distanceMeasure = distanceMeasure;
    }

    public synchronized int getDTWWindow() {
        return dtwWindow;
    }

    public synchronized void setDTWWindow(int dtwWindow) {
        this.dtwWindow = dtwWindow;
    }
}
