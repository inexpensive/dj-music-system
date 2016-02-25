/************************************************************
 * ServerProxy for DJ Music Manager (tentative title)  		*
 * waits for requests from a client and passes	            *
 * them to the server	        							*
 *															*
 * uses the libjahspotify library 							*
 * by Niels van de Weem	 (nvdweem on github)				*
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/

package server;

import jahspotify.media.Track;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ServerProxy {
	
	private ServerSocket server;
	private Socket controlSocket, currentlyPlayingSocket;
	private DJServer djServer;
	private ObjectOutputStream outToClient, outToCurrentlyPlaying;
	private ObjectInputStream inFromClient;
	private Executor pool = Executors.newCachedThreadPool();
	private ArrayList<Track> results;

	
	ServerProxy(ServerSocket ser, DJServer djServer) throws IOException{
		//setting up the socket to accept a connection from a client
		server = ser;
		this.djServer = djServer;
		controlSocket = server.accept();
		currentlyPlayingSocket = server.accept();
		System.out.println("CONNECTION ESTABLISHED");
		djServer.createNewProxy();
		outToClient = new ObjectOutputStream(controlSocket.getOutputStream());
		inFromClient = new ObjectInputStream(controlSocket.getInputStream());
		outToCurrentlyPlaying = new ObjectOutputStream(currentlyPlayingSocket.getOutputStream());
		//thread to wait for input from the client
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
	
	//sends an update request to the client to update the currently playing song
	//TODO: send an update when a client connects (if the system is playing music) (ideally to only that client)
	public void updateCurrentlyPlaying(String currentlyPlaying) throws IOException{
		outToCurrentlyPlaying.writeObject(currentlyPlaying);
	}
	
	//listens for commands from the client
		protected void listener() throws IOException, ClassNotFoundException {
			System.out.println("NOW RUNNING");
			String command;
			boolean done = false;
			while(!done){
				System.out.println("waiting for input...");
				command = (String) inFromClient.readObject();
				System.out.println("received " + command);
				switch (command) {
					
				//sends a play request to the server
				case "play":
					djServer.play();
					break;
						
				//sends a pause request to the server	
				case "pause":
					djServer.pause();
					break;
						
				//sends a skip request to the server
				case "skip":
					djServer.skip();
					break;
						
				//sends the search target string to the server
				//and sends the results to the client
				case "search":
					String target = (String) inFromClient.readObject();
					results = djServer.search(target);
					String[] out = new String[results.size()];
					for (int i = 0; i < results.size(); i++){
						out[i] = djServer.trackToString(results.get(i));
					}
					outToClient.writeObject(out);
					break;
						
				//sends the taken in index to the server and initializes playlist boolean if not initialized
				case "add":
					Track track = results.get(((Integer) inFromClient.readObject()).intValue());
					djServer.add(track);
					break;
						
				//closes the proxy
				case "close":
					//this.close();
					done = true;
					break;
				}
			}
			
		}
}
