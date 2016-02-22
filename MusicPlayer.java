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
	
	public void play(String uri){
		Track track = js.readTrack(Link.create(uri));
		MediaHelper.waitFor(track, 10);
		if (track.isLoaded()){
			js.play(track.getId());			
		}
		updateTrackDetails(track);
	}
	
	public void play(Track track){
		MediaHelper.waitFor(track, 10);
		if (track.isLoaded()){
			js.play(track.getId());
		}
	}
	
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
	
	public Track search(String target){
		Search search = new Search(Query.token(target));
		SearchResult result = SearchEngine.getInstance().search(search);
		MediaHelper.waitFor(result, 10);
		Link tmp = result.getTracksFound().get(0);
		Track out = js.readTrack(tmp);
		updateTrackDetails(out);
		return out;
	}
	
	public void updateTrackDetails(Track track){
		songTitle = track.getTitle();
		artistName = js.readArtist(track.getArtists().get(0)).getName();
		albumName = js.readAlbum(track.getAlbum()).getName();
	}
	
	public String getSongTitle(){
		return songTitle;
	}
	
	public String getArtist(){
		return artistName;
	}
	
	public String getAlbum(){
		return albumName;
	}
}
