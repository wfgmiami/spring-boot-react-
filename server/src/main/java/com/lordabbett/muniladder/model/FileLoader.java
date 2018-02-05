package com.lordabbett.muniladder.model;

;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


@Component
public class FileLoader {

    private static final String SMF_FILE_PATH = "./Security_SMF_20180124.csv";
    private static final String SMA_FILE_PATH = "./SMASecurity_20180124.csv";
    private static List<Security> secList  = new ArrayList <Security> ();
    Map<String, Security> cusipSecMap = new HashMap<String, Security>();
    private static DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");
    private static DateFormat df = new SimpleDateFormat("yyyy");
    private static Date defaultLastTraded = null;
    private Date dt = null;
    private Date currentDate = new Date();
    private String defaultState = "ZZ";

    public void loadFile() throws IOException {

        Scanner scan = new Scanner(new File(SMF_FILE_PATH));
        int cusipPos = -1;
        int pricePos = -1;
        int couponPos = -1;
        int maturityDatePos = -1;
        int sectorPos = -1;
        int ratingPos = -1;
        int effDurPos = -1;
        int modDurPos = -1;
        int yieldToWorstPos = -1;
        boolean firstRowRead = false;

        while (scan.hasNextLine()) {

            String defaultRating = "NR";
            String defaultSector = "NO_SECTOR";
            String line = scan.nextLine();
            String[] lineArray = line.split(",");

            if(!firstRowRead) {
                for (int i = 0; i < lineArray.length; i++) {
                    if ("CUSIP".equalsIgnoreCase(lineArray[i].trim())) {
                        cusipPos = i;
                    } else if ("Market Price".equalsIgnoreCase(lineArray[i].trim())) {
                        pricePos = i;
                    } else if ("Coupon".equalsIgnoreCase(lineArray[i].trim())) {
                        couponPos = i;
                    } else if ("Stated Maturity".equalsIgnoreCase(lineArray[i].trim())) {
                        maturityDatePos = i;
                    } else if ("Holdings Sector".equalsIgnoreCase(lineArray[i].trim())) {
                        sectorPos = i;
                    } else if ("Opinion Internal Rating".equalsIgnoreCase(lineArray[i].trim())) {
                        ratingPos = i;
                    } else if ("Effective (OA) Dur".equalsIgnoreCase(lineArray[i].trim())) {
                        effDurPos = i;
                    } else if ("Modified Duration".equalsIgnoreCase(lineArray[i].trim())) {
                        modDurPos = i;
                    } else if ("Yield to Worst".equalsIgnoreCase(lineArray[i].trim())) {
                        yieldToWorstPos = i;
                    }
                }
                firstRowRead = true;
                if (cusipPos == -1 || pricePos == -1 || couponPos == -1 || maturityDatePos == -1 || sectorPos == -1 ||
                        ratingPos == -1 || effDurPos == -1 || modDurPos == -1 || yieldToWorstPos == -1) {
                    throw new RuntimeException("Invalid file format " + line);
                }

            }else {
                if(!lineArray[pricePos].trim().isEmpty() && !lineArray[maturityDatePos].isEmpty() &&
                !lineArray[effDurPos].isEmpty() && !lineArray[modDurPos].isEmpty() && !lineArray[yieldToWorstPos].isEmpty()){
                    Security sec = new Security();
                    sec.setCusip(lineArray[cusipPos].trim());
                    sec.setPrice(Double.valueOf(lineArray[pricePos].trim()));
                    sec.setCoupon(Double.valueOf(lineArray[couponPos].trim()));
                    sec.setSector(lineArray[sectorPos].trim());
                    sec.setRating(lineArray[ratingPos].trim());
                    SecRating belowOrAboveA = Rating.valueOf(sec.getRating()).getQIndex() < 6 ? SecRating.ABOVE_A : SecRating.A_OR_BELOW;
                    sec.setTwoGroupsRating(belowOrAboveA);
                    sec.setEffDur(Double.valueOf(lineArray[effDurPos].trim()));
                    sec.setModDur(Double.valueOf(lineArray[modDurPos].trim()));
                    sec.setYieldToWorst(Double.valueOf(lineArray[yieldToWorstPos].trim()));

                    try{
                        dt = DATE_FORMAT.parse(lineArray[maturityDatePos].trim());
                        int yrToMat = Integer.valueOf(df.format(dt)) - Integer.valueOf(df.format(currentDate));
                        sec.setYearsToMaturity(yrToMat);
                        sec.setMaturityDate(DATE_FORMAT.format(dt));
                        secList.add(sec);
                        cusipSecMap.put(sec.getCusip(), sec);
                    }catch(ParseException e){
                        e.printStackTrace();
                    }
                }
            }
        }
        scan.close();

        scan = new Scanner(new File(SMA_FILE_PATH));
        cusipPos = -1;
        int statePos = -1;
        int lastTradedPos = -1;
        firstRowRead = false;
        df = new SimpleDateFormat("dd-MMM-yy");

        try{
            defaultLastTraded = DATE_FORMAT.parse("01/01/2001");
        }catch(ParseException e){
            e.printStackTrace();
        }

        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            String [] lineArray = line.split(",");

            if(!firstRowRead){
                for(int i = 0; i < lineArray.length; i++){
                    if("CUSIP".equalsIgnoreCase(lineArray[i])){
                        cusipPos = i;
                    }
                    if("State".equalsIgnoreCase(lineArray[i])){
                        statePos = i;
                    }
                    if("LastTradeDate".equalsIgnoreCase(lineArray[i])){
                        lastTradedPos = i;
                    }
                }
                if(cusipPos == -1 || statePos == -1 || lastTradedPos == -1){
                    throw new RuntimeException("Invalid file format: " + line);
                }
                firstRowRead = true;
            }else{
                Security sec = cusipSecMap.get(lineArray[cusipPos].trim());
                if(sec != null){
                    if(!lineArray[cusipPos].trim().isEmpty() && !lineArray[statePos].trim().isEmpty()){
                        sec.setState(lineArray[statePos]);
                        try{
                            dt = df.parse(lineArray[lastTradedPos].trim());
                            if (dt == null) {
                                sec.setLastTraded(DATE_FORMAT.format(defaultLastTraded));
                            } else {
                                sec.setLastTraded(DATE_FORMAT.format(dt));
                            }
                        }catch(ParseException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        scan.close();

        for(Security sec: secList){
            if(sec.getLastTraded() == null){
                sec.setState(defaultState);
                sec.setLastTraded(DATE_FORMAT.format(defaultLastTraded));
            }
        }
    }

    public List<Security> getSecList(){
        return this.secList;
    }

    public enum SecRating {
        ABOVE_A,
        A_OR_BELOW
    }

    public static Date getDefaultDate(){
        return defaultLastTraded;
    }
    public static DateFormat getDateFormat(){
        return DATE_FORMAT;
    }
}
