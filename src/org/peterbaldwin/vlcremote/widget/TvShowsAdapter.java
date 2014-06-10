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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.tvmodel.Episode;
import org.peterbaldwin.vlcremote.tvmodel.Season;
import org.peterbaldwin.vlcremote.tvmodel.Show;

public class TvShowsAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private ArrayList<Object> items = new ArrayList<Object>();

    public TvShowsAdapter(Context context) {
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

        /*
         * TextView tv = (TextView) v.findViewById(android.R.id.text1);
         * if(getItem(position) instanceof Show) { Show show = (Show)
         * getItem(position); TextView nextEp = (TextView)
         * v.findViewById(R.id.nextEp); tv.setText(show.getTitle()); } else
         * if(getItem(position) instanceof Season) { Season season = (Season)
         * getItem(position); tv.setText("Season "+season.getNum()); } else
         * if(getItem(position) instanceof Episode) { Episode episode =
         * (Episode) getItem(position);
         * tv.setText(episode.getSeason()+"x"+episode.getNum()); } return v;
         */
    }

    private View populateEpisode(View convertView, Episode episode) {
        convertView = mInflater.inflate(R.layout.episodes_list_item, null);
        TextView tv = (TextView) convertView.findViewById(R.id.episode_text);
        tv.setText("Ep "+episode.getNum() + " - "+episode.getTitle());
        return convertView;
    }

    private View populateSeason(View convertView, Season season) {
        convertView = mInflater.inflate(R.layout.shows_list_item, null);
        TextView tv = (TextView) convertView.findViewById(R.id.show_name);
        tv.setText("S"+season.getNum());

        ProgressBar pb = (ProgressBar) convertView.findViewById(R.id.show_progress);
        pb.setProgress(season.getProgress());
        pb.setMax(season.getTotal());
        return convertView;
    }

    private View populateShow(View convertView, Show show) {
        convertView = mInflater.inflate(R.layout.shows_list_item, null);
        TextView tv = (TextView) convertView.findViewById(R.id.show_name);
        tv.setText(show.getTitle());

        ProgressBar pb = (ProgressBar) convertView.findViewById(R.id.show_progress);
        pb.setProgress(show.getProgress());
        pb.setMax(show.getTotal());
        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    public void setData(ArrayList<Object> items) {
        this.items = items;
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
}
