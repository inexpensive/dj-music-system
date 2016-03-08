/************************************************************
 * MusicPlayer for DJ Music Manager (tentative title)  		*
 * connects to and plays songs from spotify					*
 * 												            *
 * uses the libjahspotify library 							*
 * by Niels van de Weem	 (nvdweem on github)				*
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/

package player;

import jahspotify.AbstractConnectionListener;
import jahspotify.JahSpotify;
import jahspotify.Query;
import jahspotify.Search;
import jahspotify.SearchResult;
import jahspotify.media.Link;
import jahspotify.media.Track;
import jahspotify.services.JahSpotifyService;
import jahspotify.services.MediaHelper;
import jahspotify.services.SearchEngine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import server.DJServer;


public class MusicPlayer {
	
	private JahSpotify js;
	private boolean paused = false;
	private String songTitle;
	private String artistName;
	private String albumName;
	private int duration;
	private Playlist playlist;
	private Song currentSong;
	private DJServer server;
	
	protected Executor pool = Executors.newCachedThreadPool();
	
	public MusicPlayer(DJServer s){
		playlist = new Playlist();
		server = s;
	}
	
	public MusicPlayer(Playlist pl){
		playlist = pl;
	}
	
	//this has to be called first before anything else
	public void login(final String username, final String password){
		// Determine the tempfolder and make sure it exists.
        File temp = new File(new File(MusicPlayer.class.getResource("MusicPlayer.class").getFile()).getParentFile(), "temp");
        temp.mkdirs();

        // Start JahSpotify and login
        JahSpotifyService.initialize(temp);
        
        js = JahSpotifyService.getInstance().getJahSpotify();
        
        js.addConnectionListener(new AbstractConnectionListener() {
            @Override
            public void initialized(final boolean initialized) { 

                // When JahSpotify is initialized, we can attempt to
                // login.
                if (initialized)
                    js.login(username, password, null, false);
            }
            
            @Override
            public void loggedIn(final boolean success) {
                if (!success) {
                    System.err.println("Unable to login.");
                    System.exit(1);
                }
                else {
                	System.out.println("you did it!");
                }
            }
        });
	}
	
	//play the current song in the playlist
	public void play(){
		Track track = playlist.getCurrentTrack();
		MediaHelper.waitFor(track, 10);
		if (track.isLoaded()){
			js.play(track.getId());
			this.updateTrackDetails(track);
		}
		duration = track.getLength();
		currentSong = new Song(track);
		//this avoids a bug with searching (it should be undetectable to the ear)
		//for whatever reason pausing and unpausing the player prevents the bug
		js.pause();
		try {
			TimeUnit.SECONDS.sleep(1);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		js.resume();
		Runnable r = new Runnable(){
			@Override
			public void run(){
				autoSkip();
			}
		};
		pool.execute(r);
		//send the currently song to the server
		try {
			this.updateCurrentlyPlaying();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//skip the current song and play the next one
	public void skip(){
		currentSong.setSkipped(true);
		playlist.skipTrack();
		this.play();
	}
	
	//toggle pausing and unpausing of the song
	public void pause(){
		if (!paused){
			js.pause();
			paused = true;
		}
		else{
			js.resume();
			paused = false;
		}
	}
	
	//search for the target string and output results as a Track ArrayList
	//maxes at 10 elements
	public ArrayList<Track> search(String target){
		
		//search for the target
		Search search = new Search(Query.token(target));
		SearchResult result = SearchEngine.getInstance().search(search);
		MediaHelper.waitFor(result, 5);
		//need these for later
		ArrayList<String> temp = new ArrayList<String>();
		List<Link> tempLinkResults = result.getTracksFound();
		ArrayList<Track> results = new ArrayList<Track>();
		//move the search results into other arrays
		int i = 0;
		while (i < 10 && i < tempLinkResults.size()){
			results.add(js.readTrack(tempLinkResults.get(i)));
			temp.add(trackToString(results.get(i)));
			i++;
		}
		return results;
		
		
	}
	
	//outputs desired format for the currentlyPlaying info in the client
	public String trackToString(Track track) {
		return track.getTitle() + " by " + js.readArtist(track.getArtists().get(0)).getName() + " on " + js.readAlbum(track.getAlbum()).getName();
	}

	//update the details of the Music Player
	private void updateTrackDetails(Track track){
		songTitle = track.getTitle();
		artistName = js.readArtist(track.getArtists().get(0)).getName();
		albumName = js.readAlbum(track.getAlbum()).getName();
	}
	
	//return the currently loaded song's title
	public String getSongTitle(){
		return songTitle;
	}
	
	//return the currently loaded song's artist
	public String getArtist(){
		return artistName;
	}
	
	//return the currently loaded song's album
	public String getAlbum(){
		return albumName;
	}
	
	//adds the given track to the playlist
	public void addSong(Track track){
		playlist.addToPlaylist(track);
	}

	//return the playlist
	public Playlist getPlaylist() {
		return playlist;
	}
	
	//return currently playing to the server (or notify there's nothing playing)
	public String getCurrentlyPlaying(){
		if (currentSong != null)
			return this.trackToString(currentSong.getTrack());
		return "No Song Loaded";
	}
	
	//automatically skips the track once it's over
	//TODO: figure out how to freeze the sleep counter while the system is paused in order to not trigger the autoskip early
	private void autoSkip(){
		Song tempSong = currentSong;
		try {
			TimeUnit.MILLISECONDS.sleep(duration);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		//check if the song has been skipped by an external source
		if (!tempSong.isSkipped()){
			skip();
		}
		System.out.println("auto skip thread ended");
	}
	
	//send an update currently playing song request to the server
	private void updateCurrentlyPlaying() throws IOException {
		
		String currentlyPlaying = this.trackToString(currentSong.getTrack());
		server.updateCurrentlyPlaying(currentlyPlaying);
	}
	
}
