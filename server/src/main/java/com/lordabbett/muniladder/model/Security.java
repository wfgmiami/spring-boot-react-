package com.lordabbett.muniladder.model;

import com.lordabbett.muniladder.service.Allocation;


import lombok.Data;
import lombok.ToString;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class Security implements Comparable{
//16 fields
    private String cusip;
    private double price;
    private double coupon;
    private double investAmt;
    private String maturityDate;
    private int yearsToMaturity;
    private String sector;
    private String rating;
    private String spRating;
    private String moodyRating;
    private String fitchRating;
    private FileLoader.SecRating twoGroupsRating;
    private String state;
    private String rank;
    private String latestTraded;
    private Date lastTraded;
    private double effDur;
    private double modDur;
    private double yieldToWorst;
  
    public Security(){}

    public Allocation.SecPriority getPriority(){
        if(!FileLoader.getDefaultDate().equals(lastTraded)) {
            if (FileLoader.SecRating.ABOVE_A == twoGroupsRating) {
                if (Allocation.SECTOR_HEALTHCARE.equals(sector)) {
                    if (Allocation.STATE_NY.equals(state)) {
                        return Allocation.SecPriority.ABOVE_A_HEALTHCARE_NY;
                    } else if (Allocation.STATE_CA.equals(state)) {
                        return Allocation.SecPriority.ABOVE_A_HEALTHCARE_CA;
                    } else {
                        return Allocation.SecPriority.ABOVE_A_HEALTHCARE_NON_NY_CA;
                    }
                } else {
                    if (Allocation.STATE_NY.equals(state)) {
                        return Allocation.SecPriority.ABOVE_A_NY;
                    } else if (Allocation.STATE_CA.equals(state)) {
                        return Allocation.SecPriority.ABOVE_A_CA;
                    } else {
                        return Allocation.SecPriority.ABOVE_A_NON_NY_CA;
                    }
                }
            } else {
                if (Allocation.SECTOR_HEALTHCARE.equals(sector)) {
                    if (Allocation.STATE_NY.equals(state)) {
                        return Allocation.SecPriority.A_OR_BELOW_HEALTHCARE_NY;
                    } else if (Allocation.STATE_CA.equals(state)) {
                        return Allocation.SecPriority.A_OR_BELOW_HEALTHCARE_CA;
                    } else {
                        return Allocation.SecPriority.A_OR_BELOW_HEALTHCARE_NON_NY_CA;
                    }
                } else {
                    if (Allocation.STATE_NY.equals(state)) {
                        return Allocation.SecPriority.A_OR_BELOW_NY;
                    } else if (Allocation.STATE_CA.equals(state)) {
                        return Allocation.SecPriority.A_OR_BELOW_CA;
                    } else {
                        return Allocation.SecPriority.A_OR_BELOW_NON_NY_CA;
                    }
                }
            }
        }else{
            return Allocation.SecPriority.NO_PRIORITY;
        }
    }
 
//    public int compareTo(Object o){
//    	return ((Security)o).lastTraded.compareTo(lastTraded);
//    }
    
//    public int compareTo(Object o){
//    	 
//	     int secSortOrder = getPriority().ordinal();
//	     int sortOrder = ((Security)o).getPriority().ordinal();
//		 if(secSortOrder > sortOrder){
//			 return 1;
//		 }else if(secSortOrder < sortOrder){
//		     return -1;
//		 }else{
//		     return ((Security)o).lastTraded.compareTo(lastTraded);
//		 }
//    	 
//    }
    
    public int compareTo(Object o) {
    
	 	int secSortOrder = Allocation.secPriorityList.indexOf(getPriority());
		int sortOrder = Allocation.secPriorityList.indexOf(((Security)o).getPriority());
		if(secSortOrder > sortOrder){
			return 1;
		}
		else if(secSortOrder < sortOrder){
			return -1;
		}
		else{
			return ((Security)o).lastTraded.compareTo(lastTraded);
		}
	
	} 

}


