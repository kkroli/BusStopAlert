package com.kkroli.example.BusStopAlert;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * This Class retains information of a given route
 */
public class Entry {

    private List<String> transitLine = new ArrayList<>();
    private String duration;
    private String departureTime;
    private String arrivalTime;
    private List<Step> steps = new ArrayList<>();

    public void addToSteps(LatLng startPoint, LatLng endPoint, int mode, String dir, String dist, String dur, String stopLoc, String depart, String arrival){
        steps.add(new Step(startPoint, endPoint,mode, dir, dist, dur, stopLoc, depart, arrival));
    }

    public int stepsSize(){
        return steps.size();
    }

    public LatLng getStartPoint(int i){
        return steps.get(i).getStartLocation();
    }

    public LatLng getEndPoint(int i) { return steps.get(i).getEndLocation(); }

    public int getMode(int i){
        return steps.get(i).getMode();
    }

    public List<Step> getSteps(){
        return steps;
    }

    public void setDuration(String dur){ duration = dur; }

    public void setDepartureTime(String depart) { departureTime = depart; }

    public void setArrivalTime(String arrive) { arrivalTime = arrive; }

    public String getDuration() { return duration; }

    public String getTimeLength() { return departureTime + " - " + arrivalTime; }

    public void addTransitLine(String line) { transitLine.add(line); }

    public List<String> getTransitLine(){ return transitLine; }

    public String getStopLocation(int i) { return steps.get(i).getStopLocation(); }
}
