package com.normsstuff.pager4norm;

import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import com.example.pager4norm.R;
import com.normsstuff.pager4norm.ServerSocketService.LocalBinder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.widget.Toast;

public class ServerSocketService extends Service {
	private static final int NOTIFY_ME_ID = 136;
	final String NotifyMsg = Pager4NormActivity.NotifyMsg;
	
	public final static int Port = Pager4NormActivity.PORT; // 4321;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 'T' HH:mm:ss", Locale.US);
    
	String rootDir = Environment.getExternalStorageDirectory().getPath();
	final String LogFilePathPfx = rootDir + "/ServerSocketService_log_";
	
	final String startTime = sdf.format(new java.util.Date());

	NotificationManager mgr = null;
	Context context;
	boolean running = true;
	
	private ResultReceiver resultReceiver;  // Connection to GUI for showing a message
	ServerSocket socket;

	
	//---------------------------------------------------------------------------------
  @Override
  public void onCreate() {
      super.onCreate();
      System.out.println("SSS onCreate at " + startTime);
//      Toast.makeText(getBaseContext(), "SSS onCreate", Toast.LENGTH_SHORT).show();
  }
  
  //----------------------------------------------------------------------
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) { 
  	
  	if(intent != null) {
  		Bundle bndl = intent.getExtras();
  		System.out.println("SSS onStartCmd start bndl="+bndl);
  		
		resultReceiver = intent.getParcelableExtra("receiver");
 		
  		if(bndl != null) {
  			System.out.println(" >>> bndl contents="+ Arrays.toString(bndl.keySet().toArray()));
  			startInForeground();
  		}
  	}else {
  		System.out.println("SSS OnStartCmd intent=null, flags=" + flags);
  	}
  	
  	return Service.START_STICKY;
  }


  //-----------------------------------------------------------------------
  private void startInForeground() {
	mgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

	// Build an intent with some data
	Intent intent4P = new Intent(getBaseContext(), Pager4NormActivity.class);  // Activity ???
	// How to pass this to NotifyMessage -> Need flag on PI!!!!
	intent4P.putExtra(NotifyMsg, sdf.format(new java.util.Date()));
	intent4P.putExtra(Pager4NormActivity.StartedByID, "SS Notify Icon");
	
	// This pending intent will open after notification click
	PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 0, intent4P, 
			                                     PendingIntent.FLAG_UPDATE_CURRENT);
	
	Notification note = new Notification.Builder(getBaseContext())
						.setSmallIcon(R.drawable.android_small)
						.setWhen(System.currentTimeMillis())
						.setTicker("Started ServerSocketService!")
						.setContentTitle("P4N ServerSocketService is running")
						.setContentText("Touch to Open app for control")
						.setContentIntent(pi)
						.build();
	// See number of notification arrived
	note.number = 2;
//		mgr.notify(NOTIFY_ME_ID, note);
	startForeground(android.os.Process.myPid(), note);   //<<< WHY doesn't this work ????
  	System.out.println("SSS started In Foreground at " + new Date());
  	
  	try{
  		receiveMessage(Port);  //  Should this be on own thread????
  	}catch(Exception x){
  		x.printStackTrace();
  	}
  }
  
  //------------------------------------------------------------------------------
  public void receiveMessage(final int port) { // throws IOException {
  	
//  	final Context context = getBaseContext();
  	
  	Thread t = new Thread(new Runnable() {
  		public void run() {
  		  try{
	        byte[] buf = new byte[1024];
	        socket =  new ServerSocket(port);
            System.out.println("SSS Server Socket opened on port= "+port +" at " + new Date()); //<<<<
	        
	        while(running){
	           System.out.println("SSS waiting for message... at " + new Date());
               Socket client = socket.accept();    //  Client has connected
               System.out.println("SSS Server connected with client at " + new Date());
               int len = 0;
               String msg = "";
               InputStream inputStream = client.getInputStream();
               while ((len = inputStream.read(buf)) != -1) {     //<<<<<<<<< Problem getting -1 ????
                   msg += new String(buf, 0, len);
               }
               
 	           System.out.println("SSS message received>> "+msg +"< at " + new Date());
               if(msg.length() > 500) {                       //<<<<<<<<< FOR TESTING <<<<<<<<<<
	               	msg = "Special replacement for long message (>500)\n";
              }
	           
	  	   	   Bundle bundle = new Bundle();
	  	   	   bundle.putString("text", msg);
	  	   	   resultReceiver.send(100, bundle);  // pass message back to ...
	  	   	   
	  	   	   //  Send a response ????

	           if("QUIT_LOOP".equals(msg)) {
	              System.out.println("SSS No more message. Exiting : "+msg);
	              break;
	           }
	        } // end while(running)
	
	        socket.close();
  		  }catch(Exception x){
  				x.printStackTrace();
  		  }
  		}
  	});
  	t.start();
   }
  

  /** =================================================================================
   * Class for clients to access.  Because we know this service always   ???? is this true?
   * runs in the same process as its clients, we don't need to deal with
   * IPC.
  */
  public class LocalBinder extends Binder {
	  ServerSocketService getService() {
          return ServerSocketService.this;     //<<<<<< This reference can be used to call our methods
      }
  }
  // This is the object that receives interactions from clients.  See
  // RemoteService for a more complete example.
  private final IBinder mBinder = new LocalBinder();

  
	@Override
	public IBinder onBind(Intent intent) {
		System.out.println("SSS onBind() intent="+intent);
      return mBinder;
	}
	
	//------------------------------------------------------
	public void stopRunning(){
		running = false;
		try{
		   socket.close();
		}catch(Exception x){
			x.printStackTrace();
		}
		stopSelf();
	}
	
	// returns true if changed, false if not
	public boolean setResultReceiver(ResultReceiver rr) {
		// check if resultReceiver has changed
		return resultReceiver != rr;
	}

	//---------------------------------------------------------
	@Override
	public void onDestroy() {
	    System.out.println("SSS onDestroy() at " + sdf.format(new java.util.Date()));
	    stopRunning();
	    Toast.makeText(getBaseContext(), "ServerSocketService onDestroy", Toast.LENGTH_SHORT).show();
	    if(mgr != null)
  		   mgr.cancel(NOTIFY_ME_ID);	
	}

}