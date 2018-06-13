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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;

import java.util.ArrayList;

public class EarthquakeActivity extends AppCompatActivity implements
        LoaderCallbacks<java.util.List<Earthquake>> {


    // When we get to the onPostExecute() method, we need to update the ListView. The only way
    // to update the contents of the list is to update the data set within the EarthquakeAdapter.
    // To access and modify the instance of the EarthquakeAdapter, we need to make it a global
    // variable in the EarthquakeActivity.
    /**
     * Adapter for the list of earthquakes
     */
    private EarthquakeAdapter mAdapter;
    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = EarthquakeActivity.class.getName();

    /**
     * URL for earthquake data from the USGS dataset
     */
    private static final String USGS_REQUEST_URL ="https://earthquake.usgs.gov/fdsnws/event/1/query";


    /**
     * Constant value for the earthquake loader ID. We can choose any integer.
     * This really only comes into play if you're using multiple loaders.
     */
    private static final int EARTHQUAKE_LOADER_ID = 1;

    /** TextView that is displayed when the list is empty */
    private android.widget.TextView mEmptyStateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//      LOG for testing purposes
//      android.util.Log.i(LOG_TAG, "Test: Earthquake Activity onCreate() called.");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_earthquake);

        // Get the list of earthquakes from {@link QueryUtils}
//        ArrayList<Earthquake> earthquakes = QueryUtils.extractFeatureFromJson();

        // Find a reference to the {@link ListView} in the layout
        ListView earthquakeListView = (ListView) findViewById(R.id.list);

        // If there is no earthquake data to be displayed, mEmptyStateTextView is called to action
        mEmptyStateTextView = (android.widget.TextView) findViewById(R.id.empty_view);
        earthquakeListView.setEmptyView(mEmptyStateTextView);

        // Create a new adapter that takes an empty list of earthquakes as input
        mAdapter = new EarthquakeAdapter(this, new ArrayList<Earthquake>());
        // Create a new adapter that takes the list of earthquakes as input
//        final EarthquakeAdapter adapter = new EarthquakeAdapter(this, earthquakes);

        // Set the adapter on the {@link ListView}
        // so the list can be populated in the user interface
        earthquakeListView.setAdapter(mAdapter);

        // Set an item click listener on the ListView, which sends an intent to a web browser
        // to open a website with more information about the selected earthquake.
        earthquakeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // Find the current earthquake that was clicked on
                Earthquake currentEarthquake = mAdapter.getItem(position);

                // Convert the String URL into a URI object (to pass into the Intent constructor)
                Uri earthquakeUri = Uri.parse(currentEarthquake.getUrl());

                // Create a new intent to view the earthquake URI
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, earthquakeUri);

                // Send the intent to launch a new activity
                startActivity(websiteIntent);
            }
        });

        // Get a reference to the ConnectivityManager to check state of network connectivity
        android.net.ConnectivityManager connMgr = (android.net.ConnectivityManager)
                getSystemService(android.content.Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        android.net.NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        // If there is a network connection, fetch data
        if (networkInfo != null && networkInfo.isConnected()) {
            // Get a reference to the LoaderManager, in order to interact with loaders.
            LoaderManager loaderManager = getLoaderManager();

            // Initialize the loader. Pass in the int ID constant defined above and pass in null for
            // the bundle. Pass in this activity for the LoaderCallbacks parameter (which is valid
            // because this activity implements the LoaderCallbacks interface).
            loaderManager.initLoader(EARTHQUAKE_LOADER_ID, null, this);
        } else {
            // Otherwise, display error
            // First, hide loading indicator so error message will be visible
            View loadingIndicator = findViewById(R.id.loading_indicator);
            loadingIndicator.setVisibility(View.GONE);

            // Update empty state with no connection error message
            mEmptyStateTextView.setText(R.string.no_internet_connection);
        }

    }

    // The onCreateLoader() is needed for when the LoaderManager has determined that the loader
    // with our specified ID isn't running, so we should create a new one.
    @Override
    public Loader<java.util.List<Earthquake>> onCreateLoader(int i, Bundle bundle) {
//      LOG for testing purposes
//        android.util.Log.i(LOG_TAG, "Test: Earthquake Activity onCreateLoader() called.");
        // Create a new loader for the given URL
//        return new EarthquakeLoader(this, USGS_REQUEST_URL);
        android.content.SharedPreferences sharedPrefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);

        // getString retrieves a String value from the preferences. The second param is the default
        // value for this preference.
        String minMagnitude = sharedPrefs.getString(
                getString(R.string.settings_min_magnitude_key),
                getString(R.string.settings_min_magnitude_default));

        String orderBy = sharedPrefs.getString(
                getString(R.string.settings_order_by_key),
                getString(R.string.settings_order_by_default)
        );

        // parse breaks apart the URI string that's passed into its params.
        Uri baseUri = Uri.parse(USGS_REQUEST_URL);
        // buildUpon prepares the baseUri that we just parsed so we can add query params tot it.
        Uri.Builder uriBuilder = baseUri.buildUpon();

        // Append query params & value, Exemple: the format=geojson
        uriBuilder.appendQueryParameter("format", "geojson");
        uriBuilder.appendQueryParameter("limit", "20");
        uriBuilder.appendQueryParameter("minmag", minMagnitude);
        uriBuilder.appendQueryParameter("orderby", "time");
        uriBuilder.appendQueryParameter("orderby", orderBy);

        // Return the completed uri `http://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&limit=10&minmag=minMagnitude&orderby=time
        return new EarthquakeLoader(this, uriBuilder.toString());
    }
    // The onLoadFinished() is needed for updating the dataset in the adapter
    @Override
    public void onLoadFinished(Loader<java.util.List<Earthquake>> loader, java.util.List<Earthquake> earthquakes) {
        // Clear the adapter of previous earthquake data
//      android.util.Log.i(LOG_TAG, "Test: Earthquake Activity onLoadFinished() called.");
        mAdapter.clear();

        // If there is a valid list of {@link Earthquake}s, then add them to the adapter's
        // data set. This will trigger the ListView to update.
        if (earthquakes != null && !earthquakes.isEmpty()) {
//        android.util.Log.i(LOG_TAG, "Test: Earthquake Activity onLoadFinished() if empty called.");

            // Hide loading indicator because the data has been loaded
            View loadingIndicator = findViewById(R.id.loading_indicator);
            loadingIndicator.setVisibility(View.GONE);

            // Set empty state text to display "No earthquakes found."
            mEmptyStateTextView.setText(R.string.no_earthquakes);

            // Clear the adapter of previous earthquake data
            mAdapter.clear();

            // If there is a valid list of {@link Earthquake}s, then add them to the adapter's
            // data set. This will trigger the ListView to update.
            mAdapter.addAll(earthquakes);
//            updateUi(earthquakes);
        }
    }
    // The onLoaderReset() is needed for when the data from our loader is no langer valid and
    // the adapters data set needs to be reset
    @Override
    public void onLoaderReset(Loader<java.util.List<Earthquake>> loader) {
        // Loader reset, so we can clear out our existing data.
//        android.util.Log.i(LOG_TAG, "Test: Earthquake Activity onLoaderReset() called.");
        mAdapter.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
