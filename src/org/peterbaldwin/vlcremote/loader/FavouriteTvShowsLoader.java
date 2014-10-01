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

package org.peterbaldwin.vlcremote.loader;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.peterbaldwin.vlcremote.model.Remote;
import org.peterbaldwin.vlcremote.tvmodel.Episode;
import org.peterbaldwin.vlcremote.tvmodel.Season;
import org.peterbaldwin.vlcremote.tvmodel.Show;

public class FavouriteTvShowsLoader extends ModelLoader<Remote<ArrayList<Object>>> {

    private static final String TAG = "FavouriteTvShowsLoader";
    private static final int SHOWS_LIST = 0;
    private static final int SEASONS_LIST = 1;
    private static final int EPISODES_LIST = 2;

    private int showId = 0;
    private int seasonNum = 0;
    private int type = SHOWS_LIST;

    public FavouriteTvShowsLoader(Context context, int showId, int seasonNum) {
        super(context);
        this.showId = showId;
        this.seasonNum = seasonNum;

        if (showId > 0) {
            type = SEASONS_LIST;

            if (seasonNum > 0) {
                type = EPISODES_LIST;
            }
        }
    }

    @Override
    public Remote<ArrayList<Object>> loadInBackground() {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpParams httpParameters = new BasicHttpParams();
        int timeoutConnection = 3000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        int timeoutSocket = 5000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        httpclient.setParams(httpParameters);

        HttpResponse response;
        String responseString = null;

        ArrayList<Object> array = new ArrayList<Object>();

        try {
            String url = "http://tvhelper.codeone.pl/api/shows";

            if (showId > 0) {
                url += "/" + showId;
            }
            if (seasonNum > 0) {
                url += "/" + seasonNum;
            }
            
            Log.d(TAG, url);

            HttpPost httppostreq = new HttpPost(url);
            // UrlEncodedFormEntity entity = new
            // UrlEncodedFormEntity(nameValuePairs);
            // httppostreq.setEntity(entity);
            response = httpclient.execute(httppostreq);

            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();

                Log.d(TAG, "resp: " + responseString);

                Gson gson = new Gson();
                JsonParser parser = new JsonParser();
                JsonArray jArray = parser.parse(responseString).getAsJsonArray();

                for (JsonElement obj : jArray) {
                    Object elem = null;

                    if (type == SHOWS_LIST) {
                        elem = (Show) gson.fromJson(obj, Show.class);
                    } else if (type == SEASONS_LIST) {
                        elem = (Season) gson.fromJson(obj, Season.class);
                    } else {
                        elem = (Episode) gson.fromJson(obj, Episode.class);
                    }
                    array.add(elem);
                }

            } else {
                // Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
        }
        Remote remote = Remote.data(array);
        return remote;
    }
}
