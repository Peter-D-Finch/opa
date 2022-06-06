package com.dataxign.mark.aasruckmarchpacer;

//auto imports

import android.location.Location;

import java.util.Calendar;


/**
 * Created by michael.codega on 12/28/2015.
 *
 * the indicator int is used to determine if the datapoint is meant to store accelerometry data
 * or if its meant to store gps data, since the two data types are measured in different intervals
 * but are desired to be stored in the same format (datapoint)
 */
public class DataPoints {

    public static int        INDICATOR_ACCEL    = 1;
    public static int        INDICATOR_GPS      = 2;
    public static int        INDICATOR_STEP     = 3;
    public static int        INDICATOR_GPSAVG   = 4;

    //accel
    private double      sum;
    //private int       xVal;
    //private int       yVal;
    //private int       zVal;

    //location
    private Location    loca;
    private Location    lastLoca;
    private double      distanceMove;
    private double      calcSpeed;
    private double      speed;
    private double      accuracy;

    private String      desc;

    //steps
    private double      stepCount;
    private double      stepTimeStamp;

    //universal
    private int         indicator;
    private String      time;
    private Calendar    calendar;

    public DataPoints(int indicator, float sum){
        //constructor for creating the datapoints from accelerometry data

        //indicator should be 1
        this.indicator      = indicator;
        //this sets everythign to NaN or Null
        init(indicator);
        //setting the only data value
        //the multiplication was there to preserve significant digits when casting to integer, but likely is no longer needed
        this.sum            = (double)(sum * 1000000);
    }

    public DataPoints(int indicator, double stepCount, long stepTimeStamp){
        //constructor for creatign datapoints from the step counter data

        //this should be 3
        this.indicator      = indicator;
        //sets everything to NaN or null
        init(indicator);

        //settting the only relevant variables
        this.stepCount      = stepCount;
        this.stepTimeStamp  = (double) stepTimeStamp;
        time                = Double.toString(stepTimeStamp);
    }

    public DataPoints(int indicator, Location loca, Location lastLoca){
        //constructor for creatying datapoints form the GPS data

        //this should be 2
        this.indicator      = indicator;
        //settting everything to NaN or null
        init(indicator);
        //setting the obnly relevant data
        this.lastLoca       = lastLoca;
        this.loca           = loca;
    }

    private void init(int indicator){
        //this nulls everything and sets the time stamp so each constructor has less repeat code
        calendar  =         Calendar.getInstance();

        sum             = Double.NaN;
        loca            = null;
        lastLoca        = null;
        distanceMove    = Double.NaN;
        calcSpeed       = Double.NaN;
        speed           = Double.NaN;
        accuracy        = Double.NaN;
        desc            = "";
        stepCount       = Double.NaN;
        stepTimeStamp   = Double.NaN;
        time            = "";

        //the step counter has its own time stamp
        if(indicator != 3){
            time = "" + System.currentTimeMillis();
        }
    }

    public Location getLocation(){
        return this.loca;
    }

    public Location getLastLocation(){
        return this.lastLoca;
    }

    private void catchLocation(){

        //this checks to see what values are available for the loation, because occasionally
        //null locations get through, which would cause null pointer problems
        //so all the location method calls are caught, seperately since each location object
        //can have various amounts of stored data

        //this is also what gets written when the accel or step data is present

        try{
            distanceMove = (double)lastLoca.distanceTo(loca);
        }
        catch(NullPointerException e){
            distanceMove = Double.NaN;
        }

        try{
            speed = loca.getSpeed();
        }
        catch(NullPointerException e){
            calcSpeed = Double.NaN;
        }

        try{
            calcSpeed = distanceMove / (loca.getTime() - lastLoca.getTime());

        }
        catch(NullPointerException e){
            speed = Double.NaN;
        }

        try{
            desc = loca.toString();
        }
        catch(NullPointerException e){

            //the location.toString method returns a [lat, long: other data] formatted string, so the coma throws
            //off the .csv file setup, so there has to be a coma in the control version so each data entry is aligned properly

            desc = "NO DESC ," + Double.NaN;
        }

        try{
            accuracy = loca.getAccuracy();
        }
        catch (NullPointerException e){
            accuracy = Double.NaN;
        }
    }

    public String toString(){
        //converts the datapoint into a string, designed to be individual lines in a .CSV file

        catchLocation();
        return "" + time + "," + indicator + "," + sum + "," + speed +  "," + distanceMove + "," + calcSpeed + "," + desc + "," + stepCount +  "," +stepTimeStamp + " \n";
    }

}
