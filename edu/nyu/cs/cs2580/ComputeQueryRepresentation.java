package edu.nyu.cs.cs2580;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ComputeQueryRepresentation {

	// That is, your request should have the form,
	// http://<HOST>:<PORT>/prf?query=<QUERY>&ranker=<RANKER-TYPE>&numdocs=<INTEGER>&numterms=<INTEGER>

	// Maps the word to the wordFreq for a particular document
	static Map<String, Integer> map = new HashMap<String, Integer>();

	public static void compute(Vector<ScoredDocument> scoredDocs, Query query,
			Indexer indexer, int _numTerms) {
		System.out.println("Insde compute");
		System.out.println("ScoredDocs length " + scoredDocs.size());
		int wordFreq = 0;
		String currWord = "";
		long totalWordsInSet = 0;
		// Calculates for every document the frequency of every word in the
		// query and keeps adding frequency of words to the map.
		for (ScoredDocument doc : scoredDocs) {
			String docInfo = doc.asTextResult();
			System.out.println(docInfo);
			String[] docInfoArray = docInfo.split("\t");
			String docid = docInfoArray[0];
			// Total no of words in a particular document
			DocumentIndexed documentIndexed = (DocumentIndexed) (indexer
					.getDoc(Integer.parseInt(docid)));
			System.out.println(documentIndexed.getWordFrequency());
			System.out.println("before getting bodyTokens");
			Map<String, Integer> tempWordFrequency = documentIndexed
					.getWordFrequency();
			
			Map<String, Integer> mTerms = MapUtil.sortByValue(tempWordFrequency, _numTerms);
			long totalWordsInDoc = documentIndexed.getTotalWords();
			System.out.println("totalWords in doc " + totalWordsInDoc);
			totalWordsInSet += totalWordsInDoc;

			for (Map.Entry<String, Integer> entry : mTerms
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
		calculateProbability(totalWordsInSet);
	}

	private static void calculateProbability(long totalWordsInDoc) {
		System.out.println("Inside calculateProbability");
		Map<String, Long> probabilityMap = new HashMap<String, Long>();
		int totalProbability = 0;
		for (Map.Entry<String, Integer> currWord : map.entrySet()) {
			int wordFreq = currWord.getValue();
			long probability = wordFreq / totalWordsInDoc;
			probabilityMap.put(currWord.getKey(), probability);
			totalProbability += probability;
		}
		writeToFile(probabilityMap, totalProbability);
	}

	private static void writeToFile(Map<String, Long> probabilityMap,
			int totalProbability) {
		try {
			File file = new File("query.txt");

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);
			for (Map.Entry<String, Long> entry : probabilityMap.entrySet()) {
				bw.write("<" + entry.getKey() + "> " + " <"
						+ (entry.getValue() / totalProbability) + ">");
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}