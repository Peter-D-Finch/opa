package com.dataxign.mark.aasruckmarchpacer;

/**
 * This class hold chamber specific calibration values to be used throughout the course of the experiment
 * @author Mark
 *
 */
public class Chamber {

	public final static float NUMBER_OF_FEET_IN_MILE=5280;

	public int chamber=0; // Zero indicates no chamber selected. The experiment cannot run unless the correct chamber is selected
	public double[] beltLength={9.087,9.0988,9.0876,9.0873}; //in feet
	//public double[] desSpeedCoef={1,0.871,0.8628,0.9488}; //Old settings
	//recal cahmber02 01292015
	//public double[] desSpeedCoef={1,0.9086,0.8931,0.9408}; //New settings 01/29/2015
	
	//public double[] desSpeedInter={0,0.3323,0.6608,0.324};
	public double[] desSpeedCoef={1,0.8876,0.9098,0.9263}; //New settings 02/23/2015
	public double[] desSpeedInter={0,0.4512,0.5896,0.5417};
	
	public Chamber(){
		
	}
	
	public void setChamber(int c){
		if(c<1 || c>3) chamber=0;
		else chamber=c;
	}
	public double treadmillSpeedSetting(float desiredSpeed){
		if (desiredSpeed<0.2)return 0;
		double speedSetting=desSpeedCoef[chamber]*desiredSpeed+desSpeedInter[chamber]; //Placeholder
		return speedSetting;
	}
	
	public float computeSpeedMPH(float speedRPM){
        float mph=((speedRPM*(float)beltLength[chamber])*60)/NUMBER_OF_FEET_IN_MILE;
        return mph;
	}

}
