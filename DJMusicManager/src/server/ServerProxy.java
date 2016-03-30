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

import player.Song;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

class ServerProxy {

	private final DJServer djServer;
	private ObjectOutputStream outToClient, outToCurrentlyPlaying;
	private ObjectInputStream inFromClient;
	private ArrayList<Song> results;
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
		Runnable r = () -> {
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
				switch (command) { //maybe make this an ENUM?
					
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
						out[i] = results.get(i).getSongDetails();
					}
					outToClient.writeObject(out);
				break;
						
				//sends the taken in index to the server and initializes playlist boolean if not initialized
				case "add":
					Song song = results.get((Integer) inFromClient.readObject());
					djServer.add(song);
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

                //takes in a recorded .mp4 file from the client and adds it to the playlist to be played
				case "message":
                    byte[] myByteArray = (byte[]) inFromClient.readObject();
					djServer.processMessage(myByteArray);
                break;

                //take in a password and see if it is the password to the server
                case "adminLogin":

                    if (djServer.checkPassword((String)inFromClient.readObject())) {
                        outToClient.writeObject(true);
                    }
                    else {
                        outToClient.writeObject(false);
                    }
                break;

                //directly skip the currently playing song
                case "adminSkip":
                    djServer.adminSkip();
                break;

                case "playlist":
                    String[] testCurr = djServer.getPlaylistDetails();
                    outToClient.writeObject(testCurr);
                break;

                case "playing":
                    outToClient.writeObject(djServer.isStarted());


				}
			}
			
		}

    void updateElapsed(long elapsed, long duration) {
        try {
            int[] out = {(int) duration, (int) elapsed};
            outToCurrentlyPlaying.writeObject(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
