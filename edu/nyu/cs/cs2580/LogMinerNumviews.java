package edu.nyu.cs.cs2580;

import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class LogMinerNumviews extends LogMiner {
	private Map<String, Long> numViews = new HashMap<String, Long>();

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
		String path = _options._logPrefix;
		Scanner sc = new Scanner(new FileReader(path + "/20130301-160000.log"));
		String line = null;
		while (sc.hasNextLine()) {
			line = sc.nextLine();
			String[] split = line.split(" ");
			try {
				long num = Long.parseLong(split[2].trim());
				String url = URLDecoder.decode(split[1].trim(), "UTF-8");
				numViews.put(url, num);
			} catch (IllegalArgumentException e) {
				//TODO
			}
		}
		sc.close();
		
		
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
		System.out.println("Loading using " + this.getClass().getName());
		return null;
	}
}
