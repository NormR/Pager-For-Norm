package com.normsstuff.pager4norm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.example.pager4norm.R;
import com.normstools.SaveStdOutput;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Pager4NormActivity extends Activity 
                                implements ShowEditListFragment.TimesListHandler  {
	final String Version = "Version date: Sept 9, 2019 @ 1300\n";

	//----------------------------------------------------------------------------
    final String rootDir = Environment.getExternalStorageDirectory().getPath();
    final String LogFilePath = rootDir + "/Pager4Norm_Log_"; //.txt";
//    private final String root = rootDir + "/Pager4Norm/"; 
    private final String logsFilesFolder = rootDir + "/Norms/logs";   // where log files are written
   
    Thread.UncaughtExceptionHandler lastUEH = null;
	final String ExcpFilePathPfx = logsFilesFolder + "/Pager4Normlog_";
	
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss", Locale.US);
    SimpleDateFormat sdfHHMM = new SimpleDateFormat("HH:mm", Locale.US);
    SimpleDateFormat sdfTime = new SimpleDateFormat("MM-dd 'T' HH:mm:ss", Locale.US); // to show Time
    SimpleDateFormat sdf4Time = new SimpleDateFormat("MMM dd HH:mm", Locale.US);

	//-------------------------------------------------------------------------
	// Define class to hold hour and minute values and build a display String
    static class HHMM implements Parcelable {
		int hour;
		int minute;
		
		public HHMM(String hhmm) {
			String[] split = hhmm.split(":");
			int hh = Integer.parseInt(split[0]);
			if(hh < 0 || hh > 23)
				throw new IllegalArgumentException("Invalid hour:" + hh);
			int mm = Integer.parseInt(split[1]);
			if(mm < 0 || mm > 59)
				throw new IllegalArgumentException("Invalid minute:"+mm);

			hour = hh;
			minute = mm;
		}

		public HHMM(int hh, int mm){
  			if(hh < 0 || hh > 23)
				throw new IllegalArgumentException("Invalid hour:" + hh);
			if(mm < 0 || mm > 59)
				throw new IllegalArgumentException("Invalid minute:"+mm);

			hour = hh;
			minute = mm;
		}
		
		public boolean sameTime(HHMM other) {
			return other.hour == hour && other.minute == minute;
		}

      //  Build a Calendar for this time today
      public Calendar getTimeToday() {
         Calendar cal = Calendar.getInstance();
         cal.set(Calendar.SECOND, 0);            //<<<<<< Only hours and minutes ???
         cal.set(Calendar.HOUR_OF_DAY, hour);
         cal.set(Calendar.MINUTE, minute);
         return cal;
      }
		
      //  Return hh:mm with 0 padding
		public String toString() {
			return new StringBuilder().append(padding_str(hour))
                                   .append(":")
                                   .append(padding_str(minute))
                                   .toString();
		}
		private  String padding_str(int c) {
			if (c >= 10)
			   return String.valueOf(c);
			else
			   return "0" + String.valueOf(c);
		}

		//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
		// Define methods for Parcelable

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel out, int arg1) {
			out.writeInt(hour);
			out.writeInt(minute);
		}
	    public static final Parcelable.Creator<HHMM> CREATOR
                = new Parcelable.Creator<HHMM>() 
            {
			     public HHMM createFromParcel(Parcel in) {
			         return new HHMM(in);
			     }
			
			     public HHMM[] newArray(int size) {
			         return new HHMM[size];
			     }
	         };
 
		 private HHMM(Parcel in) {
		     hour = in.readInt();
		     minute = in.readInt();
		 }
	}  // end class HHMM  ---------------------------------

    //---------------------------------------------------------------
	final static public String NotifyMsg = "Notify at";
    final String TimesFilename = "P4N_times.txt";
    final String TimeNextAlarmS = "TimeNextAlarm";
    final String RunningAlarmS = "P4NRunAlarm";
    final String NoAlarmSet = "Not set";
    final static public String StartedByID = "StartedBy";
    final static public String TimeOfAlarmID = "timeOfAlarm";
	private String timeNextAlarm = NoAlarmSet;  // set by setupAlarm 
	private boolean runningAlarm = false;
    PendingIntent alarmPI;

	private List<HHMM> times = new ArrayList<>();  // save the times to check the level
	public  List<HHMM> getTimes() {
		return times;
	}
	private HHMM nextTime = new HHMM(10,0);        //<<<<<
	private HHMM lastTime = new HHMM(0,0);

    
    //- - - - - - - - - - - - - - - - - - - - - - -
    // Define inner class to handle exceptions
    class MyExceptionHandler implements Thread.UncaughtExceptionHandler {
        public void uncaughtException(Thread t, Throwable e){
           java.util.Date dt =  new java.util.Date();
           String fn = ExcpFilePathPfx + "exception_" + sdf.format(dt) + ".txt";
           try{ 
              PrintStream ps = new PrintStream( fn );
              e.printStackTrace(ps);
              ps.close();
              System.out.println("P4N wrote trace to " + fn);
              e.printStackTrace(); // capture here also???
              SaveStdOutput.stop(); // close here vs calling flush() in class 
           }catch(Exception x){
              x.printStackTrace();
           }
           lastUEH.uncaughtException(t, e); // call last one  Gives: "Unfortunately ... stopped" message
           return;    //???? what to do here
        }
    }

    boolean debug = true;           // control debug output
    boolean startedSaveStd = false;
    boolean runningMCServer = false;  // remember if we are running the multicast server
    boolean runningSSService = false;  // for ServerSocket service

    
    public static int PORT = 8081;         // ServerSocket uses this
    public static int UDP_Port = 4321;     // for multicast
    public static String UDP_GroupIP = "230.0.0.0";
    final static int TimeOut = 10000;
    
    private String othersIP = null;
    private final String OthersIP_FN = "othersIP.txt";    // filename of saved other's IP

   //------------------------------------------------------------------------------
    //  Define a class so AlertDialog can be used by 2 different sending methods
    abstract class GetSend {
		String title;
		GetSend(String t) {
			title = t;
		}
		public abstract void sendMsg(String msg, boolean flag);
	}
	
    // Instance for each way to send message: UDP or Socket
	GetSend getSendUDP =  new GetSend("Get UDP Multicast message") {
		@Override
		public void sendMsg(String msg, boolean flag) {
			sendUDPMessage(msg, UDP_GroupIP, UDP_Port);
		}
	};
	GetSend getSendSocket = new GetSend("Get socket to send by message"){
		@Override
		public void sendMsg(String msg, boolean flag) {
	   		sendSocketMessage(msg, othersIP, PORT, flag);
		}
	};
    
	//-----------------------------------------

	// Request codes for startActivity
	final int FolderChosenForSave = 112;
    final int RESULT_SETTINGS = 31;
	final int DELAYED_MESSAGE = 321;

    private final String MessagesText_K = "messagesText";
    private final String RunningMCServer_K = "runningMCServer";
    private final String RunningSSService_K = "runningSS";

	private final int DisabledBtnColor = Color.GRAY;
	private final int StartBtnColor = Color.GREEN;
	private final int StopBtnColor = Color.RED;
	
	final public static String MsgFmMulticast = "MC_MSG";
	final public static String Command = "Command";
	final public static String CmdStrtFG = "StrtFG";
	
	TextView messages;
	GetMessageAsyncTask getMessageTask = null;
	private String savedMessages = "";          //saved/restored via preferences

	
    // For Speaking the received message
    private TextToSpeech tts1;
    final String UtteranceId = "The message";
    private boolean ttsReady = false;
    private boolean sayTheMessagePrf = true;  // control TTS
    private final String ForceSayMessage = "+";
    private final String NeverSayMessage = "-";
    // Conflict between the next two flags ???
    private boolean useWakeupPrf = false;     // controls using alarms to restart with wakeup
    private boolean repeatedWakeups = false;  // for repeated wakeup calls Set/cleared when menu item   ????


    //---------------------------------------------------------
    //  Utility classes to allow Service to send message to us
	class UpdateUI implements Runnable
	{
		String updateString;
		
		public UpdateUI(String updateString) {
			this.updateString = updateString;
		}
		public void run() {
			boolean sayForThis = false;
			boolean neverSay = false;
			if(updateString.startsWith(ForceSayMessage)) {
				sayForThis = true;
				updateString = updateString.substring(1); // strip off +
			}else if(updateString.startsWith(NeverSayMessage)) {
				updateString = updateString.substring(1); // strip off -
				neverSay = true;
			}
			//  Append new to old
			messages.append("You("+ sdfHHMM.format(new Date()) + "): " + updateString + "\n");
			if(!neverSay && (sayTheMessagePrf || sayForThis)){
			   sayTheMessage(updateString);	
			}
		}
	}

	class MyResultReceiver extends ResultReceiver
	{
		public MyResultReceiver(Handler handler) {
			super(handler);
		}

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
//			System.out.println("oRR resCode="+resultCode +", bndl="+resultData); //<<<<<<<<
			if(resultCode == 100){
				runOnUiThread(new UpdateUI(resultData.getString("text")));
			}
			else if(resultCode == 200){
				runOnUiThread(new UpdateUI(resultData.getString("end")));
			}
			else{
				runOnUiThread(new UpdateUI("Result Received "+resultCode));
			}
		}
	}  // end class
	
	private Handler handler = new Handler();
	private MyResultReceiver resultReceiver;

    String versionName;
    private int retryCount = 0;


    //---------------------------------------------------------------------------------------------
    //---------------------------------------------------------------------------------------------
    //---------------------------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pager4_norm);
		
    	try {
    	    PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
    	    versionName = pInfo.versionName;
    	} catch (PackageManager.NameNotFoundException e) {
    	    e.printStackTrace();
    	}

    	// What services are running?
		runningMCServer = isMyServiceRunning(MulticastServer.class);
		runningSSService = isMyServiceRunning(ServerSocketService.class);

		getPreferences();
		readOthersIP();
		setButtonStates();
		
	    messages = (TextView) findViewById(R.id.messages);
	    messages.setText(Version + " MC service running? " + runningMCServer
		                 + ", SS service running? " + runningSSService + "\n");
	    if(othersIP != null)
	    	messages.append("Other's IP="+othersIP +"\n");
	    
	    if(!savedMessages.isEmpty()){
//	    	messages.append(savedMessages);  //<<<<<<<< Need more logic for this
	    	savedMessages = "";
	    }
		
        if(debug && !startedSaveStd) {
	        try {
	            java.util.Date dt =  new java.util.Date();
				SaveStdOutput.start(LogFilePath + sdf.format(dt) + ".txt");
				System.out.println("P4N Started logging at " + sdfTime.format(dt)
						          + " " +Version);
				startedSaveStd = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        // Catch exceptions and write to a separate file
        lastUEH = Thread.getDefaultUncaughtExceptionHandler(); // save previous one
        Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler());
        
        loadTimesFile();  // get the wakeup times list
        
		// Test if restarted
		if(savedInstanceState != null){
			//  Restore old contents
			messages.setText(savedInstanceState.getString(MessagesText_K));
			boolean savedRunningService = savedInstanceState.getBoolean(RunningMCServer_K);
			boolean savedRunningSSService = savedInstanceState.getBoolean(RunningSSService_K);
			System.out.println("P4N + savedRunningService="+savedRunningService 
					            + " savedRunningSSService=" + savedRunningSSService);
		}
		
		
        //  What was passed to us at startup?
      	Intent intent = getIntent();
      	
    	System.out.println("P4N onCreate() intent="+intent + "\n >>data=" + intent.getData()
    			+ "\n savedInstanceState=" + savedInstanceState
    			+ "\n extras="+ intent.getExtras()
    			+ "\n rootDir="+rootDir + ", MC service running? "
    			+  runningMCServer + ", SS service running? " + runningSSService 
        		+ " IP=" + getLocalIpAddress() + " at " + sdfTime.format(new Date()) );

		
        // Were we started by an Intent?
		Bundle bndl = intent.getExtras();
		if(bndl != null) {
    		Set<String> set = bndl.keySet();
    		System.out.println(" >>>bndl keySet=" + Arrays.toString(set.toArray()));
    		String startedBy = bndl.getString(StartedByID); //  were we started by another program
    		String timeOfAlarm = bndl.getString(TimeOfAlarmID);
    		String notifyMsg = bndl.getString(NotifyMsg);
//    		timeThisAlarm = timeOfAlarm; // Save in class
    		String profile = bndl.getString("profile");   // ??? what is this
    		System.out.println("P4N startedBy=" + startedBy +", timeOfAlarm="+timeOfAlarm
    				            + ", notify msg=" + notifyMsg + ", profile=" + profile
    				            + " at "+ sdfTime.format(new Date()));
    		if(startedBy != null) {
    			messages.append(">> restarted by "+startedBy + "\n");
    			// What to do here???
    			if(repeatedWakeups)  { // Start another wakeup
    			   findSetNextAlarm();
    		    }
	    		//  We're done, exit soon
	    		int timeUntilFinish = 5000;   //  how long to leave info window up
    	        Runnable runnable = new Runnable() {
    	            public void run() {
    	            	System.out.println("P4N runnable calling finish() at " 
    	            					   + new java.util.Date());
//    	   	   			finish();      // Done ????  <<<<<<<<<<< NOTE
    	            }
    	        };
    	        new Handler().postDelayed(runnable, timeUntilFinish);
    	        return;   // exit we're done now

    		}  // end have StartedBy flag
    		
 		}  // end have bundle -> started by special Intent
		
	
	} // end onCreate()
	
	private void setButtonStates() {
   		//  Reestablish button settings from flags
		Button sendBtn = (Button)findViewById(R.id.sendBtn);
		if(runningMCServer) {
			//  Set button to show started
			Button btn = (Button)findViewById(R.id.multicastBtn);
			btn.setBackgroundColor(StopBtnColor);
			btn.setText("Stop Multicast");
			sendBtn.setEnabled(true);
	   	}
		if(runningSSService) {
			//  Set button to show started
			Button btn = (Button)findViewById(R.id.serverSocketBtn);
	    	btn.setBackgroundColor(StopBtnColor);
            btn.setText("Stop ServerSocket");
			sendBtn.setEnabled(true);
		}
	}
	
	//------------------------------------------
	private void loadTimesFile() {
		try{
			FileInputStream fis = openFileInput(TimesFilename);
			byte[] bfr = new byte[1000];    // should never be this long
			int nbrRd = fis.read(bfr);
			String listS = new String(bfr, 0, nbrRd);
			System.out.println("P4N lTF  times=" + listS);
			fis.close();
		    //<<< split on ", " from toString()
		    String[] theTimes = listS.split(", "); 

		     //  Clear and rebuild list
		     times.clear();
		     for(String hhmmS : theTimes) {
		        times.add(new HHMM(hhmmS));          // validates array values
		     }
		     System.out.println("P4N loaded times size="+times.size());
		}catch(FileNotFoundException fnf) {
			fnf.printStackTrace();
		    loadSomeTimes();  // first time -> prime the pump
		    saveTimesList();
		}catch(Exception x) {
			x.printStackTrace();
		}

	}
	//--------------------------------------------------------------------------
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
	        final String action = intent.getAction();
	        System.out.println("P4N bcR action="+action);
	        if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
	        {
	            if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false))
	                {
	                // wifi is enabled
	            	System.out.println("P4N bcR wifi enabled");
	                }
	            else
	                {
	                // wifi is disabled
	              	System.out.println("P4N bcR wifi disabled");
	              	}
	        }
        }
    };

	
	//===============================
	@Override
	protected void onResume() {
	    super.onResume();
		System.out.println("P4N onResume() at " + sdfTime.format(new Date())); 
	    TextView textIpaddr = (TextView) findViewById(R.id.ipaddr);
	    
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        registerReceiver(broadcastReceiver, intentFilter);  
        
	    WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
	    int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
	    if(ipAddress == 0) {
//	    	showMsg("IP address not found - Need to connect to internet");
        	startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));   // Go allow user to connect

	    	// Exit the program after a short delay
	        Handler handler = new Handler() {
	            @Override
	            public void handleMessage(Message msg) {
	                if(msg.what == DELAYED_MESSAGE) {
	                    Pager4NormActivity.this.finish();      // <<<< NOTE
	                }
	                super.handleMessage(msg);
	            }
	        };
	        Message message = handler.obtainMessage(DELAYED_MESSAGE);
//	        handler.sendMessageDelayed(message, 5000);      // Does this prevent Notify???
	        //  Problem with following - no way to get back
	        System.out.println("P4N Should we send user to Settings? retryCount="+retryCount );
	        retryCount++;
	    	return;             //????
	    }
	    
	    final String formatedIpAddress = String.format(Locale.US, "%d.%d.%d.%d", (ipAddress & 0xff), 
	    		                                      (ipAddress >> 8 & 0xff),
	                                                  (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
	    textIpaddr.setText("http://" + formatedIpAddress + ":" + PORT
	    		            + "    " + getLocalIpAddress()); //<<<<<<< two lines ???
	    
		//  What else show we do here to get ready???
		// do we want to try to stay alert?
		if(repeatedWakeups){
			findSetNextAlarm();
			runningAlarm = true;  
		}
	    
	}  // end onResume()
	
	//-----------------------------------------
	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		System.out.println("P4N onNewIntent() intent="+intent
				           + "\n  at " + sdfTime.format(new Date())
                           + "  StartedBy="+intent.getStringExtra(StartedByID));
    	// What services are running?
		runningMCServer = isMyServiceRunning(MulticastServer.class);
		runningSSService = isMyServiceRunning(ServerSocketService.class);
		setButtonStates();
		
		String startedBy = "unknown";
		Bundle bndl = intent.getExtras();
		if(bndl != null) {
    		Set<String> set = bndl.keySet();
    		System.out.println(" >>bndl keySet=" + Arrays.toString(set.toArray()));
    		startedBy = bndl.getString(StartedByID);
    		String notifyMsg = bndl.getString(NotifyMsg);
    		System.out.println("P4N oNI startedBy="+startedBy +", notifyMSg="+notifyMsg
    				+ ", MCS running="+runningMCServer +", SS Service running="+runningSSService);
    		if(startedBy != null && startedBy.indexOf("Alarm") >= 0) {
    			findSetNextAlarm();
    			// Does this hold screen on???
//    			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // ????
    		}
		}  // end have bundle
		
		getPreferences();
			
        if(messages != null) {
        	messages.append(">> StartedBy="+intent.getStringExtra(StartedByID)
        			         + " at " + sdfTime.format(new Date()) + "\n");
        	if(!savedMessages.isEmpty()) {
//        		messages.append(savedMessages);  //  Need more logic to prevent multiple appends
        		savedMessages = "";
        	}
        }else{
        	System.out.println("P4N ??? messages is null");
        }
//        sayTheMessage("I have been awakened in onNewIntent");
        
        //  Are we connected?
        WifiManager wifi =(WifiManager)getSystemService(Context.WIFI_SERVICE); 
        if(wifi.isWifiEnabled()) {
            //This should be OK?
        }else {
            //Otherwise can we connect here ???
        	wifi.setWifiEnabled(true);
        	System.out.println("P4N oNI attempted to enable wifi");
        }
        // Now what???
        
	} // end onNewIntent()
	
   	//--------------------------------------------------------------
	//  Save values to allow us to restore state when rotated
	@Override
	public void onSaveInstanceState(Bundle bndl) {
		super.onSaveInstanceState(bndl);
    	System.out.println("*** P4N onSaveInstanceState() at " + sdfTime.format(new Date()));
    	
		bndl.putBoolean(RunningMCServer_K, runningMCServer);
		bndl.putBoolean(RunningSSService_K, runningSSService);
		bndl.putString(MessagesText_K, messages.getText().toString());
   	}
	
	@Override
	protected void onRestoreInstanceState (Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		System.out.println("P4N onRestoreInstanceState bnld="+savedInstanceState
				             + " at " + sdfTime.format(new Date()));
    }
	
	//----------------------------------------------------------------
	@Override
	public void onPause() {
		super.onPause();
		System.out.println("P4N onPause() at "+ sdfTime.format(new Date()));
        unregisterReceiver(broadcastReceiver);
	}

	
	//----------------------------------------------------------------
	@Override
	public boolean onPrepareOptionsMenu(Menu m) {
		super.onPrepareOptionsMenu(m);
		//  Disable if no data
		MenuItem mi = (MenuItem)m.findItem(R.id.clear_messages);
		mi.setEnabled(messages.getText().length() > 0);
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.pager4_norm, menu);
		return true;
	}
	
	//-----------------------------------------------------------------
	private void loadSomeTimes() {
		// Load some times for testing
		times.add(new HHMM(10,0));
		times.add(new HHMM(11,0));
		times.add(new HHMM(11,30));
		times.add(new HHMM(12,0));  //<<<<<<<<< xtra for testing
		times.add(new HHMM(13,0));  //<<<<<<<<< xtra for testing
		times.add(new HHMM(14,0));  //<<<<<<<<< xtra for testing
		times.add(new HHMM(15,0));
		times.add(new HHMM(15,30)); //<<<<<<<<< xtra for testing
		times.add(new HHMM(16,0));  //<<<<<<<<< xtra for testing
		times.add(new HHMM(16,30)); //<<<<<<<<< xtra for testing
	}
	
	//  Define methods for TimesListHandler
	public void saveTimesList() {
		try{
			FileOutputStream fos =  openFileOutput(TimesFilename, MODE_PRIVATE); 
		    String listS = times.toString();
		    fos.write(listS.substring(1,listS.length()-1).getBytes());
		    fos.close();
		    System.out.println("P4N wrote times to " + getFilesDir());
		}catch(Exception x){
			x.printStackTrace();
		}
	}
	public List<HHMM> getTimesList() {
		return times;
	}
	public void setTimesList(List<HHMM> list) {
		times = list;
	}
	//=============================================================
   	// Check if date/time in cal is before the current time
   	public boolean isBeforeNow(Calendar cal){
           Calendar now = Calendar.getInstance();
           return (now.compareTo(cal) > 0);
   	}
   	public boolean isAfterNow(Calendar cal){
           Calendar now = Calendar.getInstance();
           return (now.compareTo(cal) < 0);
   	}

	
	//-----------------------------------------------------------------
	// Search times list for next time to use, compute seconds
	// and call setupAlarm to set an alarm after that time
   	//  How to skip setting alarm for a second time?
	private void findSetNextAlarm() {
		// find time for next check
	     nextTime = findNextTime(); 
	     if(nextTime.sameTime(lastTime)) {
	    	 System.out.println("P4N fSNA next alarm time same as last="+lastTime);
	    	 return;           // Skip if same time
	     }
	     
	     //  What if tomorrow  ?  >>>>>> SKIP
	     Calendar nextEventCal = nextTime.getTimeToday();
	     if(isBeforeNow(nextEventCal)){
	    	 System.out.println("P4N fSNA Next event time is tomorrow - Ignoring "+nextTime);
	    	 return;
//	       	  nextEventCal.add(Calendar.DAY_OF_MONTH, 1);  // move to tomorrow  
	     }
	     lastTime = nextTime;   // save for future check
	     
	      // Now set alarm for what was found
	      Calendar cal = Calendar.getInstance();
	      long nowInMillis = cal.getTimeInMillis();          // current time
	      long timeOfEvent = nextEventCal.getTimeInMillis();
	      int duration = (int)((timeOfEvent - nowInMillis) / 1000);
	      System.out.println("P4N Next event at: "+nextEventCal.getTime() 
	    		                      +" in " + duration);
	      setupAlarm(duration);
	      setPreferences();
	}
	//------------------------------------------------------
	//  Search times list for next time 
	public HHMM findNextTime() {
	      // Find next time
	      //  Assume times are in order
	      for(HHMM hhmm : times) {
	         if(isAfterNow(hhmm.getTimeToday())) {
	            return hhmm;           // this one is in the future
	         }
	      }
	      //  If not found, then use first time  which is tomorrow
	      return times.get(0);
	}

    /** ------------------------------------------------------------------------------
    * Sets up the alarm
    *
    * @param seconds
    *            - after how many seconds from now the alarm should go off
    */
    private void setupAlarm(int seconds) {
      AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
      Intent intent = new Intent(getBaseContext(), OnAlarmReceive.class);
      
       // Getting current time and add the seconds in it
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, seconds);
      timeNextAlarm = sdf4Time.format(cal.getTimeInMillis());        // Feb 02 13:59

      intent.putExtra(TimeOfAlarmID, timeNextAlarm);  // pass the time
      
      alarmPI = PendingIntent.getBroadcast(
              Pager4NormActivity.this, 0, intent,
              PendingIntent.FLAG_UPDATE_CURRENT);

      alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), alarmPI);
      
      String theMsg = "P4N Set the alarm for " + seconds + " seconds at "
                       + sdfTime.format(new Date()) + " alarm at " + timeNextAlarm;
      System.out.println(theMsg);
      messages.append(theMsg+"\n");

      Toast.makeText(getApplicationContext(), "Set alarm in "+ seconds + " for " + cal.getTime(), 
    		             Toast.LENGTH_LONG).show();
    } // end setupAlarm()

    
	private void setPreferences() {
		SharedPreferences.Editor editor = PreferenceManager
			                              .getDefaultSharedPreferences(this).edit();
		editor.putBoolean(RunningAlarmS, runningAlarm);
		editor.putString(TimeNextAlarmS, timeNextAlarm);
		editor.putString(MessagesText_K, messages.getText().toString());
		editor.commit();
	}


	//---------------------------------------------------------------------
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {

    	case R.id.get_ip_address:
    		// Get other's IP address from user
    		
        	AlertDialog.Builder alert = new AlertDialog.Builder(this);
        	alert.setTitle("Get other's IP address");
        	alert.setMessage("Enter other's IP addess.");

        	// Set an EditText view to get user input 
        	final EditText input = new EditText(this);
        	if(othersIP != null)
        	   input.setText(othersIP);  // show current value
        	alert.setView(input);

        	alert.setPositiveButton("Save IP address", new DialogInterface.OnClickListener() {
	        	public void onClick(DialogInterface dialog, int whichButton) {
	        	  String theIP = input.getText().toString();
	        	  if(theIP == null || theIP.length() == 0)
	        		  return;		// exit if user quit
	        	  
	        	  //  Add code to verify and test IP address ?
	        	  if(theIP.indexOf(":") > 0) {
	  				    runOnUiThread(new Runnable() {
	    				    @Override
	    				    public void run() {
	    		    			showMsg("Invalid IP address. Should not contain :port ");
	    				    }
	    				});                   
	        		  
	        		  return;
	        	  }
	        	  
	        	  othersIP = theIP;  // set what we are using
	        	  messages.append("Set Other's IP to "+ othersIP + "\n");
	        	  	
	    		  // Save name for next time???
	    		  try{
    				FileOutputStream fos = openFileOutput(OthersIP_FN, Context.MODE_PRIVATE);
    				fos.write(theIP.getBytes());
    				fos.close();
	    		  }catch(Exception x){
	    			  x.printStackTrace();
	    		  }
	        	}
        	});
   

        	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        	  public void onClick(DialogInterface dialog, int whichButton) {
        	    // Canceled.
        	  }
        	});

        	alert.show();
    		return true;
    		
    	case R.id.send_multicast_msg:
    		getAndSendMessage(getSendUDP, false);
    		return true;
    		
    	case R.id.send_socket_msg:
     		getAndSendMessage(getSendSocket, true);
    		System.out.println("P4N sent message to " + othersIP
    				           + " at " + sdfTime.format(new Date()));
    		return true;
    		
    	case R.id.clear_messages:
    		messages.setText(Version);
    		return true;
    		
    	case R.id.set_wu_times:
	        ShowEditListFragment showELF = new ShowEditListFragment(this);
//	        showELF.setTimesList(times);  // pass List
	        showELF.setTimesListHandler(this);
	
	        System.out.println("P4N Creating fragment for edit this="+this);

	        FragmentTransaction ft3 = getFragmentManager().beginTransaction();
	        showELF.show(ft3, "dialog");
    		
    		return true;
    		
    	case R.id.start_repeated_wakeup:
    		// Make sure receiver running first
    		if(!runningMCServer && !runningSSService) {
    			showMsg("Start a message receiver before starting alarm");
    			return true;
    		}
	    	if(item.isChecked()){
	    		// If currently checked - turn off
	    		item.setChecked(false);
	    		System.out.println("P4N turned off repeated wakeup calls");
	    		repeatedWakeups = false;
	    	}else{
	    		item.setChecked(true);
	    		System.out.println("P4N turned on repeated wakeup calls");
	    		repeatedWakeups = true;
 			    findSetNextAlarm();
 		    }
    		return true;
    		
    	case R.id.set_test_alarm:
    		setupAlarm(5*60);          //  How many seconds for testing???
    		return true;
    	
		case R.id.action_settings:
			// Starts the Settings activity on top of the current activity
			Intent intent2 = new Intent(this, SettingsActivity.class);
			startActivityForResult(intent2, RESULT_SETTINGS);
        	return true;
        	
		case R.id.help_topics:
			showMsg("Start a message receiver with one of Start buttons\n"
					+ "Enter message to send and press Send button\n"
					+ "Start message with + to always speak the message\n"
					+ "Start message with - to never speak the message");
			return true;
			
	    case R.id.About_ID:
	        showMsg("Pager For Norm program   " + versionName + "\n"
	        		+ Version 
	        		+ "email: radder@hotmail.com");
	        return true;
	
	    	
	    case R.id.Exit_ID:
	    	if(alarmPI != null) {
	    	    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
	    		alarmManager.cancel(alarmPI);
	    		alarmPI.cancel();  
	    		alarmPI = null;
	    	}
	    	if(runningMCServer){
		    	Intent intentMC2 = new Intent(this, MulticastServer.class);
		    	stopService(intentMC2);         //  Should this be in a method???
	    	}
	    	if(runningSSService){
	    		Intent intentSS = new Intent(this, ServerSocketService.class);
	    		stopService(intentSS);
	    	}
	    	finish();
	    	return true;
	    	
        default:
            System.err.println(">>> P4N unkn menuitem="+item.getItemId());
            break;
	    	
        }
		return super.onOptionsItemSelected(item);
	} // end onOptionsItemSelected()
	
	//-----------------------------------------
	//  Handle what selected activity found
	@Override
	public void onActivityResult(int reqCode, int resCode, Intent data) {
		System.out.println("P4N onActivityResult reqCode="+reqCode 
				            + ", resCode="+resCode+", intent="+data);

	   	String filePath = null;
		if(resCode == RESULT_OK) {
			if(data.getData() != null) {
				filePath = data.getData().getPath();         // Here is the standard place for response
				System.out.println("P4N onActRes filePath="+filePath+"<");
			}
		}else {
			System.out.println("P4N onActRes unknown resCode="+resCode +" vs OK="+RESULT_OK);
//			return;      //   skip rest????
		}
		
		//  Process reqCode
		if(reqCode == RESULT_SETTINGS) {
			getPreferences();
			
		}else{
			  System.out.println("P4N >>> Unknown reqCode="+reqCode);
		}
	}  // end onActivityResult
	
	
	
	//-----------------------------------------------------------------------------
	private void getAndSendMessage(final GetSend gs, final boolean flag) {
    	AlertDialog.Builder alert = new AlertDialog.Builder(Pager4NormActivity.this);
    	alert.setTitle(gs.title);        //"Get UDP Multicast message");
    	alert.setMessage("Enter message");

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(Pager4NormActivity.this);
    	alert.setView(input);

    	alert.setPositiveButton("Send message", new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int whichButton) {
        	   String message = input.getText().toString().trim();
//         	   sendUDPMessage(message, UDP_GroupIP, UDP_Port);
         	   gs.sendMsg(message, flag);
        	   System.out.println("P4N gASM Sent message="+ message +" at " + sdfTime.format(new Date()));
           	}
        });

    	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    	   public void onClick(DialogInterface dialog, int whichButton) {
    	     // Canceled.
    	   }
    	});

    	alert.show();

	}
	
	//===============================================================
	// Call when user wants to start listening for multicast
	public void multicastBtnClicked(View v) {
		Button btn = (Button)findViewById(R.id.multicastBtn);
		Button sendBtn = (Button)findViewById(R.id.sendBtn);
		if(btn.getText().toString().startsWith("Stop")) {
	    	Intent intentMC2 = new Intent(this, MulticastServer.class);
	    	stopService(intentMC2);
	    	System.out.println("P4N Stopped MCS at " + sdf.format(new java.util.Date()));
			btn.setBackgroundColor(StartBtnColor);
			btn.setText(R.string.start_multicast);
			runningMCServer = false;
            sendBtn.setEnabled(false);
		}else{
			// Setup and start service
			btn.setBackgroundColor(StopBtnColor);
			btn.setText("Stop Multicast");
	    	Intent intentMC = new Intent(this, MulticastServer.class);
	    	intentMC.putExtra("Info", "Started");
	    	intentMC.putExtra(Command, CmdStrtFG);  // Try here?? No working separately
	    	// Setup for connecting
			resultReceiver = new MyResultReceiver(handler);
			intentMC.putExtra("receiver", resultReceiver);
	    	startService(intentMC);
	    	runningMCServer = true;
            sendBtn.setEnabled(true);
	    	System.out.println("P4N started MulticastServer at " + sdf.format(new java.util.Date()));
		}
	}
	
	// Called to start ServerSocket
	public void serverSocketBtnClicked(View v){
		Button btn = (Button)findViewById(R.id.serverSocketBtn);
		Button sendBtn = (Button)findViewById(R.id.sendBtn);
		if(btn.getText().toString().startsWith("Stop")) {
	    	btn.setBackgroundColor(StartBtnColor);
	    	Intent intentMC2 = new Intent(this, ServerSocketService.class);
	    	stopService(intentMC2);
	    	runningSSService = false;
			btn.setText(R.string.start_serversocket);
            sendBtn.setEnabled(false);
		}else {
			// Setup and start service
	    	Intent intentMC = new Intent(this, ServerSocketService.class);
	    	intentMC.putExtra("Info", "Started");
	    	intentMC.putExtra(Command, CmdStrtFG);  // Try here?? No working separately
	    	// Setup for connecting
			resultReceiver = new MyResultReceiver(handler);  //???? Can this be a new one ???
			intentMC.putExtra("receiver", resultReceiver);
	    	startService(intentMC);
	    	runningSSService = true;
	    	btn.setBackgroundColor(StopBtnColor);
            btn.setText("Stop ServerSocket");
            sendBtn.setEnabled(true);
	    	System.out.println("P4N started ServerSocketService at " 
                               + sdfTime.format(new java.util.Date()));
		}
	}
	//---------------------------------------------------------------
	public void sendBtnClicked(View v) {
		TextView tv = (TextView) findViewById(R.id.txtChatLine);
		String msg = tv.getText().toString();
		if(msg.isEmpty())
			return;           // skip empty message
		// Where do we send it - choose what we're listening in
		if(runningSSService) {
	   		sendSocketMessage(msg, othersIP, PORT, false);
			
		}else if(runningMCServer){
			sendUDPMessage(msg, UDP_GroupIP, UDP_Port);
			
		}else{
			showMsg("Don't know where to send messaage. Start a server");
			return;
		}
		messages.append("Me(" + sdfHHMM.format(new Date()) + "): "+ msg + "\n");
		tv.setText("");                        //  clear
	}
	
    //----------------------------------------------------------------------------------
    private void getPreferences() {
 		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String debugKey = getResources().getString(R.string.set_debug_text_key);
		debug = preferences.getBoolean(debugKey, true);
        String sayMsgKey = getResources().getString(R.string.say_received_messages);
        sayTheMessagePrf = preferences.getBoolean(sayMsgKey, true);
        String wuKey = getResources().getString(R.string.use_wakeup);
        useWakeupPrf = preferences.getBoolean(wuKey, false);
        savedMessages = preferences.getString(MessagesText_K, "");
        System.out.println("P4N loaded preferences at " + sdfTime.format(new Date()));
    }

	
	//-------------------------------------------------------------------------------------------
	private void sendUDPMessage(final String message, final String ipAddress, final int port) {
//			      throws IOException {
	   	Thread t = new Thread(new Runnable() {
    		public void run() {
    		  try{
			      DatagramSocket socket = new DatagramSocket();
			      InetAddress group = InetAddress.getByName(ipAddress);
			      byte[] msg = message.getBytes();
			      DatagramPacket packet = new DatagramPacket(msg, msg.length, group, port);
			      System.out.println("P4N sending msg >"+message +"< at " + sdfTime.format(new Date()));
			      socket.send(packet);
			      socket.close();
       		  }catch(Exception ex){
  				ex.printStackTrace();
		    	final Exception myX = ex;
			    runOnUiThread(new Runnable() {
				    @Override
				    public void run() {
		    			showMsg("Multicast Error " + myX);
		    			messages.append("Multicast error " +myX + "\n");
				    }
				});                   
  		      }
	  		}
	  	});
	  	t.start();
					      
	}
	
	//------------------------------------------------------------------------------------------
	private void sendSocketMessage(final String message, final String host, final int port, boolean flag) {
		if(flag)
		   messages.append("Me(" + sdfHHMM.format(new Date()) + "): Attempting to send message="
	                    + message + ", host="+host +"\n");
	   	Thread t = new Thread(new Runnable() {
    		public void run() {
    		   try{
    			 System.out.println("P4N sSM run() message=" + message 
    					             +", host="+host +", port="+port + " at "
    					             + sdfTime.format(new Date()));   
   	           	 Socket soc = new Socket(host, port);      // Connect to server    Timeout ????
   	           	 soc.setSoTimeout(TimeOut);
   	             OutputStream os = soc.getOutputStream();
 			     os.write(message.getBytes());
   	          	 soc.close();
    		   }catch(UnknownHostException | SocketException ex)	{
  				    final Exception x = ex;
  				    System.out.println("P4N sSM Exception " + x);
  				    runOnUiThread(new Runnable() {
    				    @Override
    				    public void run() {
    		    			showMsg("Error " + x);
    		    			messages.append("Socket error "+x + "\n");
    				    }
    				});                   
    		   }catch(Exception x){
    				x.printStackTrace();
    		   }
  	  		}
    	 });
    	 t.start();

     }


    //--------------------------------------------------------------------------------------
    public String getLocalIpAddress() {
        try {
           for (Enumeration<?> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
              NetworkInterface intf = (NetworkInterface) en.nextElement();
              String val = "";
              for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                 InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
//                 System.out.println("P4N hostAddress="+inetAddress.getHostAddress().toString()); //<<<<<
                 if (!inetAddress.isLoopbackAddress()) {
                    val = inetAddress.getHostAddress().toString(); //  <<< Last vs first ???
                 }
              }
              System.out.println("P4N gLIA found IP="+val);
              return val;
           }
        } catch (Exception ex) {
           ex.printStackTrace();
        }
        return null;
     }
    
    //  -----------------------------------------------------------------------
    private void sayTheMessage(final String msg){
    	if(!sayTheMessagePrf) return;      // Check allowed
    	
    	if(tts1 == null) {
	        tts1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
	            @Override
	            public void onInit(int status) {
	               System.out.println("TTS onInit status="+status);	
	               if(status != TextToSpeech.ERROR) {
	                  tts1.setLanguage(Locale.US);
	                  ttsReady = true;
	                  Pager4NormActivity.this.sayTheMessageWorker(msg);  //???? will this work IT DOES
	               }
	            }
	         });
	         
	         tts1.setOnUtteranceProgressListener(new UtteranceProgressListener() {
	       	   public void onDone(String ut) {
	               Toast.makeText(getApplicationContext(), "onDone for ut="+ut, Toast.LENGTH_SHORT).show();
	       	   }

	   			@Override
	   			public void onStart(String utteranceId) {
	   				
	   			}

	   			@Override
	   			@Deprecated
	   			public void onError(String ut) {
	   	            Toast.makeText(getApplicationContext(), "onError for ut="+ut, Toast.LENGTH_SHORT).show();
	   			}
	         });
    	}
    	
    	if(ttsReady) {
    		sayTheMessageWorker(msg);
    	} else {
			System.out.println("P4N sTM ttsReady false msg=" + msg 
					 + "< " + sdfTime.format(new Date()));
    	}
    }  // end sayTheMessage()
    
    private void sayTheMessageWorker(String msg){
		int res = tts1.speak(msg, TextToSpeech.QUEUE_FLUSH, null);  //OLD version
// 	  int res = tts1.speak(text, TextToSpeech.QUEUE_FLUSH, null, UtteranceId);  // -1
		System.out.println("P4N ttsReady speak's Xres="+res + ", msg=" + msg
				           + "< " + sdfTime.format(new Date()));
    }
    
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void readOthersIP() {
    	try{
			FileInputStream fis = openFileInput(OthersIP_FN);
			byte[] bfr = new byte[100]; // should never be this long
			int nbrRd = fis.read(bfr);
			String savedIP = new String(bfr, 0, nbrRd);
			System.out.println("P4N read saved IP=" + savedIP);
			fis.close();
			othersIP = savedIP; 
    	}catch(Exception x){
    		x.printStackTrace();
    	}
    }

    @Override
    public void onStop() {
    	super.onStop();
 		System.out.println("P4N onStop()  at " + sdfTime.format(new Date()));
   }
	
	@Override
	public void onDestroy() {
		super.onDestroy();
 		System.out.println("P4N onDestroy() at " + sdfTime.format(new Date())
 				           + "  version=" + versionName);
 		if(debug) {
 			SaveStdOutput.stop();
 			startedSaveStd = false;
 		}
        if(tts1 != null){
            tts1.stop();
            tts1.shutdown();
            tts1 = null;
         }
	}
	
 	//------------------------------------------
 	//  Show a message in an Alert box
 	private void showMsg(String msg) {

 		AlertDialog ad = new AlertDialog.Builder(this).create();
 		ad.setCancelable(false); // This blocks the 'BACK' button
 		ad.setMessage(msg);
 		ad.setButton(DialogInterface.BUTTON_POSITIVE, "Clear messsge", new DialogInterface.OnClickListener() {
 		    @Override
 		    public void onClick(DialogInterface dialog, int which) {
 		        dialog.dismiss();                    
 		    }
 		});
 		ad.show();
 	} // end showMsg()
 	
 	//=====================================================================================
 	public class GetMessageAsyncTask extends AsyncTask<Void, String, String> {

 	        private Context context;
 	        private TextView statusText;
 	        private ServerSocket serverSocket;

 	        /**
 	         * @param context
 	         * @param statusText
 	         */
 	        public GetMessageAsyncTask(Context context, View statusText) {
 	            this.context = context;
 	            this.statusText = (TextView) statusText;
 	        }
 	        
 	        public void closeSocket() {
 	        	if(serverSocket != null) {
 	        		try{
 	        		   serverSocket.close();
 	        		   serverSocket = null;
 	        		   System.out.println("P4N ServerSocket closed");
 	               	} catch(Exception x) {
 	               		x.printStackTrace();
 	               	}
 	             }
 	        }

 	        @Override
 	        protected String doInBackground(Void... params) {
 	            try {
 	                serverSocket = new ServerSocket(PORT);
 	                System.out.println("P4N dIB Server Socket opened "); //<<<<
 	                
 	              //  Loop to handle multiple connections  
 	              while(!isCancelled())  {
 	            	publishProgress("Waiting for next client to connect\n");
 	                Socket client = serverSocket.accept();    //  Client has connected
 	                System.out.println("P4N Server connected with client at " + sdfTime.format(new Date()));
 	                
 	                InputStream inputstream = client.getInputStream();
 	                
 	                // Here we'll split off to handle receiving multiple files
 	                byte[] buf = new byte[1000];            //<<<< Note: how large is message??
 	                int nbrRd = inputstream.read(buf);
 	                String msg = new String(buf, 0, nbrRd);
 	                System.out.println("P4N dIB read msg="+msg);
 	                // Special testing code
 	                if(msg.length() > 50) {
 	                	msg = "Special replacement for long message\n";
 	                }
 	                publishProgress(msg);
 	              } // end while
 	              
 	 	          serverSocket.close();
 	                  
 	            } catch (Exception e) {
 	                e.printStackTrace();
 	                return null;
 	            }
 	            return null;
 	        }
 	        
 
 	        @Override
 	        protected void onProgressUpdate(String... progress) {
// 	        	 statusText.append(progress[0]);	
// 	        	 Pager4NormActivity.this.sayTheMessage(progress[0]);
 	            Bundle bundle = new Bundle();
		  	    bundle.putString("text", progress[0]);
		  	    resultReceiver.send(100, bundle); 	        	 
 	        }
 	 
 	        /*
 	         * (non-Javadoc)
 	         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
 	         */
 	        @Override
 	        protected void onPostExecute(String result) {
 
 	        }

 	        /*
 	         * (non-Javadoc)
 	         * @see android.os.AsyncTask#onPreExecute()
 	         */
 	        @Override
 	        protected void onPreExecute() {
 	            statusText.append("Opening a server socket\n");
 	        }

 	    }  // end class FileServerAsyncTask

}
