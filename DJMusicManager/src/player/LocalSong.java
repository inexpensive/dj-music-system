package player;

class LocalSong extends Song {

    private String fileLocation;

    public LocalSong(String fileLocation){
        this.fileLocation = fileLocation;
        setSkipped(false);
    }

    @Override
    public Source getSource() {
        return Source.LOCAL;
    }

    String getFileLocation(){
        return fileLocation;
    }
}
