package player;

public abstract class Song {

	enum Source {
		LOCAL, SPOTIFY, MESSAGE
	}

	private boolean skipped;

    abstract public Source getSource();

	abstract public String getSongDetails();

	boolean isSkipped() {
		return skipped;
	}

	void setSkipped(boolean skipped) {
		this.skipped = skipped;
	}
}
