/************************************************************
 * ServerProxy for DJ Music Manager (tentative title)  		*
 * waits for requests from a client and passes	            *
 * them to the server	        							*
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/

/**
 * Provides the classes necessary start a server to control a Music System.
 */
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

    /**
     * Creates a ServerProxy on the given ServerSocket and associated with the given DJServer
     * @param ser The ServerSocket clients connect to.
     * @param djServer The DJSystem associated with the ServerSocket.
     * @throws IOException
     */
	ServerProxy(ServerSocket ser, DJServer djServer) throws IOException{
		//setting up the socket to accept a connection from a client
		this.djServer = djServer;
		Socket controlSocket = ser.accept();
		Socket currentlyPlayingSocket = ser.accept();
		skipRequested = false;
        //once connected, log it on the server that a connection has been established.
		System.out.println("CONNECTION ESTABLISHED");
		djServer.createNewProxy();
		outToClient = new ObjectOutputStream(controlSocket.getOutputStream());
		inFromClient = new ObjectInputStream(controlSocket.getInputStream());
		outToCurrentlyPlaying = new ObjectOutputStream(currentlyPlayingSocket.getOutputStream());
		//thread to wait for input from the client
        //this needs to be on its own thread to be asynchronous from the updateElapsed method
		Runnable r = () -> {
            try {
                listener();
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            //close the proxy if an IOException is caught here. (happens occasionally when the client is closed unexpectedly).
            catch (IOException e) {
                System.out.println("caught IOException. closing server");
                e.printStackTrace();
                close();
            }
        };
		Executor pool = Executors.newCachedThreadPool();
		pool.execute(r);
	}

    /**
     * Let the DJServer know to remove this proxy from the proxies list.
     */
    private void close(){
		djServer.removeProxy(this);
	}

    /**
     * Reset the skipRequested status.
     */
	void resetSkipRequested(){
		skipRequested = false;
	}

    /**
     * Listens to and processes commands coming in from the client.
     * @throws IOException
     * @throws ClassNotFoundException
     */
	private void listener() throws IOException, ClassNotFoundException {
		System.out.println("NOW RUNNING");
		String command = "";
		boolean done = false;
		while(!done){
			System.out.println("waiting for input...");
			try{
				command = (String) inFromClient.readObject();
			}
            //close the proxy if an EOFException is caught here. (happens when the client is closed unexpectedly).
    		catch (EOFException e){
                System.out.println("caught EOFException. closing server");
                this.close();
                done = true;
            }
			System.out.println("received " + command);

            //I couldn't get this to work properly with an enum in both projects, so I stuck with using strings.
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
						out[i] = results.get(i).getSongDetails();
					}
					outToClient.writeObject(out);
				break;
						
				//sends the taken in index to the server and initializes playlist boolean if not initialized
				case "add":
					Song song = results.get((Integer) inFromClient.readObject());
					djServer.add(song);
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

                //sends the playlist details to the client
                case "playlist":
                    String[] playlistDetails = djServer.getPlaylistDetails();
                    outToClient.writeObject(playlistDetails);
                break;

                //returns true if the client is started, false otherwise.
                case "playing":
                    outToClient.writeObject(djServer.isStarted());
                break;
			}
	    }
    }

    /**
     * Send the elapse and duration numbers (cast as integers) to the client to update the progress bar.
     * @param elapsed The elapsed time of the Song.
     * @param duration The total duration of the Song.
     */
    void updateElapsed(long elapsed, long duration) {
        try {
            int[] out = {(int) duration, (int) elapsed};
            outToCurrentlyPlaying.writeObject(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
