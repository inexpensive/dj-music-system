/************************************************************
 * Server for DJ Music Manager (tentative title)  			*
 * waits for requests from a client and passes	            *
 * them to the MusicPlayer	        						*
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/

package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import player.MusicPlayer;

public class DJServer {
	
	private ServerSocket server;
	private Socket clientSocket;
	private MusicPlayer player;
	private ObjectOutputStream outToClient;
	private ObjectInputStream inFromClient;
	private boolean done = false;
	private String command;
	private boolean paused = false;
	private boolean playlistInit = false;
	private boolean started = false;
	
	
	protected Executor pool = Executors.newFixedThreadPool(5);
	
	public DJServer() throws IOException{
		
		//set up server and allow a client to connect
		server = new ServerSocket(1729);
		clientSocket = server.accept();
		player = new MusicPlayer(this);
		outToClient = new ObjectOutputStream(clientSocket.getOutputStream());
		inFromClient = new ObjectInputStream(clientSocket.getInputStream());
		Runnable r = new Runnable(){
			@Override
			public void run(){
				try {
					listener();
				} 
				catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
			}
		};
		pool.execute(r);
	}
	
	//sends a play request to the MusicPlayer if not started and playlist is initialized
	//calls pause if the system is paused
	public void play(){
		if (!started && playlistInit) {
			player.play();
			started = true;
		}
		else if (paused){
			pause();
		}
	}
	
	//sends a pause request to the MusicPlayer	
	public void pause(){
		player.pause();
		if (paused) {
			paused = false;
		}
		else {
			paused = true;
		}
	}
	
	//sends a skip request to the MusicPlayer
	public void skip(){
		player.skip();
	}
	
	//sends a login request the the MusicPlayer
	public void login(String username, String password){
		player.login(username, password);
	}
	
	//closes the server
	public void close() throws IOException{
		server.close();
		clientSocket.close();
		System.exit(0);
	}
	
	//listens for commands from the client
	protected void listener() throws IOException, ClassNotFoundException {
		System.out.println("NOW RUNNING");
		while(!done){
			System.out.println("waiting for input...");
			command = (String) inFromClient.readObject();
			System.out.println("received " + command);
			switch (command) {
				
			//sends a play request to the MusicPlayer
			case "play":
				this.play();
				break;
					
			//sends a pause request to the MusicPlayer	
			case "pause":
				this.pause();
				break;
					
			//sends a skip request to the MusicPlayer
			case "skip":
				this.skip();
				break;
					
			//sends the login credentials to the MusicPlayer 
			//and sends back an all good message to the client	
			case "login":
				String username = (String) inFromClient.readObject();
				String password = (String) inFromClient.readObject();
				outToClient.writeObject("yes");
				this.login(username, password);
				break;
					
			//sends the search target string to the MusicPlayer
			//and sends the results to the client
			case "search":
				String target = (String) inFromClient.readObject();
				String[] results = player.search(target);
				outToClient.writeObject(results);
				break;
					
			//sends the taken in index to the MusicPlayer and initializes playlist boolean if not initialized
			case "add":
				int index = ((Integer) inFromClient.readObject()).intValue();
				player.addSong(index);
				if (!playlistInit) {
					playlistInit = true;
				}
				break;
					
			//closes the server
			case "close":
				this.close();
				done = true;
				break;
			}
		}
		
	}
	
	//start the server!
	public static void main(String[] args) throws IOException{
			new DJServer();
	}
}
