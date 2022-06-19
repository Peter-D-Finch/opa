package com.dataxign.mark.aasruckmarchpacer.mdp;

import java.util.ArrayList;

/**
 * Created by Mark on 4/18/2016.
 * Edited by Peter Finch on 6/6/2022
 */
public class DataSmoother {

    private double speed_sum=0;
    private double speed_max=-1;
    private double speed_min=999999;
    private int speed_N=0;
    ArrayList<Double> speedList = new ArrayList<Double>(5);
    private int maxListSize=20;
    private double lastSpeedMov=0;
    private double lastSpeedAve=-1;
    private double heading_sum=0;
    private double heading_max=-1;
    private double heading_min=999999;
    private int heading_N=0;
    private double lastHeadingAve=-1;
    private boolean firstHeading=true;
    private boolean topLeft=false;

    private void resetSpeed(){
        speed_max=-1;
        speed_min=999999;
        speed_sum=0;
        speed_N=0;
    }
    private void resetHeading(){
        heading_max=-1;
        heading_min=999999;
        heading_sum=0;
        heading_N=0;
        firstHeading=true;
    }

    public void addSpeed(double speed){
        if(speed<0 || speed>15)return;

        if(speed>.1)speedList.add(new Double(speed));
        if(speedList.size()>=maxListSize)speedList.remove(0);
        lastSpeedMov=computeMovingAverageSpeed();


        speed_sum+=speed;
        speed_N++;
        if(speed>speed_max)speed_max=speed;
        if(speed<speed_min)speed_min=speed;
    }

    public double computeMovingAverageSpeed(){
        if(speedList.size()>0) {
            double sum=0;
            for (int i = 0; i < speedList.size(); i++) {
                sum+=speedList.get(i).doubleValue();
            }
            return sum/speedList.size();
        }
        else return 0;
    }

    public double getMovingAverageSpeed(){
        return lastSpeedMov;
    }

    public void addHeading(double heading){
        if(heading<0 || heading>360)return;
        //remember check if near border, convert low to high or high to low depending on first heading
        //i.e less than or greater than 180
        if(firstHeading){
            if(heading>270){
                topLeft=true;
            }
            else topLeft=false;
            firstHeading=false;
        }
        if(topLeft){
            if(heading<=90)heading=heading+360;
        }
        else{
            if(heading>270)heading=heading-360;
        }
        heading_sum+=heading;
        heading_N++;
        if(heading>heading_max)heading_max=heading;
        if(heading<heading_min)heading_min=heading;

    }

    public void smoothData(){
        smoothSpeed();
        smoothHeading();
    }

    public void smoothSpeed(){
        double sspeed=-1;
        if(speed_N>=3){
            //remove min and max
            speed_sum-=speed_max;
            speed_sum-=speed_min;
            speed_N-=2;
            sspeed=speed_sum/(double)speed_N;
        }
        lastSpeedAve=sspeed;
        resetSpeed();
    }

    public void smoothHeading(){
        lastHeadingAve=-1;
        double sheading=-1;
        if(heading_N>=3){
            //remove min and max
            heading_sum-=heading_max;
            heading_sum-=heading_min;
            heading_N-=2;
            sheading=heading_sum/(double)heading_N;

            if(sheading>360)sheading-=360;
            if(sheading<0)sheading=360+sheading;
        }
        lastHeadingAve=sheading;
        resetHeading();
    }
    public double getLastSpeed(){
        return lastSpeedAve;
    }
    public double getLastHeading(){
        return lastHeadingAve;
    }
}
