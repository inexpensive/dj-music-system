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

class ServerProxy {

	private final DJServer DJ_SERVER;
	private ObjectOutputStream outToClient, outToCurrentlyPlaying;
	private ObjectInputStream inFromClient;
	private ArrayList<Track> results;
	private boolean skipRequested;

	
	ServerProxy(ServerSocket ser, DJServer DJ_SERVER) throws IOException{
		//setting up the socket to accept a connection from a client
		this.DJ_SERVER = DJ_SERVER;
		Socket controlSocket = ser.accept();
		Socket currentlyPlayingSocket = ser.accept();
		skipRequested = false;
		System.out.println("CONNECTION ESTABLISHED");
		DJ_SERVER.createNewProxy();
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
		Executor pool = Executors.newCachedThreadPool();
		pool.execute(r);
	}
	
	//sends an update request to the client to update the currently playing song
	void updateCurrentlyPlaying(String currentlyPlaying) throws IOException{
		outToCurrentlyPlaying.writeObject(currentlyPlaying);
		skipRequested = false;
	}
	
	private void close(){
		DJ_SERVER.removeProxy(this);
	}
	
	void resetSkipRequested(){
		skipRequested = false;
	}
	
	//listens for commands from the client
		private void listener() throws IOException, ClassNotFoundException {
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
					DJ_SERVER.play();
					break;
						
				//sends a pause request to the server	
				case "pause":
					DJ_SERVER.pause();
					break;
						
				//sends a skip request to the server
				case "skip":
					if(!skipRequested) {
						DJ_SERVER.skip();
						skipRequested = true;
					}
					break;
						
				//sends the search target string to the server
				//and sends the results to the client
				case "search":
					String target = (String) inFromClient.readObject();
                    results = DJ_SERVER.search(target);
                    String[] out = new String[results.size()];
					for (int i = 0; i < results.size(); i++){
						out[i] = DJ_SERVER.trackToString(results.get(i));
					}
					outToClient.writeObject(out);
					break;
						
				//sends the taken in index to the server and initializes playlist boolean if not initialized
				case "add":
					Track track = results.get((Integer) inFromClient.readObject());
					DJ_SERVER.add(track);
					break;
					
				case "curr":
					String curr = DJ_SERVER.getCurrentlyPlaying();
					outToClient.writeObject(curr);
					break;
						
				//closes the proxy
				case "close":
					this.close();
					done = true;
					break;
				}
			}
			
		}
}
