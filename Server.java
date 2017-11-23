// (C) Oleg Petcov C14399846
// 23 November 2017

/*
*	This Class contains Logic to allow for multiple Clients to connect to the Server.
*
*
*	It will Compute any valid bids they offer, and will update the bid if it is improved,
*	Will then output this changed info to all Clients when the bid is changed or the Timer is up.
*
*
*	Handles some of the ID getting and Setting for the user.
*
*
*	Will gracefully remove Clients that disconnect from the Server.
*
*
*	Makes use of Timers to display duration of bidding process (1 Minute is max default).
*
*
*	Stores winning bet data in a text file (winning_bids.txt).
*
*
*/


import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;
import java.math.BigDecimal;

public class Server{
	
	// Socket and port number
	private static ServerSocket serverSocket;
	private static int PORT;
	
	//Timer
	private static Timer timer;
	
	//Cleint info
	private static int ID = 1;
	private static Vector<Socket> clients;
	
	// Items lists
	private static ArrayList<Item> items = new ArrayList<Item>();
	private static ArrayList<Item> soldItems = new ArrayList<Item>();
	
	//Timing stuff
	private static final int TIME_0 = 0;
	private static final int TIME_10SEC = 50000;
	private static final int TIME_15SEC = 45000;
	private static final int TIME_30SEC = 30000;
	private static final int TIME_1MIN = 60000;
	
	// Item and Timer controls
	private static boolean timerRestart = false;
	private static boolean offered = true;
	private static boolean itemsLeft = false;
	private static boolean waiting = true;
	
	// Item details
	private static int itemPos = 0;
	private static int itemAmount = 0;
	private static String itemName = "";
	private static BigDecimal itemPrice = new BigDecimal(-1);
	private static String currency = "Euro";
	
	// Item detail Strings
	public static String itemForSaleMsg = "The item for sale: " + itemName + "\n";
	public static String itemPriceMsg = "The price for this item: " + itemPrice + " " + currency;
	public static String newItemPriceMsg = "The new highest bid for this item: " + itemPrice + " " + currency;
	
	// Messages sent to user / Client
	public static String connectMsg = "Welcome to the Auction Server";
	public static String startingMsg = "Starting a new auction!";
	public static String ongoingMsg = "There is currently an auction on going!";
	public static String waitingMsg = "Waiting for more users.";
	public static String noneMsg = "No items are currently up for auction.";

	// Highest Bidder info
	private static boolean bidOn = false;
	private static int userhighest = 0;
	private static String username = "";
	private static BigDecimal bidhighest = new BigDecimal(-1);
	private static String itemhighest = "";
	
	// Write to Client socket
	private static PrintWriter output;
	

	// Writes winning bids to text file.
	// Notifies all Clients of the Winnig bid after this.
	public static void writeBidFile() throws IOException {
		
		String fDir = "./";
		String fName = "winning_bids.txt";
		String fPath = fDir + fName;
		
		File file = new File(fPath);
		
		if(!file.exists()){
			file.createNewFile();
		}
		
		FileWriter fw = new FileWriter(file, true);
		PrintWriter pw = new PrintWriter(fw);
		
		pw.print("UserID: " + userhighest + "\tUsername: " + username + "\t Item: " + itemhighest + "\t Bid: " + bidhighest + " " + currency + "\r\n");
		pw.close();
		
		broadcastWin();
	}
	
	
	// broadcast winning bid
	public static void broadcastWin(){
		for(Socket cli: clients)
		{
			try{
				output = new PrintWriter(cli.getOutputStream(),true);
				output.println("\n\nWinning Client (" + username + ") with ID: '" + userhighest + "'");
				output.println("For Item: '" + itemhighest + "' at Price: " + bidhighest + " " + currency + "\n\n");
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
	
	// Broadcast message to all clients
	public static void broadcastAll(String message){

		for(Socket cli: clients)
		{
			try{
				output = new PrintWriter(cli.getOutputStream(),true);
				output.println("ALL NOTICE: " + message + "\n");
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
		// Broadcast bid to all clients, and the highest bidder
	public static void broadcastBid(String message, Socket highestBidder){
		
		String winner = "You are the highest bidder!\n";
		
		for(Socket cli: clients)
		{
			if(cli != highestBidder)
			{
				try{
					output = new PrintWriter(cli.getOutputStream(),true);
					output.println("BID NOTICE: " + message + "\n");
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
		
		try{
			output = new PrintWriter(highestBidder.getOutputStream(),true);
			output.println("BIDDER NOTICE: " + winner);
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
	// Will write bid info to the text file,
	// If there is a winner.
	//
	// Resets bid info after this.
	//
	// Offers new items for bidding (if any left)
	//
	// Else, add unbid-on item to end of list, and try again later
	public static void bidFinished(){
		
		if(bidOn){
			try{
				writeBidFile();
				itemAmount--; // Only decrements when something has been bid on
			} catch(IOException IOEx){
				IOEx.printStackTrace();
			}
		} else {
			broadcastAll("No Bids for: " + itemName + ". Will be up for Bidding later. \n");
			Item it = new Item(items.get(itemPos).getName(),items.get(itemPos).getPrice());
			items.add(it);
		}
		
		timerRestart = true;
		itemPos++;
		bidOn = false;
		resetBid();				
		offerItems();
	}
	
	
	// Timer logic for the Server
	public static void startTimer(){
		
		timer = new Timer("aucTimer");
		
		// Starts immediately, just notifies the Server watcher
		timer.schedule(new TimerTask(){
			@Override
			public void run()
			{
				System.out.println("START TIMER\n");
			}
			
		}, TIME_0);
		
		
		// Timer task set to one minute (in milliseconds).
		timer.schedule(new TimerTask(){
			
			@Override
			public void run()
			{
				bidFinished();
			}
			
		}, TIME_1MIN);
		
		
		// Timer task set to 30 seconds (in milliseconds)
		timer.schedule(new TimerTask(){
			
			@Override
			public void run()
			{
				System.out.println("30 SECONDS LEFT\n");
				broadcastAll("30 SECONDS LEFT\n");
			}
			
		}, TIME_30SEC);
		
		
		// Timer task set to 15 seconds (in milliseconds)
		timer.schedule(new TimerTask(){
			
			@Override
			public void run()
			{
				System.out.println("15 SECONDS LEFT\n");
				broadcastAll("15 SECONDS LEFT\n");
			}
			
		}, TIME_15SEC);
		
		
		// Timer task set to 10 seconds (in milliseconds)
		// Will Loop second-by-second, with sleep()
		timer.schedule(new TimerTask(){
			
			@Override
			public void run()
			{
				System.out.println("10 SECONDS LEFT\n");
				for(int i = 10;i > 0; i--){
					try{
						//System.out.println("\r" + i + " SECONDS LEFT\n");
						broadcastAll( i + " SECONDS LEFT");
						Thread.sleep(1000);
					} catch (Exception ex){
						ex.printStackTrace();
					}
				}
				System.out.println("\n***BIDDING FINISHED!*** \n");
				broadcastAll( "\n***BIDDING FINISHED!*** ");
				broadcastAll( "\n***Please wait for more information*** ");
			}
			
		}, TIME_10SEC);
	}
	
	
	// Removes client after they disconnect
	public static void removeClient(Socket disconnClient){
		Iterator<Socket> s;
		
		if(clients != null && clients.size() > 0){
			s = clients.iterator();
			
			while(s.hasNext()) {
				Socket cli = s.next();
				
				if(cli == disconnClient){
					s.remove();
					break;
				}
			}
		} 
		else {
			System.out.println("Trying to remove client from clientlist that is empty.\n");
		}

		if(clients.size() == 0){
			System.out.println("\nRESETTING BIDDING PROCESS\n");
			timer.cancel();
			timerRestart = false;
			resetBid();
			resetItem();
			bidOn = false;
		}
		
		System.out.println("\n**Clients left:" + clients.size() + "**\n");
	}
	
	
	// Sends welcome message to new user connection
	public static void welcome(Socket newConn){
		ClientHandler newCH = new ClientHandler(newConn);
		
		String welcomeMsg = "Welcome to the Auction Server!";
		newCH.sendMsg(welcomeMsg);
		newCH = null;
	}
	
	
	// Reset bid on item information
	public static void resetItem(){
		itemName = "";
		itemPrice = new BigDecimal(-1);
		updateItem(itemName, itemPrice);
	}
	
	
	// Updates the item for sale and it's bid price
	public static void updateItem(String n, BigDecimal p){
		itemForSaleMsg = "The item for sale: " + n + "\n";
		itemPriceMsg = "The price for this item: " + p + " " + currency;
		newItemPriceMsg = "The new highest bid for this item: " + p + " " + currency;
	}
	
	
	// Used to import items from a user chosen file,
	// or from a default data file
	public static void importItems() throws IOException, ClassNotFoundException {
		
		// NOT FULLY MY OWN CODE
		// USED THIS FOR GUIDANCE
		//
		// https://stackoverflow.com/questions/10083447/selecting-folder-destination-in-java
		// https://stackoverflow.com/questions/10621687/how-to-get-full-path-directory-from-file-chooser
		//
		//
		//Choose a file with Items in it, otherwise, read / create 'BidItems.dat'
		// Used a hardcoded itemlist eitherway
		/*
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("./"));
        chooser.showSaveDialog(null);
		
		String filepath = "";
	
		if(chooser.getSelectedFile() != null){
			File f = chooser.getSelectedFile();
			filepath = f.getCanonicalPath();
		} 
		else {
			filepath = "BidItems.dat";
		}
		
		ObjectOutputStream bidItemOutStream = 
			new ObjectOutputStream(new FileOutputStream(filepath));
		*/
		
		Item[] its = {
			new Item("Van Gogh", new BigDecimal("123456.78")),
			new Item("Sticky Shoe", new BigDecimal("999.00")),
			new Item("Biro Pen", new BigDecimal("1.00")),
			new Item("Iphone 7", new BigDecimal("499.99")),
			new Item("Assortment of junk in a box", new BigDecimal("100.00"))
		};
		
		// Add to AList
		for (Item it: its){
			items.add(it);
		}
		
		// Used to count amount of items in list
		// Doesn't count re-added items
		itemAmount = items.size();
		
		/*
		bidItemOutStream.writeObject(items);
		bidItemOutStream.close();
		*/
		
		
		// TEST CODE
		// Unused test code for reading item file
		/*
		ObjectInputStream bidItemInStream = 
			new ObjectInputStream(new FileInputStream(filepath));
				
		
		try {
			
			//itemListIn = (ArrayList<Item>)bidItemInStream.readObject();
			items = (ArrayList<Item>) bidItemInStream.readObject();
			
			for (Item it:items)
			{
				System.out.println("\nItem for Bid: " + it.getName());
				System.out.println("Item Bid Price: " + it.getPrice());
				System.out.println("\n");
			}
		} catch (EOFException eofEx)
		{
			System.out.println("\n\n*** End of file ***\n");
			bidItemInStream.close();
		}
		*/
	}
	
	
	
	// Offers items for bidding purposes to all users
	// Adds new items up for bidding.
	public static void offerItems(){
		
		// If restarting the timer for a new higher bid, or ending of bidding
		if(timerRestart){
			timer.cancel();
			timerRestart = false;
			
			// Sleeps to let all user messages catch up
			try{
				Thread.sleep(4000);
			} catch (Exception ex){
				ex.printStackTrace();
			}
		}
		
		// If there's more than 2 clients, start auctions
		if(clients.size() < 2){
			broadcastAll("\n\n***Waiting for more users to login.***\n\n");
		} else{
			
			// If there's stuff to sell, then update
			// otherwise, don't update and proceed to exit
			if(!items.isEmpty() && itemPos < items.size()){
				
				itemName = items.get(itemPos).getName();
				itemPrice = items.get(itemPos).getPrice();
				itemsLeft = true;
				updateItem(itemName,itemPrice);
			} else {
				itemsLeft = false;
				System.out.println("No items left for auction\n");
			}
			
			
			if(itemsLeft){
				System.out.println("\n**Items left:" + itemAmount + "**\n");
				String newIt = "New item for Auction!\n";
				String broadNewItem = newIt + itemForSaleMsg + itemPriceMsg;
				
				broadcastAll(broadNewItem);
				startTimer();
			} else {
				broadcastAll("No items left for auction.");
				System.exit(1); // exit program
			}
		}
	}
	
	
	// Resets highest bidder info
	public static void resetBid(){
		userhighest = -1;
		username = "";
		bidhighest = new BigDecimal(-1);
		itemhighest = "";
	}
	
	
	// Set ID and username of current highest bidder
	public static void setBidID(String winnerID){
		
		String[] splitID = winnerID.split("|");
		
		userhighest = Integer.parseInt(splitID[0]);
		StringBuilder userN = new StringBuilder();
		
		for(int c = 2; c < splitID.length; c++){
			userN.append(splitID[c]);
		}

		username = userN.toString();
		
		System.out.println("Highest bidder Info. UserID: (" + userhighest + ") USER:" + username + "\n");
	}
	
	
	// Update current highest bidder info
	public static void updateBidder(ClientHandler bidder, BigDecimal bid, String item){
		bidder.getID("GETUID::");
		bidhighest = bid;
		itemhighest	= item;
	}
	
	
	// Compares bid offer and highest / reserve bid price
	public static void computeBid(BigDecimal newBid, Socket newestBidder){
		
		ClientHandler bidder = new ClientHandler(newestBidder);	
		
		if(itemsLeft){
			//continue;
		} 
		else {
			bidder.sendMsg("No items left for bidding, please try again later.");
			return;
		}
		
		// This lets you place a bid on the asking price initially.
		// Thereafter will require a new higher (not matching price), 
		// in order to win.
		boolean wonBid = false;
		
		int compare;
		compare = newBid.compareTo(itemPrice);
		
		if(!bidOn)
		{
			// If item has not been bid on before (reserve price),
			// if equal to, or greater than current item price
			if(compare == 0){
				wonBid = true;
			} else if (compare == 1){
				wonBid = true;
			}			
		} else {
			// If item has been bid on before, and higher than current bid
			// then update bid to newest offer
			if(compare == 1){
				wonBid = true;
			}
		}
		
		// If bid is greater than equal to the previous bid
		// Update the bid info, and reset the timer
		if(wonBid) {
			
			System.out.println("\nPrevious itemPrice: " + itemPrice + "\n");
			System.out.println("New itemprice: " + newBid + "\n\n");
			
			// Item has been bid on, no longer offering reserve price
			bidOn = true;
			
			itemPrice = newBid;
			updateItem(itemName,itemPrice);

			broadcastBid(newItemPriceMsg, newestBidder);

			// Updates bidder information and item information
			updateBidder(bidder,itemPrice, itemName);
			
			// Reset timer to 1 minute
			timer.cancel();
			startTimer();
			
			bidder = null;
			
		} else if(!wonBid) {
			
			String badBid = "";
			
			if(bidOn)
			{
				badBid = "Bid is not higher than current price of:" + itemPrice + " " + currency + ".\n";
			} else {
				badBid = "Bid must match or be higher than current price of:" + itemPrice + " " + currency + ".\n";
			}
			
			try{
				output = new PrintWriter(newestBidder.getOutputStream(),true);
				output.println("LOW BID NOTICE: " + badBid + "\n");
			} catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
	
	// Main method
	// Listens for Clients connections.
	// Assigns user ID's, and sends messages to the new user connection
	public static void main(String[] args) throws IOException {
		try {
			if(args.length == 1){
				clients = new Vector<Socket>();
				PORT = Integer.parseInt(args[0]);
				serverSocket = new ServerSocket(PORT);
				
				// NEED TO CLEAN UP ALL THE STUPID EXCEPTIONS AND TRY's**************************
				try {
					importItems();
				} catch(Exception ex){}
			} else {
				System.out.println("\n Incorrect entry of port number.\n");
			}
		}
		catch (IOException ioEx){
			System.out.println("\nUnable to set up port!");
			System.exit(1);
		}
		
		System.out.println("\nServer Started\n");
		
		do 
		{
			Socket client = serverSocket.accept();

			//Add client to list
			clients.add(client);
			System.out.println("\nNew client accepted: (" + ID + ")\n");
			
			//Create a thread to handle communication with this client.
			// Sends a welcome message to client.
			ClientHandler handler = new ClientHandler(client);
			handler.start();
			
			welcome(client);
			
			// Give the Client an ID number
			// Pass using the ClientHandler Thread
			String IDCONF = "IDCONF::"+ID;
			handler.sendID(IDCONF);
			ID++;
			
			
			// If two clients are connected here, 
			// start auction cycle.
			// Send message when offerItems() is called
			if(clients.size() == 2) {
				handler.sendMsg(ongoingMsg);
				
				// If waiting on a new connection, start new offer.
				// Else, a user abondoned the auction,
				// and a new 2nd user connected after they left
				if(waiting){
					offerItems();	
					waiting = false;
				} else {
					if(itemsLeft) {
						String currentlySelling = itemForSaleMsg + itemPriceMsg;
						handler.sendMsg(currentlySelling);
					} else if (!waiting){
						handler.sendMsg(noneMsg);
					}
				}
				
			}
			
			
			// Sends individual message to new client connection
			else if(clients.size() >= 3) {
				waiting = false;
				handler.sendMsg(ongoingMsg);
				
				// If there are items left for sale
				// else no items to bid on
				if(itemsLeft) {
					String currentlySelling = itemForSaleMsg + itemPriceMsg;
					handler.sendMsg(currentlySelling);
				} else if (!waiting){
					handler.sendMsg(noneMsg);
				}
			} 
			
			// If only 1 client, 
			// Will be waiting for moe connections
			else {
				waiting = true;
				handler.sendMsg(waitingMsg);
			}

			handler = null;
			
		} while (true);
	}
}
