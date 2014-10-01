/*-
 *  Copyright (C) 2011 Peter Baldwin   
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.peterbaldwin.vlcremote.fragment;

import com.google.gson.Gson;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.loader.FavouriteTvShowsLoader;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Reloadable;
import org.peterbaldwin.vlcremote.model.Reloader;
import org.peterbaldwin.vlcremote.model.Remote;
import org.peterbaldwin.vlcremote.model.Tags;
import org.peterbaldwin.vlcremote.net.xml.XmlContentHandler;
import org.peterbaldwin.vlcremote.tvmodel.Episode;
import org.peterbaldwin.vlcremote.tvmodel.Season;
import org.peterbaldwin.vlcremote.tvmodel.Show;
import org.peterbaldwin.vlcremote.widget.TvShowsAdapter;

/**
 * Show favourite tvshows.
 */
public class TvShowsFragment extends MediaListFragment implements
        LoaderManager.LoaderCallbacks<Remote<ArrayList<Object>>>, Reloadable {
    private static final String TAG = "TvShowsFragment";
    private static final String SHOW_ID = "showId";
    private static final String SEASON_NUM = "seasonNum";
    private static final String SHOW_TITLE = "showTitle";
    private Preferences mPreferences;
    private TextView mTitle;
    private TextView mEmpty;
    private TvShowsAdapter mAdapter;
    private static int showId = 0;
    private String showTitle = null;
    private int seasonNum = 0;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Reloader) activity).addReloadable(Tags.FRAGMENT_TV_SHOWS, this);
    }
    
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        boolean visible = showId > 0;
        menu.findItem(R.id.menu_go_up).setVisible(visible);
        
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mPreferences = Preferences.get(getActivity());
        if (savedInstanceState == null) {
            showId = mPreferences.getShowId();
            seasonNum = mPreferences.getSeasonNum();
            showTitle = mPreferences.getShowTitle();
        } else {
            showId = savedInstanceState.getInt(SHOW_ID);
            seasonNum = savedInstanceState.getInt(SEASON_NUM);
            showTitle = savedInstanceState.getString(SHOW_TITLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tvshows, root, false);
        mTitle = (TextView) view.findViewById(android.R.id.title);
        mEmpty = (TextView) view.findViewById(android.R.id.empty);
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_go_up:
                goUpOne();
                return true;
            case R.id.menu_refresh_shows:
                getLoaderManager().restartLoader(0, null, this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean goUpOne() {
        boolean changed = false;
        
        if (seasonNum > 0) {
            seasonNum = 0;
            changed = true;
        } else if (showId > 0) {
            showId = 0;
            showTitle = null;
            changed = true;
        }

        getLoaderManager().restartLoader(0, null, this);
        
        return changed;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SHOW_TITLE, showTitle);
        outState.putInt(SHOW_ID, showId);
        outState.putInt(SEASON_NUM, seasonNum);
    }

    @Override
    public void setEmptyText(CharSequence text) {
        mEmpty.setText(text);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new TvShowsAdapter(getActivity());
        setListAdapter(mAdapter);

        Log.d(TAG, "onActivityCreated");
        registerForContextMenu(getListView());
        getLoaderManager().initLoader(0, null, this);
    }

    private void setTitle(CharSequence title) {
        mTitle.setText(title);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void reload(Bundle args) {
        if (getActivity() != null) {
            int sId = args != null && args.containsKey(SHOW_ID) ? args.getInt(SHOW_ID) : showId;
            int sNum = args != null && args.containsKey(SEASON_NUM) ? args.getInt(SEASON_NUM)
                    : seasonNum;
            
            showTitle = args != null && args.containsKey(SHOW_TITLE) ? args.getString(SHOW_TITLE)
                    : showTitle;

            getShowSeasonsOrEpisodes(sId, sNum);
        }
    }

    private void getShowSeasonsOrEpisodes(int id, int season) {
        showId = id;
        seasonNum = season;

        mAdapter.clear();
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        
        Log.d(TAG, "onListItemClick");

        Object item = mAdapter.getItem(position);

        if (item instanceof Show) {
            showId = ((Show) item).getId();
            showTitle = ((Show) item).getTitle();
            getShowSeasonsOrEpisodes(showId, 0);
        } else if (item instanceof Season) {
            seasonNum = ((Season) item).getNum();
            getShowSeasonsOrEpisodes(showId, seasonNum);
        } else {
            Episode ep = (Episode) item;
            if(!ep.getWatched()) {
                v.setBackgroundColor(Color.parseColor("#1d4a38"));
            } else {
                v.setBackgroundColor(Color.TRANSPARENT); 
            }
            
            new ToggleEpisodeWatched( (LoaderCallbacks<?>) this, v).execute(ep.getId());
        }

    }

    /** {@inheritDoc} */
    public Loader<Remote<ArrayList<Object>>> onCreateLoader(int id, Bundle args) {
        mPreferences.setShowTitle(showTitle);
        mPreferences.setShowId(showId);
        mPreferences.setSeasonNum(seasonNum);
        setEmptyText(getText(R.string.loading));
        return new FavouriteTvShowsLoader(getActivity(), showId, seasonNum);
    }

    public void onLoadFinished(Loader<Remote<ArrayList<Object>>> loader,
            Remote<ArrayList<Object>> result) {
        Log.d(TAG, "onLoaderFinished");

        mAdapter.setData((ArrayList<Object>) result.data);
        setEmptyText(getText(R.string.connection_error));
        
        String title = "";
        
        if(showId == 0) {
            title = "Twoje seriale ("+result.data.size()+")";
        } else {
            title = showTitle;
            
            if(seasonNum > 0) {
                title += " - season "+seasonNum;
            }
        }

        setTitle(title);
        
        this.getActivity().invalidateOptionsMenu();        
    }

    /** {@inheritDoc} */
    public void onLoaderReset(Loader<Remote<ArrayList<Object>>> loader) {
        Log.d(TAG, "onLoaderRest");
        mAdapter.setData(null);
    }

    @Override
    public boolean onBackPressed() {
        if(goUpOne()) {
            return true;
        }
        return false;
    }
    
    private class ToggleEpisodeWatched extends AsyncTask<Integer, Integer, Integer> {
        
        private LoaderManager.LoaderCallbacks<?> context;
        private View elementView;
        
        public ToggleEpisodeWatched(LoaderCallbacks<?> loaderCallbacks, View v) {
            this.context = (LoaderCallbacks<?>) loaderCallbacks;
            this.elementView = v;
        }

        protected Integer doInBackground(Integer... eps) {
            
            int epId = eps[0];
            
            HttpClient hc = new DefaultHttpClient();
            HttpGet request = new HttpGet("http://tvhelper.codeone.pl/api/watched/"+epId);

            HttpResponse rp;
            try {
                rp = hc.execute(request);
                
                
                if(rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    rp.getEntity().writeTo(out);
                    out.close();
                    String responseString = out.toString();
                    return Integer.valueOf(responseString);
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return -1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result > -1) {
                if(elementView != null) {
                    if(result == 1) {
                        elementView.setBackgroundColor(Color.parseColor("#1d4a38"));
                    } else {
                        elementView.setBackgroundColor(Color.TRANSPARENT); 
                    }
                }
                //getLoaderManager().restartLoader(0, null, context);
            }
        }
    }
}


