package com.dwicke.tsat.dataprocess;

import net.seninp.jmotif.sax.TSProcessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class UCRUtils {

	private static final String CR = "\n";

	/**
	 * Reads bunch of series from file. First column treats as a class label.
	 * Rest as a real-valued series.
	 * 
	 * @param fileName
	 *            the input filename.
	 * @return time series read.
	 * @throws IOException
	 *             if error occurs.
	 */
	public static Map<String, List<double[]>> readUCRData(String fileName) throws IOException {

		Map<String, List<double[]>> res = new HashMap<String, List<double[]>>();

		BufferedReader br = new BufferedReader(new FileReader(new File(fileName)));
		String line = "";
		int count = 0;
		while ((line = br.readLine()) != null) {
			if (count == 0) {
				count++;
				continue;
			}
			if (line.trim().length() == 0) {
				continue;
			}
			String[] split = line.trim().split("[\\,\\s]+");

			String label = split[0];
			Double num;
			if (label.equals("?")) {
				num = -1.0;
			} else {
				num = parseValue(label);
			}
			String seriesType = label;
			if (!(Double.isNaN(num))) {
				seriesType = String.valueOf(num.intValue());
			}
			double[] series = new double[split.length - 1];
			for (int i = 1; i < split.length; i++) {
				series[i - 1] = Double.valueOf(split[i].trim());
			}
			
			TSProcessor tsp = new TSProcessor();

			if (!res.containsKey(seriesType)) {
				res.put(seriesType, new ArrayList<>());
			}
			System.err.println("loaded class " + seriesType + " number = " + count++);
			res.get(seriesType).add(series);
		}

		br.close();
		return res;

	}




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


}
