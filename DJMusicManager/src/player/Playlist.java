/************************************************************
 * Playlist for DJ Music Manager (tentative title)  		*
 * Defines the playlist for the MusicPlayer                 *
 *                                                          *
 * uses the libjahspotify library 							*
 * by Niels van de Weem	 (nvdweem on github)				*
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/

/**
 * Provides the classes necessary to play required music with Java.
 */
package player;

import jahspotify.JahSpotify;

import java.util.LinkedList;

class Playlist {
	
	private final LinkedList<Song>  songList = new LinkedList<>();
	private Song currentSong;


    /**
     * Create a playlist with null currentSong and JahSpotify.
     */
	Playlist(){
		currentSong = null;
	}

    /**
     * Adds the specified song to the playlist. If nothing is currently playing, add to currentSong.
     * Otherwise, add to songList.
     * @param addSong The song to be added.
     */
	void addToPlaylist(Song addSong){
		if (currentSong == null){
			currentSong = addSong;
		}
		else{
			songList.add(addSong);
		}
	}

    /**
     * Replaces with the currentSong with the next song in the songList (if it exists).
     * Otherwise, it is set to null.
     */
	void skipTrack(){
		if (!songList.isEmpty()){
			currentSong = songList.remove();
		}
        else {
            currentSong = null;
        }
	}

    /**
     * Get the SongDetails for both the currentSong and everything in the songList and return them as a String array.
     * @return A String array containing each Song's details.
     */
    String[] getPlaylistDetails() {
        if (currentSong == null) {
            return null;
        }
        String[] details = new String[songList.size() + 1];
        details[0] = currentSong.getSongDetails();
        for(int i = 0; i < songList.size() ; i++) {
            details[i + 1] = songList.get(i).getSongDetails();
        }
        return details;
    }

    /**
     * Get the current Song.
     * @return currentSong.
     */
    Song getCurrentSong(){
		return currentSong;
	}

}
