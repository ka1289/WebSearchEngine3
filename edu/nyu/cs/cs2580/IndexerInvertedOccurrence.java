package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * 
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer {
	private HashMap<String, WordAttribute_WordOccurrences>[] mapOfMaps;
	private Map<String, WordAttribute_WordOccurrences> wordMap = new HashMap<String, WordAttribute_WordOccurrences>();
	private Map<Integer, DocumentIndexed> docMap = new HashMap<Integer, DocumentIndexed>();

	public IndexerInvertedOccurrence(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}
	

	private List<String> tokenize(TokenStream stream) throws IOException {
		List<String> words = new ArrayList<String>();
		CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);
		while (stream.incrementToken()) {
			words.add(attr.toString());
		}
		stream.end();
		stream.close();
		return words;
	}

	@Override
	public void constructIndex() throws IOException {
		File corpusDir = new File(_options._corpusPrefix);
		File[] listOfFiles = corpusDir.listFiles();
		int noOfFiles = listOfFiles.length;
		int i = 0;
		int index = 1;
		initializeMap();
		for (File eachFile : listOfFiles) {
			if (i >= noOfFiles / 10) {
				// System.out.println("here");
				serialize();
				mapOfMaps = null;
				i = 0;
				initializeMap();
			}
			// System.out.println(i);
			analyse(eachFile, index);
			index++;
			i++;
		}
		serialize();
		mapOfMaps = null;
		try {
			merge();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void merge() throws ClassNotFoundException, IOException {
		File indexDir = new File(_options._indexPrefix);
		File[] indexedFiles = indexDir.listFiles();
		indexedFiles = indexDir.listFiles();
		for (File file : indexedFiles) {
			if (file.getName().equals(".DS_Store")
					|| file.getName().equals("doc_map.csv"))
				continue;
			BufferedReader ois = new BufferedReader(new FileReader(
					file.getAbsoluteFile()));
			String o;
			while (((o = ois.readLine()) != null)) {
				String[] eachLine = o.split("\t");
				String key = eachLine[0];
				if (wordMap.containsKey(key)) {
					WordAttribute_WordOccurrences wa1 = wordMap.get(key);
					wa1.setFreq(Integer.parseInt(eachLine[eachLine.length - 1])
							+ wa1.getFreq());
					HashMap<Integer, ArrayList<Integer>> currMap = wa1
							.getList();
					int i = 1;
					while (i < eachLine.length - 1) {
						int did = Integer.parseInt(eachLine[i]);
						i++;
						int fr = Integer.parseInt(eachLine[i]);
						i++;
						int k = 0;
						ArrayList<Integer> list = new ArrayList<Integer>();
						while (k < fr) {
							list.add(Integer.parseInt(eachLine[i]));
							k++;
							i++;
						}
						if (currMap.containsKey(did)) {
							currMap.get(did).addAll(list);
						} else {
							currMap.put(did, list);
						}
					}
				} else {
					WordAttribute_WordOccurrences wa = new WordAttribute_WordOccurrences();
					wa.setFreq(Integer.parseInt(eachLine[eachLine.length - 1]));
					LinkedHashMap<Integer, ArrayList<Integer>> currMap = new LinkedHashMap<Integer, ArrayList<Integer>>();
					int i = 1;
					while (i < eachLine.length - 1) {
						int did = Integer.parseInt(eachLine[i]);
						i++;
						int fr = Integer.parseInt(eachLine[i]);
						i++;
						int k = 0;
						ArrayList<Integer> list = new ArrayList<Integer>();
						while (k < fr) {
							list.add(Integer.parseInt(eachLine[i]));
							k++;
							i++;
						}
						currMap.put(did, list);
					}
					wa.setList(currMap);
					wordMap.put(key, wa);
				}
			}

			StringBuilder builder = new StringBuilder();
			builder.append(_options._indexPrefix).append("/").append("numDocs")
					.append(".csv");
			BufferedWriter buf = new BufferedWriter(new FileWriter(
					builder.toString(), true));
			buf.write(_numDocs + "");
			buf.newLine();
			buf.write(_totalTermFrequency + "");
			buf.close();

			String s = file.getName().split("_")[0];
			BufferedWriter oos = new BufferedWriter(new FileWriter(
					_options._indexPrefix + "/" + s + ".csv", true));
			for (String si : wordMap.keySet()) {
				oos.write(si + "\t");
				WordAttribute_WordOccurrences wa = wordMap.get(si);
				Map<Integer, ArrayList<Integer>> wa_list = wa.getList();
				for (int did : wa_list.keySet()) {
					oos.write(did + "\t");
					ArrayList<Integer> did_list = wa_list.get(did);
					int len = did_list.size();
					oos.write(len + "\t");
					for (int k : did_list)
						oos.write(k + "\t");
				}
				oos.write(wa.getFreq() + "");
				oos.newLine();
			}
			wordMap.clear();
			oos.close();
			ois.close();
			file.delete();
		}
	}

	private void serialize() throws IOException {
		StringBuilder builder = new StringBuilder(_options._indexPrefix)
				.append("/").append("doc_map.csv");
		BufferedWriter aoos = new BufferedWriter(new FileWriter(
				builder.toString(), true));
		// aoos.writeObject(docMap);
		for (int doc : docMap.keySet()) {
			aoos.write(doc + "\t");
			DocumentIndexed docIndexed = docMap.get(doc);
			aoos.write(docIndexed.getTitle() + "\t" + docIndexed.getUrl()
					+ "\t");
			aoos.write(docIndexed.getTotalWords() + "");
			aoos.newLine();
		}
		aoos.close();
		docMap.clear();
		for (int i = 0; i < 199; i++) {
			StringBuilder file = new StringBuilder(_options._indexPrefix)
					.append("/").append(i).append("_tmp.csv");
			BufferedWriter oos = new BufferedWriter(new FileWriter(
					file.toString(), true));
			HashMap<String, WordAttribute_WordOccurrences> attr = mapOfMaps[i];
			for (String s : attr.keySet()) {
				oos.write(s + "\t");
				WordAttribute_WordOccurrences wa = attr.get(s);
				Map<Integer, ArrayList<Integer>> wa_list = wa.getList();
				for (int did : wa_list.keySet()) {
					oos.write(did + "\t");
					ArrayList<Integer> did_list = wa_list.get(did);
					int len = did_list.size();
					oos.write(len + "\t");
					for (int k : did_list)
						oos.write(k + "\t");
				}
				oos.write(wa.getFreq() + "");
				oos.newLine();
			}
			oos.close();
		}
	}

	private void analyse(File eachFile, int index) throws IOException {
		_numDocs++;
		DocumentIndexed docIndexed = new DocumentIndexed(index);
		docIndexed.setTitle(eachFile.getName());
		docIndexed.setUrl(eachFile.getPath());
		HashSet<String> stopWords = new HashSet<String>();
		stopWords.add("the");
		stopWords.add("and");
		stopWords.add("or");
		stopWords.add("an");
		stopWords.add("if");
		stopWords.add("but");
		stopWords.add("the");
		stopWords.add("is");
		stopWords.add("an");
		stopWords.add("he");
		stopWords.add("she");
		stopWords.add("be");
		stopWords.add("me");
		stopWords.add("has");
		stopWords.add("http");
		String newFile = Parser.parse(eachFile);
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_30, stopWords);
		List<String> words = tokenize(analyzer.tokenStream("",
				new StringReader(newFile)));
		int i = 0;
		for (String word : words) {
			_totalTermFrequency++;
			String stemmed = Stemmer.stemAWord(word);
			if (stemmed.matches("[A-Za-z0-9\\p{Punct}\\s]+")) {
				int hash = Math.abs(stemmed.hashCode()) % 199;
				HashMap<String, WordAttribute_WordOccurrences> currMap = mapOfMaps[hash];
				if (!currMap.containsKey(stemmed)) {
					WordAttribute_WordOccurrences currWordAttr = new WordAttribute_WordOccurrences();
					LinkedHashMap<Integer, ArrayList<Integer>> currMapMap = new LinkedHashMap<Integer, ArrayList<Integer>>();
					currWordAttr.setList(currMapMap);
					currMap.put(stemmed, currWordAttr);
				}
				WordAttribute_WordOccurrences currWordAttr = currMap
						.get(stemmed);
				LinkedHashMap<Integer, ArrayList<Integer>> currMapMap = currWordAttr
						.getList();
				if (currMapMap.containsKey(index)) {
					ArrayList<Integer> listOfOccurrences = currMapMap
							.get(index);
					listOfOccurrences.add(i);
				} else {
					ArrayList<Integer> listOfOccurrences = new ArrayList<Integer>();
					listOfOccurrences.add(i);
					currMapMap.put(index, listOfOccurrences);
				}
				int freq = currWordAttr.getFreq();
				freq++;
				currWordAttr.setFreq(freq);
			}
			i++;
		}
		analyzer.close();
		docIndexed.setTotalWords(words.size());
		docMap.put(index, docIndexed);
	}

	@SuppressWarnings("unchecked")
	private void initializeMap() {
		mapOfMaps = (HashMap<String, WordAttribute_WordOccurrences>[]) new HashMap[199];
		for (int j = 0; j < 199; j++) {
			mapOfMaps[j] = new HashMap<String, WordAttribute_WordOccurrences>();
		}
	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		wordMap = new HashMap<String, WordAttribute_WordOccurrences>();
		File indexDir = new File(_options._indexPrefix);
		File[] indexedFiles = indexDir.listFiles();
		int i = 0;
		for (File file : indexedFiles) {
			if (file.getName().equals(".DS_Store"))
				continue;
			if (file.getName().equals("doc_map.csv")) {
				loadDocMap(file);
				continue;
			}

			if (file.getName().equals("numDocs.csv")) {
				loadNumDocs(file);
				continue;
			}

			if (i < 7 && !file.getName().equals("doc_map.csv")
					&& !file.getName().equals(".DS_Store")
					&& !file.getName().equals("numDocs.csv")) {
				loadFile(file);
			}
			i++;
		}
	}

	private void loadNumDocs(File file) throws IOException {
		BufferedReader ois = new BufferedReader(new FileReader(
				file.getAbsoluteFile()));
		String o;
		int i = 0;
		while (((o = ois.readLine()) != null)) {
			if (i == 0)
				_numDocs = Integer.parseInt(o);
			else
				_totalTermFrequency = Long.parseLong(o);
			i++;
		}
		ois.close();
	}

	private void loadFile(File file) throws NumberFormatException, IOException {
		BufferedReader ois = new BufferedReader(new FileReader(
				file.getAbsoluteFile()));
		String o;
		while (((o = ois.readLine()) != null)) {
			String[] eachLine = o.split("\t");
			String key = eachLine[0];
			WordAttribute_WordOccurrences wa = new WordAttribute_WordOccurrences();
			wa.setFreq(Integer.parseInt(eachLine[eachLine.length - 1]));
			LinkedHashMap<Integer, ArrayList<Integer>> currMap = new LinkedHashMap<Integer, ArrayList<Integer>>();
			int i = 1;
			while (i < eachLine.length - 1) {
				int did = Integer.parseInt(eachLine[i]);
				i++;
				int fr = Integer.parseInt(eachLine[i]);
				i++;
				int k = 0;
				ArrayList<Integer> list = new ArrayList<Integer>();
				while (k < fr) {
					list.add(Integer.parseInt(eachLine[i]));
					k++;
					i++;
				}
				currMap.put(did, list);
			}
			wa.setList(currMap);
			wordMap.put(key, wa);
		}
		ois.close();
	}

	private void loadDocMap(File file) throws NumberFormatException,
			IOException {
		docMap = new HashMap<Integer, DocumentIndexed>();
		BufferedReader ois = new BufferedReader(new FileReader(
				file.getAbsoluteFile()));
		String o;
		while (((o = ois.readLine()) != null)) {
			String[] eachLine = o.split("\t");
			int did = Integer.parseInt(eachLine[0]);
			DocumentIndexed wa = new DocumentIndexed(did);
			String title = eachLine[1];
			String url = eachLine[2];
			long totalWords = Integer.parseInt(eachLine[eachLine.length - 1]);
			wa.setTitle(title);
			wa.setUrl(url);
			wa.setTotalWords(totalWords);
			docMap.put(did, wa);
		}
		ois.close();
	}

	@Override
	public Document getDoc(int docid) {
		if (!checkInCache(docid)) {
			try {
				loadDocInCache(docid);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (docMap.containsKey(docid))
			return docMap.get(docid);
		return null;
	}

	/**
	 * 
	 * In HW2, you should be using {@link DocumentIndexed}.
	 * 
	 * 
	 * 
	 * @throws IOException
	 */
	@Override
	public DocumentIndexed nextDoc(Query query, int docid) {
		QueryPhrase queryPhrase = new QueryPhrase(query._query);
		queryPhrase.processQuery();
		// if docid is -1 then make docid=0
		// if (docid == -1) {
		// docid = smallestList.get(0);
		// }
		List<String> phrases = new ArrayList<String>();
		StringBuilder tokens = new StringBuilder();
		for (String strTemp : queryPhrase._tokens) {
			// Checking of the string is a phrase or not
			if (strTemp.split(" ").length > 1) {
				String[] temp_split = strTemp.split(" ");
				for (String z : temp_split) {
					if (!isPresentInCache(z)) {
						boolean flag = false;
						try {
							flag = loadInCache(z, queryPhrase);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (flag == false)
							return null;
					}
				}
				phrases.add(strTemp.trim());
			} else {
				if (!isPresentInCache(strTemp.trim())) {
					boolean flag = false;
					try {
						flag = loadInCache(strTemp.trim(), queryPhrase);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (flag == false)
						return null;
				}
				tokens.append(strTemp.trim());
				tokens.append(" ");
			}
		}
		// I get the next document to be checked for phrase which contains all
		// the other
		// non phrase tokens
		// Run a Loop here-----
		DocumentIndexed documentToBeCheckedForPhrases = null;
		try {
			if (tokens.length() == 0) {
				return functionCall(phrases, docid);
			}
			documentToBeCheckedForPhrases = nextDocToken(
					new Query(tokens.toString()), docid);
			if (documentToBeCheckedForPhrases == null)
				return null;
			// System.out.println("document : "+documentToBeCheckedForPhrases._docid);
			if (phrases.size() == 0) {
				return documentToBeCheckedForPhrases;
			} else {
				while (documentToBeCheckedForPhrases != null) {
					// Check if all the phrases in the original query are
					// present in the
					// document

					boolean value = checkIfPhrasesPresent(
							documentToBeCheckedForPhrases._docid, phrases);
					if (!value) {
						docid = documentToBeCheckedForPhrases._docid;
						try {
							documentToBeCheckedForPhrases = nextDocToken(
									new Query(tokens.toString()), docid);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						continue;
					} else {
						return documentToBeCheckedForPhrases;
					}

				}
			}
			// First find out the smallest list among the list of all the words
			// String smallestListWord = findWordWithSmallestList();
			//
			// Now take a next docId form the list of the smallestListWord
			// WordAttribute_WordOccurrences
			// smallestWordAttribute_WordOccurrences =
			// wordMap.get(smallestListWord);
			// LinkedHashMap<Integer, ArrayList<Integer>> smallestMap =
			// smallestWordAttribute_WordOccurrences.getList();
			//
			// Find the position of docid in the smallestListWord
			// ArrayList<Integer> positions = smallestMap.get(docid);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private DocumentIndexed functionCall(List<String> phrases, int docid)
			throws IOException {

		StringBuilder query = new StringBuilder();
		for (String s : phrases) {
			String[] split = s.split(" ");
			for (String b : split)
				query.append(b).append(" ");
		}

		String smallestWord = findWordWithSmallestList(new Query(query
				.toString().trim()));
		WordAttribute_WordOccurrences smallestListOccr = wordMap
				.get(smallestWord);
		LinkedHashMap<Integer, ArrayList<Integer>> list = smallestListOccr
				.getList();

		if (docid == -1) {
			Iterator<Integer> iter = list.keySet().iterator();
			docid = iter.next();
		}

		for (Map.Entry<Integer, ArrayList<Integer>> currMap : list.entrySet()) {
			int currentDocId = currMap.getKey();
			if (currentDocId <= docid)
				continue;
			boolean flag = isPresentInAll(currentDocId, smallestWord,
					new Query(query.toString().trim()));
			if (!flag)
				continue;
			boolean value = checkIfPhrasesPresent(currentDocId, phrases);
			if (value) {
				DocumentIndexed docInd = docMap.get(currentDocId);
				return docInd;
			}
		}

		return null;
	}

	private boolean checkIfPhrasesPresent(int docid, List<String> phrases) {
		for (String str : phrases) {
			boolean value = isPhrasePresent(str, docid);
			if (value) {
				continue;
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * 
	 * 
	 * Checks if the particular phrase is present in the docid
	 * 
	 * 
	 * 
	 * @param str
	 * 
	 * @param docid
	 * 
	 * @return
	 */
	private boolean isPhrasePresent(String str, int docid) {
		String[] phrase = str.split(" ");

		if (!isPresentInCache(phrase[0])) {
			boolean flag = false;
			try {
				flag = loadInCache(phrase[0]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (flag == false)
				return false;
		}

		WordAttribute_WordOccurrences currentWordAttribute_WordOccurrences = wordMap
				.get(phrase[0]);
		LinkedHashMap<Integer, ArrayList<Integer>> map = currentWordAttribute_WordOccurrences
				.getList();
		if (!map.containsKey(docid))
			return false;
		List<Integer> list = map.get(docid);

		boolean flag = false;
		for (int position : list) {
			flag = false;
			int currentPositon = position + 1;
			for (int j = 1; j < phrase.length; j++) {
				boolean value = isPresentAtPosition(currentPositon, docid,
						phrase[j]);
				if (value) {
					currentPositon++;
					continue;
				} else {
					flag = true;
					break;
				}
			}
			if (!flag) {
				return true;
			}
		}
		return false;
	}

	private boolean isPresentAtPosition(int position, int docid, String string) {
		WordAttribute_WordOccurrences currentWordAttribute_WordOccurrences = wordMap
				.get(string);
		LinkedHashMap<Integer, ArrayList<Integer>> map = currentWordAttribute_WordOccurrences
				.getList();

		if (!map.containsKey(docid))
			return false;
		List<Integer> list = map.get(docid);
		return list.contains(position);
	}

	private DocumentIndexed nextDocToken(Query query, int docid)
			throws IOException {
		query.processQuery();
		// System.out.println("in nextDocToken");
		// First find out the smallest list among the list of all the words
		String smallestListWord = findWordWithSmallestList(query);
		// System.out.println("smallestListword "+smallestListWord);
		// Now take a next docId form the list of the smallestListWord
		WordAttribute_WordOccurrences smallestWordAttribute_WordOccurrences = wordMap
				.get(smallestListWord);
		LinkedHashMap<Integer, ArrayList<Integer>> smallestMap = smallestWordAttribute_WordOccurrences
				.getList();
		// if docid is -1 then make docid=0
		if (docid == -1) {
			Iterator<Integer> iterator = smallestMap.keySet().iterator();
			docid = iterator.next();
		}
		// Now we iterate through the map and after we reach the docid given
		// From the next docid we will have to call isPresentInAll for the query
		// SImilar to the function written in IndexerInvertedDoconly.java

		for (Map.Entry<Integer, ArrayList<Integer>> currentMap : smallestMap
				.entrySet()) {
			// System.out.println("checking for loop nextDocToken");
			int currentDocId = currentMap.getKey();
			if (currentDocId <= docid) {
				continue;
			}
			boolean value = isPresentInAll(currentDocId, smallestListWord,
					query);
			if (value == true) {
				if (!checkInCache(currentDocId)) {
					// System.out.println("in cache check");
					try {
						loadDocInCache(currentDocId);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				// DocumentIndexed docId=new DocumentIndexed(currentDocId);
				return docMap.get(currentDocId);
			}
		}
		return null;
	}

	private boolean isPresentInAll(int docid, String originalWord, Query query) {
		query.processQuery();
		ArrayList<String> tokens = new ArrayList<String>();
		for (String str : query._tokens) {
			boolean flag = wordMap.containsKey(str);
			if (!isPresentInCache(str)) {
				try {
					flag = loadInCache(str);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (flag)
				tokens.add(str);
		}
		for (String str : tokens) {
			if (str.equals(originalWord)) {
				// System.out.println("in isPresentInAll first if");
				continue;
			} else if (searchForIdInWordList(str, docid)) {
				continue;
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * 
	 * 
	 * Verifies if the docid contains the particular string
	 * 
	 * 
	 * 
	 * @param str
	 * 
	 * @param docid
	 * 
	 * @return
	 */
	private boolean searchForIdInWordList(String str, int docid) {
		// Now since we have a map we can easily verify if the word is present
		// in
		// a document
		WordAttribute_WordOccurrences currentWordAttribute_WordOccurrences = wordMap
				.get(str);
		LinkedHashMap<Integer, ArrayList<Integer>> currentMap = currentWordAttribute_WordOccurrences
				.getList();
		return currentMap.containsKey(docid);
	}

	private String findWordWithSmallestList(Query query) throws IOException {
		query.processQuery();
		int minListLength = Integer.MAX_VALUE;
		String smallestListWord = "";
		for (String strTemp : query._tokens) {
			boolean flag = false;
			if (!isPresentInCache(strTemp)) {
				// System.out.println("not in cache");
				flag = loadInCache(strTemp, query);
				if (flag == false)
					continue;
			}
			WordAttribute_WordOccurrences currentWordAttribute_WordOccurrences = wordMap
					.get(strTemp);
			int mapSize = currentWordAttribute_WordOccurrences.getList().size();
			// System.out.println(mapSize);
			if (minListLength > mapSize) {
				minListLength = mapSize;
				smallestListWord = strTemp;
				// System.out.println("smallestListWord"+smallestListWord);
			}
		}
		return smallestListWord;
	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		boolean flag = false;
		if (!isPresentInCache(term)) {
			try {
				flag = loadInCache(term);
				if (flag == false)
					return 0;
			} catch (IOException e) {
				// TODO
				e.printStackTrace();
			}
		}

		return wordMap.get(term).getList().size();
	}

	private boolean isPresentInCache(String term) {
		return wordMap.containsKey(term);
	}

	@Override
	public int corpusTermFrequency(String term) {
		boolean flag = false;
		if (!isPresentInCache(term)) {
			try {
				flag = loadInCache(term);
				if (flag == false) {
					return 0;
				}
			} catch (IOException e) {
				// TODO
				e.printStackTrace();
			}
		}

		return wordMap.get(term).getFreq();
	}

	private boolean loadInCache(String term) throws IOException {
		if (wordMap.containsKey(term))
			return true;

		int hash = 0;
		boolean flag = false;
		String stemmed = Stemmer.stemAWord(term).trim();
		if (stemmed.matches("[A-Za-z0-9\\p{Punct}\\s]+"))
			hash = Math.abs(stemmed.hashCode()) % 199;
		List<String> commands = new ArrayList<String>();
		commands.add("/bin/bash");
		commands.add("-c");
		commands.add("grep $'^" + term + "\t' " + _options._indexPrefix + "/"
				+ hash + ".csv");
		ProcessBuilder pb = new ProcessBuilder(commands);
		Process p = pb.start();
		BufferedReader ois = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		String o;

		if (((o = ois.readLine()) != null)) {
			String[] eachLine = o.split("\t");
			String key = eachLine[0];
			WordAttribute_WordOccurrences wa = new WordAttribute_WordOccurrences();
			wa.setFreq(Integer.parseInt(eachLine[eachLine.length - 1]));
			LinkedHashMap<Integer, ArrayList<Integer>> currMap = new LinkedHashMap<Integer, ArrayList<Integer>>();
			int i = 1;
			while (i < eachLine.length - 1) {
				int did = Integer.parseInt(eachLine[i]);
				i++;
				int fr = Integer.parseInt(eachLine[i]);
				i++;
				int k = 0;
				ArrayList<Integer> list = new ArrayList<Integer>();
				while (k < fr) {
					list.add(Integer.parseInt(eachLine[i]));
					k++;
					i++;
				}
				currMap.put(did, list);
				// System.out.println("curr map "+currMap.containsKey(hash));
			}
			wa.setList(currMap);
			wordMap.put(key, wa);
			flag = true;
		}
		ois.close();
		return flag;
	}

	@Override
	public int documentTermFrequency(String term, String url) {
		int did = Integer.parseInt(url);
		boolean flag = false;
		if (!isPresentInCache(term)) {
			try {
				flag = loadInCache(term);
				if (flag == false)
					return 0;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		int output = wordMap.get(term).getList().get(did).size();

		return output;
	}

	private boolean loadDocInCache(int did) throws IOException {
		if (docMap.containsKey(did))
			return true;
		Runtime runtime = Runtime.getRuntime();
		if (runtime.freeMemory() < 100000000) {
			Iterator<Integer> iter = docMap.keySet().iterator();
			int temp = iter.next();
			docMap.remove(temp);
		}
		List<String> commands = new ArrayList<String>();
		commands.add("/bin/bash");
		commands.add("-c");
		commands.add("grep $'^" + did + "\t' " + _options._indexPrefix + "/"
				+ "doc_map.csv");
		ProcessBuilder pb = new ProcessBuilder(commands);
		Process p = pb.start();
		BufferedReader ois = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		String o;
		boolean flag = false;
		while (((o = ois.readLine()) != null)) {
			String[] eachLine = o.split("\t");
			int docid = Integer.parseInt(eachLine[0]);
			DocumentIndexed wa = new DocumentIndexed(docid);
			String title = eachLine[1];
			String url = eachLine[2];
			int totalWords = Integer.parseInt(eachLine[eachLine.length - 1]);
			wa.setTitle(title);
			wa.setUrl(url);
			wa.setTotalWords(totalWords);
			docMap.put(docid, wa);
			flag = true;
		}
		ois.close();
		return flag;
	}

	private boolean loadInCache(String word, Query query) throws IOException {
		int hash = 0;
		if (wordMap.containsKey(word))
			return true;
		List<String> to_be_removed = new ArrayList<String>();
		int k = 0;
		for (String s : wordMap.keySet()) {
			if (k >= query._tokens.size())
				break;
			if (!query._tokens.contains(s))
				to_be_removed.add(s);
			k++;
		}
		for (String s : to_be_removed)
			wordMap.remove(s);
		boolean flag = false;
		String stemmed = Stemmer.stemAWord(word);
		if (stemmed.matches("[A-Za-z0-9\\p{Punct}\\s]+"))
			hash = Math.abs(stemmed.hashCode()) % 199;
		// System.out.println("hashed ");
		List<String> commands = new ArrayList<String>();
		commands.add("/bin/bash");
		commands.add("-c");
		commands.add("grep $'^" + word + "\t' " + _options._indexPrefix + "/"
				+ hash + ".csv");
		ProcessBuilder pb = new ProcessBuilder(commands);
		Process p = pb.start();
		BufferedReader ois = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		String o;
		while (((o = ois.readLine()) != null)) {
			// System.out.println("ois");
			String[] eachLine = o.split("\t");
			String key = eachLine[0];
			WordAttribute_WordOccurrences wa = new WordAttribute_WordOccurrences();
			wa.setFreq(Integer.parseInt(eachLine[eachLine.length - 1]));
			LinkedHashMap<Integer, ArrayList<Integer>> currMap = new LinkedHashMap<Integer, ArrayList<Integer>>();
			int i = 1;
			while (i < eachLine.length - 1) {
				int did = Integer.parseInt(eachLine[i]);
				// System.out.println(did);
				i++;
				int fr = Integer.parseInt(eachLine[i]);
				i++;
				k = 0;
				ArrayList<Integer> list = new ArrayList<Integer>();
				while (k < fr) {
					list.add(Integer.parseInt(eachLine[i]));
					k++;
					i++;
				}
				currMap.put(did, list);
				// System.out.println("curr map "+currMap.containsKey(hash));
			}
			wa.setList(currMap);
			wordMap.put(key, wa);
			flag = true;
		}
		ois.close();
		return flag;
	}

	private boolean checkInCache(int docId) {
		return docMap.containsKey(docId);
	}
}