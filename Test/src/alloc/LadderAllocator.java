package alloc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.SortedMap;
import java.util.TreeMap;



/**
 * 
 * @author SDHALIWAL
 *
 */
public class LadderAllocator {
	private static final String NEW_LINE = System.getProperty("line.separator");
	private static final DecimalFormat PCT_FORMAT =  new DecimalFormat("#0.00%");
	private static final DecimalFormat PRICE_FORMAT =  new DecimalFormat("#0.00");
	private static final DecimalFormat ALLOC_FORMAT =  new DecimalFormat("##,##0");
	private static final int CURRENT_YEAR = Integer.valueOf(new SimpleDateFormat("yy").format(Calendar.getInstance().getTime()));
	private static DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");	
	private static boolean debug = true;
	private static BufferedWriter buffWriter = null;
	private static List<SecPriority> secPriorityList = new ArrayList<SecPriority>();
	private static Date defPurchaseDate = null; 

	//some constants that may eventually be passed as parameters
	private static final int MIN_PRICE = 100;
	private static final int MAX_PRICE_ONE = 105;
	private static final int MAX_PRICE_OTHER = 112;
	private static final int MIN_NUM_BONDS = 10;
	private static final long MIN_INCREMENT = 5000;
	private static final int PAR_PRICE = 100;
	private static final String SECTOR_HEALTHCARE = "Health Care";
	private static final String STATE_NY = "NY";
	private static final String STATE_CA = "CA";
	private static final double MAX_HEALTHCARE_PCT = 12;
	private static final double MAX_STATE_PCT = 20;
	private static final double MAX_SECTOR_PCT = 30;
	private static final double MAX_A_OR_BELOW_PCT = 30;
	private static final double MAX_SECTOR_STATE_PCT = 10;
	
	/**
	 * 
	 * @param minYr
	 * @param maxYr
	 * @param acctSize
	 */
	public void allocate(int minYr, int maxYr, long acctSize){
		LadderConfig ladderConfig = new LadderConfig(minYr, maxYr, getNumBondsPerBucket(minYr, maxYr), maxYr - minYr + 1, acctSize);
		//get the list of securities
    	List<Security> origSecList = loadSecurityData();
    	//get the filtered list
    	List<Security> secList = getFilteredList(origSecList, minYr, maxYr);
    	//sort the list
    	System.out.println("h");
    	Collections.sort(secList);
    	if(debug){
	    	for(Security sec: secList){
	    		print(sec);
	    	}
    		print("--- LadderConfig: " + ladderConfig);
    	}
    	//get the mat year buckets
    	SortedMap<Integer, List<Security>> matYrSecListMap = new TreeMap<Integer, List<Security>>(new MatYrComparator());
    	for(Security sec: secList){
    		List<Security> matyrSecList = matYrSecListMap.get(sec.matYr);
    		if(matyrSecList == null){
    			matyrSecList = new ArrayList<Security>();
    			matYrSecListMap.put(sec.matYr, matyrSecList);
    		}
    		matyrSecList.add(sec);
    	}
    	if(debug){
	    	for(Map.Entry<Integer, List<Security>> entry : matYrSecListMap.entrySet()){
	    		print("----- Maturity Year: " + entry.getKey() + ". Number of securities: " + entry.getValue().size());
		    	for(Security sec: entry.getValue()){
		    		print(sec);
		    	}
			}
    	}
    	//create ConstraintEvaluator list
    	List<ConstraintEvaluator> consEvalList = createConstraintEvaluatorsList(ladderConfig);
    	//do ladder allocation
		List<LadderBucket> ladBucketList = new ArrayList<LadderAllocator.LadderBucket>();
		doRecursiveLadderAllocation(ladBucketList, matYrSecListMap, ladderConfig, consEvalList);
		print("--- LadderConfig: " + ladderConfig);
		long totalAllocAmt = 0;
		for(LadderBucket ladderBucket: ladBucketList){
			print("##### Maturity Bucket: " + ladderBucket.matYr + ". %: " + PCT_FORMAT.format((double)ladderBucket.getLadderDollarAmount()/ladderConfig.getOriginalAccountSize()));
			print(ladderBucket);
			totalAllocAmt += ladderBucket.getLadderDollarAmount();
		}
		print("------- Constraint Evaluation Values -------");
		for(ConstraintEvaluator consEval: consEvalList){
			print(consEval);
		}
		print("------- FINAL DATA -------");
		print("Account size: " + ALLOC_FORMAT.format(ladderConfig.getOriginalAccountSize()) + 
				". Initial bucket amount: " + ALLOC_FORMAT.format(ladderConfig.getOriginalAccountSize()/ladderConfig.getOriginalMaturityRange()) +
				". Initial bucket %: " + PCT_FORMAT.format((double)1/ladderConfig.getOriginalMaturityRange()) +
				". Total allocated amount: " + ALLOC_FORMAT.format(totalAllocAmt) +
				". Cash %: " + PCT_FORMAT.format((double)(ladderConfig.getOriginalAccountSize()-totalAllocAmt)/ladderConfig.getOriginalAccountSize()));
	}
	
	/**
	 * 
	 * @param minYr
	 * @param maxYr
	 * @return
	 */
	private int getNumBondsPerBucket(int minYr, int maxYr){
		//if maturity range is > than MIN_NUM_BONDS, then that is 1
		//else it is the first multiple of of maturity range > MIN_NUM_BONDS
		int matRange = maxYr - minYr + 1;
		if(matRange >= MIN_NUM_BONDS ){
			return 1;
		}
		else{
			return MIN_NUM_BONDS/matRange + 1;
		}
	}
	
	/**
	 * 
	 * @param ladderConfig
	 * @return
	 */
	private List<ConstraintEvaluator> createConstraintEvaluatorsList(LadderConfig ladderConfig){
		List<ConstraintEvaluator> consEvalList = new ArrayList<LadderAllocator.ConstraintEvaluator>();
		consEvalList.add(new HealthCareConstraintEvaluator(ladderConfig));
		consEvalList.add(new AOrBelowConstraintEvaluator(ladderConfig));
		consEvalList.add(new StateConstraintEvaluator(ladderConfig));
		consEvalList.add(new SectorConstraintEvaluator(ladderConfig));
		consEvalList.add(new StateSectorConstraintEvaluator(ladderConfig));
		return consEvalList;
	}
	
	/**
	 * 
	 * @param consEvalList
	 * @param sec
	 * @param parAmt
	 * @return
	 */
	private boolean evaluateConstraints(List<ConstraintEvaluator> consEvalList, Security sec, long parAmt){
		for(ConstraintEvaluator consEval: consEvalList){
			boolean constraintPassed = consEval.evaluate(sec, parAmt);
			if(!constraintPassed){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 
	 * @param ladBucketList
	 * @param matYrSecListMap
	 * @param ladderConfig
	 * @param consEvalList
	 */
	private void doRecursiveLadderAllocation(List<LadderBucket> ladBucketList, SortedMap<Integer, List<Security>> matYrSecListMap, LadderConfig ladderConfig, List<ConstraintEvaluator> consEvalList){
		int matYr = matYrSecListMap.firstKey();
		LadderBucket ladderBucket = new LadderBucket(matYr);
		for(ConstraintEvaluator consEval: consEvalList){
			ladderBucket.addObserver(consEval);
		}
		ladBucketList.add(ladderBucket);
		List<Security> secList = matYrSecListMap.get(matYr);
		long totDollarRoundedAmt = 0;
		int numBondsSelected = 0;
		for(Security sec: secList){
			//get the raw amt
			long rawParAmt = (long)(ladderConfig.getLatestAccountSize()*PAR_PRICE/ladderConfig.getLatestMaturityRange()/sec.price/ladderConfig.numBondsPerBucket);
			//get the rounded amount
			long roundedParAmt = Math.round((double)(rawParAmt/MIN_INCREMENT))*MIN_INCREMENT;
			if(evaluateConstraints(consEvalList, sec, roundedParAmt)){
				totDollarRoundedAmt += roundedParAmt*sec.price/PAR_PRICE;
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
			print("--- TotDollarBucketAmt: " + ALLOC_FORMAT.format(totDollarBucketAmt) + ". TotDollarRoundedAmt: " + ALLOC_FORMAT.format(totDollarRoundedAmt));
			for(SecurityParAmt secParAmt : secParAmtList){
				print(secParAmt);
			}
    	}
		if(totDollarRoundedAmt < totDollarBucketAmt){
			for(SecurityParAmt secParAmt : secParAmtList){
				double adjDollarAmt = MIN_INCREMENT*secParAmt.sec.price/PAR_PRICE;
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
				double adjDollarAmt = MIN_INCREMENT*secParAmt.sec.price/PAR_PRICE;
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
		matYrSecListMap.remove(matYr);
		if(matYrSecListMap.size() > 0){
			ladderConfig.addMatRangeAcctSize(ladderConfig.getLatestMaturityRange()-1, ladderConfig.getLatestAccountSize() - ladderBucket.getLadderDollarAmount());
			doRecursiveLadderAllocation(ladBucketList, matYrSecListMap, ladderConfig, consEvalList);
		}
	}
	
	/**
	 * 
	 * @param origSecList
	 * @param minYr
	 * @param maxYr
	 * @return
	 */
	private List<Security> getFilteredList(List<Security> origSecList, int minYr, int maxYr){
		List<Security> secList = new ArrayList<LadderAllocator.Security>();
    	for(Security sec: origSecList){
    		if(sec.matYr >= (CURRENT_YEAR + minYr) && sec.matYr <= (CURRENT_YEAR + maxYr)){
    			//for ranges that start with 1 like 1-5 etc, max price allowed is 105
    			if(minYr == 1){
    				if(sec.price <= MAX_PRICE_ONE){
    					secList.add(sec);
    				}
    			}
    			else{
    				if(sec.price <= MAX_PRICE_OTHER){
    					secList.add(sec);
    				}
    			}
    		}
    	}
		return secList;
	}
	
	/**
	 * 
	 */
	private void init(){
		try{
        	//for debug output
        	//buffWriter = new BufferedWriter (new FileWriter("L:\\Savinder\\Java\\LadderAllocationOutput.txt"));
			buffWriter = new BufferedWriter (new FileWriter("\\\\tfiappsq1-jitv\\DataShare\\MuniLadder\\LadderAllocationOutput.txt"));
        	//get the default date
        	defPurchaseDate = DATE_FORMAT.parse("01/01/2001");
        	
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
		}
		catch(IOException ioe){
            ioe.printStackTrace();
        } catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	/**
	 * 
	 */
	private void end(){
        try{
            buffWriter.close();
        }catch(IOException ioe1){
            //Leave It
        }
	}
	
	/**
	 * 
	 * @return
	 */
	private List<Security> loadSecurityData(){
		List<Security> secList = new ArrayList<LadderAllocator.Security>();
		Map<String, Security> cusipSecMap = new HashMap<String, LadderAllocator.Security>();
		BufferedReader buffReader = null;
		try{
        	buffReader = new BufferedReader (new FileReader("\\\\tfiappsq1-jitv\\DataShare\\MuniLadder\\Security_SMF_20180124.csv"));
            String line = buffReader.readLine();
    		int cusipCtr = -1;
    		int sectorCtr = -1;
    		int maturityDateCtr = -1;
    		int priceCtr = -1;
    		int spRatingCtr = -1;
    		int moodyRatingCtr = -1;
    		int fitchRatingCtr = -1;
            if(line != null){
            	if(debug){
            		print(line);
            	}
                String[] strArray = line.split(",");
                for(int i=0; i<strArray.length; i++){
                	if("CUSIP".equalsIgnoreCase(strArray[i].trim())){
                		cusipCtr = i;
                	}
                	else if("Market Price".equalsIgnoreCase(strArray[i].trim())){
                		priceCtr = i;
                	}
                	else if("Stated Maturity".equalsIgnoreCase(strArray[i].trim())){
                		maturityDateCtr = i;
                	}
                	else if("S&P Rating".equalsIgnoreCase(strArray[i].trim())){
                		spRatingCtr = i;
                	}
                	else if("Moody Rating".equalsIgnoreCase(strArray[i].trim())){
                		moodyRatingCtr = i;
                	}
                	else if("Fitch Rating".equalsIgnoreCase(strArray[i].trim())){
                		fitchRatingCtr = i;
                	}
                	else if("Holdings Sector".equalsIgnoreCase(strArray[i].trim())){
                		sectorCtr = i;
                	}
                }
            	if(cusipCtr == -1 || priceCtr == -1 || maturityDateCtr == -1 || spRatingCtr == -1 || moodyRatingCtr == -1 || fitchRatingCtr == -1 || sectorCtr == -1){
            		throw new RuntimeException("Invalid file format: " + line);
            	}
            }
            else{
            	throw new RuntimeException("Empty file!");
            }
            line = buffReader.readLine();
            String defRating = "NR";
            String defSector = "NO_SECTOR";
            while(line != null){
            	if(debug){
            		print(line);
            	}
                String[] strArray = line.split(",");
                Security sec = new Security();
                sec.cusip = strArray[cusipCtr].trim(); 
                sec.spRating = "".equals(strArray[spRatingCtr].trim())?defRating:strArray[spRatingCtr].trim();
                sec.moodyRating = "".equals(strArray[moodyRatingCtr].trim())?defRating:strArray[moodyRatingCtr].trim();
                sec.fitchRating = "".equals(strArray[fitchRatingCtr].trim())?defRating:strArray[fitchRatingCtr].trim();
                sec.secRating = Rating.getMedianRating(sec.spRating, sec.moodyRating, sec.fitchRating, true, true, true).getQIndex() < 6? SecRating.ABOVE_A: SecRating.A_OR_BELOW;
                if(debug){
                	print("Median Rating: " + Rating.getMedianRating(sec.spRating, sec.moodyRating, sec.fitchRating, true, true, true));
                }
                sec.sector = "".equals(strArray[sectorCtr].trim())?defSector:strArray[sectorCtr].trim();
                if(!strArray[priceCtr].trim().isEmpty() && !strArray[maturityDateCtr].trim().isEmpty()){
	                sec.price = Double.valueOf(strArray[priceCtr].trim());
	                if(sec.price >= MIN_PRICE){
		                try {
							sec.maturityDate = DATE_FORMAT.parse(strArray[maturityDateCtr].trim());
			                secList.add(sec);
			                cusipSecMap.put(sec.cusip, sec);
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
                }
                line = buffReader.readLine();
            }
        }
		catch(IOException ioe){
            ioe.printStackTrace();
        }
        finally{
	        try{
	            buffReader.close();
	        }catch(IOException ioe1){
	            //Leave It
	        }
		}
		try{
        	buffReader = new BufferedReader (new FileReader("\\\\tfiappsq1-jitv\\DataShare\\MuniLadder\\SMASecurity_20180124.csv"));
        	DateFormat df = new SimpleDateFormat("dd-MMM-yy");
            String line = buffReader.readLine();
    		int cusipCtr = -1;
    		int stateCtr = -1;
    		int lastPurchaseDateCtr = -1;
            if(line != null){
            	if(debug){
            		print(line);
            	}
                String[] strArray = line.split(",");
                for(int i=0; i<strArray.length; i++){
                	if("CUSIP".equalsIgnoreCase(strArray[i].trim())){
                		cusipCtr = i;
                	}
                	else if("State".equalsIgnoreCase(strArray[i].trim())){
                		stateCtr = i;
                	}
                	else if("LastTradeDate".equalsIgnoreCase(strArray[i].trim())){
                		lastPurchaseDateCtr = i;
                	}
                }
            	if(cusipCtr == -1 || stateCtr == -1 || lastPurchaseDateCtr == -1){
            		throw new RuntimeException("Invalid file format: " + line);
            	}
            }
            else{
            	throw new RuntimeException("Empty file!");
            }
            line = buffReader.readLine();
            while(line != null){
            	if(debug){
            		print(line);
            	}
                String[] strArray = line.split(",");
                Security sec = cusipSecMap.get(strArray[cusipCtr].trim());
                if(sec != null){
                    if(!strArray[lastPurchaseDateCtr].trim().isEmpty() && !strArray[stateCtr].trim().isEmpty()){
                    	sec.state = strArray[stateCtr].trim();
    	                try {
    						sec.lastPurchaseDate = df.parse(strArray[lastPurchaseDateCtr].trim());
    					} catch (ParseException e) {
    						// TODO Auto-generated catch block
    						e.printStackTrace();
    					}
                    }
                }
                line = buffReader.readLine();
            }
            //put defaults for missing
            String defState = "ZZ";
            df = new SimpleDateFormat("yy");
            for(Security sec: secList){
            	if(sec.lastPurchaseDate == null){
                	sec.state = defState;
                	sec.lastPurchaseDate = defPurchaseDate;            
            	}
           		sec.matYr = Integer.valueOf(df.format(sec.maturityDate));
            }
        }
		catch(IOException ioe){
            ioe.printStackTrace();
        } 
        finally{
	        try{
	            buffReader.close();
	        }catch(IOException ioe1){
	            //Leave It
	        }
		}
		return secList;
	}
	
	/**
	 * 
	 * @param obj
	 */
	private static void print(Object obj){
		if(obj != null){
			System.out.println(obj.toString());
			if(buffWriter != null){
				try{
				buffWriter.write(obj.toString());
				buffWriter.write(NEW_LINE);
				}catch (IOException ioe){
					ioe.printStackTrace();
				}
			}
		}
	}

	/**
	 * 
	 * @author SDHALIWAL
	 *
	 */
	private class Security implements Comparable{
		private String cusip;
		private String sector;
		private Date maturityDate;
		private Date lastPurchaseDate;
		private double price;
		private String spRating;
		private String moodyRating;
		private String fitchRating;
		private String state;
		private int matYr ;
		private SecRating secRating;
		
		private SecPriority getSecPriority(){
			if(!defPurchaseDate.equals(lastPurchaseDate)){
				if(SecRating.ABOVE_A == secRating){
					if(SECTOR_HEALTHCARE.equals(sector)){
						if(STATE_NY.equals(state)){
							return SecPriority.ABOVE_A_HEALTHCARE_NY;
						}
						else if(STATE_CA.equals(state)){
							return SecPriority.ABOVE_A_HEALTHCARE_CA;
						}
						else{
							return SecPriority.ABOVE_A_HEALTHCARE_NON_NY_CA;
						}
					}
					else{
						if(STATE_NY.equals(state)){
							return SecPriority.ABOVE_A_NY;
						}
						else if(STATE_CA.equals(state)){
							return SecPriority.ABOVE_A_CA;
						}					
						else{
							return SecPriority.ABOVE_A_NON_NY_CA;
						}
					}
				}
				else{
					if(SECTOR_HEALTHCARE.equals(sector)){
						if(STATE_NY.equals(state)){
							return SecPriority.A_OR_BELOW_HEALTHCARE_NY;
						}
						else if(STATE_CA.equals(state)){
							return SecPriority.A_OR_BELOW_HEALTHCARE_CA;
						}
						else{
							return SecPriority.A_OR_BELOW_HEALTHCARE_NON_NY_CA;						
						}
					}
					else{
						if(STATE_NY.equals(state)){
							return SecPriority.A_OR_BELOW_NY;
						}
						else if(STATE_CA.equals(state)){
							return SecPriority.A_OR_BELOW_CA;
						}
						else{
							return SecPriority.A_OR_BELOW_NON_NY_CA;
						}
					}
				}
			}
			else{
				return SecPriority.NO_PRIORITY;
			}
		}

	    
		public String toString(){
			return "CUSIP: " + cusip + 
					". SecPriority: " + getSecPriority() +
					". Price: " + price + 
					". SecRating: " + secRating + 
					". Sector: " + sector + 
					". Maturity Date: " + DATE_FORMAT.format(maturityDate) + 
					". SP Rating: " + spRating + 
					". Moody Rating: " + moodyRating + 
					". Fitch Rating: " + fitchRating + 
					". State: " + state +
					". LastPurchaseDate: " + DATE_FORMAT.format(lastPurchaseDate) +
					". MatYr: " + matYr;
		}

		public boolean equals(Object obj){
			if(obj instanceof Security){
				return ((Security)obj).cusip.equals(this.cusip);
			}
			else{
				return false;
			}
		}
		
		public int hashCode(){
			return cusip.hashCode();
		}
		
		/**
		 * Better rankings compare as greater.
		 * @param o Rating object
		 * @return -1, 0, 1 if less than, equal to or greater
		 */
		public int compareTo(Object o) {
			int secSortOrder = secPriorityList.indexOf(getSecPriority());
			int sortOrder = secPriorityList.indexOf(((Security)o).getSecPriority());
			if(secSortOrder > sortOrder){
				return 1;
			}
			else if(secSortOrder < sortOrder){
				return -1;
			}
			else{
				return ((Security)o).lastPurchaseDate.compareTo(lastPurchaseDate);
			}
		} 
	}

	/**
	 * 
	 * @author SDHALIWAL
	 *
	 */
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
			return sec.price*roundedParAmt/PAR_PRICE;
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

	/**
	 * 
	 * @author SDHALIWAL
	 *
	 */
	private class StateSectorConstraintEvaluator extends ConstraintEvaluator{
		SortedMap<String, Double> stateSectorPctMap = new TreeMap<String, Double>();
		
		private StateSectorConstraintEvaluator (LadderConfig ladderConfig){
			super(ladderConfig);
		}
		
		private String getStateSectorKey(Security sec){
			return sec.state + "::" + sec.sector;
		}
		
		protected boolean evaluate (Security sec, long parAmt){
			double sectorPct = stateSectorPctMap.get(getStateSectorKey(sec)) == null ? 0: stateSectorPctMap.get(getStateSectorKey(sec));
			return sectorPct + getSecurityPct(sec, parAmt) < MAX_SECTOR_STATE_PCT;
		}

		public void update(Observable o, Object arg) {
			SecurityParAmt secParAmt = (SecurityParAmt)arg;
			double sectorPct = stateSectorPctMap.get(getStateSectorKey(secParAmt.sec)) == null ? 0: stateSectorPctMap.get(getStateSectorKey(secParAmt.sec));
			stateSectorPctMap.put(getStateSectorKey(secParAmt.sec), sectorPct + getUpdatedSecurityPct(secParAmt));
			super.update(o, arg);
		}

		public String toString(){
			StringBuffer sb = new StringBuffer("-----  Total State Sector %: -----------");
			for(Map.Entry<String, Double> entry: stateSectorPctMap.entrySet()){
				sb.append(NEW_LINE).append(entry.getKey()).append(" : ").append(PCT_FORMAT.format(entry.getValue()/100));
			}
			return sb.toString();
		}
	}

	/**
	 * 
	 * @author SDHALIWAL
	 *
	 */
	private class SectorConstraintEvaluator extends ConstraintEvaluator{
		SortedMap<String, Double> sectorPctMap = new TreeMap<String, Double>();
		
		private SectorConstraintEvaluator (LadderConfig ladderConfig){
			super(ladderConfig);
		}
		
		protected boolean evaluate (Security sec, long parAmt){
			double sectorPct = sectorPctMap.get(sec.sector) == null ? 0: sectorPctMap.get(sec.sector);
			return sectorPct + getSecurityPct(sec, parAmt) < MAX_SECTOR_PCT;
		}

		public void update(Observable o, Object arg) {
			SecurityParAmt secParAmt = (SecurityParAmt)arg;
			double sectorPct = sectorPctMap.get(secParAmt.sec.sector) == null ? 0: sectorPctMap.get(secParAmt.sec.sector);
			sectorPctMap.put(secParAmt.sec.sector, sectorPct + getUpdatedSecurityPct(secParAmt));
			super.update(o, arg);
		}

		public String toString(){
			StringBuffer sb = new StringBuffer("-----  Total Sector %: -----------");
			for(Map.Entry<String, Double> entry: sectorPctMap.entrySet()){
				sb.append(NEW_LINE).append(entry.getKey()).append(" : ").append(PCT_FORMAT.format(entry.getValue()/100));
			}
			return sb.toString();
		}
	}

	/**
	 * 
	 * @author SDHALIWAL
	 *
	 */
	private class StateConstraintEvaluator extends ConstraintEvaluator{
		SortedMap<String, Double> statePctMap = new TreeMap<String, Double>();
		
		private StateConstraintEvaluator (LadderConfig ladderConfig){
			super(ladderConfig);
		}
		
		protected boolean evaluate (Security sec, long parAmt){
			double statePct = statePctMap.get(sec.state) == null ? 0: statePctMap.get(sec.state);
			return statePct + getSecurityPct(sec, parAmt) < MAX_STATE_PCT;
		}

		public void update(Observable o, Object arg) {
			SecurityParAmt secParAmt = (SecurityParAmt)arg;
			double statePct = statePctMap.get(secParAmt.sec.state) == null ? 0: statePctMap.get(secParAmt.sec.state);
			statePctMap.put(secParAmt.sec.state, statePct + getUpdatedSecurityPct(secParAmt));
			super.update(o, arg);
		}

		public String toString(){
			StringBuffer sb = new StringBuffer("-----  Total State %: -----------");
			for(Map.Entry<String, Double> entry: statePctMap.entrySet()){
				sb.append(NEW_LINE).append(entry.getKey()).append(" : ").append(PCT_FORMAT.format(entry.getValue()/100));
			}
			return sb.toString();
		}
	}
	
	/**
	 * 
	 * @author SDHALIWAL
	 *
	 */
	private class AOrBelowConstraintEvaluator extends ConstraintEvaluator{
		double aOrBelowPct = 0;
		
		private AOrBelowConstraintEvaluator (LadderConfig ladderConfig){
			super(ladderConfig);
		}
		
		protected boolean evaluate (Security sec, long parAmt){
			if(sec.secRating == SecRating.A_OR_BELOW){
				return aOrBelowPct + getSecurityPct(sec, parAmt) < MAX_A_OR_BELOW_PCT;
			}
			else{
				return true;
			}
			
		}

		public void update(Observable o, Object arg) {
			SecurityParAmt secParAmt = (SecurityParAmt)arg;
			if(secParAmt.sec.secRating == SecRating.A_OR_BELOW){
				aOrBelowPct += getUpdatedSecurityPct(secParAmt);
			}
			super.update(o, arg);
		}

		public String toString(){
			return "Total A or Below %:" + PCT_FORMAT.format(aOrBelowPct/100);
		}
	}
	
	/**
	 * 
	 * @author SDHALIWAL
	 *
	 */
	private class HealthCareConstraintEvaluator extends ConstraintEvaluator{
		double healthCarePct = 0;
		
		private HealthCareConstraintEvaluator (LadderConfig ladderConfig){
			super(ladderConfig);
		}
		
		protected boolean evaluate (Security sec, long parAmt){
			if(SECTOR_HEALTHCARE.equals(sec.sector)){
				return healthCarePct + getSecurityPct(sec, parAmt) < MAX_HEALTHCARE_PCT;
			}
			else{
				return true;
			}			
		}

		public void update(Observable o, Object arg) {
			SecurityParAmt secParAmt = (SecurityParAmt)arg;
			if(SECTOR_HEALTHCARE.equals(secParAmt.sec.sector)){
				healthCarePct += getUpdatedSecurityPct(secParAmt);
			}
			super.update(o, arg);
		}
		
		public String toString(){
			return "Total Healthcare %:" + PCT_FORMAT.format(healthCarePct/100);
		}
	}
	
	private abstract class ConstraintEvaluator implements Observer{
		private Map<Security, SecurityParAmt> secParAmtMap = new HashMap<Security, LadderAllocator.SecurityParAmt>();
		private LadderConfig ladderConfig = null;
				
		private ConstraintEvaluator (LadderConfig ladderConfig){
			this.ladderConfig = ladderConfig;
		}
		
		protected double getSecurityPct(Security sec, long parAmt){
			return 100*parAmt*sec.price/PAR_PRICE/ladderConfig.getOriginalAccountSize();
		}
		
		protected double getUpdatedSecurityPct(SecurityParAmt secParAmt){
			double updatedSecPct = 0;
			//in case security already exists then delete the old pct
			SecurityParAmt oldSecParAmt = secParAmtMap.get(secParAmt.sec);
			if(oldSecParAmt != null){
				updatedSecPct -= oldSecParAmt.getDollarAmount()/ladderConfig.getOriginalAccountSize();
			}
			return 100*(updatedSecPct + secParAmt.getDollarAmount()/ladderConfig.getOriginalAccountSize());
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
	
	/**
	 * 
	 * @author SDHALIWAL
	 *
	 */
	private class LadderBucket extends Observable{
		private int matYr;
		private Map<Security, SecurityParAmt> secParAmtMap = new HashMap<Security, LadderAllocator.SecurityParAmt>();
		
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
				dollarAmt += entry.getValue().sec.price*entry.getValue().roundedParAmt/PAR_PRICE;
			}
			return dollarAmt;
		}
		
		private double getAveragePrice(){
			double avgPrice = 0;
			for(Map.Entry<Security, SecurityParAmt> entry: secParAmtMap.entrySet()){
				avgPrice += entry.getValue().sec.price;
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
	
	/**
	 * 
	 * @author SDHALIWAL
	 *
	 */
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

	/**
	 * 
	 * @author SDHALIWAL
	 *
	 */
	private enum SecRating
	{
	    ABOVE_A, 
	    A_OR_BELOW;
	}
	
	/**
	 * 
	 * @author SDHALIWAL
	 *
	 */
	private enum SecPriority
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
	
	/**
	 * 
	 * @author SDHALIWAL
	 *
	 */
	private class MatYrComparator implements Comparator<Integer>
	{
	    // Used for sorting in ascending order of
	    // roll number
	    public int compare(Integer a, Integer b)
	    {
	        return b-a;
	    }
	}
	
	/**
	 * main
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("############### In Ladder Allocation ###################");
		print("############### In Ladder Allocation ###################");
		long startTime = System.currentTimeMillis();
    	debug = false;
    	LadderAllocator alloc = new LadderAllocator();
    	alloc.init();
    	alloc.allocate(1, 5, 1000000);
		long endTime = System.currentTimeMillis();
		print("############### Out Ladder Allocation ###################  Total time (s): " + (endTime-startTime)/1000);
		alloc.end();
		System.out.println(("############### Out Ladder Allocation ###################  Total time (s): " + (endTime-startTime)/1000));
	}	
}


