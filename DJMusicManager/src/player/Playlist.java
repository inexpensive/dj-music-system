package player;

import jahspotify.media.Track;

import java.util.LinkedList;

public class Playlist {
	
	private LinkedList<Track> trackList;
	private Track currentTrack;
	
	public Playlist(){
		currentTrack = null;
		trackList = new LinkedList<Track>();
	}
	
	//adds a song to the playlist. if nothing is currently playing, add to currently playing
	//otherwise add to the trackList
	public void addToPlaylist(Track addTrack){
		if (currentTrack == null){
			currentTrack = addTrack;
		}
		else{
			trackList.add(addTrack);
		}
	}
	
	//skips the currently playing track and loads up the next one in the playlist
	//if there is no element in the playlist, it does nothing
	public void skipTrack(){
		if (!trackList.isEmpty()){
			currentTrack = trackList.remove();
		}
	}
	
	public Track getCurrentTrack(){
		return currentTrack;
	}

}
