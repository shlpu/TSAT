package com.dwicke.tsat.model;

import com.dwicke.tsat.dataprocess.LoadTSDataset;
import com.dwicke.tsat.logic.GrammarVizChartData;
import com.dwicke.tsat.logic.RPMHandler;
import com.dwicke.tsat.dataprocess.UCRUtils;
import net.seninp.gi.GIAlgorithm;
import net.seninp.gi.logic.GrammarRuleRecord;
import net.seninp.gi.logic.GrammarRules;
import net.seninp.gi.logic.RuleInterval;
import net.seninp.gi.repair.RePairFactory;
import net.seninp.gi.repair.RePairGrammar;
import net.seninp.gi.sequitur.SAXRule;
import net.seninp.gi.sequitur.SequiturFactory;
import net.seninp.jmotif.sax.NumerosityReductionStrategy;
import net.seninp.jmotif.sax.SAXProcessor;
import net.seninp.jmotif.sax.alphabet.NormalAlphabet;
import net.seninp.jmotif.sax.datastructure.SAXRecords;
import net.seninp.jmotif.sax.parallel.ParallelSAXImplementation;
import net.seninp.util.StackTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.DataFormatException;

/**
 * Implements the Sequitur Model component of MVC GUI pattern.
 * 
 * @author psenin
 * 
 */
public class GrammarVizModel extends Observable implements Observer {

  final static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
  private static final String SPACE = " ";
  private static final String CR = "\n";

  /** The data filename. */
  private String dataFileName;

  /** If that data was read - it is stored here. */
  private double[][] ts;

  /** Data structure that keeps the chart data. */
  private GrammarVizChartData chartData;

  /** RPM Object **/
  private RPMHandler rpmHandler;
  private boolean enableRPM = false;
  private String[] RPMLabels;

  // static block - we instantiate the logger
  //
  private static final Logger LOGGER = LoggerFactory.getLogger(GrammarVizModel.class);

  /**
   * The file name getter.
   * 
   * @return current filename.
   */
  public synchronized String getDataFileName() {
    return this.dataFileName;
  }

  /**
   * Set data source filename.
   * 
   * @param filename the filename.
   */
  public synchronized void setDataSource(String filename) {

    LOGGER.info("setting the file " + filename + " as current data source");

    // action
    this.dataFileName = filename;

    // notify the View
    this.setChanged();
    notifyObservers(new GrammarVizMessage(GrammarVizMessage.DATA_FNAME, this.getDataFileName()));

    // this notification tells GUI which file was selected as the data source
    this.log("set file " + filename + " as current data source");

  }



  /**
   * Load the data which is supposedly in the file which is selected as the data source.
   * 
   * @param limitStr the limit of lines to read.
   */
  public synchronized void loadData(String limitStr) {

    Object[] objData = LoadTSDataset.loadData(limitStr, this.dataFileName, false);

    if ((int)objData[0] != LoadTSDataset.singleTS) {
      this.enableRPM = true; // the single time ts is the only one that isn't classification
      this.RPMLabels =  (String []) ((Object[]) objData[1])[1];
    }
    this.ts = (double[][]) ((Object[]) objData[1])[0];




    if(!(this.ts == null)) {
      LOGGER.debug("loaded " + this.ts[0].length + " points....");

      // notify that the process finished
      this.log("loaded " + this.ts[0].length + " points from " + this.dataFileName);

      // and send the timeseries
      setChanged();
      notifyObservers(new GrammarVizMessage(GrammarVizMessage.TIME_SERIES_MESSAGE, this.ts));
      // and if RPM data was found, enable RPM mode
      if(this.enableRPM) {

        setChanged();
        notifyObservers(new GrammarVizMessage(GrammarVizMessage.RPM_DATA_MESSAGE, this.RPMLabels));
      }
    }
  }

  /**
   * converts multiple SAXRecords in to a single one
   *
   */
  private SAXRecords mergeRecords(SAXRecords records[]) {

    ArrayList<Integer> mergedIndices = records[0].getAllIndices();
    ArrayList<char[]> mergedWords = new ArrayList<char[]>();
    for(Integer i : mergedIndices)
      mergedWords.add(records[0].getByIndex(i).getPayload());

    for(int i = 1; i < records.length; i++) {
      ArrayList<Integer> indices1 = mergedIndices;
      ArrayList<char[]> words1 = mergedWords;

      ArrayList<Integer> indices2 = records[i].getAllIndices();
      ArrayList<char[]> words2 = new ArrayList<char[]>();
      for(Integer j : indices2)
        words2.add(records[i].getByIndex(j).getPayload());


      mergedIndices = new ArrayList<Integer>();
      mergedWords = new ArrayList<char[]>();


      mergedIndices.add(0);
      
      // concatenate words
      char[] word1 = words1.get(0);
      char[] word2 = words2.get(0);
      char[] mergedWord = new char[word1.length + word2.length];
      System.arraycopy(word1, 0, mergedWord, 0, word1.length);
      System.arraycopy(word2, 0, mergedWord, word1.length, word2.length);
      mergedWords.add(mergedWord);

      int index1 = 0;
      int index2 = 0;
      while(index1+1<indices1.size() || index2+1<indices2.size()) {
        boolean inc1 = false, inc2 = false;
        if(index2+1>=indices2.size() || (index1+1<indices1.size() && index2+1<indices2.size() && indices1.get(index1+1) <= indices2.get(index2+1))) {
          inc1 = true;
        }
        if(index1+1>=indices1.size() || (index1+1<indices1.size() && index2+1<indices2.size() && indices2.get(index2+1) <= indices1.get(index1+1))) {
          inc2 = true;
        }
        if(inc1) {index1++;}
        if(inc2) {index2++;}

        mergedIndices.add(Math.max(indices1.get(index1),indices2.get(index2)));

        // concatenate words
        word1 = words1.get(index1);
        word2 = words2.get(index2);
        mergedWord = new char[word1.length + word2.length];
        System.arraycopy(word1, 0, mergedWord, 0, word1.length);
        System.arraycopy(word2, 0, mergedWord, word1.length, word2.length);
        mergedWords.add(mergedWord);
      }
    }

    SAXRecords mergedRecord = new SAXRecords();
    HashMap<Integer, char[]> hm = new HashMap<Integer,char[]>();
    for(int i = 0; i < mergedIndices.size(); i++) {
      hm.put(mergedIndices.get(i), mergedWords.get(i));
    }
    mergedRecord.addAll(hm);

    return mergedRecord;
  }

  /**
   * Process data with Sequitur. Populate and broadcast ChartData object.
   * 
   * @param algorithm the algorithm, 0 Sequitur, 1 RE-PAIR.
   * @param useSlidingWindow The use sliding window parameter.
   * @param numerosityReductionStrategy The numerosity reduction strategy.
   * @param windowSize The SAX sliding window size.
   * @param paaSize The SAX PAA size.
   * @param alphabetSize The SAX alphabet size.
   * @param normalizationThreshold The normalization threshold.
   * @param grammarOutputFileName The file name to where save the grammar.
   * @throws IOException
   */
  public synchronized void processData(GIAlgorithm algorithm, boolean useSlidingWindow,
      boolean useGlobalNormalization, NumerosityReductionStrategy numerosityReductionStrategy,
      int windowSize, int paaSize, int alphabetSize, double normalizationThreshold,
      String grammarOutputFileName)
          throws IOException {

    // check if the data is loaded
    //
    if (null == this.ts || null == this.ts[0] || this.ts[0].length == 0) {
      this.log("unable to \"Process data\" - no data were loaded ...");
    }
    else {

      // the logging block
      //
      StringBuffer sb = new StringBuffer("setting up GI with params: ");
      if (GIAlgorithm.SEQUITUR.equals(algorithm)) {
        sb.append("algorithm: Sequitur, ");
      }
      else {
        sb.append("algorithm: RePair, ");
      }
      sb.append("sliding window ").append(useSlidingWindow);
      sb.append(", global normalization ").append(useGlobalNormalization);
      sb.append(", numerosity reduction ").append(numerosityReductionStrategy.toString());
      sb.append(", SAX window ").append(windowSize);
      sb.append(", PAA ").append(paaSize);
      sb.append(", Alphabet ").append(alphabetSize);
      LOGGER.info(sb.toString());
      this.log(sb.toString());

      LOGGER.debug("creating ChartDataStructure");

      // only giving chart data the first time series
      // should probably give it all of the time series
      this.chartData = new GrammarVizChartData(this.ts[0], useSlidingWindow,
          numerosityReductionStrategy, windowSize, paaSize, alphabetSize);

      NormalAlphabet na = new NormalAlphabet();

      try {

        if (GIAlgorithm.SEQUITUR.equals(algorithm)) {

          SAXProcessor sp = new SAXProcessor();


          SAXRecords saxFrequencyDataArray[] = new SAXRecords[this.ts.length];
          for(int i = 0; i < this.ts.length; i++) {
            if (useSlidingWindow) {
              if(useGlobalNormalization) {
                saxFrequencyDataArray[i] = sp.ts2saxViaWindowGlobalZNorm(ts[i], windowSize, paaSize, na.getCuts(alphabetSize),
                    numerosityReductionStrategy, normalizationThreshold);
              } else {
                saxFrequencyDataArray[i] = sp.ts2saxViaWindow(ts[i], windowSize, paaSize, na.getCuts(alphabetSize),
                    numerosityReductionStrategy, normalizationThreshold);
              }
            } else {
              saxFrequencyDataArray[i] = sp.ts2saxByChunking(ts[i], paaSize, na.getCuts(alphabetSize),
                  normalizationThreshold);
            }
          }

          SAXRecords saxFrequencyData = mergeRecords(saxFrequencyDataArray);

          LOGGER.trace("String: " + saxFrequencyData.getSAXString(SPACE));

          LOGGER.debug("running sequitur ...");
          SAXRule sequiturGrammar = SequiturFactory
              .runSequitur(saxFrequencyData.getSAXString(SPACE));

          LOGGER.debug("collecting grammar rules data ...");
          GrammarRules rules = sequiturGrammar.toGrammarRulesData();

          LOGGER.debug("mapping rule intervals on timeseries ...");
          // only use first time series, updateRuleInterval only cares about length (why does it take an array?)
          SequiturFactory.updateRuleIntervals(rules, saxFrequencyData, useSlidingWindow, this.ts[0],
              windowSize, paaSize);

          LOGGER.debug("done ...");
          this.chartData.setGrammarRules(rules);
        }
        else {

          ParallelSAXImplementation ps = new ParallelSAXImplementation();
          SAXRecords parallelRes = ps.process(ts[0], 2, windowSize, paaSize, alphabetSize,
              numerosityReductionStrategy, normalizationThreshold);

          RePairGrammar rePairGrammar = RePairFactory.buildGrammar(parallelRes);

          rePairGrammar.expandRules();
          rePairGrammar.buildIntervals(parallelRes, ts[0], windowSize);

          GrammarRules rules = rePairGrammar.toGrammarRulesData();

          this.chartData.setGrammarRules(rules);

        }

      }
      catch (Exception e) {
        this.log("error while processing data " + StackTrace.toString(e));
        e.printStackTrace();
      }

      this.log("processed data, broadcasting charts");
      LOGGER.info("process finished");

      setChanged();
      notifyObservers(new GrammarVizMessage(GrammarVizMessage.CHART_MESSAGE, this.chartData));
    }
  }

  /**
   * GUI interface for training an RPM model on loaded time series data, runs in the background.
   *
   * @param numberOfIterations the maximum number of iterations RPM will run for.
   */
  public synchronized void RPMTrain(int numberOfIterations) {
    if(this.rpmHandler == null) {
      this.rpmHandler = new RPMHandler();
      this.rpmHandler.addObserver(this);
    }
    this.log("Training...");
    try {
      this.rpmHandler.setNumberOfIterations(numberOfIterations);
      this.rpmHandler.setTrainingFilename(this.getDataFileName());
      this.rpmHandler.setTrainingData(this.ts);
      this.rpmHandler.setTrainingLabels(this.RPMLabels);

      (new Thread(this.rpmHandler)).start();
    } catch (Exception e) {
      this.log("error while training RPM model " + StackTrace.toString(e));
      e.printStackTrace();
    }
  }

  @Override
  public void update(Observable o, Object arg) {
    if (arg instanceof GrammarVizMessage) {
      this.setChanged();
      notifyObservers(arg);
    }
  }

  /**
   * GUI interface for saving RPM models to file.
   *
   * @param filename the path for where the model is to be save.
   */
  public synchronized void RPMSaveModel(String filename) {
    this.log("Testing Model using " + filename + "...");
    try {
        this.rpmHandler.RPMSaveModel(filename);
    } catch (Exception e) {
      this.log("error while saving RPM model " + StackTrace.toString(e));
      e.printStackTrace();
    }
  }

  /**
   * GUI function that loads the training time series data from file when an existing model is loaded from file.
   *
   * @param filename the path to the training time series data.
   * @return true if the file was found and loaded, false otherwise.
   */
  private boolean loadTrainingDataForModel(String filename) throws IOException{
    String tempDataFileName = this.dataFileName;
    this.dataFileName = filename;

    this.loadData("0");

    if(!(this.ts == null)) {
      this.rpmHandler.setTrainingData(this.ts);
      this.rpmHandler.setTrainingLabels(this.RPMLabels);
      this.rpmHandler.forceRPMModelReload();
      this.dataFileName = tempDataFileName;
      return true;
    } else {
      this.dataFileName = tempDataFileName;
      return false;
    }
  }

  /**
   * GUI interface for loading an RPM model from file.
   */
  public synchronized void RPMLoadModel() {
    this.log("Loading Model from " + this.dataFileName + "...");
    if (this.rpmHandler == null) {
      this.rpmHandler = new RPMHandler();
      this.rpmHandler.addObserver(this);
    }
    try {
      this.rpmHandler.RPMLoadModel(this.dataFileName);
      String filename = this.rpmHandler.getTrainingFilename();
      if((new File(filename)).exists()) {

        if(this.loadTrainingDataForModel(filename)) {
          setChanged();
          notifyObservers(new GrammarVizMessage(GrammarVizMessage.RPM_TRAIN_RESULTS_UPDATE_MESSAGE, this.rpmHandler));
        } else {
          this.rpmHandler = null;
        }
      } else {
        setChanged();
        notifyObservers(new GrammarVizMessage(GrammarVizMessage.RPM_MISSING_TRAIN_DATA_UPDATE_MESSAGE, filename));
      }
    } catch (StreamCorruptedException e) {
      setChanged();
      notifyObservers(new GrammarVizMessage(GrammarVizMessage.LOAD_FILE_ERROR_UPDATE_MESSAGE,
              "File was not a RPM Model: " + this.dataFileName));
    } catch (ClassNotFoundException e) {
      setChanged();
      notifyObservers(new GrammarVizMessage(GrammarVizMessage.LOAD_FILE_ERROR_UPDATE_MESSAGE,
              "Could not load a RPM Model from file: " + this.dataFileName));
    } catch (IOException e) {
      setChanged();
      notifyObservers(new GrammarVizMessage(GrammarVizMessage.LOAD_FILE_ERROR_UPDATE_MESSAGE,
              "Error while loading RPM model: " + this.dataFileName + " " + StackTrace.toString(e)));
    } catch (Exception e) {
      this.log("error while loading RPM model " + StackTrace.toString(e));
      e.printStackTrace();
    }
  }

  /**
   * GUI function that loads training time series data when loading a model in the event that the original path
   * found in the model does not point to the data.
   *
   * @param filename the path to the training time series data.
   */
  public synchronized void RPMLoadMissingTrain(String filename) {

    try {
      if (this.loadTrainingDataForModel(filename)) {
        setChanged();
        notifyObservers(new GrammarVizMessage(GrammarVizMessage.RPM_TRAIN_RESULTS_UPDATE_MESSAGE, this.rpmHandler));
      } else {
        this.rpmHandler = null;
      }
    } catch (IOException e) {
      setChanged();
      notifyObservers(new GrammarVizMessage(GrammarVizMessage.LOAD_FILE_ERROR_UPDATE_MESSAGE,
              "Error while loading RPM model: " + this.dataFileName + " " + StackTrace.toString(e)));
    }

  }

  /**
   * GUI interface that tests the trained RPM model on testing time series data.
   *
   * @param filename the path to the testing time series data.
   */
  public synchronized void RPMTest(String filename) {
    this.log("Testing Model using " + filename + "...");
    try {

      Object[] objData = LoadTSDataset.loadData("0", filename, true);
      if ((int)objData[0] != LoadTSDataset.singleTS) {
        this.enableRPM = true; // the single time ts is the only one that isn't classification
        this.RPMLabels =  (String []) ((Object[]) objData[1])[1];
      }
      double[][] testData = (double[][]) ((Object[]) objData[1])[0];




      if(this.enableRPM) {
        this.rpmHandler.RPMTestData(filename, testData, this.RPMLabels);
        setChanged();
        notifyObservers(new GrammarVizMessage(GrammarVizMessage.RPM_CLASS_RESULTS_UPDATE_MESSAGE, this.rpmHandler));
        this.log("Finished testing see tabs labeled RPM Classification and RPM Time Series Results for result info");
        //this.log("RPM Testing Results: " + results.toString());
      } else {
        this.log("Not RPM Data");
      }
    } catch (IOException e) {
      setChanged();
      notifyObservers(new GrammarVizMessage(GrammarVizMessage.LOAD_FILE_ERROR_UPDATE_MESSAGE,
              "Error while loading RPM model: " + this.dataFileName + " " + StackTrace.toString(e)));
    } catch (Exception e) {
      this.log("error while testing RPM model " + StackTrace.toString(e));
      e.printStackTrace();
    }
  }

  /**
   * Performs logging messages distribution.
   * 
   * @param message the message to log.
   */
  private void log(String message) {
    this.setChanged();
    notifyObservers(new GrammarVizMessage(GrammarVizMessage.STATUS_MESSAGE, "model: " + message));
  }



}
