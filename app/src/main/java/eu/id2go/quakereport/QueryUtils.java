/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.id2go.quakereport;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Helper methods related to requesting and receiving earthquake data from USGS.
 */
public final class QueryUtils {

    private static final String LOG_TAG = QueryUtils.class.getName();


    /**
     * Create a private constructor because no one should ever create a {@link QueryUtils} object.
     * This class is only meant to hold static variables and methods, which can be accessed
     * directly from the class name QueryUtils (and an object instance of QueryUtils is not needed).
     */
    private QueryUtils() {
    }

    // Modify the extractEarthquakes() method that handles JSON parsing. I renamed it to
    // extractFeatureFromJson() and to take a JSON response String as input. Instead of hardcoding
    // the extractFeatureFromJson() method to only be able to parse the hardcoded SAMPLE_JSON_RESPONSE
    // String, this method becomes more reusable in different contexts if we accept a String input.
    /**
     * Return a list of {@link Earthquake} objects that has been built up from
     * parsing a JSON response.
     */
    private static ArrayList<Earthquake> extractFeatureFromJson(String earthquakeJSON) {
        // If the JSON string is empty or null, then return early.
        if (android.text.TextUtils.isEmpty(earthquakeJSON)) {
            return null;
        }
        // Create an empty ArrayList that we can start adding earthquakes to
        java.util.ArrayList<Earthquake> earthquakes = new ArrayList<>();

        // Try to parse the SAMPLE_JSON_RESPONSE. If there's a problem with the way the JSON
        // is formatted, a JSONException exception object will be thrown.
        // Catch the exception so the app doesn't crash, and print the error message to the logs.
        try {

            // Create a JSONObject from a JSON_RESPONSE string
            JSONObject baseJsonResponse = new JSONObject(earthquakeJSON);

            // Extract the JSONArray associated with the key called "features",
            // which represents a list of features (or earthquakes).
            JSONArray earthquakeArray = baseJsonResponse.getJSONArray("features");

            // For looping through each earthquake feature in the earthquakeArray, create an
            // {@link Earthquake} object
            for (int i = 0; i < earthquakeArray.length(); i++) {

                // Get a single earthquake at position i within the list of earthquakes
                JSONObject currentEarthquake = earthquakeArray.getJSONObject(i);

                // For a given earthquake, extract the JSONObject associated with the
                // key called "properties", which represents a list of all properties
                // for that earthquake.
                JSONObject properties = currentEarthquake.getJSONObject("properties");

                // Extract the value for the key called "mag"
                double magnitude = properties.getDouble("mag");

                // Extract the value for the key called "place"
                String location = properties.getString("place");

                // Extract the value for the key called "time"
                long time = properties.getLong("time");

                // Extract the value for the key called "url"
                String url = properties.getString("url");

                // Create a new {@link Earthquake} object with the magnitude, location, time,
                // and url from the JSON response.
                Earthquake earthquake = new Earthquake(magnitude, location, time, url);

                // Add the new {@link Earthquake} to the list of earthquakes.
                earthquakes.add(earthquake);
            }

        } catch (JSONException e) {
            // If an error is thrown when executing any of the above statements in the "try" block,
            // catch the exception here, so the app doesn't crash. Print a log message
            // with the message from the exception.
            Log.e("QueryUtils", "Problem parsing the earthquake JSON results", e);
        }

        // Return the list of earthquakes
        return earthquakes;
    }


    // Add in the fetchEarthquakeData() helper method that ties all the steps together -
    // creating a java.net.URL, sending the request, processing the response.
    // Since this is the only “public” QueryUtils method that the EarthquakeAsyncTask needs to
    // interact with, make all other helper methods in QueryUtils “private”.
    /**
     * Query the USGS dataset and return a list of {@link Earthquake} objects.
     */
    public static java.util.List<Earthquake> fetchEarthquakeData(String requestUrl) {
//     Log.i(LOG_TAG, "Test: QueryUtils fetchEarthquakeData() called.");

        // Create URL object
        java.net.URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (java.io.IOException e) {
            Log.e(LOG_TAG, "Problem making the HTTP request.", e);
        }

        // Extract relevant fields from the JSON response and create a list of {@link Earthquake}s
        java.util.List<Earthquake> earthquakes = extractFeatureFromJson(jsonResponse);

        // Return the list of {@link Earthquake}s
        return earthquakes;
    }


    /**
     * Returns new URL object from the given string URL.
     */
    private static java.net.URL createUrl(String stringUrl) {
        java.net.URL url = null;
        try {
            url = new java.net.URL(stringUrl);
        } catch (java.net.MalformedURLException e) {
            Log.e(LOG_TAG, "Problem building the URL ", e);
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private static String makeHttpRequest(java.net.URL url) throws java.io.IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        java.net.HttpURLConnection urlConnection = null;
        java.io.InputStream inputStream = null;
        try {
            urlConnection = (java.net.HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (java.io.IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the earthquake JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // Closing the input stream could throw an IOException, which is why
                // the makeHttpRequest(URL url) method signature specifies than an IOException
                // could be thrown.
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link java.io.InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    private static String readFromStream(java.io.InputStream inputStream) throws java.io.IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            java.io.InputStreamReader inputStreamReader = new java.io.InputStreamReader(inputStream, java.nio.charset.Charset.forName("UTF-8"));
            java.io.BufferedReader reader = new java.io.BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

}
