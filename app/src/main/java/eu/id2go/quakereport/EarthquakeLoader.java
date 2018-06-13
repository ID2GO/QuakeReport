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

import android.content.AsyncTaskLoader;
import android.content.Context;

import java.util.List;

// Define the EarthquakeLoader class, extend AsyncTaskLoader and specify List as the generic
// parameter. This explains what type of data is expected to be loaded. In this case, the loader is
// loading a list of Earthquake objects. Then take a String URL in the constructor, and in
// loadInBackground(), do the exact same operations as in doInBackground back in
// EarthquakeAsyncTask. Important: Override the onStartLoading() method to call forceLoad() which is
// a required step to actually trigger the loadInBackground() method to execute.
/**
 * Loads a list of earthquakes by using an AsyncTask to perform the
 * network request to the given URL.
 */
public class EarthquakeLoader extends AsyncTaskLoader<List<Earthquake>> {

    /** Tag for log messages */
    private static final String LOG_TAG = EarthquakeLoader.class.getName();

    /** Query URL */
    private String mUrl;

    /**
     * Constructs a new {@link EarthquakeLoader}.
     *
     * @param context of the activity
     * @param url to load data from
     */
    public EarthquakeLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }

    @Override
    protected void onStartLoading() {
//        android.util.Log.i(LOG_TAG, "Test: Earthquake Loader onStartLoading() called.");

        forceLoad();
    }

    /**
     * This is on a background thread.
     */
    @Override
    public List<Earthquake> loadInBackground() {
//        android.util.Log.i(LOG_TAG, "Test: Earthquake Loader loadInBackground() called.");

        if (mUrl == null) {
            return null;
        }

        // Perform the network request, parse the response, and extract a list of earthquakes.
        List<Earthquake> earthquakes = QueryUtils.fetchEarthquakeData(mUrl);
        return earthquakes;
    }
}