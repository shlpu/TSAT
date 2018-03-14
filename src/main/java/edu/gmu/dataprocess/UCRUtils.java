package edu.gmu.dataprocess;

import net.seninp.jmotif.sax.TSProcessor;

import java.io.*;
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
			if (line.trim().length() == 0) {
				continue;
			}
			String[] split = line.trim().split("[\\,\\s]+");

			String label = split[0];
			Double num = parseValue(label);
			String seriesType = label;
			if (!(Double.isNaN(num))) {
				seriesType = String.valueOf(num.intValue());
			}
			double[] series = new double[split.length - 1];
			for (int i = 1; i < split.length; i++) {
				series[i - 1] = Double.valueOf(split[i].trim()).doubleValue();
			}
			
			TSProcessor tsp = new TSProcessor();
			double max = tsp.max(series);
			double min = tsp.min(series);
			for (int i = 0; i < series.length; i++) {
				series[i] = (max - series[i]) / (max - min);
			}
//			series = tsp.znorm(series, 0.05);

			if (!res.containsKey(seriesType)) {
				res.put(seriesType, new ArrayList<double[]>());
			}
			System.err.println("loaded class " + seriesType + " number = " + count++);
			res.get(seriesType).add(series);
		}

	    //res = refineClassLabel(res);
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

	/**
	 * Goes through the given labeled time series data and relabels the data with a increasing numeric value.
	 *
	 * @param res - The data to be relabeled.
	 * @return - The relabeled data.
	 */
	private static Map<String, List<double[]>> refineClassLabel(Map<String, List<double[]>> res) {
		Set<String> keys = res.keySet();
		Map<String, List<double[]>> newRes = new HashMap<String, List<double[]>>();

		HashMap<String, String> replaceMap = new HashMap<String, String>();

		int count = 1;
		for (String k : keys) {
			String newLabel = String.valueOf(count);
			replaceMap.put(k, newLabel);
			count++;
		}
		for (Entry<String, List<double[]>> e : res.entrySet()) {
			String label = (String) e.getKey();

			String newLabel = (String) replaceMap.get(label);
			newRes.put(newLabel, (List<double[]>) e.getValue());
		}
		return newRes;
	}

}
