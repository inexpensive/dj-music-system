package player;

import jahspotify.media.Track;

public class Song {

	private Track track;
	private boolean skipped;
	
	public Song(Track t){
		setTrack(t);
		setSkipped(false);
	}

	public Track getTrack() {
		return track;
	}

	public void setTrack(Track track) {
		this.track = track;
	}

	public boolean isSkipped() {
		return skipped;
	}

	public void setSkipped(boolean skipped) {
		this.skipped = skipped;
	}
}
