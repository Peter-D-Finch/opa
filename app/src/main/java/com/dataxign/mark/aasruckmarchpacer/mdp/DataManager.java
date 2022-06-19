/**
 * This class manages data from the lab experiment. Aquires data every 15 seconds, smooths the data every minute and
 * runs the PSI and core temperature estimation models every minute. The class also holds the previous state of the model
 *
 * Edited 6/1/2022 by Peter Finch
 */

package com.dataxign.mark.aasruckmarchpacer.mdp;
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

import com.dataxign.mark.aasruckmarchpacer.functions.USARIEM;

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

	public double lastGoodHR=0;
	public double lastGoodTC=0;
	public double lastGoodMPH=0;
	public double distanceCompleted=0;

	private long startTime;
	private long lastDistanceCompute;

	private Policy policy;
	private KalmanState ks=new KalmanState();

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

		policy=new Policy(r);
		appCntx=cx;
	}

	/**
	 * Updates the RAW values of the data manager
	 * @param HR The heart rate
	 * @param MPH The speed in mph
	 */
	public void update(double HR, double MPH) {
		double TC = computeEstimatedTC(HR);

		// Add the heart rate
		if (!(HR>220 || HR<40)) {
			lastGoodHR = HR;
			rawHR.add(Double.valueOf(HR));
		}

		// Add the core temp
		if (!(TC>20.0 || TC<42.5)) {
			lastGoodTC = TC;
			rawTC.add(Double.valueOf(TC));
		}

		// Add the speed in mph
		if (!(MPH>11 || MPH<0)) {
			lastGoodMPH = MPH;
			rawSpeed.add(MPH);
		}
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
	}

	/**
	 * Updates the distance traveled field
	 */
	public void computeDistance(){
		// Figure out how much time has passed since distance last computed
		long time = new Date().getTime();
		long epochTimeMillis = time - lastDistanceCompute;

		// Compute how far has been traveled so far
		double time_travelled = ((double)epochTimeMillis/(double)(1000*60*60));
		double dist = getCurrent(smoothedSpeed) * time_travelled;

		// Add that distance to the time series field
		distance.add(dist);

		// Add the distance computed to the total distance travelled
		distanceCompleted = distanceCompleted + dist;
		Log.w("DataManager","Completed Distance: " + distanceCompleted);
		lastDistanceCompute = time;
	}

	/**
	 * Computes guidance with policy, adds it to guidance time series, and returns the guidance.
	 * This function draws upon the currently stored distance data to compute.
	 * @return A double representing the guidance move speed
	 */
	public double computeGuidance(){
		// Compute how long it's been since the run started
		long time = new Date().getTime();
		time = time - startTime;

		//Compute guidance at two minutes
		double pol=policy.getPolicy(time, distanceCompleted, getCurrent(E_PSI));
		guidance.add(pol);

		return pol;
	}

	/**
	 * Initializes the data manager to be run.
	 */
	public void startSession(){
		distanceCompleted = 0;
		startTime = new Date().getTime();
		computeGuidance();

		//set Tcore at start of run
		double currentTc=getCurrent(smoothedTC);

		//If an appropriate value is not possible for TCore then will use the default of 37.1
		if(currentTc>=35.5 && currentTc<38.5){ ks.currentTC=currentTc; }

		// Empty out the system time series
		rawHR.clear();
		rawTC.clear();
		rawSpeed.clear();
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
}
