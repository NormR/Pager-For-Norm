package com.normsstuff.pager4norm;

//import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import com.example.pager4norm.R;

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


public class MulticastServer extends Service {
	private static final int NOTIFY_ME_ID = 134;
	final String NotifyMsg = Pager4NormActivity.NotifyMsg;
	
	public final static int Port = Pager4NormActivity.UDP_Port; // 4321;
	public final static String GroupIP = Pager4NormActivity.UDP_GroupIP; //"230.0.0.0";



    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 'T' HH:mm:ss", Locale.US);
	String rootDir = Environment.getExternalStorageDirectory().getPath();
	final String LogFilePathPfx = rootDir + "/MulticastServer_log_";
	
	final String startTime = sdf.format(new java.util.Date());

	NotificationManager mgr = null;
	Context context;
	boolean running = true;
	
	private ResultReceiver resultReceiver;  // Connection to GUI for showing a message
	MulticastSocket socket;

	
	//---------------------------------------------------------------------------------
  @Override
  public void onCreate() {
      super.onCreate();
      System.out.println("MCS onCreate at " + startTime);
//      Toast.makeText(getBaseContext(), "MCS onCreate", Toast.LENGTH_SHORT).show();
  }
  
  //----------------------------------------------------------------------
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) { 
  	
  	if(intent != null) {
  		Bundle bndl = intent.getExtras();
  		System.out.println("MCS onStartCmd start bndl="+bndl);
  		
		resultReceiver = intent.getParcelableExtra("receiver");
 		
  		if(bndl != null) {
  			System.out.println(" >>> bndl contents="+ Arrays.toString(bndl.keySet().toArray()));
  			startInForeground();
  		}
  	}else {
  		System.out.println("MCS OnStartCmd intent=null, flags=" + flags);
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
	intent4P.putExtra(Pager4NormActivity.StartedByID, "MC Notify Icon");
	
	// This pending intent will open after notification click
	PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 0, intent4P, 
			                                     PendingIntent.FLAG_UPDATE_CURRENT);
	
	Notification note = new Notification.Builder(getBaseContext())
						.setSmallIcon(R.drawable.flying_robot_small)
						.setWhen(System.currentTimeMillis())
						.setTicker("Started MulticastServer!")
						.setContentTitle("P4N MulticastServer is running")
						.setContentText("Touch to Open app for control")
						.setContentIntent(pi)
						.build();
	// See number of notification arrived
	note.number = 2;
//		mgr.notify(NOTIFY_ME_ID, note);
	startForeground(android.os.Process.myPid(), note);   //<<< WHY doesn't this work ????
  	System.out.println("MCS started In Foreground at " + new Date());
  	
  	try{
  		receiveUDPMessage(GroupIP, Port);  //  Should this be on own thread????
  	}catch(Exception x){
  		x.printStackTrace();
  	}
  }
  
  //------------------------------------------------------------------------------
  public void receiveUDPMessage(final String ip, final int port) { // throws IOException {
  	
  	final Context context = getBaseContext();
  	
  	Thread t = new Thread(new Runnable() {
  		public void run() {
  		  try{
		        byte[] buffer=new byte[1024];
		        socket = new MulticastSocket(port);
		        InetAddress group = InetAddress.getByName(ip);
		        socket.joinGroup(group);
		
		        while(running){
		           System.out.println("MCS waiting for multicast message... at " + new Date());
		           DatagramPacket packet=new DatagramPacket(buffer, buffer.length);
		           socket.receive(packet);
		           String msg = new String(packet.getData(), packet.getOffset(),packet.getLength());
		           System.out.println("MCS message received>> "+msg +"< at " + new Date());
/*		          
		  	       Intent intent = new Intent(context, Pager4NormActivity.class);
		  	       intent.putExtra(Pager4NormActivity.MsgFmMulticast, msg);
		  	       intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);          //<< causes nesting ????
		  	       context.startActivity(intent);
*/		  	       
		  	   	   Bundle bundle = new Bundle();
		  	   	   bundle.putString("text", msg);
		  	   	   resultReceiver.send(100, bundle);

		           if("OK".equals(msg)) {
		              System.out.println("MCS No more message. Exiting : "+msg);
		              break;
		           }
		        } // end while(true)
		
		        socket.leaveGroup(group);
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
      MulticastServer getService() {
          return MulticastServer.this;     //<<<<<< This reference can be used to call our methods
      }
  }
  // This is the object that receives interactions from clients.  See
  // RemoteService for a more complete example.
  private final IBinder mBinder = new LocalBinder();

  
	@Override
	public IBinder onBind(Intent intent) {
		System.out.println("MCS onBind() intent="+intent);
      return mBinder;
	}
	
	public void stopRunning(){
		running = false;
		socket.close();
	}

	@Override
	public void onDestroy() {
	    System.out.println("MCS onDestroy() at " + sdf.format(new java.util.Date()));
	    stopRunning();
	    Toast.makeText(getBaseContext(), "MulticastServer onDestroy", Toast.LENGTH_SHORT).show();
	    if(mgr != null)
  		   mgr.cancel(NOTIFY_ME_ID);	
	}

}