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

package com.moonbloom.vlcremote.app;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.SearchView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;

import com.moonbloom.vlcremote.R;
import com.moonbloom.vlcremote.fragment.ArtFragment;
import com.moonbloom.vlcremote.fragment.BrowseFragment;
import com.moonbloom.vlcremote.fragment.ButtonsFragment;
import com.moonbloom.vlcremote.fragment.InfoFragment;
import com.moonbloom.vlcremote.fragment.NavigationFragment;
import com.moonbloom.vlcremote.fragment.PlaybackFragment;
import com.moonbloom.vlcremote.fragment.PlayingFragment;
import com.moonbloom.vlcremote.fragment.PlaylistFragment;
import com.moonbloom.vlcremote.fragment.StatusFragment;
import com.moonbloom.vlcremote.fragment.VolumeFragment;
import com.moonbloom.vlcremote.intent.Intents;
import com.moonbloom.vlcremote.listener.MediaServerListener;
import com.moonbloom.vlcremote.listener.UIVisibilityListener;
import com.moonbloom.vlcremote.model.Preferences;
import com.moonbloom.vlcremote.model.Reloadable;
import com.moonbloom.vlcremote.model.Reloader;
import com.moonbloom.vlcremote.model.Server;
import com.moonbloom.vlcremote.model.Status;
import com.moonbloom.vlcremote.model.Tags;
import com.moonbloom.vlcremote.net.MediaServer;
import com.moonbloom.vlcremote.net.xml.XmlContentHandler;
import com.moonbloom.vlcremote.sweep.PortSweeper;
import com.moonbloom.vlcremote.util.FragmentUtil;
import com.moonbloom.vlcremote.widget.Buttons;
import com.moonbloom.vlcremote.widget.LockableViewPager;
import com.moonbloom.vlcremote.widget.VolumePanel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaybackActivity extends FragmentActivity implements TabHost.OnTabChangeListener, UIVisibilityListener, Reloader {

    private static final String TAG = "PlaybackActivity";
    
    private static final int REQUEST_PICK_SERVER = 1;

    private static final int VOLUME_LEVEL_UNKNOWN = -1;

    private static final String STATE_INPUT = "vlc:input";
    private static final String STATE_TAB = "vlc:tab";
    private static final String STATE_SEARCH = "vlc:search";

    private static final String TAB_MEDIA = "media";
    private static final String TAB_PLAYLIST = "playlist";
    private static final String TAB_BROWSE = "browse";
    private static final String TAB_NAVIGATION = "navigation";
    
    private static final int TAB_NAVIGATION_INDEX = 3;

    private static final int MAX_VOLUME = 1024;

    private final List<TabHost.TabSpec> mTabSpecList = new ArrayList<>();
    
    /**
     * This is used to store the value of the users preference before the 
     * pick server activity is created.
     */
    private boolean isHideDVDTab = false;
    
    private boolean isBottomActionbarVisible = false;
    
    private boolean isVolumeFragmentVisible = false;
    
    private boolean lastResponseError = false;
    
    private MediaServer mMediaServer;

    private TabHost mTabHost;

    private VolumePanel mVolumePanel;

    private BroadcastReceiver mStatusReceiver;

    private int mVolumeLevel = VOLUME_LEVEL_UNKNOWN;

    private int mLastNonZeroVolume = VOLUME_LEVEL_UNKNOWN;

    private String mInput;
    
    private String mLastFileName; // used for comparing against current to detect change

    private LockableViewPager mPager;
    
    private String mRestoredSearch;
    
    private SearchView mSearchView;
    
    private SearchView.OnQueryTextListener mSearchViewOnQueryListener;
    
    private List<MediaServerListener> mMediaServerListeners = new ArrayList<>();
    
    private Map<String, Reloadable> mReloadables = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.main);
        
        // Set the control stream to STREAM_MUSIC to suppress system beeps that sound even when the activity handles volume key events.
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Preferences pref = Preferences.get(this);
        isHideDVDTab = pref.isHideDVDTabSet();
        String authority = pref.getAuthority();
        if (authority != null) {
            mMediaServer = new MediaServer(this, authority);
            setServerSubtitle(pref.isServerSubtitleSet());
        }

        mPager = (LockableViewPager) findViewById(R.id.pager);
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mVolumePanel = new VolumePanel(this);

        FragmentUtil fu = new FragmentUtil(getSupportFragmentManager());
        fu.findOrAddFragment(Tags.FRAGMENT_STATUS, StatusFragment.class);
        
        if(mTabHost == null) {
            fu.findOrReplaceOptionalFragment(this, R.id.fragment_navigation, Tags.FRAGMENT_NAVIGATION, NavigationFragment.class);
            fu.findOrReplaceFragment(R.id.fragment_playlist, Tags.FRAGMENT_PLAYLIST, PlaylistFragment.class);
            fu.findOrReplaceFragment(R.id.fragment_browse, Tags.FRAGMENT_BROWSE, BrowseFragment.class);
            fu.findOrReplaceFragment(R.id.fragment_playback, Tags.FRAGMENT_PLAYBACK, PlaybackFragment.class);
            fu.findOrReplaceFragment(R.id.fragment_info, Tags.FRAGMENT_INFO, InfoFragment.class);
            fu.findOrReplaceOptionalFragment(this, R.id.fragment_art, Tags.FRAGMENT_ART, ArtFragment.class);
            fu.findOrReplaceFragment(R.id.fragment_buttons, Tags.FRAGMENT_BUTTONS, ButtonsFragment.class);
            VolumeFragment mVolume = fu.findOrReplaceFragment(R.id.fragment_volume, Tags.FRAGMENT_VOLUME, VolumeFragment.class);
            setVolumeFragmentVisible(mVolume != null);
        } else {
            setupTabHost();
            mPager.setOffscreenPageLimit(4);
            mPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(), pref.isHideDVDTabSet()));
            mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    mTabHost.setCurrentTab(position);
                }
            });
            if(savedInstanceState != null) {
                fu.removeFragmentsByTag(
                    Tags.FRAGMENT_PLAYBACK, Tags.FRAGMENT_INFO, Tags.FRAGMENT_BUTTONS,
                    Tags.FRAGMENT_VOLUME, Tags.FRAGMENT_BOTTOMBAR, Tags.FRAGMENT_BROWSE,
                    Tags.FRAGMENT_NAVIGATION, Tags.FRAGMENT_PLAYLIST
                );
            }
        }

        if (savedInstanceState == null) {
            onNewIntent(getIntent());
        } else {
            notifyMediaServerListeners();
        }
    }
    
    private void setupTabHost() {
        mTabHost.setup();
        addTab(TAB_MEDIA, R.string.nowplaying_title, R.drawable.ic_tab_artists);
        addTab(TAB_PLAYLIST, R.string.tab_playlist, R.drawable.ic_tab_playlists);
        addTab(TAB_BROWSE, R.string.goto_start, R.drawable.ic_tab_playback);
        addTab(TAB_NAVIGATION, R.string.tab_dvd, R.drawable.ic_tab_albums);
        if(isHideDVDTab) {
            mTabHost.getTabWidget().removeView(mTabHost.getTabWidget().getChildTabViewAt(TAB_NAVIGATION_INDEX));
        }
        mTabHost.setOnTabChangedListener(this);
        onTabChanged(mTabHost.getCurrentTabTag());
    }

    private void addTab(String tag, int label, int icon) {
        TabHost.TabSpec spec = mTabHost.newTabSpec(tag);
        spec.setContent(new TabFactory(this));
        spec.setIndicator(getText(label), getResources().getDrawable(icon));
        mTabHost.addTab(spec);
        mTabSpecList.add(spec);
    }
    
    public void updateTabs() {
        boolean isHideDVDSet = Preferences.get(this).isHideDVDTabSet();
        int curTab = mTabHost.getCurrentTab();
        mTabHost.setCurrentTab(0);
        mTabHost.clearAllTabs();
        for (int i = 0; i < mTabSpecList.size(); i++) {
            if(i == TAB_NAVIGATION_INDEX && isHideDVDSet) {
                continue;
            }
            mTabHost.addTab(mTabSpecList.get(i));
            mTabHost.setCurrentTab(i);
        }
        mTabHost.setCurrentTab(curTab == TAB_NAVIGATION_INDEX ? 0 : curTab);
        mPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(), isHideDVDSet));
    }

    public void onTabChanged(String tabId) {
        if(mPager.getCurrentItem() != mTabHost.getCurrentTab()) {
            mPager.setCurrentItem(mTabHost.getCurrentTab());
        }
        supportInvalidateOptionsMenu();
    }
    
    /**
     * Check if the given tab tag is the tag of the current tab.
     * @param tabTag Tab tag (cannot be null)
     * @return true if the given tag is the same as the current tab. false if
     * the tab host is null or is not the current tab
     */
    public boolean isCurrentTab(String tabTag) {
        return mTabHost != null && tabTag.equals(mTabHost.getCurrentTabTag());
    }
    
    public void toggleViewPagerLock(View v) {
        if(mPager != null) {
            mPager.setPagingEnabled(!((CheckBox) v).isChecked());
        }
    }

    @Override
    public boolean onSearchRequested() {
        String initialQuery = mInput;
        boolean selectInitialQuery = true;
        Bundle appSearchData = null;
        boolean globalSearch = false;
        startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.playback_options, menu);            
        getMenuInflater().inflate(R.menu.playlist_options, menu);            
        getMenuInflater().inflate(R.menu.browse_options, menu);
        mSearchView = (SearchView) menu.findItem(R.id.menu_action_search).getActionView();
        mSearchView.setQueryHint(getString(R.string.action_search_title));
        if(mSearchViewOnQueryListener != null) {
            mSearchView.setOnQueryTextListener(mSearchViewOnQueryListener);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean isBrowseVisible = isCurrentTab(TAB_BROWSE);
        boolean isPlaylistVisible = mTabHost == null || isCurrentTab(TAB_PLAYLIST);
        boolean defaultVisibility = mTabHost == null || isCurrentTab(TAB_MEDIA) || isCurrentTab(TAB_NAVIGATION);
        boolean isAllButtonsVisible = isBottomActionbarVisible;
        boolean isButtonGroupVisible = isCurrentTab(TAB_MEDIA) && !isAllButtonsVisible;
        menu.findItem(R.id.menu_preferences).setVisible(defaultVisibility);
        MenuItem i = menu.findItem(R.id.menu_action_search).setVisible(isPlaylistVisible);
        onPrepareSearchView(i, isPlaylistVisible);
        menu.findItem(R.id.menu_clear_playlist).setVisible(isPlaylistVisible);
        menu.findItem(R.id.menu_refresh).setVisible(isPlaylistVisible);
        menu.findItem(R.id.menu_home).setVisible(isBrowseVisible);
        menu.findItem(R.id.menu_libraries).setVisible(isBrowseVisible);
        menu.findItem(R.id.menu_parent).setVisible(isBrowseVisible);
        menu.findItem(R.id.menu_set_home).setVisible(isBrowseVisible);
        menu.findItem(R.id.menu_text_size).setVisible(isBrowseVisible);
        if(isButtonGroupVisible) {
            Buttons.setupMenu(menu, Preferences.get(this));
        }
        menu.setGroupVisible(R.id.group_vlc_actions, isCurrentTab(TAB_MEDIA) && !isAllButtonsVisible);
        return true;
    }
    
    private void onPrepareSearchView(MenuItem searchItem, boolean isPlaylistVisible) {
        if(!isPlaylistVisible) {
            searchItem.collapseActionView();
        } else if(isPlaylistVisible && mRestoredSearch != null) {
            searchItem.expandActionView();
            mSearchView.setQuery(mRestoredSearch, true);
            mSearchView.clearFocus();
            if(mRestoredSearch.equals("")) {
                searchItem.collapseActionView();
            }
            mRestoredSearch = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_preferences:
                pickServer();
                return true;
            case R.id.menu_action_button_first:
                Buttons.sendCommand(mMediaServer, this, Preferences.KEY_BUTTON_FIRST);
                return true;
            case R.id.menu_action_button_second:
                Buttons.sendCommand(mMediaServer, this, Preferences.KEY_BUTTON_SECOND);
                return true;
            case R.id.menu_action_button_third:
                Buttons.sendCommand(mMediaServer, this, Preferences.KEY_BUTTON_THIRD);
                return true;
            case R.id.menu_action_button_fourth:
                Buttons.sendCommand(mMediaServer, this, Preferences.KEY_BUTTON_FOURTH);
                return true;
            case R.id.menu_action_button_fifth:
                Buttons.sendCommand(mMediaServer, this, Preferences.KEY_BUTTON_FIFTH);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void setBottomActionbarFragmentVisible(boolean isVisible) {
        isBottomActionbarVisible = isVisible;
    }
    
    @Override
    public void setVolumeFragmentVisible(boolean isVisible) {
        isVolumeFragmentVisible = isVisible;
    }
    
    private void setServerSubtitle(boolean isServerSubtitleSet) {
        if(!isServerSubtitleSet) {
            getActionBar().setSubtitle(null);
            return;
        }
        Preferences pref = Preferences.get(this);
        List<String> servers = pref.getRememberedServers();
        for(String key : servers) {
            Server s = Server.fromKey(key);
            if(s.getUri().getAuthority().equals(pref.getAuthority())) {
                getActionBar().setSubtitle(s.getNickname().isEmpty() ? s.getHostAndPort() : s.getNickname());
                return;
            }
        }
    }

    private void pickServer() {
        Preferences preferences = Preferences.get(this);
        isHideDVDTab = preferences.isHideDVDTabSet();
        Intent intent = new Intent(this, PickServerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(PortSweeper.EXTRA_PORT, 8080);
        intent.putExtra(PortSweeper.EXTRA_FILE, "/requests/status.xml");
        startActivityForResult(intent, REQUEST_PICK_SERVER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_PICK_SERVER:
                Preferences preferences = Preferences.get(this);

                if (resultCode == RESULT_OK) {
                    String authority = data.getData().getAuthority();
                    changeServer(authority);
                    preferences.setAuthority(authority);
                    Bundle args = new Bundle();
                    args.putString(BrowseFragment.State.DIRECTORY, "~");
                    reload(Tags.FRAGMENT_BROWSE, args);
                } else {
                    reload(Tags.FRAGMENT_BROWSE, null);
                    reload(Tags.FRAGMENT_PLAYLIST, null);
                }
                
                if(preferences.isHideDVDTabSet() != isHideDVDTab && mTabHost != null) {
                    updateTabs();
                    isHideDVDTab = !isHideDVDTab;
                }
                
                reload(Tags.FRAGMENT_BOTTOMBAR, null);
                reload(Tags.FRAGMENT_BUTTONS, null);
                supportInvalidateOptionsMenu();
                setServerSubtitle(preferences.isServerSubtitleSet());

                if (mMediaServer == null) {
                    finish();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaServerListeners = null;
        mReloadables = null;
    }
    
    public void setSearchViewOnQueryTextListener(SearchView.OnQueryTextListener listener) {
        mSearchViewOnQueryListener = listener;
    }
    
    public MediaServer addMediaServerListener(MediaServerListener l) {
        mMediaServerListeners.add(l);
        return mMediaServer;
    }
    
    private void notifyMediaServerListeners() {
        for(MediaServerListener l : mMediaServerListeners) {
            l.onNewMediaServer(mMediaServer);
        }
    }

    private void changeServer(String authority) {
        mMediaServer = new MediaServer(this, authority);
        notifyMediaServerListeners();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String host = intent.getStringExtra(Intents.EXTRA_REMOTE_HOST);
        if (host != null) {
            int port = intent.getIntExtra(Intents.EXTRA_REMOTE_PORT, 8080);
            String authority = host + ":" + port;
            changeServer(authority);
        }

        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action) || Intents.ACTION_REMOTE_VIEW.equals(action)
                || Intents.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data != null) {
                changeInput(data.toString());
            }
        } else if (Intent.ACTION_SEARCH.equals(action)) {
            String input = intent.getStringExtra(SearchManager.QUERY);
            changeInput(input);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_INPUT, mInput);
        if (mTabHost != null) {
            outState.putString(STATE_TAB, mTabHost.getCurrentTabTag());
        }
        if(mSearchView != null) {
            outState.putString(STATE_SEARCH, mSearchView.getQuery().toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mInput = savedInstanceState.getString(STATE_INPUT);
        if (mTabHost != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString(STATE_TAB));
        }
        mRestoredSearch = savedInstanceState.getString(STATE_SEARCH);
    }

    private void changeInput(String input) {
        if (mMediaServer == null) {
            Log.w(TAG, "No server selected");
            return;
        }   
        mInput = input;
        if (mInput != null) {
            mMediaServer.status().command.input.play(mInput);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mStatusReceiver = new StatusReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_STATUS);
        filter.addAction(Intents.ACTION_ERROR);
        registerReceiver(mStatusReceiver, filter);
        if (mMediaServer == null) {
            pickServer();
        }
        if(Preferences.get(this).isNotificationSet()) {
            startService(Intents.service(this, Intents.ACTION_NOTIFICATION_CREATE));
        }
    }

    @Override
    public void onPause() {
        unregisterReceiver(mStatusReceiver);
        mStatusReceiver = null;
        super.onPause();
    }

    void onVolumeChanged(int volume) {
        if (!isVolumeFragmentVisible && mVolumeLevel != VOLUME_LEVEL_UNKNOWN && mVolumeLevel != volume) {
            mVolumePanel.onVolumeChanged(volume);
        }
        mVolumeLevel = volume;
        if (0 != volume) {
            mLastNonZeroVolume = volume;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int c = event.getUnicodeChar();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (mVolumeLevel != VOLUME_LEVEL_UNKNOWN) {
                setVolume(mVolumeLevel + mMediaServer.status().command.getVolumeUpDown());
            } else {
                mMediaServer.status().command.volumeUp();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (mVolumeLevel != VOLUME_LEVEL_UNKNOWN) {
                setVolume(mVolumeLevel - mMediaServer.status().command.getVolumeUpDown());
            } else {
                mMediaServer.status().command.volumeDown();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (event.isAltPressed()) {
                mMediaServer.status().command.seek(Uri.encode("-".concat(Preferences.get(this).getSeekTime())));
                return true;
            } else if (event.isShiftPressed()) {
                mMediaServer.status().command.seek(Uri.encode("-3"));
                return true;
            } else {
                mMediaServer.status().command.key("nav-left");
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (event.isAltPressed()) {
                mMediaServer.status().command.seek(Uri.encode("+".concat(Preferences.get(this).getSeekTime())));
                return true;
            } else if (event.isShiftPressed()) {
                mMediaServer.status().command.seek(Uri.encode("+3"));
                return true;
            } else {
                mMediaServer.status().command.key("nav-right");
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            mMediaServer.status().command.key("nav-up");
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            mMediaServer.status().command.key("nav-down");
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            mMediaServer.status().command.key("nav-activate");
            return true;
        } else if (c == ' ') {
            mMediaServer.status().command.playback.pause();
            return true;
        } else if (c == 's') {
            mMediaServer.status().command.playback.stop();
            return true;
        } else if (c == 'p') {
            mMediaServer.status().command.playback.previous();
            return true;
        } else if (c == 'n') {
            mMediaServer.status().command.playback.next();
            return true;
        } else if (c == '+') {
            // TODO: Play faster
            return super.onKeyDown(keyCode, event);
        } else if (c == '-') {
            // TODO: Play slower
            return super.onKeyDown(keyCode, event);
        } else if (c == 'f') {
            mMediaServer.status().command.fullscreen();
            return true;
        } else if (c == 'm') {
            mute();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void setVolume(int volume) {
        volume = Math.max(volume, 0);
        volume = Math.min(volume, MAX_VOLUME);
        mMediaServer.status().command.volume(volume);
        onVolumeChanged(volume);
    }

    private void mute() {
        // The web interface doesn't have a documented mute command.
        if (mVolumeLevel != 0) {
            // Set the volume to zero
            mMediaServer.status().command.volume(0);
        } else if (mLastNonZeroVolume != VOLUME_LEVEL_UNKNOWN) {
            // Restore the volume to the last known value
            mMediaServer.status().command.volume(mLastNonZeroVolume);
        }
    }

    public void addReloadable(String tag, Reloadable r) {
        mReloadables.put(tag, r);
    }

    public void reload(String tag, Bundle args) {
        if(mReloadables.containsKey(tag)) {
            mReloadables.get(tag).reload(args);
        }
    }
    
    public void reloadDelayed(final String tag, final Bundle args, long delayMillis) {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                reload(tag, args);
            }
        }, delayMillis);
    }

    private class StatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intents.ACTION_STATUS.equals(action)) {
                Status status = (Status) intent.getSerializableExtra(Intents.EXTRA_STATUS);
                if(!TextUtils.equals(mLastFileName, status.getTrack().getName())) {
                    mLastFileName = status.getTrack().getName();
                    Preferences.get(PlaybackActivity.this).resetPresetDelay();
                }
                if(lastResponseError) {
                    lastResponseError = false;
                    reload(Tags.FRAGMENT_BROWSE, null);
                }
                onVolumeChanged(status.getVolume());
            } else if (Intents.ACTION_ERROR.equals(action)) {
                Throwable t = (Throwable) intent.getSerializableExtra(Intents.EXTRA_THROWABLE);
                if(t != null && !XmlContentHandler.ERROR_INVALID_XML.equals(t.getMessage())) {
                    lastResponseError = true;
                }
            }
        }
    }
    
    public static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final boolean isHideDVDSet;
        
        public ViewPagerAdapter(FragmentManager fm, boolean isHideDVDSet) {
            super(fm);
            this.isHideDVDSet = isHideDVDSet;
        }
        
        @Override
        public Fragment getItem(int i) {
            switch(i) {
                case 1: return new PlaylistFragment();
                case 2: return new BrowseFragment();
                case 3: return NavigationFragment.lockableInstance();
                default: return new PlayingFragment();
            }
        }

        @Override
        public int getCount() {
            return isHideDVDSet ? 3 : 4;
        }
    }
    
    public class TabFactory implements TabContentFactory {
 
        private final Context mContext;
 
        public TabFactory(Context context) {
            mContext = context;
        }
 
        public View createTabContent(String tag) {
            View v = new View(mContext);
            v.setMinimumWidth(0);
            v.setMinimumHeight(0);
            return v;
        }
    }
}