package com.example.test;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Test {


    public static void main(String[] args){
        System.out.println(SecPriority.A_OR_BELOW_CA);
        int t = SecPriority.valueOf("A_OR_BELOW_CA").ordinal();
        System.out.println(Integer.toString(t));
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


    private enum SecPriority     {
        ABOVE_A_HEALTHCARE_NY,
        ABOVE_A_HEALTHCARE_CA,
        ABOVE_A_HEALTHCARE_NON_NY_CA,
        ABOVE_A_NY,
        ABOVE_A_CA,
        ABOVE_A_NON_NY_CA,
        A_OR_BELOW_HEALTHCARE_NY,
        A_OR_BELOW_HEALTHCARE_CA,
        A_OR_BELOW_HEALTHCARE_NON_NY_CA,
        A_OR_BELOW_NY,
        A_OR_BELOW_CA,
        A_OR_BELOW_NON_NY_CA,
        NO_PRIORITY;
    }


}

