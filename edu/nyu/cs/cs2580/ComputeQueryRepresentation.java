package edu.nyu.cs.cs2580;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

public class ComputeQueryRepresentation {

	// That is, your request should have the form,
	// http://<HOST>:<PORT>/prf?query=<QUERY>&ranker=<RANKER-TYPE>&numdocs=<INTEGER>&numterms=<INTEGER>

	// Maps the word to the wordFreq for a particular document
	private static Map<String, Integer> map = new HashMap<String, Integer>();

	public static Map<String, Double> compute(
			Vector<ScoredDocument> scoredDocs, Query query, Indexer indexer,
			int _numTerms) {
		try{
		int wordFreq = 0;
		String currWord = "";
		long totalWordsInSet = 0;

		for (ScoredDocument doc : scoredDocs) {
			int docid = doc.getDocId();
			DocumentIndexed documentIndexed = (DocumentIndexed) (indexer
					.getDoc(docid));
			totalWordsInSet += documentIndexed.getTotalWords();
		}

		// Calculates for every document the frequency of every word in the
		// query and keeps adding frequency of words to the map.
		for (ScoredDocument doc : scoredDocs) {
			int docid = doc.getDocId();
			DocumentIndexed documentIndexed = (DocumentIndexed) (indexer
					.getDoc(docid));
			Map<String, Integer> tempWordFrequency = documentIndexed
					.getWordFrequency();

			for (Map.Entry<String, Integer> entry : tempWordFrequency
					.entrySet()) {
				wordFreq = entry.getValue();
				currWord = entry.getKey();
				if (map.containsKey(currWord)) {
					int num = map.get(currWord);
					wordFreq += num;
				}
				map.put(currWord, wordFreq);
			}
		}
		return calculateProbability(totalWordsInSet, query, _numTerms);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Map<String, Double> calculateProbability(
			long totalWordsInDoc, Query query, int m) {

		Map<String, Double> probabilityMap = new HashMap<String, Double>();

		for (Map.Entry<String, Integer> currWord : map.entrySet()) {
			int wordFreq = currWord.getValue();
			double probability = wordFreq / (double) totalWordsInDoc;
			probabilityMap.put(currWord.getKey(), probability);
		}
		map.clear();
		TreeMap<String, Double> finalMapUnnormalized = MapUtil.sortByValue(
				probabilityMap, m);
		probabilityMap.clear();
		TreeMap<String, Double> finalMapNormalized = new TreeMap<String, Double>();
		double renormConstant = 0.0;
		for (Map.Entry<String, Double> entry : finalMapUnnormalized.entrySet()) {
			renormConstant += entry.getValue();
		}
		for (Map.Entry<String, Double> entry : finalMapUnnormalized.entrySet()) {
			finalMapNormalized.put(entry.getKey(), entry.getValue()
					/ renormConstant);
		}

		return finalMapNormalized;
		// return writeToFile(finalMapNormalized, totalProbability, query);
	}

	// private static Map<String, Double> writeToFile(
	// Map<String, Double> probabilityMap, double totalProbability,
	// Query query) {
	//
	// Map<String, Double> finalMap = new HashMap<String, Double>();
	// for (String entry : probabilityMap.keySet()) {
	// Double value = (probabilityMap.get(entry) / totalProbability);
	// finalMap.put(entry, value);
	// }
	//
	// return finalMap;
	// // try {
	// // System.out.println("Query " + query._query);
	// // StringBuilder builder = new StringBuilder(query._query)
	// // .append(".txt");
	// //
	// // BufferedWriter bw = new BufferedWriter(new FileWriter(
	// // builder.toString(), true));
	// // for (String entry : probabilityMap.keySet()) {
	// // bw.write("<" + entry + ">" + "\t");
	// // Double value = (probabilityMap.get(entry) / totalProbability);
	// // bw.write("<" + value + ">");
	// // bw.newLine();
	// // }
	// // bw.close();
	// // } catch (IOException e) {
	// // e.printStackTrace();
	// // }
	// }
}