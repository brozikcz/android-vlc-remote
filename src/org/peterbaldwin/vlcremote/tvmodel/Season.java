
package org.peterbaldwin.vlcremote.tvmodel;

import android.util.Log;

import java.util.List;

public class Season{
   	private int num;
   	private int progress;
   	private int total;

 	public int getNum(){
		return this.num;
	}
	public void setNum(int num){
		this.num = num;
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
	    Log.d("Show", "title: "+total);
		this.total = total;
	}
}
