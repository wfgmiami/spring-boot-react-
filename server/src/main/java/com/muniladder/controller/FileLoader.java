package com.muniladder.controller;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


@Service
public class FileLoader {
    private static final String CSV_FILE_PATH = "./munis.csv";
    private List<Muni> munis;
    private MuniRepository repository;

    public FileLoader(MuniRepository repository){
        this.repository = repository;
    }

    @PostConstruct
    public void init(){
        try(
                Reader reader = Files.newBufferedReader(Paths.get(CSV_FILE_PATH));
        ){
            CsvToBean csvToBean = new CsvToBeanBuilder(reader)
                    .withType(Muni.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            munis = csvToBean.parse();
            for(Muni m:munis){
                System.out.println(m);
//                this.repository.save(m);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Muni> getMunis(){

        return this.munis;
    }

}
