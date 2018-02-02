package com.muniladder.controller;


import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

public interface MuniRepository{
    Collection<Muni> findAll();
}
