package com.lordabbett.muniladder.controller;


import com.lordabbett.muniladder.model.FileLoader;
import com.lordabbett.muniladder.model.Security;
import com.lordabbett.muniladder.service.Allocation;
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
    List<Security> bonds = new FileLoader().getSecList();

    public HomeController(){}

    @GetMapping(value="/")
    @CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5000", "http://10.3.160.199:3000" })
    public Collection<Security> minuList(){
        return bonds.stream()
                .collect(Collectors.toList());
    }

    @GetMapping(value="/buckets")
    @CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5000", "http://10.3.160.199:3000" })
    public Collection<Security> muniFilter(@RequestParam HashMap<String, String> queryMap ){
        Allocation alloc = new Allocation();
        Collection<Security> filteredBonds = alloc.buckets(queryMap);
        return filteredBonds;

    }


}



