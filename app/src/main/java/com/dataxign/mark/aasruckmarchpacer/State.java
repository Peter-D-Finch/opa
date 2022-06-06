package com.dataxign.mark.aasruckmarchpacer;

/**
 * Created by Mark on 3/29/2016.
 * Holds the current state of the individual being monitored
 */
public class State {
    public int current_leg=-1;
    public double speedmph_inst=-1;
    public double speedmph_ave=-1;
    public double distance_overall=-1;
    public double distance_leg=-1;
    public boolean course_started=false;
    public long startTime=0;

}
