package com.dataxign.mark.aasruckmarchpacer.geo;

/**
 * Created by Mark on 3/20/2016.
 * A segment is defined by two points
 */
public class Segment {
    public LocationPoint start;
    public LocationPoint end;
    public double distance=-1.0;
    private double distanceMiles=-1.0;
    public double heading=-1;

    public Segment(LocationPoint s, LocationPoint e){
        start=s.copyOf();
        end=e.copyOf();
        computeDistance();
        computeHeading();
    }

    private void computeHeading(){
        double x=end.easting-start.easting;
        double y=end.northing-start.northing;
        double angle=Math.atan2(x,y);
        if(angle<0)angle=angle*-2.0;
        double angleDeg=angle*(360/(Math.PI*2));
        heading=angleDeg;
    }

    private void computeDistance(){
        distance=Math.sqrt(Math.pow((start.easting - end.easting), 2) + Math.pow((start.northing - end.northing), 2));
        distanceMiles=convertM2Miles(distance);
    }

    public static double convertM2Miles(double distM){
        //International definition of a mile in km = 1.609344
        double distInMiles=(distM/1000.0)/1.609344;
        return distInMiles;
    }

    @Override
    public String toString(){
        return "Route: From: "+start.lat+", "+start.lon+" to: "+end.lat+", "+end.lon+" | Total distance "+distance+ "m ("+distanceMiles+" miles)";
    }
}
