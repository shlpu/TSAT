package com.dwicke.tsat.rpm.connectGI;

import com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import com.apporiented.algorithm.clustering.Cluster;
import com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;
import com.dwicke.tsat.rpm.util.DistMethods;
import net.seninp.gi.logic.RuleInterval;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class provides the primary functions for refining patterns found in the time series data. In the RPM papers
 * this is Algorithm 1 Finding repeated patterns.
 */
public class PatternsProcess {

	/**
	 * Refines the patterns using clustering from seninp.
	 * @param origTS the orginial time series where the patterns were found.
	 * @param rp a seninp package for managing repeating patterns.
	 * @param startingPositions the start position for the patterns.
	 * @param isCoverageFre are the patterns coverage free.
	 * @return an ArrayList containing int arrays that store, the start position, the length, and the frequency.
	 */
	public ArrayList<int[]> refinePatternsByClustering(double[] origTS, RepeatedPattern rp, int[] startingPositions,
			Boolean isCoverageFre) {

		ArrayList<RuleInterval> arrPos = rp.getSequences();

		int patternNum = arrPos.size();

		double dt[][] = new double[patternNum][patternNum];
		for (int i = 0; i < patternNum; i++) {
			RuleInterval saxPos = arrPos.get(i);

			int start1 = saxPos.getStart();
			int end1 = saxPos.getEnd();
			double[] ts1 = Arrays.copyOfRange(origTS, start1, end1);

			for (int j = 0; j < arrPos.size(); j++) {
				RuleInterval saxPos2 = arrPos.get(j);
				if (dt[i][j] > 0) {
					continue;
				}
				double d = 0;
				dt[i][j] = d;
				if (i == j) {
					continue;
				}

				int start2 = saxPos2.getStart();
				int end2 = saxPos2.getEnd();
				double[] ts2 = Arrays.copyOfRange(origTS, start2, end2);

				if (ts1.length > ts2.length)
					d = DistMethods.calcDistEuclidean(ts1, ts2);
				else
					d = DistMethods.calcDistEuclidean(ts2, ts1);

				dt[i][j] = d;
			}
		}

		String[] patternsName = new String[patternNum];
		for (int i = 0; i < patternNum; i++) {
			patternsName[i] = String.valueOf(i);
		}

		ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
		Cluster cluster = alg.performClustering(dt, patternsName, new AverageLinkageStrategy());

		int minPatternPerCls = (int) (0.3 * patternNum);
		minPatternPerCls = minPatternPerCls > 0 ? minPatternPerCls : 1;

		if (cluster.getDistance() == null)
			System.out.print(false);

		double cutDist = cluster.getDistanceValue() / 2;
		ArrayList<String[]> clusterTSIdx = findCluster(cluster, cutDist, minPatternPerCls);
		while (clusterTSIdx.size() <= 0) {
			cutDist += cutDist / 2;
			clusterTSIdx = findCluster(cluster, cutDist, minPatternPerCls);
		}

		return getRepresentativeOfGroup(clusterTSIdx, arrPos, dt, startingPositions, isCoverageFre);
	}

	/**
	 * Determines if the rule is in another time series.
	 * @param arrPosThis the Rule Intervals which are all rules found.
	 * @param saxPos The rule interval to look for in the other rules data
	 * @param startingPositions the start position for the patterns in the concatenated time series data.
	 * @return whether the rule matches other time series data.
	 */
	public static Boolean isFromDifferenTS(ArrayList<RuleInterval> arrPosThis, RuleInterval saxPos,
                                           int[] startingPositions) {

		int curSP = saxPos.getStart();
		int curIDX = ConnectGI.findIdx(startingPositions, curSP);
		for (RuleInterval ri : arrPosThis) {
			int sp = ri.getStart();
			int idx = ConnectGI.findIdx(startingPositions, sp);

			if (curIDX == idx)
				return false;
		}

		return true;
	}

	/**
	 * Returns the representative patterns of a group by usering the clustering results and selecting the best patterns.
	 * @param clusterTSIdx the cluster index for the time series.
	 * @param arrPos the rule intervals.
	 * @param dt the distance found during clustering.
	 * @param startingPositions the starting position for the patterns in the concatenated time series data.
	 * @param isCoverageFre is the data coverage free.
	 * @return a list of interger arrays that store, the start position, the length, and the frequency of the best
	 * 				representative patterns for the group.
	 */
	private ArrayList<int[]> getRepresentativeOfGroup(ArrayList<String[]> clusterTSIdx, ArrayList<RuleInterval> arrPos,
			double dt[][], int[] startingPositions, Boolean isCoverageFre) {
		ArrayList<int[]> allPatterns = new ArrayList<int[]>();
		for (String[] idxInCls : clusterTSIdx) {

			int pNum = idxInCls.length;
			int[] idxInClsInt = new int[pNum];
			double dtThis[][] = new double[pNum][pNum];
			ArrayList<RuleInterval> arrPosThis = new ArrayList<RuleInterval>();
			RuleInterval centroid = new RuleInterval();

			int frequency = pNum;
			if (isCoverageFre)
				frequency = 0;

			for (int i = 0; i < pNum; i++) {
				String idxString = idxInCls[i];
				idxInClsInt[i] = Integer.parseInt(idxString);
				RuleInterval saxPos = arrPos.get(idxInClsInt[i]);

				if (isCoverageFre) {
					if (isFromDifferenTS(arrPosThis, saxPos, startingPositions)) {
						frequency++;
					}
				}

				arrPosThis.add(saxPos);
			}

			for (int i = 0; i < pNum; i++) {
				for (int j = 0; j < pNum; j++) {
					dtThis[i][j] = dt[idxInClsInt[i]][idxInClsInt[j]];
				}
			}

			int bestLine = -1;
			double smallestValue = 10000000.00;

			for (int k = 0; k < arrPosThis.size(); k++) {
				double dk = 0;
				for (int m = 0; m < arrPosThis.size(); m++) {
					dk += dtThis[k][m];
				}
				if (smallestValue > dk) {
					smallestValue = dk;
					bestLine = k;
				}
			}

			if (bestLine > -1)
				centroid = arrPosThis.get(bestLine);

			int[] p = { centroid.getStart(), centroid.getLength(), frequency };
			allPatterns.add(p);
		}
		return allPatterns;
	}

	/**
	 * Recursively descends through the cluster pulling in clusters that meet the minimum set by distance and the
	 * minimum number of patterns per cluster.
	 *
	 * @param cluster the cluster to be parsed.
	 * @param cutDist the minimum distance of the cluster.
	 * @param minPatternPerCls the minimum number of patterns in the cluster.
	 * @return reduced pattern cluster set.
	 */
	private ArrayList<String[]> findCluster(Cluster cluster, double cutDist, int minPatternPerCls) {

		ArrayList<String[]> clusterTSIdx = new ArrayList<String[]>();

		if (cluster.getDistance() != null) {
			if (cluster.getDistanceValue() > cutDist) {
				if (cluster.getChildren().size() > 0) {
					clusterTSIdx.addAll(findCluster(cluster.getChildren().get(0), cutDist, minPatternPerCls));
					clusterTSIdx.addAll(findCluster(cluster.getChildren().get(1), cutDist, minPatternPerCls));
				}
			} else {
				ArrayList<String> itemsInCluster = getNameInCluster(cluster);
				String[] idxes = itemsInCluster.toArray(new String[itemsInCluster.size()]);
				if (idxes.length > minPatternPerCls) {
					clusterTSIdx.add(idxes);
				}
			}
		}

		return clusterTSIdx;
	}

	/**
	 * Recursively finds the name of patterns in a cluster by traversing the cluster tree.
	 *
	 * @param cluster the cluster to parse.
	 * @return all the names in the cluster.
	 */
	private ArrayList<String> getNameInCluster(Cluster cluster) {
		ArrayList<String> itemsInCluster = new ArrayList<String>();

		String nodeName;
		if (cluster.isLeaf()) {
			nodeName = cluster.getName();
			itemsInCluster.add(nodeName);
		}

		for (Cluster child : cluster.getChildren()) {
			ArrayList<String> childrenNames = getNameInCluster(child);
			itemsInCluster.addAll(childrenNames);
		}
		return itemsInCluster;
	}

}
