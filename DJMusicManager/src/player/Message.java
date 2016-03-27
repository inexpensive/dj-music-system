package player;

public class Message extends Song{

    private final String fileLocation;

    public Message(String fileLocation){
        this.fileLocation = fileLocation;
        setSkipped(false);
    }

    @Override
    public Source getSource() {
        return Source.MESSAGE;
    }

    String getFileLocation(){
        return fileLocation;
    }

}
