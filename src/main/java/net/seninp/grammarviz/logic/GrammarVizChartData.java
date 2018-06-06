package net.seninp.grammarviz.logic;

import net.seninp.gi.logic.GrammarRuleRecord;
import net.seninp.gi.logic.GrammarRules;
import net.seninp.gi.logic.RuleInterval;
import net.seninp.gi.rulepruner.RulePrunerFactory;
import net.seninp.grammarviz.model.GrammarVizMessage;
import net.seninp.jmotif.sax.NumerosityReductionStrategy;
import net.seninp.jmotif.sax.discord.DiscordRecords;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Observable;
import java.util.Observer;

/**
 * The main data structure used in SAXSequitur. It contains all the information needed for charting
 * and tables.
 * 
 * @author Manfred Lerner, seninp
 * 
 */
public class GrammarVizChartData extends Observable implements Observer {

  /** SAX conversion parameters. */
  protected final boolean slidingWindowOn;
  protected final NumerosityReductionStrategy numerosityReductionStrategy;
  protected final int saxWindowSize;
  protected final int saxAlphabetSize;
  protected final int saxPAASize;


  /** Original data which will be used for the chart. */
  protected final double[] originalTimeSeries;


  /** The grammar rules. */
  private GrammarRules grammarRules;

  /** The discords. */
  protected DiscordRecords discords;


  /** Pruning related vars. */
  private ArrayList<SameLengthMotifs> allClassifiedMotifs;
  private ArrayList<PackedRuleRecord> arrPackedRuleRecords;

  /**
   * Constructor.
   *  @param ts the time series.
   * @param useSlidingWindow
   * @param numerosityReductionStrategy
   * @param windowSize SAX window size.
   * @param paaSize SAX PAA size.
   * @param alphabetSize SAX alphabet size.
   */
  public GrammarVizChartData(double[] ts, boolean useSlidingWindow,
                             NumerosityReductionStrategy numerosityReductionStrategy, int windowSize, int paaSize,
                             int alphabetSize) {


    this.slidingWindowOn = useSlidingWindow;
    this.numerosityReductionStrategy = numerosityReductionStrategy;

    this.originalTimeSeries = ts;

    this.saxWindowSize = windowSize;
    this.saxPAASize = paaSize;
    this.saxAlphabetSize = alphabetSize;
  }

  /**
   * Sets the grammar rules data.
   * 
   * @param rules the grammar rules collection.
   */
  public void setGrammarRules(GrammarRules rules) {
    this.grammarRules = rules;
  }

  /**
   * Get the grammar rules.
   * 
   * @return the grammar rules collection.
   */
  public GrammarRules getGrammarRules() {
    return this.grammarRules;
  }

  /**
   * Get the original, untransformed time series.
   * 
   * @return the original time series
   */
  public double[] getOriginalTimeseries() {
    return originalTimeSeries;
  }

  /**
   * @return SAX window size
   */
  public int getSAXWindowSize() {
    return saxWindowSize;
  }

  /**
   * @return SAX alphabet size
   */
  public int getSAXAlphabetSize() {
    return saxAlphabetSize;
  }

  /**
   * @return SAX PAA size
   */
  public int getSAXPaaSize() {
    return saxPAASize;
  }

  /**
   * Get the collection of transformed rule records.
   * 
   * @return the collection of transformed rules.
   */
  public ArrayList<PackedRuleRecord> getArrPackedRuleRecords() {
    return arrPackedRuleRecords;
  }


  /**
   * converts rules from a foreign alphabet to the internal original SAX alphabet
   * 
   * @param rule the SAX rule in foreign SAX alphabet
   * @return the SAX string in original alphabet, e.g. aabbdd
   */
  public String convert2OriginalSAXAlphabet(char firstForeignAlphabetChar, String rule) {
    String textRule = rule;
    for (int i = 0; i < getSAXAlphabetSize(); i++) {
      char c1 = (char) (firstForeignAlphabetChar + i);
      char c2 = (char) ('a' + i);
      textRule = textRule.replace(c1, c2);
    }
    return textRule;
  }


  /**
   * Recovers start and stop coordinates ofRule's subsequences.
   * 
   * @param ruleIdx The rule index.
   * @return The array of all intervals corresponding to this rule.
   */
  public ArrayList<RuleInterval> getRulePositionsByRuleNum(Integer ruleIdx) {
    GrammarRuleRecord ruleRec = this.grammarRules.getRuleRecord(ruleIdx);
    return ruleRec.getRuleIntervals();
  }

  /**
   * Get the rule-corresponding subsequences from a class.
   * 
   * @param clsIdx the class index.
   * @return the class-associated subsequences.
   */
  public ArrayList<RuleInterval> getSubsequencesPositionsByClassNum(Integer clsIdx) {

    // this will be the result
    ArrayList<RuleInterval> positions = new ArrayList<RuleInterval>();

    // the sub-sequences class container
    SameLengthMotifs thisClass = allClassifiedMotifs.get(clsIdx);

    // Use minimal length to name the file.
    // String fileName = thisClass.getMinMotifLen() + ".txt";
    // The position of those sub-sequences in the original time series.
    // String positionFileName = thisClass.getMinMotifLen() + "Position" + ".txt";

    // String path = "Result" + System.getProperties().getProperty("file.separator") + "data"
    // + System.getProperties().getProperty("file.separator");

    double[] values = this.getOriginalTimeseries();

    XYSeriesCollection data = new XYSeriesCollection();

    for (SAXMotif subSequence : thisClass.getSameLenMotifs()) {
      positions.add(new RuleInterval(subSequence.getPos().startPos, subSequence.getPos().endPos));
    }

    int index = 0;
    for (RuleInterval pos : positions) {
      XYSeries dataset = new XYSeries("Daten" + String.valueOf(index));

      int start = pos.getStart();
      int end = pos.getEnd() - 1;

      int count = 0;
      for (int i = start; (i <= end) && (i < values.length); i++) {
        dataset.add(count++, values[i]);
      }
      data.addSeries(dataset);
      index++;
    }
    // SAXFileIOHelper.writeFileXYSeries(path, fileName, positionFileName, data, positions);

    return positions;
  }

  public int getRulesNumber() {
    return grammarRules.size();
  }


  /**
   * Cleans-up the rules set by classifying the sub-sequences by length and removing the overlapping
   * in the same length range.
   * 
   * Sub-sequences with the length difference within threshold: "thresouldLength" will be classified
   * as a class with the function "classifyMotifs(double)", i.e. 1-100 and 101-205 will be
   * classified as a class when the threshold is 0.1, because the length difference is 5, which is
   * less than the threshold (0.1 * 100 = 10). If two sub-sequences within one class share a common
   * part which is more than the threshold: "thresouldCom", one of them will be removed by the
   * function "removeOverlappingInSimiliar(double)". i.e. 1-100 and 21-120.
   * 
   * @param intraThreshold, the threshold between the same motifs.
   * @param interThreshould, the threshold between the different motifs.
   */
  protected void removeOverlapping(double intraThreshold, double interThreshould) {

    classifyMotifs(intraThreshold);
  }

  /**
   * Classify the motifs based on their length.
   * 
   * It calls "getAllMotifs()" to get all the sub-sequences that were generated by Sequitur rules in
   * ascending order. Then bins all the sub-sequences by length based on the length of the first
   * sub-sequence in each class, that is, the shortest sub-sequence in each class.
   * 
   * @param lengthThreshold the motif length threshold.
   */
  protected void classifyMotifs(double lengthThreshold) {

    // reset vars
    allClassifiedMotifs = new ArrayList<>();

    // down to business
    ArrayList<SAXMotif> allMotifs = getAllMotifs();

    // is this one better?
    int currentIndex = 0;
    for (SAXMotif tmpMotif : allMotifs) {

      currentIndex++;

      if (tmpMotif.isClassified()) {
        // this breaks the loop flow, so it goes to //for (SAXMotif tempMotif : allMotifs) {
        continue;
      }

      SameLengthMotifs tmpSameLengthMotifs = new SameLengthMotifs();
      int tmpMotifLen = tmpMotif.getPos().getEnd() - tmpMotif.getPos().getStart() + 1;
      int minLen = tmpMotifLen;
      int maxLen = tmpMotifLen;

      // TODO: assuming that this motif has not been processed, right?
      ArrayList<SAXMotif> newMotifClass = new ArrayList<SAXMotif>();
      newMotifClass.add(tmpMotif);
      tmpMotif.setClassified(true);

      // TODO: this motif assumed to be the first one of it's class, traverse the rest down
      for (int i = currentIndex; i < allMotifs.size(); i++) {

        SAXMotif anotherMotif = allMotifs.get(i);

        // if the two motifs are similar or not.
        int anotherMotifLen = anotherMotif.getPos().getEnd() - anotherMotif.getPos().getStart() + 1;

        // if they have the similar length.
        if (Math.abs(anotherMotifLen - tmpMotifLen) < (tmpMotifLen * lengthThreshold)) {
          newMotifClass.add(anotherMotif);
          anotherMotif.setClassified(true);
          if (anotherMotifLen > maxLen) {
            maxLen = anotherMotifLen;
          }
          else if (anotherMotifLen < minLen) {
            minLen = anotherMotifLen;
          }
        }
      }

      tmpSameLengthMotifs.setSameLenMotifs(newMotifClass);
      tmpSameLengthMotifs.setMinMotifLen(minLen);
      tmpSameLengthMotifs.setMaxMotifLen(maxLen);
      allClassifiedMotifs.add(tmpSameLengthMotifs);
    }
    // System.out.println();
  }



  /**
   * Stores all the sub-sequences that generated by Sequitur rules into an array list sorted by
   * sub-sequence length in ascending order.
   * 
   * @return the list of all sub-sequences sorted by length in ascending order.
   */
  protected ArrayList<SAXMotif> getAllMotifs() {

    // result
    ArrayList<SAXMotif> allMotifs = new ArrayList<SAXMotif>();

    // iterate over all rules
    for (int i = 0; i < this.getRulesNumber(); i++) {

      // iterate over all segments/motifs/sub-sequences which correspond to the rule
      ArrayList<RuleInterval> arrPos = this.getRulePositionsByRuleNum(i);
      for (RuleInterval saxPos : arrPos) {
        SAXMotif motif = new SAXMotif();
        motif.setPos(saxPos);
        motif.setRuleIndex(i);
        motif.setClassified(false);
        allMotifs.add(motif);
      }

    }

    // ascending order
    Collections.sort(allMotifs);
    return allMotifs;
  }



  /**
   * Performs rules pruning based on their overlap.
   * 
   * @param thresholdLength
   * @param thresholdCom
   */
  public void performRemoveOverlapping(double thresholdLength, double thresholdCom) {

    removeOverlapping(thresholdLength, thresholdCom);

    arrPackedRuleRecords = new ArrayList<>();

    int i = 0;
    for (SameLengthMotifs subsequencesInClass : allClassifiedMotifs) {
      int classIndex = i;
      int subsequencesNumber = subsequencesInClass.getSameLenMotifs().size();
      int minLength = subsequencesInClass.getMinMotifLen();
      int maxLength = subsequencesInClass.getMaxMotifLen();

      PackedRuleRecord packedRuleRecord = new PackedRuleRecord();
      packedRuleRecord.setClassIndex(classIndex);
      packedRuleRecord.setSubsequenceNumber(subsequencesNumber);
      packedRuleRecord.setMinLength(minLength);
      packedRuleRecord.setMaxLength(maxLength);

      arrPackedRuleRecords.add(packedRuleRecord);
      i++;
    }

  }


  /**
   * This computes anomalies.
   * 
   * @throws Exception
   */
  public void findAnomalies() throws Exception {
    GrammarVizAnomalyFinder finder = new GrammarVizAnomalyFinder(this);
    finder.addObserver(this);
    finder.run();
  }

  public DiscordRecords getAnomalies() {
    return this.discords;
  }

  @Override
  public void update(Observable o, Object arg) {
    if (arg instanceof GrammarVizMessage) {
      this.setChanged();
      notifyObservers(arg);
    }
  }

  public boolean isSlidingWindowOn() {
    return this.slidingWindowOn;
  }

  // EXPERIMENTAL FUNCTIONALITY
  //
  //
  //
  //
  //
  public void performRanking() {
    GrammarRules prunedRulesSet = RulePrunerFactory.performPruning(this.originalTimeSeries,
        this.grammarRules);
    this.grammarRules = prunedRulesSet;
  }

}
