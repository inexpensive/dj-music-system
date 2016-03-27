/************************************************************
 * Server for DJ Music Manager (tentative title)  			*
 * waits for requests from a proxy and passes	            *
 * them to the MusicPlayer	        						*
 *															*
 * uses the libjahspotify library 							*
 * by Niels van de Weem	 (nvdweem on github)				*
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/

package server;

import jahspotify.media.Track;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import player.MusicPlayer;
import sun.security.util.Password;

public class DJServer {
	
	private ServerSocket server;
	private MusicPlayer player;
	private boolean paused = false;
	private boolean playlistInit = false;
	private boolean started = false;
	private int skipRequestCount;
	private final ArrayList<ServerProxy> proxies = new ArrayList<>();
	private final Executor pool = Executors.newCachedThreadPool();
	private final String password;

	
	private DJServer(String pass) throws IOException{

        //set the password for the server
        password = pass;

		//set up server and allow a client to connect
		server = new ServerSocket(1729);
		player = new MusicPlayer(this);
		Scanner in = new Scanner(System.in);
		System.out.print("username: ");
		String username = in.next();
		System.out.print("password: ");
		String spotifyPassword = in.next(); //TODO: mask this using the Console class, which doesn't work with javaw (which is what IDEs use)
		player.login(username, spotifyPassword);
		skipRequestCount = 0;
		in.close();
		
		this.createNewProxy();
		
	}
	
	 void createNewProxy() {
		Runnable r = new Runnable(){
			@Override
			public void run(){
				try {
					proxy();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		pool.execute(r);
		
	}
	
	private void proxy() throws IOException{
		proxies.add(new ServerProxy(server,this));
	}

	//sends a play request to the MusicPlayer if not started and playlist is initialized
	//calls pause if the system is paused
	 void play(){
		if (!started && playlistInit) {
			player.play();
			started = true;
		}
		else if (paused){
			pause();
		}
	}
	
	//sends a pause request to the MusicPlayer	
	void pause(){
		player.pause();
		paused = !paused;
	}
	
	//sends a skip request to the MusicPlayer once 3 skipRequests are received
	void skip(){
		skipRequestCount++;
		int SKIP_THRESHOLD = 3;
		if (skipRequestCount >= SKIP_THRESHOLD) {
			player.skip();
		}
	}
	
	//sends a login request the the MusicPlayer
	public void login(String username, String password){
		player.login(username, password);
	}
	
	//sends a search to the music player and sends back the results to the proxy
	ArrayList<Track> search(String target){
		return player.search(target);
	}
	
	//closes the server
	public void close() throws IOException{
		server.close();
		System.exit(0);
	}
	
	//sends an update request to each proxy
	public void updateCurrentlyPlaying(String currentlyPlaying) throws IOException{
		for (ServerProxy proxy : proxies) {
			proxy.updateCurrentlyPlaying(currentlyPlaying);
			proxy.resetSkipRequested();
		}
		skipRequestCount = 0;
	}
	
	//start the server!
	public static void main(String[] args) throws IOException{
			new DJServer("password");
	}
	//sends the taken in index to the MusicPlayer and initializes playlist boolean if not initialized
	void add(Track track) {
		player.addSong(track);
		if (!playlistInit) {
			playlistInit = true;
		}		
	}

	String trackToString(Track track) {
		return player.trackToString(track);
	}

	String getCurrentlyPlaying() {
		return player.getCurrentlyPlaying();
	}

	void removeProxy(ServerProxy serverProxy) {
		proxies.remove(serverProxy);
	}

    boolean checkPassword(String check){
        return password.contentEquals(check);
    }

    void adminSkip() {
        player.skip();
    }
}
