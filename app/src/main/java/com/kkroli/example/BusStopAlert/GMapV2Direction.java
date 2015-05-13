package com.kkroli.example.BusStopAlert;

import java.io.IOException;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.google.android.gms.maps.model.LatLng;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

/*
 * This Class extracts information from an xml file using the directions api and stores them in a list of Entry.
 */

public class GMapV2Direction extends AsyncTask<String,Void, List<Entry>>{

    private static final String DEBUG_TAG = "HttpExample";
    private static final String ns = null;

    private List<Entry> possibleRoutes;

    public GMapV2Direction() {
        possibleRoutes = new ArrayList<>();
    }

    @Override
    protected List<Entry> doInBackground(String... urls) {
        // params comes from the execute() call: params[0] is the url.
        try {
           downloadUrl(urls[0]);
            return possibleRoutes;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<Entry> result) {
       // Do Nothing for now
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(DEBUG_TAG, "The response is: " + response);
            is = conn.getInputStream();

            try {
                readIt(is);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            return "Success";

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    // Reads an InputStream and converts it to a String.
    public void readIt(InputStream stream) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(stream, null);
            parser.nextTag();
            readFeed(parser);
        } finally {
            stream.close();
        }
    }

    private void readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, ns, "DirectionsResponse");
        while (parser.nextToken() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the leg tag as different routes always starts with the leg tag
            if (name.equals("leg")) {
               possibleRoutes.add(readEntry(parser));
            }
        }
    }

    /*
     * Goes through the routes information and extracts what is needed to then be displayed in the MapsActivity Class.
     */
    public Entry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {

        Entry route = new Entry();

        parser.require(XmlPullParser.START_TAG, ns, "leg");
        int walkingSteps = 0;

        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("leg")) { // While not at the end of the routes information
            //The walkingSteps integer is mainly for the walking step between transit steps.
            // Google directions API will have step tags in step tags where the travel mode involves walking.
            // Since the outer step tag already has all the information needed, we want to ignore the inner step tags
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("step"))
                walkingSteps++;
            if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals("step"))
                walkingSteps--;

            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            /*
             * We will first come across the travel mode tag which specifies if the step is a transit step or a walking step.
             * Depending which one, we will send it to the specific method that deals with the specified mode of travel.
             * Also, we want to store the information that involves the total duration of the trip, the time to depart at and the arrival time to our destination,
             * which are all located after the step tags.
             */
            if (name.equals("travel_mode")) {
                if (parser.next() == XmlPullParser.TEXT) {
                    if (parser.getText().equals("WALKING") && walkingSteps == 1)
                        readWalking(parser, route);
                    else if (parser.getText().equals("TRANSIT"))
                        readTransit(parser, route);
                }
            } else if (name.equals("duration") && walkingSteps == 0){
                while(parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("duration")){
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }

                    if (parser.getName().equals("text")){
                        if (parser.next() == XmlPullParser.TEXT){
                            route.setDuration(parser.getText());
                        }
                    }
                }
            } else if (name.equals("departure_time") && walkingSteps == 0){
                while(parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("departure_time")){
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }

                    if (parser.getName().equals("text")){
                        if (parser.next() == XmlPullParser.TEXT){
                            route.setDepartureTime(parser.getText());
                        }
                    }
                }
            } else if (name.equals("arrival_time") && walkingSteps == 0){
                while(parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("arrival_time")){
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }

                    if (parser.getName().equals("text")){
                        if (parser.next() == XmlPullParser.TEXT){
                            route.setArrivalTime(parser.getText());
                        }
                    }
                }
            }
        }

        return route;
    }

    // If mode of travel at the step is walking then we go through this method.
    public Entry readWalking(XmlPullParser parser, Entry current) throws IOException, XmlPullParserException {

        LatLng start = null;
        LatLng end = null;
        String duration = "";
        String distance = "";
        String StopAddress = "";

        /*
         * First we grab the start location and the end location of the current step in latitude and longitude
         * since these are the first tags we will come across in the xml file.
         */
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("end_location")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (name.equals("start_location")){
                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("lat"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in Lat tag of start_location.");
                String lat = parser.getText();

                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("lng"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in Lng tag of start_location.");
                String lng = parser.getText();

                start = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            }

            else if (name.equals("end_location")){
                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("lat"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in Lat tag of end_location.");
                String lat = parser.getText();

                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("lng"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in Lng tag of end_location.");
                String lng = parser.getText();

                end = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            }
        }

        /*
         * Now that we have the start location and end location of the step, we could proceed in
         * getting the total distance of the current step and its duration. Unfortunately, the XML
         * file does not give the estimated time of arrival of the walking trip and has no tag
         * that gives us the name of the location we should arrive at during this step. It does
         * mention it in the html_instructions tag, however they give more information in that tag
         * as well which I find sort of useless to have.
         */
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("distance")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (name.equals("duration")) {
                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("text"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in text tag of duration.");
                duration = parser.getText();
            } else if (name.equals("html_instructions")){
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in html_instructions tag.");
                StopAddress = "departure stop needed";
            } else if (name.equals("distance")){
                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("text"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in text tag of distance.");
                distance = parser.getText();
            }
        }

        // The step is added to the list of steps of the route
        if (start != null && end != null){
            current.addToSteps(start,end,0,"Walk",distance,duration,StopAddress, "", "");
        }

        // The entry as a String that that specifies the route. We add a "W" to specify to the user that it is a Walking step
        current.addTransitLine("W");
        return current;
    }

    // If mode of travel at the step is Transit then we go through this method.
    public Entry readTransit(XmlPullParser parser, Entry current) throws IOException, XmlPullParserException {

        LatLng start = null;
        LatLng end = null;
        String duration = "";
        String direction = "";
        String distance = "";
        String StopAddress = "";
        String depart = "";
        String arrival = "";

        /*
         * Same as a walking step, we first extract the start location and end location of the current
         * step which also is located in the first tags.
         */
        while (parser.nextToken() != XmlPullParser.END_TAG || !parser.getName().equals("end_location")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (name.equals("start_location")){
                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("lat"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in Lat tag of start_location.");
                String lat = parser.getText();

                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("lng"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in Lng tag of start_location.");
                String lng = parser.getText();

                start = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            }

            else if (name.equals("end_location")){
                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("lat"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in Lat tag of end_location.");
                String lat = parser.getText();

                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("lng"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in Lng tag of end_location.");
                String lng = parser.getText();

                end = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            }
        }

        /*
         * In this while loop, we extract the information involving the distance of the transit trip,
         * the duration of it and the name of the departure stop.
         */
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("departure_stop")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (name.equals("duration")){
                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("text"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in text tag of duration.");
                duration = parser.getText();
            } else if (name.equals("html_instructions")){
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in html_instructions tag.");
                direction = parser.getText();
            } else if (name.equals("distance")){
                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("text"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in text tag of distance.");
                distance = parser.getText();
            } else if (name.equals("departure_stop")){
                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("name"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in name tag of departure stop.");
                if (current.stepsSize() > 0 && current.getSteps().get(current.stepsSize()-1).getStopLocation().equals("departure stop needed")){
                    current.getSteps().get(current.stepsSize()-1).setStopLocation(parser.getText());
                }
            }
        }

        // Here we extract the name of the Transit stop that we are supposed to exit from.
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("arrival_stop")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (name.equals("name")) {
                if (parser.next() == XmlPullParser.TEXT) {
                   StopAddress = parser.getText();
                }
            }
        }

        // Here we grab the name of the transit line.
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("line")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (name.equals("short_name")) {
                if (parser.next() == XmlPullParser.TEXT) {
                    current.addTransitLine(parser.getText());
                }
            }
        }

        // Here we grab the departure and arrival time of the transit.
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("arrival_time")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (name.equals("departure_time")) {
                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("text"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in text tag of departure_time.");
                depart = parser.getText();
            } else if (name.equals("arrival_time")) {
                do {
                    parser.next();
                } while (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("text"));
                if (parser.next() != XmlPullParser.TEXT)
                    throw new XmlPullParserException("No text field in text tag of arrival_time.");
                arrival = parser.getText();
            }
        }

        //The step is then added to the route.
        if (start != null && end != null){
            current.addToSteps(start,end,2,direction,distance,duration,StopAddress,depart,arrival);
        }

        return current;
    }
}