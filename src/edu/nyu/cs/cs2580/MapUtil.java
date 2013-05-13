package edu.nyu.cs.cs2580;

import java.util.*;
import java.util.Map.Entry;

public class MapUtil {
	
	static TreeMap<String, Double> sortByValue(Map<String, Double> probabilityMap, int numTermsM) {
		ValueComparator bvc = new ValueComparator(probabilityMap);
		TreeMap<String, Double> tmp = new TreeMap<String, Double>(bvc);
		TreeMap<String, Double> sortedMap = new TreeMap<String, Double>(bvc);
		tmp.putAll(probabilityMap);
		int count = 0;
		try {
			for (Entry<String, Double> m : tmp.entrySet()) {
				if (count < numTermsM) {
					sortedMap.put(m.getKey(), m.getValue());
					count++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sortedMap;
	}

	public static class ValueComparator implements Comparator<String> {

		Map<String, Double> base;

		public ValueComparator(Map<String, Double> base) {
			this.base = base;
		}

		// Note: this comparator imposes orderings that are inconsistent with
		// equals.
		public int compare(String a, String b) {
			if (base.get(a) >= base.get(b)) {
				return -1;
			} else {
				return 1;
			} // returning 0 would merge keys
		}
	}
}