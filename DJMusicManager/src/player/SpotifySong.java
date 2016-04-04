/************************************************************
 * SpotifySong for DJ Music Manager (tentative title)  		*
 * Defines a Spotify-sourced song for the MusicPlayer       *
 * Extends abstract class Song                              *
 * 												            *
 * uses the libjahspotify library 							*
 * by Niels van de Weem	 (nvdweem on github)				*
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/

/**
 * Provides the classes necessary to play required music with Java.
 */
package player;

import jahspotify.media.Track;

class SpotifySong extends Song {

    private Track track;
    private String title, artist, album;

    /**
     * Creates a SpotifySong with the given details.
     * @param track The track the SpotifySong is associated with.
     * @param title The title of the track.
     * @param artist The track's artist.
     * @param album The album the track is on.
     */
    SpotifySong(Track track, String title, String artist, String album){
        this.track = track;
        this.title = title;
        this.artist = artist;
        this.album = album;
        setSkipped(false);
    }

    /**
     * Get the source of the Song.
     * @return Source.SPOTIFY
     */
    @Override
    public Source getSource() {
        return Source.SPOTIFY;
    }

    /**
     * Get the details to be displayed in the playlist view in the client.
     * @return A String containing the Song details.
     */
    @Override
    public String getSongDetails() {
        return title + " by " + artist + " on " + album;
    }

    /**
     * Get the Track to be played in the MusicPlayer.
     * @return The Track associated with this SpotifySong.
     */
    Track getTrack() {
        return track;
    }
}
