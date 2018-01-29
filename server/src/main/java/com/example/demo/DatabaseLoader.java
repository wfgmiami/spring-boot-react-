package com.example.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

@Component
public class DatabaseLoader implements CommandLineRunner{

    private MuniRepository repository;

    public DatabaseLoader(MuniRepository repository){
        this.repository = repository;
    }

    @Override
    public void run(String... strings) throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<List<Muni>> typeReference = new TypeReference<List<Muni>>() {};
        List<Muni> muni = objectMapper.readValue(new File("munis.json"), typeReference);
        this.repository.save(muni);
//        repository.findAll().forEach(System.out::println);
    }
}
