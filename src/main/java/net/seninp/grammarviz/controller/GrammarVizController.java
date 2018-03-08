package net.seninp.grammarviz.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.logging.Level;
import java.util.zip.DataFormatException;
import javax.swing.*;

import net.seninp.grammarviz.model.GrammarVizMessage;
import net.seninp.grammarviz.model.GrammarVizModel;
import net.seninp.grammarviz.session.UserSession;

/**
 * Implements the Controler component for GrammarViz2 GUI MVC.
 * 
 * @author psenin
 * 
 */
public class GrammarVizController extends Observable implements ActionListener {

  private GrammarVizModel model;

  private UserSession session;

  /**
   * Constructor.
   * 
   * @param model the program's model.
   */
  public GrammarVizController(GrammarVizModel model) {
    super();
    this.model = model;
    this.session = new UserSession();
  }

  /**
   * Implements a listener for the "Browse" button at GUI; opens FileChooser and so on.
   * 
   * @return the action listener.
   */
  public ActionListener getBrowseFilesListener() {

    ActionListener selectDataActionListener = new ActionListener() {

      public void actionPerformed(ActionEvent e) {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Data File");

        String filename = model.getDataFileName();
        if (!((null == filename) || filename.isEmpty())) {
          fileChooser.setSelectedFile(new File(filename));
        }

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
          File file = fileChooser.getSelectedFile();

          // here it calls to model -informing about the selected file.
          //
          model.setDataSource(file.getAbsolutePath());
        }
      }

    };
    return selectDataActionListener;
  }

  public ActionListener getLoadFileListener() {
    ActionListener loadDataActionListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        model.loadData(e.getActionCommand());
      }
    };
    return loadDataActionListener;
  }

  /**
   * This provide Process action listener. Gets all the parameters from the session component
   * 
   * @return
   */
  public ActionListener getProcessDataListener() {

    ActionListener loadDataActionListener = new ActionListener() {
      public void actionPerformed(ActionEvent event) {

        StringBuffer logSB = new StringBuffer("controller: running inference with settings:");

        logSB.append(" SAX window: ").append(session.useSlidingWindow);
        logSB.append(", SAX paa: ").append(session.useSlidingWindow);
        logSB.append(", SAX alphabet: ").append(session.useSlidingWindow);

        logSB.append(", sliding window:").append(session.useSlidingWindow);
        logSB.append(", num.reduction:").append(session.useSlidingWindow);
        logSB.append(", norm.threshold: ").append(session.useSlidingWindow);

        logSB.append(", GI alg: ").append(session.giAlgorithm);

        logSB.append(", grammar filename: ").append(session.useSlidingWindow);

        log(logSB.toString());

        try {
          model.processData(session.giAlgorithm, session.useSlidingWindow, session.useGlobalNormalization,
              session.numerosityReductionStrategy, session.saxWindow, session.saxPAA,
              session.saxAlphabet, session.normalizationThreshold, session.grammarOutputFileName);
        }
        catch (IOException exception) {
          // TODO Auto-generated catch block
          exception.printStackTrace();
        }

      }
    };
    return loadDataActionListener;
  }

  /**
   * Provides an action listener for training an RPM model.
   *
   * @return RPM training action listener.
   */
  public ActionListener getRPMTrainListener() {
    ActionListener RPMTrainListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        model.RPMTrain(session.rpmNumberOfIterations);
      }
    };

    return RPMTrainListener;
  }

  /**
   * Provides an action listener for testing an RPM model.
   *
   * @return RPM testing action listener.
   */
  public ActionListener getRPMTestListener() {
    ActionListener RPMTestListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Test Data File");

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
          File file = fileChooser.getSelectedFile();

          // Run RPM Testing on the selected file
          model.RPMTest(file.getAbsolutePath());
        }
      }
    };

    return RPMTestListener;
  }

  /**
   * Provides an action listener for loading an RPM model.
   *
   * @return RPM loading action listener.
   */
  public ActionListener getRPMLoadListener() {
    ActionListener rpmLoadListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        model.RPMLoadModel();
      }
    };

    return rpmLoadListener;
  }

  /**
   * Provides an action listener for handling the loading of missing training data when loading an RPM model.
   *
   * @return RPM load missing training data action listener.
   */
  public ActionListener getRPMLoadMissingTrainListener() {
    ActionListener rpmLoadMissingTrainListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Missing Training Data, Select Training Data File");

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
          File file = fileChooser.getSelectedFile();
          model.RPMLoadMissingTrain(file.getAbsolutePath());
        }
      }
    };

    return rpmLoadMissingTrainListener;
  }

  /**
   * Provides an action listener for saving an RPM model.
   *
   * @return RPM saving action listener.
   */
  public ActionListener getRPMSaveListener() {
    ActionListener rpmSaveListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Model Save Location");

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
          File file = fileChooser.getSelectedFile();

          // Run RPM Testing on the selected file
          model.RPMSaveModel(file.getAbsolutePath());
        }
      }
    };

    return rpmSaveListener;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    this.setChanged();
    notifyObservers(new GrammarVizMessage(GrammarVizMessage.STATUS_MESSAGE,
        "controller: Unknown action performed " + e.getActionCommand()));
  }

  /**
   * Gets the current session.
   * 
   * @return
   */
  public UserSession getSession() {
    return this.session;
  }

  /**
   * Performs logging messages distribution.
   * 
   * @param message the message to log.
   */
  private void log(String message) {
    this.setChanged();
    notifyObservers(
        new GrammarVizMessage(GrammarVizMessage.STATUS_MESSAGE, "controller: " + message));
  }
}
