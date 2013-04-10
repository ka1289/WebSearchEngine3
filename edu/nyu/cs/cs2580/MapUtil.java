package edu.nyu.cs.cs2580;

import java.util.*;
import java.util.Map.Entry;

public class MapUtil {
	
	static Map<String, Integer> sortByValue(Map<String, Integer> map, int numTermsM) {
		ValueComparator bvc = new ValueComparator(map);
		TreeMap<String, Integer> tmp = new TreeMap<String, Integer>(bvc);
		TreeMap<String, Integer> sortedMap = new TreeMap<String, Integer>(bvc);
		tmp.putAll(map);
		int count = 0;
		try {
			for (Entry<String, Integer> m : tmp.entrySet()) {
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

		Map<String, Integer> base;

		public ValueComparator(Map<String, Integer> base) {
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