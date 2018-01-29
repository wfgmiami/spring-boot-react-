package com.example.demo;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.stream.Collectors;

@RestController
public class HomeController {

    private MuniRepository repository;

    public HomeController(MuniRepository repository){
        this.repository = repository;
    }

    @GetMapping(value="/")
    @CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5000", "http://10.3.160.199:3000" })
    public Collection<Muni> minuList(){
        return repository.findAll().stream()
                .collect(Collectors.toList());
    }
}

