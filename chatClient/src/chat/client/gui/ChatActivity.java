/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package chat.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import jade.core.MicroRuntime;
import jade.util.Logger;
import jade.wrapper.ControllerException;
import jade.wrapper.O2AException;
import jade.wrapper.StaleProxyException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import chat.client.agent.ChatClientInterface;

/**
 * This activity implement the chat interface.
 * 
 * @author Michele Izzo - Telecomitalia
 */

public class ChatActivity extends Activity {
	private Logger logger = Logger.getJADELogger(this.getClass().getName());
	LocationManager locationManager;
	String provider;
	Location location;
    String locStr;
    LocationListener locationListener;

	static final int PARTICIPANTS_REQUEST = 0;

	private MyReceiver myReceiver;

	private String nickname;
	private ChatClientInterface chatClientInterface;
	private String[] llist;
	private String[] jlist;

    static boolean isRinging = false;
    static boolean callReceived = false;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		Criteria criteria = new Criteria();
	    criteria.setAccuracy(Criteria.ACCURACY_FINE);
	    criteria.setAltitudeRequired(false);
	    criteria.setBearingRequired(false);
	    criteria.setCostAllowed(true);
	    criteria.setPowerRequirement(Criteria.POWER_LOW);
	    
	    provider = locationManager.getBestProvider(criteria, true);
	    logger.log(Level.INFO, " -- Loc. Provider --:"+provider.toString());

	    location = locationManager.getLastKnownLocation(provider);
	    if (location != null) {
	      updateWithNewLocation(location);
	      logger.log(Level.INFO, " -- Starting Location --:"+location.toString());
	    } else {
	    	location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
	    	if(location != null){
	    		updateWithNewLocation(location);
	    		logger.log(Level.INFO, " -- Passive Location --:"+location.toString());
	    	}
	    	else
	    		logger.log(Level.INFO, " -- Starting Location --: null!");
	    }
	    
	    // Define a listener that responds to location updates
	    LocationListener locationListener = new LocationListener() {
	        public void onLocationChanged(Location loc) {
	          // Called when a new location is found by the network location provider.
	        	location = loc;
	        	updateWithNewLocation(loc);
	        	if(location != null)
	        		logger.log(Level.INFO, " -- Location Changed --:"+location.toString());
	        	else
	        		logger.log(Level.INFO, " -- Location Changed --: to null!");
	        }

	        public void onStatusChanged(String provider, int status, Bundle extras) {
	        	logger.log(Level.INFO, " -- Status Changed --:(Provider): "+provider.toString()+
	        			" \n(Status): "+Integer.toString(status));
	        }

	        public void onProviderEnabled(String provider) {}

	        public void onProviderDisabled(String provider) {}
	      };

	    // Register the listener with the Location Manager to receive location updates
	    locationManager.requestLocationUpdates(provider, 500, 10, locationListener);
	    
	    Bundle extras = getIntent().getExtras();
		if (extras != null) {
			nickname = extras.getString("nickname");
		}

		try {
			chatClientInterface = MicroRuntime.getAgent(nickname)
					.getO2AInterface(ChatClientInterface.class);
		} catch (StaleProxyException e) {
			showAlertDialog(getString(R.string.msg_interface_exc), true);
		} catch (ControllerException e) {
			showAlertDialog(getString(R.string.msg_controller_exc), true);
		}

		myReceiver = new MyReceiver();

		IntentFilter refreshChatFilter = new IntentFilter();
		refreshChatFilter.addAction("jade.demo.chat.REFRESH_CHAT");
		registerReceiver(myReceiver, refreshChatFilter);
		
		IntentFilter newParticipantFilter = new IntentFilter();
		newParticipantFilter.addAction("jade.demo.chat.REFRESH_PARTICIPANTS");
		registerReceiver(myReceiver, newParticipantFilter);

		IntentFilter clearChatFilter = new IntentFilter();
		clearChatFilter.addAction("jade.demo.chat.CLEAR_CHAT");
		registerReceiver(myReceiver, clearChatFilter);

		setContentView(R.layout.chat);

		Button button = (Button) findViewById(R.id.button_send);
		button.setOnClickListener(buttonSendListener);
        
        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        manager.listen(new TeleListener(), PhoneStateListener.LISTEN_CALL_STATE);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(myReceiver);

		logger.log(Level.INFO, "Destroy activity!");
	}

	private OnClickListener buttonSendListener = new OnClickListener() {
		public void onClick(View v) {
			final EditText messageField = (EditText) findViewById(R.id.edit_message);
			String message = messageField.getText().toString();
			if (message != null && !message.equals("")) {
				String locname;
				if(locStr != null ){
					message += locStr;
//					locname = checkLoc(message);
//					if(locname != null)
//						message += locname;
				}				    
				try {
					chatClientInterface.handleSpoken(message);
					messageField.setText("");
				} catch (O2AException e) {
					showAlertDialog(e.getMessage(), false);
				}
			}

		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.chat_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_participants:
			Intent showParticipants = new Intent(ChatActivity.this,
					ParticipantsActivity.class);
			showParticipants.putExtra("nickname", nickname);
			startActivityForResult(showParticipants, PARTICIPANTS_REQUEST);
			return true;
		case R.id.menu_clear:
			/*
			Intent broadcast = new Intent();
			broadcast.setAction("jade.demo.chat.CLEAR_CHAT");
			logger.info("Sending broadcast " + broadcast.getAction());
			sendBroadcast(broadcast);
			*/
			final TextView chatField = (TextView) findViewById(R.id.chatTextView);
			chatField.setText("");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PARTICIPANTS_REQUEST) {
			if (resultCode == RESULT_OK) {
				// TODO: A participant was picked. Send a private message.
			}
		}
		else{
			final TextView chatField = (TextView) findViewById(R.id.chatTextView);
			String val = "Request code: " + Integer.toString(requestCode) + " Intent: " + data.getDataString();
			chatField.setText(val);
		}
	}

	private class MyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			logger.log(Level.INFO, "Received intent " + action);
			try{
			if (action.equalsIgnoreCase("jade.demo.chat.REFRESH_CHAT")) {
				final TextView chatField = (TextView) findViewById(R.id.chatTextView);
				String msg = intent.getExtras().getString("sentence");
				chatField.append(msg);

				String namedLoc = checkLoc(msg);
				if(namedLoc != null){
					chatField.append("This is "+namedLoc+"\n");
				}

				scrollDown();
			}
			if (action.equalsIgnoreCase("jade.demo.chat.CLEAR_CHAT")) {
				final TextView chatField = (TextView) findViewById(R.id.chatTextView);
				chatField.setText("");
			}
			if (action.equalsIgnoreCase("jade.demo.chat.REFRESH_PARTICIPANTS")) {
				final TextView chatField = (TextView) findViewById(R.id.chatTextView);	
				jlist = chatClientInterface.joinNames();
				if(jlist != null){
					for(String s: jlist){
						chatField.append(s+" has just entered the chat room!\n");
					}
				}				
				llist = chatClientInterface.leftNames();
				if(llist != null){
					for(String s: llist){
						chatField.append(s+" has just left!\n");
					}
				}		
//				chatField.append(intent.toString());
				scrollDown();
			}
//			else {
//				final TextView chatField = (TextView) findViewById(R.id.chatTextView);
//				chatField.append(intent.toString());
//				chatField.append(intent.getAction());
//				scrollDown();
//			}
			}catch(NullPointerException e){
				logger.log(Level.INFO, "NullPointerEx " + e.getLocalizedMessage());
				System.out.println(e.getMessage());
				final TextView chatField = (TextView) findViewById(R.id.chatTextView);
				chatField.append("NullPointerEx\n");
			}
		}
	}
	
	class TeleListener extends PhoneStateListener {
		public void onCallStateChanged (int state, String incomingNumber) {
			super.onCallStateChanged(state, incomingNumber);
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				if(isRinging == true && callReceived == false) {
					//Missed call from incomingNumber
					chatClientInterface.handleSpoken("Hello " + incomingNumber);
				}
				isRinging = false;
				callReceived = false;
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				callReceived = true;
				break;
			case TelephonyManager.CALL_STATE_RINGING:
				isRinging = true;
				break;
			default:
				break;
			}
		}
	}

	private void scrollDown() {
		final ScrollView scroller = (ScrollView) findViewById(R.id.scroller);
		final TextView chatField = (TextView) findViewById(R.id.chatTextView);
		scroller.smoothScrollTo(0, chatField.getBottom());
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		final TextView chatField = (TextView) findViewById(R.id.chatTextView);
		savedInstanceState.putString("chatField", chatField.getText()
				.toString());
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		final TextView chatField = (TextView) findViewById(R.id.chatTextView);
		chatField.setText(savedInstanceState.getString("chatField"));
	}

	private void showAlertDialog(String message, final boolean fatal) {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				ChatActivity.this);
		builder.setMessage(message)
				.setCancelable(false)
				.setPositiveButton("Ok",
						new DialogInterface.OnClickListener() {
							public void onClick(
									DialogInterface dialog, int id) {
								dialog.cancel();
								if(fatal) finish();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();		
	}
	
	private void updateWithNewLocation(Location loc){

	    if (loc != null) {
	      double lat = loc.getLatitude();
	      double lng = loc.getLongitude();
	      locStr = "\n @Latitude: " + lat + "\n  Longitude: " + lng;
	    } else {
	      locStr = null;
	    }
	}
	
	private boolean hasLoc(String msg){
		if(msg.contains("@Latitude:") && msg.contains("Longitude:"))
			return true;
		else
			return false;
	}
	
	private String checkLoc(String msg){
		if(hasLoc(msg)){
			String latStr = "@Latitude:";
			String lonStr = "Longitude:";
			char[] cs = new char[4];
			Double lat, lon;
			try{
			int start = msg.indexOf(latStr)+latStr.length()+1;
			int end = start;
			boolean period = false;
			while( ((msg.charAt(end) >= '0' && msg.charAt(end) <= '9') 	|| (msg.charAt(end) == '.') 
					|| (msg.charAt(end) == '-') ) && (end < msg.length()-1) ){
				if(msg.charAt(end) == '.' && period == true)
					break;
				else if(msg.charAt(end) == '.')
					period = true;
				end++;				
			}
			cs[0]=msg.charAt(start);
			cs[1]=msg.charAt(end); 
			lat = Double.parseDouble(msg.substring(start,end));
			
			start = msg.indexOf(lonStr)+lonStr.length()+1;
			end = start;
			period = false;
			while( ((msg.charAt(end) >= '0' && msg.charAt(end) <= '9') 	|| (msg.charAt(end) == '.') 
					|| (msg.charAt(end) == '-') ) && (end < msg.length()-1) ){
				if(msg.charAt(end) == '.' && period == true)
					break;
				else if(msg.charAt(end) == '.')
					period = true;
				end++;				
			}
			cs[2]=msg.charAt(start);
			cs[3]=msg.charAt(end); 
			lon = Double.parseDouble(msg.substring(start,end));
			}catch(NumberFormatException e){
				return null;
			}
			return new knownLocations().locName(lon, lat);
			
		}else{
			return null;
		}
	}
	
	public class knownLocations {
		private List<namedLocation> locList;
		
		public knownLocations(){
			locList = new ArrayList<namedLocation>(3);
			locList.add(new namedLocation(35.77198,-78.67385,"EBII (Centennial)"));
			locList.add(new namedLocation(38.90719,-77.03687,"Washington D.C."));
			locList.add(new namedLocation(48.85661,2.35222,"Paris"));
		};
		
		public String locName(Double lon, Double lat){
			for(namedLocation l : locList ){
				double d = getDistance(lat, lon, l.latitude, l.longitude);
				if(d < 50.0){
					return l.placeName;
				}
			}
			return null;
		}		
	}
	
	public class namedLocation {
		Double latitude;
		Double longitude;
		String placeName;
		
		public namedLocation(Double lat, Double lon, String nam){
			latitude=lat;
			longitude=lon;
			placeName=nam;
		}
	}
	
	private static double getDistance(double lat1, double lon1, double lat2, double lon2) {
		final double Radius = 6371 * 1E3; //Earth's mean radius
		double dLat = Math.toRadians(lat2-lat1);
		double dLon = Math.toRadians(lon2-lon1);
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
		Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
		Math.sin(dLon/2) * Math.sin(dLon/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		return Radius * c;
	}
}