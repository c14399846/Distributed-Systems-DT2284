// (C) Oleg Petcov C14399846
// 23 November 2017

/*
* Used by Server to treat each CLient connection as its own Thread.
*
* Handles basic Bid parsing.
*
* Used as a waypoint between the CLient and Server when sending messages.
*
*/


import java.io.*;
import java.net.*;
import java.util.*;
import java.math.BigDecimal;

class ClientHandler extends Thread{
	private Socket client;
	private Scanner input;
	private PrintWriter output;
	private boolean conn;
	public boolean idCheck;


	public ClientHandler(Socket socket){
		client = socket;
		conn = true;
	
		try{
			input = new Scanner(client.getInputStream());
			output = new PrintWriter(client.getOutputStream(),true);
		}
		catch(IOException ioEx)
		{
			ioEx.printStackTrace();
		}
	}

	
	// Checks that the value entered is a BigDecimal datatype
	// Rounds value down to 2 decimal places
	public void makeBid(String message){
		
		if(message.matches("^[0-9]*\\.?[0-9]*$")) {
			try{
				//double messageNum = Double.parseDouble(message);
				BigDecimal messageNum = new BigDecimal(message).setScale(2, BigDecimal.ROUND_DOWN);
				Server.computeBid(messageNum, client);
			}
			catch(NumberFormatException numForEx){
				this.sendMsg("Not a correct number format.\n");
			}
		} else {
			this.sendMsg("Must be a number value\n");
		}
		
	}
	
	
	// Removes client from Server list
	public void removeClient(){
		Server.removeClient(client);
		conn = false;
	}
	
	
	// Runs the user bid entry logic, 
	// and logic for ID setting
	public void run(){
		
		String received;
		do {
			
			// If the user uses 'ctrl+c' or a failure occurs,
			// will remove the client from the server list, and will gracefully disconnect
			if(Thread.currentThread().isInterrupted()){
				removeClient();
				break;
			}
			
			try{
				received = input.nextLine();
			} catch(Exception ex){
				System.out.println("\nUser DC\n");
				removeClient();
				break;
			}

			// If looking for user id,
			// instead of inputting new bid.
			idCheck = false;
			String pI = parseInput(received);

			// If Server is requesting ID, send ID.
			// Else, parse other user entries
			if(idCheck) {
				Server.setBidID(pI);
			} else {
				if (received.equals("QUIT")) {
					try {
						this.sendMsg("CLOSECONN");
						
						// Remove client from server list after disconnecting it
						Server.removeClient(client);
						conn = false;
						break;
					} catch(Exception ex) {
						System.out.println("Error closing Socket " + ex + "\n");
					}
				}
				
				// if not quitting
				// attempt to make a bid 
				else if(!received.isEmpty()) {
					try{
						makeBid(received);
						sleep(1000);
					} catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		} while(conn);

		
		try{
			if (client != null){
				System.out.println("Closing down connection...");
				client.close();
			} 
			if (input != null){
				input.close();
			} 
			if (output != null){
				output.close();
			}
			
			Thread.currentThread().interrupt();
		}
		catch(IOException ioEx){
			System.out.println("Unable to disconnect!");
		}
	}
	
	
	// Goes through user entered string to check for RETUID.
	// Will return the user ID and username if found,
	// Otherwise will return the 'in' String
	public synchronized String parseInput(String in){
		
		//System.out.println("in: " + in + "\n");
		
		if(in.length() > 6){
			StringBuilder check = new StringBuilder();
			
			for(int i = 0; i < 6; i++){
				check.append(Character.toString(in.charAt(i)));
			}
			
			if(check.toString().equals("RETUID")){
				String[] ID = in.split("::");
				idCheck = true;
				return ID[1];
			} else {
				return in;
			}
		} else {
			return in;
		}
	}
	
	
	
	// Stuff to get ID for User / Client
	public synchronized void getID(String get){
		sendMsg(get);
	}
	
	
	// Assigns User ID
	public synchronized void sendID(String ID){
		sendMsg(ID);
	}
	
	
	// Sends message to that individual User / Client
	public void sendMsg(String msg){
		try{
			synchronized(this){
				output = new PrintWriter(client.getOutputStream(),true);
				output.println(msg);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
