
package org.peterbaldwin.vlcremote.tvmodel;

public class Episode {
    private int id;
    
    private int season;
    private int num;
    private String title = null;
    private boolean watched;
    private boolean file;
    
    public int getNum() {
        return this.num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public int getSeason() {
        return this.season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean getWatched() {
        return this.watched;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
    }

    public boolean hasFile() {
        return file;
    }

    public void setFile(boolean file) {
        this.file = file;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
