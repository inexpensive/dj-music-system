package player;

import jahspotify.media.Track;

import java.util.LinkedList;

class Playlist {
	
	private final LinkedList<Track>  trackList = new LinkedList<>();
	private Track currentTrack;
	
	Playlist(){
		currentTrack = null;
	}
	
	//adds a song to the playlist. if nothing is currently playing, add to currently playing
	//otherwise add to the trackList
	void addToPlaylist(Track addTrack){
		if (currentTrack == null){
			currentTrack = addTrack;
		}
		else{
			trackList.add(addTrack);
		}
	}
	
	//skips the currently playing track and loads up the next one in the playlist
	//if there is no element in the playlist, it does nothing
	void skipTrack(){
		if (!trackList.isEmpty()){
			currentTrack = trackList.remove();
		}
	}
	
	Track getCurrentTrack(){
		return currentTrack;
	}

}
