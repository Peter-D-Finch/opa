package com.dataxign.mark.aasruckmarchpacer;

/**
 * Created by Mark on 3/18/2016.
 * Edited by Peter Finch on 6/6/2022
 */
public class LocationPoint {
    // Public objects for lat, long, easting, and northign
    public double lat,lon;
    public double easting=0,northing=0;
    double meridian;
    public String description="UNDEFINED";

    // Fields to store data regarding users movement
    public double speedms;
    public double speedmph;
    public double bearing;
    public int currentSegment=-1;
    public double distanceAlongSegment=-1;

    public LocationPoint(){
        lat=0.0;
        lon=0.0;
    }

    public LocationPoint(double llat,double llon){
        lat=llat;
        lon=llon;
        convertToUTM();
    }

    /**
     * This generates a deep copy of this object. REMEMBER to deep copy all members of this class
     * @return The copy of this object
     */
    public LocationPoint copyOf(){
        LocationPoint p=new LocationPoint();
        p.lat=this.lat;
        p.lon=this.lon;
        p.easting=this.easting;
        p.northing=this.northing;
        p.meridian=this.meridian;
        p.description=this.description;
        p.speedms=this.speedms;
        p.speedmph=this.speedmph;
        p.bearing=this.bearing;
        return p;
    }

    /**
     * Sets the speed
     * @param sp The speed to be set, given in m/s
     */
    public void setSpeed(double sp){
        speedms=sp;
        speedmph=ms2mph(speedms);
    }

    /**
     * Sets the location of the location point
     * @param llat Given latitude
     * @param llon Given longitude
     */
    public void setLocation(double llat, double llon){
        lat=llat;
        lon=llon;
        convertToUTM();
    }

    /**
     * Sets the UTM
     * @param e the easting to be set
     * @param n the northing to be set
     */
    public void setUTM(double e, double n){
        easting=e;
        northing=n;
    }

    /**
     * Sets the meridian
     * @param m The meridian to be set
     */
    public void setMeridian(double m){
        meridian=m;
    }

    /**
     * This function converts meters per second to miles per hour
     * @param sp Speed given in m/s
     * @return Computed speed given in mph
     */
    public static double ms2mph(double sp){
        return 2.23694*sp;
    }

    /**
     * Converts this object to UTM
     */
    public void convertToUTM(){
        DegreesToUTM.latLon2UTM(this);
    }

    /**
     * Converts the object to a readable String
     * @return The String representation on the object
     */
    public String toString(){
        return ""+description+" Lat: "+lat+", Lon: "+lon+" | Easting: "+easting+ "| Northing: "+northing+" | Meridian: "+meridian;
    }
}


