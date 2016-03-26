package player;

import jahspotify.media.Track;

class SpotifySong extends Song {

    private Track track;

    SpotifySong(Track track){
        this.track = track;
        setSkipped(false);
    }

    @Override
    public Source getSource() {
        return Source.SPOTIFY;
    }

    Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }
}
