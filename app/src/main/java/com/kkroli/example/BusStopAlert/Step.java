package com.kkroli.example.BusStopAlert;

import com.google.android.gms.maps.model.LatLng;

/**
 *  This Class retains the information for every step during the route that was retrieved from the directions API.
 *  */
public class Step {

    private LatLng startPoint;
    private LatLng endPoint;
    private int mode;
    private String direction;
    private String distance, duration;
    private String stopLocation;
    private String departureTime, arrivalTime;

    public Step(LatLng pStartPoint, LatLng pEndPoint, int pMode, String dir, String dist, String dur, String stopLoc, String depart, String arrival){
        startPoint = pStartPoint;
        endPoint = pEndPoint;
        mode = pMode;
        direction = dir;
        distance = dist;
        duration = dur;
        stopLocation = stopLoc;
        departureTime = depart;
        arrivalTime = arrival;
    }

    public LatLng getStartLocation(){
        return startPoint;
    }

    public LatLng getEndLocation() { return endPoint; }

    public int getMode(){
        return mode;
    } // the mode specifies if it is a walking step (0) or a transit step (2)

    public String getDirection() { return direction; }

    public String getDistance() { return distance; }

    public String getDuration() { return duration; }

    public String getStopLocation() { return stopLocation; }

    public void setStopLocation(String loc) { stopLocation = loc; }

    public String getDepartureTime() { return departureTime; }

    public String getArrivalTime() { return arrivalTime; }
}
