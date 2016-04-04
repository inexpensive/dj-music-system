/************************************************************
 * Message for DJ Music Manager (tentative title)  		    *
 * Defines a message for the MusicPlayer                    *
 * Extends abstract class Song                              *
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/

/**
 * Provides the classes necessary to play required music with Java.
 */
package player;

public class Message extends Song{

    private final String fileLocation;

    /**
     * Creates a message from the given fileLocation.
     * @param fileLocation The location of the message.
     */
    public Message(String fileLocation){
        this.fileLocation = fileLocation;
        setSkipped(false);
    }

    /**
     * Get the source of the Song.
     * @return Source.Message
     */
    @Override
    public Source getSource() {
        return Source.MESSAGE;
    }

    /**
     * Get the details to be displayed in the playlist view in the client.
     * @return A String containing "A recorded message."
     */
    @Override
    public String getSongDetails() {
        return "A recorded message.";
    }

    /**
     * Return the file location of the LocalSong.
     * @return The file location as a String.
     */
    String getFileLocation(){
        return fileLocation;
    }

}
