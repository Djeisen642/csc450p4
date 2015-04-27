CSC450 Project 4b: Ring Manager Agent
Team:
Taylor Deckard (stdeckar)
Aaron Mahler (almahler)
Jason Suttles (jssuttle)

Assumptions:
All events will start after midnight and not last past midnight so
that they are not multi-day events. There will be no conflicting
events that occur at the same time in the calendar.  Whether to share
a location will be based upon the availability of the event (not the
visibility).

--How to test scenarios--
Create two android emulators running Android Lollipop.
Start them both.
Let's call them emulator 1 which should be running on port 5554 and emulator 2 which should be running on port 5556.
On emulator 1, create a contact named "Sam" with phone number 5556667777.
Send a geo location of lat 20 and long 20.
On emulator 2 send a geo location of lat 0 and long 0.
Go into Run Configurations -> Target -> Launch on all compatible devices.
Run it.
To set urgency of a call, before "calling" type in "Agent: Important" or "Agent: Casual".
e.g. For Sam to call emulator 1 with an important call, type in "Agent: Important" and, on emulator 1, call 5556667777.
That should create a notification on emulator 1 notifying the user of the important missed call.
For Sam to call emulator 1 with a casual call, type in "Agent: Casual" and, on emulator 1, call 5556667777.
(Every call is marked as casual by default. So, "Agent: Casual" is unnecessary.)
(Agents are set as available unless there is an event that's on the calendar that sets the user as busy. Read below for instructions on creating a calendar event.)
So, since both are private, they won't check each others locations.
If emulator 1 is public but 2 is private, it will send emulator 2 a location check and emulator 2 won't respond.
If emulator 1 and 2 are public, emulator 1 will send emulator 2 a location check. Emulator 2 will respond with a location. Emulator 1 will receive it and check the distance. If within 50m, emulator 1 will notify the user of the missed casual call and the proximity.
We set the distance to greater than 50m, so the notification won't be created. Send a geo location of lat 20 and long 20 to emulator 2. The notification should then pop up.

If more than 3 missed calls are made to a user, the application will notify the caller that the user has misplaced their phone. For this to function as intended, a user account with an email has to be set up on the emulator. These steps can be taken to test that the notifications are functioning: 
* On an emulator (emulator1), go to Settings => Add account.
* Enter all required info. 
* Create a contact named "Andy" with phone number 3334445555.
* Run the chatClient application.
* Enter a nickname to enter the chatroom (nickname here can be anything).
* Open another emulator (emulator2) and run chatClient. 
* Enter the nickname "Andy" and enter the chatroom.
* Now place 3 calls with DDMS to the emulator1 using incoming number 3334445555, rejecting the call on the emulator each time.
* On the third rejection, emulator2 should receive a notification mentioning the email address you entered for the account you created on emulator1. 

In order to add a calendar (only works with NCSU gmail apparently)
* Open up the calendar
* Attempt to add an entry
* You will get a pop up requiring to add calendar - click ok
* Enter your ncsu email address and password and click next
* After the server retrieves your info, change domain to google\<your ncsu unity id>@ncsu.edu and Password to your ncsu password.
* Change the server to m.google.com
* Scroll down and click next.
* A Remote Security notification displays - click okay
* Select the items you want to sync and click next
* Enter a name for your calendar and click next
* You should receive a prompt to activate device administration. If you
don't see it check your notifications. Click Activate.

Now to check calendar interface make an event that is currently going
on during the check.  It will only check back to midnight for the
current day and it will only check 1 event if they're conflicting.

