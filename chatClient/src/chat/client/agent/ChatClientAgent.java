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

package chat.client.agent;

import java.util.List;
import java.util.logging.Level;

import jade.content.ContentManager;
import jade.content.Predicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.Iterator;
import jade.util.leap.Set;
import jade.util.leap.SortedSetImpl;
import chat.client.gui.R;
import chat.ontology.ChatOntology;
import chat.ontology.Joined;
import chat.ontology.Left;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;

/**
 * This agent implements the logic of the chat client running on the user
 * terminal. User interactions are handled by the ChatGui in a
 * terminal-dependent way. The ChatClientAgent performs 3 types of behaviours: -
 * ParticipantsManager. A CyclicBehaviour that keeps the list of participants up
 * to date on the basis of the information received from the ChatManagerAgent.
 * This behaviour is also in charge of subscribing as a participant to the
 * ChatManagerAgent. - ChatListener. A CyclicBehaviour that handles messages
 * from other chat participants. - ChatSpeaker. A OneShotBehaviour that sends a
 * message conveying a sentence written by the user to other chat participants.
 * 
 * @author Giovanni Caire - TILAB
 */
public class ChatClientAgent extends Agent implements ChatClientInterface {
	private static final long serialVersionUID = 1594371294421614291L;

	private Logger logger = Logger.getJADELogger(this.getClass().getName());

	private static final String CHAT_ID = "__chat__";
	private static final String CHAT_MANAGER_NAME = "manager";

	private Set participants = new SortedSetImpl();
	private Codec codec = new SLCodec();
	private Ontology onto = ChatOntology.getInstance();
	private ACLMessage spokenMsg;

	private Context context;
	private String[] leftnames;
	private String[] joinnames;
	
	private int notifyID = 0;

	protected void setup() {
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			if (args[0] instanceof Context) {
				context = (Context) args[0];
			}
		}
		
		// Register language and ontology
		ContentManager cm = getContentManager();
		cm.registerLanguage(codec);
		cm.registerOntology(onto);
		cm.setValidationMode(false);

		// Add initial behaviours
		addBehaviour(new ParticipantsManager(this));
		addBehaviour(new ChatListener(this));
//		notifyParticipantsChanged();

		// Initialize the message used to convey spoken sentences
		spokenMsg = new ACLMessage(ACLMessage.INFORM);
		spokenMsg.setConversationId(CHAT_ID);

		// Activate the GUI
		registerO2AInterface(ChatClientInterface.class, this);
		
		Intent broadcast = new Intent();
		broadcast.setAction("jade.demo.chat.SHOW_CHAT");
		logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
		context.sendBroadcast(broadcast);
	}

	protected void takeDown() {
	}

	private void notifyParticipantsChanged() {
		Intent broadcast = new Intent();
		broadcast.setAction("jade.demo.chat.REFRESH_PARTICIPANTS");
		logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
		context.sendBroadcast(broadcast);
	}

	private void notifySpoken(String speaker, String sentence, boolean isSpeaker) {
		logger.log(Level.INFO, sentence);
		if (sentence.contains("Agent: ")) {
			if (!isSpeaker) {
				if (sentence.contains("Agent: Important")) {
					Notification notification = new Notification.Builder(context)
						.setSmallIcon(R.drawable.icon)
						.setContentTitle("Missed Important Call from " + speaker)
						.setContentText("This was a very important call from " + speaker).build();
					NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
					notificationManager.notify(notifyID++, notification);
				} else if (sentence.contains("Agent: Casual")) {
					Intent broadcast = new Intent();
					broadcast.setAction("jade.demo.chat.CASUAL");
					broadcast.putExtra("sentence", speaker);
					logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
					context.sendBroadcast(broadcast);
				} else if (sentence.contains("Agent: Check Location")){
					Intent broadcast = new Intent();
					broadcast.setAction("jade.demo.chat.CHECK_LOC");
					broadcast.putExtra("sentence", speaker);
					logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
					context.sendBroadcast(broadcast);
				} else if (sentence.contains("Agent: Location")){
					Intent broadcast = new Intent();
					broadcast.setAction("jade.demo.chat.RECEIVE_LOC");
					broadcast.putExtra("sentence", speaker + ": " + sentence + "\n");
					logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
					context.sendBroadcast(broadcast);
				} else if (sentence.contains("Agent: Urgency?")){
					Intent broadcast = new Intent();
					broadcast.setAction("jade.demo.chat.URGENCY_CHECK");
					broadcast.putExtra("sentence", speaker);
					logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
					context.sendBroadcast(broadcast);
				} else if (sentence.contains("Agent: Phone Lost")) {
					logger.log(Level.INFO, "Broadcasting phone lost to " + speaker);
					logger.log(Level.INFO, "Message: " + sentence);
					Intent broadcast = new Intent();
					broadcast.setAction("jade.demo.chat.PHONE_LOST");
					broadcast.putExtra("sentence", speaker + ":" + sentence);
					logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
					context.sendBroadcast(broadcast);
				} else {
					Intent broadcast = new Intent();
					broadcast.setAction("jade.demo.chat.REFRESH_CHAT");
					broadcast.putExtra("sentence", speaker + ": " + sentence + "\n");
					logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
					context.sendBroadcast(broadcast);
				}
			}
		} else {
			Intent broadcast = new Intent();
			broadcast.setAction("jade.demo.chat.REFRESH_CHAT");
			broadcast.putExtra("sentence", speaker + ": " + sentence + "\n");
			logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
			context.sendBroadcast(broadcast);
		}
		 
	}

	
	/**
	 * Inner class ParticipantsManager. This behaviour registers as a chat
	 * participant and keeps the list of participants up to date by managing the
	 * information received from the ChatManager agent.
	 */
	
	class ParticipantsManager extends CyclicBehaviour {
		private static final long serialVersionUID = -4845730529175649756L;
		private MessageTemplate template;
		private boolean initParticipants;

		ParticipantsManager(Agent a) {
			super(a);
			initParticipants=false;
		}

		public void onStart() {			
			// Subscribe as a chat participant to the ChatManager agent
			ACLMessage subscription = new ACLMessage(ACLMessage.SUBSCRIBE);
			subscription.setLanguage(codec.getName());
			subscription.setOntology(onto.getName());
			String convId = "C-" + myAgent.getLocalName();
			subscription.setConversationId(convId);
			subscription
					.addReceiver(new AID(CHAT_MANAGER_NAME, AID.ISLOCALNAME));
			myAgent.send(subscription);
			// Initialize the template used to receive notifications
			// from the ChatManagerAgent
			template = MessageTemplate.MatchConversationId(convId);			
		}

		public void action() {
			// Receives information about people joining and leaving
			// the chat from the ChatManager agent
			ACLMessage msg = myAgent.receive(template);
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.INFORM) {
					try {						
						Predicate p = (Predicate) myAgent.getContentManager().extractContent(msg);
						if(p instanceof Joined) {
							Joined joined = (Joined) p;
							List<AID> aid = (List<AID>) joined.getWho();
							joinnames = (!initParticipants) ? new String[aid.size()] : null;
							int i=0;
							for(AID a : aid) {								
								participants.add(a);
								if(!initParticipants)
									joinnames[i++] = a.getLocalName();								
							}
							notifyParticipantsChanged();
						}
						if(p instanceof Left) {
							Left left = (Left) p;
							List<AID> aid = (List<AID>) left.getWho();
							leftnames = (!initParticipants) ? new String[aid.size()] : null;
							int i=0;
							for(AID a : aid){
								participants.remove(a);
								if(!initParticipants)
									leftnames[i++] = a.getLocalName();	
							}
							notifyParticipantsChanged();
						}
					} catch (Exception e) {
						Logger.println(e.toString());
						e.printStackTrace();
					}
				} else {
					handleUnexpected(msg);
				}
				if(initParticipants)
					initParticipants=false;
			} else {
				block();
			}			
		}		
	} // END of inner class ParticipantsManager

	/**
	 * Inner class ChatListener. This behaviour registers as a chat participant
	 * and keeps the list of participants up to date by managing the information
	 * received from the ChatManager agent.
	 */
	class ChatListener extends CyclicBehaviour {
		private static final long serialVersionUID = 741233963737842521L;
		private MessageTemplate template = MessageTemplate
				.MatchConversationId(CHAT_ID);

		ChatListener(Agent a) {
			super(a);
		}

		public void action() {
			ACLMessage msg = myAgent.receive(template);
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.INFORM) {
					notifySpoken(msg.getSender().getLocalName(),
							msg.getContent(), false);
				} else {
					handleUnexpected(msg);
				}
			} else {
				block();
			}
		}
	} // END of inner class ChatListener

	/**
	 * Inner class ChatSpeaker. INFORMs other participants about a spoken
	 * sentence
	 */
	private class ChatSpeaker extends OneShotBehaviour {
		private static final long serialVersionUID = -1426033904935339194L;
		private String sentence;

		private ChatSpeaker(Agent a, String s) {
			super(a);
			sentence = s;
		}

		public void action() {
			spokenMsg.clearAllReceiver();
			Iterator it = participants.iterator();
			while (it.hasNext()) {
				spokenMsg.addReceiver((AID) it.next());
			}
			spokenMsg.setContent(sentence);
			notifySpoken(myAgent.getLocalName(), sentence, true);
			send(spokenMsg);
		}
	} // END of inner class ChatSpeaker

	private class DirectedChatSpeaker extends OneShotBehaviour {
		private static final long serialVersionUID = 716706562454011741L;
		private AID directedTo;
		private String sentence;
		
		private DirectedChatSpeaker(Agent a, String directedTo, String s) {
			super(a);
			this.directedTo = null;
			Iterator it = participants.iterator();
			while (it.hasNext()) {
				AID cur = (AID) it.next();
				if (cur.getLocalName().equals(directedTo)) {
					this.directedTo = cur;
				}
			}
			this.sentence = s;
		}
		
		public void action() {
			if (this.directedTo != null) {
				spokenMsg.clearAllReceiver();
				spokenMsg.addReceiver(directedTo);
				spokenMsg.setContent(sentence);
				send(spokenMsg);
			}
		}
		
	}
	
	// ///////////////////////////////////////
	// Methods called by the interface
	// ///////////////////////////////////////
	public void handleSpoken(String s) {
		// Add a ChatSpeaker behaviour that INFORMs all participants about
		// the spoken sentence
		addBehaviour(new ChatSpeaker(this, s));
	}
	
	public void handleSpokenTo(String directedTo, String s) {
		addBehaviour(new DirectedChatSpeaker(this, directedTo, s));
	}
	
	public String[] getParticipantNames() {
		String[] pp = new String[participants.size()];
		Iterator it = participants.iterator();
		int i = 0;
		while (it.hasNext()) {
			AID id = (AID) it.next();
			pp[i++] = id.getLocalName();
		}
		return pp;
	}	
	
	public String[] leftNames(){
		String[] ret;
		if(leftnames != null){
			ret = leftnames.clone();
		}else{
			ret = null;
		}
		leftnames = null;
		return ret;
	}
	
	public String[] joinNames(){
		String[] ret;
		if(joinnames != null){
			ret = joinnames.clone();
		}else{
			ret = null;
		}
		joinnames = null;
		return ret;
	}

	// ///////////////////////////////////////
	// Private utility method
	// ///////////////////////////////////////
	private void handleUnexpected(ACLMessage msg) {
		if (logger.isLoggable(Logger.WARNING)) {
			logger.log(Logger.WARNING, "Unexpected message received from "
					+ msg.getSender().getName());
			logger.log(Logger.WARNING, "Content is: " + msg.getContent());
		}
	}

}
