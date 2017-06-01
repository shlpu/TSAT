package edu.gmu.grammar.test;

import edu.gmu.grammar.classification.util.*;

import java.io.IOException;

/**
 * A test class for running RPM with out a UI.
 */
public class TestPS{

	/**
	 * Tests RPM
	 *
	 * @param args - N/A.
	 */
	public static void main(String[] args) {

		try {
			PSDirectTransformAllClass testing = new PSDirectTransformAllClass();
			//RPMTrainedData output = testing.RPMTrain("data/CBF/CBF_TRAIN");
			//ClassificationResults testoutput =  testing.RPMTestData("data/CBF/CBF_TEST");
			//RPMTrainedData output = PSDirectTransformAllClass.RPMTrain("TOR", "data/TOR/TOR_TRAIN_ONE_EIGHTY");
			//PSDirectTransformAllClass.RPMTestData("TOR", "data/TOR/TOR_TEST_ONE_EIGHTY");
			//RPMTrainedData output = PSDirectTransformAllClass.RPMTrain("CAMERA-DATA", "data/camera-data/camera-to-basestation-rpm");
			//PSDirectTransformAllClass.RPMTestData("CAMERA-DATA", "data/camera-data/camera-to-basestation-rpm-test");
			//System.out.println(output);
			//System.out.println(testoutput);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
