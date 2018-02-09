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

    private boolean debug = true;
    
    List<Security> bonds = new FileLoader().getSecList();
    private int min;
    private int max;
    
    @PostConstruct
    public void init() throws IOException{
        FileLoader fileLoader = new FileLoader();
        fileLoader.loadFile();
    }

    //public Collection<Security> buckets(HashMap<String,String> queryMap){
//    public HashMap<Integer, ArrayList<Security>> buckets(HashMap<String,String> queryMap){
    public ArrayList<Object> buckets(HashMap<String,String> queryMap){
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
        
        LadderConfig ladderConfig = new LadderConfig(min, max, getNumBondsPerBucket(min, max), max - min + 1, acctSize);
      
        List<Security> filteredBonds = this.bonds.stream()
            .filter(this::filterBonds)
            .collect(Collectors.toList());
        
        Collections.sort(filteredBonds);
        
  
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
        	List<Security> couponRated = bucketSecurity;
        	Collections.sort(couponRated, new Comparator<Security>(){
        		@Override
        		public int compare(Security s1, Security s2){
        			return Double.compare( s2.getCoupon(),s1.getCoupon());
        		
        		}
        	});
        	
        	groupedByRanking.put("HealthCare", healthCareBonds);
        	groupedByRanking.put("nyBonds", nyBonds);
        	groupedByRanking.put("caBonds",caBonds);
        	groupedByRanking.put("aRatedBonds", nyBonds);
        	groupedByRanking.put("aaRatedBonds",caBonds);
        	groupedByRanking.put("couponRated", couponRated);
        	
//        	for(Map.Entry<String,List<Security>> entry:groupedByRanking.entrySet()){
//        		System.out.print(entry.getKey());
//        		for(Security sec: entry.getValue()){
//        			System.out.println(sec.toString());
//        		}
//        	}
        	
        	groupedByBucket.put(bucket,groupedByRanking);
        	bucketsByRanking.add(groupedByBucket);
        }
        
       
//        for(TreeMap<Integer, HashMap<String, List<Security>>> entry: bucketsByRanking){
//        	for(Integer key : entry.keySet()){
//        		HashMap hm = entry.get(key);
//        		System.out.println("key= " + key);
//        		System.out.println("value= " + hm);
//        	}	
//        }
       
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
        
//        ArrayList<SortedMap<String, Double>> summaryAlloc = new ArrayList<SortedMap<String, Double>>();
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
        
//        return filteredBonds;
    }

    private class LadderBucket extends Observable{
        private int matYr;
        private Map<Security, SecurityParAmt> secParAmtMap = new HashMap<Security, SecurityParAmt>();

        private LadderBucket(int matYr){
            this.matYr = matYr;
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
         private List<Integer> matRangeList = new ArrayList<Integer>();
         private List<Long> acctSizeList = new ArrayList<Long>();

         private LadderConfig(int minYr, int maxYr, int numBondsPerBucket, int matRange, long acctSize){
             this.minYr = minYr;
             this.maxYr = maxYr;
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

        protected abstract boolean evaluate (Security sec, long parAmt);
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
            this.roundedParAmt += adjAmt;
        }

        private double getDollarAmount(){
            return sec.getPrice()*roundedParAmt/PAR_PRICE;
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
        private StateSectorConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }

        private String getStateSectorKey(Security sec){
            return sec.getState() + "::" + sec.getSector();
        }

        protected boolean evaluate (Security sec, long parAmt){
            double sectorPct = stateSectorPctMap.get(getStateSectorKey(sec)) == null ? 0: stateSectorPctMap.get(getStateSectorKey(sec));
            return sectorPct + getSecurityPct(sec, parAmt) < MAX_SECTOR_STATE_PCT;
        }

        public void update(Observable o, Object arg) {
            SecurityParAmt secParAmt = (SecurityParAmt)arg;
            double sectorPct = stateSectorPctMap.get(getStateSectorKey(secParAmt.sec)) == null ? 0: stateSectorPctMap.get(getStateSectorKey(secParAmt.sec));
   
        	HashMap<String, Double>sectorInState = new HashMap<String, Double>();
        	sectorInState.put(secParAmt.sec.getSector(), sectorPct + getUpdatedSecurityPct(secParAmt));
        	stateSectorMap.put(secParAmt.sec.getState(), sectorInState);
//            	stateSectorPctMap.put(getStateSectorKey(secParAmt.sec), sectorPct + getUpdatedSecurityPct(secParAmt));
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

        private SectorConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }
        
        private String getSectorKey(Security sec){
            return sec.getSector();
        }

        protected boolean evaluate (Security sec, long parAmt){
            double sectorPct = sectorPctMap.get(getSectorKey(sec)) == null ? 0: sectorPctMap.get(getSectorKey(sec));
            return sectorPct + getSecurityPct(sec, parAmt) < MAX_SECTOR_PCT;
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
            	
            	sectorPctMap.put("Cash", getOriginalAccountSize() - allocatedAmount);
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

        private StateConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }
        
        private String getStateKey(Security sec){
            return sec.getState();
        }
        
        protected boolean evaluate (Security sec, long parAmt){
            double statePct = statePctMap.get(getStateKey(sec)) == null ? 0: statePctMap.get(getStateKey(sec));
            return statePct + getSecurityPct(sec, parAmt) < MAX_STATE_PCT;
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

        private AOrBelowConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }
        
        private FileLoader.SecRating getTwoGroupsRatingKey(Security sec){
            return sec.getTwoGroupsRating();
        }

        protected boolean evaluate (Security sec, long parAmt){
            if(getTwoGroupsRatingKey(sec) == FileLoader.SecRating.A_OR_BELOW){
                return aOrBelowPct + getSecurityPct(sec, parAmt) < MAX_A_OR_BELOW_PCT;
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

    private class HealthCareConstraintEvaluator extends ConstraintEvaluator{
        double healthCarePct = 0;

        private HealthCareConstraintEvaluator (LadderConfig ladderConfig){
            super(ladderConfig);
        }
        
        private String getSectorKey(Security sec){
            return sec.getSector();
        }
        
        protected boolean evaluate (Security sec, long parAmt){
            if(SECTOR_HEALTHCARE.equals(getSectorKey(sec))){
                return healthCarePct + getSecurityPct(sec, parAmt) < MAX_HEALTHCARE_PCT;
            }
            else{
                return true;
            }
        }

        public void update(Observable o, Object arg) {
            SecurityParAmt secParAmt = (SecurityParAmt)arg;
            if(SECTOR_HEALTHCARE.equals(getSectorKey(secParAmt.sec))){
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
            if(evaluateConstraints(consEvalList, sec, roundedParAmt)){
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
    
    private boolean evaluateConstraints(List<ConstraintEvaluator> consEvalList, Security sec, long parAmt){
        for(ConstraintEvaluator consEval: consEvalList){
            boolean constraintPassed = consEval.evaluate(sec, parAmt);
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
}
