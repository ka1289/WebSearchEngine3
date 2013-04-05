package edu.nyu.cs.cs2580;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ComputeQueryRepresentation {
	
 //	That is, your request should have the form,
//	http://<HOST>:<PORT>/prf?query=<QUERY>&ranker=<RANKER-TYPE>&numdocs=<INTEGER>&numterms=<INTEGER>
	
	//Maps the word to the wordFreq for a particular document
	static Map <String,Integer> map = new HashMap<String, Integer>();
	
	public static void compute(Vector<ScoredDocument> scoredDocs, Query query, Indexer indexer) {				
		int wordFreq = 0;
		String currWord = "";
		long totalWordsInSet = 0;
		//Calculates for every document the frequency of every word in the query 
		// And keeps adding frequency of words to the map.
		for(ScoredDocument doc : scoredDocs) {
			String docInfo = doc.asTextResult();
			String[] docInfoArray = docInfo.split("\t");			
			String docid = docInfoArray[0];
			// Total no of words in a particular document
			DocumentIndexed document = (DocumentIndexed)(indexer.getDoc(Integer.parseInt(docid)));			
			long totalWordsInDoc = document.getTotalWords();
			totalWordsInSet += totalWordsInDoc;
			for(String word : query._tokens) {
				wordFreq = indexer.documentTermFrequency(word, docid);
				currWord = word;
				if(map.containsKey(word)) {
					int num = map.get(word);
					wordFreq += num;					
				}
				map.put(word, wordFreq);				
			}			
		}	
		calculateProbability(totalWordsInSet);	
	}

	private static void calculateProbability(long totalWordsInDoc) {
		Map<String, Long> probabilityMap = new HashMap<String,Long>();
		int totalProbability = 0;
		for(Map.Entry<String, Integer> currWord : map.entrySet()) {
			int wordFreq = currWord.getValue();
			long probability = wordFreq/totalWordsInDoc;
			probabilityMap.put(currWord.getKey(), probability);
			totalProbability += probability;
		}		
		writeToFile(probabilityMap, totalProbability);
  }

	private static void writeToFile(Map<String, Long> probabilityMap, int totalProbability) {
		try {
			FileWriter fw = new FileWriter("C:\\sem4\\feedback\\query.txt" ,true);
			BufferedWriter bw = new BufferedWriter(fw);
			for(Map.Entry<String, Long> entry : probabilityMap.entrySet()) {
				bw.write("<" + entry.getKey()+"> " + " <" + (entry.getValue()/totalProbability) + ">" );
			}			    
    } catch (IOException e) {	     
	    e.printStackTrace();
    }	  
  }	
}