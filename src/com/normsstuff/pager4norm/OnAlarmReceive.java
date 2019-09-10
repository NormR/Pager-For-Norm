package com.normsstuff.pager4norm;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.os.PowerManager;

public class OnAlarmReceive extends BroadcastReceiver {
	SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd 'T' HH:mm:ss", Locale.US); // showTime
	
	  @Override
	  public void onReceive(Context context, Intent intent) {
	 
		 String timeThisAlarm = "Not set"; 
	     Bundle bndl = intent.getExtras();
	     if(bndl != null){
	    	 timeThisAlarm = bndl.getString(Pager4NormActivity.TimeOfAlarmID); //  null???
	     }
	     System.out.println("OAR BroadcastReceiver, in onReceive at " + sdfTime.format(new Date())
   		                    +", timeThisAlarm=" + timeThisAlarm);
	     
	     // Acquire wakeup lock
	     PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
//	     wl_dim = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Dim Lock");
	     // Ensures that the CPU is running; the screen and keyboard backlight will be allowed to go off.
//	     PowerManager.WakeLock wl_dim = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
	     PowerManager.WakeLock wl_dim = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK 
                                                       | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
	     
	     wl_dim.acquire(10000L);  // Is 10 seconds long enough
	     System.out.println("OAR turned on PARTIAL_WAKE_LOCK at " 
	    		 			+ sdfTime.format(new java.util.Date()));

	     // Start the  next Activity or main
	     Intent intNA = new Intent(context, Pager4NormActivity.class);
	     intNA.putExtra(Pager4NormActivity.StartedByID, "OnAlarmReceive");
	     intNA.putExtra(Pager4NormActivity.TimeOfAlarmID, timeThisAlarm);
	     
	     intNA.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	     context.startActivity(intNA);
	  }

}