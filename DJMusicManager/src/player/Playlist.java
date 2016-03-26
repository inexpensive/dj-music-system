package player;

import jahspotify.media.Track;

import java.util.LinkedList;

class Playlist {
	
	private final LinkedList<Song>  trackList = new LinkedList<>();
	private Song currentSong;
	
	Playlist(){
		currentSong = null;
	}
	
	//adds a song to the playlist. if nothing is currently playing, add to currently playing
	//otherwise add to the trackList
	void addToPlaylist(Song addTrack){
		if (currentSong == null){
			currentSong = addTrack;
		}
		else{
			trackList.add(addTrack);
		}
	}
	
	//skips the currently playing track and loads up the next one in the playlist
	//if there is no element in the playlist, it does nothing
	void skipTrack(){
		if (!trackList.isEmpty()){
			currentSong = trackList.remove();
		}
	}
	
	Song getCurrentSong(){
		return currentSong;
	}

}
