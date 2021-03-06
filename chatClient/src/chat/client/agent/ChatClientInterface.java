package chat.client.agent;

/**
 * This interface implements the logic of the chat client running on the user
 * terminal.
 * 
 * @author Michele Izzo - Telecomitalia
 */

public interface ChatClientInterface {
	public void handleSpoken(String s);
	public void handleSpokenTo(String name, String s);
	public String[] getParticipantNames();
	public String[] leftNames();
	public String[] joinNames();
}