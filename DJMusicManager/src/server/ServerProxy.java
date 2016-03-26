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

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

class ServerProxy {

	private final DJServer djServer;
	private ObjectOutputStream outToClient, outToCurrentlyPlaying;
	private ObjectInputStream inFromClient;
	private ArrayList<Track> results;
	private boolean skipRequested;

	
	ServerProxy(ServerSocket ser, DJServer djServer) throws IOException{
		//setting up the socket to accept a connection from a client
		this.djServer = djServer;
		Socket controlSocket = ser.accept();
		Socket currentlyPlayingSocket = ser.accept();
		skipRequested = false;
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
				catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
                catch (IOException e){
                    System.out.println("caught IOException. closing server");
                    e.printStackTrace();
                    close();
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
		djServer.removeProxy(this);
	}
	
	void resetSkipRequested(){
		skipRequested = false;
	}
	
	//listens for commands from the client
		private void listener() throws IOException, ClassNotFoundException {
			System.out.println("NOW RUNNING");
			String command = "";
			boolean done = false;
			while(!done){
				System.out.println("waiting for input...");
				try{
					command = (String) inFromClient.readObject();
				}
				catch (EOFException e){
                    System.out.println("caught EOFException. closing server");
                    this.close();
                    done = true;
                }
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
					if(!skipRequested) {
						djServer.skip();
						skipRequested = true;
					}
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
					Track track = results.get((Integer) inFromClient.readObject());
					djServer.add(track);
					break;
					
				case "curr":
					String curr = djServer.getCurrentlyPlaying();
					outToClient.writeObject(curr);
					break;
						
				//closes the proxy
				case "close":
					this.close();
					done = true;
					break;

				case "message":
					File test = new File("/home/lawrence/Documents/School/test.3gp");
                    byte[] myByteArray = (byte[]) inFromClient.readObject();
                    FileOutputStream fos = new FileOutputStream(test);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    bos.write(myByteArray, 0 , myByteArray.length);
                    bos.flush();
                    bos.close();

				}
			}
			
		}
}
