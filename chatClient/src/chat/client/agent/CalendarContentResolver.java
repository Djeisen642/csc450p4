package chat.client.agent;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Events;

/**
 * Get event availability
 * <p>
 * Will return current event status of public/private
 * 
 * Assumptions: no conflicting events, all events start after midnight
 *  (i.e. they're not multi-day events). 
 * 
 * Pseudo Source modeled after :
 *   http://stackoverflow.com/questions/5883938/getting-events-from-calendar
 *
 * @param  ctx  application context
 * @return 
 */
@SuppressLint("SimpleDateFormat")
public class CalendarContentResolver { 
	 
	  ContentResolver contentResolver;
	 
	  public  CalendarContentResolver(Context ctx) {
	    contentResolver = ctx.getContentResolver();
	  } 
	  
	  /**
	   * Get current availability
	   * <p>
	   * Will return current event status of public/private
	   * 
	   * Assumptions: no conflicting events, all events start after midnight
	   *  (i.e. they're not multi-day events). 
	   * 
	   * Pseudo Source modeled after :
	   *   http://stackoverflow.com/questions/5883938/getting-events-from-calendar
	   *
	   * @param  context  application context
	   * @return ACCESS_LEVEL (0,1 == server default, 2 == private, 3 == public)
	   */
	  public static int findAvailability(Context context) {
		  
	    int retval = 2;	//return value, private default

        ContentResolver contentResolver = context.getContentResolver();
        Calendar calendar = Calendar.getInstance();
        String dtstart = "dtstart";
        String dtend = "dtend";  

        SimpleDateFormat startFormatter = new SimpleDateFormat("MM/dd/yy");
        String dateString = startFormatter.format(calendar.getTime());

        long nowt = calendar.getTimeInMillis();
        SimpleDateFormat formatterr = new SimpleDateFormat("hh:mm:ss MM/dd/yy");
        Calendar endOfDay = Calendar.getInstance();
        Calendar startOfDay = Calendar.getInstance();
        Date dateCCC;
		try {
			dateCCC = formatterr.parse("23:59:59 " + dateString);
			endOfDay.setTime(dateCCC);
			dateCCC = formatterr.parse("00:00:00 " + dateString);
			startOfDay.setTime(dateCCC);
		} catch (java.text.ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        Cursor cursor = contentResolver.query(Uri.parse("content://com.android.calendar/events"),
        		(new String[] { "calendar_id", "title", "description", "dtstart", "dtend","eventTimezone",
        				        "eventLocation", Events.ACCESS_LEVEL, Events.ALLOWED_AVAILABILITY }),
        		"(" + dtstart + ">" + startOfDay.getTimeInMillis() + " and " + dtend + "<" + endOfDay.getTimeInMillis() + ")", null, "dtstart ASC");

        try {
            if (cursor.getCount() > 0) {
            	int counts = cursor.getCount();
            	System.out.println(counts);
            	String[] colnames = cursor.getColumnNames();
            	String[] colvals = null;
            	if(colnames != null)
            		colvals = new String[colnames.length];
            	
                while (cursor.moveToNext()) {                	
            		for(int i=0; i<colnames.length; i++){
            			colvals[i] = cursor.getString(i);
            			System.out.println(colnames[i] + ": " + colvals[i]);
            		}
            		if(Long.parseLong(colvals[3]) < nowt && nowt < Long.parseLong(colvals[4])){
        			//We're currently in the event
            			//Column index 7 == access level, keys:
                		// 0,1 default
                		// 2 private
                		// 3 public
            			retval = Integer.parseInt(colvals[7]);
            		}
                }
            }
        } catch (AssertionError ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retval;
    }
}