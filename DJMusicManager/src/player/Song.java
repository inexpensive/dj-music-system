package player;

import jahspotify.media.Track;

class Song {

	private Track track;
	private boolean skipped;
	
	Song(Track t){
		setTrack(t);
		setSkipped(false);
	}

	Track getTrack() {
		return track;
	}

	private void setTrack(Track track) {
		this.track = track;
	}

	boolean isSkipped() {
		return skipped;
	}

	void setSkipped(boolean skipped) {
		this.skipped = skipped;
	}
}
