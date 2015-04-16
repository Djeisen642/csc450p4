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

import chat.client.agent.ChatClientInterface;
import jade.core.MicroRuntime;
import jade.util.Logger;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.provider.Contacts.People;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Profile;

/**
 * This activity implement the participants interface.
 * 
 * @author Michele Izzo - Telecomitalia
 */

public class ParticipantsActivity extends ListActivity {
	private Logger logger = Logger.getJADELogger(this.getClass().getName());

	private MyReceiver myReceiver;

	private String nickname;
	private ChatClientInterface chatClientInterface;
	private Uri mContactUri;
	
	//Code from: 
	// http://blog.wittchen.biz.pl/how-to-read-contacts-in-android-device-using-contentresolver/
	public class Contact {
	    public int id;
	    public String name;
	    public String phone;
	    public String email;
	    public String uriString;
	}

	//Code from: 
	// http://blog.wittchen.biz.pl/how-to-read-contacts-in-android-device-using-contentresolver/
	public class ContactsProvider {
		 
	    private Uri QUERY_URI = ContactsContract.Contacts.CONTENT_URI;
	    private String CONTACT_ID = ContactsContract.Contacts._ID;
	    private String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
	    private Uri EMAIL_CONTENT_URI = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
	    private String EMAIL_CONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
	    private String EMAIL_DATA = ContactsContract.CommonDataKinds.Email.DATA;
	    private String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
	    private String PHONE_NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
	    private Uri PHONE_CONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
	    private String PHONE_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
	    private String STARRED_CONTACT = ContactsContract.Contacts.STARRED;
	    private ContentResolver contentResolver;
	 
	    public ContactsProvider() {
	        contentResolver = getContentResolver();
	    }
	 
	    public List<Contact> getContacts() {
	        List<Contact> contactList = new ArrayList<Contact>();
	        String[] projection = new String[]{CONTACT_ID, DISPLAY_NAME, HAS_PHONE_NUMBER, STARRED_CONTACT};
	        String selection = null;
	        Cursor cursor = contentResolver.query(QUERY_URI, projection, selection, null, null);
	 
	        while (cursor.moveToNext()) {
	            Contact contact = getContact(cursor);
	            contactList.add(contact);
	        }
	 
	        cursor.close();
	        return contactList;
	    }
	 
	    private Contact getContact(Cursor cursor) {
	        String contactId = cursor.getString(cursor.getColumnIndex(CONTACT_ID));
	        String name = (cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)));
	        Uri uri = Uri.withAppendedPath(QUERY_URI, String.valueOf(contactId));
	 
	        Intent intent = new Intent(Intent.ACTION_VIEW);
	        intent.setData(uri);
	        String intentUriString = intent.toUri(0);
	 
	        Contact contact = new Contact();
	        contact.id = Integer.valueOf(contactId);
	        contact.name = name;
	        contact.uriString = intentUriString;
	 
	        getPhone(cursor, contactId, contact);
	        getEmail(contactId, contact);
	        return contact;
	    }
	 
	    private void getEmail(String contactId, Contact contact) {
	        Cursor emailCursor = contentResolver.query(EMAIL_CONTENT_URI, null, EMAIL_CONTACT_ID + " = ?", new String[]{contactId}, null);
	        while (emailCursor.moveToNext()) {
	            String email = emailCursor.getString(emailCursor.getColumnIndex(EMAIL_DATA));
	            if (!TextUtils.isEmpty(email)) {
	                contact.email = email;
	            }
	        }
	        emailCursor.close();
	    }
	 
	    private void getPhone(Cursor cursor, String contactId, Contact contact) {
	        int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));
	        if (hasPhoneNumber > 0) {
	            Cursor phoneCursor = contentResolver.query(PHONE_CONTENT_URI, null, PHONE_CONTACT_ID + " = ?", new String[]{contactId}, null);
	            while (phoneCursor.moveToNext()) {
	                String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(PHONE_NUMBER));
	                contact.phone = phoneNumber;
	            }
	            phoneCursor.close();
	        }
	    }
	}

    
       
//    Contacts.Data.
    //Contentresolver type
    //getContentResolver()
    //contentResolver.query()
    
    //for adding:
    //contentProviderOperationBuilder
    //type.add(op.build()) where type is an arraylist of cpob above
    //getContactResolver.applyBatch()

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ContactsProvider cProvider = null;

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			nickname = extras.getString("nickname");
		}

		try {
			chatClientInterface = MicroRuntime.getAgent(nickname)
					.getO2AInterface(ChatClientInterface.class);
			cProvider = new ContactsProvider();
		} catch (StaleProxyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ControllerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(NullPointerException e){
			logger.log(Level.INFO, "new cProvider: " +e.getLocalizedMessage());
		}

		myReceiver = new MyReceiver();

		IntentFilter refreshParticipantsFilter = new IntentFilter();
		refreshParticipantsFilter
				.addAction("jade.demo.chat.REFRESH_PARTICIPANTS");
		registerReceiver(myReceiver, refreshParticipantsFilter);

		setContentView(R.layout.participants);

		try{
		List<Contact> cList = cProvider.getContacts();
		List<String> cnames;
		if(cList != null){
			cnames = new ArrayList<String>(cList.size());
			for(Contact c: cList){
				cnames.add(c.name);
			}
		}else{
			cnames = null;
		}		
		
		String[] names = chatClientInterface.getParticipantNames();
		if(names != null && cnames != null){
			for(int i=0; i<names.length; i++){
				if (cnames.contains(names[i]))
					names[i] += "[Y]";
				else
					names[i] += "[N]";
					
			}
		}
		setListAdapter(new ArrayAdapter<String>(this, R.layout.participant,
				names));
		
		}catch(NullPointerException e){
			logger.log(Level.INFO, "Name check: " +e.getLocalizedMessage());
		}
		

		ListView listView = getListView();
		listView.setTextFilterEnabled(true);
		listView.setOnItemClickListener(listViewtListener);
	}

	private OnItemClickListener listViewtListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {

			ListAdapter la = getListAdapter();
			String newname = la.getItem(position).toString();
			//See if existing
			String exists = newname.substring(newname.length()-3,newname.length());
			//remove last 3 chars
			newname = newname.substring(0,newname.length()-3);

			if(exists.equalsIgnoreCase("[N]")){
	
				//Code based off of:
				// http://developer.android.com/guide/topics/providers/contacts-provider.html
			    // Creates a new array of ContentProviderOperation objects.
			    ArrayList<ContentProviderOperation> ops =
			            new ArrayList<ContentProviderOperation>();

			    Account mSelectedAccount = new Account(newname, newname);
			    ContentProviderOperation.Builder op =
			            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
			            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, mSelectedAccount.type)
			            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, mSelectedAccount.name);
			    ops.add(op.build());
			    
			 // Creates the display name for the new raw contact, as a StructuredName data row.
			    op =
			            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
			            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
			            .withValue(ContactsContract.Data.MIMETYPE,
			                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
			            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newname);
			    ops.add(op.build());

			    op.withYieldAllowed(true);
			    ops.add(op.build());
			    
			    // Ask the Contacts Provider to create a new contact
			    logger.log(Level.INFO,"Selected account: " + mSelectedAccount.name + " (" +
			            mSelectedAccount.type + ")");
			    logger.log(Level.INFO,"Creating contact: " + newname);

			    try {
			            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			          //Show alert dialog instead?
			            Context ctx = getApplicationContext();
			            CharSequence txt = getString(R.string.contactCreationSuccess);
			            int duration = Toast.LENGTH_SHORT;
			            Toast toast = Toast.makeText(ctx, txt, duration);
			            toast.show();
			    } catch (Exception e) {
			            // Display a warning
			            Context ctx = getApplicationContext();
			            CharSequence txt = getString(R.string.contactCreationFailure);
			            int duration = Toast.LENGTH_SHORT;
			            
			            //Show alert dialog instead?
			            Toast toast = Toast.makeText(ctx, txt, duration);
			            toast.show();
	
			            // Log exception
			            logger.log(Level.INFO, "Exception encountered while inserting contact: " + e);
			    }
			}else{
				showAlertDialog("Contact "+newname+" already exists", false);
			}
			recreate();
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(myReceiver);

		logger.log(Level.INFO, "Destroy activity!");
	}

	private class MyReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			logger.log(Level.INFO, "Received intent " + action);
			if (action.equalsIgnoreCase("jade.demo.chat.REFRESH_PARTICIPANTS")) {
				ContactsProvider cProvider = new ContactsProvider();
				List<Contact> cList = cProvider.getContacts();
				List<String> cnames;
				if(cList != null){
					cnames = new ArrayList<String>(cList.size());
					for(Contact c: cList){
						cnames.add(c.name);
					}
				}else{
					cnames = null;
				}		
				
				String[] names = chatClientInterface.getParticipantNames();
				if(names != null && cnames != null){
					for(int i=0; i<names.length; i++){
						if (cnames.contains(names[i]))
							names[i] += "[Y]";
						else
							names[i] += "[N]";
							
					}
				}
				setListAdapter(new ArrayAdapter<String>(
						ParticipantsActivity.this, 
						R.layout.participant, names));
			}
			else {
				String[] s = new String[1];
				s[0] = intent.getAction();
				setListAdapter(new ArrayAdapter<String>(
						ParticipantsActivity.this, 
						R.layout.participant, s));
			}
		}
	}
	
	private void showAlertDialog(String message, final boolean fatal) {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				ParticipantsActivity.this);
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

}


