package com.dataxign.mark.aasruckmarchpacer;

/**
 * Created by Mark on 4/18/2016.
 */
public class HelperStuff {

    public static String trimIt(double d, int dp){
        String num=Double.toString(d);
        int posdot=num.indexOf(".");
        if(num.length()>=posdot+dp+1) num=num.substring(0,posdot+dp+1);
        return num;
    }
}
