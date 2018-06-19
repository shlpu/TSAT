package com.dwicke.tsat.view;

import com.dwicke.tsat.model.UserSession;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Implements the parameter panel for GrammarViz.
 * 
 * @author psenin
 * 
 */
public class GrammarvizGuesserPane extends JPanel {

  private static final long serialVersionUID = -941188995659753923L;

  // The labels
  //
  private static final JLabel SAMPLING_INTERVAL_LABEL = new JLabel("Sampling interval range:");
  private static final JLabel MINIMAL_COVER_LABEL = new JLabel("Minimal cover threshold:");
  private static final JLabel WINDOW_BOUND_LABEL = new JLabel("Window range and step:");
  private static final JLabel PAA_BOUND_LABEL = new JLabel("PAA range and step:");
  private static final JLabel ALPHABET_BOUND_LABEL = new JLabel("Alphabet range and step:");

  // and their UI widgets
  //
  private static final JTextField intervalStartField = new JFormattedTextField(
      integerNumberFormatter());
  private static final JTextField intervalEndField = new JFormattedTextField(
      integerNumberFormatter());

  private static final JTextField minimalCoverField = new JFormattedTextField(
      new DecimalFormat("0.00"));

  private static final JTextField windowMinField = new JFormattedTextField(
      integerNumberFormatter());
  private static final JTextField windowMaxField = new JFormattedTextField(
      integerNumberFormatter());
  private static final JTextField windowIncrement = new JFormattedTextField(
      integerNumberFormatter());

  private static final JTextField paaMinField = new JFormattedTextField(integerNumberFormatter());
  private static final JTextField paaMaxField = new JFormattedTextField(integerNumberFormatter());
  private static final JTextField paaIncrement = new JFormattedTextField(integerNumberFormatter());

  private static final JTextField alphabetMinField = new JFormattedTextField(
      integerNumberFormatter());
  private static final JTextField alphabetMaxField = new JFormattedTextField(
      integerNumberFormatter());
  private static final JTextField alphabetIncrement = new JFormattedTextField(
      integerNumberFormatter());

  /**
   * Constructor.
   * 
   * @param userSession
   */
  public GrammarvizGuesserPane(UserSession userSession) {

    super(new MigLayout("fill", "[][fill,grow][fill,grow][fill,grow]", "[grow]"));

    this.add(SAMPLING_INTERVAL_LABEL, "span 2");
    this.add(intervalStartField);
    this.add(intervalEndField, "wrap");

    this.add(MINIMAL_COVER_LABEL, "span 2");
    this.add(minimalCoverField, "wrap");

    this.add(WINDOW_BOUND_LABEL);
    this.add(windowMinField);
    this.add(windowMaxField);
    this.add(windowIncrement, "wrap");

    this.add(PAA_BOUND_LABEL);
    this.add(paaMinField);
    this.add(paaMaxField);
    this.add(paaIncrement, "wrap");

    this.add(ALPHABET_BOUND_LABEL);
    this.add(alphabetMinField);
    this.add(alphabetMaxField);
    this.add(alphabetIncrement, "wrap");

    setValues(userSession);

  }

  private void setValues(UserSession userSession) {

    intervalStartField.setText(Integer.valueOf(userSession.samplingStart).toString());
    intervalEndField.setText(Integer.valueOf(userSession.samplingEnd).toString());

    minimalCoverField.setText(Double.valueOf(userSession.minimalCoverThreshold).toString());

    windowMinField.setText(Integer.valueOf(userSession.boundaries[0]).toString());
    windowMaxField.setText(Integer.valueOf(userSession.boundaries[1]).toString());
    windowIncrement.setText(Integer.valueOf(userSession.boundaries[2]).toString());

    paaMinField.setText(Integer.valueOf(userSession.boundaries[3]).toString());
    paaMaxField.setText(Integer.valueOf(userSession.boundaries[4]).toString());
    paaIncrement.setText(Integer.valueOf(userSession.boundaries[5]).toString());

    alphabetMinField.setText(Integer.valueOf(userSession.boundaries[6]).toString());
    alphabetMaxField.setText(Integer.valueOf(userSession.boundaries[7]).toString());
    alphabetIncrement.setText(Integer.valueOf(userSession.boundaries[8]).toString());

  }

  public void updateSession(UserSession userSession) {

    userSession.samplingStart = Integer.valueOf(intervalStartField.getText());
    userSession.samplingEnd = Integer.valueOf(intervalEndField.getText());

    userSession.minimalCoverThreshold = Double.valueOf(minimalCoverField.getText());

    userSession.boundaries[0] = Integer.valueOf(windowMinField.getText());
    userSession.boundaries[1] = Integer.valueOf(windowMaxField.getText());
    userSession.boundaries[2] = Integer.valueOf(windowIncrement.getText());

    userSession.boundaries[3] = Integer.valueOf(paaMinField.getText());
    userSession.boundaries[4] = Integer.valueOf(paaMaxField.getText());
    userSession.boundaries[5] = Integer.valueOf(paaIncrement.getText());

    userSession.boundaries[6] = Integer.valueOf(alphabetMinField.getText());
    userSession.boundaries[7] = Integer.valueOf(alphabetMaxField.getText());
    userSession.boundaries[8] = Integer.valueOf(alphabetIncrement.getText());

  }

  /**
   * Provides a convenient integer formatter.
   * 
   * @return a formatter instance.
   */
  private static NumberFormatter integerNumberFormatter() {
    NumberFormat format = NumberFormat.getInstance();
    NumberFormatter formatter = new NumberFormatter(format);
    formatter.setValueClass(Integer.class);
    formatter.setMinimum(0);
    formatter.setMaximum(Integer.MAX_VALUE);
    // If you want the value to be committed on each keystroke instead of focus lost
    formatter.setCommitsOnValidEdit(true);
    return formatter;
  }

}
