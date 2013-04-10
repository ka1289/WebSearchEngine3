package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class LogMinerNumviews extends LogMiner {
	private Map<String, Integer> numViews = new HashMap<String, Integer>();

	public LogMinerNumviews(Options options) {
		super(options);
	}

	/**
	 * This function processes the logs within the log directory as specified by
	 * the {@link _options}. The logs are obtained from Wikipedia dumps and have
	 * the following format per line: [language]<space>[article]<space>[#views].
	 * Those view information are to be extracted for documents in our corpus
	 * and stored somewhere to be used during indexing.
	 * 
	 * Note that the log contains view information for all articles in Wikipedia
	 * and it is necessary to locate the information about articles within our
	 * corpus.
	 * 
	 * @throws IOException
	 */
	@Override
	public void compute() throws IOException {
		try {
			File corpusDir = new File(_options._corpusPrefix);
			File[] listOfFiles = corpusDir.listFiles();
			int noOfFiles = listOfFiles.length;
			
			numViews = new HashMap<String, Integer>(noOfFiles);
			
			for (File eachFile : listOfFiles) {
				if (!eachFile.getName().startsWith("."))
					numViews.put(eachFile.getName(), 0);
			}

			String path = _options._logPrefix;
			Scanner sc = new Scanner(new FileReader(path
					+ "/20130301-160000.log"));
			String line = null;
			while (sc.hasNextLine()) {
				line = sc.nextLine();
				String[] split = line.split(" ");
				if (split.length > 2) {
					try {
						String url = URLDecoder
								.decode(split[1].trim(), "UTF-8");
						int num = Integer.parseInt(split[2].trim());
						if (numViews.containsKey(url))
							numViews.put(url, num);
					} catch (IllegalArgumentException e) {
					}
				}
			}
			sc.close();
			

		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		StringBuilder builder = new StringBuilder(_options._indexPrefix)
				.append("/").append("numViews.csv");
		BufferedWriter aoos = new BufferedWriter(new FileWriter(
				builder.toString(), true));
		for (String url : numViews.keySet()) {
			int docid = _options._docMap.get(url);
			aoos.write(url + " ");
			long num = numViews.get(url);
			aoos.write(num + " ");
			aoos.write(docid + "");
			aoos.newLine();
		}
		aoos.close();
		numViews.clear();
		return;
	}

	/**
	 * During indexing mode, this function loads the NumViews values computed
	 * during mining mode to be used by the indexer.
	 * 
	 * @throws IOException
	 */
	@Override
	public Object load() throws IOException {
		BufferedReader ois = new BufferedReader(new FileReader(
				_options._indexPrefix + "/numViews.csv"));
		String o;
		int i = 0;

		Map<Integer, Integer> numViews_withDocid = new HashMap<Integer, Integer>();
		while (((o = ois.readLine()) != null) && i < 2000) {
			String[] eachLine = o.split(" ");
			int docid = Integer.parseInt(eachLine[2]);
			int temp = Integer.parseInt(eachLine[1]);
			numViews_withDocid.put(docid, temp);
			i++;
		}
		ois.close();
		return numViews_withDocid;
	}
}
