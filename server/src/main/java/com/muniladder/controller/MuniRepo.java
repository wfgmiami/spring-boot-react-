package com.muniladder.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class MuniRepo implements MuniRepository {

    @Autowired
    private Collection<Muni> munis;

    @Override
    @Cacheable("munis")
    public Collection<Muni> findAll(){
        return munis;
    }

}
