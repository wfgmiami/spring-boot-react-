package com.lordabbett.muniladder.service;

import com.lordabbett.muniladder.model.FileLoader;
import com.lordabbett.muniladder.model.Security;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Service
public class Allocation {
	
	private static final String NEW_LINE = System.getProperty("line.separator");
    private static final DecimalFormat PCT_FORMAT =  new DecimalFormat("#0.00%");
    private static final DecimalFormat PRICE_FORMAT =  new DecimalFormat("#0.00");
    private static final DecimalFormat ALLOC_FORMAT =  new DecimalFormat("##,##0");
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
    public static final int MIN_PAR = 40000;
    public static final int MAX_PAR = 75000;
    public static final double MAX_BOND_PCT = 10;
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
        
        generateLadder(ladBucketList, bucketsByRank, ladderConfig, consEvalList, allocatedData);
        
        ArrayList<Object> summaryAlloc = new ArrayList<Object>();
//        summaryAlloc = reduceCashBalance(allocatedData,consEvalList,bucketsByRankingFinal,ladderConfig);
        
        for(ConstraintEvaluator consEval: consEvalList){
           System.out.println(consEval.toString());
           if(consEval.showAllocation() == null){
        	   summaryAlloc.add(consEval.showAllocationSectorInState());
           }else{
        	   summaryAlloc.add(consEval.showAllocation());
           }
        }
        
        summaryAlloc.add(allocatedData);
        return summaryAlloc;

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

        private ConstraintEvaluator (LadderConfig ladderConfig){
            this.ladderConfig = ladderConfig;
        }

        public SortedMap<String, Double> showAllocation() {
			// TODO Auto-generated method stub
			return null;
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
//                updatedSecPct -= oldSecParAmt.getDollarAmount()/ladderConfig.getOriginalAccountSize();
                updatedSecPct -= oldSecParAmt.getDollarAmount();
            }
//            return 100*(updatedSecPct + secParAmt.getDollarAmount()/ladderConfig.getOriginalAccountSize());
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
//        public int compareTo(Object o) {
//            if (getDollarAmount() < ((SecurityParAmt)o).getDollarAmount()) {
//                return -1;
//            }
//            else if (getDollarAmount() > ((SecurityParAmt)o).getDollarAmount()) {
//                return 1;
//            }
//            else {
//                return 0;
//            }
//        }

        public int compareTo(Object o) {
            if (getRank() < ((SecurityParAmt)o).getRank()) {
                return -1;
            }
            else if (getRank() > ((SecurityParAmt)o).getRank()) {
                return 1;
            }
            else {
                return 0;
            }
        }
        
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
        
        private StateSectorConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }

        private String getStateSectorKey(Security sec){
            return sec.getState() + "::" + sec.getSector();
        }

        protected boolean evaluate (Security sec, long parAmt, List<LadderBucket> ladderBucket){
            double sectorPct = stateSectorPctMap.get(getStateSectorKey(sec)) == null ? 0: stateSectorPctMap.get(getStateSectorKey(sec));
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
//        		if(currentSectorInState.containsKey(sector)){
        			currentSectorInState.put(sector, sectorPct + getUpdatedSecurityPct(secParAmt));
        			stateSectorMap.put(state, currentSectorInState);
//        		}else{
        			
//        		}
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
        
        public SortedMap<String, Double> showAllocation(){
        	return null;
        }
        
        public String toString(){
            StringBuffer sb = new StringBuffer("-----  Total State Sector %: -----------");
            for(Map.Entry<String, Double> entry: stateSectorPctMap.entrySet()){
                sb.append(NEW_LINE).append(entry.getKey()).append(" : ").append(PCT_FORMAT.format(entry.getValue()/100));
            }
            return sb.toString();
        }
    }

    private class SectorConstraintEvaluator extends ConstraintEvaluator{
        SortedMap<String, Double> sectorPctMap = new TreeMap<String, Double>();
        long accountSize = getOriginalAccountSize();
        
        private SectorConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }
        
        private String getSectorKey(Security sec){
            return sec.getSector();
        }

        protected boolean evaluate (Security sec, long parAmt, List<LadderBucket> ladderBucket){
            double sectorPct = sectorPctMap.get(getSectorKey(sec)) == null ? 0: sectorPctMap.get(getSectorKey(sec));
            return sectorPct + getSecurityPct(sec, parAmt) * accountSize / 100 < MAX_SECTOR_PCT * accountSize / 100;
        }

        public void update(Observable o, Object arg) {
            SecurityParAmt secParAmt = (SecurityParAmt)arg;
            double sectorPct = sectorPctMap.get(secParAmt.sec.getSector()) == null ? 0: sectorPctMap.get(secParAmt.sec.getSector());
            sectorPctMap.put(secParAmt.sec.getSector(), sectorPct + getUpdatedSecurityPct(secParAmt));
            super.update(o, arg);
        }

        @Override
        public SortedMap<String, Double> showAllocation(){
        	Double allocatedAmount = 0.0;
        	if(!sectorPctMap.containsKey("Cash")){
        		for(Map.Entry<String, Double>entry:sectorPctMap.entrySet()){
            		allocatedAmount += entry.getValue();
            	}
            	
            	sectorPctMap.put("Cash", accountSize - allocatedAmount);
        	}
        
        	return sectorPctMap;
        }
        
        public String toString(){
            StringBuffer sb = new StringBuffer("-----  Total Sector %: -----------");
            for(Map.Entry<String, Double> entry: sectorPctMap.entrySet()){
                sb.append(NEW_LINE).append(entry.getKey()).append(" : ").append(PCT_FORMAT.format(entry.getValue()/100));
            }
            return sb.toString();
        }
    }

    private class StateConstraintEvaluator extends ConstraintEvaluator{
        SortedMap<String, Double> statePctMap = new TreeMap<String, Double>();
        long accountSize = getOriginalAccountSize();
        
        private StateConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }
        
        private String getStateKey(Security sec){
            return sec.getState();
        }
        
        protected boolean evaluate (Security sec, long parAmt, List<LadderBucket> ladderBucketList){
            double statePct = statePctMap.get(getStateKey(sec)) == null ? 0: statePctMap.get(getStateKey(sec));
            return statePct + getSecurityPct(sec, parAmt) * accountSize / 100 < MAX_STATE_PCT * accountSize / 100;
        }

        public void update(Observable o, Object arg) {
            SecurityParAmt secParAmt = (SecurityParAmt)arg;
            double statePct = statePctMap.get(secParAmt.sec.getState()) == null ? 0: statePctMap.get(secParAmt.sec.getState());
            statePctMap.put(secParAmt.sec.getState(), statePct + getUpdatedSecurityPct(secParAmt));
            super.update(o, arg);
        }

        @Override
        public SortedMap<String, Double> showAllocation(){
        	return statePctMap;
        }

        public String toString(){
            StringBuffer sb = new StringBuffer("-----  Total State %: -----------");
            for(Map.Entry<String, Double> entry: statePctMap.entrySet()){
                sb.append(NEW_LINE).append(entry.getKey()).append(" : ").append(PCT_FORMAT.format(entry.getValue()/100));
            }
            return sb.toString();
        }
    }

    private class AOrBelowConstraintEvaluator extends ConstraintEvaluator{
        double aOrBelowPct = 0;
        long accountSize = getOriginalAccountSize();
        
        private AOrBelowConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }
        
        private FileLoader.SecRating getTwoGroupsRatingKey(Security sec){
            return sec.getTwoGroupsRating();
        }

        protected boolean evaluate (Security sec, long parAmt, List<LadderBucket> ladderBucketList){
            if(getTwoGroupsRatingKey(sec) == FileLoader.SecRating.A_OR_BELOW){
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
        public SortedMap<String, Double> showAllocation(){
        	return this.showAorBelowAlloc();
        }

        public String toString(){
            return "Total A or Below %:" + PCT_FORMAT.format(aOrBelowPct/100);
        }
    }

    public ArrayList<Integer>optimizeHealthCare(List<LadderBucket> ladderBucketList){
    	
    	List<LadderBucket> lb = ladderBucketList;
    	for(int i = 0; i < ladderBucketList.size(); i++){
    		LadderBucket bucket = ladderBucketList.get(i);
    		List<SecurityParAmt> test = bucket.getSecurityParAmtList();
    		test.forEach(bond-> System.out.println(bond.sec.getSector()));
//    		System.out.println(test.toString());
    	
    	}
//    	Security firstBond = (Security) secParAmtMap.keySet().toArray()[0];
//    	Security secondBond = (Security) secParAmtMap.keySet().toArray()[1];
//
//    	double firstBondInvestAmt = firstBond.getInvestAmt();
//    	double secondBondInvestAmt = secondBond.getInvestAmt();
//    	
//		double firstPrice = firstBond.getPrice() /100;
//		double secondPrice = secondBond.getPrice() / 100;
		int investedAmount = 1000000;
		Double maxAllocPerBond = MAX_BOND_PCT * investedAmount;
    	ArrayList<Integer> optimizedPar = new ArrayList<Integer> ();
    	
   	    ArrayList<Integer> parList = getParAmount();
 	
    	for( int par1 = 0; par1 < parList.size() - 1; par1++ ){
			for( int par2 = 0; par2 < parList.size() - 1; par2++ ){
//				if( parList.get(par1) * firstPrice <= maxAllocPerBond && parList.get(par2) * secondPrice <= maxAllocPerBond ){
//					if( parList.get(par1) * firstPrice + parList.get(par2) * secondPrice === allocLimit){
//						trackObj.closeToLimit.push( 0 );
//						trackObj.parOne.push( parList[par1] );
//						trackObj.parTwo.push( parList[par2] );
//					}else if( parList.get(par1) * firstPrice + parList.get(par2) * secondPrice < allocLimit ){
//						closeToLimit = allocLimit - ( parList.get(par1) * firstPrice + parList.get(par2) * secondPrice );
//						trackObj.closeToLimit.push( closeToLimit );
//						trackObj.parOne.push( parList.get(par1) );
//						trackObj.parTwo.push( parList.get(par2) );
//					}
//				}
			}
		}

//		let closest = Math.min( ...trackObj.closeToLimit );
//
//		for( let i = 0; i < trackObj['closeToLimit'].length; i++ ){
//			if( trackObj['closeToLimit'][i] === closest ) return [ trackObj.parOne[i], trackObj.parTwo[i] ];
//		}
//    	
    	optimizedPar.add(50000);
    	optimizedPar.add(55000);
    	return optimizedPar;
    	
    }
    
    private class HealthCareConstraintEvaluator extends ConstraintEvaluator{
        double healthCarePct = 0;
        long accountSize = getOriginalAccountSize();
        
        private HealthCareConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }
        
        private String getSectorKey(Security sec){
            return sec.getSector();
        }
        
        protected boolean evaluate (Security sec, long parAmt, List<LadderBucket> ladderBucketList){
            if(SECTOR_HEALTHCARE.equals(getSectorKey(sec))){
            	//healthCarePct is $amt not %
            	
            	if(!(healthCarePct + getSecurityPct(sec, parAmt) * accountSize / 100 < MAX_HEALTHCARE_PCT * accountSize / 100)){
            		optimizeHealthCare(ladderBucketList);
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
        public SortedMap<String, Double> showAllocation(){
        	return this.showHealthCareAlloc();
        }
        
        
        public String toString(){
            return "Total Healthcare %:" + PCT_FORMAT.format(healthCarePct/100);
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
		long allocSize = MIN_PAR + 3 * MIN_INCREMENT;

		if( Math.floor( bucketSize / allocSize ) < 2 ){
			allocSize = MIN_PAR;
		}
		
		if( allocSize * chosenBond.getPrice() / 100 > maxBondSize ){
			int bondIdx = ladderBucket.getBondIndex();
			double testPrice = 0.0;

			do{
				List<Security> secInBucketByRanking = secInBucket.get(RANKING[currentRankIndex]);
				
				for( long parSize = MAX_PAR - MIN_INCREMENT; parSize >= MIN_PAR; parSize -= MIN_INCREMENT ){
					do{
						testPrice = secInBucketByRanking.get(bondIdx).getPrice();
						bondIdx++;
					}while( bondIdx <  secInBucketByRanking.size() && parSize * testPrice / 100 > maxBondSize );
					allocSize = parSize;
					if( allocSize * testPrice / 100 <= maxBondSize ) break;
					bondIdx = ladderBucket.getBondIndex();
				}

				if( allocSize * testPrice / 100 > maxBondSize ){
					boolean foundBond = false;
					
					while( !foundBond && currentRankIndex < RANKING.length - 1 ){
						chosenBond = secInBucket.get(RANKING[++currentRankIndex]).get(0);
					}
				
					if( !foundBond ){
						return 0;
					}
			
					bondIdx = 0;
					allocSize = MAX_PAR;
					
				}else{
					chosenBond = secInBucket.get(RANKING[++currentRankIndex]).get(--bondIdx);
					return (long)(allocSize * chosenBond.getPrice() / PAR_PRICE);
				}

			} while ( currentRankIndex < RANKING.length );
	
			return 0;

		}else if ( MIN_PAR * chosenBond.getPrice() / 100  > investedAmount ) {
			return 0;
		}else{
			return (long)(allocSize * chosenBond.getPrice() / PAR_PRICE);
		}
		
    }
    
    private void generateLadder(List<LadderBucket> ladBucketList,  ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>> bucketsByRank, LadderConfig ladderConfig, List<ConstraintEvaluator> consEvalList, 
    		SortedMap<Integer, ArrayList<Security>> allocatedData){
    	
    	ArrayList<Integer> buckets = getBuckets();  	
//    	ArrayList<LadderBucket>ladderBucketList = new ArrayList<LadderBucket>();
    	double totDollarBucketAmt = ladderConfig.getAccountSize()/ladderConfig.getLatestMaturityRange();
    	int numBuckets = buckets.size();
    	boolean moveToNextBucket = true;
    	
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
		int currentRankIndex = 0;
		int aaRatedRankIndex = Arrays.asList(RANKING).indexOf("aaRatedBonds");
		Security chosenBond = null;
		
    	do {
    		LadderBucket ladderBucket = ladBucketList.get(bucketIndex);
    		currentRankIndex = ladderBucket.getRankIndex();
    		int currentBondIndex = ladderBucket.getBondIndex();
    		
	        ArrayList<Security> bucketBonds = new ArrayList<Security>();
	        if(currentRankIndex < RANKING.length) {
	        	if(bucketsByRank.get(bucketIndex).get(bucketMatYr).get(RANKING[currentRankIndex]).isEmpty() && currentRankIndex < RANKING.length) {
	        		chosenBond = lookForBondInDiffRanking( ladderBucket, bucketsByRank.get(bucketIndex).get(bucketMatYr) );
	        	}else {
	        		chosenBond = bucketsByRank.get(bucketIndex).get(bucketMatYr).get(RANKING[currentRankIndex]).get(currentBondIndex);
	        	}
	        		
	        	
	        	long rawParAmt = getParAmount(chosenBond, ladderBucket, ladderConfig, bucketsByRank.get(bucketIndex).get(bucketMatYr));
	 
		        long roundedParAmt = Math.round((double)(rawParAmt/MIN_INCREMENT))*MIN_INCREMENT;

	            if( evaluateConstraints(consEvalList, chosenBond, roundedParAmt, ladBucketList) ){

	                ladderBucket.addSecurityParAmount(new SecurityParAmt(chosenBond, rawParAmt, roundedParAmt));
	                ladderBucket.increaseBondIndex();                

	            }else {
	            	moveToNextBucket = false;
	            	if( currentRankIndex != aaRatedRankIndex ){
	                  	ladBucketList.forEach( bucket -> { 
		            		bucket.increaseRankIndex();
		            		bucket.setBondIndex(0);
		            	});
	            	}else{
	            		ladderBucket.increaseBondIndex();   
	            	}
	            }
	                 
		        
	            if( ladderBucket.getLadderDollarAmt() >= totDollarBucketAmt ) {
	            	
	            	ladderBucket.subtractRoundedParAmount(chosenBond, roundedParAmt);
	            	ladderBucket.removeSecurity(chosenBond);
	            	
	            	List<SecurityParAmt> secParAmtList = ladderBucket.getSecurityParAmtList();
	   		        Collections.sort(secParAmtList);
	   		               
	        		double leftInCash =  totDollarBucketAmt - ladderBucket.getLadderDollarAmt();		
	        		
	    	        for(SecurityParAmt secParAmt : secParAmtList){   	        	
	    	        	Double investedAmt = secParAmt.getDollarAmount();
	    	        	secParAmt.sec.setInvestAmt(investedAmt);
	    	            bucketBonds.add(secParAmt.sec);         
	    	        }
	        		
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
			
//    		numBuckets--;
    	}while(numBuckets > 0);
    	
    		
    }
    
    private HashMap<Double, String> checkLimits(Security testBond, double investedAmount, double allocatedCash, Map<String, Double> allocRating, Map<String, Double> allocSector, Map<String, Double> allocState){
    	
		double maxHealthCare = MAX_HEALTHCARE_PCT * investedAmount / 100;
		double maxSector = MAX_SECTOR_PCT * investedAmount / 100;
		double maxNYState = MAX_STATE_PCT * investedAmount / 100;
		double maxCAState = MAX_STATE_PCT * investedAmount / 100;
		double maxState = MAX_STATE_PCT * investedAmount / 100;
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
    	
    	String limitType = sector;
		
		allocationLimit = maxSector - allocSector.get(sector);

		leftRoom = maxState -  allocState.get(state);
		if( allocationLimit > leftRoom ){
			allocationLimit = leftRoom;
			limitType = state;
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
    
    private ArrayList<Object> reduceCashBalance(SortedMap<Integer, ArrayList<Security>>allocatedData, List<ConstraintEvaluator>consEvalList, ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>> bucketsByRankingFinal,
    		LadderConfig ladderConfig){
    	ArrayList<Integer> buckets = getBuckets();
    	
        ArrayList<Object> summaryAlloc = new ArrayList<Object>();
        for(ConstraintEvaluator consEval: consEvalList){
           if(consEval.showAllocation() == null){
        	   summaryAlloc.add(consEval.showAllocationSectorInState());
           }else{
        	   summaryAlloc.add(consEval.showAllocation());
           }
        }
        ArrayList<TreeMap<Integer,HashMap<String, List<Security>>>> b = bucketsByRankingFinal;
        
    	buckets.forEach( bucketNumber -> {
    		
    		double investedAmount = ladderConfig.getAccountSize();
    		double minIncrementToAllocate = 0.0;
            double allocationLimit = 0.0;
            int bondNum = 0;
			int checkIncrements = 0;
			boolean maxIncrement = false;
			boolean allocCheck = false;
			int bucketIndex = buckets.indexOf(bucketNumber);
			
			HashMap<String, Boolean> trackOverLimit = new HashMap<String, Boolean>();
			HashMap<Double, String> allocationLimitType = new HashMap<Double, String>();
	        
			Map<String, Double> allocHealthCare = ( TreeMap<String, Double> ) summaryAlloc.get(0);
	        Map<String, Double> allocRating = ( TreeMap<String, Double> ) summaryAlloc.get(1);
	        Map<String, Double> allocState = ( TreeMap<String, Double> ) summaryAlloc.get(2);
	        Map<String, Double> allocSector = ( TreeMap<String, Double> ) summaryAlloc.get(3);
	        
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
						allocationLimitType = checkLimits(testBond, investedAmount, allocatedCash, allocRating, allocSector, allocState);
						allocationLimit = (double) allocationLimitType.keySet().toArray()[0];
						
						minIncrementToAllocate = ( allocatedCash ) / ( MIN_PAR * ( price * 1 / 100 ) );
						bondNum = (int) Math.floor( minIncrementToAllocate );

						checkIncrements = 0;
						maxIncrement = false;

						if( bondNum > 0 ){

							do{
								minIncrementToAllocate = ( MIN_PAR  + ( checkIncrements ) * MIN_INCREMENT )  * ( price * 1 / 100 );

								if( minIncrementToAllocate > 0 && minIncrementToAllocate <= allocationLimit && ( minIncrementToAllocate <= MAX_BOND_PCT / 100 * investedAmount ) && ( minIncrementToAllocate / 
										( testBond.getPrice() / 100 ) ) <= MAX_PAR ){
									checkIncrements++;
								}else{
									maxIncrement = true;
								}
							} while( !maxIncrement );
						}

						if( checkIncrements > 0 ) --checkIncrements;
						minIncrementToAllocate = ( MIN_PAR  + ( checkIncrements ) * MIN_INCREMENT )  * ( price * 1 / 100 );

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
							
							double currentAllocCash = allocSector.get("Cash") - allocatedCash;
//							allocSector.put("Cash", currentAllocCash);
							allocatedCash = bucket.get(bucketLength).getInvestAmt();
							allocSector.put("Cash", currentAllocCash + allocatedCash);
							
							bucket.add(bucket.size() - 1, testBond);
							bucketLength++;
						}
					}
					rankIndex++;

				}while( rankIndex < RANKING.length );
				
			}
			
			for( int i = 0; i < bucketLength; i++ ){

				double price = bucket.get(i).getPrice();
				String sector =  bucket.get(i).getSector();
				String state =  bucket.get(i).getState();
				String limitType = sector;
				
				testBond = bucket.get(i);
//				allocationLimit = checkLimits(testBond, investedAmount, allocatedCash, allocRating, allocState, allocSector);
				allocationLimitType = checkLimits(testBond, investedAmount, allocatedCash, allocRating, allocSector, allocState);
				allocationLimit = (double) allocationLimitType.keySet().toArray()[0];
				limitType = allocationLimitType.values().toString();
				
				
				minIncrementToAllocate = ( allocatedCash ) / ( MIN_INCREMENT * ( price * 1 / 100 ) );
				bondNum = (int) Math.floor( minIncrementToAllocate );
				checkIncrements = 1;
				maxIncrement = false;
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
					
					double currentAllocCash = allocSector.get("Cash") - allocatedCash;
					allocatedCash = bucket.get(bucketLength).getInvestAmt();
					allocSector.put("Cash", currentAllocCash + allocatedCash);

				}		
			}	
						
    	});
    	
    	return summaryAlloc;
    }
  
    private boolean evaluateConstraints(List<ConstraintEvaluator> consEvalList, Security sec, long parAmt, List<LadderBucket> ladderBucketList){
        for(ConstraintEvaluator consEval: consEvalList){
            boolean constraintPassed = consEval.evaluate(sec, parAmt, ladderBucketList);
            if(!constraintPassed){
                return false;
            }
        }
        return true;
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
    
    
    
    public ArrayList<Object> bucketsApp2(HashMap<String,String> queryMap){
    	ArrayList<Integer> buckets = new ArrayList<Integer>();
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
        
        ArrayList<Object> summaryAlloc = new ArrayList<Object>();
        for(ConstraintEvaluator consEval: consEvalList){
           System.out.println(consEval.toString());
           if(consEval.showAllocation() == null){
        	   summaryAlloc.add(consEval.showAllocationSectorInState());
           }else{
        	   summaryAlloc.add(consEval.showAllocation());
           }
        }
        
        summaryAlloc.add(allocatedData);
       
        return summaryAlloc;
        
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
            if(evaluateConstraints(consEvalList, sec, roundedParAmt, ladBucketList)){
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
    
}



