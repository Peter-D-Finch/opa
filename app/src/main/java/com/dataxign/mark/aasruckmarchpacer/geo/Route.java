package com.dataxign.mark.aasruckmarchpacer.geo;

import android.content.res.Resources;
import android.util.Log;

import com.dataxign.mark.aasruckmarchpacer.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Created by Mark on 3/20/2016.
 * A route is defined by a set of contiguous Segments
 * i.e. a segment end should lead to the same segment start
 * Edited on 6/4/2022 by Peter Finch
 */
public class Route {
    public ArrayList<Segment> route;
    public boolean routeDefined=false;
    public String title="UNDEFINED";

    // These private fields keep track of the min and max northing and easting
    private double max_northing=0;
    private double min_northing=90000000;
    private double max_easting=0;
    private double min_easting=90000000;

    public double width=0;
    public double height;
    public double top=0;
    public double left=0;
    public int currentSegment=-1;
    public double distanceCurrentSegment=0;

    /**
     * First constructor that takes no arguments
     */
    public Route(){
        route=new ArrayList<Segment>(2);
    }

    /**
     * Second constructor that takes two arguments
     * @param rs App resources
     * @param whichRoute an integer representing which route is to be loaded
     */
    public Route(Resources rs, int whichRoute){
        route=new ArrayList<Segment>(2);
        generateRouteFromFile(rs,whichRoute);
    }

    /**
     * Loads a route from a file.
     * @param rs App resources
     * @param whichRoute an integer representing which route is to be loaded
     */
    public void generateRouteFromFile(Resources rs, int whichRoute){
        //BufferedReader in = new BufferedReader(new InputStreamReader(rs.openRawResource(R.raw.route_ft_campbell_grades)));
        //BufferedReader in = new BufferedReader(new InputStreamReader(rs.openRawResource(R.raw.route_douglas)));
        BufferedReader in = new BufferedReader(new InputStreamReader(rs.openRawResource(R.raw.route_natick)));
        Log.d("Route", "Reading route file...");

        // Reading a route file
        try{
            //First Row Title
            String row=in.readLine();
            StringTokenizer st=new StringTokenizer(row,",");
            title=st.nextToken();

            //Second Row Header
            row=in.readLine();

            //Remaining rows are LocationPoints in sequence
            boolean endReached=false;
            boolean firstPoint=true;
            LocationPoint lastPoint=null;
            while(!endReached){
                // Read in a row
                row = in.readLine();

                // Check the row isn't empty
                if(row == null) { break; }

                // Parse the row using "," as delimiter
                st=new StringTokenizer(row,",");
                int pointID=Integer.parseInt(st.nextToken());
                double lat=Double.parseDouble(st.nextToken());
                double lon=Double.parseDouble(st.nextToken());
                String desc=st.nextToken();

                // Initialize new LocationPoint with data that was just parsed
                LocationPoint currentPoint=new LocationPoint(lat,lon);
                currentPoint.description=desc;

                // Checking if any of the values read are min / max values
                if(max_easting<currentPoint.easting)max_easting=currentPoint.easting;
                if(min_easting>currentPoint.easting)min_easting=currentPoint.easting;
                if(max_northing<currentPoint.northing)max_northing=currentPoint.northing;
                if(min_northing>currentPoint.northing)min_northing=currentPoint.northing;

                //A point has been successfully read. If it is the first point then
                //store as last location and move on
                if(firstPoint){
                    firstPoint=false;
                    lastPoint=currentPoint.copyOf();
                }
                else{
                    //generate a new route segment
                    Segment seg=new Segment(lastPoint,currentPoint);
                    addSegment(seg);
                    lastPoint=currentPoint.copyOf();
                }
            }
            computeExtent();
            routeDefined=true;
        }
        // If we couldn't read the route file
        catch(IOException e){
            Log.e("Route", "Uh Oh Can't read route file. This is BAD! " + e.getMessage());
        }

    }

    public LocationPoint snapToLine(LocationPoint p){
        LocationPoint sn=new LocationPoint();
        ArrayList<Integer> possSegments = new ArrayList<Integer>(2);
        ArrayList<Double> perpDist=new ArrayList<Double>(2);
        int smallestStart=9999999;
        int smSidx=0;
        int smallestEnd=9999999;
        int smEidx=0;
        int smallestPerpendicular=9999999;
        int smP=0;

        for(int i=0;i<route.size();i++){
            Segment s=route.get(i);
            double distance= findPerpendicularDistanceToSegment(p, s);
            double distFromStart=findDistanceFromStartOfLineSegment(p,s);
            double distFromEnd=findDistanceFromEndOfLineSegment(p,s);
            Log.v("Route","Segment: "+i+" Distance: Perpen.= "+ distance +" From Start = "+distFromStart +" From End = "+distFromEnd);

            if(Math.abs(s.distance-(distFromStart+distFromEnd))<0.1){
                possSegments.add(new Integer(i));
                perpDist.add(new Double(distance));
            }

        }
        int ans=-1;
        distanceCurrentSegment=-1;
        double lowestDist=9999999;
        for(int idx=0;idx<possSegments.size();idx++){
            if(perpDist.get(idx).doubleValue()<lowestDist){
                ans=possSegments.get(idx).intValue();
                lowestDist=perpDist.get(idx).doubleValue();
            }
        }

        if(ans>-1){
            computeSnapLocation(p, route.get(ans), sn);
        }
        sn.currentSegment=ans;
        currentSegment=sn.currentSegment;
        return sn;
    }

    private LocationPoint computeSnapLocation(LocationPoint p, Segment s,LocationPoint snap){
        LocationPoint line_vector=new LocationPoint();
        line_vector.easting=s.end.easting-s.start.easting;
        line_vector.northing=s.end.northing-s.start.northing;

        LocationPoint point_vector=new LocationPoint();
        point_vector.easting=p.easting-s.start.easting;
        point_vector.northing=p.northing-s.start.northing;

        LocationPoint dist_vector=new LocationPoint();

        double dotLandP= line_vector.easting*point_vector.easting+line_vector.northing*point_vector.northing;
        double normLsq=Math.pow(line_vector.easting,2)+Math.pow(line_vector.northing,2);
        dist_vector.easting=line_vector.easting*(dotLandP/normLsq);
        dist_vector.northing=line_vector.northing*(dotLandP/normLsq);

        distanceCurrentSegment=Math.sqrt(Math.pow(dist_vector.easting,2)+Math.pow(dist_vector.northing,2));
        snap.distanceAlongSegment=distanceCurrentSegment;

        //absolute location
        snap.easting=dist_vector.easting+s.start.easting;
        snap.northing=dist_vector.northing+s.start.northing;


        return snap;
    }

    private double findDistanceFromEndOfLineSegment(LocationPoint p,Segment s){
        double d=findPerpendicularDistanceToSegment(p,s);
        double h=Math.sqrt(Math.pow((p.easting-s.end.easting),2)+Math.pow((p.northing-s.end.northing),2));
        double a=Math.sqrt(h*h-d*d);
        return a;
    }

    private double findDistanceFromStartOfLineSegment(LocationPoint p,Segment s){
        double d=findPerpendicularDistanceToSegment(p, s);
        double h=Math.sqrt(Math.pow((p.easting - s.start.easting), 2) + Math.pow((p.northing - s.start.northing), 2));
        double a=Math.sqrt(h*h-d*d);
        return a;
    }

    private double findPerpendicularDistanceToSegment(LocationPoint p, Segment s){
        double distance=Math.abs((s.end.easting-s.start.easting)*(s.start.northing-p.northing)-(s.start.easting-p.easting)*(s.end.northing-s.start.northing))/Math.sqrt(Math.pow((s.end.easting-s.start.easting),2)+Math.pow((s.end.northing-s.start.northing),2));
        return distance;
    }

    private void computeExtent(){
        left=min_easting-100;
        width=(max_easting+100)-left;
        top=max_northing+100;
        height=top-(min_northing-100);
    }

    public void addSegment(Segment s){
        route.add(s);
    }
}
