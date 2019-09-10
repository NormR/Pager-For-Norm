package com.normsstuff.pager4norm;
//package com.normsstuff.batterylevelwarning;

import java.util.ArrayList;
import java.util.List;

import com.example.pager4norm.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class ShowEditListFragment extends DialogFragment 
                                  implements OnItemClickListener, OnItemSelectedListener, OnClickListener,
                                             EditTimesFragment.TimeSaver
                                  {
	
	public interface TimesListHandler {
		public List<Pager4NormActivity.HHMM> getTimesList();
		public void setTimesList(List<Pager4NormActivity.HHMM> list);
		public void saveTimesList();
	}
	
    Button upBtn, downBtn, deleteBtn, undoBtn, editBtn, addBtn, saveBtn;

	ListView lv;
	ArrayAdapter<Pager4NormActivity.HHMM> aaStr;
	Context cntx;    //<<<<<<<<<<< for showMsg
	final String WorkList_K = "WorkList";
	final String SaveList_K = "SavedList";
	
	List<Pager4NormActivity.HHMM> hhmmListSaved;
	List<Pager4NormActivity.HHMM> workList;     // do updates here
	
	public void setTimesList(List<Pager4NormActivity.HHMM> list) {
		hhmmListSaved = list;         // save passed ref to times list
		workList = new ArrayList<>();
		workList.addAll(list);
	}
	
	TimesListHandler tlh;
	public void setTimesListHandler(TimesListHandler tlh) {
	   this.tlh = tlh;
    }
	
	public ShowEditListFragment(TimesListHandler tlh) {
	   this.tlh = tlh;
    }
	
	
    int deletedPos = -1;
    Pager4NormActivity.HHMM deletedItem = null;

    int firstVisibleItem = -1;
    int visibleItemCount = 0;

	boolean debugging = false;  // control print outs

	
	//---------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getActivity().setContentView(R.layout.editlist);  // This replaces original contents!!!
        cntx = getActivity();  // save for showMsg()
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo_Light_DialogWhenLarge); //Light_Dialog);  //???? what other values here
        // http://developer.android.com/reference/android/R.style.html
        
        hhmmListSaved = tlh.getTimesList();  // get Alarm times
		workList = new ArrayList<>();
		workList.addAll(hhmmListSaved);
        
        if(savedInstanceState != null) {
			workList = savedInstanceState.getParcelableArrayList(WorkList_K);
//			hhmmListSaved = savedInstanceState.getParcelableArrayList(SaveList_K);
//			hhmmListSaved = ((Pager4NormActivity)getActivity()).getTimes();       //???? Get from Activity
        	System.out.println("SELF onCreate sIS="+savedInstanceState
        			+ ", workList="+workList
        			+" \n  hhmmListSaved=" + hhmmListSaved);
        }
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
        	System.out.println("SELF onAttach activity="+activity);
 //           mListener = (OnArticleSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
        }
    }

    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		System.out.println("SELF onCrtView  workList="+workList);
        View v = inflater.inflate(R.layout.editlist, container, false);
        
        // Put filename in title
        TextView tv = (TextView)v.findViewById(R.id.start_info);
        String title = tv.getText().toString();
        tv.setText(title + "\n   Filename=wpFn");
        
        // Set up button listeners
        upBtn = (Button) v.findViewById(R.id.upBtn);
        upBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                upBtnClicked(v);  // pass it on
            }
        });
        downBtn = (Button) v.findViewById(R.id.downBtn);
        downBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                downBtnClicked(v);  // pass it on
            }
        });
        deleteBtn = (Button) v.findViewById(R.id.deleteBtn);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deleteBtnClicked(v);  // pass it on
            }
        });
        undoBtn = (Button) v.findViewById(R.id.undoBtn);
        undoBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                undoBtnClicked(v);  // pass it on
            }
        });
        editBtn = (Button) v.findViewById(R.id.editBtn);
        editBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editBtnClicked(v);  // pass it on
            }
        });
        
        addBtn = (Button) v.findViewById(R.id.addBtn);
        addBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addBtnClicked(v);  // pass it on
            }
        });
        
        saveBtn = (Button) v.findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveBtnClicked(v);  // pass it on
            }
        });
       
		lv = (ListView) v.findViewById(android.R.id.list);
		lv.setOnScrollListener(new AbsListView.OnScrollListener() {
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount){
				if(debugging) System.out.println("onScroll firstVI="+firstVisibleItem 
						            + ", visibleCnt="+visibleItemCount);
				ShowEditListFragment.this.firstVisibleItem = firstVisibleItem;   // save for below
				ShowEditListFragment.this.visibleItemCount = visibleItemCount;
			}
			public void onScrollStateChanged(AbsListView view, int scrollState){
				if(debugging) System.out.println("onScrollSC state="+scrollState);
			}
		});
		
	
        System.out.println("SELF onCreateView v="+v);
        aaStr = new ArrayAdapter<Pager4NormActivity.HHMM>(getActivity(),
                            android.R.layout.simple_list_item_activated_1, workList);
        lv.setAdapter(aaStr);                     //<<<< NPE
        lv.setOnItemSelectedListener(this); //???? for AdapterView
        lv.setClickable(true);
        lv.setOnItemClickListener(this); 
        //Don't call setOnClickListener for an AdapterView. You probably want setOnItemClickListener instead

        getDialog().setTitle("Norm's Times list editor");
        return v;
    }
    
 	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }
 	
	//--------------------------------------------------------------
	//  Save values to allow us to restore state when rotated
	@Override
	public void onSaveInstanceState(Bundle bndl) {
		super.onSaveInstanceState(bndl);
		bndl.putParcelableArrayList(WorkList_K, (ArrayList<Pager4NormActivity.HHMM>)workList);
		bndl.putParcelableArrayList(SaveList_K, (ArrayList<Pager4NormActivity.HHMM>)hhmmListSaved);
	}


	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		int selItem = lv.getCheckedItemPosition();   // WORKS <<<<<<<<<<
		if(debugging) System.out.println("onItemClick arg0="+arg0+", arg1="+arg1 +", arg2="+arg2 +", arg3="+arg3
				+"\nselItem="+selItem +", nbr children="+lv.getChildCount()
				+", first="+lv.getFirstVisiblePosition());
		//selItem=5, nbr children=8, first=2
		//selItem=20, nbr children=8, first=13
		//selItem=9, nbr children=9, first=6      ??  9 vs 8 childern here

		// Enable buttons now
		upBtn.setEnabled(selItem > 0);    // If not at top
		downBtn.setEnabled(selItem < workList.size()-1);  // If not at bottom
		deleteBtn.setEnabled(true);
		editBtn.setEnabled(true);
		addBtn.setEnabled(true);
		
	}

	//==========================================================================================
	//------------------------------------------------------------
	// Define button press handlers
	//Could not find a method downBtnClicked(View) in the activity class android.view.ContextThemeWrapper 
	// for onClick handler on view class android.widget.Button with id 'downBtn'

	public void upBtnClicked(View v){
//		int selItem = lv.getSelectedItemPosition();  // -1 ????
		int selItem = lv.getCheckedItemPosition();   // WORKS <<<<<<<<<<
//		showMsg("up clicked selItem="+selItem);
		if(debugging) System.out.println("up clicked setSelection for selItem="+selItem 
				+", nbr children="+lv.getChildCount() //up clicked setSelection for selItem=13, nbr children=8
				+", first="+lv.getFirstVisiblePosition());
				// always 8 get == null for item > 8
		
		if(selItem > 0) {
			lv.setItemChecked(selItem, false);
			// Move item up
			Pager4NormActivity.HHMM item = aaStr.getItem(selItem);
			aaStr.remove(item);
			aaStr.insert(item, selItem-1);
			lv.setItemChecked(selItem-1, true);    // move up one
			if(selItem == 1)  // turn off Up button if at top now
				upBtn.setEnabled(false);
			// Make sure moved item still in view
			if(selItem < firstVisibleItem) {
				// move view up to show selected item
				int newPos = (firstVisibleItem - 2) >= 0 ? (firstVisibleItem - 2) : 0; 
				lv.smoothScrollToPosition(newPos);    // Weird/delayed actions
				lv.setSelectionFromTop(newPos, 0);
				if(debugging) System.out.println("scroll/setPos up to "+newPos);
			}
		}
		

/*		
		// Change position if ... 
		selItem = (selItem == 0) ? selItem + 1 : selItem - 1; // change
//		lv.setSelection(selItem);  // NOTHING HAPPENS ???
		// Set color ???
		View selV = lv.getChildAt(selItem);
		if(selV == null) {  //<<<< WHY Null sometimes ???<<<<<<<<<<<
			System.out.println("selV == null for selItem="+selItem);
		}else {
//			selV.setBackgroundColor(Color.RED);  // This works!!! Color stays
			selV = lv.getChildAt(5);
			selV.setPressed(true);
//			selV = lv.getChildAt(6);
//			selV.setSelected(true);
		}
*/		
	}
	public void downBtnClicked(View v){
		int selItem = lv.getCheckedItemPosition();   // WORKS <<<<<<<<<<
//		showMsg("down clicked selItem="+selItem);
		if(debugging) System.out.println("down clicked selItem="+selItem);
		// Turn off this one and set one beneath it
		if(selItem < workList.size()-1) {
			lv.setItemChecked(selItem, false);
			Pager4NormActivity.HHMM item = aaStr.getItem(selItem);
			aaStr.insert(item, selItem+2);
			aaStr.remove(item);
			lv.setItemChecked(selItem+1, true);  // THIS WORKS <<<<<<<
			if(!upBtn.isEnabled())
				upBtn.setEnabled(true);   // must be ok if moved down
			// Make sure moved item still in view
			if(selItem > (firstVisibleItem + visibleItemCount)) {
				// move view down to see item
				int newPos = firstVisibleItem + 3;
				lv.smoothScrollToPosition(newPos);  // Weird movements
				lv.setSelectionFromTop(newPos, 0); // Try this???
				if(debugging) System.out.println("scroll/setPos down to "+newPos);
			}

		}
/*		
		View selV = lv.getChildAt(selItem+2);
		if(selV == null) {  //<<<< WHY Null sometimes ???<<<<<<<<<<<
			System.out.println("selV == null for selItem="+selItem);
		}else {
//			selV.setBackgroundColor(Color.RED);  // This works!!! Color stays
			selV.setPressed(true);
		}
*/		
	}
	
	public void deleteBtnClicked(View v){
		int selItem = lv.getCheckedItemPosition();   // WORKS <<<<<<<<<<
//		showMsg("down clicked selItem="+selItem);
		if(debugging) System.out.println("delete clicked selItem="+selItem);
		lv.setItemChecked(selItem, false);
		deletedItem = aaStr.getItem(selItem);
		deletedPos = selItem;
		aaStr.remove(deletedItem);
		undoBtn.setEnabled(true);
		
		// Nothing selected, turn off buttons
		upBtn.setEnabled(false);
		downBtn.setEnabled(false);
		deleteBtn.setEnabled(false);
		editBtn.setEnabled(false);
		addBtn.setEnabled(false); //????????? when
	}
	
	public void undoBtnClicked(View v){
		if(debugging) System.out.println("SELF undo clicked deletedPos="+deletedPos);
		aaStr.insert(deletedItem, deletedPos);
		lv.setItemChecked(deletedPos, true);
		deletedPos = -1; // clear
		undoBtn.setEnabled(false);
		// Restore these buttons
		upBtn.setEnabled(true);  // Should test position first
		downBtn.setEnabled(true);
		deleteBtn.setEnabled(true);
	}
	
	public void editBtnClicked(View v){
		int selItem = lv.getCheckedItemPosition();   // WORKS <<<<<<<<<<
//		showMsg("edit clicked selItem="+selItem);
		if(debugging) System.out.println("SELF edit clicked selItem="+selItem);
	
		// Get the line and present to user for changes
		Pager4NormActivity.HHMM aTime = aaStr.getItem(selItem);
        EditTimesFragment editTimesF = new EditTimesFragment();
        editTimesF.setTimeSaver(this, aTime, selItem);         // pass to show in list 

        if(debugging) System.out.println("SELF Creating  edit fragment");
        // Add the fragment to the activity, pushing this transaction
        // on to the back stack.
        FragmentTransaction ft3 = getFragmentManager().beginTransaction();
        editTimesF.show(ft3, "dialog2");

	}
	//---------------------------------------------------------
	// Callback method for the EditTimesFragment class
	//  for interface TimeSaver
	public void saveTime(Pager4NormActivity.HHMM hhmm, int loc){
		// remove and insert
		System.out.println("SELF saveTime loc="+loc + ", hhmm="+hhmm);
		Pager4NormActivity.HHMM oldLine = aaStr.getItem(loc);
		aaStr.remove(oldLine);   // NEED original ??? here
		aaStr.insert(hhmm,  loc);
	}
	
	public void addBtnClicked(View v) {
		int selItem = lv.getCheckedItemPosition(); 
		Pager4NormActivity.HHMM newItem = new Pager4NormActivity.HHMM(12,0);
		aaStr.insert(newItem, selItem);	
	}
	
	public void saveBtnClicked(View v){
		// Save all the times ???? not needed
		System.out.println("SELF save btn clicked, hhmmlist="+workList
				+"\n  hhmmListSaved="+hhmmListSaved);
		showMsg("Saving Times list="+workList);
		
		// Update the passed list
		hhmmListSaved.clear();
		hhmmListSaved.addAll(workList);
		tlh.setTimesList(hhmmListSaved);
		tlh.saveTimesList();
	}  // end saveBtnClicked()
	
	//========================================================
	//  Show a message in an Alert box
	private void showMsg(String msg) {

		AlertDialog ad = new AlertDialog.Builder(cntx).create();
		ad.setCancelable(false); // This blocks the 'BACK' button
		ad.setMessage(msg);
		ad.setButton(DialogInterface.BUTTON_POSITIVE, "Clear messsge", new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        dialog.dismiss();                    
		    }
		});
		ad.show();
	}

	
}
