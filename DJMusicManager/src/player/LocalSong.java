/************************************************************
 * LocalSong for DJ Music Manager (tentative title)  		*
 * Defines a locally-sourced song for the MusicPlayer       *
 * Extends abstract class Song                              *
 * 												            *
 * uses the apache commons library                          *
 *                                                          *
 * uses the apache tika library                             *
 * 															*
 * by Lawrence Bouzane (inexpensive on github)				*
 ************************************************************/

/**
 * Provides the classes necessary to play required music with Java.
 */
package player;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.parser.mp4.MP4Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;

public class LocalSong extends Song {

    private final String fileLocation;
    private String title, artist, album;

    /**
     * Creates a local song from the given fileLocation.
     * @param fileLocation The location of the song to be added.
     */
    public LocalSong(String fileLocation){
        this.fileLocation = fileLocation;
        try {
            setSkipped(false);
            //set up a parser to get details from the metadata of the file
            InputStream in = new FileInputStream(new File(fileLocation));
            ContentHandler handler = new DefaultHandler();
            Metadata metadata = new Metadata();
            Parser parser = null;
            String ext = FilenameUtils.getExtension(fileLocation);
            //depending on the file extension it sets up either an MP4Parser or an MP3Parser
            if (ext.contentEquals("m4a")) {
                parser = new MP4Parser();
            }
            else if (ext.contentEquals("mp3")){
                parser = new Mp3Parser();
            }
            ParseContext parseContext = new ParseContext();
            //get the song details from the parser
            if (parser != null) {
                parser.parse(in, handler, metadata, parseContext);
                this.title = metadata.get("dc:title");
                this.artist = metadata.get("xmpDM:artist");
                this.album = metadata.get("xmpDM:album");
            }
            in.close();
        } catch (SAXException | IOException | TikaException e) {
            e.printStackTrace();
        }


    }

    /**
     * Get the source of the Song.
     * @return Source.LOCAL
     */
    @Override
    public Source getSource() {
        return Source.LOCAL;
    }

    /**
     * Return the file location of the LocalSong.
     * @return The file location as a String.
     */
    String getFileLocation(){
        return fileLocation;
    }

    /**
     * Get the title of the LocalSong.
     * @return The title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the artist of the LocalSong
     * @return The artist.
     */
    public String getArtist() {
        return artist;
    }

    /**
     * Get the album of the LocalSong
     * @return The album.
     */
    public String getAlbum() {
        return album;
    }

    /**
     * Get the details to be displayed in the playlist view in the client.
     * @return A String containing the Song details.
     */
    @Override
    public String getSongDetails() {
        return title + " by " + artist + " on " + album;
    }
}
