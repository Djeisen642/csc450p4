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

import chat.client.agent.CalendarContentResolver;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import jade.core.MicroRuntime;
import jade.util.Logger;
import jade.wrapper.ControllerException;
import jade.wrapper.O2AException;
import jade.wrapper.StaleProxyException;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
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
	private Context context = this;
	
	private Set<String> missedCallers = new HashSet<String>();
	
	private double lat = -91.0;
	private double lng = -91.0;

    static boolean isRinging = false;
    static boolean callReceived = false;
    static int missedCalls = 0;
    
	private boolean urgent = false;
    
    Handler locTimerHandler = new Handler();
    Runnable locTimerRunnable = new Runnable() {
    	@Override
    	public void run() {
			boolean priv = (CalendarContentResolver.findAvailability(context)==2)?true:false;
    		if(!priv) {
    			for(String to : missedCallers) {
        			String message = "Agent: Check Location";
    				chatClientInterface.handleSpokenTo(to, message);
    			}
    		} 
    		logger.log(Level.INFO, "locTimerRunnable");
    		locTimerHandler.postDelayed(this, 10000); // 10 seconds
    	}
    };
    
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
			lat = location.getLatitude();
			lng = location.getLongitude();
			logger.log(Level.INFO, " -- Starting Location --:"+location.toString());
	    } else {
	    	location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
	    	if(location != null){
				lat = location.getLatitude();
				lng = location.getLongitude();
	    		logger.log(Level.INFO, " -- Passive Location --:"+location.toString());
	    	}
	    	else
	    		logger.log(Level.INFO, " -- Starting Location --: null!");
	    }
	    
	    // Define a listener that responds to location updates
	    LocationListener locationListener = new LocationListener() {
	        public void onLocationChanged(Location loc) {
	          // Called when a new location is found by the network location provider.
	        	if(loc != null) {
					lat = loc.getLatitude();
					lng = loc.getLongitude();
	        		logger.log(Level.INFO, " -- Location Changed --:"+loc.toString());
	        	} else
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

		IntentFilter casualFilter = new IntentFilter();
		casualFilter.addAction("jade.demo.chat.CASUAL");
		registerReceiver(myReceiver, casualFilter);

		IntentFilter checkLocFilter = new IntentFilter();
		checkLocFilter.addAction("jade.demo.chat.CHECK_LOC");
		registerReceiver(myReceiver, checkLocFilter);

		IntentFilter receiveLocFilter = new IntentFilter();
		receiveLocFilter.addAction("jade.demo.chat.RECEIVE_LOC");
		registerReceiver(myReceiver, receiveLocFilter);

		IntentFilter urgencyCheckFilter = new IntentFilter();
		urgencyCheckFilter.addAction("jade.demo.chat.URGENCY_CHECK");
		registerReceiver(myReceiver, urgencyCheckFilter);
		
		IntentFilter lostPhoneFilter = new IntentFilter();
		lostPhoneFilter.addAction("jade.demo.chat.PHONE_LOST");
		registerReceiver(myReceiver, lostPhoneFilter);
		
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
			if (message.contains("Agent: ")) {
				if (Pattern.compile(Pattern.quote("important"), Pattern.CASE_INSENSITIVE).matcher(message).find()) {
					urgent = true;
					messageField.setText("");
				} else if (Pattern.compile(Pattern.quote("casual"), Pattern.CASE_INSENSITIVE).matcher(message).find()) {
					urgent = false;
					messageField.setText("");
				}
			} else if (message != null && !message.equals("")) {  
				try {
					chatClientInterface.handleSpoken(message);
					messageField.setText("");
				} catch (O2AException e) {
					showAlertDialog(e.getMessage(), false);
				}
			}

		}
	};
	
	private double getDistanceFromPhone(double lat2, double lon2) {
		double lat1 = lat;
		double lon1 = lng;
		final double Radius = 6371 * 1E3; //Earth's mean radius
		double dLat = Math.toRadians(lat2-lat1);
		double dLon = Math.toRadians(lon2-lon1);
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
		Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
		Math.sin(dLon/2) * Math.sin(dLon/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		return Radius * c;
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
					chatField.append(intent.getExtras().getString("sentence"));
					scrollDown();
				} else if (action.equalsIgnoreCase("jade.demo.chat.CLEAR_CHAT")) {
					final TextView chatField = (TextView) findViewById(R.id.chatTextView);
					chatField.setText("");
				} else if (action.equalsIgnoreCase("jade.demo.chat.REFRESH_PARTICIPANTS")) {
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
				} else if (action.equalsIgnoreCase("jade.demo.chat.CASUAL")) {
		    		logger.log(Level.INFO, "Casual");
					String speaker = intent.getExtras().getString("sentence");
					missedCallers.add(speaker);
					locTimerHandler.removeCallbacks(locTimerRunnable);
					locTimerHandler.postDelayed(locTimerRunnable, 0);
				} else if (action.equalsIgnoreCase("jade.demo.chat.CHECK_LOC")) {
				
					//Check calendar if we're in a public event w/ an available status
					//2 = private, 0, 1 = server default, 3 = public
					boolean priv = (CalendarContentResolver.findAvailability(context)==2)?true:false;
					
					if (!priv && lat != -91.0 && lng != -91.0) {
			    		logger.log(Level.INFO, "Check Loc");
						String speaker = intent.getExtras().getString("sentence");
						String message = "Agent: Location:"+ lat+ ":" + lng;
						chatClientInterface.handleSpokenTo(speaker, message);
					}
				} else if (action.equalsIgnoreCase("jade.demo.chat.RECEIVE_LOC")) {
					String message = intent.getExtras().getString("sentence");
					String[] msgSplit = message.split(":");
					logger.log(Level.INFO, message);
					if (msgSplit.length == 5){
			    		logger.log(Level.INFO, "Receive Loc");
						for(String caller : missedCallers) {
							if (caller.equals(msgSplit[0])) {
								double theirLat = Double.parseDouble(msgSplit[3]);
								double theirLong = Double.parseDouble(msgSplit[4]);
								if (getDistanceFromPhone(theirLat, theirLong) < 50.0) {
									Notification notification = new Notification.Builder(context)
									.setSmallIcon(R.drawable.icon)
									.setContentTitle("Missed Casual Call from " + caller)
									.setContentText(caller +" is in the area.").build();
									NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
									notificationManager.notify(0, notification);
									missedCallers.remove(caller);
								}
							}
						}
						if (missedCallers.isEmpty()) {
							locTimerHandler.removeCallbacks(locTimerRunnable);
						}
					}
				} else if (action.equalsIgnoreCase("jade.demo.chat.URGENCY_CHECK")) {
					if (urgent) {
						chatClientInterface.handleSpokenTo(intent.getExtras().getString("sentence"), "Agent: Important");
					} else {
						chatClientInterface.handleSpokenTo(intent.getExtras().getString("sentence"), "Agent: Casual");
					}
					urgent = false;
				} else if (action.equalsIgnoreCase("jade.demo.chat.PHONE_LOST")) {
					logger.log(Level.INFO, "Callee's Phone is Lost");
					String message = intent.getExtras().getString("sentence");
					String[] msgSplit = message.split(":");
					if (msgSplit.length == 6) {
						String callee = msgSplit[0];
						String email = msgSplit[3];
						Notification notification = new Notification.Builder(context)
						.setSmallIcon(R.drawable.icon)
						.setContentTitle(callee + " seems to have misplaced their phone.")
						.setContentText("Reach " + callee + " by email at " + email + ".").build();
						NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
						notificationManager.notify(0, notification);
					}
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
				chatField.append(action.toString()+"\n");
			}
		}
	}
	
	// http://stackoverflow.com/questions/3712112/search-contact-by-phone-number
	public String getContactDisplayNameByNumber(String number) {
		if (number == null || number.equals("")) {
			return "Unknown";
		}
	    Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
	    String name = "?";

	    ContentResolver contentResolver = getContentResolver();
	    Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
	            ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

	    try {
	        if (contactLookup != null && contactLookup.getCount() > 0) {
	            contactLookup.moveToNext();
	            name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
	            //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
	        }
	    } finally {
	        if (contactLookup != null) {
	            contactLookup.close();
	        }
	    }

	    return name;
	}
	
	class TeleListener extends PhoneStateListener {
		public void onCallStateChanged (int state, String incomingNumber) {
			logger.log(Level.INFO, "Incoming Number: " + incomingNumber);
			super.onCallStateChanged(state, incomingNumber);
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				String name = getContactDisplayNameByNumber(incomingNumber);
				if(isRinging == true && callReceived == false) {
					//Missed call from incomingNumber
					if (!name.isEmpty()) {
						missedCalls++;
						logger.log(Level.INFO, name);
						chatClientInterface.handleSpokenTo(name, "Agent: Urgency?");
					}
				}
				logger.log(Level.INFO, "Concurrent Missed Calls: " + missedCalls);
				if (missedCalls >= 3) {
					if (!name.isEmpty()) {
						logger.log(Level.INFO, name);
						chatClientInterface.handleSpokenTo(name, nickname + ":" + name + ":" + getEmail(context) + ":Agent: Phone Lost");
					}
				}
				isRinging = false;
				callReceived = false;
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				callReceived = true;
				missedCalls = 0;
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
	
	static String getEmail(Context context) {
		AccountManager accountManager = AccountManager.get(context); 
		Account account = getAccount(accountManager);

		if (account == null) {
			return null;
		} else {
			return account.name;
		}
	}

	private static Account getAccount(AccountManager accountManager) {
		Account[] accounts = accountManager.getAccounts();
		Account account;
		if (accounts.length > 0) {
			account = accounts[0];      
		} else {
			account = null;
		}
		return account;
	}
	
	
}

