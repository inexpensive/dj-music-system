package player;

import jahspotify.media.Track;

class SpotifySong extends Song {

    private Track track;
    private String title, artist, album;

    SpotifySong(Track track, String title, String artist, String album){
        this.track = track;
        this.title = title;
        this.artist = artist;
        this.album = album;
        setSkipped(false);
    }

    @Override
    public Source getSource() {
        return Source.SPOTIFY;
    }

    @Override
    public String getSongDetails() {
        return title + " by " + artist + " on " + album;
    }

    Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }
}
