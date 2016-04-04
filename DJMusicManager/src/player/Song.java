/************************************************************
 * Song for DJ Music Manager (tentative title)  	    	*
 * An abstract class that represents a Song.                *
 * 												            *
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/


/**
 * Provides the classes necessary to play required music with Java.
 */
package player;

public abstract class Song {

    /**
     * Defines the possible sources of a Song.
     */
	enum Source {
		LOCAL, SPOTIFY, MESSAGE
	}

	private boolean skipped;

	/**
	 * Get the source of the Song.
	 * @return The Song's source.
     */
    abstract public Source getSource();

    /**
     * Get the details to be displayed in the playlist view in the client.
     * @return A String containing the Song details.
     */
	abstract public String getSongDetails();

    /**
     * Get the skipped status of the Song.
     * @return The skipped status.
     */
	boolean isSkipped() {
		return skipped;
	}

    /**
     * Set the skipped status of the Song.
     * @param skipped The status to set skipped to.
     */
	void setSkipped(boolean skipped) {
		this.skipped = skipped;
	}
}
