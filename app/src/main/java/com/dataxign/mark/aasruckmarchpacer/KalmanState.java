package com.dataxign.mark.aasruckmarchpacer;

import java.util.HashMap;
import java.util.Map;

public class KalmanState {
	
	//Generic Model Parameters that can be set
	public double gamma=0.022;
	public double sigma=18.88;
	public double b_0=-7887.1; 
	public double b_1=384.4286; 
	public double b_2=-4.5714;
	
	//Generic Model Starting Points
	public double currentTC=37.1;
	public double currentV=0;
	
	public String whosModel="Generic";

	class ModelParams{
		double b0;
		double b1;
		double b2;
		double g;
		double s;
		ModelParams(double bb0, double bb1, double bb2, double gg, double ss){
			b0=bb0;
			b1=bb1;
			b2=bb2;
			g=gg;
			s=ss;
		}
	}
	private Map<String, ModelParams>map=new HashMap<String,ModelParams>();

	
	public KalmanState(){
		populateMap();
	}

	public KalmanState(double tc,double v, String who){
		currentTC=tc;
		currentV=v;
		populateMap();
		setModel(who);
	}
	
	private void populateMap(){
		//%gamma=0.022^2; Original b_0=-7887.1; b_1=384.4286; b_2=-4.5714; sigma=18.88^2; 
		map.put("GENERIC", new ModelParams(-7887.1, 384.4286,-4.5714, 0.022, 18.88));
		//4701= from 10/24 Run gamma=0.0163 b_0 = -8951.5865; b_1 = 435.1971; b_2 = -5.1744 %sigma=22;
		//map.put("4701", new ModelParams(-8951.5865, 435.1971,-5.1744,0.0163, 22));
		//4702 Not completed
		//4703 = from 12/17 b_0 = -8353.8846 b_1 = 406.7889, b_2 = -4.8366 gamma = 0.0350 sigma = 15.3773;
		//map.put("4703", new ModelParams(-8353.8846, 406.7889,-4.8366,0.0350, 15.3773));
		//4704 from 12/17 b_0 = -9025.1742 b_1 = 438.7481, b_2 = -5.2166,gamma = 0.0175 sigma = 27;
		//map.put("4704", new ModelParams(-9025.1742, 438.7481,-5.2166,0.0175, 27));
		
	}
	
	public void setModel(String who){
		
		ModelParams p=map.get(who);
		if(p!=null){
			b_0=p.b0;
			b_1=p.b1;
			b_2=p.b2;
			gamma=p.g;
			sigma=p.s;
		}
		
		whosModel=who;

	}
	
}
