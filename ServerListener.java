// (C) Oleg Petcov C14399846
// 23 November 2017

/*
* Thread used by Client to listen to any Server messages.
*
* Does basic checking for Setting and Gettign ID
* ( IDCONF::[ID] and GETUID::[ID] )
*
* Displays messages that are not related to ID stuff, like Timings.
*
*/


import java.io.*;
import java.net.*;
import java.util.*;

class ServerListener extends Thread{
	Socket socket;
	public ServerListener(Socket socket){
		this.socket = socket;
	}

	public void run(){
		try{
			Scanner networkInput = new Scanner(socket.getInputStream());
			String response = "";
			
			while(!Thread.currentThread().isInterrupted()){
			
				boolean idCheck = false;
				boolean closeCheck = false;
			
				response = networkInput.nextLine();
				
				// If received command to close Server comms
				if(response.equals("CLOSECONN")){
					closeCheck = true;
				}
				
				// If not closing a connection,
				// Check for ID Setting / Getting Requests:
				// ( IDCONF::[ID] and GETUID::[ID] )
				if(!closeCheck && response.length() > 6){
					StringBuilder check = new StringBuilder();
					
					for(int i = 0; i < 6; i++){
						check.append(Character.toString(response.charAt(i)));
					}
					
					if(check.toString().equals("IDCONF")){
						String[] ID = response.split("::");
						String msgID = "Your ID is: '" + ID[1] + "'";
						idCheck = true;
						System.out.println(msgID + "\n");
						Client.setID(Integer.parseInt(ID[1]));
						
					} else if(check.toString().equals("GETUID")) {
						Client.getID();
						idCheck = true;
					}
				}
				
				
				// If closing the connection, stop thread.
				//
				// Else, if not checking ID, print out Server messages
				if(closeCheck){
					System.out.println("\nConnection Closed. Thank you for auctioning!\n");
					Thread.currentThread().interrupt();
					break;
				} else if(!idCheck) {
					if(response.equals("")) {
						System.out.println("\n");
						sleep(1000);
					} else {
						System.out.println("\nSERVER> " + response);
						sleep(1000);
					}
				}
			}
			
			socket.close();
			
		} catch(IOException ioEx){
			System.out.println("\n\nServer has disconnected.\n\n");
			
			try{
				Thread.sleep(3000);
			} catch (Exception ioInner){
				//ioInner.printStackTrace();
			}
			
			Thread.currentThread().interrupt();
			System.exit(1);
			//ioEx.printStackTrace();
			
		} catch(InterruptedException inEx) {
			Thread.currentThread().interrupt();
			//inEx.printStackTrace();
			
		} catch(Exception ex) {
			System.out.println("\n\nServer has disconnected.\n\n");
			
			try{
				Thread.sleep(3000);
			} catch (Exception exInner) {
				//exInner.printStackTrace();
			}
			
			Thread.currentThread().interrupt();
			System.exit(1);
		}
	}
}
