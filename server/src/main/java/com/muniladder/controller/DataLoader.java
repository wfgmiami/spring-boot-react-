package com.muniladder.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muniladder.controller.Muni;
import com.muniladder.controller.MuniRepository;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

//@Component
//public class DataLoader implements CommandLineRunner{
//
////    private MuniRepository repository;
////    public DataLoader(MuniRepository repository){
////        this.repository = repository;
////    }
//
////    @Override
////    public void run(String... strings) throws Exception{
////        ObjectMapper objectMapper = new ObjectMapper();
////        TypeReference<List<Muni>> typeReference = new TypeReference<List<Muni>>() {};
////        List<Muni> muni = objectMapper.readValue(new File("munis.json"), typeReference);
////        this.repository.save(muni);
//////        repository.findAll().forEach(System.out::println);
////    }
//
//}
