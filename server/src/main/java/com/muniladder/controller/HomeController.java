package com.muniladder.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class HomeController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private MuniRepository repository;

    public HomeController(MuniRepository repository){
        this.repository = repository;
    }

    @GetMapping(value="/")
    @CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5000", "http://10.3.160.199:3000" })
    public Collection<Muni> minuList(){
        log.info("debugging");
        return repository.findAll().stream()
                .collect(Collectors.toList());
    }

    @GetMapping(value="/buckets")
    @CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5000", "http://10.3.160.199:3000" })
    public Collection<Muni> muniFilter(@RequestParam HashMap<String, String> queryMap ){
        BucketsCreation bucketCreator = new BucketsCreation(this.repository);
        Collection<Muni> filteredMuni = bucketCreator.buckets(queryMap);
        return filteredMuni;

    }


}



