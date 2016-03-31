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
	
	public MusicPlayer(DJServer s, MediaPlayer mediaP){
		playlist = new Playlist();
		server = s;
		mediaPlayer = mediaP;
	}
	
	private MusicPlayer(Playlist pl){
		playlist = pl;
	}

	public void testPlayLocal(){
		String song = Paths.get("/home/lawrence/Documents/School/Taylor Swift – 1989/02 Blank Space.m4a").toUri().toString();
		final Media file = new Media(song);
		mediaPlayer = new MediaPlayer(file);

		mediaPlayer.setOnReady(() -> {

            System.out.println("Duration: "+file.getDuration().toSeconds());

            // display media's metadata
            for (Map.Entry<String, Object> entry : file.getMetadata().entrySet()){
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }

            // play if you want
            mediaPlayer.play();
        });
	}
	
	//this has to be called first before anything else
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
					playlist.setJahSpotify(js);
                }
            }
        });
	}
	
	//play the current song in the playlist
	public void play(){
		currentSong = playlist.getCurrentSong();

		//if the song is from spotify
        if (currentSong.getSource() == Song.Source.SPOTIFY) {
            Track track = ((SpotifySong)currentSong).getTrack();
            MediaHelper.waitFor(track, 10);
            if (track.isLoaded()) {
                js.play(track.getId());
                this.updateTrackDetails(track);
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

			Runnable r = this::autoSkip;
			POOL.execute(r);

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

                // play if you want
                mediaPlayer.play();

                Runnable r = this::autoSkip;
                POOL.execute(r);
            });
		}

		//if the song is a message
		else if (currentSong.getSource() == Song.Source.MESSAGE) {
			String songPath = Paths.get(((Message) currentSong).getFileLocation()).toUri().toString();
			Media message = new Media(songPath);
			mediaPlayer = new MediaPlayer(message);

			mediaPlayer.setOnReady(() -> {


                duration = new Double(message.getDuration().toMillis()).longValue();

                // play if you want
                mediaPlayer.play();

                Runnable r = this::autoSkip;
                POOL.execute(r);
            });
		}


	}
	
	//skip the current song and play the next one
	public void skip(){
		currentSong.setSkipped(true);
		playlist.skipTrack();
		//if the song is from spotify, stop JavaFX
		if(currentSong.getSource() == Song.Source.LOCAL || currentSong.getSource() == Song.Source.MESSAGE){
			mediaPlayer.stop();
		}
		else {
			js.stop();
		}

		this.play();
	}
	
	//toggle pausing and unpausing of the song
	public void pause(){
		if (!paused){
			if (currentSong.getSource() == Song.Source.SPOTIFY) {
				js.pause();
			}
			else {
				mediaPlayer.pause();
			}
			paused = true;
		}
		else{
			if (currentSong.getSource() == Song.Source.SPOTIFY) {
				js.resume();
			}
			else {
				mediaPlayer.play();
			}
			paused = false;
		}
	}
	
	//search for the target string and output results as a Song ArrayList
	//maxes at 10 elements
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
		while (i < 10 && i < tempLinkResults.size()){
			Track track = js.readTrack(tempLinkResults.get(i));
			String title = track.getTitle();
			String artist = js.readArtist(track.getArtists().get(0)).getName();
			String album = js.readAlbum(track.getAlbum()).getName();
			results.add(new SpotifySong(track, title, artist, album));
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
	
	//adds the given song to the playlist
	public void addSong(Song song){
		playlist.addToPlaylist(song);
	}

	//return the playlist
	public Playlist getPlaylist() {
		return playlist;
	}
	
	//return currently playing to the server (or notify there's nothing playing)
	public String getCurrentlyPlaying(){
		if (currentSong != null)
			return this.trackToString(((SpotifySong) currentSong).getTrack());
		return "No Song Loaded";
	}

    public String[] getPlaylistDetails() {
        return playlist.getPlaylistDetails();
    }
	
	//automatically skips the track once it's over
	//TODO: figure out how to freeze the sleep counter while the system is paused in order to not trigger the autoskip early
	private void autoSkip(){
		Song tempSong = currentSong;
        elapsed = 0;
        server.sendUpdateElapsed(elapsed, duration);
        while (elapsed < duration && !tempSong.isSkipped()) {
            try {
                if (!paused) {
                    TimeUnit.MILLISECONDS.sleep(100);
                    elapsed += 100;
                    server.sendUpdateElapsed(elapsed, duration);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //check if the song has been skipped by an external source
        if (!tempSong.isSkipped()) {
            skip();
        }
		System.out.println("auto skip thread ended");
	}


}
