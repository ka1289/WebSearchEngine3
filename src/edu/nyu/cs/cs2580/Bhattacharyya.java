package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Bhattacharyya {
	private static String pathToPRF;
	private static String pathToOutput;
	private static HashMap<String, HashMap<String, Double>> queryPairCoeff = new HashMap<String, HashMap<String, Double>>();
	private static HashMap<String, String> queryPath = new HashMap<String, String>();
	private static ArrayList<String> queriesList = new ArrayList<String>();

	private static void prepare(String pathToPRF) throws NumberFormatException,
			IOException {

		String line = "";

		BufferedReader prfFile = new BufferedReader(new FileReader(pathToPRF));
		while ((line = prfFile.readLine()) != null) {
			// file has format query1:prf1.tsv so split it to get query list
			// and get path of each query
			String[] singleLine = line.split(":");
			queriesList.add(singleLine[0]);
			queryPath.put(singleLine[0], singleLine[1]);
		}
		prfFile.close();

		// for every query in queriesList create the coeff map
		for (String query : queriesList) {
			queryPairCoeff.put(query, new HashMap<String, Double>());
			HashMap<String, Double> temp_CoeffMap = queryPairCoeff.get(query);
			BufferedReader br = new BufferedReader(new FileReader(
					queryPath.get(query)));
			String eachLine = "";
			while ((eachLine = br.readLine()) != null) {
				String[] query2 = eachLine.split("\t");
				temp_CoeffMap.put(query2[0], Double.valueOf(query2[1])
						.doubleValue());
			}
			br.close();
		}
		writeToFile();
	}

	private static void writeToFile() throws IOException {
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(
				pathToOutput, true));
		for (int i = 0; i < queriesList.size(); i++) {
			for (int j = i + 1; j < queriesList.size(); j++) {
				String query1 = queriesList.get(i);
				String query2 = queriesList.get(j);
				Set<String> intersection = compare(query1, query2);
				double summand = 0;
				for (String inter : intersection) {
					double probabilityTerm1 = queryPairCoeff.get(query1).get(
							inter);
					double probabilityTerm2 = queryPairCoeff.get(query2).get(
							inter);
					summand = summand
							+ Math.sqrt(probabilityTerm1 * probabilityTerm2);
				}
				bWriter.write(query1 + "\t" + query2 + "\t" + summand);
				bWriter.newLine();
			}
		}
		bWriter.close();

	}

	private static Set<String> compare(String query1, String query2)
			throws IOException {
		String query1String = queryPath.get(query1);
		String query2String = queryPath.get(query2);
		Set<String> q1Set = new HashSet<String>();
		Set<String> q2Set = new HashSet<String>();
		for (int i = 0; i < 2; i++) {
			BufferedReader br;
			if (i == 0) {
				br = new BufferedReader(new FileReader(query1String));
				String line = "";
				while ((line = br.readLine()) != null) {
					q1Set.add(line.split("\t")[0]);
				}
			} else {
				br = new BufferedReader(new FileReader(query2String));
				String line = "";
				while ((line = br.readLine()) != null) {
					q2Set.add(line.split("\t")[0]);
				}
			}
			br.close();
		}
		q1Set.retainAll(q2Set);
		return q1Set;
	}

	public static void main(String[] args) {
		try {
			if (args.length < 2) {
				throw new IllegalArgumentException(
						"Usage: java -cp src edu.nyu.cs.cs2580.Bhattacharyya <Input-File> <Output-File>");
			} else {
				pathToPRF = args[0];
				pathToOutput = args[1];
				prepare(pathToPRF);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
}