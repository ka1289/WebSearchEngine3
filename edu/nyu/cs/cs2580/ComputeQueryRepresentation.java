package edu.nyu.cs.cs2580;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ComputeQueryRepresentation {

	// That is, your request should have the form,
	// http://<HOST>:<PORT>/prf?query=<QUERY>&ranker=<RANKER-TYPE>&numdocs=<INTEGER>&numterms=<INTEGER>

	// Maps the word to the wordFreq for a particular document
	private static Map<String, Integer> map = new HashMap<String, Integer>();

	public static void compute(Vector<ScoredDocument> scoredDocs, Query query,
			Indexer indexer, int _numTerms) {

		int wordFreq = 0;
		String currWord = "";
		long totalWordsInSet = 0;

		// Calculates for every document the frequency of every word in the
		// query and keeps adding frequency of words to the map.
		for (ScoredDocument doc : scoredDocs) {
			String docInfo = doc.asTextResult();
			String[] docInfoArray = docInfo.split("\t");
			String docid = docInfoArray[0];

			// Total no of words in a particular document
			DocumentIndexed documentIndexed = (DocumentIndexed) (indexer
					.getDoc(Integer.parseInt(docid)));
			Map<String, Integer> tempWordFrequency = documentIndexed
					.getWordFrequency();

			Map<String, Integer> mTerms = MapUtil.sortByValue(
					tempWordFrequency, _numTerms);
			long totalWordsInDoc = documentIndexed.getTotalWords();
			totalWordsInSet += totalWordsInDoc;
			for (Map.Entry<String, Integer> entry : mTerms.entrySet()) {
				wordFreq = entry.getValue();
				currWord = entry.getKey();
				if (map.containsKey(currWord)) {
					int num = map.get(currWord);
					wordFreq += num;
				}
				map.put(currWord, wordFreq);
			}
		}
		calculateProbability(totalWordsInSet, query);
	}

	private static void calculateProbability(long totalWordsInDoc, Query query) {
		
		Map<String, Double> probabilityMap = new HashMap<String, Double>();
		double totalProbability = 0;
		
		for (Map.Entry<String, Integer> currWord : map.entrySet()) {
			int wordFreq = currWord.getValue();
			double probability = wordFreq / (double) totalWordsInDoc;
			probabilityMap.put(currWord.getKey(), probability);
			totalProbability += probability;
		}
		
		map.clear();
		writeToFile(probabilityMap, totalProbability, query);
	}

	private static void writeToFile(Map<String, Double> probabilityMap,
			double totalProbability, Query query) {
		try {
			System.out.println(query._query);
			StringBuilder builder = new StringBuilder(query._query)
					.append(".txt");

			BufferedWriter bw = new BufferedWriter(new FileWriter(
					builder.toString(), true));
			for (String entry : probabilityMap.keySet()) {
				bw.write("<" + entry + ">" + "\t");
				Double value = (probabilityMap.get(entry) / totalProbability);
				bw.write("<" + value + ">");
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}