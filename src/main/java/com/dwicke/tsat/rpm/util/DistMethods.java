package com.dwicke.tsat.rpm.util;

import java.util.Random;

public class DistMethods {

	/**
	 * Calculating the distance between time series and pattern using euclidean.
	 *
	 * @param ts - a series of points for time series.
	 * @param p  - a series of points for pattern.
	 * @return - the euclidean distance between ts and p
	 */
	public static double calcDistEuclidean(double[] ts, double[] p) {
		if (ts.length - p.length < 0) {
			return Double.POSITIVE_INFINITY;
		}

		int lastStart = ts.length - p.length;
		int randStart = new Random().nextInt(lastStart + 1);
		double best = euclideanDistNorm(ts, p, randStart);

		for (int i = 0; i < lastStart; i++) {
			best = euclideanDistNorm(ts, p, i, best);
		}

		return best;
	}

	/**
	 * Calculates the euclidean distance normal for ts and p with a starting position w.
	 *
	 * @param ts - a series of points for time series.
	 * @param p  - a series of points for pattern.
	 * @param w  - a starting position.
	 * @return - the normal distance measure.
	 */
	private static double euclideanDistNorm(double[] ts, double[] p, int w) {
		return euclideanDistNorm(ts, p, w, Double.POSITIVE_INFINITY);
	}

	/**
	 * Calculates the euclidean distance normal for ts and p with a starting position start.
	 *
	 * @param ts    - a series of points for time series.
	 * @param p     - a series of points for pattern.
	 * @param start - a starting position.
	 * @param best  - the current best distance
	 * @return - the normal distance measure.
	 */
	private static double euclideanDistNorm(double[] ts, double[] p, int start, double best) {
		double bestDist = Math.pow(best * p.length, 2);
		double dist = 0;

		for (int i = 0; i < p.length; i++) {
			dist += Math.pow(ts[start + i] - p[i], 2);
			if (dist > bestDist) {
				return best;
			}
		}

		return Math.sqrt(dist) / p.length;
	}

	/**
	 * Calculated the distance between time series and pattern using Dynamic Time Warping.
	 *
	 * @param ts     - a series of points for time series.
	 * @param p      - a series of points for patter
	 * @param window - a window size to be used in DTW, reduces the complexity.
	 * @return - the DTW distance between ts and p.
	 */
	public static double calcDistDTW(double[] ts, double[] p, int window) {
		if (ts.length - p.length < 0) {
			return Double.POSITIVE_INFINITY;
		}

		int lastStart = ts.length - p.length;
		int randStart = new Random().nextInt(lastStart + 1);
		double best = dtwDistNorm(ts, p, randStart, window);

		for (int i = 0; i < lastStart; i++) {
			best = dtwDistNorm(ts, p, i, best, window);
		}

		return best;
	}

	/**
	 * Calculates the DTW distance normal for ts and p with a starting position start.
	 *
	 * @param ts     - a series of points for time series.
	 * @param p      - a series of points for pattern.
	 * @param start  - a starting position.
	 * @param window - a window size to be used in DTW, reduces the complexity.
	 * @return - the normal distance measure.
	 */
	private static double dtwDistNorm(double[] ts, double[] p, int start, int window) {
		return dtwDistNorm(ts, p, start, Double.POSITIVE_INFINITY, window);
	}

	/**
	 * Calculates the DTW distance normal for ts and p with a starting position start.
	 *
	 * Dynamic Time Warping algorithm:
	 * DTW := array [0..n, 0..m]
	 *
	 * w := max(w, abs(n-m)) // adapt window size (*)
	 *
	 * for i := 0 to n
	 *  for j:= 0 to m
	 *      DTW[i, j] := infinity
	 * DTW[0, 0] := 0
	 *
	 * for i := 1 to n
	 *  for j := max(1, i-w) to min(m, i+w)
	 *      cost := d(s[i], t[j])
	 *      DTW[i, j] := cost + minimum(DTW[i-1, j  ],    // insertion
	 *                                  DTW[i  , j-1],    // deletion
	 *                                  DTW[i-1, j-1])    // match
	 *
	 * return DTW[n, m]
	 *
	 * @param ts     - a series of points for time series.
	 * @param p      - a series of points for pattern.
	 * @param start  - a starting position.
	 * @param best   - the current best distance
	 * @param window - a window size to be used in DTW, reduces the complexity.
	 * @return - the normal distance measure.
	 */
	private static double dtwDistNorm(double[] ts, double[] p, int start, double best, int window) {
		int n = p.length;
		int w = (int) Math.round(window / 100.0 * n);
		double bestDist = Math.pow(best * n, 2);

		double[][] dtw = new double[n + 1][n + 1];
		for (int i = 0; i <= n; i++) {
			for (int j = 0; j <= n; j++) {
				dtw[i][j] = Double.POSITIVE_INFINITY;
			}
		}
		dtw[0][0] = 0;

		for (int i = 1; i <= n; i++) {
			int jMin = (i - w > 1) ? i - w : 1;
			int jMax = (i + w < n) ? i + w : n;

			for (int j = jMin; j <= jMax; j++) {
				double dist = Math.pow(p[i - 1] - ts[start + j - 1], 2);
				double min = dtw[i - 1][j - 1];

				if (min > dtw[i - 1][j]) {
					min = dtw[i - 1][j];
				} else if (min > dtw[i][j - 1]) {
					min = dtw[i][j - 1];
				}

				dtw[i][j] = dist + min;
				if (dtw[i][j] > bestDist) {
					return best;
				}
			}
		}

		return Math.sqrt(dtw[n][n]) / n;
	}
}