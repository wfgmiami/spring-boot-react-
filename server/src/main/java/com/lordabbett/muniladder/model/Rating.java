package com.lordabbett.muniladder.model;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;

/**
 * <p>Immutable enumeration representing a Moodys or Standard and Poors Rating.
 * Includes string input/output and numerical mapping. </p>
 *
 * <p>Department: Taxable Fixed Income</p>
 * <p>Last Mod: $Author:   gsingh  $</p>
 * <p>Update Time: $Modtime:   Jul 24 2012 16:50:22  $</p>
 *
 * @author gsingh
 * @version $Revision:   1.3  $
 */
public class Rating implements Serializable, Comparable {

	static final long serialVersionUID = 93743823076488565L;

	public static final Rating GOV = new Rating("GOV", "GOV", "GOV", 1);
	public static final Rating AGN = new Rating("AGN", "AGN", "AGN", 1.5);
	public static final Rating AAA = new Rating("Aaa", "AAA", "AAA", 2);
	public static final Rating AA1 = new Rating("Aa1", "AA+", "AA+", 3);
	public static final Rating AA2 = new Rating("Aa2", "AA", "AA", 4);
	public static final Rating AA3 = new Rating("Aa3", "AA-", "AA-", 5);
	public static final Rating A1 = new Rating("A1", "A+", "A+", 6);
	public static final Rating A2 = new Rating("A2", "A", "A", 7);
	public static final Rating A3 = new Rating("A3", "A-", "A-", 8);
	public static final Rating BAA1 = new Rating("Baa1", "BBB+", "BBB+", 9);
	public static final Rating BAA2 = new Rating("Baa2", "BBB", "BBB", 10);
	public static final Rating BAA3 = new Rating("Baa3", "BBB-", "BBB-", 11);
	public static final Rating BA1 = new Rating("Ba1", "BB+", "BB+", 12);
	public static final Rating BA2 = new Rating("Ba2", "BB", "BB", 13);
	public static final Rating BA3 = new Rating("Ba3", "BB-", "BB-", 14);
	public static final Rating B1 = new Rating("B1", "B+", "B+", 15);
	public static final Rating B2 = new Rating("B2", "B", "B", 16);
	public static final Rating B3 = new Rating("B3", "B-", "B-", 17);
	public static final Rating CAA1 = new Rating("Caa1", "CCC+", "CCC+", 18);
	public static final Rating CAA2 = new Rating("Caa2", "CCC", "CCC", 19);
	public static final Rating CAA3 = new Rating("Caa3", "CCC-", "CCC-", 20);
	public static final Rating CA = new Rating("Ca", "CC", "CC", 21);
	public static final Rating C = new Rating("C", "C", "C", 22);
	public static final Rating D = new Rating("D", "D", "D", 23);
	public static final Rating NR = new Rating("NR", "NR", "NR", 24);

	/** Array of ratings. */
	public static final Rating[] s_ratings = new Rating[] {
		GOV, AGN, AAA, AA1, AA2, AA3, A1, A2, A3, BAA1, BAA2, BAA3, BA1, BA2, BA3,
		B1, B2, B3, CAA1, CAA2, CAA3, CA, C, D, NR};

	
	public static final Rating[] Corporate_ratings = new Rating[] {
		  AAA, AA1, AA2, AA3, A1, A2, A3, BAA1, BAA2, BAA3, BA1, BA2, BA3,
		B1, B2, B3, CAA1, CAA2, CAA3, CA, C, D, NR };
	
	/**
	 * Returns a rating object for a String in Moody or SP format.
	 * @param rateString the rating as a string
	 * @return Rating
	 */
	public static Rating valueOf(String rateString) {
		if (rateString == null) {
			return NR;
		}
		for (int i = 0; i < s_ratings.length; i++) {
			if (s_ratings[i].getMoodyString().equalsIgnoreCase(rateString) ||
				s_ratings[i].getSPString().equalsIgnoreCase(rateString) ||
				s_ratings[i].getFitchString().equalsIgnoreCase(rateString)) {
				return s_ratings[i];
			}
		}
		return NR;
	}

	/**
	 * Returns the Rating object based on the QIndex.
	 * @param qIndex double
	 * @return Rating
	 */
	public static Rating valueOf(double qIndex) {
		for (int i = 0; i < s_ratings.length; i++) {
			if (s_ratings[i].getQIndex() == qIndex) {
				return s_ratings[i];
			}
		}
		return NR;
	}

	// Private constructor
	protected Rating(String moodyRating, String spRating, String fitchRating, double qIndex) {
		m_moodyString = moodyRating;
		m_spString = spRating;
		m_fitchString = fitchRating;
		m_qIndex = qIndex;
	}

	/*
	 * Necessary for serialization.
	 */
	private Object readResolve() throws ObjectStreamException {
		return valueOf(m_moodyString);
	}

	/**
	 * Returns the default String representation.  (S&P Rating)
	 * @return strings
	 */
	public String toString() {
		return getSPString();
	}

	/**
	 * Returns Moody representation for the rating.
	 * @return Moody's credit rating
	 */
	public String getMoodyString() {
		return m_moodyString;
	}

	/**
	 * Returns Standard and Poors representation for the rating.
	 * @return S&P's credit rating
	 */
	public String getSPString() {
		return m_spString;
	}

	/**
	 * Returns Fitch representation for the rating.
	 * @return Fitch's credit rating
	 */
	public String getFitchString() {
		return m_fitchString;
	}

	/**
	 * Returns numerical quality rating.
	 * @return 1 (GOV) to 24 (NR).
	 */
	public double getQIndex() {
		return m_qIndex;
	}

	/**
	 * Better rankings compare as greater.
	 * @param o Rating object
	 * @return -1, 0, 1 if less than, equal to or greater
	 */
	public int compareTo(Object o) {
		if (m_qIndex < ((Rating)o).getQIndex()) {
			return 1;
		}
		else if (m_qIndex == ((Rating)o).getQIndex()) {
			return 0;
		}
		else {
			return -1;
		}
	}

	/**
	 * Returns a String array of Moody's ratings
	 * @return String[]
	 */
	public static String[] getMoodyRatings() {
		String[] moodyRatings = new String[s_ratings.length];
		for (int i = 0; i < s_ratings.length; i++) {
			moodyRatings[i] = s_ratings[i].getMoodyString();
		}
		return moodyRatings;
	}

	/**
	 * Returns a String array of S&P ratings
	 * @return String[]
	 */
	public static String[] getSnPRatings() {
		String[] snpRatings = new String[s_ratings.length];
		for (int i = 0; i < s_ratings.length; i++) {
			snpRatings[i] = s_ratings[i].getSPString();
		}
		return snpRatings;
	}

	/**
	 * Returns a String array of Fitch ratings
	 * @return String[]
	 */
	public static String[] getFitchRatings() {
		String[] fitchRatings = new String[s_ratings.length];
		for (int i = 0; i < s_ratings.length; i++) {
			fitchRatings[i] = s_ratings[i].getFitchString();
		}
		return fitchRatings;
	}

	/**
	 * Determines if the object passed is is equal to this object.
	 * @param obj Object the Rating object
	 * @return boolean true if equal, false if not
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof Rating)) {
			return false;
		}
		Rating trd = (Rating)obj;
		return m_qIndex == trd.m_qIndex;
	}

	/**
	 * returns the next higher rating than the given rating
	 * If the given rating is NR, AAA, GOV or AGN return the same rating
	 * @param ratingString
	 * @return
	 */
	public static Rating getNextHigherRating(String ratingString){
		Rating rating = valueOf(ratingString) ;
		if(rating.getQIndex() <= 2 || rating == NR){
			return rating;
		}
		else{
			return valueOf(rating.getQIndex() - 1);
		}
	}
	
	/**
	 * returns the next lower rating than the given rating
	 * If the given rating is NR or D return the same rating
	 * @param ratingString
	 * @return
	 */
	public static Rating getNextLowerRating(String ratingString){
		Rating rating = valueOf(ratingString) ;
		if(rating.getQIndex() >= 23){
			return rating;
		}
		else{
			return valueOf(rating.getQIndex() + 1);
		}
	}

	 /**
	  * Return the average rating rounded down to nearest rating
	  * @param spRating
	  * @param moodyRating
	  * @param fitchRating
	  * @param useSnp
	  * @param useMoody
	  * @param useFitch
	  * @return
	  */
	public static Rating getAverageRating(String spRating, String moodyRating, String fitchRating, boolean useSnp, boolean useMoody, boolean useFitch) {
		double spQIndex = !useSnp || Rating.valueOf(spRating) == Rating.NR ? 0 : Rating.valueOf(spRating).getQIndex();
		double moodyQIndex = !useMoody || Rating.valueOf(moodyRating) == Rating.NR ? 0 : Rating.valueOf(moodyRating).getQIndex();
		double fitchQIndex = !useFitch || Rating.valueOf(fitchRating) == Rating.NR ? 0 : Rating.valueOf(fitchRating).getQIndex();
		if (spQIndex != 0 && moodyQIndex != 0 && fitchQIndex != 0) {
			return Rating.valueOf(Math.ceil((spQIndex + moodyQIndex + fitchQIndex) / 3));
		}
		else if (spQIndex != 0 && moodyQIndex != 0 && fitchQIndex == 0) {
			return Rating.valueOf(Math.ceil((spQIndex + moodyQIndex) / 2));
		}
		else if (spQIndex != 0 && moodyQIndex == 0 && fitchQIndex != 0) {
			return Rating.valueOf(Math.ceil((spQIndex + fitchQIndex) / 2));
		}
		else if (spQIndex == 0 && moodyQIndex != 0 && fitchQIndex != 0) {
			return Rating.valueOf(Math.ceil((moodyQIndex + fitchQIndex) / 2));
		}
		else if (spQIndex != 0 && moodyQIndex == 0 && fitchQIndex == 0) {
			return Rating.valueOf(spQIndex);
		}
		else if (spQIndex == 0 && moodyQIndex != 0 && fitchQIndex == 0) {
			return Rating.valueOf(moodyQIndex);
		}
		else if (spQIndex == 0 && moodyQIndex == 0 && fitchQIndex != 0) {
			return Rating.valueOf(fitchQIndex);
		}
		else {
			return Rating.NR;
		}
	}
	
	/**
	 * Returns median rating
	 * @param spRating
	 * @param moodyRating
	 * @param fitchRating
	 * @param useSnp
	 * @param useMoody
	 * @param useFitch
	 * @return
	 */
	public static  Rating getMedianRating(String spRating, String moodyRating, String fitchRating, boolean useSnp, boolean useMoody, boolean useFitch) {
		double spQIndex = !useSnp || Rating.valueOf(spRating) == Rating.NR ? 0 : Rating.valueOf(spRating).getQIndex();
		double moodyQIndex = !useMoody || Rating.valueOf(moodyRating) == Rating.NR ? 0 : Rating.valueOf(moodyRating).getQIndex();
		double fitchQIndex = !useFitch || Rating.valueOf(fitchRating) == Rating.NR ? 0 : Rating.valueOf(fitchRating).getQIndex();
		// return middle one
		if (spQIndex != 0 && moodyQIndex != 0 && fitchQIndex != 0) {
			Double list[]= new Double[]{spQIndex,moodyQIndex,fitchQIndex};
			Arrays.sort(list);
			return Rating.valueOf(list[1]);
		}
		// return lowest rating 
		else if (spQIndex != 0 && moodyQIndex != 0 && fitchQIndex == 0) {
			return Rating.valueOf(spQIndex > moodyQIndex ? spQIndex : moodyQIndex);
		}
		else if (spQIndex != 0 && moodyQIndex == 0 && fitchQIndex != 0) {
			return Rating.valueOf(spQIndex > fitchQIndex ? spQIndex : fitchQIndex);
		}
		else if (spQIndex == 0 && moodyQIndex != 0 && fitchQIndex != 0) {
			return Rating.valueOf(moodyQIndex > fitchQIndex ? moodyQIndex : fitchQIndex);
		}
		else if (spQIndex != 0 && moodyQIndex == 0 && fitchQIndex == 0) {
			return Rating.valueOf(spQIndex);
		}
		else if (spQIndex == 0 && moodyQIndex != 0 && fitchQIndex == 0) {
			return Rating.valueOf(moodyQIndex);
		}
		else if (spQIndex == 0 && moodyQIndex == 0 && fitchQIndex != 0) {
			return Rating.valueOf(fitchQIndex);
		}
		else {
			return Rating.NR;
		}
	}
	
	
	
	private String m_moodyString = null;
	private String m_spString = null;
	private String m_fitchString = null;
	private double m_qIndex = 24;
}



class MoodyRating extends Rating {
	
	
	
	private MoodyRating(String moodyRating, String spRating, String fitchRating, double qIndex) {
		super(moodyRating, spRating, fitchRating, qIndex);
	}
	
	public static MoodyRating valueOf(String rateString) {
		Rating r =  Rating.valueOf(rateString);
		return new MoodyRating(r.getMoodyString(), r.getSPString(), r.getFitchString(), r.getQIndex());
	}
	
	public String toString(){
		return getMoodyString();
	}
	
		
	@Override
	public int hashCode() {
		return new Double(getQIndex()).intValue();
	}

}