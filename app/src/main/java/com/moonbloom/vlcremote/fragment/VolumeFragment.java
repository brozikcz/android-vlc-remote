
package com.moonbloom.vlcremote.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.moonbloom.vlcremote.R;
import com.moonbloom.vlcremote.intent.Intents;
import com.moonbloom.vlcremote.model.Status;

public class VolumeFragment extends MediaFragment implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private static final int MAX_VOLUME = 512;

    private ImageView mIcon;
    private SeekBar mSeekBar;
    private TextView mVolumeText;

    private BroadcastReceiver mStatusReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.volume_fragment, root, false);
        mIcon = (ImageView) view.findViewById(android.R.id.icon);
        mSeekBar = (SeekBar) view.findViewById(android.R.id.progress);
        mIcon.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);
        mVolumeText = (TextView) view.findViewById(R.id.volume_text);
        return view;
    }

    /** {@inheritDoc} */
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            setVolume(progress);
        }
    }

    /** {@inheritDoc} */
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    /** {@inheritDoc} */
    public void onStopTrackingTouch(SeekBar seekBar) {
        setVolume(seekBar.getProgress());
    }

    private void setVolume(int value) {
        getMediaServer().status().command.volume(value);
        updateVolumeText(value);
    }

    private void updateVolumeText(int volume) {
        long calcVolume = Math.round(volume / 2.56);
        mVolumeText.setText(calcVolume + "%");
    }

    void onVolumeChanged(int value) {
        getMediaServer().status().command.setLastVolume(value);
        mIcon.setImageResource(getVolumeImage(value));
        mSeekBar.setProgress(value);
        updateVolumeText(value);
    }

    private static int getVolumeImage(int volume) {
        if (volume == 0) {
            return R.drawable.ic_media_volume_muted;
        } else if (volume < (MAX_VOLUME / 3)) {
            return R.drawable.ic_media_volume_low;
        } else if (volume < (2 * MAX_VOLUME / 3)) {
            return R.drawable.ic_media_volume_medium;
        } else {
            return R.drawable.ic_media_volume_high;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mStatusReceiver = new StatusReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_STATUS);
        getActivity().registerReceiver(mStatusReceiver, filter);
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mStatusReceiver);
        mStatusReceiver = null;
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        getMediaServer().status().command.mute();
    }

    private class StatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intents.ACTION_STATUS.equals(action)) {
                Status status = (Status) intent.getSerializableExtra(Intents.EXTRA_STATUS);
                onVolumeChanged(status.getVolume());
            }
        }
    }
}