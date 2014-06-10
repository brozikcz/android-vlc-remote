
package org.peterbaldwin.vlcremote.tvmodel;

import android.util.Log;

import java.util.List;

public class Show{
   	private int id;
   	private String title;
   	private int progress;
   	private int total;

 	public int getId(){
		return this.id;
	}
	public void setId(int id){
		this.id = id;
	}
 	public String getTitle(){
		return this.title;
	}
	public void setTitle(String title){
		this.title = title;
	}
 	public int getProgress(){
		return this.progress;
	}
	public void setProgress(int progress){
		this.progress = progress;
	}
 	public int getTotal(){
		return this.total;
	}
	public void setTotal(int total){
		this.total = total;
	}
}
