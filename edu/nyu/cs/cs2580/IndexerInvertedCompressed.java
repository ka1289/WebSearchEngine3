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
import java.util.Scanner;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer {
	private HashMap<String, WordAttribute_WordOccurrences>[] mapOfMaps;
	private Map<String, WordAttribute_WordOccurrences> wordMap = new HashMap<String, WordAttribute_WordOccurrences>();
	private Map<Integer, DocumentIndexed> docMap = new HashMap<Integer, DocumentIndexed>();
	private Map<String, WordAttribute_WordOccurrences> wordMapUncompressed = new HashMap<String, WordAttribute_WordOccurrences>();
	private Map<Integer, Integer> numViewsMap = new HashMap<Integer, Integer>();
	private Map<Integer, Float> pageRank = new HashMap<Integer, Float>();

	public IndexerInvertedCompressed(Options options) {
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

	@SuppressWarnings("unchecked")
	@Override
	public void constructIndex() throws IOException {
		try {
			File corpusDir = new File(_options._corpusPrefix);
			File[] listOfFiles = corpusDir.listFiles();
			int noOfFiles = listOfFiles.length;

			int i = 0;
			int index = 0;
			initializeMap();

			LogMiner miner = LogMiner.Factory
					.getLogMinerByOption(SearchEngine.OPTIONS);
			Check(miner != null, "Miner " + SearchEngine.OPTIONS._logMinerType
					+ " not found!");
			numViewsMap = (Map<Integer, Integer>) miner.load();

			CorpusAnalyzer analyzer = CorpusAnalyzer.Factory
					.getCorpusAnalyzerByOption(SearchEngine.OPTIONS);
			Check(analyzer != null, "Analyzer "
					+ SearchEngine.OPTIONS._corpusAnalyzerType + " not found!");
			pageRank = (Map<Integer, Float>) analyzer.load();
			int h = 0;
			for (File eachFile : listOfFiles) {
				int numViews = getNumViews(index, eachFile.getName());
				float pageRank = getPageRank(index);
				if (i >= noOfFiles / 20) {
					serialize();
					serializeDocMap(index, noOfFiles, h);
					mapOfMaps = null;
					i = 0;
					initializeMap();
					h++;
				}
				analyse(eachFile, index, numViews, pageRank);
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void serializeDocMap(int index, int noOfFiles, int fileNum)
			throws IOException {

		StringBuilder builder = new StringBuilder(_options._indexPrefix)
				.append("/").append("doc_map_").append(fileNum).append(".csv");
		BufferedWriter aoos = new BufferedWriter(new FileWriter(
				builder.toString(), true));
		for (int doc : docMap.keySet()) {
			aoos.write(doc + "\t");
			DocumentIndexed docIndexed = docMap.get(doc);
			aoos.write(docIndexed.getTitle() + "\t" + docIndexed.getUrl()
					+ "\t");
			aoos.write(docIndexed.getNumViews() + "\t"
					+ docIndexed.getPageRank() + "\t");

			HashMap<String, Integer> wordFreq = docIndexed.getWordFrequency();
			for (String word : wordFreq.keySet()) {
				aoos.write(word + "\t");
				aoos.write(wordFreq.get(word) + "\t");
			}
			aoos.write(docIndexed.getTotalWords() + "");
			aoos.newLine();
		}
		aoos.close();
		docMap.clear();
	}

	private int getNumViews(int docid, String eachFile) throws IOException {
		if (numViewsMap.containsKey(docid))
			return numViewsMap.get(docid);
		else {
			List<String> commands = new ArrayList<String>();
			commands.add("/bin/bash");
			commands.add("-c");
			commands.add("grep $'^" + eachFile + "\t' " + _options._indexPrefix
					+ "/" + "numViews.csv");
			ProcessBuilder pb = new ProcessBuilder(commands);
			Process p = pb.start();
			BufferedReader ois = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			String o = ois.readLine();
			if (o != null) {
				String[] eachLine = o.split(" ");
				int num = Integer.parseInt(eachLine[1]);
				return num;
			}
			ois.close();
		}
		return 0;
	}

	private float getPageRank(int index) throws IOException {
		if (pageRank.containsKey(index))
			return pageRank.get(index);
		else {
			List<String> commands = new ArrayList<String>();
			commands.add("/bin/bash");
			commands.add("-c");
			commands.add("grep $'^" + index + "\t' " + _options._indexPrefix
					+ "/" + "pageRanks.csv");
			ProcessBuilder pb = new ProcessBuilder(commands);
			Process p = pb.start();
			BufferedReader ois = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			String o = ois.readLine();
			if (o != null) {
				String[] eachLine = o.split(" ");
				float rank = Float.parseFloat(eachLine[1]);
				return rank;
			}
			ois.close();
		}
		return 0;
	}

	public static void Check(boolean condition, String msg) {
		if (!condition) {
			System.err.println("Fatal error: " + msg);
			System.exit(-1);
		}
	}

	private void merge() throws ClassNotFoundException, IOException {
		File indexDir = new File(_options._indexPrefix);
		File[] indexedFiles = indexDir.listFiles();
		indexedFiles = indexDir.listFiles();
		for (File file : indexedFiles) {
			if (file.getName().equals(".DS_Store")
					|| file.getName().matches("doc_map_[0-9][0-9]?.csv")
					|| file.getName().equals("numViews.csv")
					|| file.getName().equals("pageRanks.csv")
					|| file.getName().equals("numDocs.csv"))
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
				WordAttribute_WordOccurrences temp_attr = wordMap.get(si);
				LinkedHashMap<Integer, ArrayList<Integer>> temp_map = temp_attr
						.getList();
				for (int docid : temp_map.keySet()) {
					oos.write(CompressionUtility.encodeByteAlign(docid));

					ArrayList<Integer> temp_occr = temp_map.get(docid);

					for (int eachOccr : temp_occr) {
						oos.write(CompressionUtility
								.encodeByteAlign((eachOccr)));
					}

					oos.write("\t");
				}
				int freq = temp_attr.getFreq();
				oos.write(CompressionUtility.encodeByteAlign(freq) + "");
				oos.newLine();
			}
			oos.close();
			ois.close();
			file.delete();
			wordMap.clear();
		}

	}

	private void serialize() throws IOException {

		for (int i = 0; i < 199; i++) {
			StringBuilder file = new StringBuilder(_options._indexPrefix)
					.append("/").append(i).append("_tmp.csv");

			BufferedWriter oos = new BufferedWriter(new FileWriter(
					file.toString(), true));
			HashMap<String, WordAttribute_WordOccurrences> attr = mapOfMaps[i];
			for (String s : attr.keySet()) {
				oos.write(s + "\t");
				WordAttribute_WordOccurrences temp_attr = attr.get(s);
				LinkedHashMap<Integer, ArrayList<Integer>> temp_map = temp_attr
						.getList();
				for (int docid : temp_map.keySet()) {
					ArrayList<Integer> temp_occr = temp_map.get(docid);

					int len = temp_occr.size();

					if (len == 0)
						continue;

					oos.write(docid + "\t");

					oos.write(len + "\t");

					for (int eachOccr : temp_occr) {
						oos.write((eachOccr) + "\t");
					}

				}
				int freq = temp_attr.getFreq();
				oos.write(freq + "");

				oos.newLine();
			}
			oos.close();
		}
	}

	private void analyse(File eachFile, int index, int numViews, float pageRank)
			throws IOException {
		_numDocs++;
		DocumentIndexed docIndexed = new DocumentIndexed(index);
		docIndexed.setTitle(eachFile.getName());
		docIndexed.setUrl(eachFile.getPath());
		docIndexed.setNumViews(numViews);
		docIndexed.setPageRank(pageRank);

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
			docIndexed.incrementWordFrequency(word);

			String stemmed = Stemmer.stemAWord(word).trim();
			if (stemmed.matches("[A-Za-z0-9]+")) {
				_totalTermFrequency++;
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
					int delta = 0;
					ArrayList<Integer> listOfDocid = currMapMap.get(index);
					for (int k : listOfDocid) {
						delta += k;
					}
					delta = i - delta;
					int temp_deltaDocid = delta;
					listOfDocid.add(temp_deltaDocid);
					currMapMap.remove(index);
					currMapMap.put(index, listOfDocid);
				} else {
					int temp_deltaDocid = i;
					ArrayList<Integer> listOfDocid = new ArrayList<Integer>();
					listOfDocid.add(temp_deltaDocid);
					currMapMap.put(index, listOfDocid);
				}

				int freq = currWordAttr.getFreq();
				freq++;
				currWordAttr.setFreq(freq);
				i++;
			}

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
		wordMapUncompressed = new HashMap<String, WordAttribute_WordOccurrences>();

		File indexDir = new File(_options._indexPrefix);
		File[] indexedFiles = indexDir.listFiles();

		int noFilesLoaded = 0;
		boolean isOneDocMapLoaded = false;
		for (File file : indexedFiles) {
			if (file.getName().equals(".DS_Store")) {
				continue;
			}

			if (file.getName().matches("doc_map_[0-9][0-9]?.csv")) {
				if (!isOneDocMapLoaded) {
					loadDocMap(file);
					isOneDocMapLoaded = true;
				}

				continue;
			}

			if (file.getName().equals("numDocs.csv")) {
				loadNumDocs(file);
				continue;
			}

			if (noFilesLoaded < 20
					&& !file.getName().matches("doc_map_[0-9][0-9]?.csv")
					&& !file.getName().equals(".DS_Store")
					&& !file.getName().equals("numDocs.csv")
					&& !file.getName().equals("numViews.csv")
					&& !file.getName().equals("pageRank_graph.csv")
					&& !file.getName().equals("pageRanks.csv")) {
				loadFile(file);
				noFilesLoaded++;
			}
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

	/**
	 * Reads a file and loads the data into wordMapUncompressed.
	 * 
	 * @param file
	 * @throws IOException
	 */
	private void loadFile(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(
				file.getAbsoluteFile()));
		String line = "";
		while (((line = br.readLine()) != null)) {
			String[] eachLine = line.split("\t");
			String word = eachLine[0];
			WordAttribute_WordOccurrences wordAttribute_WordOccurrences = new WordAttribute_WordOccurrences();
			// get the frequency for the words
			char[] tempCH = eachLine[eachLine.length - 1].toCharArray();
			Vector<Character> tempVectorArray = new Vector<Character>();
			for (char c : tempCH)
				tempVectorArray.add(c);

			int freq = DecompressionUtility.decodeByteAlign(tempVectorArray)
					.get(0);
			wordAttribute_WordOccurrences.setFreq(freq);
			LinkedHashMap<Integer, ArrayList<Integer>> currMap = new LinkedHashMap<Integer, ArrayList<Integer>>();

			int i = 1;
			while (i < eachLine.length - 1) {
				char[] temp_char = eachLine[i].toCharArray();
				Vector<Character> tempVectorArray_char = new Vector<Character>();
				for (char c : temp_char)
					tempVectorArray_char.add(c);

				List<Integer> tempList = DecompressionUtility
						.decodeByteAlign(tempVectorArray_char);
				int did = tempList.get(0);

				int frequencyInDoc = tempList.size();
				int k = 1;
				int prev = 0;
				ArrayList<Integer> list = new ArrayList<Integer>();
				while (k < frequencyInDoc) {
					int temp = tempList.get(k);
					list.add(temp + prev);
					k++;
				}
				currMap.put(did, list);
				i++;
			}
			wordAttribute_WordOccurrences.setList(currMap);
			wordMapUncompressed.put(word, wordAttribute_WordOccurrences);
		}
		br.close();

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
			int numViews = Integer.parseInt(eachLine[3]);
			float pageRank = Float.parseFloat(eachLine[4]);
			long totalWords = Integer.parseInt(eachLine[eachLine.length - 1]);
			wa.setTitle(title);
			wa.setUrl(url);
			wa.setTotalWords(totalWords);
			wa.setNumViews(numViews);
			wa.setPageRank(pageRank);

			HashMap<String, Integer> temp_map = new HashMap<String, Integer>();

			int index = 5;
			while (index < eachLine.length - 3) {
				String temp_word = eachLine[index];
				index++;
				int temp_freq = Integer.parseInt(eachLine[index]);
				index++;
				temp_map.put(temp_word, temp_freq);
			}

			wa.setWordFrequency(temp_map);
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

	public DocumentIndexed nextDoc(Query query, int docid) {
		QueryPhrase queryPhrase = new QueryPhrase(query._query);
		queryPhrase.processQuery();
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
							e.printStackTrace();
						}
						if (flag == false)
							return null;
					}
				}
				phrases.add(strTemp.trim());
			} else {
				// system.out.println("in nextDoc " + strTemp);
				if (!isPresentInCache(strTemp.trim())) {
					boolean flag = false;
					try {
						// system.out.println("Happening?");
						flag = loadInCache(strTemp.trim(), queryPhrase);
						// system.out.println("FLAG " + flag);
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (flag == false)
						return null;
				}
				tokens.append(strTemp.trim());
				tokens.append(" ");
			}
		}
		DocumentIndexed documentToBeCheckedForPhrases = null;
		try {
			// system.out.println("IS that it?");
			if (tokens.length() == 0) {
				return functionCall(phrases, docid);
			}

			documentToBeCheckedForPhrases = nextDocToken(
					new Query(tokens.toString()), docid);
			// system.out.println(" --- " + documentToBeCheckedForPhrases);
			if (documentToBeCheckedForPhrases == null)
				return null;
			if (phrases.size() == 0) {
				return documentToBeCheckedForPhrases;
			} else {
				while (documentToBeCheckedForPhrases != null) {
					boolean value = checkIfPhrasesPresent(
							documentToBeCheckedForPhrases._docid, phrases);
					if (!value) {
						docid = documentToBeCheckedForPhrases._docid;
						try {
							documentToBeCheckedForPhrases = nextDocToken(
									new Query(tokens.toString()), docid);
							// system.out.println("Returning from nextDocToken");
						} catch (IOException e) {
							e.printStackTrace();
						}

						continue;
					} else {
						return documentToBeCheckedForPhrases;
					}

				}
			}
		} catch (IOException e) {
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
		WordAttribute_WordOccurrences smallestListOccr = wordMapUncompressed
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
				if (!checkInCache(currentDocId)) {
					loadDocInCache(currentDocId);
				}
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

		WordAttribute_WordOccurrences currentWordAttribute_WordOccurrences = wordMapUncompressed
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

	private void loadDocInCache(int did) throws IOException {
		try {
			if (docMap.containsKey(did))
				return;

			float t = _numDocs / 20;
			int maxEntries = (int) t;

			if (docMap.size() > maxEntries) {
				Iterator<Integer> iter = docMap.keySet().iterator();
				int temp = iter.next();
				docMap.remove(temp);
			}

			int fileNum = (int) (did / t);
			StringBuilder builder = new StringBuilder(_options._indexPrefix)
					.append("/").append("doc_map_").append(fileNum)
					.append(".csv");
			String doc_mapFile = builder.toString();
			List<String> commands = new ArrayList<String>();
			commands.add("/bin/bash");
			commands.add("-c");
			commands.add("grep $'^" + did + "\t' " + doc_mapFile);
			ProcessBuilder pb = new ProcessBuilder(commands);
			Process p = pb.start();
			BufferedReader ois = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			String o;
			while (((o = ois.readLine()) != null)) {
				String[] eachLine = o.split("\t");
				int docid = Integer.parseInt(eachLine[0]);
				DocumentIndexed wa = new DocumentIndexed(docid);
				String title = eachLine[1];
				String url = eachLine[2];
				int numViews = Integer.parseInt(eachLine[3]);
				float pageRank = Float.parseFloat(eachLine[4]);

				HashMap<String, Integer> temp_map = new HashMap<String, Integer>();

				int index = 5;
				while (index < eachLine.length - 3) {
					String temp_word = eachLine[index];
					index++;
					int temp_freq = Integer.parseInt(eachLine[index]);
					index++;
					temp_map.put(temp_word, temp_freq);
				}

				int totalWords = Integer
						.parseInt(eachLine[eachLine.length - 1]);

				wa.setTitle(title);
				wa.setUrl(url);
				wa.setTotalWords(totalWords);
				wa.setNumViews(numViews);
				wa.setPageRank(pageRank);
				wa.setWordFrequency(temp_map);
				docMap.put(docid, wa);
			}
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean loadInCache(String word, Query query) throws IOException {
		if (wordMapUncompressed.containsKey(word))
			return true;

		List<String> to_be_removed = new ArrayList<String>();
		int k = 0;
		for (String s : wordMapUncompressed.keySet()) {
			if (k >= query._tokens.size())
				break;
			if (!query._tokens.contains(s))
				to_be_removed.add(s);
			k++;
		}

		for (String s : to_be_removed) {
			wordMapUncompressed.remove(s);
		}
		boolean flag = false;
		int firstLetter = Math.abs(word.hashCode()) % 199;

		Scanner scanner = new Scanner(new FileReader(_options._indexPrefix
				+ "/" + firstLetter + ".csv"));
		String line = "";
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			if (line.split("\t")[0].equals(word)) {
				flag = true;
				break;
			}
		}
		if (flag) {
			String[] eachLine = line.split("\t");
			String key = eachLine[0];
			WordAttribute_WordOccurrences wa = new WordAttribute_WordOccurrences();
			LinkedHashMap<Integer, ArrayList<Integer>> currMap = new LinkedHashMap<Integer, ArrayList<Integer>>();
			char[] tempCH = eachLine[eachLine.length - 1].toCharArray();
			Vector<Character> tempVectorArray = new Vector<Character>();
			for (char c : tempCH)
				tempVectorArray.add(c);
			int freq = DecompressionUtility.decodeByteAlign(tempVectorArray)
					.get(0);
			wa.setFreq(freq);
			int i = 1;
			while (i < eachLine.length - 1) {
				char[] temp_char = eachLine[i].toCharArray();
				Vector<Character> tempVectorArray_char = new Vector<Character>();
				for (char c : temp_char)
					tempVectorArray_char.add(c);

				List<Integer> tempList = DecompressionUtility
						.decodeByteAlign(tempVectorArray_char);
				int did = tempList.get(0);

				int frequencyInDoc = tempList.size();
				int ki = 1;
				int prev = 0;
				ArrayList<Integer> list = new ArrayList<Integer>();
				while (ki < frequencyInDoc) {
					int temp = tempList.get(ki);
					list.add(temp + prev);
					prev = temp;
					ki++;
				}
				currMap.put(did, list);
				i++;
			}
			wa.setList(currMap);
			wordMapUncompressed.put(key, wa);

			// flag = true;
		}
		scanner.close();
		return flag;
	}

	private boolean checkInCache(int docid) {
		return docMap.containsKey(docid);
	}

	private boolean loadInCache(String word) throws IOException {
		if (wordMapUncompressed.containsKey(word))
			return true;

		boolean flag = false;
		int firstLetter = Math.abs(word.hashCode()) % 199;
		List<String> commands = new ArrayList<String>();
		commands.add("/bin/bash");
		commands.add("-c");
		commands.add("grep $'^" + word + "\t' " + _options._indexPrefix + "/"
				+ firstLetter + ".csv");
		ProcessBuilder pb = new ProcessBuilder(commands);
		Process p = pb.start();
		BufferedReader ois = new BufferedReader(new InputStreamReader(
				p.getInputStream()));

		String o;
		if (((o = ois.readLine()) != null)) {
			String[] eachLine = o.split("\t");
			String key = eachLine[0];
			WordAttribute_WordOccurrences wa = new WordAttribute_WordOccurrences();
			LinkedHashMap<Integer, ArrayList<Integer>> currMap = new LinkedHashMap<Integer, ArrayList<Integer>>();
			char[] tempCH = eachLine[eachLine.length - 1].toCharArray();
			Vector<Character> tempVectorArray = new Vector<Character>();
			for (char c : tempCH)
				tempVectorArray.add(c);
			int freq = DecompressionUtility.decodeByteAlign(tempVectorArray)
					.get(0);
			wa.setFreq(freq);
			int i = 1;
			while (i < eachLine.length - 1) {
				char[] temp_char = eachLine[i].toCharArray();
				Vector<Character> tempVectorArray_char = new Vector<Character>();
				for (char c : temp_char)
					tempVectorArray_char.add(c);

				List<Integer> tempList = DecompressionUtility
						.decodeByteAlign(tempVectorArray_char);
				int did = tempList.get(0);

				int frequencyInDoc = tempList.size();
				int ki = 1;
				int prev = 0;
				ArrayList<Integer> list = new ArrayList<Integer>();
				while (ki < frequencyInDoc) {
					int temp = tempList.get(ki);
					list.add(temp + prev);
					prev = temp;
					ki++;
				}
				currMap.put(did, list);
				i++;
			}
			wa.setList(currMap);
			wordMapUncompressed.put(key, wa);

			flag = true;
		}
		ois.close();

		return flag;
	}

	private boolean isPresentAtPosition(int position, int docid, String string) {

		WordAttribute_WordOccurrences currentWordAttribute_WordOccurrences = wordMapUncompressed
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

		// First find out the smallest list among the list of all the words
		String smallestListWord = findWordWithSmallestList(query);
		// Now take a next docId form the list of the smallestListWord
		WordAttribute_WordOccurrences smallestWordAttribute_WordOccurrences = wordMapUncompressed
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
			int currentDocId = currentMap.getKey();
			if (currentDocId <= docid) {
				continue;
			}
			boolean value = isPresentInAll(currentDocId, smallestListWord,
					query);
			if (value == true) {

				if (!checkInCache(currentDocId)) {
					try {
						loadDocInCache(currentDocId);
						// system.out.println("here?");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				// system.out.println("Hi");
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
				// //system.out.println("in isPresentInAll first if");
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
		WordAttribute_WordOccurrences currentWordAttribute_WordOccurrences = wordMapUncompressed
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
				flag = loadInCache(strTemp, query);
				if (flag == false)
					continue;
			}
			WordAttribute_WordOccurrences currentWordAttribute_WordOccurrences = wordMapUncompressed
					.get(strTemp);
			int mapSize = currentWordAttribute_WordOccurrences.getList().size();
			if (minListLength > mapSize) {
				minListLength = mapSize;
				smallestListWord = strTemp;
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

		return wordMapUncompressed.get(term).getList().size();
	}

	private boolean isPresentInCache(String term) {
		return wordMapUncompressed.containsKey(term);
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
		return wordMapUncompressed.get(term).getFreq();
	}

	/**
	 * @CS2580: Implement this for bonus points.
	 */
	@Override
	public int documentTermFrequency(String term, String url) {
		int did = Integer.parseInt(url);

		if (!isPresentInCache(term)) {
			boolean flag;
			try {
				flag = loadInCache(term);
				if (flag == false)
					return 0;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (wordMapUncompressed.get(term).getList().get(did) != null)
			return wordMapUncompressed.get(term).getList().get(did).size();

		else
			return 0;
		// int output = wordMapUncompressed.get(term).getList().get(did).size();
		// return output;
	}

}
