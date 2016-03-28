package player;

import jahspotify.JahSpotify;
import jahspotify.media.Track;

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
        String details = null;
        if (song.getSource() == Song.Source.SPOTIFY) {
            Track track = ((SpotifySong) song).getTrack();
            details = track.getTitle() + " by "
                    + js.readArtist(track.getArtists().get(0)).getName() + " on "
                    + js.readAlbum(track.getAlbum()).getName();
        }
        else if (song.getSource() == Song.Source.LOCAL){
            details = "A local song."; //this needs to show the details in the song

        }
        else if (song.getSource() == Song.Source.MESSAGE){
            details = "A message from someone at the party!";
        }
        return details;
    }


    Song getCurrentSong(){
		return currentSong;
	}

}
