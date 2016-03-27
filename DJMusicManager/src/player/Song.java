package player;

abstract class Song {

	enum Source {
		LOCAL, SPOTIFY, MESSAGE
	}

	private boolean skipped;

    abstract public Source getSource();

	boolean isSkipped() {
		return skipped;
	}

	void setSkipped(boolean skipped) {
		this.skipped = skipped;
	}
}
