package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {

	private HashMap<Integer, ArrayList<Integer>> graph = new HashMap<Integer, ArrayList<Integer>>();
	private HashMap<String, Integer> docMap = new HashMap<String, Integer>();
	private HashMap<Integer, Float> I = new HashMap<Integer, Float>();
	int index;
	int noOfFiles;
	private HashMap<Integer, Float> R = new HashMap<Integer, Float>();

	public CorpusAnalyzerPagerank(Options options) {
		super(options);
	}

	/**
	 * This function processes the corpus as specified inside {@link _options}
	 * and extracts the "internal" graph structure from the pages inside the
	 * corpus. Internal means we only store links between two pages that are
	 * both inside the corpus.
	 * 
	 * Note that you will not be implementing a real crawler. Instead, the
	 * corpus you are processing can be simply read from the disk. All you need
	 * to do is reading the files one by one, parsing them, extracting the links
	 * for them, and computing the graph composed of all and only links that
	 * connect two pages that are both in the corpus.
	 * 
	 * Note that you will need to design the data structure for storing the
	 * resulting graph, which will be used by the {@link compute} function.
	 * Since the graph may be large, it may be necessary to store partial graphs
	 * to disk before producing the final graph.
	 * 
	 * @throws IOException
	 */
	@Override
	public void prepare() throws IOException {
		File corpusDir = new File(_options._corpusPrefix);
		File[] listOfFiles = corpusDir.listFiles();
		noOfFiles = listOfFiles.length;
		initializeDocumentMap();

		for (File eachFile : listOfFiles) {

			if (isValidDocument(eachFile)) {
				ArrayList<Integer> adjList = new ArrayList<Integer>();
				int index_file = docMap.get(eachFile.getName());

				HeuristicLinkExtractor extractor = new HeuristicLinkExtractor(
						eachFile);
				String next = null;
				while ((next = extractor.getNextInCorpusLinkTarget()) != null) {
					if (docMap.containsKey(next)) {
						int temp = docMap.get(next);
						adjList.add(temp);
					}
				}
				graph.put(index_file, adjList);
			}
		}
		return;
	}

	private void initializeDocumentMap() {
		File corpusDir = new File(_options._corpusPrefix);
		File[] listOfFiles = corpusDir.listFiles();

		index = 1;
		for (File eachFile : listOfFiles) {
			String name = eachFile.getName();
			docMap.put(name, index);
			index++;
		}
	}

	/**
	 * This function computes the PageRank based on the internal graph generated
	 * by the {@link prepare} function, and stores the PageRank to be used for
	 * ranking.
	 * 
	 * Note that you will have to store the computed PageRank with each document
	 * the same way you do the indexing for HW2. I.e., the PageRank information
	 * becomes part of the index and can be used for ranking in serve mode.
	 * Thus, you should store the whatever is needed inside the same directory
	 * as specified by _indexPrefix inside {@link _options}.
	 * 
	 * @throws IOException
	 */
	@Override
	public void compute() throws IOException {
		int i = 1;
		int noOfPages = index - 1;
		while (i < index) {
			float probability_P = (1 / noOfPages);
			I.put(i, probability_P);
			i++;
		}

		int iterations = 0;
		float lambda = 0.90f;

		while (iterations < 2) {

			float random_selection = lambda / noOfPages;

			for (int entry : I.keySet()) {
				R.put(entry, random_selection);
			}

			for (int docid : graph.keySet()) {
				ArrayList<Integer> Q = graph.get(docid);

				if (Q.size() > 0) {
					for (int page : Q) {
						float Rq = R.get(page);
						float Ip = I.get(docid);
						Rq = Rq + (((1 - lambda) * Ip) / Q.size());
						R.put(page, Rq);
					}
				} else {
					for (int page : I.keySet()) {
						float Rq = R.get(page);
						float Ip = I.get(docid);
						float Rp = Rq + (((1 - lambda) * Ip) / I.size());
						R.put(page, Rp);
					}
				}
				I = R;
			}
			iterations++;
		}

		serializeR();
		return;
	}

	private void serializeR() throws IOException {
		StringBuilder builder = new StringBuilder(_options._indexPrefix)
				.append("/").append("pageRanks.csv");
		BufferedWriter aoos = new BufferedWriter(new FileWriter(
				builder.toString(), true));
		for (int docid : R.keySet()) {
			aoos.write(docid + " ");
			float pageRankValue = R.get(docid);
			aoos.write(pageRankValue + "");
			aoos.newLine();
		}
		aoos.close();
		R.clear();
	}

	/**
	 * During indexing mode, this function loads the PageRank values computed
	 * during mining mode to be used by the indexer.
	 * 
	 * @throws IOException
	 */
	@Override
	public Object load() throws IOException {
		StringBuilder builder = new StringBuilder(_options._indexPrefix)
				.append("/").append("pageRanks.csv");
		BufferedReader ois = new BufferedReader(new FileReader(
				builder.toString()));
		String o;
		int i = 0;
		while (((o = ois.readLine()) != null) && i < 2000) {
			String[] eachLine = o.split(" ");
			int tmp = Integer.parseInt(eachLine[0]);
			float temp = Float.parseFloat(eachLine[1]);
			R.put(tmp, temp);
			i++;
		}
		ois.close();
		return R;
	}
}
