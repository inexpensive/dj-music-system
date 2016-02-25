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

public class DJServer {
	
	private ServerSocket server;
	private MusicPlayer player;
	private boolean paused = false;
	private boolean playlistInit = false;
	private boolean started = false;
	private ArrayList<ServerProxy> proxies;
	
	protected Executor pool = Executors.newCachedThreadPool();

	
	public DJServer() throws IOException{
		
		proxies = new ArrayList<ServerProxy>();
		
		//set up server and allow a client to connect
		server = new ServerSocket(1729);
		player = new MusicPlayer(this);
		Scanner in = new Scanner(System.in);
		System.out.print("username: ");
		String username = in.next();
		System.out.print("password: ");
		String password = in.next(); //TODO: mask this
		player.login(username, password);
		in.close();
		
		this.createNewProxy();
		
	}
	
	public void createNewProxy() {
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
	
	protected void proxy() throws IOException{
		proxies.add(new ServerProxy(server,this));
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
	
	//sends a search to the music player and sends back the results to the proxy
	public ArrayList<Track> search(String target){
		ArrayList<Track> results = player.search(target);
		return results;
	}
	
	//closes the server
	public void close() throws IOException{
		server.close();
		System.exit(0);
	}
	
	//sends an update request to each proxy
	public void updateCurrentlyPlaying(String currentlyPlaying) throws IOException{
		for (int i = 0; i < proxies.size(); i++){
			proxies.get(i).updateCurrentlyPlaying(currentlyPlaying);
		}
	}
	
	//start the server!
	public static void main(String[] args) throws IOException{
			new DJServer();
	}
	//sends the taken in index to the MusicPlayer and initializes playlist boolean if not initialized
	public void add(Track track) {
		player.addSong(track);
		if (!playlistInit) {
			playlistInit = true;
		}		
	}

	public String trackToString(Track track) {
		// TODO Auto-generated method stub
		return player.trackToString(track);
	}
}
