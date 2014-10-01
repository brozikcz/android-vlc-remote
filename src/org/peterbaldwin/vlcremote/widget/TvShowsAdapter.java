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

package org.peterbaldwin.vlcremote.widget;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.tvmodel.Episode;
import org.peterbaldwin.vlcremote.tvmodel.Season;
import org.peterbaldwin.vlcremote.tvmodel.Show;

public class TvShowsAdapter extends BaseAdapter implements OnClickListener {
    private LayoutInflater mInflater;
    private ArrayList<Object> items = new ArrayList<Object>();
    private Context context;

    public TvShowsAdapter(Context context) {
        this.context = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItem(position) instanceof Show) {
            Show show = (Show) getItem(position);
            convertView = populateShow(convertView, show);
        } else if (getItem(position) instanceof Season) {
            Season season = (Season) getItem(position);
            convertView = populateSeason(convertView, season);
        } else if (getItem(position) instanceof Episode) {
            Episode episode = (Episode) getItem(position);
            convertView = populateEpisode(convertView, episode);
        }
        return convertView;
    }

    private View populateEpisode(View convertView, Episode episode) {
        if(convertView == null || !convertView.getTag().equals("episode")) {
            convertView = mInflater.inflate(R.layout.episodes_list_item, null);
        }

        if(episode.getWatched()) {
            convertView.setBackgroundColor(Color.parseColor("#1d4a38"));
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT); 
        }
        
        ImageButton play = (ImageButton) convertView.findViewById(R.id.playEpisode);
        
        if(episode.hasFile()) {
            play.setVisibility(View.VISIBLE);
            play.setTag(episode.getId());
            play.setOnClickListener(this);
        } else {
            play.setVisibility(View.GONE);
        }
        
        
        
        TextView tv = (TextView) convertView.findViewById(R.id.episode_text);
        tv.setText("Ep "+episode.getNum() + " - "+episode.getTitle());
        convertView.setTag("episode");
        return convertView;
    }

    private View populateSeason(View convertView, Season season) {
        if(convertView == null || !convertView.getTag().equals("season")) {
            convertView = mInflater.inflate(R.layout.seasons_list_item, null);
        }
        
        TextView tv = (TextView) convertView.findViewById(R.id.season_num);
        tv.setText("S"+season.getNum());

        ProgressBar pb = (ProgressBar) convertView.findViewById(R.id.season_progress);
        pb.setProgress(season.getProgress());
        pb.setMax(season.getTotal());
        convertView.setTag("season");
        return convertView;
    }

    private View populateShow(View convertView, Show show) {
        if(convertView == null || !convertView.getTag().equals("show")) {
            convertView = mInflater.inflate(R.layout.shows_list_item, null);
        }
        TextView tv = (TextView) convertView.findViewById(R.id.show_name);
        tv.setText(show.getTitle());

        ProgressBar pb = (ProgressBar) convertView.findViewById(R.id.show_progress);
        pb.setProgress(show.getProgress());
        pb.setMax(show.getTotal());
        convertView.setTag("show");
        return convertView;
    }
    
    public void setData(ArrayList<Object> items) {
        clear();
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return this.items.size();
    }

    @Override
    public Object getItem(int position) {
        return this.items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public void onClick(View v) {
        new PlayEpisode().execute((Integer) v.getTag());
    }
    
    private class PlayEpisode extends AsyncTask<Integer, Integer, Boolean> {
        private static final String TAG = "PlayEpisode";

        protected Boolean doInBackground(Integer... eps) {
            
            int epId = eps[0];
            
            HttpClient hc = new DefaultHttpClient();
            HttpGet request = new HttpGet("http://tvhelper.codeone.pl/show/watch/"+epId);

            HttpResponse rp;
            try {
                rp = hc.execute(request);
                
                
                if(rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    rp.getEntity().writeTo(out);
                    out.close();
                    String responseString = out.toString();
                    Log.d(TAG, responseString);
                    return true;
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }
    }
}
