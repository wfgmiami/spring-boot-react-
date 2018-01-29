package com.example.demo;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Entity
public class Muni{

    @Id
    @GeneratedValue
    private Long id;
    private String cusip;
    private Long price;
    private Long coupon;
    private String maturity;
    private Long ytm;
    private String sector;
    private String rating;
    private String state;
    private String lastTraded;
    private Long ed;
    private Long md;
    private Long ytw;

    public Muni(){}

}
