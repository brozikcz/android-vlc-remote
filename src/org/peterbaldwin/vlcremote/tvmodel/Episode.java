
package org.peterbaldwin.vlcremote.tvmodel;

public class Episode {
    private int season;
    private int num;
    private String title = null;

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
}
