package com.lordabbett.muniladder.service;

import com.lordabbett.muniladder.model.FileLoader;
import com.lordabbett.muniladder.model.Security;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
    private Long min;
    private Long max;

    private ArrayList<Long> buckets = new ArrayList<Long>();

    @PostConstruct
    public void init() throws IOException{
        FileLoader fileLoader = new FileLoader();
        fileLoader.loadFile();
    }

    public Collection<Security> buckets(HashMap<String,String> queryMap){

        max = Long.parseLong(queryMap.get("max"));
        min = Long.parseLong(queryMap.get("min"));
        for(long i = min; i <= max; i++){
            buckets.add(i);
        }

        return this.bonds.stream()
        .filter(this::filterBonds)
        .collect(Collectors.toList());
    }

    private boolean filterBonds(Security sec){
        int yearsToMat = sec.getYearsToMaturity();
        double price = sec.getPrice();

        if(buckets.get(0).equals(1L)){
            return yearsToMat<= max && yearsToMat >= min && price >= 100 && price <= 105;
        }
        return yearsToMat <= max && yearsToMat >= min;
    }
}
