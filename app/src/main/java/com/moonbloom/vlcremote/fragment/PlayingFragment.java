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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.moonbloom.vlcremote.R;
import com.moonbloom.vlcremote.app.PlaybackActivity;
import com.moonbloom.vlcremote.listener.UIVisibilityListener;
import com.moonbloom.vlcremote.model.Tags;
import com.moonbloom.vlcremote.util.FragmentUtil;

/**
 *
 * @author Ice
 */
public class PlayingFragment extends Fragment {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View v =  inflater.inflate(R.layout.playing_fragment, root, false);
        FragmentUtil fu = new FragmentUtil(getChildFragmentManager());
        fu.findOrReplaceFragment(R.id.fragment_info, Tags.FRAGMENT_INFO, InfoFragment.class);
        VolumeFragment mVolume = fu.findOrReplaceOptionalFragment(v, R.id.fragment_volume, Tags.FRAGMENT_VOLUME, VolumeFragment.class);
        fu.findOrReplaceFragment(R.id.fragment_buttons, Tags.FRAGMENT_BUTTONS, ButtonsFragment.class);
        fu.findOrReplaceFragment(R.id.fragment_playback, Tags.FRAGMENT_PLAYBACK, PlaybackFragment.class);
        BottomActionbarFragment mBottomActionBar = fu.findOrReplaceOptionalFragment(v, R.id.fragment_bottom_actionbar, Tags.FRAGMENT_BOTTOMBAR, BottomActionbarFragment.class);
        UIVisibilityListener ui = (PlaybackActivity) getActivity();
        // notify activity about optional fragments visibility
        ui.setBottomActionbarFragmentVisible(mBottomActionBar != null);
        ui.setVolumeFragmentVisible(mVolume != null);
        return v;
    }
}