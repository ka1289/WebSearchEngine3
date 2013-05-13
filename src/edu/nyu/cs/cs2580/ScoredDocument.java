package edu.nyu.cs.cs2580;

/**
 * Document with score.
 * 
 * @author fdiaz
 * @author congyu
 */
class ScoredDocument implements Comparable<ScoredDocument> {
	private Document _doc;
	private double _score;
	private float _pageRank;
	private int _numViews;

	public ScoredDocument(Document doc, double score, float pageRank,
			int numViews) {
		_doc = doc;
		_score = score;
		_pageRank = pageRank;
		_numViews = numViews;
	}

	public int getDocId() {
		return _doc._docid;
	}
	public String asTextResult() {
		StringBuffer buf = new StringBuffer();
		buf.append("DocId: ").append(_doc._docid).append("\t");
		buf.append("Title: ").append(_doc.getTitle()).append("\t");
		buf.append("Score: ").append(_score).append("\t");
		buf.append("PageRank: ").append(_pageRank).append("\t");
		buf.append("NumViews: ").append(_numViews);

		return buf.toString();
	}

	/**
	 * @CS2580: Student should implement {@code asHtmlResult} for final project.
	 */
	public String asHtmlResult() {
		return "";
	}

	@Override
	public int compareTo(ScoredDocument o) {
		if (this._score == o._score) {
			return 0;
		}
		return (this._score > o._score) ? 1 : -1;
	}
}
