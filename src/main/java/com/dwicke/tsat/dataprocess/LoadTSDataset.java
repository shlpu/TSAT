package com.dwicke.tsat.dataprocess;

import com.dwicke.tsat.cli.RPM.TSATRPM;
import net.seninp.util.StackTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.server.RMIClassLoader;
import java.util.*;
import java.util.zip.DataFormatException;

public class LoadTSDataset {

    public static final int singleTS = 0;
    public static final int colRPM = 1;
    public static final int rowRPM = 2;
    public static final int ARFF = 3;
    public static final int JSON = 4;

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadTSDataset.class);

    final static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


    private static int getFormat(String fileName) {

        // first check the filetype arff and json
        if (fileName.contains("arff")) {
            return ARFF;
        } else if (fileName.contains("json")) {
            return JSON;
        }


        int formatStyle = singleTS;
        Path path = Paths.get(fileName);

        // lets go
        try {
            // open the reader
            BufferedReader reader = Files.newBufferedReader(path, DEFAULT_CHARSET);

            String line = reader.readLine();
            String[] lineSplit = line.trim().split("\\s+");
            if (lineSplit[0].compareTo("#") == 0) {
                if (lineSplit.length == 1) {
                    formatStyle = rowRPM;
                } else {
                    formatStyle = colRPM;
                }
            }
            reader.close();
        }catch(IOException e) {

        }
        return formatStyle;
    }




    public static Object[] loadData(String limitStr, String fileName, boolean isTestDataset) {
        // check if everything is ready
        if ((null == fileName) || fileName.isEmpty()) {
            log("unable to load data - no data source selected yet");
            return null;
        }


        // make sure the path exists
        Path path = Paths.get(fileName);
        if (!(Files.exists(path))) {
            log("file " + fileName + " doesn't exist.");
            return null;
        }


        int formatStyle = getFormat(fileName);

        if (formatStyle == singleTS || formatStyle == colRPM) {
            return new Object[]{formatStyle, loadDataColumnWise(limitStr,fileName, isTestDataset)};
        }else if (formatStyle == rowRPM) {
            return new Object[]{formatStyle, loadRowTS(fileName, isTestDataset)};
        }
        else if (formatStyle == ARFF) {
            return new Object[] {formatStyle, loadARFF(fileName, isTestDataset)};
        }

        return null;
    }



    public static Object[] loadARFF(String fileName, boolean isTestDataset) {
        Path path = Paths.get(fileName);

        try {
            BufferedReader reader =
                    new BufferedReader(new FileReader(path.toString()));
            ArffLoader.ArffReader arff = new ArffLoader.ArffReader(reader);
            Instances data = arff.getData();
            data.setClassIndex(data.numAttributes() - 1);

            double dataset[][] = new double[data.numInstances()][];
            String[] RPMLabels = new String[data.numInstances()];

            int i = 0;
            for(Instance instance : data) {
                double[] original = instance.toDoubleArray();
                dataset[i] = Arrays.copyOf(original, original.length-1);
                RPMLabels[i] = instance.toString(instance.classIndex());
                i++;
            }

            return new Object[] {dataset, RPMLabels};

        } catch(IOException e) {
            return null;
        }
    }

    public static Object[] loadRowTS(String fileName, boolean isTestDataset) {
        try {
            Map<String, List<double[]>> data =  UCRUtils.readUCRData(fileName);
            int numEntries = 0;
            for (Map.Entry<String, List<double[]>> en : data.entrySet())
            {
                numEntries += en.getValue().size();
            }

            if (!isTestDataset && data.keySet().size() == 1 && !data.keySet().toArray(new String[data.keySet().size()])[0].equals("-1")) {
                throw new DataFormatException("There needs to be more than one example for each class during training");
            }
            double dataset[][] = new double[numEntries][];
            String[] RPMLabels = new String[numEntries];
            int index = 0;
            System.err.println("creating the dataset");
            for (Map.Entry<String, List<double[]>> en : data.entrySet())
            {
                for (double[] lis : en.getValue()) {
                    RPMLabels[index] = en.getKey();
                    dataset[index] = lis;
                    index++;
                }
            }

            return new Object[] {dataset, RPMLabels};

        }catch(Exception e) {
            String stackTrace = StackTrace.toString(e);
            log("error while trying to read data from " + fileName + ":\n " + e.getMessage() + " \n" + stackTrace);

            return null;
        }
    }

    public static Object[] loadDataColumnWise(String limitStr, String fileName, boolean isTestDataset) {
        if ((null == fileName) || fileName.isEmpty()) {
            log("unable to load data - no data source selected yet");
            return null;
        }

        // make sure the path exists
        Path path = Paths.get(fileName);
        if (!(Files.exists(path))) {
            log("file " + fileName + " doesn't exist.");
            return null;
        }

        // read the input
        //
        // init the data araay
        ArrayList<ArrayList<Double>> data = new ArrayList<>();
        String[] RPMLabels = null;
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
            String line;
            long lineCounter = 0;

            while ((line = reader.readLine()) != null) {

                String[] lineSplit = line.trim().split("\\s+");

                if(0 == lineCounter)
                {
                    if(lineSplit[0].compareTo("#") == 0) {
                        log("Found RPM Data");
                        ArrayList<String> labels = new ArrayList<String>();

                        for(int i = 1; i < lineSplit.length; i++) {
                            labels.add(lineSplit[i]);
                        }

                        if (!isTestDataset) {
                            HashSet<String> setlabels = new HashSet<>(labels);

                            if (setlabels.size() == 1) {
                                throw new DataFormatException("There needs to be more than one class");
                            }
                            for (String curlabel : setlabels) {
                                if (Collections.frequency(labels, curlabel) == 1) {
                                    throw new DataFormatException("There needs to be more than one example for each class during training");
                                }
                            }
                        }
                        RPMLabels = labels.toArray(new String[labels.size()]);
                        continue;
                    }
                    data = new ArrayList<>();
                    for (int i = 0; i < lineSplit.length; i++) {
                        data.add(new ArrayList<>());
                    }
                }

                if (lineSplit.length < data.size()) {
                    log("line " + (lineCounter+1) + " of file " + fileName + " contains too few data points.");
                }

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
            String stackTrace = StackTrace.toString(e);
            log("error while trying to read data from " + fileName + ":\n" + stackTrace);
            return null;
        }

        double[][] output = null;
        // convert to simple doubles array and clean the variable
        if (!(data.isEmpty())) {
            output = new double[data.size()][data.get(0).size()];

            for (int i = 0; i < data.size(); i++) {
                for (int j = 0; j < data.get(0).size(); j++) {
                    output[i][j] = data.get(i).get(j);
                }
            }
        }

        return new Object[]{output, RPMLabels};
    }




    /**
     * Logging function.
     *
     * @param message the message to be logged.
     */
    public static void log(String message) {
        LOGGER.debug(message);
    }
}
