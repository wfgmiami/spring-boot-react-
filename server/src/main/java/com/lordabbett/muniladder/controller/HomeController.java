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
    
    @GetMapping(value="/app2")
    @CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5000", "http://10.3.160.199:3000" })
    public Collection<Security> minuListApp2(){
        return bonds.stream()
                .collect(Collectors.toList());
    }
    
    @GetMapping(value="/buckets")
    @CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5000", "http://10.3.160.199:3000" })
      public ArrayList<Object> muniFilter(@RequestParam HashMap<String, String> queryMap ){
        Allocation alloc = new Allocation();
        ArrayList<Object> allocation = alloc.buckets(queryMap);
        return allocation;
    }
    
    @GetMapping(value="/app2/buckets")
    @CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5000", "http://10.3.160.199:3000" })

      public ArrayList<Object> muniFilterApp2(@RequestParam HashMap<String, String> queryMap ){
        Allocation alloc = new Allocation();
        ArrayList<Object> allocation = alloc.bucketsApp2(queryMap);
        return allocation;

    }


}



