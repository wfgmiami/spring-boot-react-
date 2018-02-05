package com.example.test;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Test {


    public static void main(String[] args){
//        Test t = new Test();
//        t.test();

        DateFormat df = new SimpleDateFormat("mm-dd-yy");
        try{
            Date dt = df.parse("01-05-2018");
            df = new SimpleDateFormat("yy");
            int i = Integer.valueOf(df.format(dt));
            System.out.println(Integer.toString(i));
        }catch(ParseException e){
            e.printStackTrace();
        }

    }

    public void test(){


        HashMap<String,String> m = new HashMap<String,String>();
        m.put("max","5");
        m.put("min", "1");
        Long max = Long.parseLong(m.get("max"));
        Long min = Long.parseLong(m.get("min"));
        ArrayList<String> alst = new ArrayList<String>();

        for(long i = min; i <= max; i++){
            alst.add(Long.toString(i));
        }

        for(String s:alst){
            System.out.print(s);
        }
    }


}

