package com.normsstuff.pager4norm;
//package com.normsstuff.batterylevelwarning;

//import java.util.ArrayList;

import com.example.pager4norm.R;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class EditTimesFragment extends DialogFragment {
	
	// Define interface with callback to save updated HHMM
	interface TimeSaver {
		public void saveTime(Pager4NormActivity.HHMM hhmm, int loc);
	}
	
	final String HHMM_K = "hhmm";
	
	TimeSaver timeSaver;                 //<<<<<<<<<< Not saved after rotate!!!!
	Pager4NormActivity.HHMM hhmm;
	int theLoc;
	
	// Save above for callback
	public void setTimeSaver(TimeSaver ts, Pager4NormActivity.HHMM str, int loc) {
		timeSaver = ts;
		hhmm = str;
		theLoc = loc;
	}
	
	Context ctx; 
	Button saveBtn, quitBtn, clearBtn;
	
	boolean debugging = false;  // control print outs
	
	//---------------------------------------------------------------------------
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      ctx = getActivity();  // save for showMsg()
      setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo_Light_Dialog);
      
      if(savedInstanceState != null) {
			hhmm = savedInstanceState.getParcelable(HHMM_K);
      	System.out.println("ETF onCreate sIS="+savedInstanceState
      			+ ", hhmm="+hhmm);
      }
      setRetainInstance(true); //  Will this save timeSaver?
  }
  
  @Override
  public void onAttach(Activity activity) {
      super.onAttach(activity);
      try {
      	System.out.println("ETF onAttach activity="+activity);
//           mListener = (OnArticleSelectedListener) activity;
      } catch (ClassCastException e) {
          throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
      }
  }

  //-----------------------------------------------------------------
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
      final View theView = inflater.inflate(R.layout.edit_time_details, container, false);
      
 		EditText latDegET = (EditText)theView.findViewById(R.id.hoursValue);
		latDegET.setText(""+hhmm.hour);
		EditText latMinET = (EditText)theView.findViewById(R.id.minutesValue);
		latMinET.setText(""+hhmm.minute);
      
      
      // Set up button listeners
      saveBtn = (Button) theView.findViewById(R.id.saveBtn);
      saveBtn.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
          	// Get new times from GUI
      		EditText hoursET = (EditText)theView.findViewById(R.id.hoursValue);
      		String hoursS = hoursET.getText().toString();
      		EditText minET = (EditText)theView.findViewById(R.id.minutesValue);
      		String minutesS = minET.getText().toString();
      		Pager4NormActivity.HHMM newHHMM = null;
      		try{
      			newHHMM = new Pager4NormActivity.HHMM(hoursS+":"+minutesS); 
      		}catch(Exception x){
          		TextView lblET = (TextView)theView.findViewById(R.id.message_area);
          		lblET.setText(x.getMessage());
     			    return;
      		}
          	System.out.println("ELF Save btn clicked newHHMM="+ newHHMM);

          	timeSaver.saveTime(newHHMM, theLoc);
          	dismiss();                    // done ???
      	}
      });
      
      quitBtn = (Button) theView.findViewById(R.id.quitBtn);
      quitBtn.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
          	System.out.println("ELF Quit btn clicked");
          	dismiss();  // Done
          }
      });
      
      clearBtn = (Button) theView.findViewById(R.id.clearBtn);
      clearBtn.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
          	System.out.println("ETF Clear btn clicked");
          	// Clear all the fields
      		EditText latDegET = (EditText)theView.findViewById(R.id.hoursValue);
      		latDegET.setText("");
      		EditText latMinET = (EditText)theView.findViewById(R.id.minutesValue);
      		latMinET.setText("");
      		TextView lblET = (TextView)theView.findViewById(R.id.message_area);
      		lblET.setText("");
      		// give focus to latDegET
      		latDegET.requestFocus();

          }
      });
      
      getDialog().setTitle("Line text editor");

      return theView;
  }
  
	//--------------------------------------------------------------
	//  Save values to allow us to restore state when rotated
	@Override
	public void onSaveInstanceState(Bundle bndl) {
		super.onSaveInstanceState(bndl);
		bndl.putParcelable(HHMM_K, hhmm);
	}

}