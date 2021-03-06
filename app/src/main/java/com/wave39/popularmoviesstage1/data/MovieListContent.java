package com.wave39.popularmoviesstage1.data;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.wave39.popularmoviesstage1.MainActivity;
import com.wave39.popularmoviesstage1.PosterListAdapter;
import com.wave39.popularmoviesstage1.PosterListFragment;
import com.wave39.popularmoviesstage1.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MovieListContent
 * Created by bp on 8/4/15.
 */

public class MovieListContent
{
    public final String LOG_TAG = MovieListContent.class.getSimpleName();

    public static List<MovieListItem> ITEMS = new ArrayList<>();
    public static Map<Integer, MovieListItem> ITEM_MAP = new HashMap<>();

    public PosterListAdapter theAdapter;
    public PosterListFragment theFragment;

    private String paramSortBy;

    private class DownloadMovieListTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                readMovieData(theFragment.getActivity());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return "Done";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(LOG_TAG, "onPostExecute result: " + result);
        }

        private void getMovieListDataFromJson(final Activity theActivity, String movieListJsonStr)
                throws JSONException {
            final String RESULTS = "results";
            final String ORIGINAL_TITLE = "original_title";
            final String TITLE = "title";
            final String MOVIE_ID = "id";
            final String POSTER_PATH = "poster_path";
            final String OVERVIEW = "overview";
            final String VOTE_AVERAGE = "vote_average";
            final String RELEASE_DATE = "release_date";

            clearArrays();
            try {
                JSONObject movieListJson = new JSONObject(movieListJsonStr);
                JSONArray resultsArray = movieListJson.getJSONArray(RESULTS);
                for (int idx = 0; idx < resultsArray.length(); idx++)
                {
                    JSONObject movie = resultsArray.getJSONObject(idx);
                    int movieId = movie.getInt(MOVIE_ID);
                    String originalTitle = movie.getString(ORIGINAL_TITLE);
                    String movieTitle = movie.getString(TITLE);
                    String posterPath = movie.getString(POSTER_PATH);
                    String overview = movie.getString(OVERVIEW);
                    double voteAverage = movie.getDouble(VOTE_AVERAGE);

                    @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date releaseDate = null;
                    String releaseDateString = movie.getString(RELEASE_DATE);
                    //Log.i(LOG_TAG, "Movie " + originalTitle + " has a release date of " + releaseDateString);
                    if ((releaseDateString != null) && (releaseDateString.length() == 10)) {
                        try {
                            releaseDate = dateFormat.parse(releaseDateString);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

//                    Log.i(LOG_TAG, "Found movie at index " + Integer.toString(idx) +
//                            " with id " + Integer.toString(movieId) + " and title " + movieTitle);

                    MovieListItem newItem = new MovieListItem();
                    newItem.id = movieId;
                    newItem.originalTitle = originalTitle;
                    newItem.title = movieTitle;
                    newItem.posterPath = posterPath;
                    newItem.overview = overview;
                    newItem.voteAverage = voteAverage;
                    newItem.releaseDate = releaseDate;

                    addNewMovieListItem(newItem);
                }

                theActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        theFragment.redrawWithNewData();
                    }
                });
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
        }

        private void readMovieData(Activity theActivity) throws JSONException {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String movieListJsonStr;

            try {
                final String MOVIE_LIST_BASE_URL =
                        "http://api.themoviedb.org/3/discover/movie?";
                final String SORT_BY_PARAM = "sort_by";
                final String API_KEY_PARAM = "api_key";
                final String API_KEY_VALUE = MainActivity.getContext().getString(R.string.TMDB_API_KEY);
                Uri builtUri = Uri.parse(MOVIE_LIST_BASE_URL).buildUpon()
                        .appendQueryParameter(SORT_BY_PARAM, paramSortBy)
                        .appendQueryParameter(API_KEY_PARAM, API_KEY_VALUE)
                        .build();

                URL url = new URL(builtUri.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                if (inputStream == null) {
                    return;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }

                if (buffer.length() == 0) {
                    return;
                }

                movieListJsonStr = buffer.toString();
                getMovieListDataFromJson(theActivity, movieListJsonStr);

                Log.i(LOG_TAG, "Done with readMovieData");
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
        }
    }

    private void readTheMovieData()
    {
        new DownloadMovieListTask().execute();
    }

    private static void clearArrays()
    {
        ITEMS.clear();
        ITEM_MAP.clear();
    }

    private static void addNewMovieListItem(MovieListItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    public void setAdapter(PosterListAdapter sourceAdapter)
    {
        theAdapter = sourceAdapter;
    }

    public void readAndDisplayData(String sortBy)
    {
        paramSortBy = sortBy;
        readTheMovieData();
    }

    public MovieListContent(PosterListFragment fragment, String sortBy)
    {
        theFragment = fragment;
        readAndDisplayData(sortBy);
    }
}
