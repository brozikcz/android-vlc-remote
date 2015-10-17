/*
 * Copyright (C) 2013 Sam Malone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.moonbloom.vlcremote.fragment;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.ListFragment;

import com.moonbloom.vlcremote.app.PlaybackActivity;
import com.moonbloom.vlcremote.listener.MediaServerListener;
import com.moonbloom.vlcremote.net.MediaServer;

/**
 *
 * @author Sam Malone
 */
public class MediaListFragment extends ListFragment implements MediaServerListener {

    private MediaServer mMediaServer;
    
    public void onNewMediaServer(MediaServer server) {
        mMediaServer = server;
    }
    
    private MediaServer addMediaServerListener(PlaybackActivity activity) {
        return (activity == null) ? null : activity.addMediaServerListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mMediaServer = addMediaServerListener((PlaybackActivity) activity);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMediaServer = addMediaServerListener((PlaybackActivity) context);
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        mMediaServer = null;
    }
    
    /**
     * Get the media server instance
     * @return media server instance or null if none set
     */
    protected MediaServer getMediaServer() {
        return mMediaServer;
    }
}