package com.dwicke.tsat;

import com.dwicke.tsat.model.GrammarVizController;
import com.dwicke.tsat.model.GrammarVizModel;
import com.dwicke.tsat.view.GrammarVizView;

import java.util.Locale;

/**
 * Main runnable of Sequitur GUI.
 * 
 * @author psenin
 * 
 */
public class TSATGUI {

  /** The model instance. */
  private static GrammarVizModel model;

  /** The controller instance. */
  private static GrammarVizController controller;

  /** The view instance. */
  private static GrammarVizView view;

  /**
   * Runnable GIU.
   * 
   * @param args None used.
   */
  public static void main(String[] args) {

    System.out.println("Starting the Time Series Analysis Tool (TSAT) ...");

    /** Boilerplate */
    // the locale setup
    Locale defaultLocale = Locale.getDefault();
    Locale newLocale = Locale.US;
    System.out.println(
        "Changing runtime locale setting from " + defaultLocale + " to " + newLocale + " ...");
    Locale.setDefault(newLocale);

    // this is the Apple UI fix
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", "SAXSequitur");

    /** On the stage. */
    // model...
    model = new GrammarVizModel();

    // controller...
    controller = new GrammarVizController(model);

    // view...
    view = new GrammarVizView(controller);

    // make sure these two met...
    model.addObserver(view);
    controller.addObserver(view);

    // live!!!
    view.showGUI();

  }

}
