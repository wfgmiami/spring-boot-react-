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
//13 fields
    private String cusip;
    private double price;
    private double coupon;
    private String maturityDate;
    private int yearsToMaturity;
    private String sector;
    private String rating;
    private FileLoader.SecRating twoGroupsRating;
    private String state;
    private String latestTraded;
    private Date lastTraded;
    private double effDur;
    private double modDur;
    private double yieldToWorst;
  
    
    public Security(){}

    public SecPriority getPriority(){
        if(!FileLoader.getDefaultDate().equals(lastTraded)) {
            if (FileLoader.SecRating.ABOVE_A == twoGroupsRating) {
                if (Allocation.SECTOR_HEALTHCARE.equals(sector)) {
                    if (Allocation.STATE_NY.equals(state)) {
                        return SecPriority.ABOVE_A_HEALTHCARE_NY;
                    } else if (Allocation.STATE_CA.equals(state)) {
                        return SecPriority.ABOVE_A_HEALTHCARE_CA;
                    } else {
                        return SecPriority.ABOVE_A_HEALTHCARE_NON_NY_CA;
                    }
                } else {
                    if (Allocation.STATE_NY.equals(state)) {
                        return SecPriority.ABOVE_A_NY;
                    } else if (Allocation.STATE_CA.equals(state)) {
                        return SecPriority.ABOVE_A_CA;
                    } else {
                        return SecPriority.ABOVE_A_NON_NY_CA;
                    }
                }
            } else {
                if (Allocation.SECTOR_HEALTHCARE.equals(sector)) {
                    if (Allocation.STATE_NY.equals(state)) {
                        return SecPriority.A_OR_BELOW_HEALTHCARE_NY;
                    } else if (Allocation.STATE_CA.equals(state)) {
                        return SecPriority.A_OR_BELOW_HEALTHCARE_CA;
                    } else {
                        return SecPriority.A_OR_BELOW_HEALTHCARE_NON_NY_CA;
                    }
                } else {
                    if (Allocation.STATE_NY.equals(state)) {
                        return SecPriority.A_OR_BELOW_NY;
                    } else if (Allocation.STATE_CA.equals(state)) {
                        return SecPriority.A_OR_BELOW_CA;
                    } else {
                        return SecPriority.A_OR_BELOW_NON_NY_CA;
                    }
                }
            }
        }else{
            return SecPriority.NO_PRIORITY;
        }
    }
 
//    public String getLastTraded(){
//    	return FileLoader.DATE_FORMAT.format(lastTraded);
//    }

    public int compareTo(Object o){
    	return ((Security)o).lastTraded.compareTo(lastTraded);
    }

    private enum SecPriority     {
        ABOVE_A_HEALTHCARE_NY,
        ABOVE_A_HEALTHCARE_CA,
        ABOVE_A_HEALTHCARE_NON_NY_CA,
        ABOVE_A_NY,
        ABOVE_A_CA,
        ABOVE_A_NON_NY_CA,
        A_OR_BELOW_HEALTHCARE_NY,
        A_OR_BELOW_HEALTHCARE_CA,
        A_OR_BELOW_HEALTHCARE_NON_NY_CA,
        A_OR_BELOW_NY,
        A_OR_BELOW_CA,
        A_OR_BELOW_NON_NY_CA,
        NO_PRIORITY;
    }

}


