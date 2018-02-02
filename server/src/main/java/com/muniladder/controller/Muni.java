package com.muniladder.controller;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
public class Muni{

    private String cusip;
    private String price;
    private String coupon;
    private String maturity;
    private String ytm;
    private String sector;
    private String rating;
    private String state;
    private String lastTraded;
    private String ed;
    private String md;
    private String ytw;

//
//public Muni(String cusip, String price, String coupon, String maturity,String ytm,String sector,String rating,String state,
////    String lastTraded,String ed,String md,String ytw){
////        this.cusip = cusip;
////        this.price = price;
////        this.coupon = coupon;
////        this.maturity = maturity;
////        this.ytm = ytm;
////        this.sector = sector;
////        this.rating = rating;
////        this.state = state;
////        this.lastTraded = lastTraded;
////        this.ed = ed;
////        this.md = md;
////        this.ytw = ytw;
////    }

    public Muni(){}
    public String getState(){
        return state;
    }
    public String getCusip(){
        return cusip;
    }

    public String getYtm(){
        return ytm;
    }

    public String getPrice(){
        return price;
    }

}
