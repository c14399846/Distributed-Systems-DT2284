// (C) Oleg Petcov C14399846
// 23 November 2017

/*
* Client Class
*
* Sets up initial contact with the Server.
*
* After this will loop user entry for bids.
*
* Contains methods for getting and setting ID,
* All the logic is handled in Server, and ServerListener + ClientHandler Threads
*
*
*/



import java.io.*;
import java.net.*;
import java.util.*;

public class Client
{
	private static int PORT;
	private static InetAddress host;
	private static PrintWriter networkOutput = null;
	private static String userName = "";
	private static int myID;
	
	
	public static void main(String[] args)
	{
		try
		{
			if(args.length == 2){
				String hostname = args[0];
				host = InetAddress.getByName(hostname);
				
				PORT = Integer.parseInt(args[1]);
				//userName = args[2];
				
				//System.out.println("Connection details: " + hostname + ", " + PORT + ", "
				//+ userName + "\n");
			} else if(args.length == 3){
				String hostname = args[0];
				host = InetAddress.getByName(hostname);
				
				PORT = Integer.parseInt(args[1]);
				userName = args[2];
			} else {
				System.out.println("\n Incorrect entry of arguments for:Hostname, port number, or username\n");
			}
		}
		catch(UnknownHostException uhEx)
		{
			System.out.println("\nHost ID not found!\n");
			System.exit(1);
		}
		
		sendMessages();
	}

	
	// Used by Server to get highest bidder ID
	public static void getID(){
		networkOutput.println("RETUID::" + myID  + "|" + userName);
	}
	
	
	// Sets user ID on creation of ClientHandler thread
	public static void setID(int ID){
		myID = ID;
	}
	
	
	// Sends user input to the Server
	private static void sendMessages()
	{
		Socket socket = null;
		
		Scanner userEntry = new Scanner(System.in);
		
		
		if(userName.isEmpty()){
			System.out.print("Please enter your username:");
			do{
				userName = userEntry.nextLine();
			} while (userName.isEmpty());
			
			System.out.print("\r\n");
		}
		
		
		try
		{
			socket = new Socket(host,PORT);
			
			// Socket used to communicate with the Server
			networkOutput = new PrintWriter(socket.getOutputStream(),true);

			// Listens to outputs from Server
			ServerListener listener = new ServerListener(socket);
			listener.start();
			
			String message, response;
			
			System.out.print("Enter Bid Amount ('QUIT' to exit): ");
			
			do
			{
				// User can enter bids or 'QUIT' / 'help' here
				message = userEntry.nextLine();
				
				if(message.equals("help")){
					System.out.print("\n HELP: Can enter bids in decimal format (12.99) \n");
					System.out.print("Can also enter 'QUIT' to exit the program\n");
				} else if(message.equals("QUIT")) {
					break;
				} else {
					// Output to Server
					networkOutput.println(message);
				}
			} while (!message.equals("QUIT"));
		} catch(IOException ioEx) {
			ioEx.printStackTrace();
		}

		finally
		{
			try
			{
				// Closes socket for the Server and Client
				networkOutput.println("QUIT");
				System.out.println("\nClosing connection...");
				networkOutput.close();
				socket.close();
				System.exit(1);
			} catch(IOException ioEx) {
				System.out.println("Unable to disconnect!");
				System.exit(1);
			}
		}
	}
}
