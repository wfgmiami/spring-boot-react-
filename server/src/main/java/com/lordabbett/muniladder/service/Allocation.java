package com.lordabbett.muniladder.service;

import com.lordabbett.muniladder.model.FileLoader;
import com.lordabbett.muniladder.model.Security;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Service
public class Allocation {

    private static final int MIN_PRICE = 100;
    private static final int MAX_PRICE_ONE = 105;
    private static final int MAX_PRICE_OTHER = 112;
    private static final int MIN_NUM_BONDS = 10;
    private static final long MIN_INCREMENT = 5000;
    private static final int PAR_PRICE = 100;
    public static final String SECTOR_HEALTHCARE = "Health Care";
    public static final String STATE_NY = "NY";
    public static final String STATE_CA = "CA";
    public static final double MAX_HEALTHCARE_PCT = 12;
    public static final double MAX_STATE_PCT = 20;
    public static final double MAX_SECTOR_PCT = 30;
    public static final double MAX_A_OR_BELOW_PCT = 30;
    public static final double MAX_SECTOR_STATE_PCT = 10;

    List<Security> bonds = new FileLoader().getSecList();
    private int min;
    private int max;

    private ArrayList<Integer> buckets = new ArrayList<Integer>();

    @PostConstruct
    public void init() throws IOException{
        FileLoader fileLoader = new FileLoader();
        fileLoader.loadFile();
    }

    public Collection<Security> buckets(HashMap<String,String> queryMap){

    	SortedMap<Integer, Map<String,List<Security>>> bucketSecMap = new TreeMap<Integer, Map<String,List<Security>>>();
    	
        max = Integer.valueOf(queryMap.get("max"));
        min = Integer.parseInt(queryMap.get("min"));
        for(int i = min; i <= max; i++){
            buckets.add(i);
        }

        List<Security> filteredBonds = this.bonds.stream()
            .filter(this::filterBonds)
            .collect(Collectors.toList());
        
        Map<String, List<Security>> groupedByRanking = new HashMap<String, List<Security>>();
        
        for(int bucket:buckets){
        	List<Security> bucketSecurity = filteredBonds.stream()
        			.filter( bond -> bond.getYearsToMaturity() == bucket)
        			.collect(Collectors.toList());
        	List<Security> healthCareBonds = bucketSecurity.stream()
        			.filter( bond -> bond.getSector().equals(SECTOR_HEALTHCARE))
        			.collect(Collectors.toList());
        	List<Security> nyBonds =  bucketSecurity.stream()
        			.filter( bond -> bond.getState().equals(STATE_NY))
        			.collect(Collectors.toList());
        	List<Security> caBonds =  bucketSecurity.stream()
        			.filter( bond -> bond.getState().equals(STATE_CA))
        			.collect(Collectors.toList());
        	List<Security> aRatedBonds =  bucketSecurity.stream()
        			.filter( bond -> bond.getTwoGroupsRating().equals(FileLoader.SecRating.A_OR_BELOW))
        			.collect(Collectors.toList());
        	List<Security> aaRatedBonds =  bucketSecurity.stream()
        			.filter( bond -> bond.getTwoGroupsRating().equals(FileLoader.SecRating.ABOVE_A))
        			.collect(Collectors.toList());
        	
        	groupedByRanking.put("HealthCare", healthCareBonds);
        	groupedByRanking.put("nyBonds", nyBonds);
        	groupedByRanking.put("caBonds",caBonds);
        	groupedByRanking.put("aRatedBonds", nyBonds);
        	groupedByRanking.put("aaRatedBonds",caBonds);
        	
        	bucketSecMap.put(bucket,groupedByRanking);
        }
        
        //Collections.sort(filteredBonds);
        
//
//        for(Security sec: filteredBonds){
//            List<Security> secList = bucketSecMap.get(sec.getYearsToMaturity());
//            if(secList == null){
//                secList = new ArrayList<Security>();
//                bucketSecMap.put(sec.getYearsToMaturity(), secList);
//            }
//            secList.add(sec);
//        }
//        for(Map.Entry<Integer,List<Security>> entry: bucketSecMap.entrySet()){
//        	System.out.println(".................");
//        	System.out.println(Integer.toString(entry.getKey()));
//        	for(Security sec:entry.getValue()){
//        	}
//        }
        return filteredBonds;
    }



    private boolean filterBonds(Security sec){
        int yearsToMat = sec.getYearsToMaturity();
        double price = sec.getPrice();

        if(buckets.get(0).equals(1)){
            return yearsToMat<= max && yearsToMat >= min && price >= MIN_PRICE && price <= MAX_PRICE_ONE;
        }
        return yearsToMat <= max && yearsToMat >= min && price <= MAX_PRICE_OTHER;
    }
    
    private abstract class ConstraintEvaluator implements Observer{
    
    	
    }
}
