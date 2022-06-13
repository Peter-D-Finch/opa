package com.dataxign.mark.aasruckmarchpacer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import android.content.res.Resources;
import android.util.Log;

public class Policy {
	
	private static final int NUMBER_OF_ACTIONS=36;
	private static final int NUMBER_OF_PSIS=39;
	private static final int NUMBER_OF_DISTANCES=900;
	private static final int NUMBER_OF_TIME_PERIODS=30;
	//public static final String POLICY_FILE_NAME="policy_lessnoise_7_5_oldclothing.csv";
	public String policyFileName="None";
	private final double DISTANCE_UNIT=0.2*(2.0/60.0);
	
	public int tIdx=0;
	public int dIdx=0;
	public int pIdx=0;
	public double unGuide=0;
	
	public boolean gotPolicy=false;
	
	int[][][] policy; //[Time period][PSI][Dist]
	
	public Policy(Resources rs){
		//Read in file
		policy=new int[NUMBER_OF_TIME_PERIODS][NUMBER_OF_PSIS][NUMBER_OF_DISTANCES];
		readPolicyFile(rs);
	}
	
	private void readPolicyFile(Resources rs){
		
		long startTime=System.currentTimeMillis();
		Log.w("Policy", "Reading Policy... (Can take a while)");

		//BufferedReader in = new BufferedReader(new InputStreamReader(rs.openRawResource(R.raw.policy_7_5_actual_relax1029)));
		//policyFileName="policy_7_5_actual_relax1029.csv";
		BufferedReader in = new BufferedReader(new InputStreamReader(rs.openRawResource(R.raw.policy_7_5_actual_relaxsmooth0120)));
		policyFileName="policy_7_5_actual_relaxsmooth0120.csv";
		try{
			for(int tp=0;tp<NUMBER_OF_TIME_PERIODS;tp++){
				for(int npsi=0;npsi<NUMBER_OF_PSIS;npsi++){
					String distances=in.readLine();
					StringTokenizer st=new StringTokenizer(distances,",");
					for (int dist=0;dist<NUMBER_OF_DISTANCES;dist++){
						policy[tp][npsi][dist]=Integer.parseInt(st.nextToken());
					}
				}
			}
		}
		catch(IOException e){
            Log.e("Policy", "Uh Oh Can't read policy file. This is BAD! "+e.getMessage());
            
		}
		
		long tookTime=System.currentTimeMillis()-startTime;
		Log.w("Policy", "Policy Read Done! "+tookTime + "ms");
		gotPolicy=true;

	}
	/**
	 * 
	 * @param time, Given in milliseconds since the start of the course
	 * @param distance, given in miles
	 * @param psi, given in PSI units
	 * @return returns optimal movement speed
	 */
	public double getPolicy(long time, double distance, double psi){
		int timeIndex=timeIndex(time);
		if(timeIndex>NUMBER_OF_TIME_PERIODS-1)return 0;
		int distIndex=distIndex(distance);
		if(distIndex>NUMBER_OF_DISTANCES-1)return 0;
		int psiIndex=psiIndex(psi);
		
		int speed=policy[timeIndex][psiIndex][distIndex];
		unGuide=speed;
		
		
		return speedValue(speed);
	}

	/**
	 * Converts from real time to time index
	 * There are 30 epochs
	 * Epochs are 2 minutes each
	 * Index 0 = Time 0 to time <2
	 * Index 30 = Time 58
	 * @param time in milliseconds since the start of the course
	 * @return timeIndex (0-30)
	 */
	private int timeIndex(long time){
		
		int timeIndex=(int) Math.round((double)time/(1000*120)); //1000 for ms and 120 for two minutes
		//Possibly limit timeIndex to 30 but maybe this can be given 
		//if(timeIndex>=NUMBER_OF_TIME_PERIODS)timeIndex=29;
		tIdx=timeIndex;
		Log.e("Policy","Time Index="+timeIndex);
		return timeIndex;
	}
	
	/**
	 * PSI is from 0.5 to 10.0 in 0.25 increments
	 * Index 0 is 0.5 psi
	 * Index 38 is 10 psi
	 * @param psi actual PSI
	 * @return psiIndex
	 */
	private int psiIndex(double psi){
		int psiIndex=(int)Math.round(psi*(double)4)-2;
		if(psiIndex<0)psiIndex=0;
		if(psiIndex>38)psiIndex=38;
		pIdx=psiIndex;
		Log.e("Policy","PSI Index="+psiIndex);
		return psiIndex;
	}
	/**
	 * distanceUnit=0.2*(2/60);
	 * numDistanceUnits=900;
	 * @param distance in miles
	 * @return
	 */
	private int distIndex(double distance){
		
		int distanceIndex=(int)(distance/DISTANCE_UNIT);
		if(distanceIndex<0)distanceIndex=0;
		if(distanceIndex>899)distanceIndex=899;
		dIdx=distanceIndex;
		Log.e("Policy","Distance="+distance+ " Index="+distanceIndex);
		return distanceIndex;
	}
	
	/**
	 * Speed Index is from 1 to 36
	 * from 0 to 7 mph in .2 mph increments
	 * @param speedIndex Index returned from policy
	 * @return Actual speed in miles per hour that the policy recommends
	 */
	private double speedValue(int speedIndex){
		double speedValue=((double)(speedIndex-1)*.2);
		return speedValue;
	}

}
