package com.muniladder.controller;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

@Component
public class BucketsCreation {
    private MuniRepository repository;

    private Long min;
    private Long max;
    private ArrayList<Long> buckets = new ArrayList<Long>();

    public BucketsCreation(MuniRepository repository){
        this.repository = repository;
    }

    public Collection<Muni> buckets(HashMap<String,String> queryMap){
        max = Long.parseLong(queryMap.get("max"));
        min = Long.parseLong(queryMap.get("min"));
        for(long i = min; i < max; i++){
            buckets.add(i);
        }

        for(Long l:buckets){
            System.out.println(Long.toString(l));
        }

        return this.repository.findAll().stream()
        .filter(this::filterMunis)
        .collect(Collectors.toList());
    }

    private boolean filterMunis(Muni muni){
        long ytm = Long.parseLong(muni.getYtm());
        float price = Float.parseFloat(muni.getPrice());

        if(buckets.get(0).equals(1)){
            return ytm <= max && ytm >= min && price >= 100 && price <= 105;
        }
        return ytm <= max && ytm >= min;
    }
}
