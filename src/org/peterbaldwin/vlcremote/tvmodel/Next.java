
package org.peterbaldwin.vlcremote.tvmodel;

import java.util.List;

public class Next{
    private String episode_num;
    private String season_num;
    private String title;

    public String getEpisode_num(){
        return this.episode_num;
    }
    public void setEpisode_num(String episode_num){
        this.episode_num = episode_num;
    }
    public String getSeason_num(){
        return this.season_num;
    }
    public void setSeason_num(String season_num){
        this.season_num = season_num;
    }
    public String getTitle(){
        return this.title;
    }
    public void setTitle(String title){
        this.title = title;
    }
}
