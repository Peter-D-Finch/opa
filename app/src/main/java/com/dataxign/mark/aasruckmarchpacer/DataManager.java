/**
 * This class manages data from the lab experiment. Aquires data every 15 seconds, smooths the data every minute and
 * runs the PSI and core temperature estimation models every minute. The class also holds the previous state of the model
 *
 * Edited 6/1/2022 by Peter Finch
 */

package com.dataxign.mark.aasruckmarchpacer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;

public class DataManager {

	// These are the codes used by getCurrent fn representing fields
	public final static int RAWHR=1;
	public final static int RAWTC=2;
	public final static int RAWSPD=3;
	public final static int HR=4;
	public final static int TC=5;
	public final static int SPEED=6;
	public final static int PSI=7;
	public final static int E_TC=8;
	public final static int E_PSI=9;
	public final static int DIST=10;
	public final static int GUID=11;
	
	// These fields hold the timeseries data
	private ArrayList<Double> rawHR;
	private ArrayList<Double> rawTC;
	private ArrayList<Double> rawSpeed;
	private ArrayList<Double> smoothedHR;
	private ArrayList<Double> smoothedTC;
	private ArrayList<Double> smoothedSpeed;
	private ArrayList<Double> estTC;
	private ArrayList<Double> obsPSI;
	private ArrayList<Double> estPSI;
	private ArrayList<Double> distance;
	private ArrayList<Double> guidance;

	// Used for storing the previous values of core temp and velocity
	public double lastGoodHR=0;
	public double lastGoodTC=0;
	public double lastGoodRPM=0;

	// These fields store information about the session
	private double sessionStartDistance=0;
	private double sessionTime=0;
	private int startIndex=0;
	public double distanceCompleted=0;

	// Used for conversion
	public final static float NUMBER_OF_FEET_IN_MILE=5280;

	// Objects that the DataManager uses
	public Chamber chamber;
	public Policy policy; // Policy to provide guidance
	private KalmanState ks=new KalmanState();
	
	private File fileName;
	private final String fileNameBase = "PacingStudyData";

	public String ssid="None"; // Service Set Identifier
	public int chamberID=0;
	public int session=0;
	public int sessionMin=0;
	
	public int rpe=-999;
	public int thermal=-999;
	public int feeling=-999;
	
	public int timeIdx=0;
	public int psiIdx=0;
	public int distIdx=0;
	
	public double unadjustedGuidance=0;
	
	private boolean fileOutputGood=true;
	
	private Context appCntx;

	/**
	 * Constructor method.
	 * @param TCstart Initial core temp
	 * @param Vstart Initial velocity
	 * @param r App resources
	 * @param fp Filepath for data to be written to
	 * @param cx App context
	 */
	public DataManager(double TCstart, double Vstart, Resources r, File fp, Context cx){
		appCntx=cx;
		chamber=new Chamber();
		rawHR=new ArrayList<Double>();
		rawTC=new ArrayList<Double>();
		rawSpeed=new ArrayList<Double>();
		
		smoothedHR=new ArrayList<Double>();
		smoothedTC=new ArrayList<Double>();
		smoothedSpeed=new ArrayList<Double>();
		
		estTC=new ArrayList<Double>();
		obsPSI=new ArrayList<Double>();
		estPSI=new ArrayList<Double>();
		distance=new ArrayList<Double>();
		guidance=new ArrayList<Double>();
		
		// Load the policy
		policy=new Policy(r);

		// Create the file to record the data in
		Date dt = new Date();
		Timestamp ts = new Timestamp(dt.getTime());
		String fileDate = "" + ts;
		fileName = new File(fp, fileNameBase + fileDate + ".csv");
		createFile();
	}

	/**
	 * Initialize a file to write the data to
	 */
	public void createFile(){
		try {
			OutputStream os = new FileOutputStream(fileName, true);

			String outs = "Using Policy Function: "+policy.policyFileName+"\n";
			outs=outs+"time,ssid,chamber,session,runmin,rawHR,rawTC,rawSpd,minHR,minTC,minSpd,obsPSI, estTC, estPSI,distance,guidance,rpe,thermal,feeling,actualDist,timeIdx,distIdx,psiIdx,unadjustedGuidance\n";

			byte[] out = outs.getBytes();
			os.write(out);
			os.close();
			Log.i("DataManager","Writing File: " + fileName.toString());

		}
		catch (IOException e) {
			fileOutputGood=false;
			Log.e("DataManager","Error opening file" + fileName.toString() + "  " + e);
            Toast.makeText(appCntx, "NO DATA LOGGING", Toast.LENGTH_SHORT).show();

		}

	}

	/**
	 * Post an update to the data file
	 */
	public void writeToFile() {
		String outs = "";
		try {
			OutputStream os = new FileOutputStream(fileName, true);

			// Write real data here if fail OK just ignore
			Date dt = new Date();
			Timestamp ts = new Timestamp(dt.getTime());

			outs = "" + ts + "," + ssid + ","+chamberID+","+session+","+sessionMin+",";
			
			for(int i=1;i<=11;i++){
				if(i==this.E_TC || i==this.E_PSI) outs=outs+getCurrent(i) + ",";
				else outs=outs+String.format("%2.2f", getCurrent(i)) + ",";
			}
			outs=outs+rpe+","+thermal+","+feeling+","+policy.tIdx+","+ this.distanceCompleted+","+policy.dIdx+","+policy.pIdx+","+policy.unGuide+"\n";

			byte[] out = outs.getBytes();
			os.write(out);
			os.close();
			fileOutputGood=true;
		}
		catch (IOException e) {
			fileOutputGood=false;
			Log.e("DataManager","BAD DATA WRITE: " + outs + "  " + e);
            Toast.makeText(appCntx, "!ALERT!     NO DATA LOGGING     !ALERT!", Toast.LENGTH_SHORT).show();
			
		}
	}

	/**
	 * Writes a marker into the data file
	 * @param marker A custom message that will be written in the file
	 */
	public void writeMarkerToFile(String marker) {
		String outs = "";
		try {
			OutputStream os = new FileOutputStream(fileName, true);

			// Write real data here if fail OK just ignore
			Date dt = new Date();
			Timestamp ts = new Timestamp(dt.getTime());

			outs = marker+"\n";

			byte[] out = outs.getBytes();
			os.write(out);
			os.close();
			fileOutputGood=true;
		}
		catch (IOException e) {
			fileOutputGood=false;
			Log.e("DataManager","BAD DATA WRITE: " + outs + "  " + e);
            Toast.makeText(appCntx, "!ALERT!     NO DATA LOGGING     !ALERT!", Toast.LENGTH_SHORT).show();
			
		}
	}

	/**
	 * Adds a heart rate value to time series ArrayList. Does minor filtering
	 * @param hr The heart rate value to be added
	 */
	public void addRawHR(double hr){
		//filter by limits
		double MIN=30;
		double MAX=220;
		if(hr>MAX || hr<MIN)return;
		lastGoodHR=hr;
		rawHR.add(Double.valueOf(hr));
	}

	/**
	 * Adds a core temp value to the time series ArrayList. Does minor filtering
	 * @param tc
	 */
	public void addRawTC(double tc){
		//filter by limits
		double MIN=20.0;
		double MAX=42.5;
		if(tc>MAX || tc<MIN)return;
		lastGoodTC=tc;
		rawTC.add(Double.valueOf(tc));
	}

	/**
	 * Adds a speed value to the time series ArrayList. Does minor filtering
	 * @param rp
	 */
	public void addRawRPM(double rp){
		//filter by limits
		double MIN=0;
		double MAX=110;
		if(rp>MAX || rp<MIN)return;
		lastGoodRPM=rp;
		float mph=chamber.computeSpeedMPH((float)rp);
		
		rawSpeed.add(Double.valueOf(mph));
	}

	/**
	 * Computes a smoothed value using the median of 1 minute of time series data
	 */
	public void computeMinuteValues(){
		smoothedHR.add(Double.valueOf(computeMedian(rawHR)));
		rawHR.clear();
		smoothedTC.add(Double.valueOf(computeMedian(rawTC)));
		rawTC.clear();
		smoothedSpeed.add(Double.valueOf(computeMedian(rawSpeed)));
		rawSpeed.clear();

		obsPSI.add(computePSI(getCurrent(smoothedTC),getCurrent(smoothedHR)));
		estTC.add(computeEstimatedTC(getCurrent(smoothedHR)));
		estPSI.add(computePSI(getCurrent(estTC),getCurrent(smoothedHR)));
		
		//don't forget the speed computations etc.
		//Compute the minute distance
		//distance.add(getCurrent(smoothedSpeed)/60);
	}

	/**
	 * Computes the distance traveled from the start.
	 * @param epochTimeMillis Milliseconds since distance was last computed
	 */
	public void computeDistance(long epochTimeMillis){
		// Compute how far has been traveled so far
		double time_travelled = ((double)epochTimeMillis/(double)(1000*60*60));
		double dist = getCurrent(smoothedSpeed) * time_travelled;

		// Add that distance to the time series field
		distance.add(dist);

		// Add the distance computed to the total distance travelled
		distanceCompleted = distanceCompleted + dist;
		Log.w("DataManager","Completed Distance: " + distanceCompleted);
	}

	/**
	 * Initializes the data manager to be run.
	 */
	public void startSession(){
		distanceCompleted = 0;
		startIndex = smoothedHR.size();
		computeGuidance(0);

		//set Tcore at start of run
		double currentTc=getCurrent(smoothedTC);

		//If an appropriate value is not possible for TCore then will use the default of 37.1
		if(currentTc>=35.5 && currentTc<38.5){
			ks.currentTC=currentTc;
		}

		// Empty out the system time series
		rawHR.clear();
		rawTC.clear();
		rawSpeed.clear();
	}

	/**
	 * Computes guidance with policy, adds it to guidance time series, and returns the guidance.
	 * This function draws upon the currently stored distance data to compute.
	 * @param time The current time in the run
	 * @return A double representing the guidance move speed
	 */
	public double computeGuidance(long time){
		//Compute guidance at two minutes
		double pol=policy.getPolicy(time, distanceCompleted, getCurrent(E_PSI));
		
		//adjust for chamber calibration
		pol=chamber.treadmillSpeedSetting((float)pol);
		guidance.add(pol);
		//Log.w("DataManager","Policy:"+pol+ " Time="+time+" Dist="+getCurrent(DIST)+" PSI="+getCurrent(E_PSI));

		return pol;
	}

	/**
	 * Computes guidance with policy, adds it to guidance time series, and returns the guidance.
	 * This function takes a distance as an argument
	 * @param time The current time in the run
	 * @param dist The current distance in the run
	 * @return A double representing the guidance move speed
	 */
	public double computeGuidance(long time, double dist){
		//Compute guidance at two minutes
		double pol=policy.getPolicy(time, dist, getCurrent(E_PSI));
		pol=chamber.treadmillSpeedSetting((float)pol);
		guidance.add(pol);
		//Log.w("DataManager","Policy:"+pol+ " Time="+time+" Dist="+dist+" PSI="+getCurrent(E_PSI));

		return pol;
	}

	/**
	 * Adds a 0 to the guidance list
	 */
	public void setZeroGuidance(){
		guidance.add(0.0);
	}

	/**
	 * This function returns the last item in a ArrayList object
	 * @param al The ArrayList<Double> object
	 * @return The last item in the list
	 */
	private double getCurrent(ArrayList<Double> al){
		// Checking for error
		if(al.size()==0) { return -10; }

		// Retrieves the last item in the arrayList
		return al.get(al.size()-1).doubleValue();
	}

	/**
	 * Gives access to the class fields. On error returns -10
	 * @param type Integer representing which field you want to access
	 * @return The current value of the field
	 */
	public double getCurrent(int type){
		switch(type){
		case(RAWHR):
			return getCurrent(rawHR);
		case(RAWTC):
			return getCurrent(rawTC);
		case(RAWSPD):
			return getCurrent(rawSpeed);
		case(TC):
			return getCurrent(smoothedTC);
		case(HR):
			return getCurrent(smoothedHR);
		case(PSI):
			return getCurrent(obsPSI);
		case(E_TC):
			return getCurrent(estTC);
		case(E_PSI):
			return getCurrent(estPSI);
		case(SPEED):
			return getCurrent(smoothedSpeed);
		case(DIST):
			return getCurrent(distance);
		case(GUID):
			return getCurrent(guidance);
		default:
			return -10;
		}
	}

	/**
	 * Function computes PSI Moran 1998 assuming HR0 and TC0 as documented in Moran
	 * @param HR Input HR value
	 * @param TC Input TC value
	 * @return PSI from 0 to 10 roughly
	 */
	private double computePSI(double TC, double HR){
		if(TC==-10 || HR==-10)return -10;
		return USARIEM.calcPSI(TC, 37.1, HR, 71);
	}

	/**
	 * Computes the median of a given ArrayList. Returns -10 on error
	 * @param al The ArrayList<Double> object
	 * @return The median of the list
	 */
	private double computeMedian(ArrayList<Double> al){
		// Checking for input error
		if(al.size() == 0) { return -10; }

		// Converting the ArrayList to normal Array of doubles
		double[] ad = new double[al.size()];
		for(int i=0;i<al.size();i++){ ad[i]=al.get(i).doubleValue(); }

		// Sorting the array
		Arrays.sort(ad);

		// If the size of the list is 1, then that's the median
		if(al.size()==1) { return ad[0]; }

		// In order to find the median, we must first check if the array has even or odd num elements
		if(al.size()%2==0) { // Even number of elements
			int p1=(al.size()/2)-1;
			int p2=p1+1;
			return (ad[p1]+ad[p2])/2;
		}
		else { // Odd number of elements
			int p1 = (int) Math.ceil(al.size() / 2);
			return ad[p1];
		}
	}

	/**
	* Can be used to compute the core temp from HR
	* @param HR Input HR value
	* @return The core temp
    */
	private double computeEstimatedTC(double HR){
		if(HR==-10)return-10;
		ks=USARIEM.estimateTcore(HR, ks);
		Log.i("DataManager","Estimated TC = "+ks.currentTC+" | Estimated V = "+ks.currentV);
		//lastTC=ks.currentTC;
		//lastV=ks.currentV;
		return ks.currentTC;
	}

	/**
	 * Returns the state of the Kalman model as a string.
	 * @return
	 */
	public String getKalmanState(){
		return(""+ks.whosModel+": sigma="+ks.sigma+", gamma="+ks.gamma+"\nb_0="+ks.b_0+", b_1="+ks.b_1+", b_2="+ks.b_2);
	}

	/**
	 * Sets the state of the Kalman model.
	 * @param who The string that holds the state of the model
	 * @return The new Kalman state
	 */
	public String setKalmanState(String who){
		ks.setModel(who);
		return getKalmanState();
	}
}
