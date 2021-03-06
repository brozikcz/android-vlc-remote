
package com.moonbloom.vlcremote.loader;

import com.moonbloom.vlcremote.R;
import com.moonbloom.vlcremote.net.MediaServer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.IOException;

public class ArtLoader extends ModelLoader<Drawable> {

    private final MediaServer mMediaServer;

    public ArtLoader(Context context, MediaServer mediaServer) {
        super(context);
        mMediaServer = mediaServer;
    }

    @Override
    public Drawable loadInBackground() {
        Resources res = getContext().getResources();
        if(mMediaServer == null) {
            return res.getDrawable(R.drawable.albumart_mp_unknown);
        }
        try {
            return new BitmapDrawable(res, mMediaServer.art().read());
        } catch (IOException e) {
            return res.getDrawable(R.drawable.albumart_mp_unknown);
        }
    }
}
