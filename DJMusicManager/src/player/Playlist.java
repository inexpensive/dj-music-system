package player;

import jahspotify.JahSpotify;

import java.util.LinkedList;

class Playlist {
	
	private final LinkedList<Song>  trackList = new LinkedList<>();
	private Song currentSong;


    private JahSpotify js;
	
	Playlist(){
		currentSong = null;
        js = null;
	}
	
	//adds a song to the playlist. if nothing is currently playing, add to currently playing
	//otherwise add to the trackList
	void addToPlaylist(Song addSong){
		if (currentSong == null){
			currentSong = addSong;
		}
		else{
			trackList.add(addSong);
		}
	}
	
	//skips the currently playing track and loads up the next one in the playlist
	//if there is no element in the playlist, it does nothing
	void skipTrack(){
		if (!trackList.isEmpty()){
			currentSong = trackList.remove();
		}
	}

    public JahSpotify getJahSpotify() {
        return js;
    }

    void setJahSpotify(JahSpotify js) {
        this.js = js;
    }

    String[] getPlaylistDetails() {
        if (currentSong == null) {
            return null;
        }
        String[] details = new String[trackList.size() + 1];
        details[0] = getSongDetails(currentSong);
        for(int i = 0; i < trackList.size() ; i++) {
            details[i + 1] = getSongDetails(trackList.get(i));
        }
        return details;
    }

    private String getSongDetails(Song song){
        return song.getSongDetails();
    }


    Song getCurrentSong(){
		return currentSong;
	}

}
