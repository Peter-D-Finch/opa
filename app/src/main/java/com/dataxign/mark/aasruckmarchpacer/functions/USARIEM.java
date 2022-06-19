/*
 * A class of helper functions to compute physiological indices and estimates
 */

package com.dataxign.mark.aasruckmarchpacer.functions;

import com.dataxign.mark.aasruckmarchpacer.mdp.KalmanState;

/**
 *
 * @author Mark
 */
public class USARIEM {
     /**
 *  method pandolfMetCalc
 *  M = 1.5W + 2.0(W + L)(L/W)^2 + η(W + L)(1.5V^2 + 0.35VG)
 *  M = Metabolic Rate (watts)
 *  W = Nude body weight (kg)
 *  L = Load (clothing + equipment), (kg)
 *  η = terrain factor (unitless) (1.0 = treadmill)
 *  V = walking velocity (m/s)
 *  G = Grade (%) 0% = flat surface
 */
    public static double calcPandolfMet (double w, double l, double n, double v, double g){
        //Ignore Negative grades and make 0
        if(g<0)g=0;
        //Pandolf from Pandolf Givoni and Goldman, 1977, JAP 42(4)577-581
        //Predicting Energy Expenditure with Loads While Standing or Walking Very Slowly
        double M=(1.5*w+2.0*(w + l)*(l/w)*(l/w)+n*(w+l)*(1.5*v*v+0.35*v*g));

        //According to Givoni and Goldman if M>900 most likely running and a running correction
        //should be applied
        //Givoni, Goldman 1971, JAP Vol 30(3) pp 429-433
        //Predicting Metabolic Energy Cost
        if(M>900){
            M=(M+0.47*(900-M))*(1+(g/100));
        }

        return M;
    }
    /**
     *  Method Clalculates Physiological Strain Index based on Moran 1998
     *  PSI = 5(Tret - Tre0)/(39.5 - Tre0) + 5(HRt - HR0)/(180 -HR0)
     *  PSI = Physiological Strain Index (unitless)
     * @param tret = Rectal Temperature at time t (Celsius)
     * @param tre0 = Rectal Temperature at rest (Celsius)
     * @param hrt = Heart Rate at time t (beats per minute)
     * @param hr0  = Heart Rate at rest (beats per minute)
     * @return PSI
     */
    public static double calcPSI (double tret, double tre0, double hrt, double hr0){
        return (5*(tret-tre0)/(39.5-tre0)+5*(hrt-hr0)/(180-hr0));
    }
    
    /**
     * Adapted from: Buller MJ, Tharion WJ, Cheuvront SN, Montain SJ, Kenefick RW, Castellani J, 
     * Latzka WA, Roberts WS, Richter M, Jenkins OC, Hoyt RW. (2013) 
     * Estimation of human core temperature from sequential heart rate observations. 
     * Physiological Measurement 34 781�798. (Matlab Function Appendix)
     *
     * Inputs: Previous or intial core temperature, Previous or initial variance, Current heart rate.
     * Output: In object form Current curren core temperature, and current variance
     *
     * TCprev floating point - previous Tcore 
     * Vprev floating point - previous Variance
     * HRcurrent - floating point - current Heart Rate Value
     * 
     * Returns:
     * tc_current - Current TC updated by this function
     * v_current - Current Variance updated by this function
     * Returns an object.tc_current current core body temperature, object.v_current current variance
     *
     */
    public static KalmanState estimateTcore(double HRcurrent, KalmanState ks){
       	//Model Parameters
    	double a=1; 
    	
    	//double gamma=Math.pow(0.022,2);
    	//double b_0=-7887.1; 
    	//double b_1=384.4286; 
    	//double b_2=-4.5714; 
    	//double sigma=Math.pow(18.88,2); 
    	double gamma=Math.pow(ks.gamma,2);
    	double b_0=ks.b_0; 
    	double b_1=ks.b_1; 
    	double b_2=ks.b_2; 
    	double sigma=Math.pow(ks.sigma,2); 

    	//Initialize Kalman filter
    	double x=ks.currentTC; 
    	double v=ks.currentV;

    	//Time Update Phase
    	double x_pred=a*x; 												//Equation 3
    	double v_pred=(Math.pow(a,2))*v+gamma; 							//Equation 4
     
     	//Observation Update Phase
    	double z=HRcurrent; 
    	double c_vc=2*b_2*x_pred+b_1;									//Equation 5
    	double k=(v_pred*c_vc)/(Math.pow(c_vc,2)*v_pred+sigma); 	  	//Equation 6
    	x=x_pred+k*(z-(b_2*Math.pow(x_pred,2)+b_1*x_pred+b_0)); 		//Equation 7
    	v=(1-k*c_vc)*v_pred;					   						//Equation 8
    	
    	//Return values
    	ks.currentTC=x;
    	ks.currentV=v;
    	
    	return ks;
    }
    
}
