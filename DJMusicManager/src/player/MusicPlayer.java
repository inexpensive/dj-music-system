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

public class MusicPlayer {
	
	private JahSpotify js;
	private boolean paused = false;
	private String songTitle;
	private String artistName;
	private String albumName;
	
	public MusicPlayer(){
		//empty constructor
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
	
	//load and play the given spotify track uri
	public void play(String uri){
		Track track = js.readTrack(Link.create(uri));
		MediaHelper.waitFor(track, 10);
		if (track.isLoaded()){
			js.play(track.getId());			
		}
		updateTrackDetails(track);
	}
	
	//load and play the given track
	public void play(Track track){
		MediaHelper.waitFor(track, 10);
		if (track.isLoaded()){
			js.play(track.getId());
		}
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
	
	//search for the target string. returns the first result as a Track
	public Track search(String target){
		Search search = new Search(Query.token(target));
		SearchResult result = SearchEngine.getInstance().search(search);
		MediaHelper.waitFor(result, 10);
		Link tmp = result.getTracksFound().get(0);
		Track out = js.readTrack(tmp);
		updateTrackDetails(out);
		return out;
	}
	
	//update the details of the Music Player
	public void updateTrackDetails(Track track){
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
}
