/************************************************************
 * MusicPlayer for DJ Music Manager (tentative title)  		*
 * connects to and plays songs from spotify 				*
 * and locally stored files                                 *
 * 												            *
 * uses the libjahspotify library 							*
 * by Niels van de Weem	 (nvdweem on github)				*
 *                                                          *
 * uses JavaFX                                              *
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/


/**
 * Provides the classes necessary to play required music with Java.
 */
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import server.DJServer;


public class MusicPlayer {
	
	private JahSpotify js;
	private boolean paused = false;
	private String songTitle;
	private String artistName;
	private String albumName;
	private long duration, elapsed;
	private final Playlist playlist;
	private Song currentSong;
	private DJServer server;
	private MediaPlayer mediaPlayer;

	private final Executor POOL = Executors.newCachedThreadPool();

    /**
     * Creates a MusicPlayer with the necessary connections to a MediaPlayer and a DJServer
     * @param djServer the DJServer associated with this MusicPlayer
     * @param mediaP the JavaFX MediaPlayer associated with this MusicPlayer
     */
	public MusicPlayer(DJServer djServer, MediaPlayer mediaP){
		playlist = new Playlist();
		server = djServer;
		mediaPlayer = mediaP;
	}

    /**
     * Logs into Spotify. Requires a premium subscription.
     * @param username The Spotify username.
     * @param password The Spotify password.
     */
	public void login(final String username, final String password){
		// Determine the tempfolder and make sure it exists.
        File temp = new File(new File(MusicPlayer.class.getResource("MusicPlayer.class").getFile()).getParentFile(), "temp");
		//noinspection ResultOfMethodCallIgnored
		temp.mkdirs();

        // Start JahSpotify and login
        JahSpotifyService.initialize(temp);
        
        js = JahSpotifyService.getInstance().getJahSpotify();
        
        js.addConnectionListener(new AbstractConnectionListener() {
            @Override
            public void initialized(final boolean initialized) { 

                // When JahSpotify is initialized, we can attempt to login.
                if (initialized)
                    js.login(username, password, null, false);
            }
            
            @Override
            public void loggedIn(final boolean success) {
                //if it fails, let the user know and exit the system.
                if (!success) {
                    System.err.println("Unable to login.");
                    System.exit(1);
                }
                //otherwise let the userknow they are logged in.
                else {
                	System.out.println("Login to Spotify successful!");
                }
            }
        });
	}

    /**
     * Play the current song in the playlist, and update currentSong with it.
     */
	public void play(){
		currentSong = playlist.getCurrentSong();

		//if the song is from spotify
        if (currentSong.getSource() == Song.Source.SPOTIFY) {
            Track track = ((SpotifySong)currentSong).getTrack();
            MediaHelper.waitFor(track, 10);
            if (track.isLoaded()) {
                js.play(track.getId());
            }
            duration = track.getLength();
            //this avoids a bug with searching (it should be undetectable to the ear)
            //for whatever reason pausing and unpausing the player prevents the bug
            js.pause();
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            js.resume();
        }
		//if the song is local
		else if (currentSong.getSource() == Song.Source.LOCAL) {
			String songPath = Paths.get(((LocalSong) currentSong).getFileLocation()).toUri().toString();
			Media localSong = new Media(songPath);
			mediaPlayer = new MediaPlayer(localSong);

			mediaPlayer.setOnReady(() -> {


                duration = new Double(localSong.getDuration().toMillis()).longValue();

                // display media's metadata
                for (Map.Entry<String, Object> entry : localSong.getMetadata().entrySet()){
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }

                // play the song
                mediaPlayer.play();
            });
		}

		//if the song is a message
		else if (currentSong.getSource() == Song.Source.MESSAGE) {
			String songPath = Paths.get(((Message) currentSong).getFileLocation()).toUri().toString();
			Media message = new Media(songPath);
			mediaPlayer = new MediaPlayer(message);

			mediaPlayer.setOnReady(() -> {


                duration = new Double(message.getDuration().toMillis()).longValue();

                // play the message
                mediaPlayer.play();
            });
		}

        //Start an autoSkip thread to skip the song once it's over
        Runnable r = this::autoSkip;
        POOL.execute(r);


	}

    /**
     * Skip the current song and start the next one. Send a resetSkipRequests command to the DJServer.
     */
	public void skip(){
		currentSong.setSkipped(true);
		playlist.skipTrack();
		//if the song is not from spotify, stop JavaFX
		if(currentSong.getSource() == Song.Source.LOCAL || currentSong.getSource() == Song.Source.MESSAGE){
			mediaPlayer.stop();
		}
        //otherwise stop JahSpotify
		else {
			js.stop();
		}

		this.play();
        //reset the skip status in the DJServer
        server.resetSkipRequests();
	}

    /**
     * Toggles the paused stated of the MusicPlayer and sends the proper command to pause or resume playback of music.
     */
	public void pause(){
		if (!paused){
            //pause JahSpotify if the song is from Spotify
			if (currentSong.getSource() == Song.Source.SPOTIFY) {
				js.pause();
			}
            //otherwise pause JavaFX
			else {
				mediaPlayer.pause();
			}
            //set paused status to true
			paused = true;
		}
		else{
            //resume JahSpotify is the song is from Spotify
			if (currentSong.getSource() == Song.Source.SPOTIFY) {
				js.resume();
			}
            //otherwise resume JavaFX
			else {
				mediaPlayer.play();
			}
            //set paused status to false
			paused = false;
		}
	}

    /**
     * Search Spotify for tracks that match the given target String.
     * Maxes out at the top ten results.
     * @param target The target to be searched.
     * @return An ArrayList of SpotifySongs matching the search target.
     */
	public ArrayList<Song> search(String target){
		
		//search for the target
		Search search = new Search(Query.token(target));
		SearchResult result = SearchEngine.getInstance().search(search);
		MediaHelper.waitFor(result, 5);
		//need these for later
		List<Link> tempLinkResults = result.getTracksFound();
		ArrayList<Song> results = new ArrayList<>();
		//move the search results into other arrays
		int i = 0;
        //add the songs to the ArrayList
		while (i < 10 && i < tempLinkResults.size()){
            //get the track details
			Track track = js.readTrack(tempLinkResults.get(i));
			String title = track.getTitle();
			String artist = js.readArtist(track.getArtists().get(0)).getName();
			String album = js.readAlbum(track.getAlbum()).getName();
            //add to the list
			results.add(new SpotifySong(track, title, artist, album));
			i++;
		}
		return results;
		
		
	}

    /**
     * Adds the given song to the playlist.
     * @param song The song to be added to the playlist.
     */
	public void addSong(Song song){
		playlist.addToPlaylist(song);
	}

    /**
     * Get the playlist details.
     * @return A String array containing the details of the playlist.
     */
    public String[] getPlaylistDetails() {
        return playlist.getPlaylistDetails();
    }

    /**
     * Automatically skips the track once it's over.
     */
	private void autoSkip(){
		Song tempSong = currentSong;
        elapsed = 0;
        //send an update to the server of the status of the song.
        server.sendUpdateElapsed(elapsed, duration);
        //while the song is still playing and the song hasn't been skipped externally, update the status
        while (elapsed < duration && !tempSong.isSkipped()) {
            try {
                //if the song isn't paused, wait for 100 milliseconds and increase elapsed by that amount.
                if (!paused) {
                    TimeUnit.MILLISECONDS.sleep(100);
                    elapsed += 100;
                    //send an update to the server.
                    server.sendUpdateElapsed(elapsed, duration);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //check if the song has been skipped by an external source and skip if it hasn't.
        if (!tempSong.isSkipped()) {
            skip();
        }
	}


}
