package com.lordabbett.muniladder.service;

import com.lordabbett.muniladder.model.FileLoader;
import com.lordabbett.muniladder.model.Rating;
import com.lordabbett.muniladder.model.Security;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.Collections;

@Component
@Service
public class Allocation {
	
	private static final String NEW_LINE = System.getProperty("line.separator");
    private static final DecimalFormat PCT_FORMAT =  new DecimalFormat("#0.00%");
    private static final DecimalFormat PRICE_FORMAT =  new DecimalFormat("#0.00");
    private static final DecimalFormat ALLOC_FORMAT =  new DecimalFormat("##,##0");
    private static final int CURRENT_YEAR = Integer.valueOf(new SimpleDateFormat("yy").format(Calendar.getInstance().getTime()));
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
    public static final String[] RANKING = { "HealthCare", "nyBonds", "caBonds", "aRatedBonds", "aaRatedBonds", "couponRated"};
    public static final int MIN_PAR = 45000;
    public static final int MAX_PAR = 90000;
    public static final int STARTING_PAR = 70000;
    public static final int CASH_REDUCTION_PAR = 20000;
    public static final double MAX_BOND_PCT = 10;
	public static List<SecPriority> secPriorityList = new ArrayList<SecPriority>();
    private ArrayList<Integer> buckets = new ArrayList<Integer>();  	
    private HashMap<String, Object> tempObj = new HashMap<String, Object>();
    private boolean debug = true;
    
    List<Security> bonds = new FileLoader().getSecList();
    private int min;
    private int max;
    
    @PostConstruct
    public void init() throws IOException{
        FileLoader fileLoader = new FileLoader();
        fileLoader.loadFile();
    }

    ArrayList<Integer> getBuckets(){
    	ArrayList<Integer> copyOfBuckets = new ArrayList<Integer>(buckets);
    	return copyOfBuckets;
    }
    
    ArrayList<Integer> getParAmount(){
    	ArrayList<Integer> parList = new ArrayList<Integer>();
    	for(int i = MIN_PAR; i <= MAX_PAR; i += MIN_INCREMENT){
    		parList.add(i);
    	}
    	return parList;
    }
   
    public ArrayList<Object> buckets(HashMap<String,String> queryMap){
  
        ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>> bucketsByRanking = new ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>>();
        ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>> bucketsByRankingFinal = new ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>>();
        SortedMap<Integer, ArrayList<Security>> allocatedData = new TreeMap<Integer, ArrayList<Security>>();
        
        Long acctSize = 0L;
        
        max = Integer.valueOf(queryMap.get("max"));
        min = Integer.parseInt(queryMap.get("min"));
        acctSize = Long.parseLong(queryMap.get("investedAmount"));
        double dbAccountSize = Double.parseDouble(queryMap.get("investedAmount"));
 
        for(int i = min; i <= max; i++){
            buckets.add(i);
        }
        
        LadderConfig ladderConfig = new LadderConfig(min, max, getNumBondsPerBucket(min, max), max - min + 1, acctSize, dbAccountSize);
        initSD();
        
        List<Security> filteredBonds = this.bonds.stream()
            .filter(this::filterBonds)
            .collect(Collectors.toList());
        
        Collections.sort(filteredBonds);
        
        for(int bucket:buckets){
        	TreeMap<Integer, HashMap<String, List<Security>>> groupedByBucket = new TreeMap<Integer, HashMap<String,List<Security>>>();
         	HashMap<String, List<Security>> groupedByRanking = new HashMap<String, List<Security>>();
         	
         	List<Security> filterZZStates = filteredBonds.stream()
         			.filter( bond -> !bond.getState().equals("ZZ"))
         			.collect(Collectors.toList());
        	List<Security> bucketSecurity = filterZZStates.stream()
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
        	List<Security> couponRated = bucketSecurity;
        	Collections.sort(couponRated, new Comparator<Security>(){
        		@Override
        		public int compare(Security s1, Security s2){
        			return Double.compare( s2.getCoupon(),s1.getCoupon());
        		
        		}
        	});
           
        	groupedByRanking.put("HealthCare", healthCareBonds);
        	groupedByRanking.put("nyBonds", nyBonds);
        	groupedByRanking.put("caBonds", caBonds);
        	groupedByRanking.put("aRatedBonds", aRatedBonds);
        	groupedByRanking.put("aaRatedBonds", aaRatedBonds);
        	groupedByRanking.put("couponRated", couponRated);
        	
        	groupedByBucket.put(bucket,groupedByRanking);
        	bucketsByRanking.add(groupedByBucket);
        }     
        
    	for( int bucketIndex = 0; bucketIndex < buckets.size(); bucketIndex++ ) {
    		 List<Security> tempArr = new ArrayList<Security>();
             HashMap<Integer, ArrayList<Security>> nyStrippedHc = new HashMap<Integer, ArrayList<Security>>();
             HashMap<Integer, ArrayList<Security>> caStrippedHc = new HashMap<Integer, ArrayList<Security>>();
             HashMap<Integer, ArrayList<Security>> aStrippedHcState = new HashMap<Integer, ArrayList<Security>>();
             HashMap<Integer, ArrayList<Security>> aaStrippedHcState = new HashMap<Integer, ArrayList<Security>>();
             HashMap<Integer, ArrayList<Security>> couponStrippedAll = new HashMap<Integer, ArrayList<Security>>();
    		 TreeMap<Integer, HashMap<String, List<Security>>> groupedByBucketFinal = new TreeMap<Integer, HashMap<String,List<Security>>>();
    		 HashMap<String, List<Security>> groupedByRankingFinal = new HashMap<String, List<Security>>();
    		 
    		 int bucket = buckets.get(bucketIndex);
    		 int rankIndex = Arrays.asList(RANKING).indexOf("HealthCare");
    		 tempObj.clear();
    		 
    		 bucketsByRanking.get(bucketIndex).get(bucket).get(RANKING[rankIndex]).forEach( hcBond -> tempObj.put(hcBond.getCusip(),hcBond));
    		 
    		 //Stripping Health Care sector from the rest of ranking groups
    		 tempArr = bucketsByRanking.get(bucketIndex).get(bucket).get(RANKING[++rankIndex]).stream().filter(nyBond -> !tempObj.containsKey(nyBond.getCusip()))
    		         .collect(Collectors.toList());
    		 nyStrippedHc.put(bucket, (ArrayList<Security>) tempArr);		
    		 
    		 tempArr = bucketsByRanking.get(bucketIndex).get(bucket).get(RANKING[++rankIndex]).stream().filter(caBond -> !tempObj.containsKey(caBond.getCusip()))
    		         .collect(Collectors.toList());
    		 caStrippedHc.put(bucket, (ArrayList<Security>) tempArr);	
    		 
    		 tempArr = bucketsByRanking.get(bucketIndex).get(bucket).get(RANKING[++rankIndex]).stream().filter(aRatedBond -> !tempObj.containsKey(aRatedBond.getCusip()))
    				 .collect(Collectors.toList());
    		 aStrippedHcState.put(bucket, (ArrayList<Security>) tempArr);	
    		 
    		 tempArr = bucketsByRanking.get(bucketIndex).get(bucket).get(RANKING[++rankIndex]).stream().filter(aaRatedbond -> !tempObj.containsKey(aaRatedbond.getCusip()))
    		         .collect(Collectors.toList());
    		 aaStrippedHcState.put(bucket, (ArrayList<Security>) tempArr);	
    		 
    		 tempArr = bucketsByRanking.get(bucketIndex).get(bucket).get(RANKING[++rankIndex]).stream().filter(couponBond -> !tempObj.containsKey(couponBond.getCusip()))
    		         .collect(Collectors.toList());
    		 couponStrippedAll.put(bucket, (ArrayList<Security>) tempArr);	
    		 
    		 //Stripping NY bonds from the rest of ranking groups
    		 rankIndex = Arrays.asList(RANKING).indexOf("nyBonds");
    		 tempObj.clear();
    		 bucketsByRanking.get(bucketIndex).get(bucket).get(RANKING[rankIndex]).forEach( nyBond -> tempObj.put(nyBond.getCusip(),nyBond));
    		 
    		 tempArr = aStrippedHcState.get(bucket).stream().filter(aRatedBond -> !tempObj.containsKey(aRatedBond.getCusip()))
    				 .collect(Collectors.toList());
    		 aStrippedHcState.put(bucket, (ArrayList<Security>) tempArr);	
    		 
    		 tempArr = aaStrippedHcState.get(bucket).stream().filter(aaRatedbond -> !tempObj.containsKey(aaRatedbond.getCusip()))
    		         .collect(Collectors.toList());
    		 aaStrippedHcState.put(bucket, (ArrayList<Security>) tempArr);	
    		 
    		 tempArr = couponStrippedAll.get(bucket).stream().filter(couponBond -> !tempObj.containsKey(couponBond.getCusip()))
    		         .collect(Collectors.toList());
    		 couponStrippedAll.put(bucket, (ArrayList<Security>) tempArr);	
    		 
    		 //Stripping CA bonds from the rest of ranking groups
    		 rankIndex = Arrays.asList(RANKING).indexOf("caBonds");
    		 tempObj.clear();
    		 bucketsByRanking.get(bucketIndex).get(bucket).get(RANKING[rankIndex]).forEach( caBond -> tempObj.put(caBond.getCusip(),caBond));
    		 
    		 tempArr = aStrippedHcState.get(bucket).stream().filter(aRatedBond -> !tempObj.containsKey(aRatedBond.getCusip()))
    				 .collect(Collectors.toList());
    		 aStrippedHcState.put(bucket, (ArrayList<Security>) tempArr);	
    		 
    		 tempArr = aaStrippedHcState.get(bucket).stream().filter(aaRatedbond -> !tempObj.containsKey(aaRatedbond.getCusip()))
    		         .collect(Collectors.toList());
    		 aaStrippedHcState.put(bucket, (ArrayList<Security>) tempArr);	
    		 
    		 tempArr = couponStrippedAll.get(bucket).stream().filter(couponBond -> !tempObj.containsKey(couponBond.getCusip()))
    		         .collect(Collectors.toList());
    		 couponStrippedAll.put(bucket, (ArrayList<Security>) tempArr);	
    		 
    		 //Stripping A and below rated from Coupon ranked bonds from the rest of ranking groups
    		 rankIndex = Arrays.asList(RANKING).indexOf("aRatedBonds");
    		 tempObj.clear();
    		 bucketsByRanking.get(bucketIndex).get(bucket).get(RANKING[rankIndex]).forEach( aRatedBond -> tempObj.put(aRatedBond.getCusip(), aRatedBond));
    		 
    		 tempArr = couponStrippedAll.get(bucket).stream().filter(couponBond -> !tempObj.containsKey(couponBond.getCusip()))
    		         .collect(Collectors.toList());
    		 couponStrippedAll.put(bucket, (ArrayList<Security>) tempArr);	
    		 
     		 //Stripping AA and above rated from Coupon ranked bonds from the rest of ranking groups
    		 rankIndex = Arrays.asList(RANKING).indexOf("aaRatedBonds");
    		 tempObj.clear();
    		 bucketsByRanking.get(bucketIndex).get(bucket).get(RANKING[rankIndex]).forEach( aaRatedBond -> tempObj.put(aaRatedBond.getCusip(), aaRatedBond));
    		 
    		 tempArr = couponStrippedAll.get(bucket).stream().filter(couponBond -> !tempObj.containsKey(couponBond.getCusip()))
    		         .collect(Collectors.toList());
    		 couponStrippedAll.put(bucket, (ArrayList<Security>) tempArr);	
    		 
    		rankIndex = Arrays.asList(RANKING).indexOf("HealthCare");
    		 
    		bucketsByRanking.get(bucketIndex).get(bucket).get(RANKING[rankIndex]).forEach( bond -> bond.setRank("HealthCare"));
    		nyStrippedHc.get(bucket).forEach( bond -> bond.setRank("nyBonds"));
    		caStrippedHc.get(bucket).forEach( bond -> bond.setRank("caBonds"));
    		aStrippedHcState.get(bucket).forEach( bond -> bond.setRank("aRatedBonds"));
    		aaStrippedHcState.get(bucket).forEach( bond -> bond.setRank("aaRatedBonds"));
    		couponStrippedAll.get(bucket).forEach( bond -> bond.setRank("couponRated"));
			
    		groupedByRankingFinal.put("HealthCare", bucketsByRanking.get(bucketIndex).get(bucket).get(RANKING[rankIndex]));
         	groupedByRankingFinal.put("nyBonds", nyStrippedHc.get(bucket));
         	groupedByRankingFinal.put("caBonds",caStrippedHc.get(bucket));
         	groupedByRankingFinal.put("aRatedBonds", aStrippedHcState.get(bucket));
         	groupedByRankingFinal.put("aaRatedBonds",aaStrippedHcState.get(bucket));
         	groupedByRankingFinal.put("couponRated", couponStrippedAll.get(bucket));
         	
    		 groupedByBucketFinal.put(bucket,groupedByRankingFinal);
             bucketsByRankingFinal.add(groupedByBucketFinal);
    	}
    	
        List<ConstraintEvaluator> consEvalList = createConstraintEvaluatorsList(ladderConfig);
        
        List<LadderBucket> ladBucketList = new ArrayList<LadderBucket>();
        
        ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>> bucketsByRank = new ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>>(bucketsByRankingFinal);
        
        createLadder(ladBucketList, bucketsByRank, ladderConfig, consEvalList, allocatedData);
        
        ArrayList<Object> summary = new ArrayList<Object>();
        ArrayList<Map<String, Double>> summaryAlloc = new ArrayList<Map<String, Double>>();
      
        CashReducer cashReducer = new CashReducer(allocatedData,consEvalList,bucketsByRankingFinal,ladderConfig);
        summaryAlloc = cashReducer.reduceCashBalance();
  
//        summaryAlloc = allocatedByConstraints(consEvalList);
        
    	RatingCalculations avgAndMedRating = new RatingCalculations(allocatedData, ladderConfig.getOriginalAccountSize(), summaryAlloc);
    	Map<String, String> ratingStats = avgAndMedRating.calcPortAvgAndMedRating();
    	
        ArrayList<Map<String, Double>> sortedByAllocAmount = new ArrayList<Map<String, Double>>();
    	summaryAlloc.forEach(alloc->{
    		
    				Map<String, Double>sorted = alloc.entrySet().stream()
        			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
        			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
        					(oldValue, newValue) -> oldValue, LinkedHashMap::new));
    		
        			 sortedByAllocAmount.add(sorted);
    	});
    	        	
        ArrayList<Map<String, HashMap<String, Double>>> sortedSectorsInState = new ArrayList<Map<String, HashMap<String, Double>>>();
        Map<String, HashMap<String, Double>> sectorAmounts = new TreeMap<String, HashMap<String, Double>>();
        
        sectorsInStateAlloc(consEvalList).forEach(alloc -> {
        	HashMap<String, Double> sortedSectors;
        	for(Entry<String, HashMap<String, Double>> entry: alloc.entrySet()){
        		HashMap<String, Double> test = entry.getValue();
        		sortedSectors = test.entrySet().stream()
        		.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
    			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
    					(oldValue, newValue) -> oldValue, LinkedHashMap::new));
        		System.out.println(sortedSectors);
        		sectorAmounts.put(entry.getKey(), sortedSectors);
        		
        	}
        	sortedSectorsInState.add(sectorAmounts);
        });
        
        summary.add(sortedByAllocAmount);
        summary.add(sortedSectorsInState);
        summary.add(ratingStats);
        summary.add(allocatedData);
    
        return summary;

    }
    
    private class LadderBucket extends Observable{
        private int matYr;
        private int currentRankIndex = 0;
        private int currentBondIndex = 0;
        
        private Map<Security, SecurityParAmt> secParAmtMap = new HashMap<Security, SecurityParAmt>();


        private int getMatYr() {
        	return matYr;
        }
        
        private void increaseRankIndex() {
        	currentRankIndex++;
        }
        
        private int getRankIndex() {
        	return currentRankIndex;
        }
        
        private void increaseBondIndex() {
        	currentBondIndex++;
        }
        
        private int getBondIndex() {
        	return currentBondIndex;
        }
        
        private void setBondIndex(int idx) {
        	currentBondIndex = idx;
        }
        
        private LadderBucket(int matYr){
            this.matYr = matYr;
        }

        private void removeSecurity(Security sec){
        	this.secParAmtMap.remove(sec);
        }
        
        private void addSecurityParAmount(SecurityParAmt secParAmt){
            this.secParAmtMap.put(secParAmt.sec, secParAmt);
            setChanged();
            notifyObservers(secParAmt);
        }
        
        private SecurityParAmt getLadderSecurity(Security sec){
            return this.secParAmtMap.get(sec);
        }
        
        private void addRoundedParAmount(Security sec, long adjAmt){
            SecurityParAmt secParAmt = secParAmtMap.get(sec);
            if(secParAmt != null){
                secParAmt.addRoundedAmount(adjAmt);
                
                setChanged();
                notifyObservers(secParAmt);
            }
        }

        private void subtractRoundedParAmount(Security sec, long adjAmt){
            SecurityParAmt secParAmt = secParAmtMap.get(sec);
            if(secParAmt != null){
                secParAmt.subtractRoundedAmount(adjAmt);
                setChanged();
                notifyObservers(secParAmt);
            }
        }

        private long getLadderDollarAmount(){
            long dollarAmt = 0;
            for(Map.Entry<Security, SecurityParAmt> entry: secParAmtMap.entrySet()){
                dollarAmt += entry.getValue().sec.getPrice()*entry.getValue().roundedParAmt/PAR_PRICE;
            }
            return dollarAmt;
        }

        private double getLadderDollarAmt(){
            double dollarAmt = 0.0;
            for(Map.Entry<Security, SecurityParAmt> entry: secParAmtMap.entrySet()){
                dollarAmt += entry.getValue().sec.getPrice()*entry.getValue().roundedParAmt/PAR_PRICE;
            }
            return dollarAmt;
        }
        
        private double getAveragePrice(){
            double avgPrice = 0;
            for(Map.Entry<Security, SecurityParAmt> entry: secParAmtMap.entrySet()){
                avgPrice += entry.getValue().sec.getPrice();
            }
            return avgPrice/secParAmtMap.size();
        }

        private List<SecurityParAmt> getSecurityParAmtList(){
            return new ArrayList(secParAmtMap.values());
        }
        
        private List<SecurityParAmt> getSecurityList(){
        	
            return new ArrayList(secParAmtMap.values());
        }
        
        public String toString(){
            StringBuffer sb = new StringBuffer("Maturity Year: " + matYr
                    + ". Dollar Amount: " + ALLOC_FORMAT.format(getLadderDollarAmount())
                    + ". Average Price: " + PRICE_FORMAT.format(getAveragePrice()));
            List<SecurityParAmt> secParAmtList = getSecurityParAmtList();
            Collections.sort(secParAmtList);
            for(SecurityParAmt secParAmt: secParAmtList){
                sb.append(NEW_LINE).append(secParAmt);
            }
            return  sb.toString();
        }
    }
    
    private class LadderConfig {
    	 private int minYr;
         private int maxYr;
         private int numBondsPerBucket;
         private double accountSize;
         private List<Integer> matRangeList = new ArrayList<Integer>();
         private List<Long> acctSizeList = new ArrayList<Long>();

         private LadderConfig(int minYr, int maxYr, int numBondsPerBucket, int matRange, long acctSize, double accountSize){
             this.minYr = minYr;
             this.maxYr = maxYr;
             this.accountSize = accountSize;
             this.numBondsPerBucket = numBondsPerBucket;
             this.matRangeList.add(matRange);
             this.acctSizeList.add(acctSize);
         }

         private void addMatRangeAcctSize(int matRange, long acctSize){
             this.matRangeList.add(matRange);
             this.acctSizeList.add(acctSize);
         }

         private int getOriginalMaturityRange(){
             return matRangeList.get(0);
         }

         private int getLatestMaturityRange(){
             return matRangeList.get(matRangeList.size()-1);
         }

         private long getOriginalAccountSize(){
             return acctSizeList.get(0);
         }

         private long getLatestAccountSize(){
             return acctSizeList.get(acctSizeList.size()-1);
         }

         private double getAccountSize(){
        	 return accountSize;
         }
         
         public String toString(){
             return "Min Year: " + minYr +
                     ". Max Year: " + maxYr +
                     ". Number of Bonds Per Bucket: " + numBondsPerBucket +
                     ". Maturity Range List: " + matRangeList +
                     ". Account Size List: " + acctSizeList ;

         }
    }
    
    private boolean filterBonds(Security sec){
        int yearsToMat = sec.getYearsToMaturity();
        double price = sec.getPrice();
        String lastTraded = (FileLoader.DATE_FORMAT.format(sec.getLastTraded()));
        sec.setLatestTraded(lastTraded);
        if(min == 1){
            return yearsToMat<= max && yearsToMat >= min && price >= MIN_PRICE && price <= MAX_PRICE_ONE;
        }
        return yearsToMat <= max && yearsToMat >= min && price <= MAX_PRICE_OTHER;
    }

    private List<ConstraintEvaluator> createConstraintEvaluatorsList(LadderConfig ladderConfig){

        List<ConstraintEvaluator> consEvalList = new ArrayList<ConstraintEvaluator>();

        consEvalList.add(new HealthCareConstraintEvaluator(ladderConfig));
        consEvalList.add(new AOrBelowConstraintEvaluator(ladderConfig));
        consEvalList.add(new StateConstraintEvaluator(ladderConfig));
        consEvalList.add(new SectorConstraintEvaluator(ladderConfig));
        consEvalList.add(new StateSectorConstraintEvaluator(ladderConfig));
        return consEvalList;
    }
    
    private abstract class ConstraintEvaluator implements Observer{
        private Map<Security, SecurityParAmt> secParAmtMap = new HashMap<Security, SecurityParAmt>();
        private LadderConfig ladderConfig = null;
        
        public String getLimitReachedBy() {
        	return null;
        }
        
        private ConstraintEvaluator (LadderConfig ladderConfig){
            this.ladderConfig = ladderConfig;
        }

        public Map<String, Double> showAllocation() {
			// TODO Auto-generated method stub
			return null;
		}
        
        public void allocateCashSector() {
        
        }
        
        public SortedMap<String, HashMap<String,Double>>showAllocationSectorInState(){
        	return null;
        }

        public Long getOriginalAccountSize(){
        	return ladderConfig.getOriginalAccountSize();
        }
        
		protected double getSecurityPct(Security sec, long parAmt){
            return 100*parAmt*sec.getPrice()/PAR_PRICE/ladderConfig.getOriginalAccountSize();
        }

        protected double getUpdatedSecurityPct(SecurityParAmt secParAmt){
            double updatedSecPct = 0;
            //in case security already exists then delete the old pct
            SecurityParAmt oldSecParAmt = secParAmtMap.get(secParAmt.sec);
            if(oldSecParAmt != null){
                updatedSecPct -= oldSecParAmt.getDollarAmount();
            }
            return (updatedSecPct + secParAmt.getDollarAmount());
        }

        protected SecurityParAmt getSecurityParAmt(Security sec){
            return secParAmtMap.get(sec);
        }  		
        
        @Override
        public void update(Observable o, Object arg) {
            SecurityParAmt argSecParAmt = (SecurityParAmt)arg;
            SecurityParAmt secParAmt = secParAmtMap.get(argSecParAmt.sec);
            if(secParAmt == null){
                secParAmt = new SecurityParAmt(argSecParAmt);
                secParAmtMap.put(secParAmt.sec, secParAmt);
            }
            else{
                secParAmt.roundedParAmt = argSecParAmt.roundedParAmt;
            }
        }

        protected abstract boolean evaluate (Security sec, long parAmt, List<LadderBucket> ladderBucketList);
    }

    private class SecurityParAmt implements Comparable{
        private Security sec;
        private long rawParAmt;
        private long roundedParAmt;

        private SecurityParAmt(Security sec, long rawParAmt, long roundedParAmt){
            this.sec = sec;
            this.rawParAmt = rawParAmt;
            this.roundedParAmt = roundedParAmt;
        }

        private SecurityParAmt(SecurityParAmt secParAmt){
            this.sec = secParAmt.sec;
            this.rawParAmt = secParAmt.rawParAmt;
            this.roundedParAmt = secParAmt.roundedParAmt;
        }

        private void addRoundedAmount (long adjAmt){
            this.roundedParAmt += adjAmt;
        }

        private void subtractRoundedAmount (long adjAmt){
            this.roundedParAmt -= adjAmt;
        }
        
        private double getDollarAmount(){
            return sec.getPrice()*roundedParAmt/PAR_PRICE;
        }

        private long getRoundedParAmount(){
        	return roundedParAmt;
        }
        
        private int getRank(){
        	String t = sec.getRank();
        	int test = Arrays.asList(RANKING).indexOf(sec.getRank());
            return  Arrays.asList(RANKING).indexOf(sec.getRank());
        }

        /**
         * Better rankings compare as greater.
         * @param o Rating object
         * @return -1, 0, 1 if less than, equal to or greater
         */
        public int compareTo(Object o) {
            if (getDollarAmount() < ((SecurityParAmt)o).getDollarAmount()) {
                return -1;
            }
            else if (getDollarAmount() > ((SecurityParAmt)o).getDollarAmount()) {
                return 1;
            }
            else {
                return 0;
            }
        }

//        public int compareTo(Object o) {
//            if (getRank() < ((SecurityParAmt)o).getRank()) {
//                return -1;
//            }
//            else if (getRank() > ((SecurityParAmt)o).getRank()) {
//                return 1;
//            }
//            else {
//                return 0;
//            }
//        }
        
        public String toString(){
            return "Dollar Amount: " + ALLOC_FORMAT.format(getDollarAmount()) +
                    ". RoundedParAmt: " + ALLOC_FORMAT.format(roundedParAmt) +
                    ". RawParAmt: " + ALLOC_FORMAT.format(rawParAmt) +
                    ". Security: " + sec;
        }
    }

    private class StateSectorConstraintEvaluator extends ConstraintEvaluator{
        SortedMap<String, Double> stateSectorPctMap = new TreeMap<String, Double>();
        SortedMap<String, HashMap<String, Double>> stateSectorMap = new TreeMap<String, HashMap<String, Double>>();
        long accountSize = getOriginalAccountSize();
        String limitReachedBy;
        
        private StateSectorConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }

        private String getStateSectorKey(Security sec){
            return sec.getState() + "::" + sec.getSector();
        }

        protected boolean evaluate (Security sec, long parAmt, List<LadderBucket> ladderBucket){
            double sectorPct = stateSectorPctMap.get(getStateSectorKey(sec)) == null ? 0: stateSectorPctMap.get(getStateSectorKey(sec));
            
            if(sectorPct + getSecurityPct(sec, parAmt) * accountSize / 100 >= MAX_SECTOR_STATE_PCT * accountSize / 100) {   	
            	limitReachedBy = getStateSectorKey(sec);
            }
            return sectorPct + getSecurityPct(sec, parAmt) * accountSize / 100 < MAX_SECTOR_STATE_PCT * accountSize / 100;
        }

        public void update(Observable o, Object arg) {
            SecurityParAmt secParAmt = (SecurityParAmt)arg;
            double sectorPct = stateSectorPctMap.get(getStateSectorKey(secParAmt.sec)) == null ? 0: stateSectorPctMap.get(getStateSectorKey(secParAmt.sec));
   
        	HashMap<String, Double>sectorInState = new HashMap<String, Double>();
        	String state = secParAmt.sec.getState();
        	String sector = secParAmt.sec.getSector();
        	
        	if(stateSectorMap.containsKey(state)){
        		HashMap<String, Double> currentSectorInState = stateSectorMap.get(state);
        			currentSectorInState.put(sector, sectorPct + getUpdatedSecurityPct(secParAmt));
        			stateSectorMap.put(state, currentSectorInState);
        	}else{
        		sectorInState.put(sector, sectorPct + getUpdatedSecurityPct(secParAmt));
        		stateSectorMap.put(state, sectorInState);
        	}
        	
           	stateSectorPctMap.put(getStateSectorKey(secParAmt.sec), sectorPct + getUpdatedSecurityPct(secParAmt));
            super.update(o, arg);
        }

      
        public SortedMap<String, HashMap<String, Double>> showAllocationSectorInState(){
        	return stateSectorMap;
        }
        
        public Map<String, Double> showAllocation(){
        	return null;
//        	return stateSectorMap;
        }
        
        public String toString(){
            StringBuffer sb = new StringBuffer("-----  Total State Sector %: -----------");
            for(Map.Entry<String, Double> entry: stateSectorPctMap.entrySet()){
                sb.append(NEW_LINE).append(entry.getKey()).append(" : ").append(PCT_FORMAT.format(entry.getValue()/100));
            }
            return sb.toString();
        }
        
        @Override
        public String getLimitReachedBy() {
        	return this.limitReachedBy;
        }
    }

    private class SectorConstraintEvaluator extends ConstraintEvaluator{
        SortedMap<String, Double> sectorPctMap = new TreeMap<String, Double>();
        long accountSize = getOriginalAccountSize();
        String limitReachedBy;
        
        private SectorConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }
        
        private String getSectorKey(Security sec){
            return sec.getSector();
        }

        protected boolean evaluate (Security sec, long parAmt, List<LadderBucket> ladderBucket){
            double sectorPct = sectorPctMap.get(getSectorKey(sec)) == null ? 0: sectorPctMap.get(getSectorKey(sec));
            
            if(sectorPct + getSecurityPct(sec, parAmt) * accountSize / 100 >= MAX_SECTOR_PCT * accountSize / 100) {
            	limitReachedBy = "sector" + getSectorKey(sec);
            }
            return sectorPct + getSecurityPct(sec, parAmt) * accountSize / 100 < MAX_SECTOR_PCT * accountSize / 100;
        }

        public void update(Observable o, Object arg) {
            SecurityParAmt secParAmt = (SecurityParAmt)arg;
            double sectorPct = sectorPctMap.get(secParAmt.sec.getSector()) == null ? 0: sectorPctMap.get(secParAmt.sec.getSector());
            sectorPctMap.put(secParAmt.sec.getSector(), sectorPct + getUpdatedSecurityPct(secParAmt));
            super.update(o, arg);
        }

        @Override
        public Map<String, Double> showAllocation(){
        	return sectorPctMap;
        }
        
        @Override
        public void allocateCashSector() {
        	Double allocatedAmount = 0.0;
        	for(Map.Entry<String, Double>entry:sectorPctMap.entrySet()){
        		allocatedAmount += entry.getValue();
        	}  	
        	sectorPctMap.put("Cash", accountSize - allocatedAmount);
        }
        
        public String toString(){
            StringBuffer sb = new StringBuffer("-----  Total Sector %: -----------");
            for(Map.Entry<String, Double> entry: sectorPctMap.entrySet()){
                sb.append(NEW_LINE).append(entry.getKey()).append(" : ").append(PCT_FORMAT.format(entry.getValue()/100));
            }
            return sb.toString();
        }
        
        @Override
        public String getLimitReachedBy() {
        	return this.limitReachedBy;
        }
    }

    private class StateConstraintEvaluator extends ConstraintEvaluator{
        SortedMap<String, Double> statePctMap = new TreeMap<String, Double>();
        
        long accountSize = getOriginalAccountSize();
        private String limitReachedBy;
        
        private StateConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }
        
        private String getStateKey(Security sec){
            return sec.getState();
        }
        
        protected boolean evaluate (Security sec, long parAmt, List<LadderBucket> ladderBucketList){
            double statePct = statePctMap.get(getStateKey(sec)) == null ? 0: statePctMap.get(getStateKey(sec));
            if(statePct + getSecurityPct(sec, parAmt) * accountSize / 100 >= MAX_STATE_PCT * accountSize / 100) {
            	if(getStateKey(sec).toUpperCase().equals("NY") || getStateKey(sec).toUpperCase().equals("CA")) {
            		limitReachedBy = getStateKey(sec).toLowerCase() + "Bonds";
            	}else {
            		limitReachedBy = getStateKey(sec).toLowerCase() + "StateBonds";
            	}
            	
            }
            return statePct + getSecurityPct(sec, parAmt) * accountSize / 100 < MAX_STATE_PCT * accountSize / 100;
        }

        public void update(Observable o, Object arg) {
            SecurityParAmt secParAmt = (SecurityParAmt)arg;
            double statePct = statePctMap.get(secParAmt.sec.getState()) == null ? 0: statePctMap.get(secParAmt.sec.getState());
            statePctMap.put(secParAmt.sec.getState(), statePct + getUpdatedSecurityPct(secParAmt));
            super.update(o, arg);
        }

        @Override
        public Map<String, Double> showAllocation(){
        	return statePctMap;
        }

        public String toString(){
            StringBuffer sb = new StringBuffer("-----  Total State %: -----------");
            for(Map.Entry<String, Double> entry: statePctMap.entrySet()){
                sb.append(NEW_LINE).append(entry.getKey()).append(" : ").append(PCT_FORMAT.format(entry.getValue()/100));
            }
            return sb.toString();
        }
        
        @Override
        public String getLimitReachedBy() {
        	return this.limitReachedBy;
        }
    }
    
    private class AOrBelowConstraintEvaluator extends ConstraintEvaluator{
        double aOrBelowPct = 0;
        long accountSize = getOriginalAccountSize();
        private String limitReachedBy;
        
        private AOrBelowConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }
        
        private FileLoader.SecRating getTwoGroupsRatingKey(Security sec){
            return sec.getTwoGroupsRating();
        }

        protected boolean evaluate (Security sec, long parAmt, List<LadderBucket> ladderBucketList){
            if(getTwoGroupsRatingKey(sec) == FileLoader.SecRating.A_OR_BELOW){
            	if(aOrBelowPct + getSecurityPct(sec, parAmt) * accountSize / 100 >= MAX_A_OR_BELOW_PCT * accountSize / 100) {
            		limitReachedBy = "aRatedBonds";
            	}
                return aOrBelowPct + getSecurityPct(sec, parAmt) * accountSize / 100 < MAX_A_OR_BELOW_PCT * accountSize / 100;
            }
            else{
                return true;
            }

        }

        public void update(Observable o, Object arg) {
            SecurityParAmt secParAmt = (SecurityParAmt)arg;
            if(getTwoGroupsRatingKey(secParAmt.sec) == FileLoader.SecRating.A_OR_BELOW){
                aOrBelowPct += getUpdatedSecurityPct(secParAmt);
            }
            super.update(o, arg);
        }
        
        public TreeMap<String, Double> showAorBelowAlloc(){
        	TreeMap<String, Double> aOrBelowMap = new TreeMap<String, Double>();
        	aOrBelowMap.put("aAndBelow", aOrBelowPct);
        	return aOrBelowMap;
        }
        
        @Override
        public Map<String, Double> showAllocation(){
        	return this.showAorBelowAlloc();
        }

        public String toString(){
            return "Total A or Below %:" + PCT_FORMAT.format(aOrBelowPct/100);
        }
        
        @Override
        public String getLimitReachedBy() {
        	return this.limitReachedBy;
        }
    }

    private class HealthCareConstraintEvaluator extends ConstraintEvaluator{
        double healthCarePct = 0;
        long accountSize = getOriginalAccountSize();
        private String limitReachedBy;
        
        private HealthCareConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }
        
        private String getSectorKey(Security sec){
            return sec.getSector();
        }
        
        protected boolean evaluate (Security sec, long parAmt, List<LadderBucket> ladderBucketList){
            if(SECTOR_HEALTHCARE.equals(getSectorKey(sec))){
            	//healthCarePct is $amt not %
            	if(healthCarePct + getSecurityPct(sec, parAmt) * accountSize / 100 >= MAX_HEALTHCARE_PCT * accountSize / 100){
            		limitReachedBy = "HealthCare";
            	}
                return healthCarePct + getSecurityPct(sec, parAmt) * accountSize / 100 < MAX_HEALTHCARE_PCT * accountSize / 100;
            }
            else{
                return true;
            }
        }

        public void update(Observable o, Object arg) {
            SecurityParAmt secParAmt = (SecurityParAmt)arg;
            if(SECTOR_HEALTHCARE.equals(getSectorKey(secParAmt.sec))){
            	//getUpdatedSecurityPct return $amt not %
                healthCarePct += getUpdatedSecurityPct(secParAmt);
            }
            super.update(o, arg);
        }
        
        public TreeMap<String, Double> showHealthCareAlloc(){
        	TreeMap<String, Double> healthCareMap = new TreeMap<String, Double>();
        	healthCareMap.put("Health Care", healthCarePct);
        	return healthCareMap;
        }
        
        @Override
        public Map<String, Double> showAllocation(){
        	return this.showHealthCareAlloc();
        }
        
        
        public String toString(){
            return "Total Healthcare %:" + PCT_FORMAT.format(healthCarePct/100);
        }
        
        @Override
        public String getLimitReachedBy() {
        	return this.limitReachedBy;
        }
        
    }
    
    private class Optimizer {
    	
    	private Security allocBond = null;
    	private double allocBondInvestAmt = 0.0;
		private double allocBondPrice = 0.0;
		private double chosenBondInvestAmt = 0.0;
		private double chosenBondPrice = 0.0;
		private int i = 0;
		private int stateSectorFlag = -1;
		private String stateSector = null;
		private String sectorState = null;
		private String sectorFlag = null;
		private String stateFlag = null;
		
		ArrayList<Integer> ladderIndex = new ArrayList<Integer>();

    	public void optimize(List<LadderBucket> ladderBucketList, Double investedAmount, String limitReachedBy, List<ConstraintEvaluator>consEvalList,
    			Security chosenBond, LadderBucket ladderBucket){
    		
    		double allocLimit = 0.0;
    		double allocPercent = 0.0;
    		int allocPar = 0;
    		double maxHealthCare = MAX_HEALTHCARE_PCT * investedAmount / 100;
    		double maxNYState = MAX_STATE_PCT * investedAmount / 100;
    		double maxCAState = MAX_STATE_PCT * investedAmount / 100;
    		double maxSector = MAX_SECTOR_PCT * investedAmount / 100;
    		double maxAandBelow = MAX_A_OR_BELOW_PCT * investedAmount / 100;
    		double maxState = MAX_STATE_PCT * investedAmount / 100;
    		double maxStateSector = MAX_SECTOR_STATE_PCT * investedAmount / 100;
    		
    		chosenBondPrice = chosenBond.getPrice() / 100;
    		chosenBondInvestAmt = chosenBond.getInvestAmt();
    		
    		ArrayList<Map<String, Double>>summaryAlloc = new ArrayList<Map<String, Double>>();
    		summaryAlloc = allocatedByConstraints(consEvalList);
    		
    		Map<String, Double> allocHealthCare = ( Map<String, Double> ) summaryAlloc.get(0);
	        Map<String, Double> allocRating = ( Map<String, Double> ) summaryAlloc.get(1);
	        Map<String, Double> allocState = ( Map<String, Double> ) summaryAlloc.get(2);
	        Map<String, Double> allocSector = ( Map<String, Double> ) summaryAlloc.get(3);
	        Map<String, HashMap<String, Double>> allocStateSector = (Map<String, HashMap<String, Double>>)  sectorsInStateAlloc(consEvalList).get(0);
	       
	        ladderIndex.clear();
	        allocBond = null;
	        stateSectorFlag = -1;
	        sectorFlag = null;
	        stateFlag = null;
	        
        	for(i = 0; i < ladderBucketList.size(); i++){
        		
        		LadderBucket bucket = ladderBucketList.get(i);
        		List<SecurityParAmt> securityParAmtList = bucket.getSecurityParAmtList();
        		stateSectorFlag  = limitReachedBy.indexOf("::");
    
        		if(limitReachedBy.substring(0,6).equals("sector")) sectorFlag = limitReachedBy.substring(6);
        		if(limitReachedBy.substring(2).equals("StateBonds")) stateFlag = limitReachedBy.substring(0,2).toUpperCase();
        		
        		securityParAmtList.forEach(bond-> {   		

        			if(bond.sec.getRank().equals(limitReachedBy)) {
        				allocBond = bond.sec;
    					allocBondInvestAmt = bond.rawParAmt;
    					allocBondPrice = allocBond.getPrice() /100;
    					ladderIndex.add(i);
        			}
        			
        			if(stateSectorFlag != -1) {
        				stateSector = limitReachedBy.substring(0,2);
            			sectorState = limitReachedBy.substring(4);
        				if(bond.sec.getState().equals(stateSector) && bond.sec.getSector().equals(sectorState) ) {
        					allocBond = bond.sec;
        					allocBondInvestAmt = bond.rawParAmt;
        					allocBondPrice = allocBond.getPrice() /100;
        					ladderIndex.add(i);
        				}
        			}
        			
        			if(sectorFlag != null) {
        				if(bond.sec.getSector().equals(sectorFlag)) {
        					allocBond = bond.sec;
        					allocBondInvestAmt = bond.rawParAmt;
        					allocBondPrice = allocBond.getPrice() /100;
        					ladderIndex.add(i);
        				}
        			}
        			
        			if(stateFlag != null) {
        				if(bond.sec.getState().equals(stateFlag)) {
        					allocBond = bond.sec;
        					allocBondInvestAmt = bond.rawParAmt;
        					allocBondPrice = allocBond.getPrice() /100;
        					ladderIndex.add(i);
        				}
        			}
        			
        		});
        		
        		if(allocBond != null) break;

        	}
        	
        	if( limitReachedBy.equals("HealthCare")){
				allocLimit = maxHealthCare - ( allocSector.get("Health Care") - allocBondInvestAmt );

			}else if( limitReachedBy.equals("nyBonds") ){
				allocLimit = maxNYState - ( allocState.get("NY") - allocBondInvestAmt );

			}else if( limitReachedBy.equals("caBonds") ){
				allocLimit = maxCAState - ( allocState.get("CA") - allocBondInvestAmt );
				
			}else if( limitReachedBy.equals("aRatedBonds") ){
				allocLimit = maxAandBelow - ( allocRating.get("aAndBelow") - allocBondInvestAmt );

			}else if( limitReachedBy.matches("(.*)stateBonds") ){
				String state = limitReachedBy.substring(0,2).toUpperCase();
				allocLimit = maxState - ( allocState.get(state) - allocBondInvestAmt );
				
			}else if(stateSectorFlag != -1 && ladderIndex.size() != 0 ) {
				HashMap<String, Double> sectorStateAlloc = (HashMap<String, Double>)allocStateSector.get(stateSector);
				allocLimit = maxStateSector - ( sectorStateAlloc.get(sectorState));
				
			}else if(stateSectorFlag != -1 && ladderIndex.size() == 0 ) {
				allocLimit = maxStateSector;
				allocPercent = MAX_SECTOR_STATE_PCT;
				
			}else if(sectorFlag != null){
				allocLimit = maxSector - ( allocSector.get(sectorFlag) - allocBondInvestAmt );
				
			}else if(stateFlag != null) {
				allocLimit = maxState - ( allocState.get(stateFlag) - allocBondInvestAmt );
			}
        	
        	
    		ArrayList<Double>trackClosest = new ArrayList<Double>();
    			
    		ArrayList<Integer>parOne = new ArrayList<Integer>();
    		ArrayList<Integer>parTwo = new ArrayList<Integer>();
    		
    		Double maxAllocPerBond = MAX_BOND_PCT * investedAmount /100;
        	ArrayList<Integer> optimizedPar = new ArrayList<Integer> ();
        	
       	    ArrayList<Integer> parList = getParAmount();
     	
       	    if(allocBond != null) {
       	    	for( int par1 = 0; par1 < parList.size() - 1; par1++ ){
        			for( int par2 = 0; par2 < parList.size() - 1; par2++ ){
        				if( parList.get(par1) * allocBondPrice <= maxAllocPerBond && parList.get(par2) * chosenBondPrice <= maxAllocPerBond ){
        					if( parList.get(par1) * allocBondPrice + parList.get(par2) * chosenBondPrice == allocLimit){
        						trackClosest.add(0.0);	
        						parOne.add( parList.get(par1) );
        						parTwo.add( parList.get(par2) );
        					}else if( parList.get(par1) * allocBondPrice + parList.get(par2) * chosenBondPrice < allocLimit ){
        						double closeToLimit = allocLimit - ( parList.get(par1) * allocBondPrice + parList.get(par2) * chosenBondPrice );
        						trackClosest.add( closeToLimit );
        						parOne.add( parList.get(par1) );
        						parTwo.add( parList.get(par2) );
        					}
        				}
        			}
        		}
       	    }else{
       	    	
	    		int checkIncrements = 0;
	    		boolean maxIncrement = false;
	    		
				do{
					allocPar = (int) ( MIN_PAR  + ( checkIncrements ) * MIN_INCREMENT );
	
					if( (allocPar * chosenBondPrice ) < maxAllocPerBond && ( ( allocPar * chosenBondPrice ) < allocPercent * investedAmount / 100) ) {						
						checkIncrements++;
					}else{
						maxIncrement = true;
					}
				} while( !maxIncrement );
				
				
				LadderBucket chosenBucket = ladderBucket;
				chosenBucket.addSecurityParAmount(new SecurityParAmt(chosenBond, (int) (chosenBondPrice * allocPar), allocPar));
       	    		
       	    }
     

        	if(trackClosest.size() != 0 && ladderIndex.size() != 0) {
        		double closest = trackClosest.stream().min(Double::compare).get();
        		   
        		for( int i = 0; i < trackClosest.size(); i++ ){
        			if( trackClosest.get(i) == closest ) {     				
        				optimizedPar.add(parOne.get(i));
        				optimizedPar.add(parTwo.get(i));		
        			}
        		}
        	
        		LadderBucket allocatedBucket = ladderBucketList.get(ladderIndex.get(0));
        		LadderBucket chosenBucket = ladderBucket;
        		allocatedBucket.removeSecurity(allocBond);
        		
        		int firstRawParAmt = (int) (allocBondPrice * optimizedPar.get(0));
        		int secondRawParAmt = (int) (chosenBondPrice * optimizedPar.get(1));
        		allocatedBucket.addSecurityParAmount(new SecurityParAmt(allocBond, firstRawParAmt,optimizedPar.get(0)));
        		chosenBucket.addSecurityParAmount(new SecurityParAmt(chosenBond, secondRawParAmt, optimizedPar.get(1)));
        	}
        }
    	
    }

	private Security lookForBondInDiffRanking( LadderBucket ladderBucket, HashMap<String, List<Security>> secInBucket ){
	
		int rankIndex = ladderBucket.getRankIndex();
		rankIndex++;
		List<Security> secInBucketByRanking = secInBucket.get(RANKING[rankIndex]);
		int bondIndex = 0;
		Security chosenBond = null;
		
		while( secInBucket.get(RANKING[rankIndex]).isEmpty() && rankIndex < RANKING.length - 1 ){
			rankIndex++;
			ladderBucket.increaseRankIndex();
		}
		if(  !secInBucket.get(RANKING[rankIndex]).isEmpty() ) {
			chosenBond = secInBucket.get(RANKING[rankIndex]).get(bondIndex);
		}
		
		return chosenBond;
	}
	
    private long getParAmount(Security chosenBond, LadderBucket ladderBucket, LadderConfig ladderConfig, HashMap<String, List<Security>> secInBucket) {
    	
		long investedAmount = ladderConfig.getLatestAccountSize();
		int currentRankIndex = (int) ladderBucket.getRankIndex();
		double maxBondSize = MAX_BOND_PCT * investedAmount / 100;
		double bucketSize = investedAmount / ladderConfig.getLatestMaturityRange();
		long allocSize = STARTING_PAR;
		int adjMinPar = MIN_PAR;
		
		//if bucketMoney/70k < 2 or 70k*price > 10% then reduce alloc size to 20k 
		//in order to fill the buckets with bonds not leave in cash
		if( Math.floor(bucketSize / allocSize) < 2 || allocSize * chosenBond.getPrice() / 100 > maxBondSize){
			adjMinPar = CASH_REDUCTION_PAR;
			allocSize = (long) (Math.floor( investedAmount / ladderConfig.getOriginalMaturityRange() / MIN_INCREMENT ) * MIN_INCREMENT);
		}
		
		if( allocSize * chosenBond.getPrice() / 100 > maxBondSize ){
			int bondIdx = ladderBucket.getBondIndex();
			double testPrice = 0.0;

			do{
				List<Security> secInBucketByRanking = secInBucket.get(RANKING[currentRankIndex]);
				if(secInBucketByRanking.isEmpty()) {
					currentRankIndex++;
				}else {
					for( long parSize = MAX_PAR - MIN_INCREMENT; parSize >= adjMinPar; parSize -= MIN_INCREMENT ){
						do{
							testPrice = secInBucketByRanking.get(bondIdx).getPrice();
							bondIdx++;
						}while( bondIdx <  secInBucketByRanking.size() && parSize * testPrice / 100 > maxBondSize );
						allocSize = parSize;
						if( allocSize * testPrice / 100 <= maxBondSize ) {
							chosenBond = secInBucket.get(RANKING[currentRankIndex]).get(--bondIdx);
							return (long)(allocSize * chosenBond.getPrice() / PAR_PRICE);
						}
						bondIdx = ladderBucket.getBondIndex();
					}
	
					currentRankIndex++;
				}
			} while ( currentRankIndex < RANKING.length );
			return 0;
		}else{
			return (long)(allocSize * chosenBond.getPrice() / PAR_PRICE);
		}
		
    }
    
    private void createLadder(List<LadderBucket> ladBucketList,  ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>> bucketsByRank, LadderConfig ladderConfig, List<ConstraintEvaluator> consEvalList, 
    		SortedMap<Integer, ArrayList<Security>> allocatedData){
    	
    	ArrayList<Integer> buckets = getBuckets();  	
    	double accountSize = ladderConfig.getAccountSize();
    	double totDollarBucketAmt = accountSize /ladderConfig.getLatestMaturityRange();
    	int numBuckets = buckets.size();
    	boolean moveToNextBucket = true;
    	long roundedParAmt = 0;
    	
    	Optimizer optimizer = new Optimizer();
    	
    	for(int i = 0; i < numBuckets; i++) {
    		int matYr = bucketsByRank.get(i).firstKey();
	        ladBucketList.add(new LadderBucket(matYr)); 
	        LadderBucket currentBucket = ladBucketList.get(ladBucketList.size() - 1);
	        for(ConstraintEvaluator consEval: consEvalList){
	            currentBucket.addObserver(consEval);
	        }
    	}
    	
    	int bucketIndex = numBuckets - 1;
		int bucketMatYr = buckets.get(bucketIndex);
		int aaRatedRankIndex = Arrays.asList(RANKING).indexOf("aaRatedBonds");
		double leftInCash;
		Security chosenBond = null;
		
    	do {
    		LadderBucket ladderBucket = ladBucketList.get(bucketIndex);
    		int currentRankIndex = ladderBucket.getRankIndex();
    		int currentBondIndex = ladderBucket.getBondIndex();
    		
	        ArrayList<Security> bucketBonds = new ArrayList<Security>();
	        if(currentRankIndex < RANKING.length) {
	        	
	        	if(bucketsByRank.get(bucketIndex).get(bucketMatYr).get(RANKING[currentRankIndex]).isEmpty() && currentRankIndex < RANKING.length) {
	        		chosenBond = lookForBondInDiffRanking( ladderBucket, bucketsByRank.get(bucketIndex).get(bucketMatYr) );
	        	}else {
	        		if(bucketsByRank.get(bucketIndex).get(bucketMatYr).get(RANKING[currentRankIndex]).size() - 1 < currentBondIndex &&
	        				currentRankIndex < RANKING.length	) {
	        			chosenBond = lookForBondInDiffRanking( ladderBucket, bucketsByRank.get(bucketIndex).get(bucketMatYr) );
	        		}else {
	        			chosenBond = bucketsByRank.get(bucketIndex).get(bucketMatYr).get(RANKING[currentRankIndex]).get(currentBondIndex);
	        		}		
	        	}
	        		
	        	if( chosenBond == null ) {
	        		moveToNextBucket = true;
	            	
	            	List<SecurityParAmt> secParAmtList = ladderBucket.getSecurityParAmtList();
	   		        Collections.sort(secParAmtList);
	   		            
	        		leftInCash =  totDollarBucketAmt - ladderBucket.getLadderDollarAmt();		
	        		
	    	        for(SecurityParAmt secParAmt : secParAmtList){   	        	
	    	        	Double investedAmt = secParAmt.getDollarAmount();
	    	        	secParAmt.sec.setInvestAmt(investedAmt);
	    	            bucketBonds.add(secParAmt.sec);         
	    	        }
	    	     // duplicate code - remove to separate function
					Security cashSecurity = new Security();
					cashSecurity.setCusip("Cash");
					cashSecurity.setInvestAmt(leftInCash);
					bucketBonds.add(cashSecurity);   
					allocatedData.put(bucketMatYr, bucketBonds);
					bucketsByRank.remove(bucketIndex);
					buckets.remove(bucketIndex);
					ladBucketList.remove(bucketIndex);
	        	}else {
	        		
	        		long rawParAmt = getParAmount(chosenBond, ladderBucket, ladderConfig, bucketsByRank.get(bucketIndex).get(bucketMatYr));
			        roundedParAmt = Math.round((double)(rawParAmt/MIN_INCREMENT))*MIN_INCREMENT;
			        String limitReachedBy = evaluateConstraints(consEvalList, chosenBond, roundedParAmt, ladBucketList);
			        		
		            if( limitReachedBy == null && rawParAmt != 0){
		                ladderBucket.addSecurityParAmount(new SecurityParAmt(chosenBond, rawParAmt, roundedParAmt));
		                ladderBucket.increaseBondIndex();        
		            }else if(rawParAmt == 0) {
		            	// duplicate code - remove to separate function
		            	leftInCash =  totDollarBucketAmt - ladderBucket.getLadderDollarAmt();	
		            	Security cashSecurity = new Security();
						cashSecurity.setCusip("Cash");
						cashSecurity.setInvestAmt(leftInCash);
						bucketBonds.add(cashSecurity);   
						allocatedData.put(bucketMatYr, bucketBonds);
						bucketsByRank.remove(bucketIndex);
						buckets.remove(bucketIndex);
						ladBucketList.remove(bucketIndex);

		            }else {
		            
		            	moveToNextBucket = false;
		            	chosenBond.setInvestAmt(rawParAmt);
		            	optimizer.optimize(ladBucketList, accountSize, limitReachedBy, consEvalList, chosenBond, ladderBucket);
		            	
		            	if(ladderBucket.getLadderSecurity(chosenBond) != null) {
		            		roundedParAmt = ladderBucket.getLadderSecurity(chosenBond).roundedParAmt;
		            	}
		            		
		            	
		            	if( currentRankIndex != aaRatedRankIndex ){
		                  	ladBucketList.forEach( bucket -> { 
		                  		if(bucket.getRankIndex() == currentRankIndex) {
		                  			bucket.increaseRankIndex();
				            		bucket.setBondIndex(0);
		                  		}		
			            	});
		            	}else{
		            		ladderBucket.increaseBondIndex();   
		            	}
		            }
		                 
			        
		            if( ladderBucket.getLadderDollarAmt() >= totDollarBucketAmt ) {
		            	moveToNextBucket = true;
		            	
		            	ladderBucket.subtractRoundedParAmount(chosenBond, roundedParAmt);
		            	ladderBucket.removeSecurity(chosenBond);
		            	
		            	List<SecurityParAmt> secParAmtList = ladderBucket.getSecurityParAmtList();
		   		        Collections.sort(secParAmtList);
		   		               
		        		leftInCash =  totDollarBucketAmt - ladderBucket.getLadderDollarAmt();		
		        		
		    	        for(SecurityParAmt secParAmt : secParAmtList){   	        	
		    	        	Double investedAmt = secParAmt.getDollarAmount();
		    	        	secParAmt.sec.setInvestAmt(investedAmt);
		    	            bucketBonds.add(secParAmt.sec);         
		    	        }
		    	        
		    	     // duplicate code - remove to separate function
						Security cashSecurity = new Security();
						cashSecurity.setCusip("Cash");
						cashSecurity.setInvestAmt(leftInCash);
						bucketBonds.add(cashSecurity);   
						allocatedData.put(bucketMatYr, bucketBonds);
						bucketsByRank.remove(bucketIndex);
						buckets.remove(bucketIndex);
						ladBucketList.remove(bucketIndex);
						
		           }
	        		
	        	}
	        }
	           
			numBuckets = buckets.size();

			if( !moveToNextBucket ){
				moveToNextBucket = true;
			}else{

				if( ( bucketIndex == 0 &&  numBuckets > 1 ) || ( numBuckets == 1 ) ){
					bucketIndex = numBuckets - 1;
					bucketMatYr = buckets.get(bucketIndex);
				}else if( numBuckets > 1 ){
					bucketMatYr = buckets.get(--bucketIndex);
				}

			}
			
    	}while(numBuckets > 0);

    }
       
    private HashMap<Double, String> checkLimits(Security testBond, double investedAmount, double allocatedCash, Map<String, Double> allocRating, Map<String, Double> allocSector, 
    		Map<String, Double> allocState, Map<String, HashMap<String, Double>>allocSectorInState){
    	
		double maxHealthCare = MAX_HEALTHCARE_PCT * investedAmount / 100;
		double maxSector = MAX_SECTOR_PCT * investedAmount / 100;
		double maxNYState = MAX_STATE_PCT * investedAmount / 100;
		double maxCAState = MAX_STATE_PCT * investedAmount / 100;
		double maxState = MAX_STATE_PCT * investedAmount / 100;
		double maxStateSector = MAX_SECTOR_STATE_PCT * investedAmount / 100;
		double maxAandBelow = MAX_A_OR_BELOW_PCT * investedAmount / 100;
		double allocationLimit = 0.0;
	    double leftRoom = 0.0;
    	double price = testBond.getPrice();
		String sector = testBond.getSector();
		String state = testBond.getState();
		HashMap<Double, String>allocationLimitType = new HashMap<Double, String>();
		
    	if(!allocSector.containsKey(sector)){
    		allocSector.put(sector, 0.0);
    	}
    	
    	if(!allocState.containsKey(state)){
    		allocState.put(state, 0.0);
    	}
   
      	if(allocSectorInState.containsKey(state)){
      		if(!allocSectorInState.get(state).containsKey(sector)) {
      			HashMap<String, Double> sectorValue = new HashMap<String, Double>();
        		sectorValue.put(sector, 0.0);
        		allocSectorInState.put(state, sectorValue);
      		}
    	}else{
    		HashMap<String, Double> sectorValue = new HashMap<String, Double>();
    		sectorValue.put(sector, 0.0);
    		allocSectorInState.put(state, sectorValue);
    	}
      	
    	String limitType = sector;
		
    	//check limit for Sector and then compare to limit for State and then to limit for Sector in State
		allocationLimit = maxSector - allocSector.get(sector);

		leftRoom = maxState -  allocState.get(state);
		if( allocationLimit > leftRoom ){
			allocationLimit = leftRoom;
			limitType = state;
		}
		
		leftRoom = maxStateSector - allocSectorInState.get(state).get(sector);
		if( allocationLimit > leftRoom ){
			allocationLimit = leftRoom;
			limitType = "sectorInState";
		}
		
		if( testBond.getRank().equals("HealthCare") ){
				leftRoom = maxHealthCare - allocSector.get(sector);
				if( allocationLimit > leftRoom ){
				allocationLimit = leftRoom;
				limitType = "HealthCare";
			}
		}

		if( testBond.getRank().equals("nyBonds") ){
	   		leftRoom = maxNYState - allocState.get("NY");
			if( allocationLimit > leftRoom ){
				allocationLimit = leftRoom;
				limitType = state;
			}
		}

		if( testBond.getRank().equals("caBonds")){
			leftRoom = maxCAState - allocState.get("CA");
			if( allocationLimit > leftRoom ){
				allocationLimit = leftRoom;
				limitType = state;
			}
		}

		if( testBond.getTwoGroupsRating().equals(FileLoader.SecRating.A_OR_BELOW) ){
			leftRoom = maxAandBelow - allocRating.get("aAndBelow");
			if( allocationLimit > leftRoom ){
				allocationLimit = leftRoom;
				limitType = "aAndBelow";
			}
		}
		
		if( allocationLimit < 0 ) allocationLimit = 0;
		if( allocationLimit > allocatedCash ) allocationLimit = allocatedCash;
		
		allocationLimitType.put(allocationLimit, limitType);
		return allocationLimitType;
    }
    
    private class CashReducer{
    	
    	private ArrayList<Integer> buckets = getBuckets();
        private ArrayList<Map<String, Double>> summaryAlloc = new ArrayList<Map<String, Double>>();
    	private double totalCash = 0.0;
    	private SortedMap<Integer, ArrayList<Security>>allocatedData;
    	private List<ConstraintEvaluator>consEvalList;
    	private ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>> bucketsByRankingFinal;
    	private LadderConfig ladderConfig;
    	private double investedAmount;
    	private double limitOnCashMove;
    	private String shuffleBuckets = null;
		private Map<String, Double> allocHealthCare;
	    private Map<String, Double> allocRating;
	    private Map<String, Double> allocState;
	    private Map<String, Double> allocSector;
	    private Map<String, HashMap<String, Double>>allocSectorInState;
		private HashMap<String, Boolean> trackOverLimit = new HashMap<String, Boolean>();
		
    	private CashReducer(SortedMap<Integer, ArrayList<Security>>allocatedData, List<ConstraintEvaluator>consEvalList, 
    			ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>> bucketsByRankingFinal, LadderConfig ladderConfig){
    		this.allocatedData = allocatedData;
    		this.consEvalList = consEvalList;
    		this.bucketsByRankingFinal = bucketsByRankingFinal;
    		this.ladderConfig = ladderConfig;
    		this.investedAmount = ladderConfig.getAccountSize();
    		//0.005 for 1% max deviation between lowest and highest allocated bucket
    		this.limitOnCashMove = 0.01 * investedAmount;
    		this.summaryAlloc = allocatedByConstraints(consEvalList);
    		this.allocHealthCare = ( Map<String, Double> ) summaryAlloc.get(0);
	        this.allocRating = ( Map<String, Double> ) summaryAlloc.get(1);
	        this.allocState = ( Map<String, Double> ) summaryAlloc.get(2);
	        this.allocSector = ( Map<String, Double> ) summaryAlloc.get(3);
	        this.allocSectorInState = (Map<String, HashMap<String, Double>>) sectorsInStateAlloc(consEvalList).get(0);
    	}
        
    	private void allocCashToNewBonds(){
    		//Allocates Total Cash in the Portfolio to the allocSector
    		this.consEvalList.get(3).allocateCashSector();
    		
    		buckets.forEach( bucketNumber -> {
        		
        		double minIncrementToAllocate = 0.0;
                double allocationLimit = 0.0;
                int bondNum = 0;
    			int checkIncrements = 0;
    			boolean maxIncrement = false;
    			boolean allocCheck = false;
    			int bucketIndex = buckets.indexOf(bucketNumber);
    			HashMap<Double, String> allocationLimitType = new HashMap<Double, String>();   	
    			ArrayList<String> checkCusips = new ArrayList<String>();
    			ArrayList<Security> bucket = new ArrayList<Security>();
    			Security testBond = null;
    			bucket = allocatedData.get(bucketNumber);
    			int rankIndex = 0; 
    			int bucketLength = bucket.size() - 1;
    			
    			double allocatedCash = bucket.get(bucketLength).getInvestAmt();
    			
    			bucket.forEach( position -> {
    				checkCusips.add( position.getCusip() );
    			});
    		
    			if( allocatedCash > 0){
    				do{
    					int bondsInBucket =  bucketsByRankingFinal.get(bucketIndex).get(bucketNumber).get(RANKING[rankIndex]).size();
    					
    					for( int i = 0; i <  bondsInBucket; i++ ){
    						testBond = bucketsByRankingFinal.get(bucketIndex).get(bucketNumber).get(RANKING[rankIndex]).get(i);
    						double price = testBond.getPrice();
    						String sector = testBond.getSector();
    						String state = testBond.getState();
    						
    						//Checking if the amount will fall within limits for 1.Rating; 2.Sector/incl Health Care/ 3.State(incl NY, CA/ 4.Sector in State
    						allocationLimitType = checkLimits(testBond, investedAmount, allocatedCash, allocRating, allocSector, allocState, allocSectorInState);
    						allocationLimit = (double) allocationLimitType.keySet().toArray()[0];
    						
    						minIncrementToAllocate = ( allocatedCash ) / ( CASH_REDUCTION_PAR * ( price * 1 / 100 ) );
    						bondNum = (int) Math.floor( minIncrementToAllocate );

    						checkIncrements = 0;
    						maxIncrement = false;

    						if( bondNum > 0 ){

    							do{
    								minIncrementToAllocate = ( CASH_REDUCTION_PAR  + ( checkIncrements ) * MIN_INCREMENT )  * ( price * 1 / 100 );

    								if( minIncrementToAllocate > 0 && minIncrementToAllocate <= allocationLimit && ( minIncrementToAllocate <= MAX_BOND_PCT / 100 * investedAmount ) && ( minIncrementToAllocate / 
    										( testBond.getPrice() / 100 ) ) <= MAX_PAR ){
    									checkIncrements++;
    								}else{
    									maxIncrement = true;
    								}
    							} while( !maxIncrement );
    						}

    						if( checkIncrements > 0 ) --checkIncrements;
    						minIncrementToAllocate = ( CASH_REDUCTION_PAR  + ( checkIncrements ) * MIN_INCREMENT )  * ( price * 1 / 100 );

    						allocCheck = checkCusips.contains(testBond.getCusip());
    						
    						if( minIncrementToAllocate > 0 && minIncrementToAllocate <= allocationLimit && ( minIncrementToAllocate <= MAX_BOND_PCT * investedAmount ) && ( minIncrementToAllocate / 
    								( testBond.getPrice() / 100 ) ) <= MAX_PAR && !allocCheck ){
    							bucket.get(bucketLength).setInvestAmt(allocatedCash - minIncrementToAllocate);
    							testBond.setInvestAmt( 0 + minIncrementToAllocate );

    							if(allocState.containsKey(state)){
    								double currentAllocState = allocState.get(state);
    								allocState.put(state, currentAllocState + minIncrementToAllocate);
    							}else{
    								allocState.put(state, minIncrementToAllocate);
    							}
    							if( testBond.getTwoGroupsRating().equals(FileLoader.SecRating.A_OR_BELOW) ){
    								double currentAllocRating = allocRating.get("aAndBelow");
    								allocRating.put("aAndBelow", currentAllocRating + minIncrementToAllocate);
    							}
    							
    							if(allocSector.containsKey(sector)){
    								double currentAllocSector = allocSector.get(sector);
    								allocSector.put(sector, currentAllocSector + minIncrementToAllocate);
    							}else{
    								allocSector.put(sector, minIncrementToAllocate);
    							}
    							   							
    							double currentAllocSectorInState = allocSectorInState.get(state).get(sector);
					    		HashMap<String, Double> sectorValue = new HashMap<String, Double>();
					    		sectorValue.put(sector, currentAllocSectorInState + minIncrementToAllocate);
					    		allocSectorInState.put(state, sectorValue);
    					     	
    							double currentAllocCash = allocSector.get("Cash") - allocatedCash;
    							allocatedCash = bucket.get(bucketLength).getInvestAmt();
    							allocSector.put("Cash", currentAllocCash + allocatedCash);
    							
    							bucket.add(bucket.size() - 1, testBond);
    							bucketLength++;
    						}
    					}
    					rankIndex++;

    				}while( rankIndex < RANKING.length );
    				
    			}
    			allocCashToExistingBonds(bucket);
    		});
    	}
    	
    	private void allocCashToExistingBonds(ArrayList<Security>bucket){
    		
    		int bucketLength = bucket.size() - 1;
    		double allocatedCash = bucket.get(bucketLength).getInvestAmt();
    		boolean allocCheck = false;
    		
    		
    		for( int i = 0; i < bucketLength; i++ ){

				double price = bucket.get(i).getPrice();
				String sector =  bucket.get(i).getSector();
				String state =  bucket.get(i).getState();
				String limitType = sector;
				
				Security testBond = bucket.get(i);
				//Checking if the amount will fall within limits for 1.Rating; 2.Sector/incl Health Care/ 3.State(incl NY, CA/ 4.Sector in State
				HashMap<Double, String> allocationLimitType = checkLimits(testBond, investedAmount, allocatedCash, allocRating, allocSector, allocState, allocSectorInState);
				double allocationLimit = (double) allocationLimitType.keySet().toArray()[0];
				limitType = allocationLimitType.values().toString();
				
				
				double minIncrementToAllocate = ( allocatedCash ) / ( MIN_INCREMENT * ( price * 1 / 100 ) );
				int bondNum = (int) Math.floor( minIncrementToAllocate );
				int checkIncrements = 1;
				boolean maxIncrement = false;
				boolean stopIncrease = false;
				
				if( bondNum > 0 ){
					do{

						minIncrementToAllocate = Math.floor( checkIncrements ) * ( MIN_INCREMENT * ( price * 1 / 100 ) );
						allocCheck = ( minIncrementToAllocate <= allocationLimit ||  ( !stopIncrease && !trackOverLimit.containsKey(limitType))) && 
								( bucket.get(i).getInvestAmt() + minIncrementToAllocate <= MAX_BOND_PCT * investedAmount ) &&
								( ( bucket.get(i).getInvestAmt() + minIncrementToAllocate ) / ( bucket.get(i).getPrice() / 100 ) ) <= MAX_PAR; 

						if( minIncrementToAllocate > 0 && allocCheck ){
							if( minIncrementToAllocate > allocationLimit ){
							   stopIncrease = true;
							}
							checkIncrements++;
						}else{
							maxIncrement = true;
						}
					} while( checkIncrements <= bondNum && !maxIncrement );
				}

				if( checkIncrements > 1 ) --checkIncrements;
				minIncrementToAllocate = checkIncrements;
				minIncrementToAllocate = Math.floor( minIncrementToAllocate ) * ( MIN_INCREMENT * ( price * 1 / 100 ) );
				
				allocCheck = ( bucket.get(i).getInvestAmt() + minIncrementToAllocate <= MAX_BOND_PCT * investedAmount ) && ( ( bucket.get(i).getInvestAmt() + minIncrementToAllocate ) / 
						 ( bucket.get(i).getPrice() / 100 ) ) <= MAX_PAR && allocatedCash >= minIncrementToAllocate;

				if( minIncrementToAllocate > 0 && minIncrementToAllocate <= allocationLimit && allocCheck || ( ( stopIncrease || checkIncrements == 1 ) 
						&& !trackOverLimit.containsKey(limitType) && allocatedCash >= minIncrementToAllocate && allocCheck) ){

					if( !trackOverLimit.containsKey(limitType) && minIncrementToAllocate > allocationLimit ) {
						trackOverLimit.put(limitType, true);
					}

					bucket.get(bucketLength).setInvestAmt(allocatedCash - minIncrementToAllocate);
					double currentAllocAmount = bucket.get(i).getInvestAmt();
					bucket.get(i).setInvestAmt(currentAllocAmount + minIncrementToAllocate);
				
					double currentAllocState = allocState.get(state);
					allocState.put(state, currentAllocState + minIncrementToAllocate);
				
					double currentAllocSector = allocSector.get(sector);
					allocSector.put(sector, currentAllocSector + minIncrementToAllocate);
					
					if( bucket.get(i).getTwoGroupsRating().equals(FileLoader.SecRating.A_OR_BELOW) ){
						double currentAllocRating = allocRating.get("aAndBelow");
						allocRating.put("aAndBelow", currentAllocRating + minIncrementToAllocate);
					}
					
					double currentAllocSectorInState = allocSectorInState.get(state).get(sector);
		    		HashMap<String, Double> sectorValue = new HashMap<String, Double>();
		    		sectorValue.put(sector, currentAllocSectorInState + minIncrementToAllocate);
		    		allocSectorInState.put(state, sectorValue);
		    		
					double currentAllocCash = allocSector.get("Cash") - allocatedCash;
					allocatedCash = bucket.get(bucketLength).getInvestAmt();
					allocSector.put("Cash", currentAllocCash + allocatedCash);

				}		
			}
			
    		if(!(shuffleBuckets == "doneCashRealloc") && buckets.size() > 1){
    			allocatedCash = bucket.get(bucketLength).getInvestAmt();
				if(allocatedCash > limitOnCashMove) {
					totalCash += limitOnCashMove;
					bucket.get(bucketLength).setInvestAmt(allocatedCash - limitOnCashMove);
				}else {
					totalCash += allocatedCash;
					bucket.get(bucketLength).setInvestAmt(0.0);
				}	
    		}
    		
    	}
    	
    	private ArrayList<Map<String, Double>> reduceCashBalance(){
        	
    		allocCashToNewBonds();
    		
    		while(!(shuffleBuckets == "doneCashRealloc") && buckets.size() > 1){
    			do {
            		buckets.forEach( bucketNumber -> {
            	
    	        		ArrayList<Security>bucket = allocatedData.get(bucketNumber);
    	        		int bucketLength = bucket.size() - 1;
    	        		double cashLeft = bucket.get(bucketLength).getInvestAmt();
            		
            			if(cashLeft == 0.0) {
                			if(totalCash >= limitOnCashMove) {
                				bucket.get(bucketLength).setInvestAmt(limitOnCashMove);
                				totalCash -= limitOnCashMove;
                			}else {
                				bucket.get(bucketLength).setInvestAmt(totalCash);
                				totalCash = 0.0;
                			}
                		}    		
            		});
            		//above distributes cash only to buckets with 0 cash. Below allocate cash to buckets that have already some cash after
            		//the first redistribution of cash
            		if(totalCash > 0) {
            			double cashToAlloc = totalCash / buckets.size();
            			buckets.forEach( bucketNumber -> {
            				ArrayList<Security>bucket = allocatedData.get(bucketNumber);
            				int bucketLength = bucket.size() - 1;
            				double currentCash = bucket.get(bucketLength).getInvestAmt();
            				bucket.get(bucketLength).setInvestAmt(currentCash + cashToAlloc);
            				totalCash -= cashToAlloc;
            			});
            		}
            	}while(totalCash > 1.0);
            
            	if(shuffleBuckets == "reverse") {
            		Collections.reverse(buckets);
            		shuffleBuckets = "middleBucketFirst";
            	}else if(shuffleBuckets == "middleBucketFirst") {
            		ArrayList<Integer>newBuckets = new ArrayList<Integer>();
            		Collections.reverse(buckets);
            		int middleIdx = (int)Math.floor(buckets.size()/2);
            		int minIdx = 0;
            		int maxIdx = buckets.size() - 1;
            		int minReached = middleIdx - 1;
            		int maxReached = middleIdx + 1;
            		newBuckets.add(buckets.get(middleIdx));
            		
            		while(minReached >= minIdx || maxReached <= maxIdx) {
            			if(minReached >= minIdx) newBuckets.add(buckets.get(minReached));
            			if(maxReached <= maxIdx) newBuckets.add(buckets.get(maxReached));
            			minReached--;
            			maxReached++;
            		}
            		
            		buckets.clear();
            		buckets = newBuckets;
            		shuffleBuckets = "doneCashRealloc";
            	}else {
            		shuffleBuckets = "reverse";
            	};
            	
            	buckets.forEach( bucketNumber -> {
            		ArrayList<Security> bucket = allocatedData.get(bucketNumber);
            		allocCashToExistingBonds(bucket);
            	});
    			
    		}
    		
        	return summaryAlloc;
        }
    	
    }

    private class RatingCalculations{
    	
    	private SortedMap<Integer, ArrayList<Security>>allocatedData;
    	private ArrayList<Map<String, Double>> summaryAlloc;
    	private double investedAmount;
    	private double medRating = 0.0;
    	private double avgRating = 0.0;
    	
    	private RatingCalculations(SortedMap<Integer, ArrayList<Security>>allocatedData, double investedAmount, ArrayList<Map<String, Double>> summaryAlloc){
    		this.allocatedData = allocatedData;
    		this.investedAmount = investedAmount;
    		this.summaryAlloc = summaryAlloc;
    	}
    	
        private HashMap<String, String> calcPortAvgAndMedRating(){
        	HashMap<String, String> ratingStats = new HashMap<String, String>();
        	ArrayList<Integer> buckets = getBuckets();
        	Rating medianRating = null;
        	Rating averageRating = null;
        	
        	buckets.forEach( bucketNumber -> {
        		
        		double mvPercent = 0.0;
    			ArrayList<Security> bucket = new ArrayList<Security>();
    			bucket = allocatedData.get(bucketNumber);
    			int bucketLength = bucket.size();
    	
    			int bucketEnd = bucket.get(bucketLength - 1).getCusip() == "Cash" ? bucketLength - 1 : bucketLength;
        		for( int i = 0; i < bucketEnd; i++ ){
        			Security sec = bucket.get(i);
    				mvPercent = bucket.get(i).getInvestAmt() / investedAmount;
    				
    				medRating += mvPercent * Rating.getMedianRating(sec.getSpRating(), sec.getMoodyRating(), sec.getFitchRating(), true, true, true).getQIndex();
    				avgRating += mvPercent * Rating.getAverageRating(sec.getSpRating(), sec.getMoodyRating(), sec.getFitchRating(), true, true, true).getQIndex();
        		}
        		
        	});
        	
        	medRating = Math.ceil(medRating);
        	avgRating = Math.ceil(avgRating);
      
        	medianRating = Rating.valueOf(medRating);
        	averageRating = Rating.valueOf(avgRating);
        	ratingStats.put("AverageRating", averageRating.toString());
        	ratingStats.put("MedianRating", medianRating.toString());
        	return ratingStats;
        	
        }
    	
    }

    private String evaluateConstraints(List<ConstraintEvaluator> consEvalList, Security sec, long parAmt, List<LadderBucket> ladderBucketList){
        String result = null;
    	for(ConstraintEvaluator consEval: consEvalList){
            boolean constraintPassed = consEval.evaluate(sec, parAmt, ladderBucketList);
            if(!constraintPassed){
            	return consEval.getLimitReachedBy();
            }
        }
        return result;
    }
    
    private int getNumBondsPerBucket(int minYr, int maxYr){
        int matRange = maxYr - minYr + 1;
        if(matRange >= MIN_NUM_BONDS ){
            return 1;
        }
        else{
            return MIN_NUM_BONDS/matRange + 1;
        }
    }

    private class YrToMatComparator implements Comparator<Integer>{
    	
    	public int compare(Integer a, Integer b){
    		return b - a;
    	}
    }

    private ArrayList<Map<String, Double>>allocatedByConstraints(List<ConstraintEvaluator> consEvalList){
    	
    	ArrayList<Map<String, Double>> summaryAlloc = new ArrayList<Map<String, Double>>();
        
    	for(ConstraintEvaluator consEval: consEvalList){
            if(consEval.showAllocation() == null){
//         	   summaryAlloc.add(consEval.showAllocationSectorInState());
            }else{
         	   summaryAlloc.add(consEval.showAllocation());
            }
         }
        
        return summaryAlloc;
    }
    
    private ArrayList<Map<String, HashMap<String, Double>>>sectorsInStateAlloc(List<ConstraintEvaluator> consEvalList){
    	
    	ArrayList<Map<String, HashMap<String, Double>>> sectorsInState = new ArrayList<Map<String, HashMap<String, Double>>>();
        
    	for(ConstraintEvaluator consEval: consEvalList){
            if(consEval.showAllocation() == null){
            	sectorsInState.add(consEval.showAllocationSectorInState());
            }
         }
    	
        return sectorsInState;
    }
    
    
    public ArrayList<Object> bucketsApp2(HashMap<String,String> queryMap){
    	HashMap<String, List<Security>> groupedByRanking = new HashMap<String, List<Security>>();
    	TreeMap<Integer, HashMap<String, List<Security>>> groupedByBucket = new TreeMap<Integer, HashMap<String,List<Security>>>();
        ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>> bucketsByRanking = new ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>>();
        
        SortedMap<Integer, ArrayList<Security>> allocatedData = new TreeMap<Integer, ArrayList<Security>>();
        
        Long acctSize = 0L;
        
        max = Integer.valueOf(queryMap.get("max"));
        min = Integer.parseInt(queryMap.get("min"));
        acctSize = Long.parseLong(queryMap.get("investedAmount"));
        
        for(int i = min; i <= max; i++){
            buckets.add(i);
        }
        
        LadderConfig ladderConfig = new LadderConfig(min, max, getNumBondsPerBucket(min, max), max - min + 1, acctSize, 0.0);
      
        List<Security> filteredBonds = this.bonds.stream()
            .filter(this::filterBonds)
            .collect(Collectors.toList());
    
        Collections.sort(filteredBonds);
        
        SortedMap<Integer, List<Security>> matYrSecListMap = new TreeMap<Integer, List<Security>>(new YrToMatComparator());
        for(Security sec: filteredBonds){
            List<Security> matyrSecList = matYrSecListMap.get(sec.getYearsToMaturity());
            if(matyrSecList == null){
                matyrSecList = new ArrayList<Security>();
                matYrSecListMap.put(sec.getYearsToMaturity(), matyrSecList);
            }
            matyrSecList.add(sec);
        }
        if(debug){
            for(Map.Entry<Integer, List<Security>> entry : matYrSecListMap.entrySet()){
                Object o = ("----- Maturity Year: " + entry.getKey() + ". Number of securities: " + entry.getValue().size());
                System.out.println(o.toString());
                
                for(Security sec: entry.getValue()){
                    System.out.print(sec.toString());
                }
            }
        }
        
        List<ConstraintEvaluator> consEvalList = createConstraintEvaluatorsList(ladderConfig);
        
        List<LadderBucket> ladBucketList = new ArrayList<LadderBucket>();
        generateLadder(ladBucketList, matYrSecListMap, ladderConfig, consEvalList, allocatedData);
        
        ArrayList<Object> summary = new ArrayList<Object>();
        ArrayList<Map<String, Double>> summaryAlloc = new ArrayList<Map<String, Double>>();
        
        summaryAlloc = allocatedByConstraints(consEvalList);
    	RatingCalculations avgAndMedRating = new RatingCalculations(allocatedData, ladderConfig.getOriginalAccountSize(), summaryAlloc);
    	Map<String, String> ratingStats = avgAndMedRating.calcPortAvgAndMedRating();
    	
        ArrayList<Map<String, Double>> sortedByAllocAmount = new ArrayList<Map<String, Double>>();
    	summaryAlloc.forEach(alloc->{
    		
    				Map<String, Double>sorted = alloc.entrySet().stream()
        			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
        			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
        					(oldValue, newValue) -> oldValue, LinkedHashMap::new));
    		
        			 sortedByAllocAmount.add(sorted);
    	});
        	
        ArrayList<Map<String, HashMap<String, Double>>> sortedSectorsInState = new ArrayList<Map<String, HashMap<String, Double>>>();
        Map<String, HashMap<String, Double>> sectorAmounts = new TreeMap<String, HashMap<String, Double>>();
        sectorsInStateAlloc(consEvalList).forEach(alloc -> {
        	HashMap<String, Double> sortedSectors;
        	for(Entry<String, HashMap<String, Double>> entry: alloc.entrySet()){
        		HashMap<String, Double> test = entry.getValue();
        		sortedSectors = test.entrySet().stream()
        		.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
    			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
    					(oldValue, newValue) -> oldValue, LinkedHashMap::new));
        		System.out.println(sortedSectors);
        		sectorAmounts.put(entry.getKey(), sortedSectors);
        		
        	}
        	sortedSectorsInState.add(sectorAmounts);
        });
       
        summary.add(sortedByAllocAmount);
        summary.add(sortedSectorsInState);
        summary.add(ratingStats);
        summary.add(allocatedData);
        
        return summary;
       
    }
    
    private void generateLadder(List<LadderBucket> ladBucketList, SortedMap<Integer, List<Security>> matYrSecListMap, LadderConfig ladderConfig, List<ConstraintEvaluator> consEvalList, 
    		SortedMap<Integer, ArrayList<Security>> allocatedData){
    	
        int matYr = matYrSecListMap.firstKey();
        LadderBucket ladderBucket = new LadderBucket(matYr);
        ArrayList<Security> bucketBonds = new ArrayList<Security>();
        
        for(ConstraintEvaluator consEval: consEvalList){
            ladderBucket.addObserver(consEval);
        }
        
        ladBucketList.add(ladderBucket);
        List<Security> secList = matYrSecListMap.get(matYr);
        long totDollarRoundedAmt = 0;
        int numBondsSelected = 0;
        
        for(Security sec: secList){
            //get the raw amt
            long rawParAmt = (long)(ladderConfig.getLatestAccountSize()*PAR_PRICE/ladderConfig.getLatestMaturityRange()/sec.getPrice()/ladderConfig.numBondsPerBucket);
            //get the rounded amount
            long roundedParAmt = Math.round((double)(rawParAmt/MIN_INCREMENT))*MIN_INCREMENT;
            if(evaluateConstraints(consEvalList, sec, roundedParAmt, ladBucketList) == null){
                totDollarRoundedAmt += roundedParAmt*sec.getPrice()/PAR_PRICE;
                ladderBucket.addSecurityParAmount(new SecurityParAmt(sec, rawParAmt, roundedParAmt));
                numBondsSelected++;
                if(numBondsSelected == ladderConfig.numBondsPerBucket){
                    break;
                }
            }
        }
        //because of rounding, the totDollarRoundedAmt will not be equal to totDollarBucketAmt. so try to come close to it
        long totDollarBucketAmt = ladderConfig.getLatestAccountSize()/ladderConfig.getLatestMaturityRange();
        List<SecurityParAmt> secParAmtList = ladderBucket.getSecurityParAmtList();
        Collections.sort(secParAmtList);
        
        if(debug){
        	Object o = "--- TotDollarBucketAmt: " + ALLOC_FORMAT.format(totDollarBucketAmt) + ". TotDollarRoundedAmt: " + ALLOC_FORMAT.format(totDollarRoundedAmt);
            System.out.println(o.toString());
            for(SecurityParAmt secParAmt : secParAmtList){
                System.out.println(secParAmt.toString());
            }
        }
        if(totDollarRoundedAmt < totDollarBucketAmt){
            for(SecurityParAmt secParAmt : secParAmtList){
                double adjDollarAmt = MIN_INCREMENT*secParAmt.sec.getPrice()/PAR_PRICE;
                if(totDollarRoundedAmt + adjDollarAmt <= totDollarBucketAmt){
                    ladderBucket.addRoundedParAmount(secParAmt.sec, MIN_INCREMENT);
                    totDollarRoundedAmt += adjDollarAmt;
                }
                else{
                    if(ladderConfig.getLatestMaturityRange()%2 == 0){
                        ladderBucket.addRoundedParAmount(secParAmt.sec, MIN_INCREMENT);
                        totDollarRoundedAmt += adjDollarAmt;
                    }
                    break;
                }
            }
        }
        else if(totDollarRoundedAmt > totDollarBucketAmt){
            for(SecurityParAmt secParAmt : secParAmtList){
                double adjDollarAmt = MIN_INCREMENT*secParAmt.sec.getPrice()/PAR_PRICE;
                if(totDollarRoundedAmt - adjDollarAmt >= totDollarBucketAmt){
                    ladderBucket.subtractRoundedParAmount(secParAmt.sec, MIN_INCREMENT);
                    totDollarRoundedAmt -= adjDollarAmt;
                }
                else{
                    if(ladderConfig.getLatestMaturityRange()%2 == 0){
                        ladderBucket.subtractRoundedParAmount(secParAmt.sec, MIN_INCREMENT);
                        totDollarRoundedAmt -= adjDollarAmt;
                    }
                    break;
                }
            }
        }
       
        Collections.sort(secParAmtList);
        for(SecurityParAmt secParAmt : secParAmtList){
        	
        	Double investedAmt = secParAmt.getDollarAmount();
        	secParAmt.sec.setInvestAmt(investedAmt);
   
            bucketBonds.add(secParAmt.sec);
            allocatedData.put(secParAmt.sec.getYearsToMaturity(), bucketBonds);
            System.out.println(allocatedData.toString());
            
        }
    
        matYrSecListMap.remove(matYr);
        if(matYrSecListMap.size() > 0){
            ladderConfig.addMatRangeAcctSize(ladderConfig.getLatestMaturityRange()-1, ladderConfig.getLatestAccountSize() - ladderBucket.getLadderDollarAmount());
            generateLadder(ladBucketList, matYrSecListMap, ladderConfig, consEvalList, allocatedData);
        }
    }
    
    public void initSD(){
//    	try{
        	//for debug output
        	//buffWriter = new BufferedWriter (new FileWriter("L:\\Savinder\\Java\\LadderAllocationOutput.txt"));
//    		buffWriter = new BufferedWriter (new FileWriter("\\\\tfiappsq1-jitv\\DataShare\\MuniLadder\\LadderAllocationOutput.txt"));
        	//get the default date
//        	defPurchaseDate = DATE_FORMAT.parse("01/01/2001");
        	
        	secPriorityList.add(SecPriority.ABOVE_A_HEALTHCARE_NON_NY_CA);
        	secPriorityList.add(SecPriority.ABOVE_A_NY);
        	secPriorityList.add(SecPriority.ABOVE_A_CA);
        	secPriorityList.add(SecPriority.ABOVE_A_HEALTHCARE_NY);
        	secPriorityList.add(SecPriority.ABOVE_A_HEALTHCARE_CA);
        	secPriorityList.add(SecPriority.A_OR_BELOW_HEALTHCARE_NON_NY_CA);
        	secPriorityList.add(SecPriority.A_OR_BELOW_NY);
        	secPriorityList.add(SecPriority.A_OR_BELOW_CA);
        	secPriorityList.add(SecPriority.A_OR_BELOW_HEALTHCARE_NY);
        	secPriorityList.add(SecPriority.A_OR_BELOW_HEALTHCARE_CA);
        	secPriorityList.add(SecPriority.A_OR_BELOW_NON_NY_CA);
        	secPriorityList.add(SecPriority.ABOVE_A_NON_NY_CA);
        	secPriorityList.add(SecPriority.NO_PRIORITY);
//    	}
//    	catch(IOException ioe){
//            ioe.printStackTrace();
//        } catch (ParseException e) {
//    		// TODO Auto-generated catch block
//    		e.printStackTrace();
//    	}		
    }
    
    public enum SecPriority
	{
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





